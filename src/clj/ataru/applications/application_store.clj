(ns ataru.applications.application-store
  (:require [ataru.application.application-states :as application-states]
            [ataru.application.review-states :refer [incomplete-states] :as application-review-states]
            [ataru.db.db :as db]
            [ataru.dob :as dob]
            [ataru.forms.form-store :as forms]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.log.audit-log :as audit-log]
            [ataru.schema.form-schema :as schema]
            [ataru.util :refer [answers-by-key] :as util]
            [ataru.util.language-label :as label]
            [ataru.util.random :as crypto]
            [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit]
            [camel-snake-kebab.core :as t :refer [->snake_case ->kebab-case-keyword ->camelCase]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [clj-time.coerce :as c]
            [clj-time.core :as time]
            [clj-time.format :as f]
            [clojure.java.jdbc :as jdbc]
            [schema.core :as s]
            [taoensso.timbre :refer [info]]
            [yesql.core :refer [defqueries]]))

(defqueries "sql/application-queries.sql")

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

(defn- auditlog-read
  [application session]
  (audit-log/log {:new              (dissoc application :content)
                  :id               (get-in session [:identity :oid])
                  :operation        audit-log/operation-read}))

(defn unwrap-application
  [application]
  (when application
    (assoc (->kebab-case-kw (dissoc application :content))
      :answers
      (mapv (fn [answer]
              (update answer :label (fn [label]
                                      (label/get-language-label-in-preferred-order label))))
            (-> application :content :answers)))))

