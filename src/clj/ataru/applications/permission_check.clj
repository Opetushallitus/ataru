(ns ataru.applications.permission-check
  (:require [taoensso.timbre :as log]
            [ataru.applications.application-access-control :as aac]
            [ataru.applications.application-store :as application-store]))

(defn check [tarjonta-service check-dto]
  (try
    (let [orgs (set (:organisationOids check-dto))]
      {:accessAllowed
       (->> (application-store/persons-applications-authorization-data
             (:personOidsForSamePerson check-dto))
            (aac/filter-authorized tarjonta-service
                                   (some-fn (partial aac/authorized-by-form? orgs)
                                            (partial aac/authorized-by-tarjoajat? orgs)))
            not-empty
            boolean)})
    (catch Exception e
      (let [msg "Error while checking permission"]
        (log/error e msg)
        {:accessAllowed false
         :errorMessage  msg}))))
