(ns ataru.applications.application-service
  (:require
   [ataru.applications.application-access-control :as aac]
   [ataru.forms.form-access-control :as form-access-control]
   [ataru.forms.form-store :as form-store]
   [ataru.koodisto.koodisto :as koodisto]
   [ataru.applications.application-store :as application-store]
   [ataru.middleware.user-feedback :refer [user-feedback-exception]]
   [ataru.applications.excel-export :as excel]
   [ataru.tarjonta-service.hakukohde :refer [populate-hakukohde-answer-options]]
   [ataru.hakija.hakija-form-service :as hakija-form-service]
   [taoensso.timbre :refer [spy debug]]
   [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
   [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-service]
   [ataru.organization-service.ldap-client :as ldap]
   [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit]
   [ataru.information-request.information-request-store :as information-request-store]
   [ataru.email.application-email-confirmation :as email]
   [ataru.person-service.person-service :as person-service]
   [ataru.util :as util]
   [ataru.person-service.birth-date-converter :as bd-converter]
   [medley.core :refer [filter-vals]])
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

(defn- populate-koodisto-fields [application {:keys [content]}]
  (let [koodisto-fields (extract-koodisto-fields content)
        lang            (-> (:lang application)
                            clojure.string/lower-case
                            keyword)]
    (update application :answers
      (partial map
        (fn [{:keys [key] :as answer}]
          (cond-> answer
            (contains? koodisto-fields key)
            (update :value (fn [koodi-value]
                             (let [koodisto-uri (get-in koodisto-fields [key :uri])
                                   version (get-in koodisto-fields [key :version])
                                   koodisto (koodisto/get-koodisto-options koodisto-uri version)
                                   get-label (fn [koodi-uri]
                                               (let [labels (:label (get-koodi koodisto koodi-uri))]
                                                 (or (some #(when (not (clojure.string/blank? (get labels %)))
                                                              (get labels %))
                                                           [lang :fi :sv :en])
                                                     (str "Tuntematon koodi " koodi-uri))))]
                               (cond (string? koodi-value)
                                     (let [values (clojure.string/split koodi-value #"\s*,\s*")]
                                       (if (< 1 (count values))
                                         (map get-label values)
                                         (get-label (first values))))
                                     (and (vector? koodi-value)
                                          (every? vector? koodi-value))
                                     (map (partial map get-label) koodi-value)
                                     :else
                                     (map get-label koodi-value)))))))))))

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

(defn get-country-by-code [code]
  (or (->> (koodisto/get-koodisto-options "maatjavaltiot2" 1)
           (filter #(= code (:value %)))
           first
           :label
           :fi)
      (str "Tuntematon maakoodi " code)))

(defn populate-person-koodisto-fields [person]
  (-> person
      (update :gender util/gender-int-to-string)
      (update :nationality get-country-by-code)))

(defn- person-info-from-onr-person [person]
  {:first-name     (:etunimet person)
   :preferred-name (:kutsumanimi person)
   :last-name      (:sukunimi person)
   :ssn            (:hetu person)
   :birth-date     (some-> person :syntymaaika bd-converter/convert-to-finnish-format)
   :gender         (-> person :sukupuoli)
   :nationality    (-> person :kansalaisuus first (get :kansalaisuusKoodi "999"))})

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
  (when-let [bare-application (aac/get-latest-application-by-key
                               organization-service
                               tarjonta-service
                               session
                               application-key)]
    (let [tarjonta-info (tarjonta-parser/parse-tarjonta-info-by-haku
                         tarjonta-service
                         organization-service
                         ohjausparametrit-service
                         (:haku bare-application)
                         (:hakukohde bare-application))
          form          (-> (:form bare-application)
                            form-store/fetch-by-id
                            (populate-hakukohde-answer-options tarjonta-info)
                            (hakija-form-service/populate-can-submit-multiple-applications tarjonta-info))
          application   (populate-koodisto-fields bare-application form)
          person        (get-person application person-client)]
      {:application          (-> application
                                 (dissoc :person-oid)
                                 (assoc :person (if (:yksiloity person)
                                                  (populate-person-koodisto-fields person)
                                                  person))
                                 (merge tarjonta-info))
       :form                 form
       :hakukohde-reviews    (parse-application-hakukohde-reviews application-key)
       :attachment-reviews   (parse-application-attachment-reviews application-key)
       :events               (application-store/get-application-events application-key)
       :review               (application-store/get-application-review application-key)
       :review-notes         (application-store/get-application-review-notes application-key)
       :information-requests (information-request-store/get-information-requests application-key)})))

(defn- belongs-to-hakukohderyhma?
  [hakukohderyhma-oid hakukohde]
  (->> (:ryhmaliitokset hakukohde)
       (map :ryhmaOid)
       (some #(= hakukohderyhma-oid %))))

(defn- applied-to-hakukohderyhma?
  [hakukohderyhma-oid _ application]
  (some #(belongs-to-hakukohderyhma? hakukohderyhma-oid %)
        (:hakukohde application)))

(defn- first-hakukohde-in-hakukohderyhma
  [hakukohderyhma-oid application]
  (->> (:hakukohde application)
       (filter #(belongs-to-hakukohderyhma? hakukohderyhma-oid %))
       first))

(defn- belongs-to-some-organization?
  [authorized-organization-oids hakukohde]
  (not-empty
   (clojure.set/intersection
    authorized-organization-oids
    (set (:tarjoajaOids hakukohde)))))

(defn- applied-ensisijaisesti-hakukohderyhmassa?
  [hakukohderyhma-oid authorized-organization-oids application]
  (and (some? authorized-organization-oids)
       (belongs-to-some-organization? authorized-organization-oids
                                      (first-hakukohde-in-hakukohderyhma
                                       hakukohderyhma-oid
                                       application))))

(defn get-application-list-by-query
  [organization-service person-service tarjonta-service session query-key query-value]
  (let [[query-key query-value predicates]
        (cond (= :hakukohde query-key)
              [(if (:ensisijaisesti query-value)
                 :ensisijainen-hakukohde-oid
                 :hakukohde-oid)
               (:hakukohde-oid query-value)
               []]
              (= :hakukohderyhma query-key)
              [:haku-oid
               (:haku-oid query-value)
               [(partial (if (:ensisijaisesti query-value)
                           applied-ensisijaisesti-hakukohderyhmassa?
                           applied-to-hakukohderyhma?)
                         (:hakukohderyhma-oid query-value))]]
              :else
              [query-key
               query-value
               []])
        applications (aac/get-application-list-by-query
                      organization-service
                      tarjonta-service
                      session
                      query-key
                      query-value
                      predicates)
        persons      (person-service/get-persons
                      person-service
                      (distinct (keep :person-oid applications)))]
    (map (fn [application]
           (let [onr-person (get persons (keyword (:person-oid application)))
                 person     (if (or (:yksiloity onr-person)
                                    (:yksiloityVTJ onr-person))
                              {:preferred-name (:kutsumanimi onr-person)
                               :last-name      (:sukunimi onr-person)
                               :yksiloity      true}
                              {:preferred-name (:preferred-name application)
                               :last-name      (:last-name application)
                               :yksiloity      false})]
             (-> application
                 (assoc :person person)
                 (dissoc :person-oid :preferred-name :last-name))))
         applications)))

(defn get-excel-report-of-applications-by-key
  [application-keys selected-hakukohde user-wants-to-skip-answers? session organization-service tarjonta-service ohjausparametrit-service person-service]
  (let [applications                     (application-store/get-applications-by-keys application-keys)
        forms                            (->> applications
                                              (map :form-key)
                                              (distinct))
        allowed-forms                    (set (filter #(form-access-control/form-allowed-by-key?
                                                         %
                                                         session
                                                         organization-service
                                                         [:view-applications :edit-applications])
                                                forms))
        allowed-applications             (filter #(contains? allowed-forms (:form-key %)) applications)
        application-reviews              (->> allowed-applications
                                              (map :key)
                                              application-store/get-application-reviews-by-keys
                                              (reduce #(assoc %1 (:application-key %2) %2) {}))
        onr-persons                      (->> (map :person-oid allowed-applications)
                                              distinct
                                              (filter some?)
                                              (person-service/get-persons person-service))
        applications-with-persons        (map (fn [application]
                                                  (assoc application
                                                    :person (->> (:person-oid application)
                                                                 keyword
                                                                 (get onr-persons)
                                                                 (parse-person application))))
                                              allowed-applications)
        skip-answers-to-preserve-memory? (<= 2000 (count allowed-applications))
        skip-answers?                    (or user-wants-to-skip-answers?
                                             skip-answers-to-preserve-memory?)]
    (ByteArrayInputStream. (excel/export-applications applications-with-persons
                                                      application-reviews
                                                      selected-hakukohde
                                                      skip-answers?
                                                      tarjonta-service
                                                      organization-service
                                                      ohjausparametrit-service))))

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

(defn send-modify-application-link-email [application-key session organization-service tarjonta-service]
  (when-let [application-id (:id (aac/get-latest-application-by-key
                                  organization-service
                                  tarjonta-service
                                  session
                                  application-key))]
    (application-store/add-new-secret-to-application application-key)
    (email/start-email-submit-confirmation-job tarjonta-service application-id)
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
