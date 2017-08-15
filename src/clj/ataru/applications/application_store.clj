(ns ataru.applications.application-store
  (:require [ataru.log.audit-log :as audit-log]
            [ataru.util.language-label :as label]
            [ataru.schema.form-schema :as schema]
            [ataru.application.review-states :refer [incomplete-states]]
            [ataru.virkailija.authentication.virkailija-edit]
            [camel-snake-kebab.core :as t :refer [->snake_case ->kebab-case-keyword]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [clj-time.core :as time]
            [schema.core :as s]
            [ataru.db.db :as db]
            [yesql.core :refer [defqueries]]
            [clojure.java.jdbc :as jdbc]
            [ataru.dob :as dob]
            [crypto.random :as crypto]
            [taoensso.timbre :refer [info]]))

(defqueries "sql/application-queries.sql")
(defqueries "sql/virkailija-credentials-queries.sql")

(defn- exec-db
  [ds-key query params]
  (db/exec ds-key query params))

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

(defn- add-new-application-version
  "Add application and also initial metadata (event for receiving application, and initial review record)"
  [application conn]
  (let [connection           {:connection conn}
        answers              (:answers application)
        secret               (:secret application)
        application-to-store {:form_id        (:form application)
                              :key            (or (:key application)
                                                  (str (java.util.UUID/randomUUID)))
                              :lang           (:lang application)
                              :preferred_name (find-value-from-answers "preferred-name" answers)
                              :last_name      (find-value-from-answers "last-name" answers)
                              :ssn            (find-value-from-answers "ssn" answers)
                              :dob            (dob/str->dob (find-value-from-answers "birth-date" answers))
                              :email          (find-value-from-answers "email" answers)
                              :hakukohde      (or (:hakukohde application) [])
                              :haku           (:haku application)
                              :content        {:answers answers}
                              :secret         (or secret (crypto/url-part 34))
                              :person_oid     (:person-oid application)}
        application          (yesql-add-application-query<! application-to-store connection)]
    (unwrap-application application)))

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
    (let [{:keys [id key] :as new-application} (add-new-application-version new-application conn)
          connection                {:connection conn}]
      (audit-log/log {:new       new-application
                      :operation audit-log/operation-new
                      :id        (extract-email new-application)})
      (yesql-add-application-event! {:application_key  key
                                     :event_type       "received-from-applicant"
                                     :new_review_state nil}
                                    connection)
      (yesql-add-application-review! {:application_key key
                                      :state           "unprocessed"}
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
         (select-keys old-application [:key :secret :haku :hakukohde :person-oid])))

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
                                                 (merge-applications new-application old-application) conn)]
      (info (str "Updating application with key "
                 (:key old-application)
                 " based on valid application secret, retaining key and secret from previous version"))
      (yesql-add-application-event! {:application_key  key
                                     :event_type       (if updated-by-applicant?
                                                         "updated-by-applicant"
                                                         "updated-by-virkailija")
                                     :new_review_state nil}
                                    {:connection conn})
      (audit-log/log {:new       (application->loggable-form new-application)
                      :old       (application->loggable-form old-application)
                      :operation audit-log/operation-modify
                      :id        (if updated-by-applicant?
                                   (extract-email new-application)
                                   (get-virkailija-oid virkailija-secret key conn))})
      id)))

(defn- older?
  "Check if application given as first argument is older than
   application given as second argument by comparing :created-time."
  [a1 a2]
  (time/before? (:created-time a1)
                (:created-time a2)))

(defn- latest-versions-only [applications]
  (->> applications
       (reduce (fn [applications {:keys [key] :as a1}]
                 (let [a2 (get applications key)]
                   (if (or (nil? a2)
                           (older? a2 a1))
                     (assoc applications key a1)
                     applications)))
               {})
       (vals)
       (sort-by :created-time)
       (reverse)
       (vec)))

(defn get-application-list-by-form
  "Only list with header-level info, not answers. Does NOT include applications associated with any hakukohde."
  [form-key]
  (->> (exec-db :db yesql-get-application-list-by-form {:form_key form-key})
       (map ->kebab-case-kw)
       (latest-versions-only)))

