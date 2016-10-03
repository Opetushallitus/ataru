(ns ataru.person-service.person-integration
  (:require [ataru.applications.application-store :as application-store]))

(defn- extract-field [{:keys [answers]} field]
  (some (fn [{:keys [key value]}]
          (when (= key field)
            value))
        answers))

(defn store-person-oid
  "Fetch person OID from person service and store it to database"
  [{:keys [application-id]}
   {:keys [person-service]}]
  {:pre [(not (nil? application-id))
         (not (nil? person-service))]}
  (let [application (application-store/get-application application-id)
        ssn         (extract-field application "ssn")
        email       (extract-field application "email")]
    (if (and (some? ssn)
             (some? email))
      (let [person     (.get-person person-service ssn email)
            person-oid (:oidHenkilo person)]
        (application-store/add-person-oid application-id person-oid)
        {:transition {:id :final}}))))

(def job-definition {:steps {:initial store-person-oid}})
