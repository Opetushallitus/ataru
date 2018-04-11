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
(defqueries "sql/virkailija-credentials-queries.sql")

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

(defn generate-new-application-secret
  [connection]
  (loop [secret (crypto/url-part 34)]
    (if-not (application-exists-with-secret-tx? secret connection)
      secret
      (recur (crypto/url-part 34)))))

(defn- create-attachment-reviews
  [attachment-field application-key hakutoiveet]
  (let [review-base {:application_key application-key
                     :attachment_key  (:id attachment-field)
                     :state           "not-checked"}]
    (map #(assoc review-base :hakukohde %)
         (cond
           (not-empty (:belongs-to-hakukohteet attachment-field))
           (clojure.set/intersection (set hakutoiveet)
                                     (-> attachment-field :belongs-to-hakukohteet set))

           (not-empty hakutoiveet)
           hakutoiveet

           :else ["form"]))))

(defn- create-attachment-hakukohde-reviews-for-application
  [application connection]
  (doseq [review (->> (forms/fetch-by-id (:form_id application))
                      :content
                      util/flatten-form-fields
                      (filter #(= "attachment" (:fieldType %)))
                      (mapcat #(create-attachment-reviews % (:key application) (:hakukohde application))))]
    (yesql-save-attachment-review! review connection)))

(defn- add-new-application-version
  "Add application and also initial metadata (event for receiving application, and initial review record)"
  [application create-new-secret? conn]
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
    (create-attachment-hakukohde-reviews-for-application new-application {:connection conn})
    (when create-new-secret?
      (yesql-add-application-secret!
        {:application_key (:key new-application)
         :secret          (generate-new-application-secret connection)}
        connection))
    (unwrap-application new-application)))

(def ^:private email-pred (comp (partial = "email") :key))

(defn- extract-email [application]
  (->> (:answers application)
       (filter email-pred)
       (first)
       :value))

(defn- get-latest-version-and-lock-for-update [secret lang conn]
  (if-let [application (first (yesql-get-latest-version-by-secret-lock-for-update {:secret secret} {:connection conn}))]
    (unwrap-application application)
    (throw (ex-info "No existing form found when updating" {:secret secret}))))

(defn- get-latest-version-for-virkailija-edit-and-lock-for-update [virkailija-secret lang conn]
  (if-let [application (first (yesql-get-latest-version-by-virkailija-secret-lock-for-update {:virkailija_secret virkailija-secret} {:connection conn}))]
    (unwrap-application application)
    (throw (ex-info "No existing form found when updating as virkailija" {:virkailija-secret virkailija-secret}))))

(defn add-application [new-application]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (info (str "Inserting new application"))
    (let [{:keys [id key] :as new-application} (add-new-application-version new-application true conn)
          connection                {:connection conn}]
      (audit-log/log {:new       new-application
                      :operation audit-log/operation-new
                      :id        (extract-email new-application)})
      (yesql-add-application-event<! {:application_key  key
                                      :event_type       "received-from-applicant"
                                      :new_review_state nil
                                      :virkailija_oid   nil
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
  (cond-> (select-keys application [:id :key :form-id :answers])
    (some? form)
    (form->form-id)))

(defn- merge-applications [new-application old-application]
  (merge new-application
         (select-keys old-application [:key :haku :person-oid :secret])))

(defn- not-blank? [x]
  (not (clojure.string/blank? x)))

(defn- get-virkailija-oid [virkailija-secret application-key conn]
  (->> (yesql-get-virkailija-oid {:virkailija_secret virkailija-secret
                                  :application_key   application-key}
                                 {:connection conn})
       (map :oid)
       (first)))

(defn update-application [{:keys [lang secret virkailija-secret] :as new-application}]
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
                                                 conn)
          virkailija-oid        (when-not updated-by-applicant? (get-virkailija-oid virkailija-secret key conn))]
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
                                   (extract-email new-application)
                                   virkailija-oid)})
      id)))

(defn get-application-list-by-form
  "Only list with header-level info, not answers. Does NOT include applications associated with any hakukohde."
  [form-key]
  (->> (exec-db :db yesql-get-application-list-by-form {:form_key form-key})
       (map ->kebab-case-kw)))

