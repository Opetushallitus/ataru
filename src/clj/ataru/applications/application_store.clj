(ns ataru.applications.application-store
  (:require [ataru.application.application-states :as application-states]
            [cheshire.core :as json]
            [ataru.application.option-visibility :refer [visibility-checker]]
            [ataru.application.review-states :as application-review-states]
            [ataru.component-data.base-education-module-higher :as higher-module]
            [ataru.component-data.koski-tutkinnot-module :as ktm]
            [ataru.db.db :as db]
            [ataru.koodisto.koodisto-codes :refer [finland-country-code]]
            [ataru.dob :as dob]
            [ataru.forms.form-store :as forms]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.log.audit-log :as audit-log]
            [ataru.util :refer [answers-by-key] :as util]
            [ataru.tutkinto.tutkinto-util :as tutkinto-util]
            [ataru.person-service.person-service :as person-service]
            [ataru.selection-limit.selection-limit-service :as selection-limit]
            [ataru.util.random :as crypto]
            [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
            [camel-snake-kebab.core :refer [->snake_case ->kebab-case-keyword ->camelCase]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [clojure.set]
            [clojure.string]
            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as log]
            [ataru.applications.application-store-queries :as queries]
            [ataru.config.core :refer [config]]
            [ataru.application.harkinnanvaraisuus.harkinnanvaraisuus-util :refer [get-harkinnanvaraisuus-reason-for-hakukohde]]
            [ataru.component-data.base-education-module-2nd :refer [base-education-choice-key base-education-2nd-language-value-to-lang]]
            [clojure.edn :as edn])
  (:import [java.time
            LocalDateTime
            ZoneId]
           [org.postgresql.util
            PGobject
            PSQLException]
           java.time.format.DateTimeFormatter))

(defn- exec-db
  [ds-key query params]
  (db/exec ds-key query params))

(defn- get-datasource
  []
  {:connection
   {:datasource (db/get-datasource :db)}})

(def ^:private ->kebab-case-kw (partial transform-keys ->kebab-case-keyword))

(defn- find-value-from-answers [key answers]
  (:value (first (filter #(= key (:key %)) answers))))

(defn unwrap-application
  [application]
  (when application
    (assoc (->kebab-case-kw (dissoc application :content))
           :answers
           (-> application :content :answers))))

(defn- application-exists-with-secret-tx?
  "NB: takes into account also expired secrets"
  [hakija-secret connection]
  (let [application-count (->> (queries/yesql-get-application-count-for-secret {:secret hakija-secret} connection)
                               (first)
                               :count)]
    (pos? application-count)))

(defn application-exists-with-secret?
  "NB: takes into account also expired secrets"
  [hakija-secret]
  (application-exists-with-secret-tx? hakija-secret (get-datasource)))

(defn get-application-language-by-secret
  "NB: takes into account also expired secrets"
  [hakija-secret]
  (-> (exec-db :db queries/yesql-get-latest-application-language-by-any-version-of-secret {:secret hakija-secret})
      (first)
      :lang))

(def unique-violation "23505")

(defn add-new-secret-to-application-in-tx
  [connection application-key]
  (loop []
    (let [secret     (crypto/url-part 34)
          collision? (try
                       (queries/yesql-add-application-secret<! {:application_key application-key
                                                                :secret          secret}
                                                               {:connection connection})
                       false
                       (catch PSQLException e
                         (if (= unique-violation (.getSQLState e))
                           true
                           (throw e))))]
      (if collision?
        (do (log/warn "Application secret collision")
            (recur))
        secret))))

(defn add-new-koski-tutkinnot-for-application
  [application-key tutkinnot]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
      (queries/yesql-add-koski-tutkinnot<! {:application_key application-key
                                            :tutkinnot (json/generate-string {:tutkinnot tutkinnot})}
                                           {:connection conn})))

(defn koski-tutkinnot-for-application [application-key]
  (:tutkinnot (:tutkinnot (first (exec-db :db queries/yesql-get-koski-tutkinnot-for-application {:key application-key})))))

(defn- intersect?
  [set1 set2]
  (not-empty (clojure.set/intersection set1 set2)))

(defn hakukohde-oids-for-attachment-review
  [attachment-field hakutoiveet fields-by-id ylioppilastutkinto? excluded-attachment-ids-when-yo-and-jyemp]
  (let [belongs-tos (loop [field       attachment-field
                           belongs-tos []]
                      (if (some? field)
                        (recur (some->> (or (:followup-of field)
                                            (:children-of field))
                                        keyword
                                        (get fields-by-id))
                               (conj belongs-tos
                                     (set (concat (:belongs-to-hakukohteet field)
                                                  (:belongs-to-hakukohderyhma field)))))
                        belongs-tos))
        jyemp?      (and ylioppilastutkinto?
                         (contains? excluded-attachment-ids-when-yo-and-jyemp (:id attachment-field)))]
    (if (not-empty hakutoiveet)
      (->> hakutoiveet
           (filter #(and (not (and jyemp? (:jos-ylioppilastutkinto-ei-muita-pohjakoulutusliitepyyntoja? %)))
                         (every? (fn [belongs-to]
                                   (or (empty? belongs-to)
                                       (contains? belongs-to (:oid %))
                                       (intersect? belongs-to (set (:hakukohderyhmat %)))))
                                 belongs-tos)))
           (map :oid))
      ["form"])))

(defn- create-attachment-reviews
  [attachment-field answer old-answer update? application-key hakutoiveet fields-by-id ylioppilastutkinto? excluded-attachment-ids-when-yo-and-jyemp]
  (let [value-changed? (and update?
                            (not= old-answer answer))
        review-base    {:application_key application-key
                        :attachment_key  (:id attachment-field)
                        :state           (if (or (empty? answer)
                                                 (and (or (vector? (first answer)) (nil? (first answer)))
                                                      (every? empty? answer)))
                                           "attachment-missing"
                                           "not-checked")
                        :updated?        value-changed?}]
    (map #(assoc review-base :hakukohde %)
         (hakukohde-oids-for-attachment-review attachment-field hakutoiveet fields-by-id ylioppilastutkinto? excluded-attachment-ids-when-yo-and-jyemp))))

(defn- followup-option-selected?
  [field parent-field fields answers]
  (let [parent-value (get-in answers [(keyword (:id parent-field)) :value])]
    (cond
      (ktm/is-tutkinto-configuration-component? parent-field)
      (tutkinto-util/tutkinto-option-selected field fields answers)
      :else
      (->>  (:options parent-field)
            (filter (visibility-checker parent-field parent-value))
            (some #(= (:option-value field) (:value %)))
            boolean))))

(defn filter-visible-attachments
  [answers fields fields-by-id]
  (filter (fn [field]
            (and (= "attachment" (:fieldType field))
                 (-> field :params :hidden not)
                 (-> field :hidden not)
                 (loop [followup field]
                   (cond (:children-of followup)
                         (recur (get fields-by-id (keyword (:children-of followup))))
                         (:followup-of followup)
                         (followup-option-selected? followup
                                                    (get fields-by-id (keyword (:followup-of followup)))
                                                    fields
                                                    answers)
                         :else
                         true))))
          fields))

(defn- ylioppilastutkinto? [answers-by-key]
  (boolean (some #(or (= "pohjakoulutus_yo" %)
                      (= "pohjakoulutus_yo_ammatillinen" %)
                      (= "pohjakoulutus_yo_kansainvalinen_suomessa" %)
                      (= "pohjakoulutus_yo_ulkomainen" %))
                 (get-in answers-by-key [:higher-completed-base-education :value]))))

(defn create-application-attachment-reviews
  [application-key visible-attachments answers-by-key old-answers applied-hakukohteet update? fields-by-id excluded-attachment-ids-when-yo-and-jyemp]
  (let [ylioppilastutkinto? (ylioppilastutkinto? answers-by-key)]
    (mapcat (fn [attachment]
              (let [attachment-key (-> attachment :id keyword)
                    answer         (-> answers-by-key attachment-key :value)
                    old-answer     (-> old-answers attachment-key :value)]
                (create-attachment-reviews attachment
                                           answer
                                           old-answer
                                           update?
                                           application-key
                                           applied-hakukohteet
                                           fields-by-id
                                           ylioppilastutkinto?
                                           excluded-attachment-ids-when-yo-and-jyemp)))
            visible-attachments)))

(defn delete-orphan-attachment-reviews
  [application-key reviews connection]
  (queries/yesql-delete-application-attachment-reviews!
   {:application_key                            application-key
    :attachment_key_and_applied_hakukohde_array (->> reviews
                                                     (map (fn [review]
                                                            [(:attachment_key review) (:hakukohde review)]))
                                                     (json/generate-string))}
   connection))

(defn store-reviews [reviews connection]
  (doseq [review reviews]
    ((if (:updated? review)
       queries/yesql-update-attachment-hakukohde-review!
       queries/yesql-save-attachment-review!)
     (dissoc review :updated?) connection)))

(defn- create-attachment-hakukohde-reviews-for-application
  [application applied-hakukohteet old-answers form update? connection]
  (let [flat-form-content                         (-> form :content util/flatten-form-fields)
        fields-by-id                              (util/form-fields-by-id form)
        excluded-attachment-ids-when-yo-and-jyemp (higher-module/non-yo-attachment-ids form)
        answers-by-key                            (-> application :content :answers util/answers-by-key)
        visible-attachments                       (filter-visible-attachments answers-by-key flat-form-content fields-by-id)
        reviews                                   (create-application-attachment-reviews
                             (:key application)
                             visible-attachments
                             answers-by-key
                             old-answers
                             applied-hakukohteet
                             update?
                             fields-by-id
                             excluded-attachment-ids-when-yo-and-jyemp)]
    (store-reviews reviews connection)
    (when update?
      (delete-orphan-attachment-reviews (:key application)
                                        reviews
                                        connection))))

(defn- add-new-application-version
  "Add application and also initial metadata (event for receiving application, and initial review record)"
  [application create-new-secret? applied-hakukohteet old-answers form update? conn oppija-session]
  (let [connection           {:connection conn}
        answers              (:answers application)
        application-to-store {:form_id          (:form application)
                              :key              (:key application)
                              :lang             (:lang application)
                              :preferred_name   (find-value-from-answers "preferred-name" answers)
                              :last_name        (find-value-from-answers "last-name" answers)
                              :ssn              (find-value-from-answers "ssn" answers)
                              :dob              (dob/str->dob (find-value-from-answers "birth-date" answers))
                              :email            (find-value-from-answers "email" answers)
                              :hakukohde        (or (:hakukohde application) [])
                              :haku             (:haku application)
                              :content          {:answers answers}
                              :person_oid       (:person-oid application)
                              :tunnistautuminen {:session oppija-session}} ;todo, how does session info work for application edits? Currently, the info is simply copied from the previous version in sql.
        new-application      (if (contains? application :key)
                               (queries/yesql-add-application-version<! application-to-store connection)
                               (queries/yesql-add-application<! application-to-store connection))
        add-answers-args     {:application_id (:id new-application)
                              :answers        (doto (new PGobject)
                                                (.setType "jsonb")
                                                (.setValue (json/generate-string answers)))}]
    (queries/yesql-add-application-answers! add-answers-args connection)
    (queries/yesql-add-application-multi-answers! add-answers-args connection)
    (queries/yesql-add-application-multi-answer-values! add-answers-args connection)
    (queries/yesql-add-application-group-answers! add-answers-args connection)
    (queries/yesql-add-application-group-answer-groups! add-answers-args connection)
    (queries/yesql-add-application-group-answer-values! add-answers-args connection)
    (create-attachment-hakukohde-reviews-for-application new-application applied-hakukohteet old-answers form update? {:connection conn})
    (when create-new-secret?
      (add-new-secret-to-application-in-tx conn (:key new-application)))
    (unwrap-application new-application)))

(defn- get-latest-version-and-lock-for-update [secret conn]
  (if-let [application (first (queries/yesql-get-latest-version-by-secret-lock-for-update {:secret secret} {:connection conn}))]
    (unwrap-application application)
    (throw (ex-info "No existing form found when updating" {:secret secret}))))

(defn- get-latest-version-for-virkailija-edit-and-lock-for-update [virkailija-secret conn]
  (if-let [application (first (queries/yesql-get-latest-version-by-virkailija-secret-lock-for-update {:virkailija_secret virkailija-secret} {:connection conn}))]
    (unwrap-application application)
    (throw (ex-info "No existing form found when updating as virkailija" {:virkailija-secret virkailija-secret}))))

(defn- get-latest-version-for-virkailija-edit-and-lock-for-rewrite [virkailija-secret conn]
  (if-let [application (first (queries/yesql-get-latest-version-by-virkailija-secret-lock-for-rewrite {:virkailija_secret virkailija-secret} {:connection conn}))]
    (unwrap-application application)
    (throw (ex-info "No existing form found when rewriting as virkailija" {:virkailija-secret virkailija-secret}))))

(defn- get-virkailija-oid-for-update-secret
  [conn secret]
  (->> (jdbc/query conn ["SELECT virkailija_oid
                          FROM virkailija_update_secrets
                          WHERE secret = ?"
                         secret])
       first
       :virkailija_oid))

(defn- get-virkailija-oid-for-rewrite-secret
  [conn secret]
  (->> (jdbc/query conn ["SELECT virkailija_oid
                          FROM virkailija_rewrite_secrets
                          WHERE secret = ?"
                         secret])
       first
       :virkailija_oid))

(defn- get-virkailija-oid-for-create-secret
  [conn secret]
  (->> (jdbc/query conn ["SELECT virkailija_oid
                          FROM virkailija_create_secrets
                          WHERE secret = ?"
                         secret])
       first
       :virkailija_oid))

(defn- trim-and-remove-null-bytes-from-value
  [value]
  (cond (string? value)
        (clojure.string/replace (clojure.string/trim value) "\u0000" "")
        (sequential? value)
        (mapv trim-and-remove-null-bytes-from-value value)
        :else
        value))

(defn- remove-null-bytes-from-answer
  [answer]
  (update answer :value trim-and-remove-null-bytes-from-value))

(defn add-application [new-application applied-hakukohteet form session audit-logger oppija-session]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [selection-id                         (:selection-id new-application)
          virkailija-oid                       (when-let [secret (:virkailija-secret new-application)]
                           (get-virkailija-oid-for-create-secret conn secret))
          {:keys [id key] :as new-application} (add-new-application-version
                                                (update new-application :answers (partial mapv remove-null-bytes-from-answer))
                                                true
                                                applied-hakukohteet
                                                nil
                                                form
                                                false
                                                conn
                                                oppija-session)
          connection                           {:connection conn}]
      (audit-log/log audit-logger
                     {:new       new-application
                      :operation audit-log/operation-new
                      :session   session
                      :id        {:email (util/extract-email new-application)}})
      (queries/yesql-add-application-event<! {:application_key          key
                                              :event_type               (if (some? virkailija-oid)
                                                                          "received-from-virkailija"
                                                                          "received-from-applicant")
                                              :new_review_state         nil
                                              :virkailija_oid           virkailija-oid
                                              :virkailija_organizations nil
                                              :hakukohde                nil
                                              :review_key               nil}
                                             connection)
      (queries/yesql-add-application-review! {:application_key key
                                              :state           application-review-states/initial-application-review-state}
                                             connection)

      (selection-limit/permanent-select-on-store-application key new-application selection-id form connection)

      {:id id :key key})))

