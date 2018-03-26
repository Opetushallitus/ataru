(ns ataru.applications.permission-check
  (:require [taoensso.timbre :refer [error]]
            [ataru.applications.application-access-control :as aac]
            [ataru.applications.application-store :as application-store]))

(defn check [tarjonta-service check-dto]
  (try
    {:accessAllowed
     (->> (application-store/persons-applications-authorization-data
           (:personOidsForSamePerson check-dto))
          (aac/filter-authorized tarjonta-service
                                 (set (:organisationOids check-dto)))
          not-empty)}
    (catch Exception e
      (let [msg "Error while checking permission"]
        (error e msg)
        {:accessAllowed false
         :errorMessage msg}))))
