(ns ataru.applications.application-service
  (:require
   [ataru.applications.application-access-control :as aac]
   [ataru.applications.application-store :as application-store]
   [ataru.applications.excel-export :as excel]
   [ataru.email.application-email-confirmation :as email]
   [ataru.forms.form-access-control :as form-access-control]
   [ataru.forms.form-store :as form-store]
   [ataru.hakija.hakija-form-service :as hakija-form-service]
   [ataru.information-request.information-request-store :as information-request-store]
   [ataru.koodisto.koodisto :as koodisto]
   [ataru.middleware.user-feedback :refer [user-feedback-exception]]
   [ataru.person-service.birth-date-converter :as bd-converter]
   [ataru.person-service.person-service :as person-service]
   [ataru.tarjonta-service.hakukohde :refer [populate-hakukohde-answer-options]]
   [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
   [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-service]
   [ataru.util :as util]
   [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit]
   [medley.core :refer [filter-vals]]
   [taoensso.timbre :refer [spy debug]])
  (:import [java.io ByteArrayInputStream]))

(defn- extract-koodisto-fields [field-descriptor-list]
  (reduce
    (fn [result {:keys [children id koodisto-source options followups]}]
      (cond
        (some? children)
        (merge result (extract-koodisto-fields children))

        (some :followups options)
        (merge result (extract-koodisto-fields options))

        (not-empty followups)
        (merge result (extract-koodisto-fields followups))

        :else
        (cond-> result
          (every? some? [id koodisto-source])
          (assoc id (select-keys koodisto-source [:uri :version])))))
    {}
    field-descriptor-list))

(defn- get-koodi [koodisto koodi-value]
  (let [koodi-pred (comp (partial = koodi-value) :value)]
    (->> koodisto
         (filter koodi-pred)
         first)))

(defn- parse-application-hakukohde-reviews
  [application-key]
  (reduce
    (fn [acc {:keys [hakukohde requirement state]}]
      (update-in acc [(or hakukohde :form)] assoc (keyword requirement) state))
    {}
    (application-store/get-application-hakukohde-reviews application-key)))

(defn- parse-application-attachment-reviews
  [application-key]
  (reduce
   (fn [acc {:keys [attachment-key state hakukohde]}]
     (assoc-in acc [hakukohde attachment-key] state))
   {}
   (application-store/get-application-attachment-reviews application-key)))

(defn- person-info-from-application [application]
  (let [answers (util/answers-by-key (:answers application))]
    {:first-name     (-> answers :first-name :value)
     :preferred-name (-> answers :preferred-name :value)
     :last-name      (-> answers :last-name :value)
     :ssn            (-> answers :ssn :value)
     :birth-date     (-> answers :birth-date :value)
     :gender         (-> answers :gender :value)
     :nationality    (-> answers :nationality :value)}))

(defn- person-info-from-onr-person [person]
  {:first-name     (:etunimet person)
   :preferred-name (:kutsumanimi person)
   :last-name      (:sukunimi person)
   :ssn            (:hetu person)
   :birth-date     (some-> person :syntymaaika bd-converter/convert-to-finnish-format)
   :gender         (-> person :sukupuoli)
   :nationality    (->> (-> person :kansalaisuus)
                        (mapv #(vector (get % :kansalaisuusKoodi "999"))))})

(defn parse-person [application person-from-onr]
  (let [yksiloity       (or (-> person-from-onr :yksiloity)
                            (-> person-from-onr :yksiloityVTJ))
        person-info     (if yksiloity
                          (person-info-from-onr-person person-from-onr)
                          (person-info-from-application application))]
    (merge
      {:oid         (:person-oid application)
       :turvakielto (-> person-from-onr :turvakielto boolean)
       :yksiloity   (boolean yksiloity)}
      person-info)))

(defn get-person
  [application person-client]
  (let [person-from-onr (some->> (:person-oid application)
                                 (person-service/get-person person-client))]
    (parse-person application person-from-onr)))

(defn get-application-with-human-readable-koodis
  "Get application that has human-readable koodisto values populated
   onto raw koodi values."
  [application-key session organization-service tarjonta-service ohjausparametrit-service person-client]
  (when-let [application (aac/get-latest-application-by-key
                           organization-service
                           tarjonta-service
                           session
                           application-key)]
    (let [tarjonta-info (tarjonta-parser/parse-tarjonta-info-by-haku
                         tarjonta-service
                         organization-service
                         ohjausparametrit-service
                         (:haku application)
                         (:hakukohde application))
          form-key      (->> (-> tarjonta-info :tarjonta :hakukohteet)
                             (map :form-key)
                             (distinct)
                             (remove nil?)
                             first)
          form          (-> (:form application)
                            form-store/fetch-by-id
                            koodisto/populate-form-koodisto-fields
                            (populate-hakukohde-answer-options tarjonta-info)
                            (hakija-form-service/populate-can-submit-multiple-applications tarjonta-info))
          newest-form   (some-> form-key form-store/fetch-by-key
                          (assoc :content []) (dissoc :organization-oid))]
      (util/remove-nil-values {:application          (-> application
                                                         (dissoc :person-oid)
                                                         (assoc :person (get-person application person-client))
                                                         (merge tarjonta-info))
                               :form                 form
                               :newest-form          (when (not= (:id form) (:id newest-form)) newest-form)
                               :hakukohde-reviews    (parse-application-hakukohde-reviews application-key)
                               :attachment-reviews   (parse-application-attachment-reviews application-key)
                               :events               (application-store/get-application-events application-key)
                               :review               (application-store/get-application-review application-key)
                               :review-notes         (application-store/get-application-review-notes application-key)
                               :information-requests (information-request-store/get-information-requests application-key)}))))

(defn- belongs-to-hakukohderyhma?
  [hakukohderyhma-oid hakukohde]
  (->> (:ryhmaliitokset hakukohde)
       (map :ryhmaOid)
       (some #(= hakukohderyhma-oid %))))

(defn- applied-to-hakukohderyhma?
  [hakukohderyhma-oid _ application]
  (some #(belongs-to-hakukohderyhma? hakukohderyhma-oid %)
        (:hakukohde application)))

(defn- filter-with-hakukohde [hakukohde-oid hakukohde]
  (if hakukohde-oid
    (when (= hakukohde-oid (:oid hakukohde))
      hakukohde)
    hakukohde))

(defn- first-hakukohde-in-hakukohderyhma
  [hakukohderyhma-oid rajaus-hakukohteella application]
  (->> (:hakukohde application)
       (filter #(belongs-to-hakukohderyhma? hakukohderyhma-oid %))
       first
       (filter-with-hakukohde rajaus-hakukohteella)))

(defn- belongs-to-some-organization?
  [authorized-organization-oids hakukohde]
  (not-empty
   (clojure.set/intersection
    authorized-organization-oids
    (set (:tarjoajaOids hakukohde)))))

(defn- applied-ensisijaisesti-hakukohderyhmassa?
  [rajaus-hakukohteella hakukohderyhma-oid authorized-organization-oids application]
  (and (some? authorized-organization-oids)
       (belongs-to-some-organization? authorized-organization-oids
                                      (first-hakukohde-in-hakukohderyhma
                                       hakukohderyhma-oid
                                       rajaus-hakukohteella
                                       application))))

(defn ->form-query
  [key]
  {:form key
   :predicate   (constantly true)})

(defn ->hakukohde-query
  [hakukohde-oid ensisijaisesti]
  (if ensisijaisesti
    {:ensisijainen-hakukohde hakukohde-oid
     :predicate              (constantly true)}
    {:hakukohde hakukohde-oid
     :predicate (constantly true)}))

(defn ->hakukohderyhma-query
  [haku-oid hakukohderyhma-oid ensisijaisesti rajaus-hakukohteella]
  {:haku      haku-oid
   :predicate (partial (if ensisijaisesti
                         (partial applied-ensisijaisesti-hakukohderyhmassa? rajaus-hakukohteella)
                         applied-to-hakukohderyhma?)
                       hakukohderyhma-oid)})

(defn ->haku-query
  [haku-oid]
  {:haku      haku-oid
   :predicate (constantly true)})

(defn ->ssn-query
  [ssn]
  {:ssn       ssn
   :predicate (constantly true)})

(defn ->dob-query
  [dob]
  {:dob       dob
   :predicate (constantly true)})

(defn ->email-query
  [email]
  {:email     email
   :predicate (constantly true)})

(defn ->name-query
  [name]
  {:name      (application-store/->name-query-value name)
   :predicate (constantly true)})

(defn ->person-oid-query
  [person-oid]
  {:person-oid person-oid
   :predicate  (constantly true)})

(defn ->application-oid-query
  [application-oid]
  {:application-oid application-oid
   :predicate       (constantly true)})

(defn ->application-oids-query
  [application-oids]
  {:application-oids application-oids
   :predicate        (constantly true)})

(defn ->empty-query
  []
  {:predicate (constantly true)})

(defn ->and-query
  ([] (->empty-query))
  ([query other-query]
   (assoc (merge query other-query)
          :predicate (every-pred (:predicate query) (:predicate other-query)))))

(defn get-application-list-by-query
  [organization-service person-service tarjonta-service session query]
  (let [applications (aac/get-application-list-by-query
                      organization-service
                      tarjonta-service
                      session
                      query)
        persons      (person-service/get-persons
                      person-service
                      (distinct (keep :person-oid applications)))]
    (map (fn [application]
           (let [onr-person (get persons (keyword (:person-oid application)))
                 person     (if (or (:yksiloity onr-person)
                                    (:yksiloityVTJ onr-person))
                              {:oid            (:oidHenkilo onr-person)
                               :preferred-name (:kutsumanimi onr-person)
                               :last-name      (:sukunimi onr-person)
                               :yksiloity      true
                               :ssn            (boolean (:hetu onr-person))}
                              {:oid            (:person-oid application)
                               :preferred-name (:preferred-name application)
                               :last-name      (:last-name application)
                               :yksiloity      false
                               :ssn            (boolean (:ssn application))})]
             (-> application
                 (assoc :person person)
                 (dissoc :ssn :person-oid :preferred-name :last-name))))
         applications)))

(defn get-excel-report-of-applications-by-key
  [application-keys selected-hakukohde user-wants-to-skip-answers? session organization-service tarjonta-service ohjausparametrit-service person-service]
  (when (aac/applications-access-authorized? organization-service tarjonta-service session application-keys [:view-applications :edit-applications])
    (let [applications                     (application-store/get-applications-by-keys application-keys)
          application-reviews              (->> applications
                                                (map :key)
                                                application-store/get-application-reviews-by-keys
                                                (reduce #(assoc %1 (:application-key %2) %2) {}))
          application-review-notes         (->> applications
                                                (map :key)
                                                application-store/get-application-review-notes-by-keys
                                                (group-by :application-key))
          onr-persons                      (->> (map :person-oid applications)
                                                distinct
                                                (filter some?)
                                                (person-service/get-persons person-service))
          applications-with-persons        (map (fn [application]
                                                  (assoc application
                                                    :person (->> (:person-oid application)
                                                                 keyword
                                                                 (get onr-persons)
                                                                 (parse-person application))))
                                                applications)
          skip-answers-to-preserve-memory? (<= 4500 (count applications))
          skip-answers?                    (or user-wants-to-skip-answers?
                                               skip-answers-to-preserve-memory?)
          lang                             (keyword (or (-> session :identity :lang) :fi))]
      (ByteArrayInputStream. (excel/export-applications applications-with-persons
                                                        application-reviews
                                                        application-review-notes
                                                        selected-hakukohde
                                                        skip-answers?
                                                        lang
                                                        tarjonta-service
                                                        organization-service
                                                        ohjausparametrit-service)))))

(defn- save-application-hakukohde-reviews
  [application-key hakukohde-reviews session]
  (doseq [[hakukohde review] hakukohde-reviews]
    (doseq [[review-requirement review-state] review]
      (application-store/save-application-hakukohde-review
        application-key
        (name hakukohde)
        (name review-requirement)
        (name review-state)
        session))))

(defn- save-attachment-hakukohde-reviews
  [application-key attachment-reviews session]
  (doseq [[hakukohde review] attachment-reviews
          [attachment-key review-state] review]
    (application-store/save-attachment-hakukohde-review
      application-key
      (name hakukohde)
      (name attachment-key)
      review-state
      session)))

(defn save-application-review
  [organization-service tarjonta-service session review]
  (let [application-key (:application-key review)]
    (when (aac/applications-access-authorized?
           organization-service
           tarjonta-service
           session
           [application-key]
           [:edit-applications])
      (application-store/save-application-review review session)
      (save-application-hakukohde-reviews application-key (:hakukohde-reviews review) session)
      (save-attachment-hakukohde-reviews application-key (:attachment-reviews review) session)
      {:events (application-store/get-application-events application-key)})))

(defn mass-update-application-states
  [organization-service tarjonta-service session application-keys hakukohde-oid from-state to-state]
  (when (aac/applications-access-authorized?
         organization-service
         tarjonta-service
         session
         application-keys
         [:edit-applications])
    (application-store/mass-update-application-states
     session
     application-keys
     hakukohde-oid
     from-state
     to-state)))

(defn send-modify-application-link-email
  [application-key session organization-service tarjonta-service job-runner]
  (when-let [application-id (:id (aac/get-latest-application-by-key
                                  organization-service
                                  tarjonta-service
                                  session
                                  application-key))]
    (application-store/add-new-secret-to-application application-key)
    (email/start-email-submit-confirmation-job tarjonta-service job-runner application-id)
    (application-store/add-application-event {:application-key application-key
                                              :event-type      "modification-link-sent"}
                                             session)))

(defn add-review-note [organization-service tarjonta-service session note]
  (when (aac/applications-access-authorized?
         organization-service
         tarjonta-service
         session
         [(:application-key note)]
         [:view-applications :edit-applications])
    (application-store/add-review-note note session)))

(defn remove-review-note [note-id]
  (application-store/remove-review-note note-id))

(defn get-application-version-changes
  [organization-service tarjonta-service session application-key]
  (when (aac/applications-access-authorized?
         organization-service
         tarjonta-service
         session
         [application-key]
         [:view-applications :edit-applications])
    (application-store/get-application-version-changes application-key)))

(defn omatsivut-applications
  [organization-service person-service session person-oid]
  (->> (person-service/linked-oids person-service person-oid)
       :linked-oids
       (mapcat #(aac/omatsivut-applications organization-service session %))))

(defn get-applications-for-valintalaskenta
  [organization-service person-service session hakukohde-oid application-keys]
  (if-let [applications (aac/get-applications-for-valintalaskenta
                         organization-service
                         session
                         hakukohde-oid
                         application-keys)]
    (if-let [yksiloimattomat (->> applications
                                  (map :personOid)
                                  distinct
                                  (person-service/get-persons person-service)
                                  vals
                                  (remove #(or (:yksiloity %)
                                               (:yksiloityVTJ %)))
                                  (map :oidHenkilo)
                                  distinct
                                  seq)]
      {:yksiloimattomat yksiloimattomat}
      {:applications applications})
    {:unauthorized nil}))
