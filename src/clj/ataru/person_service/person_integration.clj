(ns ataru.person-service.person-integration
  (:require
   [taoensso.timbre :as log]
   [ataru.applications.application-store :as application-store]))

(defn- extract-field [{:keys [answers]} field]
  (some (fn [{:keys [key value]}]
          (when (= key field)
            value))
        answers))

(def finnish-date-regex #"(\d{2})\.(\d{2})\.(\d{4})")

(defn- convert-birth-date [finnish-format-date]
  {:post [(not= % "--")]} ;; When no match for finnish date, this would result in "--"
  (let [[_ day month year] (re-find finnish-date-regex finnish-format-date)]
    (str year "-" month "-" day)))

(defn- extract-birth-date [application]
  (let [finnish-format-date (extract-field application "birth-date")]
    (if-not finnish-format-date (throw (Exception. "Expected a birth-date in application")))
    (convert-birth-date finnish-format-date)))

(defn extract-person [application]
  (let [email        (extract-field application "email")
        basic-fields {:email          email
                      :firstName      (extract-field application "first-name")
                      :lastName       (extract-field application "last-name")
                      :gender         (extract-field application "gender")
                      :nativeLanguage (extract-field application "language")
                      :nationality    (extract-field application "nationality")
                      :idpEntitys     [{:idpEntityId "oppijaToken" :identifier email}]}
        person-id    (extract-field application "ssn")]
    (if person-id
      (assoc basic-fields :personId person-id)
      (assoc basic-fields :birthDate (extract-birth-date application)))))

(defn upsert-and-log-person [person-service application-id]
  (let [person-to-send (-> (application-store/get-application application-id)
                           (extract-person))]
    (log/info "Sending person" person-to-send)
    (.upsert-person person-service person-to-send)))

(defn upsert-person
  "Fetch person OID from person service and store it to database"
  [{:keys [application-id]}
   {:keys [person-service]}]
  {:pre [(not (nil? application-id))
         (not (nil? person-service))]}
  (log/info "Trying to add applicant from application" application-id "to person service")
  (let [response-person (upsert-and-log-person person-service application-id)
        person-oid      (:personOid response-person)]
    (log/info "Added person" person-oid "to person service")
    (application-store/add-person-oid application-id person-oid))
    {:transition {:id :final}})

(def job-type (str (ns-name *ns*)))

(def job-definition {:steps {:initial upsert-person}
                     :type  job-type})