(defn get-application-list-by-hakukohde
  "Only list with header-level info, not answers. ONLY include applications associated with given hakukohde."
  [hakukohde-oid organization-oids]
  (->> (exec-db :db yesql-get-application-list-by-hakukohde {:hakukohde_oid                hakukohde-oid
                                                             :query_type                   "ORGS"
                                                             :authorized_organization_oids organization-oids})
       (map ->kebab-case-kw)
       (latest-versions-only)))

(defn get-full-application-list-by-hakukohde
  "Only list with header-level info, not answers. ONLY include applications associated with given hakukohde."
  [hakukohde-oid]
  (->> (exec-db :db yesql-get-application-list-by-hakukohde {:hakukohde_oid hakukohde-oid
                                                             :query_type "ALL"
                                                             :authorized_organization_oids [""]})
       (map ->kebab-case-kw)
       (latest-versions-only)))

(defn get-application-list-by-haku
  "Only list with header-level info, not answers. ONLY include applications associated with given hakukohde."
  [haku-oid organization-oids]
  (->> (exec-db :db yesql-get-application-list-by-haku {:haku_oid                     haku-oid
                                                        :query_type                   "ORGS"
                                                        :authorized_organization_oids organization-oids})
       (map ->kebab-case-kw)
       (latest-versions-only)))

(defn get-full-application-list-by-haku
  "Only list with header-level info, not answers. ONLY include applications associated with given hakukohde."
  [haku-oid]
  (->> (exec-db :db yesql-get-application-list-by-haku {:haku_oid haku-oid
                                                        :query_type "ALL"
                                                        :authorized_organization_oids [""]})
       (map ->kebab-case-kw)
       (latest-versions-only)))

(defn get-application-list-by-ssn
  "Only list with header-level info"
  [ssn organization-oids]
  (->> (exec-db :db yesql-get-application-list-by-ssn {:ssn                          ssn
                                                       :query_type                   "ORGS"
                                                       :authorized_organization_oids organization-oids})
       (map ->kebab-case-kw)
       (latest-versions-only)))

(defn get-full-application-list-by-ssn
  "Only list with header-level info"
  [ssn]
  (->> (exec-db :db yesql-get-application-list-by-ssn {:ssn                          ssn
                                                       :query_type                   "ALL"
                                                       :authorized_organization_oids [""]})
       (map ->kebab-case-kw)
       (latest-versions-only)))

(defn get-application-list-by-dob [dob organization-oids]
  (->> (exec-db :db yesql-get-application-list-by-dob {:dob                          dob
                                                       :query_type                   "ORGS"
                                                       :authorized_organization_oids organization-oids})
       (map ->kebab-case-kw)
       (latest-versions-only)))

(defn get-full-application-list-by-dob [dob]
  (->> (exec-db :db yesql-get-application-list-by-dob {:dob                          dob
                                                       :query_type                   "ALL"
                                                       :authorized_organization_oids [""]})
       (map ->kebab-case-kw)
       (latest-versions-only)))

(defn get-application-list-by-email [email organization-oids]
  (->> (exec-db :db yesql-get-application-list-by-email {:email                        email
                                                         :query_type                   "ORGS"
                                                         :authorized_organization_oids organization-oids})
       (map ->kebab-case-kw)
       (latest-versions-only)))

(defn get-full-application-list-by-email [email]
  (->> (exec-db :db yesql-get-application-list-by-email {:email                        email
                                                         :query_type                   "ALL"
                                                         :authorized_organization_oids [""]})
       (map ->kebab-case-kw)
       (latest-versions-only)))

(defn get-full-application-list-by-person-oid-for-omatsivut [person-oid]
  (->> (exec-db :db yesql-get-application-list-by-person-oid-for-omatsivut
         {:person_oid                   person-oid
          :query_type                   "ALL"
          :authorized_organization_oids [""]})
       (map ->kebab-case-kw)
       (latest-versions-only)))

(defn get-application-review [application-key]
  (->kebab-case-kw (first (exec-db :db yesql-get-application-review {:application_key application-key}))))

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

