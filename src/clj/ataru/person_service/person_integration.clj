(ns ataru.person-service.person-integration
  (:require [ataru.applications.application-store :as application-store]))

(defn- extract-ssn [{:keys [answers]}]
  (some (fn [{:keys [key value]}]
          (when (= key "ssn")
            value))
        answers))

(defn store-person-oid
  "Fetch person OID from person service and store it to database"
  [{:keys [application-id]}
   {:keys [person-service]}]
  {:pre [(not (nil? application-id))
         (not (nil? person-service))]}
  (when-let [ssn (-> (application-store/get-application application-id)
                     (extract-ssn))]
    (let [person     (.get-person person-service ssn)
          person-oid (:oidHenkilo person)]
      (application-store/add-person-oid application-id person-oid)
      {:transition    {:id :final}})))

(def job-definition {:steps {:initial store-person-oid}})
