(ns ataru.hakukohde.hakukohde-access-control
  (:require
   [ataru.virkailija.user.session-organizations :as session-orgs]
   [ataru.applications.application-store :as application-store]))

(defn get-hakukohteet [session organization-service]
  (session-orgs/run-org-authorized
   session
   organization-service
   vector
   #(application-store/get-hakukohteet %)
   #(application-store/get-all-hakukohteet)))