(defn get-latest-application-by-secret [secret]
  (when-let [application (->> (exec-db :db yesql-get-latest-application-by-secret {:secret secret})
                              (first)
                              (unwrap-application))]
    (assoc application :state (-> (:key application) get-application-review :state))))

(defn- get-latest-application-for-virkailija-edit [virkailija-secret]
  (when-let [application (->> (exec-db :db yesql-get-latest-application-by-virkailija-secret {:virkailija_secret virkailija-secret})
                              (first)
                              (unwrap-application))]
    (assoc application :state (-> (:key application) get-application-review :state))))

(defn get-latest-version-of-application-for-edit
  [{secret :secret
    virkailija-sercret :virkailija-secret :as application}]
  (if secret
    (get-latest-application-by-secret secret)
    (get-latest-application-for-virkailija-edit virkailija-sercret)))

(defn get-application-events [application-key]
  (mapv ->kebab-case-kw (exec-db :db yesql-get-application-events {:application_key application-key})))

(defn get-application-organization-oid [application-key]
  (:organization_oid (first (exec-db :db yesql-get-application-organization-by-key {:application_key application-key}))))

(defn get-application-review-organization-oid [review-id]
  (:organization_oid (first (exec-db :db yesql-get-application-review-organization-by-id {:review_id review-id}))))

(defn save-application-review [review session]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [connection       {:connection conn}
          app-key          (:application-key review)
          old-review       (first (yesql-get-application-review {:application_key app-key} connection))
          review-to-store  (transform-keys ->snake_case review)
          username         (get-in session [:identity :username])
          organization-oid (get-in session [:identity :organizations 0 :oid])]
      (audit-log/log {:new              review-to-store
                      :old              old-review
                      :id               username
                      :operation        audit-log/operation-modify
                      :organization-oid organization-oid})
      (yesql-save-application-review! review-to-store connection)
      (when (not= (:state old-review) (:state review-to-store))
        (let [application-event {:application_key  app-key
                                 :event_type       "review-state-change"
                                 :new_review_state (:state review-to-store)}]
          (yesql-add-application-event!
            application-event
            connection)
          (audit-log/log {:new              application-event
                          :id               username
                          :operation        audit-log/operation-new
                          :organization-oid organization-oid}))))))

(s/defn get-applications-for-form :- [schema/Application]
  [form-key :- s/Str filtered-states :- [s/Str]]
  (->> {:form_key form-key :filtered_states filtered-states}
       (exec-db :db yesql-get-applications-for-form)
       (mapv unwrap-application)
       (latest-versions-only)))

(s/defn get-applications-for-hakukohde :- [schema/Application]
  [filtered-states :- [s/Str]
   hakukohde-oid :- s/Str]
  (->> (exec-db :db yesql-get-applications-for-hakukohde
                {:filtered_states filtered-states
                 :hakukohde_oid   hakukohde-oid})
       (mapv (partial unwrap-application))
       (latest-versions-only)))

(s/defn get-applications-for-haku :- [schema/Application]
  [haku-oid :- s/Str
   filtered-states :- [s/Str]]
  (->> (exec-db :db yesql-get-applications-for-haku
         {:filtered_states filtered-states
          :haku_oid        haku-oid})
       (mapv (partial unwrap-application))
       (latest-versions-only)))

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

(defn get-direct-form-haut [organization-oids]
  (mapv ->kebab-case-kw (exec-db :db yesql-get-direct-form-haut
                                 {:incomplete_states incomplete-states
                                  :query_type "ORGS"
                                  :authorized_organization_oids organization-oids})))

(defn get-all-direct-form-haut []
  (mapv ->kebab-case-kw (exec-db :db yesql-get-direct-form-haut
                                 {:incomplete_states incomplete-states
                                  :query_type "ALL"
                                  :authorized_organization_oids [""]})))

(defn add-application-feedback
  [feedback]
  (->kebab-case-kw
    (exec-db :db yesql-add-application-feedback<! (transform-keys ->snake_case feedback))))

(defn get-hakija-secret-by-virkailija-secret [virkailija-secret]
  (-> (exec-db :db yesql-get-hakija-secret-by-virkailija-secret {:virkailija_secret virkailija-secret})
      (first)
      :secret))