(defn- form->form-id [{:keys [form] :as application}]
  (assoc (dissoc application :form) :form-id form))

(defn- application->loggable-form [{:keys [form] :as application}]
  (cond-> application
          (some? form)
          (form->form-id)))

(defn- merge-applications [new-application old-application]
  (merge new-application
         (select-keys old-application [:key :haku :person-oid :secret])))

(defn- not-blank? [x]
  (not (clojure.string/blank? x)))

(defn update-application [{:keys [secret virkailija-secret selection-id] :as new-application} applied-hakukohteet form session audit-logger oppija-session]
  {:pre [(or (not-blank? secret)
             (not-blank? virkailija-secret))]}
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [updated-by-applicant?            (not-blank? secret)
          [virkailija-oid rewrite-secret?] (if updated-by-applicant?
                                             [nil nil]
                                             (if-let [oid (get-virkailija-oid-for-rewrite-secret conn virkailija-secret)]
                                               [oid true]
                                               [(get-virkailija-oid-for-update-secret conn virkailija-secret) false]))
          old-application                  (cond
                                             rewrite-secret?
                                             (get-latest-version-for-virkailija-edit-and-lock-for-rewrite virkailija-secret conn)

                                             updated-by-applicant?
                                             (get-latest-version-and-lock-for-update secret conn)

                                             :else
                                             (get-latest-version-for-virkailija-edit-and-lock-for-update virkailija-secret conn))
          {:keys [id key] :as new-application} (add-new-application-version
                                                (merge-applications new-application old-application)
                                                updated-by-applicant?
                                                applied-hakukohteet
                                                (-> old-application :answers util/answers-by-key)
                                                form
                                                true
                                                conn
                                                oppija-session)]
      (log/info (str "Updating application with key "
                     (:key old-application)
                     " based on valid application secret, retaining key" (when-not updated-by-applicant? " and secret") " from previous version"))
      (queries/yesql-add-application-event<! {:application_key          key
                                              :event_type               (if updated-by-applicant?
                                                                          "updated-by-applicant"
                                                                          "updated-by-virkailija")
                                              :new_review_state         nil
                                              :virkailija_oid           virkailija-oid
                                              :virkailija_organizations nil
                                              :hakukohde                nil
                                              :review_key               nil}
                                             {:connection conn})

      (selection-limit/permanent-select-on-store-application key new-application selection-id form {:connection conn})

      (audit-log/log audit-logger
                     {:new       (application->loggable-form new-application)
                      :old       (application->loggable-form old-application)
                      :operation audit-log/operation-modify
                      :session   (if updated-by-applicant?
                                   session
                                   (assoc-in session [:identity :oid] virkailija-oid))
                      :id        {:applicationOid key}})
      {:id id :key key})))

