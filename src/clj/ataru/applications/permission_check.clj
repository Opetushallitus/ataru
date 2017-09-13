(ns ataru.applications.permission-check
  (:require [taoensso.timbre :refer [error]]
            [ataru.virkailija.user.ldap-client :refer [get-organization-oids-for-right]]
            [ataru.virkailija.user.organization-service :refer [get-all-organizations]]
            [ataru.virkailija.user.organization-client :refer [oph-organization]]
            [ataru.applications.application-store :refer [get-organization-oids-of-applications-of-persons]]))

(defn- application-access-in-organizations [check-dto]
  (let [[_ view-organizations] (get-organization-oids-for-right
                                :view-applications
                                (:loggedInUserRoles check-dto))
        [_ edit-organizations] (get-organization-oids-for-right
                                :edit-applications
                                (:loggedInUserRoles check-dto))]
    (set (concat view-organizations edit-organizations))))

(defn- expanded-organizations [organization-service organizations]
  (->> organizations
       (map #(hash-map :oid %))
       (get-all-organizations organization-service)
       (map :oid)
       set))

(defn check [organization-service check-dto]
  (try
    (let [organizations (application-access-in-organizations check-dto)]
      {:accessAllowed
       (cond (empty? organizations)
             false
             (contains? organizations oph-organization)
             true
             :else
             (not (empty? (clojure.set/intersection
                           (expanded-organizations organization-service organizations)
                           (get-organization-oids-of-applications-of-persons
                            (:personOidsForSamePerson check-dto))))))})
    (catch Exception e
      (let [msg "Error while checking permission"]
        (error e msg)
        {:accessAllowed false
         :errorMessage msg}))))