(defn- name-search-query [name]
  (->> (clojure.string/split name #"\s+")
       (remove clojure.string/blank?)
       (map #(str % ":*"))
       (clojure.string/join " & ")))

(defn get-application-heading-list
  ([query-key query-value organization-oids]
   (let [have-organization? (not-empty organization-oids)
         parsed-value       (case :query-key
                              :name (name-search-query query-value)
                              query-value)]
     (->> (exec-db :db yesql-get-application-list-for-virkailija
                   {:query_type                   (if have-organization? "ORGS" "ALL")
                    :query_key                    (name query-key)
                    :query_value                  parsed-value
                    :authorized_organization_oids (if have-organization? organization-oids [""])})
          (map ->kebab-case-kw))))
  ([query-key query-value]
   (get-application-heading-list query-key query-value nil)))

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
     :kansalaisuus (-> answers :nationality :value)
     :aidinkieli   (-> answers :language :value)
     :matkapuhelin (-> answers :phone :value)
     :email        email
     :lahiosoite   (-> answers :address :value)
     :postinumero  (-> answers :postal-code :value)
     :passinNumero (-> answers :passport-number :value)
     :idTunnus     (-> answers :national-id-number :value)}))

(defn onr-applications [person-oid organizations]
  (->> (exec-db :db yesql-onr-applications
                {:person_oid person-oid
                 :query_type (if (nil? organizations) "ALL" "ORGS")
                 :authorized_organization_oids (if (nil? organizations)
                                                 [""]
                                                 organizations)})
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
       (map ->kebab-case-kw)))

(defn get-application-review [application-key]
  (->> (exec-db :db yesql-get-application-review {:application_key application-key})
       (map ->kebab-case-kw)
       (first)))

(defn get-application-reviews-by-keys [application-keys]
  (map ->kebab-case-kw
       (exec-db :db yesql-get-application-reviews-by-keys {:application_keys application-keys})))

(defn get-application [application-id]
  (unwrap-application (first (exec-db :db yesql-get-application-by-id {:application_id application-id}))))

(defn get-latest-application-by-key [application-key organization-oids]
  (-> (exec-db :db yesql-get-latest-application-by-key {:application_key              application-key
                                                        :query_type                   "ORGS"
                                                        :authorized_organization_oids organization-oids})
      (first)
      (unwrap-application)))

(defn get-latest-application-by-key-unrestricted [application-key]
  (-> (exec-db :db yesql-get-latest-application-by-key {:application_key              application-key
                                                        :query_type                   "ALL"
                                                        :authorized_organization_oids [""]})
      (first)
      (unwrap-application)))

(defn get-latest-application-by-key-with-hakukohde-reviews
  [application-key]
  (-> (exec-db :db
               yesql-get-latest-application-by-key-with-hakukohde-reviews
               {:application_key              application-key
                :query_type                   "ALL"
                :authorized_organization_oids [""]})
      (first)
      (unwrap-application)))

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
          application     (get-latest-application-by-key-unrestricted application-key)
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
          application     (get-latest-application-by-key-unrestricted application-key)
          new-secret      (generate-new-application-secret connection)]
      (yesql-add-application-secret! {:application_key application-key :secret new-secret} connection)
      (:id application))))

(defn get-application-events [application-key]
  (mapv ->kebab-case-kw (exec-db :db yesql-get-application-events {:application_key application-key})))

(defn get-application-organization-oid [application-key]
  (:organization_oid (first (exec-db :db yesql-get-application-organization-by-key {:application_key application-key}))))

(defn get-organization-oids-of-applications-of-persons [person-oids]
  (if (empty? person-oids)
    #{}
    (set (map :organization_oid
              (exec-db :db
                       yesql-organization-oids-of-applications-of-persons
                       {:person_oids person-oids})))))

(defn- auditlog-review-modify
  [review old-value session]
  (audit-log/log {:new              review
                  :old              old-value
                  :id               (get-in session [:identity :username])
                  :operation        audit-log/operation-modify
                  :organization-oid (get-in session [:identity :organizations 0 :oid])}))

(defn- store-and-log-review-event
  [connection event session]
  (yesql-add-application-event<! event connection)
  (audit-log/log {:new              event
                  :id               (get-in session [:identity :username])
                  :operation        audit-log/operation-new
                  :organization-oid (get-in session [:identity :organizations 0 :oid])}))

