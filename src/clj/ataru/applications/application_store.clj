(ns ataru.applications.application-store
  (:require [ataru.log.audit-log :as audit-log]
            [ataru.util.language-label :as label]
            [ataru.schema.form-schema :as schema]
            [ataru.application.review-states :refer [incomplete-states]]
            [ataru.application.application-states :as application-states]
            [ataru.virkailija.authentication.virkailija-edit]
            [ataru.util :refer [answers-by-key]]
            [ataru.application.review-states :as application-review-states]
            [camel-snake-kebab.core :as t :refer [->snake_case ->kebab-case-keyword ->camelCase]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [clj-time.core :as time]
            [clj-time.format :as f]
            [clj-time.coerce :as c]
            [schema.core :as s]
            [ataru.db.db :as db]
            [yesql.core :refer [defqueries]]
            [clojure.java.jdbc :as jdbc]
            [ataru.dob :as dob]
            [ataru.util.random :as crypto]
            [taoensso.timbre :refer [info]]
            [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit]))

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
        answers              (->> application
                                  :answers
                                  (filter #(not-empty (:value %))))
        secret               (:secret application)
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
                              :secret         (or secret (crypto/url-part 34))
                              :person_oid     (:person-oid application)}
        application          (if (contains? application :key)
                               (yesql-add-application-version<! application-to-store connection)
                               (yesql-add-application<! application-to-store connection))]
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
         (select-keys old-application [:key :secret :haku :person-oid])))

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
                                                 (merge-applications new-application old-application) conn)
          virkailija-oid        (when-not updated-by-applicant? (get-virkailija-oid virkailija-secret key conn))]
      (info (str "Updating application with key "
                 (:key old-application)
                 " based on valid application secret, retaining key and secret from previous version"))
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

(defn get-application-list-by-hakukohde
  "Only list with header-level info, not answers. ONLY include applications associated with given hakukohde."
  [hakukohde-oid organization-oids]
  (->> (exec-db :db yesql-get-application-list-by-hakukohde {:hakukohde_oid                hakukohde-oid
                                                             :query_type                   "ORGS"
                                                             :authorized_organization_oids organization-oids})
       (map ->kebab-case-kw)))

(defn get-full-application-list-by-hakukohde
  "Only list with header-level info, not answers. ONLY include applications associated with given hakukohde."
  [hakukohde-oid]
  (->> (exec-db :db yesql-get-application-list-by-hakukohde {:hakukohde_oid hakukohde-oid
                                                             :query_type "ALL"
                                                             :authorized_organization_oids [""]})
       (map ->kebab-case-kw)))

(defn get-application-list-by-haku
  "Only list with header-level info, not answers. ONLY include applications associated with given hakukohde."
  [haku-oid organization-oids]
  (->> (exec-db :db yesql-get-application-list-by-haku {:haku_oid                     haku-oid
                                                        :query_type                   "ORGS"
                                                        :authorized_organization_oids organization-oids})
       (map ->kebab-case-kw)))

(defn get-full-application-list-by-haku
  "Only list with header-level info, not answers. ONLY include applications associated with given hakukohde."
  [haku-oid]
  (->> (exec-db :db yesql-get-application-list-by-haku {:haku_oid haku-oid
                                                        :query_type "ALL"
                                                        :authorized_organization_oids [""]})
       (map ->kebab-case-kw)))

(defn get-application-list-by-ssn
  "Only list with header-level info"
  [ssn organization-oids]
  (->> (exec-db :db yesql-get-application-list-by-ssn {:ssn                          ssn
                                                       :query_type                   "ORGS"
                                                       :authorized_organization_oids organization-oids})
       (map ->kebab-case-kw)))

(defn get-full-application-list-by-ssn
  "Only list with header-level info"
  [ssn]
  (->> (exec-db :db yesql-get-application-list-by-ssn {:ssn                          ssn
                                                       :query_type                   "ALL"
                                                       :authorized_organization_oids [""]})
       (map ->kebab-case-kw)))

(defn get-application-list-by-dob [dob organization-oids]
  (->> (exec-db :db yesql-get-application-list-by-dob {:dob                          dob
                                                       :query_type                   "ORGS"
                                                       :authorized_organization_oids organization-oids})
       (map ->kebab-case-kw)))

(defn get-full-application-list-by-dob [dob]
  (->> (exec-db :db yesql-get-application-list-by-dob {:dob                          dob
                                                       :query_type                   "ALL"
                                                       :authorized_organization_oids [""]})
       (map ->kebab-case-kw)))

(defn get-application-list-by-email [email organization-oids]
  (->> (exec-db :db yesql-get-application-list-by-email {:email                        email
                                                         :query_type                   "ORGS"
                                                         :authorized_organization_oids organization-oids})
       (map ->kebab-case-kw)))

(defn get-full-application-list-by-email [email]
  (->> (exec-db :db yesql-get-application-list-by-email {:email                        email
                                                         :query_type                   "ALL"
                                                         :authorized_organization_oids [""]})
       (map ->kebab-case-kw)))

