(ns ataru.applications.permission-check
  (:require [taoensso.timbre :refer [error]]
            [ataru.applications.application-access-control :as aac]
            [ataru.applications.application-store :as application-store]))

(defn check [tarjonta-service check-dto]
  (try
    {:accessAllowed
     (let [applications                 (application-store/persons-applications-authorization-data
                                         (:personOidsForSamePerson check-dto))
           tarjoajat                    (aac/applications-tarjoajat tarjonta-service
                                                                    applications)
           authorized-organization-oids (set (:organisationOids check-dto))]
       (boolean
        (some #(aac/authorized? authorized-organization-oids tarjoajat %)
              applications)))}
    (catch Exception e
      (let [msg "Error while checking permission"]
        (error e msg)
        {:accessAllowed false
         :errorMessage msg}))))