(defn- str->name-query-value
  [name]
  (->> (-> name
           (clojure.string/replace #"[&\|!<>:*]" "")
           (clojure.string/split #"\s+"))
       (remove clojure.string/blank?)
       (map #(str % ":*"))
       (clojure.string/join " & ")))

(defn- to-sql-array [s connection type]
  (.createArrayOf (:connection connection) type (to-array s)))

(defn- query->ensisijainen-hakukohde-snip
  [connection query]
  (queries/ensisijainen-hakukohde-snip
   (cond-> {:ensisijainen-hakukohde
            (to-sql-array (:ensisijainen-hakukohde query) connection "varchar")}
           (seq (:ensisijaisesti-hakukohteissa query))
           (assoc :ensisijaisesti-hakukohteissa
                  (to-sql-array (:ensisijaisesti-hakukohteissa query) connection "varchar")))))

(defn- query->attachment-snip
  [connection query]
  (let [[field-key states] (first (:attachment-review-states query))]
    (queries/attachment-snip
     (cond-> {:attachment-key field-key}

             (seq states)
             (assoc :states (to-sql-array states connection "varchar"))

             (or (seq (:ensisijainen-hakukohde query))
                 (seq (:hakukohde query)))
             (assoc :hakukohde (to-sql-array
                                (or (seq (:ensisijainen-hakukohde query))
                                    (seq (:hakukohde query)))
                                connection
                                "varchar"))))))

(defn- query->edited-hakutoiveet-snip
  [query]
  (let [only-edited? (:edited-hakutoiveet query)]
    (queries/edited-hakutoiveet-snip
      (when only-edited? {:only-edited true}))))

(defn- query->option-answers-snip
  [connection query]
  (let [option-answers (first (:option-answers query))]
    (queries/option-answers-snip
     (cond-> {:key (:key option-answers)}

             (true? (:use-original-question option-answers))
             (assoc :original-question true)

             (true? (:use-original-followup option-answers))
             (assoc :original-followup true)

             (some? (:hakukohde query))
             (assoc :hakukohde (to-sql-array (:hakukohde query) connection "varchar"))

             (seq (:options option-answers))
             (assoc :options (to-sql-array (:options option-answers) connection "varchar"))))))

(defn- sort->offset-snip
  [sort]
  (queries/offset-snip
   (merge {:order-by (:order-by sort)
           :order    (if (= "asc" (:order sort)) ">" "<")}
          (:offset sort))))

(defn- sort->order-by-snip
  [sort]
  (queries/order-by-snip
   {:order-by (:order-by sort)
    :order    (if (= "asc" (:order sort)) "ASC" "DESC")}))

(defn query->db-query
  [connection query sort]
  (let [query (cond-> (assoc query :order-by-snip (sort->order-by-snip sort))

                      (contains? query :name)
                      (update :name str->name-query-value)

                      (seq (:hakukohde query))
                      (update :hakukohde to-sql-array connection "varchar")

                      (seq (:ensisijainen-hakukohde query))
                      (assoc :ensisijainen-hakukohde-snip (query->ensisijainen-hakukohde-snip connection query))

                      (contains? query :attachment-review-states)
                      (assoc :attachment-snip (query->attachment-snip connection query))

                      (contains? query :edited-hakutoiveet)
                      (assoc :edited-hakutoiveet-snip (query->edited-hakutoiveet-snip query))

                      (contains? query :option-answers)
                      (assoc :option-answers-snip (query->option-answers-snip connection query))

                      (contains? sort :offset)
                      (assoc :offset-snip (sort->offset-snip sort)))]
    (queries/get-application-list-sqlvec query)))

(defn get-application-content-form-list
  [application-ids]
  (exec-db :db queries/yesql-get-application-content-form-list-by-ids {:ids application-ids}))

(defn get-application-heading-list
  [query sort]
  (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
    (let [db-query (query->db-query connection query sort)]
      (try
        (jdbc/query connection db-query)
        (catch Exception e
          (log/error e "Virhe suoritettaessa tietokantakyselyä '" db-query "'")
          (throw e))))))

(defn get-full-application-list-by-person-oid-for-omatsivut-and-refresh-old-secrets
  [person-oid]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (->> (queries/yesql-get-application-list-by-person-oid-for-omatsivut
          {:person_oid             person-oid
           :secret_link_valid_days (-> config :public-config :secret-link-valid-days)}
          {:connection conn})
         ->kebab-case-kw
         (mapv #(if (nil? (:secret %))
                  (do (log/info "Refreshing secret for application" (:key %))
                      (assoc % :secret (add-new-secret-to-application-in-tx conn (:key %))))
                  %)))))

(defn get-applications-persons-and-hakukohteet
  [haku]
  (exec-db :db queries/yesql-applications-person-and-hakukohteet-by-haku {:haku haku}))

(defn get-ensisijainen-applications-counts-for-haku
  [haku-oid]
  (exec-db :db queries/yesql-get-ensisijaisesti-hakeneet-counts {:haku_oid haku-oid}))

(defn- unwrap-onr-application
  [{:keys [key haku form email content person_oid]}]
  (let [answers (answers-by-key (:answers content))]
    {:oid          key
     :henkiloOid   person_oid
     :haku         haku
     :form         form
     :kansalaisuus (-> answers :nationality :value flatten)
     :aidinkieli   (-> answers :language :value)
     :matkapuhelin (-> answers :phone :value)
     :email        email
     :lahiosoite   (-> answers :address :value)
     :postinumero  (-> answers :postal-code :value)
     :passinNumero (-> answers :passport-number :value)
     :idTunnus     (-> answers :national-id-number :value)}))

(defn onr-applications [person-oids]
  (->> (exec-db :db queries/yesql-onr-applications {:person_oids person-oids})
       (map unwrap-onr-application)))

(defn has-ssn-applied [haku-oid ssn]
  (->> (exec-db :db queries/yesql-has-ssn-applied {:haku_oid haku-oid
                                                   :ssn      ssn})
       first
       ->kebab-case-kw))

(defn has-eidas-applied [haku-oid eidas-id]
  (->> (exec-db :db queries/yesql-has-eidas-applied {:haku_oid haku-oid
                                                     :eidas_id eidas-id})
       first
       ->kebab-case-kw))

(defn has-email-applied [haku-oid email]
  (->> (exec-db :db queries/yesql-has-email-applied {:haku_oid haku-oid
                                                     :email    email})
       first
       ->kebab-case-kw))

(defn get-application-review-notes [application-key]
  (->> (exec-db :db queries/yesql-get-application-review-notes {:application_key application-key})
       (map ->kebab-case-kw)
       (map util/remove-nil-values)))

(defn get-application-review [application-key]
  (->> (exec-db :db queries/yesql-get-application-review {:application_key application-key})
       (map ->kebab-case-kw)
       (first)))

(defn get-application-reviews-by-keys [application-keys]
  (map ->kebab-case-kw
       (exec-db :db queries/yesql-get-application-reviews-by-keys {:application_keys application-keys})))

(defn get-application-review-notes-by-keys [application-keys]
  (map ->kebab-case-kw
       (exec-db :db queries/yesql-get-application-review-notes-by-keys {:application_keys application-keys})))

(defn get-application [application-id]
  (unwrap-application (first (exec-db :db queries/yesql-get-application-by-id {:application_id application-id}))))

(defn get-not-inactivated-application [application-id]
  (unwrap-application (first (exec-db :db queries/yesql-get-not-inactivated-application-by-id {:application_id application-id}))))

(defn get-latest-application-by-key-in-tx
  [connection application-key]
  (-> (queries/yesql-get-latest-application-by-key
       {:application_key application-key}
       {:connection connection})
      first
      unwrap-application))

(defn get-latest-application-by-key [application-key]
  (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
    (dissoc (get-latest-application-by-key-in-tx connection application-key)
            :secret
            :application-hakukohde-reviews)))


(defn get-latest-application-by-key-for-odw [application-key]
  (exec-db :db queries/yesql-get-single-odw-application-by-key {:key application-key}))

(defn get-latest-applications-by-haku [haku-oid limit offset]
  (exec-db :db queries/yesql-get-applications-by-haku {:haku haku-oid :limit limit :offset (or offset 0)}))

(defn post-process-application-attachments [koodisto-cache
                                            tarjonta-service
                                            organization-service
                                            ohjausparametrit-service
                                            application-key
                                            audit-logger
                                            session]
  (jdbc/with-db-transaction
    [conn {:datasource (db/get-datasource :db)}]
    (let [application (get-latest-application-by-key-in-tx conn application-key)
          answers              (:answers application)
          application-to-store {:form_id        (:form application)
                                :key            (:key application)
                                :lang           (:lang application)
                                :preferred_name (find-value-from-answers "preferred-name" answers)
                                :last_name      (find-value-from-answers "last-name" answers)
                                :ssn            (find-value-from-answers "ssn" answers)
                                :dob            (dob/str->dob (find-value-from-answers "birth-date" answers))
                                :email          (find-value-from-answers "email" answers)
                                :hakukohde      (or (:hakukohde application) [])
                                :haku           (:haku application)
                                :content        {:answers answers}
                                :person_oid     (:person-oid application)}
          old-answers         nil
          update?             false
          form                (forms/get-form-by-application application-to-store)
          tarjonta-info       (when (:haku application)
                                (tarjonta-parser/parse-tarjonta-info-by-haku
                                  koodisto-cache
                                  tarjonta-service
                                  organization-service
                                  ohjausparametrit-service
                                  (:haku application)))
          hakukohteet         (get-in tarjonta-info [:tarjonta :hakukohteet])
          applied-hakukohteet (filter #(contains? (set (:hakukohde application)) (:oid %)) hakukohteet)]
      (audit-log/log audit-logger
                     {:new       application
                      :operation audit-log/operation-modify
                      :session   session
                      :id        {:email (util/extract-email application)}})
      (create-attachment-hakukohde-reviews-for-application
        application-to-store applied-hakukohteet old-answers form update? {:connection conn}))))

(defn get-application-hakukohde-reviews
  [application-key]
  (mapv ->kebab-case-kw (exec-db :db queries/yesql-get-application-hakukohde-reviews {:application_key application-key})))

(defn get-application-attachment-reviews
  [application-key]
  (mapv ->kebab-case-kw (exec-db :db queries/yesql-get-application-attachment-reviews {:application_key application-key})))

(defn get-latest-application-by-secret [secret]
  (when-let [application (->> (exec-db :db
                                       queries/yesql-get-latest-application-by-secret
                                       {:secret                 secret
                                        :secret_link_valid_days (-> config :public-config :secret-link-valid-days)})
                              (first)
                              (unwrap-application))]
    (-> application
        (assoc :state (-> (:key application) get-application-review :state))
        (assoc :application-hakukohde-reviews (get-application-hakukohde-reviews (:key application))))))

(defn get-latest-application-for-virkailija-edit [virkailija-secret]
  (when-let [application (->> (exec-db :db queries/yesql-get-latest-application-by-virkailija-secret {:virkailija_secret virkailija-secret})
                              (first)
                              (unwrap-application))]
    (assoc application :state (-> (:key application) get-application-review :state))))

(defn get-latest-application-for-virkailija-rewrite-edit [virkailija-secret]
  (when-let [application (->> (exec-db :db queries/yesql-get-latest-application-by-virkailija-rewrite-secret {:virkailija_secret virkailija-secret})
                              (first)
                              (unwrap-application))]
    (assoc application :state (-> (:key application) get-application-review :state))))

(defn get-latest-version-of-application-for-edit
  [rewrite? {:keys [secret virkailija-secret]}]
  (cond
    secret
    (get-latest-application-by-secret secret)
    rewrite?
    (get-latest-application-for-virkailija-rewrite-edit virkailija-secret)
    :else
    (get-latest-application-for-virkailija-edit virkailija-secret)))

(defn get-latest-application-secret
  []
  (:secret (first (->> (exec-db :db queries/yesql-get-latest-application-secret {})))))

(defn alter-application-hakukohteet-with-secret
  [secret hakukohteet answers]
  (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
    (queries/yesql-set-application-hakukohteet-by-secret!
     {:secret    secret
      :hakukohde hakukohteet
      :content   {:answers answers}}
     {:connection connection})
    (queries/yesql-delete-application-hakukohteet-answer-values-by-secret!
     {:secret secret}
     {:connection connection})
    (queries/yesql-insert-application-hakukohteet-answer-values-by-secret!
     {:secret      secret
      :hakukohteet (doto (new PGobject)
                     (.setType "jsonb")
                     (.setValue (json/generate-string hakukohteet)))}
     {:connection connection})))

(defn add-new-secret-to-application
  [application-key]
  (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
    (add-new-secret-to-application-in-tx connection application-key)))

(defn add-new-secret-to-application-by-old-secret
  [old-secret]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [connection      {:connection conn}
          application-key (-> (queries/yesql-get-application-key-for-any-version-of-secret {:secret old-secret} connection)
                              (first)
                              :application_key)
          application     (-> (queries/yesql-get-latest-application-by-key
                               {:application_key application-key}
                               connection)
                              (first)
                              (unwrap-application))]
      (add-new-secret-to-application-in-tx conn application-key)
      (:id application))))

(defn get-application-events [application-key]
  (->> (exec-db :db queries/yesql-get-application-events {:application_key application-key})
       (mapv ->kebab-case-kw)
       (mapv #(if (nil? (:virkailija-organizations %))
                (dissoc % :virkailija-organizations)
                %))))

(defn- auditlog-review-modify
  [review old-value session audit-logger]
  (audit-log/log audit-logger
                 {:new       review
                  :old       old-value
                  :id        {:applicationOid (:application_key review)
                              :hakukohdeOid   (:hakukohde review)
                              :requirement    (:requirement review)}
                  :session   session
                  :operation audit-log/operation-modify}))

(defn- edit-application-right-organizations->json [session]
  (->> (-> session :identity :user-right-organizations :edit-applications)
       (map :oid)
       (json/generate-string)))

(defn save-application-review [review session audit-logger]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [connection      {:connection conn}
          app-key         (:application-key review)
          old-review      (when-let [data (first (queries/yesql-get-application-review {:application_key app-key} connection))]
                            {:application_key (:application_key data)
                             :state           (:state data)
                             :score           (some-> (:score data) (.doubleValue))})
          review-to-store {:application_key (:application-key review)
                           :state           (:state review)
                           :score           (:score review)}]
      (when (not= old-review review-to-store)
        (queries/yesql-save-application-review! review-to-store connection)
        (auditlog-review-modify review-to-store old-review session audit-logger))
      (when (not= (:state old-review) (:state review-to-store))
        (let [event {:application_key          app-key
                     :event_type               "review-state-change"
                     :new_review_state         (:state review-to-store)
                     :virkailija_oid           (-> session :identity :oid)
                     :virkailija_organizations (edit-application-right-organizations->json session)
                     :hakukohde                nil
                     :review_key               nil}]
          (:id (queries/yesql-add-application-event<! event connection)))))))