(defn save-application-review [review session]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [connection       {:connection conn}
          app-key          (:application-key review)
          old-review       (first (yesql-get-application-review {:application_key app-key} connection))
          review-to-store  (transform-keys ->snake_case review)
          username         (get-in session [:identity :username])
          organization-oid (get-in session [:identity :organizations 0 :oid])]
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
                                  existing-requirement-review (yesql-get-existing-requirement-review review-to-store connection)
                                  username                    (get-in session [:identity :username])
                                  organization-oid            (get-in session [:identity :organizations 0 :oid])]
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
  [organization-oids]
  (mapv ->kebab-case-kw (exec-db :db yesql-get-haut-and-hakukohteet-from-applications
                                 {:incomplete_states incomplete-states
                                  :query_type "ORGS"
                                  :authorized_organization_oids organization-oids})))

(defn get-all-haut
  []
  (mapv ->kebab-case-kw (exec-db :db yesql-get-haut-and-hakukohteet-from-applications
                                 {:incomplete_states incomplete-states
                                  :query_type "ALL"
                                  :authorized_organization_oids [""]})))

(defn get-direct-form-haut
  [organization-oids]
  (->> (exec-db :db yesql-get-direct-form-haut
                {:incomplete_states            incomplete-states
                 :query_type                   "ORGS"
                 :authorized_organization_oids organization-oids})
       (mapv ->kebab-case-kw)
       (reduce #(assoc %1 (:key %2) %2) {})))

(defn get-all-direct-form-haut
  []
  (->> (exec-db :db yesql-get-direct-form-haut
                {:incomplete_states            incomplete-states
                 :query_type                   "ALL"
                 :authorized_organization_oids [""]})
       (mapv ->kebab-case-kw)
       (reduce #(assoc %1 (:key %2) %2) {})))

(defn add-application-feedback
  [feedback]
  (->kebab-case-kw
    (exec-db :db yesql-add-application-feedback<! (transform-keys ->snake_case feedback))))

(defn get-hakija-secret-by-virkailija-secret [virkailija-secret]
  (-> (exec-db :db yesql-get-hakija-secret-by-virkailija-secret {:virkailija_secret virkailija-secret})
      (first)
      :secret))

(defn- payment-obligation-to-application [application payment-obligations]
  (let [obligations (reduce (fn [r o]
                              (assoc r (:hakukohde o) (:state o)))
                            {}
                            (get payment-obligations (:oid application)))]
    (assoc application :paymentObligations obligations)))

(defn- payment-obligations-for-applications [hakemus-oids]
  (->> (exec-db :db
                yesql-get-payment-obligation-for-applications
                {:hakemus_oids hakemus-oids})
       (group-by :application_key)))

(defn- kk-base-educations [answers]
  (->> [["kk" :higher-education-qualification-in-finland-year-and-date]
        ["avoin" :studies-required-by-higher-education-field]
        ["ulk" :higher-education-qualification-outside-finland-year-and-date]
        ["muu" :other-eligibility-year-of-completion]]
       (remove (fn [[_ id]] (clojure.string/blank? (-> answers id :value first first))))
       (map first)))

(defn- unwrap-hakurekisteri-application
  [{:keys [key haku hakukohde person_oid lang email content]}]
  (let [answers (answers-by-key (:answers content))]
    {:oid                 key
     :personOid           person_oid
     :applicationSystemId haku
     :kieli               lang
     :hakukohteet         hakukohde
     :email               email
     :matkapuhelin        (-> answers :phone :value)
     :lahiosoite          (-> answers :address :value)
     :postinumero         (-> answers :postal-code :value)
     :postitoimipaikka    (-> answers :postal-office :value)
     ; Default asuinmaa to finland for forms that are from before
     ; country-of-residence was implemented, or copied from those forms.
     :asuinmaa            (or (-> answers :country-of-residence :value) "246")
     :kotikunta           (-> answers :home-town :value)
     :kkPohjakoulutus     (kk-base-educations answers)}))

(defn get-hakurekisteri-applications
  [haku-oid hakukohde-oids person-oids modified-after]
  (let [applications        (->> (exec-db :db yesql-applications-for-hakurekisteri
                                          {:haku_oid       haku-oid
                                           :hakukohde_oids (cons "" hakukohde-oids)
                                           :person_oids    (cons "" person-oids)
                                           :modified_after (some->> modified-after
                                                                    (f/parse (f/formatter "yyyyMMddHHmm"
                                                                                          (time/time-zone-for-id "Europe/Helsinki")))
                                                                    str)})
                                 (map unwrap-hakurekisteri-application))
        payment-obligations (when (not-empty applications)
                              (payment-obligations-for-applications (map :oid applications)))]
    (map #(payment-obligation-to-application % payment-obligations) applications)))

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

(defn- unwrap-external-application
  [{:keys [key haku person_oid lang email hakukohde] :as application}]
  {:oid           key
   :hakuOid       haku
   :henkiloOid    person_oid
   :asiointikieli lang
   :email         email
   :hakutoiveet   (->> (application-states/get-all-reviews-for-all-requirements
                        (clojure.set/rename-keys application
                                                 {:application_hakukohde_reviews :application-hakukohde-reviews})
                        nil)
                       (group-by :hakukohde)
                       (requirement-names-mapped-to-states-by-hakukohde)
                       (hakutoiveet-to-list)
                       (hakutoiveet-priority-order hakukohde))})

(defn get-external-applications
  [haku-oid hakukohde-oid hakemus-oids organizations]
  (->> (exec-db :db
                yesql-applications-by-haku-and-hakukohde-oids
                {:query_type (if (nil? organizations) "ALL" "ORGS")
                 :authorized_organization_oids (if (nil? organizations)
                                                 [""]
                                                 organizations)
                 :haku_oid                     haku-oid
                 ; Empty string to avoid empty parameter lists
                 :hakukohde_oids               (cond-> [""]
                                                       (some? hakukohde-oid)
                                                       (conj hakukohde-oid))
                 :hakemus_oids                 (cons "" hakemus-oids)})
       (map unwrap-external-application)))

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

(defn- update-hakukohde-process-state!
  [connection username organization-oid session hakukohde-oid from-state to-state application-key]
  (let [application      (get-latest-application-by-key-with-hakukohde-reviews application-key)
        existing-reviews (filter
                           #(= (:state %) from-state)
                           (application-states/get-all-reviews-for-requirement "processing-state" application hakukohde-oid))
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
    {:new              new-event
     :id               username
     :operation        audit-log/operation-new
     :organization-oid organization-oid}))

(defn mass-update-application-states
  [session application-keys hakukohde-oid from-state to-state]
  (let [audit-log-entries (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (let [connection       {:connection conn}
                                  username         (get-in session [:identity :username])
                                  organization-oid (get-in session [:identity :organizations 0 :oid])]
                              (mapv
                                (partial update-hakukohde-process-state! connection username organization-oid session hakukohde-oid from-state to-state)
                                application-keys)))]
    (doseq [audit-log-entry audit-log-entries]
      (audit-log/log audit-log-entry))))

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

(defn get-applications-newer-than [date]
  (exec-db :db yesql-get-applciations-by-created-time {:date date}))

(defn add-review-note [note session]
  {:pre [(-> note :application-key clojure.string/blank? not)
         (-> note :notes clojure.string/blank? not)]}
  (-> (exec-db :db yesql-add-review-note<! {:application_key (:application-key note)
                                            :notes           (:notes note)
                                            :virkailija_oid  (-> session :identity :oid)})
      (merge (select-keys (:identity session) [:first-name :last-name]))
      (dissoc :virkailija_oid :removed)
      (->kebab-case-kw)))

(defn get-application-info-for-tilastokeskus [haku-oid]
  (exec-db :db yesql-tilastokeskus-applications {:haku_oid haku-oid}))

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

(defn- flatten-question-group-answers [key group-values]
  (->> group-values
       (map-indexed
        (fn [group-index values]
          (map-indexed
           (fn [index value]
             [(format "%s_group%d_%d" key (inc group-index) (inc index)) value])
           values)))
       (apply concat))) ; Flatten only 1 level

(defn- flatten-sequential-answers [key values]
  (->> values
       (map-indexed
        (fn [index value]
          [(format "%s_%d" key (inc index)) value]))))

(defn- flatten-application-answers [answers]
  (reduce
   (fn [acc answer]
     (let [key             (:key answer)
           value-or-values (:value answer)]
       (if (sequential? value-or-values)
         (if (every? sequential? value-or-values)
           (into acc (flatten-question-group-answers key value-or-values))
           (into acc (flatten-sequential-answers key value-or-values)))
         (assoc acc key value-or-values))))
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
  (->> (exec-db :db yesql-valintalaskenta-applications {:hakukohde_oid hakukohde-oid
                                                        :application_keys (cons "" application-keys)})
       (map unwrap-valintalaskenta-application)))


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