(defn- application-exists-with-secret-tx?
  "NB: takes into account also expired secrets"
  [hakija-secret connection]
  (let [application-count (->> (yesql-get-application-count-for-secret {:secret hakija-secret} connection)
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
  (-> (exec-db :db yesql-get-latest-application-language-by-any-version-of-secret {:secret hakija-secret})
      (first)
      :lang))

(defn generate-new-application-secret
  [connection]
  (loop [secret (crypto/url-part 34)]
    (if-not (application-exists-with-secret-tx? secret connection)
      secret
      (recur (crypto/url-part 34)))))

(defn- intersect?
  [set1 set2]
  (not-empty (clojure.set/intersection set1 set2)))

(defn- hakukohde-oids-for-attachment-review
  [attachment-field hakutoiveet]
  (let [belongs-to-hakukohteet    (set (:belongs-to-hakukohteet attachment-field))
        belongs-to-hakukohderyhma (set (:belongs-to-hakukohderyhma attachment-field))]
    (cond
      (or (not-empty belongs-to-hakukohteet)
          (not-empty belongs-to-hakukohderyhma))
      (->> hakutoiveet
           (filter #(or (contains? belongs-to-hakukohteet (:oid %))
                        (intersect? belongs-to-hakukohderyhma (set (:hakukohderyhmat %)))))
           (map :oid))

      (not-empty hakutoiveet)
      (map :oid hakutoiveet)

      :else ["form"])))

(defn- create-attachment-reviews
  [attachment-field answer old-answer update? application-key hakutoiveet]
  (let [value-changed? (and update?
                            (not= old-answer answer))
        review-base    {:application_key application-key
                        :attachment_key  (:id attachment-field)
                        :state           (if (empty? answer)
                                           "incomplete"
                                           "not-checked")
                        :updated?        value-changed?}]
    (map #(assoc review-base :hakukohde %)
         (hakukohde-oids-for-attachment-review attachment-field hakutoiveet))))

(defn- followup-option-selected?
  [field answers]
  (let [parent-answer-key (-> field :followup-of keyword)
        answers    (-> answers
                       parent-answer-key
                       :value
                       vector ; Make sure we won't flatten a string answer to ()
                       flatten
                       set)]
    (contains? answers (:option-value field))))

(defn filter-visible-attachments
  [answers fields]
  (filter (fn [field]
            (and (= "attachment" (:fieldType field))
                 (or (not (contains? field :followup-of))
                     (followup-option-selected? field answers))))
          fields))

(defn create-application-attachment-reviews
  [application-key visible-attachments answers-by-key old-answers applied-hakukohteet update?]
  (mapcat (fn [attachment]
            (let [attachment-key (-> attachment :id keyword)
                  answer         (-> answers-by-key attachment-key :value)
                  old-answer     (-> old-answers attachment-key :value)]
              (create-attachment-reviews attachment
                                         answer
                                         old-answer
                                         update?
                                         application-key
                                         applied-hakukohteet)))
          visible-attachments))

(defn- delete-orphan-attachment-reviews
  [application-key visible-attachments applied-hakukohteet connection]
  (yesql-delete-application-attachment-reviews!
   {:application_key     application-key
    :attachment_keys     (cons "" (map :id visible-attachments))
    :applied_hakukohteet (cons "" applied-hakukohteet)}
   connection))

(defn- create-attachment-hakukohde-reviews-for-application
  [application applied-hakukohteet old-answers form update? connection]
  (let [flat-form-content   (-> form :content util/flatten-form-fields)
        answers-by-key      (-> application :content :answers util/answers-by-key)
        visible-attachments (filter-visible-attachments answers-by-key flat-form-content)
        reviews             (create-application-attachment-reviews
                             (:key application)
                             visible-attachments
                             answers-by-key
                             old-answers
                             applied-hakukohteet
                             update?)]
    (doseq [review reviews]
      ((if (:updated? review)
         yesql-update-attachment-hakukohde-review!
         yesql-save-attachment-review!)
       (dissoc review :updated?) connection))
    (when update?
      (delete-orphan-attachment-reviews (:key application)
                                        visible-attachments
                                        (map :oid applied-hakukohteet)
                                        connection))))

(defn- add-new-application-version
  "Add application and also initial metadata (event for receiving application, and initial review record)"
  [application create-new-secret? applied-hakukohteet old-answers form update? conn]
  (let [connection                  {:connection conn}
        answers                     (->> application
                                         :answers
                                         (filter #(not-empty (:value %))))
        application-to-store        {:form_id        (:form application)
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
        new-application             (if (contains? application :key)
                                      (yesql-add-application-version<! application-to-store connection)
                                      (yesql-add-application<! application-to-store connection))]
    (create-attachment-hakukohde-reviews-for-application new-application applied-hakukohteet old-answers form update? {:connection conn})
    (when create-new-secret?
      (yesql-add-application-secret!
        {:application_key (:key new-application)
         :secret          (generate-new-application-secret connection)}
        connection))
    (unwrap-application new-application)))

(defn- get-latest-version-and-lock-for-update [secret lang conn]
  (if-let [application (first (yesql-get-latest-version-by-secret-lock-for-update {:secret secret} {:connection conn}))]
    (unwrap-application application)
    (throw (ex-info "No existing form found when updating" {:secret secret}))))

(defn- get-latest-version-for-virkailija-edit-and-lock-for-update [virkailija-secret lang conn]
  (if-let [application (first (yesql-get-latest-version-by-virkailija-secret-lock-for-update {:virkailija_secret virkailija-secret} {:connection conn}))]
    (unwrap-application application)
    (throw (ex-info "No existing form found when updating as virkailija" {:virkailija-secret virkailija-secret}))))

(defn- get-virkailija-oid-for-update-secret
  [conn secret]
  (->> (jdbc/query conn ["SELECT virkailija_oid
                          FROM virkailija_update_secrets
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

(defn add-application [new-application applied-hakukohteet form]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (info (str "Inserting new application"))
    (let [virkailija-oid                       (when-let [secret (:virkailija-secret new-application)]
                                                 (get-virkailija-oid-for-create-secret conn secret))
          {:keys [id key] :as new-application} (add-new-application-version new-application
                                                                            true
                                                                            applied-hakukohteet
                                                                            nil
                                                                            form
                                                                            false
                                                                            conn)
          connection                           {:connection conn}]
      (audit-log/log {:new       new-application
                      :operation audit-log/operation-new
                      :id        (if (some? virkailija-oid)
                                   virkailija-oid
                                   (util/extract-email new-application))})
      (yesql-add-application-event<! {:application_key  key
                                      :event_type       (if (some? virkailija-oid)
                                                          "received-from-virkailija"
                                                          "received-from-applicant")
                                      :new_review_state nil
                                      :virkailija_oid   virkailija-oid
                                      :hakukohde        nil
                                      :review_key       nil}
                                     connection)
      (yesql-add-application-review! {:application_key key
                                      :state           application-review-states/initial-application-review-state}
                                     connection)
      id)))

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

(defn update-application [{:keys [lang secret virkailija-secret] :as new-application} applied-hakukohteet form]
  {:pre [(or (not-blank? secret)
             (not-blank? virkailija-secret))]}
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [updated-by-applicant? (not-blank? secret)
          old-application       (if updated-by-applicant?
                                  (get-latest-version-and-lock-for-update secret lang conn)
                                  (get-latest-version-for-virkailija-edit-and-lock-for-update virkailija-secret lang conn))
          {:keys [id key] :as new-application} (add-new-application-version
                                                 (merge-applications new-application old-application)
                                                 updated-by-applicant?
                                                 applied-hakukohteet
                                                 (->  old-application :answers util/answers-by-key)
                                                 form
                                                 true
                                                 conn)
          virkailija-oid        (when-not updated-by-applicant?
                                  (get-virkailija-oid-for-update-secret conn virkailija-secret))]
      (info (str "Updating application with key "
                 (:key old-application)
                 " based on valid application secret, retaining key" (when-not updated-by-applicant? " and secret") " from previous version"))
      (yesql-add-application-event<! {:application_key  key
                                      :event_type       (if updated-by-applicant?
                                                          "updated-by-applicant"
                                                          "updated-by-virkailija")
                                      :new_review_state nil
                                      :virkailija_oid   virkailija-oid
                                      :hakukohde        nil
                                      :review_key       nil}
                                    {:connection conn})
      (audit-log/log {:new       (application->loggable-form new-application)
                      :old       (application->loggable-form old-application)
                      :operation audit-log/operation-modify
                      :id        (if updated-by-applicant?
                                   (util/extract-email new-application)
                                   virkailija-oid)})
      id)))

(defn ->name-query-value
  [name]
  (->> (clojure.string/split name #"\s+")
       (remove clojure.string/blank?)
       (map #(str % ":*"))
       (clojure.string/join " & ")))

(defn- query->db-query
  [connection query]
  (-> (merge {:form                   nil
              :application_oid        nil
              :application_oids       nil
              :person_oid             nil
              :name                   nil
              :email                  nil
              :dob                    nil
              :ssn                    nil
              :haku                   nil
              :hakukohde              nil
              :ensisijainen_hakukohde nil}
             (transform-keys ->snake_case query))
      (update :application_oids
              #(some->> (seq %)
                        to-array
                        (.createArrayOf (:connection connection) "text")))))

(defn get-application-heading-list
  [query]
  (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
                           (yesql-get-application-list-for-virkailija
                             (query->db-query connection query)
                             {:connection connection})))

(defn get-full-application-list-by-person-oid-for-omatsivut-and-refresh-old-secrets
  [person-oid]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [connection       {:connection conn}
          application-keys (map :key (yesql-get-application-list-by-person-oid-for-omatsivut
                                       {:person_oid person-oid} connection))
          old-secrets      (if (not-empty application-keys)
                             (yesql-get-expiring-secrets-for-applications
                               {:application_keys application-keys} connection)
                             [])]
      (doseq [old-secret old-secrets]
        (info "Refreshing secret for application" (:application_key old-secret))
        (yesql-add-application-secret!
          {:application_key (:application_key old-secret)
           :secret          (generate-new-application-secret connection)}
          connection))
      (->kebab-case-kw
        (yesql-get-application-list-by-person-oid-for-omatsivut
          {:person_oid person-oid} connection)))))

(defn- unwrap-onr-application
  [{:keys [key haku form email content]}]
  (let [answers (answers-by-key (:answers content))]
    {:oid          key
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

(defn onr-applications [person-oid]
  (->> (exec-db :db yesql-onr-applications {:person_oid person-oid})
       (map unwrap-onr-application)))

(defn has-ssn-applied [haku-oid ssn]
  (->> (exec-db :db yesql-has-ssn-applied {:haku_oid haku-oid
                                           :ssn ssn})
       first
       ->kebab-case-kw))

(defn has-email-applied [haku-oid email]
  (->> (exec-db :db yesql-has-email-applied {:haku_oid haku-oid
                                             :email email})
       first
       ->kebab-case-kw))

(defn get-application-review-notes [application-key]
  (->> (exec-db :db yesql-get-application-review-notes {:application_key application-key})
       (map ->kebab-case-kw)
       (map util/remove-nil-values)))

(defn get-application-review [application-key]
  (->> (exec-db :db yesql-get-application-review {:application_key application-key})
       (map ->kebab-case-kw)
       (first)))

(defn get-application-reviews-by-keys [application-keys]
  (map ->kebab-case-kw
       (exec-db :db yesql-get-application-reviews-by-keys {:application_keys application-keys})))

(defn get-application-review-notes-by-keys [application-keys]
  (map ->kebab-case-kw
       (exec-db :db yesql-get-application-review-notes-by-keys {:application_keys application-keys})))

(defn get-application [application-id]
  (unwrap-application (first (exec-db :db yesql-get-application-by-id {:application_id application-id}))))

(defn get-latest-application-by-key [application-key session]
  (let [application (-> (exec-db :db yesql-get-latest-application-by-key
                          {:application_key application-key})
                        (first)
                        (unwrap-application))]
    (auditlog-read application session)
    application))

(defn get-application-hakukohde-reviews
  [application-key]
  (mapv ->kebab-case-kw (exec-db :db yesql-get-application-hakukohde-reviews {:application_key application-key})))

(defn get-application-attachment-reviews
  [application-key]
  (mapv ->kebab-case-kw (exec-db :db yesql-get-application-attachment-reviews {:application_key application-key})))

(defn get-latest-application-by-secret [secret]
  (when-let [application (->> (exec-db :db yesql-get-latest-application-by-secret {:secret secret})
                              (first)
                              (unwrap-application))]
    (-> application
        (assoc :state (-> (:key application) get-application-review :state))
        (assoc :application-hakukohde-reviews (get-application-hakukohde-reviews (:key application))))))

(defn get-latest-application-for-virkailija-edit [virkailija-secret]
  (when-let [application (->> (exec-db :db yesql-get-latest-application-by-virkailija-secret {:virkailija_secret virkailija-secret})
                              (first)
                              (unwrap-application))]
    (assoc application :state (-> (:key application) get-application-review :state))))

(defn get-latest-version-of-application-for-edit
  [{:keys [secret virkailija-secret]}]
  (if secret
    (get-latest-application-by-secret secret)
    (get-latest-application-for-virkailija-edit virkailija-secret)))

(defn get-latest-application-secret
  []
  (:secret (first (->> (exec-db :db yesql-get-latest-application-secret {})))))

(defn alter-application-hakukohteet-with-secret
  [secret new-hakukohteet]
  (when-not (= (exec-db :db yesql-set-application-hakukohteet-by-secret! {:secret secret :hakukohde new-hakukohteet}) 0)
    secret))

(defn add-new-secret-to-application
  [application-key]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [connection      {:connection conn}
          application     (-> (yesql-get-latest-application-by-key
                               {:application_key application-key}
                               connection)
                              (first)
                              (unwrap-application))
          new-secret      (generate-new-application-secret connection)]
      (yesql-add-application-secret! {:application_key application-key :secret new-secret} connection)
      (:id application))))

(defn add-new-secret-to-application-by-old-secret
  [old-secret]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [connection      {:connection conn}
          application-key (-> (yesql-get-application-key-for-any-version-of-secret {:secret old-secret} connection)
                              (first)
                              :application_key)
          application     (-> (yesql-get-latest-application-by-key
                               {:application_key application-key}
                               connection)
                              (first)
                              (unwrap-application))
          new-secret      (generate-new-application-secret connection)]
      (yesql-add-application-secret! {:application_key application-key :secret new-secret} connection)
      (:id application))))

(defn get-application-events [application-key]
  (mapv ->kebab-case-kw (exec-db :db yesql-get-application-events {:application_key application-key})))

(defn- auditlog-review-modify
  [review old-value session]
  (audit-log/log {:new              review
                  :old              old-value
                  :id               (get-in session [:identity :oid])
                  :operation        audit-log/operation-modify}))

(defn- store-and-log-review-event
  [connection event session]
  (yesql-add-application-event<! event connection)
  (audit-log/log {:new              event
                  :id               (get-in session [:identity :oid])
                  :operation        audit-log/operation-new}))

(defn save-application-review [review session]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [connection       {:connection conn}
          app-key          (:application-key review)
          old-review       (first (yesql-get-application-review {:application_key app-key} connection))
          review-to-store  (transform-keys ->snake_case review)]
      (auditlog-review-modify review-to-store old-review session)
      (yesql-save-application-review! review-to-store connection)
      (when (not= (:state old-review) (:state review-to-store))
        (let [application-event {:application_key  app-key
                                 :event_type       "review-state-change"
                                 :new_review_state (:state review-to-store)
                                 :virkailija_oid   (-> session :identity :oid)
                                 :hakukohde        nil
                                 :review_key       nil}]
          (store-and-log-review-event connection application-event session))))))

(defn save-application-hakukohde-review
  [application-key hakukohde-oid hakukohde-review-requirement hakukohde-review-state session]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (let [connection                  {:connection conn}
                                  review-to-store             {:application_key application-key
                                                               :requirement     hakukohde-review-requirement
                                                               :state           hakukohde-review-state
                                                               :hakukohde       hakukohde-oid}
                                  existing-duplicate-review   (yesql-get-existing-application-hakukohde-review review-to-store connection)
                                  existing-requirement-review (yesql-get-existing-requirement-review review-to-store connection)]
                              (when (empty? existing-duplicate-review)
                                (auditlog-review-modify review-to-store (first existing-requirement-review) session)
                                (yesql-upsert-application-hakukohde-review! review-to-store connection)
                                (let [hakukohde-event {:application_key  application-key
                                                       :event_type       "hakukohde-review-state-change"
                                                       :new_review_state (:state review-to-store)
                                                       :review_key       hakukohde-review-requirement
                                                       :hakukohde        (:hakukohde review-to-store)
                                                       :virkailija_oid   (-> session :identity :oid)}]
                                  (store-and-log-review-event connection hakukohde-event session))))))

(defn save-attachment-hakukohde-review
  [application-key hakukohde-oid attachment-key hakukohde-review-state session]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [connection      {:connection conn}
          review-to-store {:application_key application-key
                           :attachment_key  attachment-key
                           :state           hakukohde-review-state
                           :hakukohde       hakukohde-oid}]
      (if-let [existing-attachment-review (first (yesql-get-existing-attachment-review review-to-store connection))]
        (when-not (= hakukohde-review-state (:state existing-attachment-review))
          (auditlog-review-modify review-to-store existing-attachment-review session)
          (yesql-update-attachment-hakukohde-review! review-to-store connection)
          (let [hakukohde-event {:application_key  application-key
                                 :event_type       "attachment-review-state-change"
                                 :new_review_state (:state review-to-store)
                                 :review_key       attachment-key
                                 :hakukohde        (:hakukohde review-to-store)
                                 :virkailija_oid   (-> session :identity :oid)}]
            (store-and-log-review-event connection hakukohde-event session)))
        (throw (new IllegalStateException (str "No existing attahcment review found for " review-to-store)))))))