(defn save-application-hakukohde-review
  [application-key hakukohde-oid hakukohde-review-requirement hakukohde-review-state session audit-logger]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [connection                  {:connection conn}
          review-to-store             {:application_key application-key
                                       :requirement     hakukohde-review-requirement
                                       :state           hakukohde-review-state
                                       :hakukohde       hakukohde-oid}
          existing-duplicate-review   (queries/yesql-get-existing-application-hakukohde-review review-to-store connection)
          existing-requirement-review (queries/yesql-get-existing-requirement-review review-to-store connection)]
      (when (empty? existing-duplicate-review)
        (auditlog-review-modify review-to-store (first existing-requirement-review) session audit-logger)
        (queries/yesql-upsert-application-hakukohde-review! review-to-store connection)
        (let [event {:application_key          application-key
                     :event_type               "hakukohde-review-state-change"
                     :new_review_state         (:state review-to-store)
                     :review_key               hakukohde-review-requirement
                     :hakukohde                (:hakukohde review-to-store)
                     :virkailija_oid           (-> session :identity :oid)
                     :virkailija_organizations (edit-application-right-organizations->json session)}]
          (queries/yesql-add-application-event<! event connection))))))

(defn save-payment-obligation-automatically-changed
  "Only used by automatic tuition fee obligation logic. Sets new obligation state if previous one was not set by
   virkailija and the state transition is allowed. If successful, adds an event marking the obligation was set automatically."
  [application-key hakukohde-oid hakukohde-review-requirement hakukohde-review-state]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [connection                  {:connection conn}
          review-to-store             {:application_key application-key
                                       :requirement     hakukohde-review-requirement
                                       :state           hakukohde-review-state
                                       :hakukohde       hakukohde-oid}
          automatically-changed?      (->> (queries/yesql-get-application-events {:application_key application-key} connection)
                                           (filter #(and (= hakukohde-oid (:hakukohde %))
                                                         (= hakukohde-review-requirement (:review_key %))))
                                           last
                                           :event_type
                                           (= "payment-obligation-automatically-changed"))
          existing-requirement-review (first (queries/yesql-get-existing-requirement-review review-to-store connection))]
      ; Main idea:
      ; - When the state is still unreviewed, it can be always automatically changed.
      ; - When virkailija has made the latest modification to tuition fee obligation (not automatically-changed?)
      ;   we should never override that state automatically anymore.
      ; - Once the state is automatically set as obligated, don't modify further without virkailija input.
      (when (or (and (= "not-obligated" (:state review-to-store))
                     (= "unreviewed" (:state existing-requirement-review "unreviewed")))
                (and (= "unreviewed" (:state review-to-store))
                     (= "not-obligated" (:state existing-requirement-review))
                     automatically-changed?)
                (and (= "obligated" (:state review-to-store))
                     (= "unreviewed" (:state existing-requirement-review "unreviewed")))
                (and (= "obligated" (:state review-to-store))
                     (= "not-obligated" (:state existing-requirement-review))
                     automatically-changed?))
        ; Whenever we set the obligation automatically, also add an event for auditing and possible further state changes.
        (queries/yesql-upsert-application-hakukohde-review! review-to-store connection)
        (let [event {:application_key          application-key
                     :event_type               "payment-obligation-automatically-changed"
                     :new_review_state         (:state review-to-store)
                     :review_key               hakukohde-review-requirement
                     :hakukohde                (:hakukohde review-to-store)
                     :virkailija_oid           nil
                     :virkailija_organizations nil}]
          (queries/yesql-add-application-event<! event connection))))))

(defn save-attachment-hakukohde-review
  [application-key hakukohde-oid attachment-key hakukohde-review-state session audit-logger]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [connection      {:connection conn}
          review-to-store {:application_key application-key
                           :attachment_key  attachment-key
                           :state           hakukohde-review-state
                           :hakukohde       hakukohde-oid}]
      (if-let [existing-attachment-review (first (queries/yesql-get-existing-attachment-review review-to-store connection))]
        (when-not (= hakukohde-review-state (:state existing-attachment-review))
          (auditlog-review-modify review-to-store existing-attachment-review session audit-logger)
          (queries/yesql-update-attachment-hakukohde-review! review-to-store connection)
          (let [event {:application_key          application-key
                       :event_type               "attachment-review-state-change"
                       :new_review_state         (:state review-to-store)
                       :review_key               attachment-key
                       :hakukohde                (:hakukohde review-to-store)
                       :virkailija_oid           (-> session :identity :oid)
                       :virkailija_organizations (edit-application-right-organizations->json session)}]
            (queries/yesql-add-application-event<! event connection)))
        (throw (new IllegalStateException (str "No existing attahcment review found for " review-to-store)))))))

(defn get-applications-by-keys
  [application-keys]
  (mapv unwrap-application
        (exec-db :db queries/yesql-get-applications-by-keys {:application_keys application-keys})))

(defn add-person-oid
  "Add person OID to an application"
  [application-id person-oid]
  (exec-db :db queries/yesql-add-person-oid!
           {:id application-id :person_oid person-oid}))

(defn get-haut
  []
  (mapv ->kebab-case-kw (exec-db :db queries/yesql-get-haut-and-hakukohteet-from-applications {})))

(defn get-direct-form-haut
  []
  (mapv ->kebab-case-kw (exec-db :db queries/yesql-get-direct-form-haut {})))

(defn add-application-feedback
  [feedback]
  (->kebab-case-kw
   (exec-db :db queries/yesql-add-application-feedback<! (transform-keys ->snake_case feedback))))

(defn- kk-base-educations-old-module [answers]
  (->> [["kk" :higher-education-qualification-in-finland-year-and-date]
        ["avoin" :studies-required-by-higher-education-field]
        ["ulk" :higher-education-qualification-outside-finland-year-and-date]
        ["muu" :other-eligibility-year-of-completion]]
       (remove (fn [[_ id]]
                 (or (not (sequential? (-> answers id :value)))
                     (not (sequential? (-> answers id :value first)))
                     (clojure.string/blank? (-> answers id :value first first)))))
       (map first)))

(defn- kk-base-educations-old-module-pohjakoulutuskklomake [answers]
  (->> [["pohjakoulutus_kk" :higher-education-qualification-in-finland-year-and-date]
        ["pohjakoulutus_avoin" :studies-required-by-higher-education-field]
        ["pohjakoulutus_ulk" :higher-education-qualification-outside-finland-year-and-date]
        ["pohjakoulutus_muu" :other-eligibility-year-of-completion]]
       (remove (fn [[_ id]]
                 (or (not (sequential? (-> answers id :value)))
                     (not (sequential? (-> answers id :value first)))
                     (clojure.string/blank? (-> answers id :value first first)))))
       (map first)))

(defn- kk-base-educations-pohjakoulutuskklomake [answers]
  (distinct (concat (-> answers :higher-completed-base-education :value)
                    (kk-base-educations-old-module-pohjakoulutuskklomake answers))))

(defn- kk-base-educations-new-module [answers]
  (let [m {"pohjakoulutus_yo"              ["yo"]
           "pohjakoulutus_yo_ammatillinen" ["yo" "am"]
           "pohjakoulutus_am"              ["am"]
           "pohjakoulutus_amp"             ["am"]
           "pohjakoulutus_amv"             ["am"]
           "pohjakoulutus_amt"             ["amt"]
           "pohjakoulutus_kk"              ["kk"]
           "pohjakoulutus_avoin"           ["avoin"]
           "pohjakoulutus_ulk"             ["ulk"]
           "pohjakoulutus_muu"             ["muu"]}]
    (->> (-> answers :higher-completed-base-education :value)
         (mapcat m)
         distinct)))

(defn- kk-base-educations [answers]
  (distinct (concat (kk-base-educations-old-module answers)
                    (kk-base-educations-new-module answers))))

(defn- korkeakoulututkinto-vuosi [answers]
  (cond (or (= "Yes" (get-in answers [:finnish-vocational-before-1995 :value] "No"))
            ;; kevään 2019 kk yhteishaun lomakkeella vastauksien tunnisteina "0" ja "1"
            (= "0" (get-in answers [:finnish-vocational-before-1995 :value] "1")))
        (Integer/valueOf (get-in answers [:finnish-vocational-before-1995--year-of-completion :value]))
        ;; syksyn 2018 kk yhteishaun lomakkeella kysymyksellä on satunnainen tunniste
        (= "0" (get-in answers [:2bfb9ea5-3896-4d82-9966-a03d418012fb :value]))
        (Integer/valueOf (get-in answers [:ea33f9b9-674c-4513-9b0c-93c22a24043e :value]))
        :else
        nil))

