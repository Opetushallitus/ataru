(ns ataru.applications.application-store
  (:require [ataru.application.application-states :as application-states]
            [cheshire.core :as json]
            [ataru.application.review-states :refer [incomplete-states] :as application-review-states]
            [ataru.component-data.higher-education-base-education-module :refer [higher-completed-base-education-id attachment-always-visible?]]
            [ataru.db.db :as db]
            [ataru.koodisto.koodisto-codes :refer [finland-country-code]]
            [ataru.dob :as dob]
            [ataru.forms.form-store :as forms]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.log.audit-log :as audit-log]
            [ataru.schema.form-schema :as schema]
            [ataru.util :refer [answers-by-key] :as util]
            [ataru.person-service.person-service :as person-service]
            [ataru.selection-limit.selection-limit-service :as selection-limit]
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
            [taoensso.timbre :refer [info warn]]
            [yesql.core :refer [defqueries]]
            [ataru.config.core :refer [config]])
  (:import [java.time
            LocalDateTime
            ZoneId]
           org.postgresql.util.PSQLException
           java.time.format.DateTimeFormatter))

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

(def unique-violation "23505")

(defn add-new-secret-to-application-in-tx
  [connection application-key]
  (loop []
    (let [secret     (crypto/url-part 34)
          collision? (try
                       (yesql-add-application-secret<! {:application_key application-key
                                                        :secret          secret}
                                                       {:connection connection})
                       false
                       (catch PSQLException e
                         (if (= unique-violation (.getSQLState e))
                           true
                           (throw e))))]
      (if collision?
        (do (warn "Application secret collision")
            (recur))
        secret))))

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
                        :state           (if (empty? answer)
                                           "attachment-missing"
                                           "not-checked")
                        :updated?        value-changed?}]
    (map #(assoc review-base :hakukohde %)
         (hakukohde-oids-for-attachment-review attachment-field hakutoiveet fields-by-id ylioppilastutkinto? excluded-attachment-ids-when-yo-and-jyemp))))

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
                 (-> field :params :hidden not)
                 (or (not (contains? field :followup-of))
                     (followup-option-selected? field answers))))
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
  (yesql-delete-application-attachment-reviews!
    {:application_key                            application-key
     :attachment_key_and_applied_hakukohde_array (->> reviews
                                                      (map (fn [review]
                                                               [(:attachment_key review) (:hakukohde review)]))
                                                      (json/generate-string))}
    connection))

(defn store-reviews [reviews update? connection]
  (doseq [review reviews]
    ((if (:updated? review)
       yesql-update-attachment-hakukohde-review!
       yesql-save-attachment-review!)
     (dissoc review :updated?) connection)))

