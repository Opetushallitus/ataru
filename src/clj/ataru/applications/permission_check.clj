(ns ataru.applications.permission-check
  (:require [taoensso.timbre :refer [error]]
            [ataru.applications.application-store :refer [get-organization-oids-of-applications-of-persons]]))

(defn check [check-dto]
  (try
    {:accessAllowed
     (not (empty? (clojure.set/intersection
                   (set (:organisationOids check-dto))
                   (get-organization-oids-of-applications-of-persons
                    (:personOidsForSamePerson check-dto)))))}
    (catch Exception e
      (let [msg "Error while checking permission"]
        (error e msg)
        {:accessAllowed false
         :errorMessage msg}))))