(s/defn get-applications-for-form :- [schema/Application]
  [form-key :- s/Str filtered-states :- [s/Str]]
  (->> {:form_key form-key :filtered_states filtered-states}
       (exec-db :db yesql-get-applications-for-form)
       (mapv unwrap-application)))

(defn get-applications-by-keys
  [application-keys]
  (mapv unwrap-application
        (exec-db :db yesql-get-applications-by-keys {:application_keys application-keys})))

(s/defn get-applications-for-hakukohde :- [schema/Application]
  [filtered-states :- [s/Str]
   hakukohde-oid :- s/Str]
  (mapv unwrap-application
        (exec-db :db yesql-get-applications-for-hakukohde
                 {:filtered_states filtered-states
                  :hakukohde_oid   hakukohde-oid})))

(s/defn get-applications-for-haku :- [schema/Application]
  [haku-oid :- s/Str
   filtered-states :- [s/Str]]
  (mapv unwrap-application
        (exec-db :db yesql-get-applications-for-haku
                 {:filtered_states filtered-states
                  :haku_oid        haku-oid})))

(defn add-person-oid
  "Add person OID to an application"
  [application-id person-oid]
  (exec-db :db yesql-add-person-oid!
    {:id application-id :person_oid person-oid}))

(defn get-haut
  []
  (mapv ->kebab-case-kw (exec-db :db yesql-get-haut-and-hakukohteet-from-applications {})))