(defn- name-search-query [name]
  (->> (clojure.string/split name #"\s+")
       (remove clojure.string/blank?)
       (map #(str % ":*"))
       (clojure.string/join " & ")))

(defn get-application-list-by-name [name organization-oids]
  (->> (exec-db :db yesql-get-application-list-by-name {:name                         (name-search-query name)
                                                        :query_type                   "ORGS"
                                                        :authorized_organization_oids organization-oids})
       (map ->kebab-case-kw)))

(defn get-full-application-list-by-name [name]
  (->> (exec-db :db yesql-get-application-list-by-name {:name                         (name-search-query name)
                                                        :query_type                   "ALL"
                                                        :authorized_organization_oids [""]})
       (map ->kebab-case-kw)))

(defn get-full-application-list-by-person-oid-for-omatsivut [person-oid]
  (->> (exec-db :db yesql-get-application-list-by-person-oid-for-omatsivut
                {:person_oid person-oid})
       (map ->kebab-case-kw)))

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
  [{:keys [secret virkailija-secret]}]
  (if secret
    (get-latest-application-by-secret secret)
    (get-latest-application-for-virkailija-edit virkailija-secret)))

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

(defn save-application-review [review session virkailija]
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
                                 :new_review_state (:state review-to-store)
                                 :virkailija_oid   (:oid virkailija)
                                 :hakukohde        nil
                                 :review_key       nil}]
          (yesql-add-application-event<!
            application-event
            connection)
          (audit-log/log {:new              application-event
                          :id               username
                          :operation        audit-log/operation-new
                          :organization-oid organization-oid}))))))

(defn get-application-hakukohde-reviews
  [application-key]
  (mapv ->kebab-case-kw (exec-db :db yesql-get-application-hakukohde-reviews {:application_key application-key})))

(defn save-application-hakukohde-review
  [virkailija application-key hakukohde-oid hakukohde-review-requirement hakukohde-review-state session]
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
                                (audit-log/log {:new              review-to-store
                                                :old              (first existing-requirement-review)
                                                :id               username
                                                :operation        audit-log/operation-modify
                                                :organization-oid organization-oid})
                                (yesql-upsert-application-hakukohde-review! review-to-store connection)
                                (let [hakukohde-event {:application_key  application-key
                                                       :event_type       "hakukohde-review-state-change"
                                                       :new_review_state (:state review-to-store)
                                                       :review_key       hakukohde-review-requirement
                                                       :hakukohde        (:hakukohde review-to-store)
                                                       :virkailija_oid   (:oid virkailija)}]
                                  (yesql-add-application-event<!
                                    hakukohde-event
                                    connection)
                                  (audit-log/log {:new              hakukohde-event
                                                  :id               username
                                                  :operation        audit-log/operation-new
                                                  :organization-oid organization-oid}))))))

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
  (->> (exec-db :db yesql-get-applications-for-hakukohde
                {:filtered_states filtered-states
                 :hakukohde_oid   hakukohde-oid})
       (mapv (partial unwrap-application))))

(s/defn get-applications-for-haku :- [schema/Application]
  [haku-oid :- s/Str
   filtered-states :- [s/Str]]
  (->> (exec-db :db yesql-get-applications-for-haku
         {:filtered_states filtered-states
          :haku_oid        haku-oid})
       (mapv (partial unwrap-application))))

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

(defn get-all-direct-form-haut
  []
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
                                                                    (f/parse (f/formatter "yyyyMMddHHmm"))
                                                                    (c/to-sql-date))})
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

(defn- unwrap-external-application
  [{:keys [key haku person_oid lang email] :as application}]
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
                       (hakutoiveet-to-list))})

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
  [connection username organization-oid hakukohde-oid from-state to-state application-key]
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
                          :event_type       "review-state-change"
                          :new_review_state to-state
                          :virkailija_oid   nil
                          :hakukohde        hakukohde-oid
                          :review_key       nil}]
    (when (seq new-reviews)
      (info "Updating" (count new-reviews) "application-hakukohde-reviews"))
    (doseq [new-review new-reviews]
      (yesql-upsert-application-hakukohde-review! new-review connection)
      (yesql-add-application-event<! new-event connection))
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
                                (partial update-hakukohde-process-state! connection username organization-oid hakukohde-oid from-state to-state)
                                application-keys)))]
    (doseq [audit-log-entry audit-log-entries]
      (audit-log/log audit-log-entry))))

(defn add-application-event [event session]
  (jdbc/with-db-transaction [db {:datasource (db/get-datasource :db)}]
    (let [conn       {:connection db}
          virkailija (virkailija-edit/upsert-virkailija session)]
      (-> {:event_type       nil
           :new_review_state nil
           :virkailija_oid   (:oid virkailija)
           :hakukohde        nil
           :review_key       nil}
          (merge (transform-keys ->snake_case event))
          (yesql-add-application-event<! conn)
          (dissoc :virkailija_oid)
          (merge (select-keys virkailija [:first_name :last_name]))
          (->kebab-case-kw)))))

(defn get-applications-newer-than [date]
  (exec-db :db yesql-get-applciations-by-created-time {:date date}))

(defn add-review-note [note session]
  {:pre [(-> note :application-key clojure.string/blank? not)
         (-> note :notes clojure.string/blank? not)]}
  (let [virkailija (-> session virkailija-edit/upsert-virkailija)]
    (-> (exec-db :db yesql-add-review-note<! {:application_key (:application-key note)
                                              :notes           (:notes note)
                                              :virkailija_oid  (:oid virkailija)})
        (merge (select-keys virkailija [:first_name :last_name]))
        (dissoc :virkailija_oid :removed)
        (->kebab-case-kw))))

(defn get-application-info-for-tilastokeskus [haku-oid]
  (exec-db :db yesql-tilastokeskus-applications {:haku_oid haku-oid}))

(defn remove-review-note [note-id]
  (when-not (= (exec-db :db yesql-remove-review-note! {:id note-id}) 0)
    note-id))

(defn get-application-keys []
  (exec-db :db yesql-get-latest-application-ids-distinct-by-person-oid nil))
