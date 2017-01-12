(ns ataru.hakukohde.hakukohde-access-control
  (:require [ataru.applications.application-store :as application-store]
            [ataru.util.access-control-utils :as access-control-utils]
            [ataru.virkailija.user.organization-client :as organization-client]))

(defn get-hakukohteet [session organization-service]
  (let [organizations     (access-control-utils/organizations session)
        organization-oids (map :oid organizations)]
    (cond (some #{organization-client/oph-organization} organization-oids)
          (application-store/get-all-hakukohteet)

          (empty? organization-oids)
          []

          :else
          (application-store/get-hakukohteet (access-control-utils/all-org-oids organization-service organizations)))))