(defn get-direct-form-haut
  []
  (mapv ->kebab-case-kw (exec-db :db yesql-get-direct-form-haut {})))

(defn add-application-feedback
  [feedback]
  (->kebab-case-kw
    (exec-db :db yesql-add-application-feedback<! (transform-keys ->snake_case feedback))))

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

(defn- kk-base-educations-new-module [answers]
  (let [m {"pohjakoulutus_kk"    "kk"
           "pohjakoulutus_avoin" "avoin"
           "pohjakoulutus_ulk"   "ulk"
           "pohjakoulutus_muu"   "muu"}]
    (keep m (-> answers :higher-completed-base-education :value))))

(defn- kk-base-educations [answers]
  (distinct (concat (kk-base-educations-old-module answers)
                    (kk-base-educations-new-module answers))))

(defn- korkeakoulututkinto-vuosi [answers]
  (cond (= "Yes" (get-in answers [:finnish-vocational-before-1995 :value] "No"))
        (Integer/valueOf (get-in answers [:finnish-vocational-before-1995--year-of-completion :value]))
        ;; syksyn 2018 kk yhteishaun lomakkeella kysymyksellä on satunnainen tunniste
        (= "0" (get-in answers [:2bfb9ea5-3896-4d82-9966-a03d418012fb :value]))
        (Integer/valueOf (get-in answers [:ea33f9b9-674c-4513-9b0c-93c22a24043e :value]))
        :else
        nil))

