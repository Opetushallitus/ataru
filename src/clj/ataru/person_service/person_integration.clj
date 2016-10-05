(ns ataru.person-service.person-integration
  (:require
   [taoensso.timbre :as log]
   [ataru.applications.application-store :as application-store]))

(defn- extract-field [{:keys [answers]} field]
  (some (fn [{:keys [key value]}]
          (when (= key field)
            value))
        answers))

(defn- extract-person [application]
  {:email          (extract-field application "email")
   :firstName      (extract-field application "first-name")
   :personId       (extract-field application "ssn")
   :lastName       (extract-field application "last-name")
   :nativeLanguage (extract-field application "language")
   :idpEntitys     []})

(defn upsert-person
  "Fetch person OID from person service and store it to database"
  [{:keys [application-id]}
   {:keys [person-service]}]
  {:pre [(not (nil? application-id))
         (not (nil? person-service))]}
  (log/info "Trying to add applicant from application " application-id " to person service")
  (let [person     (->> (application-store/get-application application-id)
                        extract-person
                        (.upsert-person person-service))
        person-oid (:personOid person)]
    (log/info "Added person " person-oid " to person service")
    (application-store/add-person-oid application-id person-oid)
    {:transition {:id :final}}))

(def job-definition {:steps {:initial upsert-person}
                     :type  (str (ns-name *ns*))})
