(ns ataru.haku.haku-access-control
  (:require [ataru.util.access-control-utils :as access-control-utils]
            [ataru.applications.application-store :as application-store]
            [ataru.virkailija.user.organization-client :as organization-client]))

(defn get-haut [session organization-service]
  (let [organizations     (access-control-utils/organizations session)
        organization-oids (map :oid organizations)]
    (cond (some #{organization-client/oph-organization} organization-oids)
          (application-store/get-all-haut)

          (empty? organization-oids)
          []

          :else
          (application-store/get-haut (access-control-utils/all-org-oids organization-service organizations)))))
