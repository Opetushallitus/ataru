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
   [ataru.virkailija.user.ldap-client :as ldap]
   [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit]
   [ataru.information-request.information-request-store :as information-request-store]
   [ataru.hakija.application-email-confirmation :as email]
   [ataru.person-service.person-service :as person-service]
   [ataru.util :as util]
   [ataru.person-service.birth-date-converter :as bd-converter]
   [medley.core :refer [filter-vals]])
  (:import [java.io ByteArrayInputStream]))

(defn get-application-list-by-form [form-key session organization-service]
  (aac/check-form-access form-key session organization-service [:view-applications :edit-applications])
  {:applications (application-store/get-application-list-by-form form-key)})

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
                                               (let [koodi (get-koodi koodisto koodi-uri)]
                                                 (get-in koodi [:label lang])))]
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
  (->> (koodisto/get-koodisto-options "maatjavaltiot2" 1)
       (filter #(= code (:value %)))
       first
       :label
       :fi))

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

(defn get-person [application person-client]
  (let [person-from-onr (when-let [oid (:person-oid application)]
                          (person-service/get-person person-client oid))
        yksiloity       (or (-> person-from-onr :yksiloity)
                            (-> person-from-onr :yksiloityVTJ))
        person-info     (if yksiloity
                          (person-info-from-onr-person person-from-onr)
                          (person-info-from-application application))]
    (merge
     {:oid         (:person-oid application)
      :turvakielto (-> person-from-onr :turvakielto boolean)
      :yksiloity   (boolean yksiloity)}
     person-info)))

(defn get-application-with-human-readable-koodis
  "Get application that has human-readable koodisto values populated
   onto raw koodi values."
  [application-key session organization-service tarjonta-service ohjausparametrit-service person-client]
  (let [bare-application (aac/get-latest-application-by-key application-key session organization-service)
        tarjonta-info    (tarjonta-parser/parse-tarjonta-info-by-haku
                          tarjonta-service
                          ohjausparametrit-service
                          (:haku bare-application)
                          (:hakukohde bare-application))
        form             (-> (:form bare-application)
                             form-store/fetch-by-id
                             (populate-hakukohde-answer-options tarjonta-info)
                             (hakija-form-service/populate-can-submit-multiple-applications tarjonta-info))
        application      (populate-koodisto-fields bare-application form)
        person           (get-person application person-client)]
    (aac/check-application-access application-key session organization-service [:view-applications :edit-applications])
    {:application          (-> application
                               (dissoc :person-oid)
                               (assoc :person (if (:yksiloity person)
                                                (populate-person-koodisto-fields person)
                                                person))
                               (merge tarjonta-info))
     :form                 form
     :hakukohde-reviews    (parse-application-hakukohde-reviews application-key)
     :events               (application-store/get-application-events application-key)
     :review               (application-store/get-application-review application-key)
     :review-notes         (application-store/get-application-review-notes application-key)
     :information-requests (information-request-store/get-information-requests application-key)}))

(defn get-excel-report-of-applications-by-key
  [application-keys selected-hakukohde skip-answers? session organization-service tarjonta-service ohjausparametrit-service]
  (let [applications         (application-store/get-applications-by-keys application-keys)
        forms                (->> applications
                                  (map :form-key)
                                  (distinct))
        allowed-forms        (set (filter #(form-access-control/form-allowed-by-key?
                                             %
                                             session
                                             organization-service
                                             [:view-applications :edit-applications])
                                          forms))
        allowed-applications (filter #(contains? allowed-forms (:form-key %)) applications)
        application-reviews  (->> allowed-applications
                                  (map :key)
                                  application-store/get-application-reviews-by-keys
                                  (reduce #(assoc %1 (:application-key %2) %2) {}))]
    (ByteArrayInputStream. (excel/export-applications allowed-applications
                                                      application-reviews
                                                      selected-hakukohde
                                                      skip-answers?
                                                      tarjonta-service
                                                      ohjausparametrit-service))))

(defn- save-application-hakukohde-reviews
  [virkailija application-key hakukohde-reviews session]
  (doseq [[hakukohde review] hakukohde-reviews]
    (doseq [[review-requirement review-state] review]
      (application-store/save-application-hakukohde-review
        virkailija
        application-key
        (name hakukohde)
        (name review-requirement)
        (name review-state)
        session))))

(defn save-application-review
  [review session organization-service]
  (let [application-key (:application-key review)
        virkailija (virkailija-edit/upsert-virkailija session)]
    (aac/check-application-access
      application-key
      session
      organization-service
      [:edit-applications])
    (application-store/save-application-review review session virkailija)
    (save-application-hakukohde-reviews virkailija application-key (:hakukohde-reviews review) session)
    {:review (application-store/get-application-review application-key)
     :events (application-store/get-application-events application-key)
     :hakukohde-reviews (parse-application-hakukohde-reviews application-key)}))

(defn mass-update-application-states
  [session organization-service application-keys hakukohde-oid from-state to-state]
  (doseq [application-key application-keys]
    (aac/check-application-access
      application-key
      session
      organization-service
      [:edit-applications]))
  (application-store/mass-update-application-states session application-keys hakukohde-oid from-state to-state)
  {})

(defn send-modify-application-link-email [application-key session organization-service tarjonta-service]
  (when-let [application-id (:id (aac/get-latest-application-by-key application-key session organization-service))]
    (email/start-email-submit-confirmation-job tarjonta-service application-id)
    (application-store/add-application-event {:application-key application-key
                                              :event-type      "modification-link-sent"}
                                             session)))

(defn add-review-note [note session organization-service]
  (aac/check-application-access (:application-key note)
                                session
                                organization-service
                                [:view-applications :edit-applications])
  (application-store/add-review-note note session))

(defn remove-review-note [note-id]
  (application-store/remove-review-note note-id))