(defn- unwrap-hakurekisteri-application
  [{:keys [key haku hakukohde person_oid lang email content payment-obligations eligibilities]}]
  (let [answers (answers-by-key (:answers content))]
    {:oid                         key
     :personOid                   person_oid
     :applicationSystemId         haku
     :kieli                       lang
     :hakukohteet                 hakukohde
     :email                       email
     :matkapuhelin                (-> answers :phone :value)
     :lahiosoite                  (-> answers :address :value)
     :postinumero                 (-> answers :postal-code :value)
     :postitoimipaikka            (-> answers :postal-office :value)
     ;; Default asuinmaa to finland for forms that are from before
     ;; country-of-residence was implemented, or copied from those forms.
     :asuinmaa                    (or (-> answers :country-of-residence :value) "246")
     :kotikunta                   (-> answers :home-town :value)
     :kkPohjakoulutus             (kk-base-educations answers)
     :sahkoisenAsioinninLupa      (= "Kyllä" (-> answers :sahkoisen-asioinnin-lupa :value))
     :valintatuloksenJulkaisulupa (= "Kyllä" (-> answers :valintatuloksen-julkaisulupa :value))
     :koulutusmarkkinointilupa    (= "Kyllä" (-> answers :koulutusmarkkinointilupa :value))
     :korkeakoulututkintoVuosi    (korkeakoulututkinto-vuosi answers)
     :paymentObligations          (reduce-kv #(assoc %1 (name %2) %3) {} payment-obligations)
     :eligibilities               (reduce-kv #(assoc %1 (name %2) %3) {} eligibilities)}))

(defn get-hakurekisteri-applications
  [haku-oid hakukohde-oids person-oids modified-after]
  (->> (exec-db :db yesql-applications-for-hakurekisteri
                {:haku_oid       haku-oid
                 :hakukohde_oids (cons "" hakukohde-oids)
                 :person_oids    (cons "" person-oids)
                 :modified_after (some->> modified-after
                                          (f/parse (f/formatter "yyyyMMddHHmm"
                                                                (time/time-zone-for-id "Europe/Helsinki")))
                                          str)})
       (map unwrap-hakurekisteri-application)))

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
         (merge (dissoc requirements :languageRequirement :selectionState :degreeRequirement)
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
  [{:keys [key haku organization_oid person_oid lang email hakukohde content] :as application}]
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

(defn get-external-applications
  [haku-oid hakukohde-oid hakemus-oids]
  (->> (exec-db :db
                yesql-applications-by-haku-and-hakukohde-oids
                {:haku_oid                     haku-oid
                 ; Empty string to avoid empty parameter lists
                 :hakukohde_oids               (cond-> [""]
                                                       (some? hakukohde-oid)
                                                       (conj hakukohde-oid))
                 :hakemus_oids                 (cons "" hakemus-oids)})
       (map unwrap-external-application)))

(defn valinta-ui-applications
  [query]
  (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
    (->> {:connection connection}
         (yesql-valinta-ui-applications (query->db-query connection query))
         (map #(assoc (->kebab-case-kw %)
                      :hakutoiveet
                      (unwrap-external-application-hakutoiveet %)))
         (map #(select-keys % [:oid
                               :haku-oid
                               :person-oid
                               :lahiosoite
                               :postinumero
                               :hakukohde
                               :hakutoiveet])))))

(defn- unwrap-person-and-hakemus-oid
  [{:keys [key person_oid]}]
  {key person_oid})

(defn get-person-and-application-oids
  [haku-oid hakukohde-oids]
  (->> (exec-db :db yesql-applications-by-haku-and-hakukohde-oids {:haku_oid       haku-oid
                                                                   ; Empty string to avoid empty parameter lists
                                                                   :hakukohde_oids (cons "" hakukohde-oids)
                                                                   :hakemus_oids    [""]})
       (map unwrap-person-and-hakemus-oid)
       (into {})))

(defn- get-latest-application-by-key-with-hakukohde-reviews
  [connection application-key]
  (-> (yesql-get-latest-application-by-key-with-hakukohde-reviews
       {:application_key application-key}
       connection)
      (first)
      (unwrap-application)))

(defn- update-hakukohde-process-state!
  [connection session hakukohde-oid from-state to-state application-key]
  (let [application      (get-latest-application-by-key-with-hakukohde-reviews
                          connection
                          application-key)
        existing-reviews (filter
                           #(= (:state %) from-state)
                           (application-states/get-all-reviews-for-requirement "processing-state" application (seq hakukohde-oid)))
        new-reviews      (map
                           #(-> %
                                (assoc :state to-state)
                                (assoc :application_key application-key))
                           existing-reviews)
        new-event        {:application_key  application-key
                          :event_type       "hakukohde-review-state-change"
                          :new_review_state to-state
                          :virkailija_oid   (-> session :identity :oid)
                          :first_name       (:first-name session)
                          :last_name        (:last-name session)
                          :review_key       "processing-state"}]
    (when (seq new-reviews)
      (info "Updating" (count new-reviews) "application-hakukohde-reviews"))
    (doseq [new-review new-reviews]
      (yesql-upsert-application-hakukohde-review! new-review connection)
      (yesql-add-application-event<! (assoc new-event :hakukohde (:hakukohde new-review))
                                     connection))
    (when new-reviews
      {:new       new-event
       :id        (-> session :identity :oid)
       :operation audit-log/operation-new})))

(defn applications-authorization-data [application-keys]
  (map ->kebab-case-kw
       (exec-db :db yesql-applications-authorization-data
                {:application_keys application-keys})))

(defn persons-applications-authorization-data [person-oids]
  (map ->kebab-case-kw
       (exec-db :db yesql-persons-applications-authorization-data
                {:person_oids person-oids})))

(defn mass-update-application-states
  [session application-keys hakukohde-oid from-state to-state]
  (let [audit-log-entries (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (let [connection {:connection conn}]
                              (mapv
                               (partial update-hakukohde-process-state! connection session hakukohde-oid from-state to-state)
                               application-keys)))]
    (doseq [audit-log-entry (filter some? audit-log-entries)]
      (audit-log/log audit-log-entry))
    true))

(defn add-application-event [event session]
  (jdbc/with-db-transaction [db {:datasource (db/get-datasource :db)}]
    (let [conn       {:connection db}]
      (-> {:event_type       nil
           :new_review_state nil
           :virkailija_oid   (-> session :identity :oid)
           :hakukohde        nil
           :review_key       nil}
          (merge (transform-keys ->snake_case event))
          (yesql-add-application-event<! conn)
          (dissoc :virkailija_oid)
          (merge (select-keys (:identity session) [:first-name :last-name]))
          (->kebab-case-kw)))))

(defn get-applications-newer-than [date limit offset]
  (exec-db :db yesql-get-applications-by-created-time {:date date :limit limit :offset (or offset 0)}))

(defn add-review-note [note session]
  {:pre [(-> note :application-key clojure.string/blank? not)
         (-> note :notes clojure.string/blank? not)]}
  (-> (exec-db :db yesql-add-review-note<! {:application_key (:application-key note)
                                            :notes           (:notes note)
                                            :virkailija_oid  (-> session :identity :oid)
                                            :hakukohde       (:hakukohde note)
                                            :state_name      (:state-name note)})
      util/remove-nil-values
      (merge (select-keys (:identity session) [:first-name :last-name]))
      (dissoc :virkailija_oid :removed)
      (->kebab-case-kw)))

(defn get-application-info-for-tilastokeskus [haku-oid hakukohde-oid]
  (exec-db :db yesql-tilastokeskus-applications {:haku_oid haku-oid :hakukohde_oid hakukohde-oid}))

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
  (apply concat (map-indexed (fn [i values]
                               (index-fn (format "%s_group%d" key i) values))
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

(defn- flatten-application-answers [answers]
  (reduce
   (fn [acc {:keys [key value fieldType] :as answer}]
     (let [index-fn (cond (= "multipleChoice" fieldType)
                          indexed-by-values
                          (= "textField" fieldType)
                          indexed-by-value-order
                          :else
                          not-indexed)]
       (into acc (cond (= "attachment" fieldType)
                       nil
                       (and (sequential? value)
                            (every? sequential? value))
                       (indexed-by-question-group index-fn key value)
                       (and (sequential? value)
                            (contains? #{"multipleChoice" "textField"}
                                       fieldType))
                       (index-fn key value)
                       (not (sequential? value))
                       [[key value]]
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
        (assoc :keyValues (merge keyword-values eligibilities-by-hakutoive))
        (clojure.set/rename-keys {:key :hakemusOid :person_oid :personOid :haku :hakuOid}))))

(defn get-applications-for-valintalaskenta [hakukohde-oid application-keys]
  (let [partition-size 10000
        fetch          (fn [key-partition]
                         (->> (exec-db :db yesql-valintalaskenta-applications
                                       {:hakukohde_oid    hakukohde-oid
                                        :application_keys (cons "" key-partition)})
                              (map unwrap-valintalaskenta-application)))]
    (if (empty? application-keys)
      (fetch [])
      (->> application-keys
           (partition partition-size partition-size nil)
           (mapcat fetch)))))

(defn- unwrap-siirto-application [application]
  (let [keyword-values (->> application
                            :content
                            :answers
                            (filter #(not= "hakukohteet" (:key %)))
                            flatten-application-answers)]
    (-> application
        (dissoc :content)
        (assoc :keyValues keyword-values)
        (clojure.set/rename-keys {:key :hakemusOid :person-oid :personOid :haku :hakuOid}))))

(defn siirto-applications [hakukohde-oid application-keys]
  (->> (exec-db :db yesql-siirto-applications {:hakukohde_oid    hakukohde-oid
                                               :application_keys (cons "" application-keys)})
       (map unwrap-siirto-application)))

(defn remove-review-note [note-id]
  (when-not (= (exec-db :db yesql-remove-review-note! {:id note-id}) 0)
    note-id))

(defn get-application-keys []
  (exec-db :db yesql-get-latest-application-ids-distinct-by-person-oid nil))

(defn get-application-version-changes [application-key]
  (let [all-versions         (exec-db :db
                                      yesql-get-application-versions
                                      {:application_key application-key})
        all-versions-paired  (map vector all-versions (rest all-versions))
        get-koodisto-options (memoize koodisto/get-koodisto-options)]
    (map (fn [[older-application newer-application]]
           (let [older-version-answers (util/application-answers-by-key older-application)
                 newer-version-answers (util/application-answers-by-key newer-application)
                 answer-keys           (set (concat (keys older-version-answers) (keys newer-version-answers)))
                 lang                  (or (-> newer-application :lang keyword) :fi)
                 form-fields           (util/form-fields-by-id (forms/get-form-by-application newer-application))]
             (into {}
                   (for [key answer-keys
                         :let [old-value (-> older-version-answers key :value)
                               new-value (-> newer-version-answers key :value)
                               field     (key form-fields)]
                         :when (not= old-value new-value)]
                     {key {:label (-> field :label lang)
                           :old   (util/populate-answer-koodisto-values old-value field get-koodisto-options)
                           :new   (util/populate-answer-koodisto-values new-value field get-koodisto-options)}}))))
         all-versions-paired)))
