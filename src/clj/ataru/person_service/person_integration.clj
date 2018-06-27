(ns ataru.person-service.person-integration
  (:require
   [clojure.core.match :refer [match]]
   [taoensso.timbre :as log]
   [ataru.applications.application-store :as application-store]
   [ataru.person-service.person-service :as person-service]))

(defn upsert-and-log-person [person-service application-id]
  (let [application (application-store/get-application application-id)]
    (try
      (let [{:keys [status oid]} (person-service/create-or-find-person
                                  person-service
                                  application)]
        (match status
          :created
          (log/info "Added person" oid "to oppijanumerorekisteri")
          :exists
          (log/info "Person" oid "already existed in oppijanumerorekisteri"))
        (application-store/add-person-oid application-id oid)
        (log/info "Added person" oid "to application" application-id))
      (catch IllegalArgumentException e
        (log/error e "Failed to create-or-find person for application"
                   application-id)))
    {:transition {:id :final}}))

(defn upsert-person
  [{:keys [application-id]}
   {:keys [person-service]}]
  {:pre [(not (nil? application-id))
         (not (nil? person-service))]}
  (log/info "Trying to add applicant from application"
            application-id
            "to oppijanumerorekisteri")
  (upsert-and-log-person person-service application-id))

(def job-type (str (ns-name *ns*)))

(def job-definition {:steps {:initial upsert-person}
                     :type  job-type})
