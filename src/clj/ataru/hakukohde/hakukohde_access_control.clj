(ns ataru.hakukohde.hakukohde-access-control
  (:require [ataru.applications.application-store :as application-store]
            [ataru.util.access-control-utils :as access-control-utils]
            [ataru.virkailija.user.organization-client :as organization-client]))

(defn get-hakukohteet [session organization-service]
  (let [organization-oids (access-control-utils/org-oids session)]
    (cond (some #{organization-client/oph-organization} organization-oids)
          (application-store/get-all-hakukohteet)

          (empty? organization-oids)
          []

          :else
          (application-store/get-hakukohteet (access-control-utils/all-org-oids organization-service organization-oids)))))