(defn- create-attachment-hakukohde-reviews-for-application
  [application applied-hakukohteet old-answers form update? connection]
  (let [flat-form-content   (-> form :content util/flatten-form-fields)
        fields-by-id        (util/form-fields-by-id form)
        excluded-attachment-ids-when-yo-and-jyemp (util/attachment-ids-from-children (:content form)
                                                                                     higher-completed-base-education-id
                                                                                     attachment-always-visible?)
        answers-by-key      (-> application :content :answers util/answers-by-key)
        visible-attachments (filter-visible-attachments answers-by-key flat-form-content)
        reviews             (create-application-attachment-reviews
                             (:key application)
                             visible-attachments
                             answers-by-key
                             old-answers
                             applied-hakukohteet
                             update?
                             fields-by-id
                             excluded-attachment-ids-when-yo-and-jyemp)]
    (store-reviews reviews update? connection)
    (when update?
      (delete-orphan-attachment-reviews (:key application)
                                        reviews
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
      (add-new-secret-to-application-in-tx conn (:key new-application)))
    (unwrap-application new-application)))

(defn- get-latest-version-and-lock-for-update [secret lang conn]
  (if-let [application (first (yesql-get-latest-version-by-secret-lock-for-update {:secret secret} {:connection conn}))]
    (unwrap-application application)
    (throw (ex-info "No existing form found when updating" {:secret secret}))))

(defn- get-latest-version-for-virkailija-edit-and-lock-for-update [virkailija-secret lang conn]
  (if-let [application (first (yesql-get-latest-version-by-virkailija-secret-lock-for-update {:virkailija_secret virkailija-secret} {:connection conn}))]
    (unwrap-application application)
    (throw (ex-info "No existing form found when updating as virkailija" {:virkailija-secret virkailija-secret}))))

(defn- get-latest-version-for-virkailija-edit-and-lock-for-rewrite [virkailija-secret lang conn]
  (if-let [application (first (yesql-get-latest-version-by-virkailija-secret-lock-for-rewrite {:virkailija_secret virkailija-secret} {:connection conn}))]
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

(defn add-application [new-application applied-hakukohteet form session]
    (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
      (info (str "Inserting new application"))
      (let [selection-id   (:selection-id new-application)
            virkailija-oid (when-let [secret (:virkailija-secret new-application)]
                             (get-virkailija-oid-for-create-secret conn secret))

            {:keys [id key] :as new-application} (add-new-application-version new-application
                                                   true
                                                   applied-hakukohteet
                                                   nil
                                                   form
                                                   false
                                                   conn)
            connection     {:connection conn}]
        (audit-log/log {:new       new-application
                        :operation audit-log/operation-new
                        :session   session
                        :id        {:email (util/extract-email new-application)}})
        (yesql-add-application-event<! {:application_key          key
                                        :event_type               (if (some? virkailija-oid)
                                                                    "received-from-virkailija"
                                                                    "received-from-applicant")
                                        :new_review_state         nil
                                        :virkailija_oid           virkailija-oid
                                        :virkailija_organizations nil
                                        :hakukohde                nil
                                        :review_key               nil}
          connection)
        (yesql-add-application-review! {:application_key key
                                        :state           application-review-states/initial-application-review-state}
          connection)

        (selection-limit/permanent-select-on-store-application key new-application selection-id form connection)

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

(defn update-application [{:keys [lang secret virkailija-secret selection-id] :as new-application} applied-hakukohteet form session]
  {:pre [(or (not-blank? secret)
             (not-blank? virkailija-secret))]}
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [updated-by-applicant? (not-blank? secret)
          [virkailija-oid rewrite-secret?] (if updated-by-applicant?
                                             [nil nil]
                                             (if-let [oid (get-virkailija-oid-for-rewrite-secret conn virkailija-secret)]
                                               [oid true]
                                               [(get-virkailija-oid-for-update-secret conn virkailija-secret) false]))
          old-application       (cond
                                  rewrite-secret?
                                  (get-latest-version-for-virkailija-edit-and-lock-for-rewrite virkailija-secret lang conn)

                                  updated-by-applicant?
                                  (get-latest-version-and-lock-for-update secret lang conn)

                                  :else
                                  (get-latest-version-for-virkailija-edit-and-lock-for-update virkailija-secret lang conn))
          {:keys [id key] :as new-application} (add-new-application-version
                                                 (merge-applications new-application old-application)
                                                 updated-by-applicant?
                                                 applied-hakukohteet
                                                 (-> old-application :answers util/answers-by-key)
                                                 form
                                                 true
                                                 conn)]
      (info (str "Updating application with key "
                 (:key old-application)
                 " based on valid application secret, retaining key" (when-not updated-by-applicant? " and secret") " from previous version"))
      (yesql-add-application-event<! {:application_key          key
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

      (audit-log/log {:new       (application->loggable-form new-application)
                      :old       (application->loggable-form old-application)
                      :operation audit-log/operation-modify
                      :session   session
                      :id        {:applicationOid key}})
      id)))

(defn ->name-query-value
  [name]
  (->> (-> name
           (clojure.string/replace #"[&\|!<>:*]" "")
           (clojure.string/split #"\s+"))
       (remove clojure.string/blank?)
       (map #(str % ":*"))
       (clojure.string/join " & ")))

(defn- query->db-query
  [connection query sort]
  (let [query (cond-> query
                      (contains? query :hakukohde)
                      (update :hakukohde
                              #(some->> (seq %)
                                        to-array
                                        (.createArrayOf (:connection connection) "varchar")))
                      (contains? query :ensisijainen-hakukohde)
                      (update :ensisijainen-hakukohde
                              #(some->> (seq %)
                                        to-array
                                        (.createArrayOf (:connection connection) "varchar")))
                      (contains? query :ensisijaisesti-hakukohteissa)
                      (update :ensisijaisesti-hakukohteissa
                              #(some->> (seq %)
                                        to-array
                                        (.createArrayOf (:connection connection) "varchar"))))
        comp  (if (= "asc" (:order sort)) ">" "<")
        order (if (= "asc" (:order sort)) "ASC" "DESC")]
    (cons
     (str "SELECT a.id,
       a.person_oid                     AS \"person-oid\",
       a.key,
       a.lang,
       a.preferred_name                 AS \"preferred-name\",
       a.last_name                      AS \"last-name\",
       a.created_time                   AS \"created-time\",
       a.haku,
       a.hakukohde,
       a.ssn,
       to_char(a.dob, 'dd.MM.YYYY')     AS \"dob\",
       hcbe.value                       AS \"base-education\",
       ar.state                         AS state,
       ar.score                         AS score,
       a.form_id                        AS form,
       ae.eligibility_set_automatically AS \"eligibility-set-automatically\",
       ae.new_modifications_count       AS \"new-application-modifications\",
       a.submitted,
       lf.organization_oid              AS \"organization-oid\",
       (SELECT jsonb_agg(jsonb_build_object('requirement', requirement,
                                            'state', state,
                                            'hakukohde', hakukohde))
        FROM application_hakukohde_reviews ahr
        WHERE ahr.application_key = a.key) AS \"application-hakukohde-reviews\",
       (SELECT jsonb_agg(jsonb_build_object('attachment-key', attachment_key,
                                            'state', state,
                                            'hakukohde', hakukohde))
        FROM application_hakukohde_attachment_reviews aar
        WHERE aar.application_key = a.key) AS \"application-attachment-reviews\"
FROM applications AS a
LEFT JOIN applications AS la ON la.key = a.key AND la.id > a.id
JOIN application_reviews AS ar ON a.key = ar.application_key
JOIN forms AS f ON f.id = a.form_id
JOIN LATERAL (SELECT organization_oid
              FROM forms
              WHERE key = f.key
              ORDER BY id DESC
              LIMIT 1) AS lf ON true
LEFT JOIN LATERAL (SELECT value->'value' AS value
                   FROM jsonb_array_elements(a.content->'answers')
                   WHERE value->>'key' = 'higher-completed-base-education'
                   LIMIT 1) AS hcbe ON true
JOIN LATERAL (SELECT coalesce(array_agg(ae.hakukohde) FILTER (WHERE ae.review_key = 'eligibility-state' AND
                                                                    ae.event_type = 'eligibility-state-automatically-changed'), '{}')
                       AS eligibility_set_automatically,
                     count(*) FILTER (WHERE ae.review_key = 'processing-state' AND
                                            ae.new_review_state = 'information-request' AND
                                            ae.time < a.created_time)
                       AS new_modifications_count
              FROM (SELECT DISTINCT ON (hakukohde, review_key) hakukohde, review_key, event_type, new_review_state, time
                    FROM application_events
                    WHERE application_key = a.key
                    ORDER BY hakukohde, review_key, id DESC) AS ae) AS ae ON true
WHERE la.key IS NULL\n"
          (when (contains? query :form)
            "      AND (f.key = ? AND a.haku IS NULL)\n")
          (when (contains? query :application-oid)
            "      AND a.key = ?\n")
          (when (contains? query :person-oid)
            "      AND a.person_oid = ?\n")
          (when (contains? query :name)
            "      AND to_tsvector('unaccent_simple', a.preferred_name || ' ' || a.last_name) @@ to_tsquery('unaccent_simple', ?)\n")
          (when (contains? query :email)
            "      AND lower(a.email) = lower(?)\n")
          (when (contains? query :dob)
            "      AND a.dob = to_date(?, 'DD.MM.YYYY')\n")
          (when (contains? query :ssn)
            "      AND a.ssn = ?\n")
          (when (contains? query :haku)
            "      AND a.haku = ?\n")
          (when (contains? query :hakukohde)
            "      AND a.hakukohde && ?\n")
          (when (contains? query :ensisijainen-hakukohde)
            (if (contains? query :ensisijaisesti-hakukohteissa)
              "      AND (SELECT t.h
           FROM unnest(a.hakukohde) WITH ORDINALITY AS t(h, i)
           WHERE t.h = ANY(?)
           ORDER BY t.i ASC
           LIMIT 1) = ANY(?)\n"
              "      AND a.hakukohde[1] = ANY(?)\n"))
          (when (contains? sort :offset)
            (case (:order-by sort)
              "submitted"
              (str "      AND (date_trunc('second', a.submitted AT TIME ZONE 'Europe/Helsinki'), a.key) "
                   comp
                   " (date_trunc('second', ? AT TIME ZONE 'Europe/Helsinki'), ?)\n")
              "created-time"
              (str "      AND (date_trunc('second', a.created_time AT TIME ZONE 'Europe/Helsinki'), a.key) "
                   comp
                   " (date_trunc('second', ? AT TIME ZONE 'Europe/Helsinki'), ?)\n")
              "applicant-name"
              (str "      AND (a.last_name, a.preferred_name, a.key) "
                   comp
                   " (? COLLATE \"fi_FI\", ? COLLATE \"fi_FI\", ?)\n")))
          (case (:order-by sort)
            "submitted"
            (str "ORDER BY date_trunc('second', a.submitted AT TIME ZONE 'Europe/Helsinki') " order ",\n"
                 "         a.key " order "\n")
            "created-time"
            (str "ORDER BY date_trunc('second', a.created_time AT TIME ZONE 'Europe/Helsinki') " order ",\n"
                 "         a.key " order "\n")
            "applicant-name"
            (str "ORDER BY a.last_name COLLATE \"fi_FI\" " order ",\n"
                 "         a.preferred_name COLLATE \"fi_FI\" " order ",\n"
                 "         a.key " order "\n"))
          "LIMIT 1000;")
     (concat (keep #(get query %) [:form
                                   :application-oid
                                   :person-oid
                                   :name
                                   :email
                                   :dob
                                   :ssn
                                   :haku
                                   :hakukohde
                                   :ensisijaisesti-hakukohteissa
                                   :ensisijainen-hakukohde])
             (when (contains? sort :offset)
               (case (:order-by sort)
                 "submitted"
                 [(:submitted (:offset sort)) (:key (:offset sort))]
                 "created-time"
                 [(:created-time (:offset sort)) (:key (:offset sort))]
                 "applicant-name"
                 [(:last-name (:offset sort)) (:preferred-name (:offset sort)) (:key (:offset sort))]))))))

(defn get-application-heading-list
  [query sort]
  (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
    (jdbc/query connection (query->db-query connection query sort))))

(defn get-full-application-list-by-person-oid-for-omatsivut-and-refresh-old-secrets
  [person-oid]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (->> (yesql-get-application-list-by-person-oid-for-omatsivut
          {:person_oid             person-oid
           :secret_link_valid_days (-> config :public-config :secret-link-valid-days)}
          {:connection conn})
         ->kebab-case-kw
         (mapv #(if (nil? (:secret %))
                  (do (info "Refreshing secret for application" (:key %))
                      (assoc % :secret (add-new-secret-to-application-in-tx conn (:key %))))
                  %)))))

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

(defn selection-state-used? [haku-oid]
  (:exists (first (exec-db :db yesql-selection-state-used {:haku_oid haku-oid}))))

(defn get-application [application-id]
  (unwrap-application (first (exec-db :db yesql-get-application-by-id {:application_id application-id}))))

(defn get-latest-application-by-key-in-tx
  [connection application-key]
  (-> (yesql-get-latest-application-by-key
       {:application_key application-key}
       {:connection connection})
      first
      unwrap-application))

(defn get-latest-application-by-key [application-key]
  (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
    (get-latest-application-by-key-in-tx connection application-key)))

(defn get-application-hakukohde-reviews
  [application-key]
  (mapv ->kebab-case-kw (exec-db :db yesql-get-application-hakukohde-reviews {:application_key application-key})))

(defn get-application-attachment-reviews
  [application-key]
  (mapv ->kebab-case-kw (exec-db :db yesql-get-application-attachment-reviews {:application_key application-key})))

(defn get-latest-application-by-secret [secret]
  (when-let [application (->> (exec-db :db
                                       yesql-get-latest-application-by-secret
                                       {:secret secret
                                        :secret_link_valid_days (-> config :public-config :secret-link-valid-days)})
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

(defn get-latest-application-for-virkailija-rewrite-edit [virkailija-secret]
  (when-let [application (->> (exec-db :db yesql-get-latest-application-by-virkailija-rewrite-secret {:virkailija_secret virkailija-secret})
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
  (:secret (first (->> (exec-db :db yesql-get-latest-application-secret {})))))

(defn alter-application-hakukohteet-with-secret
  [secret new-hakukohteet]
  (when-not (= (exec-db :db yesql-set-application-hakukohteet-by-secret! {:secret secret :hakukohde new-hakukohteet}) 0)
    secret))

(defn add-new-secret-to-application
  [application-key]
  (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
    (add-new-secret-to-application-in-tx connection application-key)))

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
                              (unwrap-application))]
      (add-new-secret-to-application-in-tx conn application-key)
      (:id application))))

(defn get-application-events [application-key]
  (->> (exec-db :db yesql-get-application-events {:application_key application-key})
       (mapv ->kebab-case-kw)
       (mapv #(if (nil? (:virkailija-organizations %))
                (dissoc % :virkailija-organizations)
                %))))

(defn- auditlog-review-modify
  [review old-value session]
  (audit-log/log {:new       review
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

(defn save-application-review [review session]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [connection      {:connection conn}
          app-key         (:application-key review)
          old-review      (when-let [data (first (yesql-get-application-review {:application_key app-key} connection))]
                            {:application_key (:application_key data)
                             :state           (:state data)
                             :score           (some-> (:score data) (.doubleValue))})
          review-to-store {:application_key (:application-key review)
                           :state           (:state review)
                           :score           (:score review)}]
      (when (not= old-review review-to-store)
        (yesql-save-application-review! review-to-store connection)
        (auditlog-review-modify review-to-store old-review session))
      (when (not= (:state old-review) (:state review-to-store))
        (let [event {:application_key          app-key
                     :event_type               "review-state-change"
                     :new_review_state         (:state review-to-store)
                     :virkailija_oid           (-> session :identity :oid)
                     :virkailija_organizations (edit-application-right-organizations->json session)
                     :hakukohde                nil
                     :review_key               nil}]
          (:id (yesql-add-application-event<! event connection)))))))

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
                                (let [event {:application_key          application-key
                                             :event_type               "hakukohde-review-state-change"
                                             :new_review_state         (:state review-to-store)
                                             :review_key               hakukohde-review-requirement
                                             :hakukohde                (:hakukohde review-to-store)
                                             :virkailija_oid           (-> session :identity :oid)
                                             :virkailija_organizations (edit-application-right-organizations->json session)}]
                                  (yesql-add-application-event<! event connection))))))

(defn save-payment-obligation-automatically-changed
  [application-key hakukohde-oid hakukohde-review-requirement hakukohde-review-state]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [connection                  {:connection conn}
          review-to-store             {:application_key application-key
                                       :requirement     hakukohde-review-requirement
                                       :state           hakukohde-review-state
                                       :hakukohde       hakukohde-oid}
          automatically-changed?      (->> (yesql-get-application-events {:application_key application-key} connection)
                                           (filter #(and (= hakukohde-oid (:hakukohde %))
                                                         (= hakukohde-review-requirement (:review_key %))))
                                           last
                                           :event_type
                                           (= "payment-obligation-automatically-changed"))
          existing-requirement-review (first (yesql-get-existing-requirement-review review-to-store connection))]
      (when (and (not= (:state review-to-store) (:state existing-requirement-review))
                 (or (nil? (:state existing-requirement-review))
                     (= "unreviewed" (:state existing-requirement-review))
                     automatically-changed?))
        (yesql-upsert-application-hakukohde-review! review-to-store connection)
        (let [event {:application_key          application-key
                     :event_type               "payment-obligation-automatically-changed"
                     :new_review_state         (:state review-to-store)
                     :review_key               hakukohde-review-requirement
                     :hakukohde                (:hakukohde review-to-store)
                     :virkailija_oid           nil
                     :virkailija_organizations nil}]
          (yesql-add-application-event<! event connection))))))

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
          (let [event {:application_key          application-key
                       :event_type               "attachment-review-state-change"
                       :new_review_state         (:state review-to-store)
                       :review_key               attachment-key
                       :hakukohde                (:hakukohde review-to-store)
                       :virkailija_oid           (-> session :identity :oid)
                       :virkailija_organizations (edit-application-right-organizations->json session)}]
            (yesql-add-application-event<! event connection)))
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
  (let [m {"pohjakoulutus_yo"              ["yo"]
           "pohjakoulutus_yo_ammatillinen" ["yo" "am"]
           "pohjakoulutus_am"              ["am"]
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

(defn- unwrap-hakurekisteri-application
  [{:keys [key haku hakukohde person_oid lang email content payment-obligations eligibilities]}]
  (let [answers  (answers-by-key (:answers content))
        foreign? (not= finland-country-code (-> answers :country-of-residence :value))]
    {:oid                         key
     :personOid                   person_oid
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
     :kkPohjakoulutus             (kk-base-educations answers)
     :sahkoisenAsioinninLupa      (= "Kyllä" (-> answers :sahkoisen-asioinnin-lupa :value))
     :valintatuloksenJulkaisulupa (= "Kyllä" (-> answers :valintatuloksen-julkaisulupa :value))
     :koulutusmarkkinointilupa    (= "Kyllä" (-> answers :koulutusmarkkinointilupa :value))
     :korkeakoulututkintoVuosi    (korkeakoulututkinto-vuosi answers)
     :paymentObligations          (reduce-kv #(assoc %1 (name %2) %3) {} payment-obligations)
     :eligibilities               (reduce-kv #(assoc %1 (name %2) %3) {} eligibilities)}))

(defn get-hakurekisteri-applications ;; deprecated, use suoritusrekisteri-applications
  [haku-oid hakukohde-oids person-oids modified-after]
  (->> (jdbc/with-db-connection [conn {:datasource (db/get-datasource :db)}]
         (yesql-applications-for-hakurekisteri
          {:has_haku_oid       (some? haku-oid)
           :haku_oid           haku-oid
           :has_hakukohde_oids (not (empty? hakukohde-oids))
           :has_person_oids    (not (empty? person-oids))
           :hakukohde_oids     (->> hakukohde-oids
                                    (to-array)
                                    (.createArrayOf (:connection conn) "varchar"))
           :person_oids        (->> person-oids
                                    to-array
                                    (.createArrayOf (:connection conn) "text"))
           :has_modified_after (some? modified-after)
           :modified_after     (some-> modified-after
                                       (LocalDateTime/parse (DateTimeFormatter/ofPattern "yyyyMMddHHmm"))
                                       (.atZone (ZoneId/of "Europe/Helsinki"))
                                       .toOffsetDateTime)}
          {:connection conn}))
       (map unwrap-hakurekisteri-application)))

(defn suoritusrekisteri-applications
  [haku-oid hakukohde-oids person-oids modified-after offset]
  (let [as (->> (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
                  (yesql-suoritusrekisteri-applications
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

(defn get-external-applications ;; deprecated, use valinta-tulos-service-applications
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

(defn valinta-tulos-service-applications
  [haku-oid hakukohde-oid hakemus-oids offset]
  (let [as (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
             (map #(hash-map :oid (:oid %)
                             :hakuOid (:haku %)
                             :hakukohdeOids (:hakukohde %)
                             :henkiloOid (:person-oid %)
                             :asiointikieli (:asiointikieli %)
                             :email (:email %))
                  (yesql-valinta-tulos-service-applications
                   {:haku_oid      haku-oid
                    :hakukohde_oid hakukohde-oid
                    :hakemus_oids  (some->> (seq hakemus-oids)
                                            to-array
                                            (.createArrayOf (:connection connection) "text"))
                    :offset        offset}
                   {:connection connection})))]
    (merge {:applications as}
           (when-let [a (first (drop 999 as))]
             {:offset (:oid a)}))))

(defn- convert-asiointikieli [kielikoodi]
      (cond
        (= "fi" kielikoodi) {:kieliKoodi "fi" :kieliTyyppi "suomi"}
        (= "sv" kielikoodi) {:kieliKoodi "sv" :kieliTyyppi "svenska"}
        (= "en" kielikoodi) {:kieliKoodi "en" :kieliTyyppi "english"}
        :else {:kieliKoodi "" :kieliTyyppi ""}))

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
         (yesql-valinta-ui-applications (-> (merge {:application_oids nil
                                                    :name             nil
                                                    :haku             nil
                                                    :hakukohde        nil}
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
                           (application-states/get-all-reviews-for-requirement "processing-state" application (when hakukohde-oid [hakukohde-oid])))
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
      (yesql-upsert-application-hakukohde-review! new-review connection)
      (yesql-add-application-event<! (assoc new-event :hakukohde (:hakukohde new-review))
                                     connection))
    (when new-reviews
      {:new       new-event
       :id        {:applicationOid application-key
                   :hakukohdeOid   hakukohde-oid
                   :requirement    "processing-state"}
       :operation audit-log/operation-modify
       :session   session})))

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
  (info "Mass updating" (count application-keys) "applications from" from-state "to" to-state "with hakukohde" hakukohde-oid)
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
      (-> {:event_type               nil
           :new_review_state         nil
           :virkailija_oid           (-> session :identity :oid)
           :virkailija_organizations (edit-application-right-organizations->json session)
           :hakukohde                nil
           :review_key               nil}
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
  (-> (exec-db :db yesql-add-review-note<! {:application_key          (:application-key note)
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
  [{:keys [haku-oid hakemus-oid henkilo-oid hakukohde-oids content]}]
  (let [answers (answers-by-key (:answers content))]
    {:hakemus_oid     hakemus-oid
     :haku_oid        haku-oid
     :henkilo_oid     henkilo-oid
     :hakukohde_oids  hakukohde-oids
     :kotikunta       (-> answers :home-town :value)
     :asuinmaa        (-> answers :country-of-residence :value)}))

(defn get-application-info-for-tilastokeskus [haku-oid hakukohde-oid]
  (->> (exec-db :db yesql-tilastokeskus-applications {:haku_oid haku-oid :hakukohde_oid hakukohde-oid})
       (map unwrap-tilastokeskus-application)))

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
                          (or (= "textField" fieldType)
                              (= "attachment" fieldType))
                          indexed-by-value-order
                          :else
                          not-indexed)]
       (into acc (cond (and (sequential? value)
                            (every? sequential? value))
                       (indexed-by-question-group index-fn key value)
                       (and (sequential? value)
                            (or (= "attachment" fieldType)
                                (= "multipleChoice" fieldType)
                                (= "textField" fieldType)))
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
        (assoc :maksuvelvollisuus (reduce-kv #(assoc %1 (name %2) %3) {} (:maksuvelvollisuus application)))
        (assoc :keyValues (merge keyword-values eligibilities-by-hakutoive))
        (assoc :hakutoiveet (unwrap-external-application-hakutoiveet application))
        (dissoc :hakukohde)
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

(defn get-application-keys-for-person-oid [person-oid]
  (exec-db :db yesql-get-latest-application-keys-distinct-by-person-oid {:person_oid person-oid}))

(defn get-application-version-changes [koodisto-cache application-key]
  (let [all-versions         (exec-db :db
                                      yesql-get-application-versions
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
                   (for [key answer-keys
                         :let [old-value (-> older-version-answers key :value)
                               new-value (-> newer-version-answers key :value)
                               field     (key form-fields)]
                         :when (not= old-value new-value)]
                     {key {:label (-> field :label lang)
                           :old   (util/populate-answer-koodisto-values old-value field get-koodisto-options)
                           :new   (util/populate-answer-koodisto-values new-value field get-koodisto-options)}}))))
         all-versions-paired)))