(def JodaFormatter (.withZone (org.joda.time.format.DateTimeFormat/forPattern "yyyy-MM-dd'T'HH:mm:ss")
                              (org.joda.time.DateTimeZone/forID "Europe/Helsinki")))

(def ZonedJodaFormatter (.withZone (org.joda.time.format.DateTimeFormat/forPattern "yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
                              (org.joda.time.DateTimeZone/forID "Europe/Helsinki")))

(defn- unwrap-hakurekisteri-application
  [{:keys [key haku hakukohde created_time submitted person_oid lang email content
           payment-obligations eligibilities attachment_reviews]}]
  (let [answers  (answers-by-key (:answers content))
        foreign? (not= finland-country-code (-> answers :country-of-residence :value))]
    {:oid                         key
     :personOid                   person_oid
     :createdTime                 (.print JodaFormatter created_time) ; viimeisimmän hakemusversion luontihetki
     :hakemusFirstSubmittedTime  (.print JodaFormatter submitted) ; ensimmäisen hakemusversion luontihetki
     :applicationSystemId         haku
     :kieli                       lang
     :hakukohteet                 hakukohde
     :email                       email
     :matkapuhelin                (-> answers :phone :value)
     :lahiosoite                  (-> answers :address :value)
     :postinumero                 (-> answers :postal-code :value)
     :postitoimipaikka            (if foreign?
                                    (-> answers :city :value)
                                    (-> answers :postal-office :value))
     ;; Default asuinmaa to finland for forms that are from before
     ;; country-of-residence was implemented, or copied from those forms.
     :asuinmaa                    (or (-> answers :country-of-residence :value) "246")
     :kotikunta                   (-> answers :home-town :value)
     ; Newer Sure APIs use the unmapped kkPohjakoulutusLomake codes.
     ; Older ones still require mapped ones for compatibility.
     :kkPohjakoulutus             (kk-base-educations answers)
     :kkPohjakoulutusLomake       (kk-base-educations-pohjakoulutuskklomake answers)
     :sahkoisenAsioinninLupa      (= "Kyllä" (-> answers :sahkoisen-asioinnin-lupa :value))
     :valintatuloksenJulkaisulupa (= "Kyllä" (-> answers :valintatuloksen-julkaisulupa :value))
     :koulutusmarkkinointilupa    (= "Kyllä" (-> answers :koulutusmarkkinointilupa :value))
     :korkeakoulututkintoVuosi    (korkeakoulututkinto-vuosi answers)
     :paymentObligations          (reduce-kv #(assoc %1 (name %2) %3) {} payment-obligations)
     :attachments                 (reduce-kv #(assoc %1 (name %2) %3) {} attachment_reviews)
     :eligibilities               (reduce-kv #(assoc %1 (name %2) %3) {} eligibilities)}))

(defn- unwrap-hakurekisteri-person-info
  [{:keys [key person_oid ssn]}]
  {:oid       key
   :personOid person_oid
   :ssn       ssn})

(def urheilija-fields-with-single-key [:keskiarvo
                                       :peruskoulu
                                       :tamakausi
                                       :viimekausi
                                       :toissakausi
                                       :sivulaji
                                       :valmennusryhma_seurajoukkue
                                       :valmennusryhma_piirijoukkue
                                       :valmennusryhma_maajoukkue
                                       :valmentaja_nimi
                                       :valmentaja_email
                                       :valmentaja_puh
                                       :liitto
                                       :seura])

(def ^:private option-muu "21")

(defn- get-urheilija-laji [answers-by-key lang {:keys [laji-dropdown-key muu-laji-key value-to-label]}]
  (let [dropdown-answer (-> answers-by-key
                            laji-dropdown-key
                            :value)
        option-text (if (= dropdown-answer option-muu)
                      (-> answers-by-key
                          muu-laji-key
                          :value)
                      ((keyword lang) (get value-to-label dropdown-answer)))]
    {:laji option-text}))

;Valmentajan yhteystietokentissä vastaukset ovat arrayn sisällä, mutta niitä voi nykytilanteessa olla vain yksi.
(defn- to-single-value [value]
  (if (coll? value)
    (first value)
    value))

(defn- get-urheilijan-lisakysymykset [answers-by-key keys]
  (into {} (map (fn [field] {field (-> answers-by-key (get (-> keys field keyword)) :value to-single-value)}) urheilija-fields-with-single-key)))

(defn- unwrap-hakurekisteri-application-toinenaste
  [questions urheilija-amm-hakukohdes haun-hakukohteet {:keys [key hakukohde created_time submitted person_oid lang email content attachment_reviews]}]

  (try (let [answers (answers-by-key (:answers content))
             foreign? (not= finland-country-code (-> answers :country-of-residence :value))
             form-hakukohde-key (fn [id hakukohde-oid] (keyword (str id "_" hakukohde-oid)))
             sports-key (:urheilijan-amm-lisakysymys-key questions)
             interested-in-sports-amm? (when sports-key (-> answers sports-key :value))
             get-hakukohde-fn (fn [oid] (first (filter #(= oid (:oid %)) haun-hakukohteet)))
             hakukohteet (map (fn [oid]
                                {:oid                                               oid
                                 :harkinnanvaraisuus
                                 (get-harkinnanvaraisuus-reason-for-hakukohde answers (get-hakukohde-fn oid))
                                 :terveys                                           (= "1" (:value ((form-hakukohde-key (:sora-terveys-key questions) oid) answers)))
                                 :aiempiPeruminen                                   (= "1" (:value ((form-hakukohde-key (:sora-aiempi-key questions) oid) answers)))
                                 :kiinnostunutKaksoistutkinnosta                    (->> (:kaksoistutkinto-keys questions)
                                                                                         (map #(:value ((form-hakukohde-key % oid) answers)))
                                                                                         (some #(= "0" %)))
                                 :kiinnostunutUrheilijanAmmatillisestaKoulutuksesta (when (and interested-in-sports-amm?
                                                                                               (some #(= oid %) urheilija-amm-hakukohdes))
                                                                                      (= "0" interested-in-sports-amm?))})
                              hakukohde)
             first-huoltaja (when (or (-> answers :guardian-name :value)
                                      (-> answers :guardian-firstname :value)
                                      (-> answers :guardian-lastname :value)
                                      (-> answers :guardian-email :value)
                                      (-> answers :guardian-phone :value))
                              {:etunimi      (or (-> answers :guardian-firstname :value first)
                                                 (-> answers :guardian-name :value first))
                               :sukunimi     (-> answers :guardian-lastname :value first)
                               :matkapuhelin (-> answers :guardian-phone :value first)
                               :email        (-> answers :guardian-email :value first)})
             second-huoltaja (when (or (-> answers :guardian-name-secondary :value)
                                       (-> answers :guardian-firstname-secondary :value)
                                       (-> answers :guardian-lastname-secondary :value)
                                       (-> answers :guardian-email-secondary :value)
                                       (-> answers :guardian-phone-secondary :value))
                               {:etunimi      (or (-> answers :guardian-firstname-secondary :value first)
                                                  (-> answers :guardian-name-secondary :value first))
                                :sukunimi     (-> answers :guardian-lastname-secondary :value first)
                                :matkapuhelin (-> answers :guardian-phone-secondary :value first)
                                :email        (-> answers :guardian-email-secondary :value first)})
             huoltajat (->> []
                            (concat [first-huoltaja second-huoltaja])
                            (filter #(not (nil? %))))
             base-education-key (keyword base-education-choice-key)
             oppisopimuskoulutus-key (:oppisopimuskoulutus-key questions)
             tutkinto-vuosi-key (->> (:tutkintovuosi-keys questions)
                                     (filter #(not (nil? (% answers))))
                                     first)
             tutkinto-vuosi (when tutkinto-vuosi-key
                              (-> answers tutkinto-vuosi-key :value))
             tutkinto-kieli-key (->> (:tutkintokieli-keys questions)
                                     (filter #(not (nil? (% answers))))
                                     first)
             tutkinto-kieli (when tutkinto-kieli-key
                              (-> answers tutkinto-kieli-key :value (base-education-2nd-language-value-to-lang)))
             urheilija-laji (get-urheilija-laji answers lang (:urheilijan-lisakysymys-laji-key-and-mapping questions))
             urheilija-laji-ammatillinen (get-urheilija-laji answers lang (:urheilijan-ammatillinen-lisakysymys-laji-key-and-mapping questions))
             urheilijan-lisakysymykset (get-urheilijan-lisakysymykset answers (:urheilijan-lisakysymys-keys questions))
             urheilijan-lisakysymykset-ammatillinen (get-urheilijan-lisakysymykset answers (:urheilijan-amm-lisakysymys-keys questions))]
         {:oid                                  key
          :personOid                            person_oid
          :createdTime                          (.print JodaFormatter created_time) ;viimeisimmän hakemusversion luontihetki
          :hakemusFirstSubmittedTime            (.print JodaFormatter submitted) ;ensimmäisen hakemusversion luontihetki
          :kieli                                lang
          :hakukohteet                          hakukohteet
          :email                                email
          :matkapuhelin                         (-> answers :phone :value)
          :lahiosoite                           (-> answers :address :value)
          :postinumero                          (-> answers :postal-code :value)
          :postitoimipaikka                     (if foreign?
                                                  (-> answers :city :value)
                                                  (-> answers :postal-office :value))
          :asuinmaa                             (-> answers :country-of-residence :value)
          :kotikunta                            (-> answers :home-town :value)
          :sahkoisenAsioinninLupa               (= "Kyllä" (-> answers :paatos-opiskelijavalinnasta-sahkopostiin :value))
          :valintatuloksenJulkaisulupa          (= "Kyllä" (-> answers :valintatuloksen-julkaisulupa :value))
          :koulutusmarkkinointilupa             (= "Kyllä" (-> answers :koulutusmarkkinointilupa :value))
          :attachments                          (reduce-kv #(assoc %1 (name %2) %3) {} attachment_reviews)
          :huoltajat                            huoltajat
          :pohjakoulutus                        (or (-> answers base-education-key :value) "")
          :tutkintoKieli                        tutkinto-kieli
          :tutkintoVuosi                        (edn/read-string tutkinto-vuosi)
          :kiinnostunutOppisopimusKoulutuksesta (when oppisopimuskoulutus-key (= "0" (-> answers oppisopimuskoulutus-key :value)))
          :urheilijanLisakysymykset             (merge urheilijan-lisakysymykset urheilija-laji)
          :urheilijanLisakysymyksetAmmatillinen (merge urheilijan-lisakysymykset-ammatillinen urheilija-laji-ammatillinen)
          }) (catch Exception e
               (log/warn e "Exception while mapping suoritusrekisteri-application-toinenaste for application " key ". Exception: " e))))

(defn suoritusrekisteri-applications
  [haku-oid hakukohde-oids person-oids modified-after offset]
  (let [as (->> (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
                  (queries/yesql-suoritusrekisteri-applications
                   {:haku_oid       haku-oid
                    :hakukohde_oids (some->> (seq hakukohde-oids)
                                             to-array
                                             (.createArrayOf (:connection connection) "varchar"))
                    :person_oids    (some->> (seq person-oids)
                                             to-array
                                             (.createArrayOf (:connection connection) "text"))
                    :modified_after (some-> modified-after
                                            (LocalDateTime/parse (DateTimeFormatter/ofPattern "yyyyMMddHHmm"))
                                            (.atZone (ZoneId/of "Europe/Helsinki"))
                                            .toOffsetDateTime)
                    :offset         offset}
                   {:connection connection}))
                (map unwrap-hakurekisteri-application))]
    (merge {:applications as}
           (when-let [a (first (drop 999 as))]
             {:offset (:oid a)}))))

(defn suoritusrekisteri-person-info
  [haku-oid hakukohde-oids offset]
  (let [as (->> (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
                                         (queries/yesql-suoritusrekisteri-person-info
                                           {:haku_oid       haku-oid
                                            :hakukohde_oids (some->> (seq hakukohde-oids)
                                                                     to-array
                                                                     (.createArrayOf (:connection connection) "varchar"))
                                            :offset         offset}
                                           {:connection connection}))
                (map unwrap-hakurekisteri-person-info))]
    (merge {:applications as}
           (when-let [a (first (drop 199999 as))];todo, set a reasonable limit, in case 200000 is problematic somehow.
             {:offset (:oid a)}))))

(defn suoritusrekisteri-applications-toinenaste
  [haku-oid hakukohde-oids person-oids modified-after offset questions urheilija-amm-hakukohdes haun-hakukohteet]
   (let [as (->> (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
                                          (queries/yesql-suoritusrekisteri-applications
                                            {:haku_oid       haku-oid
                                             :hakukohde_oids (some->> (seq hakukohde-oids)
                                                                      to-array
                                                                      (.createArrayOf (:connection connection) "varchar"))
                                             :person_oids    (some->> (seq person-oids)
                                                                      to-array
                                                                      (.createArrayOf (:connection connection) "text"))
                                             :modified_after (some-> modified-after
                                                                     (LocalDateTime/parse (DateTimeFormatter/ofPattern "yyyyMMddHHmm"))
                                                                     (.atZone (ZoneId/of "Europe/Helsinki"))
                                                                     .toOffsetDateTime)
                                             :offset         offset}
                                            {:connection connection}))
                 (map #(unwrap-hakurekisteri-application-toinenaste questions urheilija-amm-hakukohdes haun-hakukohteet %))
                 (remove nil?))]
     (merge {:applications as}
            (when-let [a (first (drop 999 as))]
              {:offset (:oid a)}))))

(defn- requirement-names-mapped-to-states
  [requirements]
  (reduce (fn [acc requirement]
            (assoc acc (-> requirement :requirement keyword ->camelCase) (:state requirement)))
          {}
          requirements))

(defn- requirement-names-mapped-to-states-by-hakukohde
  [requirements-by-hakukohde]
  (reduce (fn [acc [hakukohde-oid requirements]]
            (assoc acc hakukohde-oid (requirement-names-mapped-to-states requirements)))
          {}
          requirements-by-hakukohde))

(defn- hakutoiveet-to-list
  [hakutoiveet]
  (map (fn [[hakukohde-oid requirements]]
         (merge (dissoc requirements :selectionState)
                {:hakukohdeOid hakukohde-oid}))
       hakutoiveet))

(defn- hakutoiveet-priority-order
  [hakukohteet-in-priority-order hakutoiveet]
  (map (fn [hakukohde-oid]
         (->> hakutoiveet
              (filter #(= hakukohde-oid (:hakukohdeOid %)))
              first))
       hakukohteet-in-priority-order))

(defn- unwrap-external-application-hakutoiveet
  [application]
  (->> (application-states/get-all-reviews-for-all-requirements
        (clojure.set/rename-keys
         application
         {:application_hakukohde_reviews :application-hakukohde-reviews}))
       (group-by :hakukohde)
       requirement-names-mapped-to-states-by-hakukohde
       hakutoiveet-to-list
       (hakutoiveet-priority-order (:hakukohde application))))

(defn- unwrap-external-application
  [{:keys [key haku organization_oid person_oid lang email content] :as application}]
  (let [answers (answers-by-key (:answers content))]
    {:oid              key
     :hakuOid          haku
     :organization-oid organization_oid
     :henkiloOid       person_oid
     :asiointikieli    lang
     :email            email
     :lahiosoite       (-> answers :address :value)
     :postinumero      (-> answers :postal-code :value)
     :postitoimipaikka (or (-> answers :postal-office :value)
                           (-> answers :city :value))
     :maa              (-> answers :country-of-residence :value)
     :hakutoiveet      (unwrap-external-application-hakutoiveet application)}))

(defn get-external-applications ;; deprecated, use valinta-tulos-service-applications
  [haku-oid hakukohde-oid hakemus-oids]
  (->> (exec-db :db
                queries/yesql-applications-by-haku-and-hakukohde-oids
                {:haku_oid       haku-oid
                 ;; Empty string to avoid empty parameter lists
                 :hakukohde_oids (cond-> [""]
                                         (some? hakukohde-oid)
                                         (conj hakukohde-oid))
                 :hakemus_oids   (cons "" hakemus-oids)})
       (map unwrap-external-application)))

(defn valinta-tulos-service-applications
  [haku-oid hakukohde-oid hakemus-oids offset]
  (let [as (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
             (map #(hash-map :oid (:oid %)
                             :hakuOid (:haku %)
                             :hakukohdeOids (:hakukohde %)
                             :henkiloOid (:person-oid %)
                             :asiointikieli (:asiointikieli %)
                             :email (:email %)
                             :paymentObligations (zipmap (vec (map (fn [x] (key x)) (:payment-obligations %)))
                                                         (vec (map (fn [x] (case (val x)
                                                                             "unreviewed" "NOT_CHECKED"
                                                                             "obligated" "REQUIRED"
                                                                             "not-obligated" "NOT_REQUIRED"))
                                                                   (:payment-obligations %)))))
                  (queries/yesql-valinta-tulos-service-applications
                    {:haku_oid      haku-oid
                     :hakukohde_oid hakukohde-oid
                     :hakemus_oids  (some->> (seq hakemus-oids)
                                             to-array
                                             (.createArrayOf (:connection connection) "text"))
                     :offset        offset}
                    {:connection connection})))]
    (merge {:applications as}
           (when-let [a (first (drop 4999 as))]
             {:offset (:oid a)}))))

(defn- convert-asiointikieli [kielikoodi]
  (cond
    (= "fi" kielikoodi) {:kieliKoodi "fi" :kieliTyyppi "suomi"}
    (= "sv" kielikoodi) {:kieliKoodi "sv" :kieliTyyppi "svenska"}
    (= "en" kielikoodi) {:kieliKoodi "en" :kieliTyyppi "english"}
    :else               {:kieliKoodi "" :kieliTyyppi ""}))

(defn- enrich-persons-from-onr [person-service applications]
  (let [persons (person-service/get-persons person-service (map #(get % :person-oid) applications))]
    (map #(let [person        (get persons (get % :person-oid))
                parsed-person (person-service/parse-person % person)]
            (assoc %
                   :sukunimi      (get parsed-person :last-name)
                   :etunimet      (get parsed-person :first-name)
                   :henkilotunnus (get parsed-person :ssn)))
         applications)))

(defn valinta-ui-applications
  [query person-service]
  (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
    (->> {:connection connection}
         (queries/yesql-valinta-ui-applications (-> (merge {:application_oids nil
                                                            :name             nil
                                                            :haku             nil
                                                            :hakukohde        nil
                                                            :ssn              nil
                                                            :person_oid       nil}
                                                           (transform-keys ->snake_case query))
                                                    (update :application_oids
                                                            #(some->> (seq %)
                                                                      to-array
                                                                      (.createArrayOf (:connection connection) "text")))
                                                    (update :hakukohde
                                                            #(some->> (seq %)
                                                                      to-array
                                                                      (.createArrayOf (:connection connection) "varchar")))))
         (map #(assoc (->kebab-case-kw %)
                      :hakutoiveet
                      (unwrap-external-application-hakutoiveet %)))
         (map #(select-keys % [:oid
                               :haku-oid
                               :person-oid
                               :asiointikieli
                               :lahiosoite
                               :postinumero
                               :hakukohde
                               :hakutoiveet
                               :answers]))
         (enrich-persons-from-onr person-service)
         (map #(dissoc % :answers))
         (map #(assoc % :asiointikieli (convert-asiointikieli (get % :asiointikieli)))))))

(defn- unwrap-person-and-hakemus-oid
  [{:keys [key person_oid]}]
  {key person_oid})

(defn get-person-and-application-oids
  ([haku-oid hakukohde-oids]
  (->> (exec-db :db queries/yesql-applications-by-haku-and-hakukohde-oids {:haku_oid       haku-oid
                                                                           ;; Empty string to avoid empty parameter lists
                                                                           :hakukohde_oids (cons "" hakukohde-oids)
                                                                           :hakemus_oids   [""]})
       (map unwrap-person-and-hakemus-oid)
       (into {})))
  ([haku-oid]
  (get-person-and-application-oids haku-oid nil)))

(defn- update-hakukohde-process-state!
  [connection session hakukohde-oids from-state to-state application-key]
  (let [application      (get-latest-application-by-key-in-tx connection
                                                              application-key)
        existing-reviews (filter
                          #(= (:state %) from-state)
                          (application-states/get-all-reviews-for-requirement "processing-state" application hakukohde-oids))
        new-reviews      (map
                          #(-> %
                               (assoc :state to-state)
                               (assoc :application_key application-key))
                          existing-reviews)
        new-event        {:application_key          application-key
                          :event_type               "hakukohde-review-state-change"
                          :new_review_state         to-state
                          :virkailija_oid           (-> session :identity :oid)
                          :virkailija_organizations (edit-application-right-organizations->json session)
                          :first_name               (:first-name session)
                          :last_name                (:last-name session)
                          :review_key               "processing-state"}]
    (doseq [new-review new-reviews]
      (queries/yesql-upsert-application-hakukohde-review! new-review {:connection connection})
      (queries/yesql-add-application-event<! (assoc new-event :hakukohde (:hakukohde new-review))
                                             {:connection connection}))
    (when new-reviews
      {:new       new-event
       :id        {:applicationOid application-key
                   :hakukohdeOids (clojure.string/join ", " (set (map :hakukohde existing-reviews)))
                   :requirement    "processing-state"}
       :operation audit-log/operation-modify
       :session   session})))

(defn applications-authorization-data [application-keys]
  (map ->kebab-case-kw
       (exec-db :db queries/yesql-applications-authorization-data
                {:application_keys application-keys})))

(defn persons-applications-authorization-data [person-oids]
  (map ->kebab-case-kw
       (exec-db :db queries/yesql-persons-applications-authorization-data
                {:person_oids person-oids})))

(defn mass-update-application-states
  [session application-keys hakukohde-oids from-state to-state audit-logger]
  (log/info "Mass updating" (count application-keys) "applications from" from-state "to" to-state "with hakukohtees" hakukohde-oids)
  (let [audit-log-entries (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                            (mapv
                             (partial update-hakukohde-process-state! connection session hakukohde-oids from-state to-state)
                             application-keys))]
    (doseq [audit-log-entry (filter some? audit-log-entries)]
      (audit-log/log audit-logger audit-log-entry))
    true))

(defn add-application-event-in-tx [conn event session]
  (-> {:event_type               nil
       :new_review_state         nil
       :virkailija_oid           (-> session :identity :oid)
       :virkailija_organizations (some-> session edit-application-right-organizations->json)
       :hakukohde                nil
       :review_key               nil}
      (merge (transform-keys ->snake_case event))
      (queries/yesql-add-application-event<! {:connection conn})
      (dissoc :virkailija_oid)
      (merge (select-keys (:identity session) [:first-name :last-name]))
      (->kebab-case-kw)))

(defn add-application-event [event session]
  (jdbc/with-db-transaction [db {:datasource (db/get-datasource :db)}]
                            (add-application-event-in-tx db event session)))

(defn get-applications-newer-than [date limit offset]
  (exec-db :db queries/yesql-get-applications-by-created-time {:date date :limit limit :offset (or offset 0)}))

(defn get-applications-between-start-and-end [start end limit offset]
  (exec-db :db queries/yesql-get-applications-by-created-time-between-start-and-end {:start start :end end :limit limit :offset (or offset 0)}))

(defn add-review-note [note session]
  {:pre [(-> note :application-key clojure.string/blank? not)
         (-> note :notes clojure.string/blank? not)]}
  (-> (exec-db :db queries/yesql-add-review-note<! {:application_key          (:application-key note)
                                                    :notes                    (:notes note)
                                                    :virkailija_oid           (-> session :identity :oid)
                                                    :hakukohde                (:hakukohde note)
                                                    :virkailija_organizations (edit-application-right-organizations->json session)
                                                    :state_name               (:state-name note)})
      util/remove-nil-values
      (merge (select-keys (:identity session) [:first-name :last-name]))
      (dissoc :virkailija_oid :removed)
      (->kebab-case-kw)))

(defn- unwrap-tilastokeskus-application
  [{:keys [haku-oid hakemus-oid henkilo-oid hakukohde-oids content hakemus-tila lahetysaika]}]
  (let [answers (answers-by-key (:answers content))]
    {:hakemus_oid    hakemus-oid
     :hakemus_tila   hakemus-tila
     :haku_oid       haku-oid
     :henkilo_oid    henkilo-oid
     :hakukohde_oids hakukohde-oids
     :content        content
     :kotikunta      (-> answers :home-town :value)
     :asuinmaa       (-> answers :country-of-residence :value)
     :submitted      lahetysaika}))

(defn get-application-info-for-tilastokeskus [haku-oid hakukohde-oid]
  (->> (exec-db :db queries/yesql-tilastokeskus-applications {:haku_oid haku-oid :hakukohde_oid hakukohde-oid})
       (map unwrap-tilastokeskus-application)))

(defn get-application-info-for-valintapiste [haku-oid hakukohde-oid]
  (->> (exec-db :db queries/yesql-valintapiste-applications {:haku_oid haku-oid :hakukohde_oid hakukohde-oid})
       (map (fn [{:keys [haku-oid hakemus-oid henkilo-oid hakukohde-oids hakemus-tila]}]
              {:hakemus_oid    hakemus-oid
               :hakemus_tila   hakemus-tila
               :haku_oid       haku-oid
               :henkilo_oid    henkilo-oid
               :hakukohde_oids hakukohde-oids}))))

(defn- get-application-eligibilities-by-hakutoive [application]
  (let [eligibilities-by-hakukohde (:application_hakukohde_reviews application)]
    (->> (:hakutoiveet application)
         (map-indexed
          (fn [index hakukohde-oid]
            (let [preference             (format "preference%d-Koulutus-id" (inc index))
                  preference-eligibility (str preference "-eligibility")
                  eligibility            (get eligibilities-by-hakukohde (keyword hakukohde-oid) "unreviewed")]
              {preference             hakukohde-oid
               preference-eligibility eligibility})))
         (into {}))))

(defn- indexed-by-question-group [index-fn key values]
  (apply concat (keep-indexed (fn [i values]
                                (when (some? values)
                                  (index-fn (format "%s_group%d" key i) values)))
                              values)))

(defn- indexed-by-values [key values]
  (map (fn [value]
         [(format "%s_%s" key value) value])
       values))

(defn- indexed-by-value-order [key values]
  (map-indexed (fn [i value]
                 [(format "%s_%d" key i) value])
               values))

(defn- not-indexed [key values]
  [[key (if (nil? (first values))
          ""
          (first values))]])

(defn flatten-application-answers [answers]
  (reduce
   (fn [acc {:keys [key value fieldType] :as answer}]
     (let [index-fn (cond (= "multipleChoice" fieldType)
                          indexed-by-values
                          (or (= "textField" fieldType)
                              (= "attachment" fieldType))
                          indexed-by-value-order
                          :else
                          not-indexed)]
       (into acc (cond (util/is-question-group-answer? value)
                       (indexed-by-question-group index-fn key value)
                       (vector? value)
                       (index-fn key value)
                       (string? value)
                       [[key value]]
                       (nil? value)
                       [[key ""]]
                       :else
                       (throw (new RuntimeException
                                   (str "Unknown answer form " answer)))))))
   {}
   answers))

(defn- unwrap-valintalaskenta-application [application]
  (let [keyword-values             (flatten-application-answers (->> application
                                                                     :content
                                                                     :answers
                                                                     (filter #(not= "hakukohteet" (:key %)))))
        eligibilities-by-hakutoive (get-application-eligibilities-by-hakutoive application)]
    (-> application
        (dissoc :content :application_hakukohde_reviews)
        (assoc :maksuvelvollisuus (reduce-kv #(assoc %1 (name %2) %3) {} (:maksuvelvollisuus application)))
        (assoc :keyValues (merge keyword-values eligibilities-by-hakutoive))
        (assoc :hakutoiveet (unwrap-external-application-hakutoiveet application))
        (dissoc :hakukohde)
        (clojure.set/rename-keys {:key :hakemusOid :person_oid :personOid :haku :hakuOid}))))

(defn get-applications-for-valintalaskenta [hakukohde-oid application-keys]
  (let [partition-size 10000
        fetch          (fn [key-partition]
                         (->> (exec-db :db queries/yesql-valintalaskenta-applications
                                       {:hakukohde_oid    hakukohde-oid
                                        :application_keys (cons "" key-partition)})
                              (map unwrap-valintalaskenta-application)))]
    (if (empty? application-keys)
      (fetch [])
      (->> application-keys
           (partition partition-size partition-size nil)
           (mapcat fetch)))))

(defn get-application-oids-for-valintalaskenta [hakukohde-oids]
  (set (->> (exec-db :db queries/yesql-valintalaskenta-application-oids
                     {:hakukohde_oids hakukohde-oids})
            (map :key))))

(defn get-raw-key-values [answers]
  (reduce
    (fn [acc {:keys [key value]}]
      (into acc [[key value]]))
    {}
    answers))

(defn- unwrap-siirtotiedosto-application [application]
  (let [attachments (->> application
                         :content
                         :answers
                         (filter #(="attachment" (:fieldType %)))
                         flatten-application-answers)
        keyword-values (->> application
                            :content
                            :answers
                            (filter #(not= "hakukohteet" (:key %)))
                            get-raw-key-values)
        payment-state   (or (:application-payment-states application) {})]
    (-> application
         (assoc :attachments attachments)
        (assoc :keyValues keyword-values)
        (assoc :applicationPaymentState payment-state)
        (dissoc :application-payment-states)
        (clojure.set/rename-keys {:key :hakemusOid :person-oid :personOid :haku :hakuOid}))))

(defn- unwrap-siirto-application [application]
  (let [attachments (->> application
                         :content
                         :answers
                         (filter #(="attachment" (:fieldType %)))
                         flatten-application-answers)
        keyword-values (->> application
                            :content
                            :answers
                            (filter #(not= "hakukohteet" (:key %)))
                            flatten-application-answers)
        application-hakukohde-reviews (or (:application-hakukohde-reviews application) [])
        application-hakukohde-attachment-reviews (or (:application-hakukohde-attachment-reviews application) [])
        application-review-notes (or (:application-review-notes application) [])
        application-payment-states (map (fn [state] (update state :total #(edn/read-string %))) (or (:application-payment-states application) []))
        submitted-formatted (.print ZonedJodaFormatter (:submitted application))
        created-formatted (.print ZonedJodaFormatter (:created application))
        modified-formatted (.print ZonedJodaFormatter (:modified application))]
  (-> application
      (dissoc :content :application-hakukohde-reviews :application-hakukohde-attachment-reviews :application-review-notes :application-payment-states :submitted :created :modified)
      (assoc :attachments attachments)
      (assoc :keyValues keyword-values)
      (assoc :hakukohdeReviews application-hakukohde-reviews)
      (assoc :hakukohdeAttachmentReviews application-hakukohde-attachment-reviews)
      (assoc :applicationReviewNotes application-review-notes)
      (assoc :applicationPaymentStates application-payment-states)
      (assoc :originallySubmitted submitted-formatted)
      (assoc :versionCreated created-formatted)
      (assoc :versionModified modified-formatted)
      (clojure.set/rename-keys {:key :hakemusOid :person-oid :personOid :haku :hakuOid}))))

(defn siirto-applications [hakukohde-oid haku-oid application-keys modified-after return-inactivated]
  (->> (exec-db :db queries/yesql-siirto-applications {:hakukohde_oid      hakukohde-oid
                                                       :haku_oid           haku-oid
                                                       :application_keys   (cons "" application-keys)
                                                       :modified_after     modified-after
                                                       :return_inactivated (boolean return-inactivated)})
       (map unwrap-siirto-application)))

(defn siirtotiedosto-applications-for-ids [ids]
  (log/info "Fetching applications for" (count ids) "ids.")
  (->> (exec-db :db queries/yesql-get-siirtotiedosto-applications-for-ids {:ids ids})
       (map unwrap-siirtotiedosto-application)))

(defn siirtotiedosto-application-ids [{:keys [window-start window-end haku-oid] :as params}]
  (log/info "Fetching application ids for params:" params)
  (if (some? haku-oid)
    (exec-db :db queries/yesql-get-siirtotiedosto-application-ids-for-haku {:haku_oid haku-oid})
    (exec-db :db queries/yesql-get-siirtotiedosto-application-ids {:window_start window-start
                                                                   :window_end window-end})))
(defn kouta-application-count-for-hakukohde [hakukohde-oid]
  (->> (exec-db :db queries/yesql-kouta-application-count-for-hakukohde {:hakukohde_oid    hakukohde-oid})
       (map #(:application_count %))
       (first)))

(defn remove-review-note [note-id]
  (when-not (= (exec-db :db queries/yesql-remove-review-note! {:id note-id}) 0)
    note-id))

(defn get-application-keys-for-person-oid [person-oid]
  (exec-db :db queries/yesql-get-latest-application-keys-distinct-by-person-oid {:person_oid person-oid}))

(defn get-application-version-changes [koodisto-cache application-key]
  (let [all-versions         (exec-db :db
                                      queries/yesql-get-application-versions
                                      {:application_key application-key})
        all-versions-paired  (map vector all-versions (rest all-versions))
        get-koodisto-options (partial koodisto/get-koodisto-options
                                      koodisto-cache)]
    (map (fn [[older-application newer-application]]
           (let [older-version-answers (util/application-answers-by-key older-application)
                 newer-version-answers (util/application-answers-by-key newer-application)
                 answer-keys           (set (concat (keys older-version-answers) (keys newer-version-answers)))
                 lang                  (or (-> newer-application :lang keyword) :fi)
                 form-fields           (util/form-fields-by-id (forms/get-form-by-application newer-application))]
             (into {}
                   (for [key   answer-keys
                         :let  [old-value (-> older-version-answers key :value)
                               new-value (-> newer-version-answers key :value)
                               field     (key form-fields)]
                         :when (not= old-value new-value)]
                     {key {:label (-> field :label lang)
                           :old   (util/populate-answer-koodisto-values old-value field get-koodisto-options)
                           :new   (util/populate-answer-koodisto-values new-value field get-koodisto-options)}}))))
         all-versions-paired)))

(defn get-application-ids-for-haku
  [haku-oid]
  (map :id (exec-db :db queries/yesql-get-application-ids-for-haku {:haku haku-oid})))

(defn get-application-person-oids-for-haku
  [haku-oid]
  (map :person_oid (exec-db :db queries/yesql-get-application-person-oids-for-haku {:haku haku-oid})))

(defn mass-delete-application-data
  [session application-keys delete-ordered-by reason-of-delete audit-logger]
  (log/info "Mass deleting" (count application-keys) "applications" application-keys)
  (let [not-deleted-keys (conj [] (doall
                                    (map #(if
                                            (or
                                              (> (:count (first (exec-db :db queries/yesql-get-application-events-processed-count-by-application-key {:key %}))) 0)
                                              (= (:state (get-application-review %)) "inactivated"))
                                            (do
                                              (log/info "Deleting application data for application key:" %)
                                              (try
                                                (exec-db :db queries/yesql-add-application-delete-history! {:application_key   %
                                                                                                            :deleted_by        (get-in session [:identity :oid])
                                                                                                            :delete_ordered_by delete-ordered-by
                                                                                                            :reason_of_delete  reason-of-delete})
                                                (exec-db :db queries/yesql-delete-multi-answers-by-application-key! {:key %})
                                                (exec-db :db queries/yesql-delete-multi-answer-values-by-application-key! {:key %})
                                                (exec-db :db queries/yesql-delete-information-requests-by-application-key! {:key %})
                                                (exec-db :db queries/yesql-delete-group-answers-by-application-key! {:key %})
                                                (exec-db :db queries/yesql-delete-group-answer-values-by-application-key! {:key %})
                                                (exec-db :db queries/yesql-delete-group-answer-groups-by-application-key! {:key %})
                                                (exec-db :db queries/yesql-delete-field-deadlines-by-application-key! {:key %})
                                                (exec-db :db queries/yesql-delete-application-secrets-by-application-key! {:key %})
                                                (exec-db :db queries/yesql-delete-application-review-notes-by-application-key! {:key %})
                                                (exec-db :db queries/yesql-delete-application-reviews-by-application-key! {:key %})
                                                (exec-db :db queries/yesql-delete-application-hakukohde-reviews-by-application-key! {:key %})
                                                (exec-db :db queries/yesql-delete-application-hakukohde-attachment-reviews-by-application-key! {:key %})
                                                (exec-db :db queries/yesql-delete-application-events-by-application-key! {:key %})
                                                (exec-db :db queries/yesql-delete-answers-by-application-key! {:key %})
                                                (exec-db :db queries/yesql-delete-application-by-application-key! {:key %})
                                                (audit-log/log audit-logger
                                                               {:new       {:application-key   %
                                                                            :deleted_by        (get-in session [:identity :oid])
                                                                            :delete_ordered_by delete-ordered-by
                                                                            :reason_of_delete  reason-of-delete}
                                                                :id        {:applicationOid %}
                                                                :session   session
                                                                :operation audit-log/operation-delete})
                                                (catch Exception e (log/error e "Delete failed for application-key:" % ", Exception:" e))))
                                            (do
                                              (log/warn "Application" % "status is not processed or inactivated or application is not found - deletion skipped.")
                                              %))
                                         application-keys)))]
    (remove nil? (vec (flatten not-deleted-keys)))))


(defn- inactivate-application
  [session reason-of-inactivation audit-logger application-key]
  (if (contains? #{"active" "processing" "unprocessed"}
                 (:state (get-application-review application-key)))
    (do
      (log/info "Inactivating application for application key" application-key)
      (try
        (save-application-review {:application-key application-key :state "inactivated"} session audit-logger)
        (add-review-note {:application-key application-key :notes reason-of-inactivation} session)
        nil
        (catch Exception e
          (log/error e "Inactivation failed for application key" application-key "exception:" e)
          application-key)))
    (do
      (log/warn "Application" application-key "was not found or active - inactivation skipped.")
      application-key)))

(defn- reactivate-application
  [session reason-of-reactivation audit-logger application-key]
  (if (= (:state (get-application-review application-key)) "inactivated")
    (do
      (log/info "Reactivating application for application key" application-key)
      (try
        (save-application-review {:application-key application-key
                                  :state application-review-states/initial-application-review-state}
                                 session audit-logger)
        (add-review-note {:application-key application-key :notes reason-of-reactivation} session)
        nil
        (catch Exception e
          (log/error e "Reactivation failed for application key" application-key "exception:" e)
          application-key)))
    (do
      (log/warn "Application" application-key "was not inactivated or found - reactivation skipped.")
      application-key)))

(defn mass-inactivate-applications
  "Marks a number of applications as inactivated. Returns keys of applications that were not inactivated."
  [session application-keys reason-of-inactivation audit-logger]
  (log/info "Mass inactivating" (count application-keys) "applications" application-keys)
  (let [not-inactivated-keys (conj [] (doall
                                       (map (partial inactivate-application session reason-of-inactivation audit-logger)
                                            application-keys)))
        not-inactivated-filtered (remove nil? (vec (flatten not-inactivated-keys)))
        inactivated-keys (remove (set not-inactivated-filtered) application-keys)]
    (log/info "Inactivated" (count inactivated-keys) "applications, failed to inactivate" (count not-inactivated-filtered)
              "applications. Inactivated keys:" inactivated-keys "not inactivated keys:" not-inactivated-filtered)
    not-inactivated-filtered))

(defn mass-reactivate-applications
  "Reactivates a number of applications. Returns keys of applications that were not reactivated."
  [session application-keys reason-of-reactivation audit-logger]
  (log/info "Reactivating" (count application-keys) "applications" application-keys)
  (let [not-reactivated-keys (conj [] (doall
                                        (map (partial reactivate-application session reason-of-reactivation audit-logger)
                                             application-keys)))
        not-reactivated-filtered (remove nil? (vec (flatten not-reactivated-keys)))
        reactivated-keys (remove (set not-reactivated-filtered) application-keys)]
    (log/info "Reactivated" (count reactivated-keys) "applications, failed to reactivate" (count not-reactivated-filtered)
              "applications. Reactivated keys:" reactivated-keys "not reactivated keys:" not-reactivated-filtered)
    not-reactivated-filtered))

(defn get-latest-applications-for-kk-payment-processing
  [person-oids haku-oids]
  (exec-db :db queries/yesql-get-latest-applications-for-kk-payment-processing {:person_oids person-oids
                                                                                :haku_oids (vec haku-oids)}))
