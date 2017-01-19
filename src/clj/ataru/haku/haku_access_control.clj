(ns ataru.haku.haku-access-control
  (:require
   [ataru.virkailija.user.session-organizations :as session-orgs]
   [ataru.applications.application-store :as application-store]))

(defn get-haut [session organization-service]
  (session-orgs/run-org-authorized
   session
   organization-service
   vector
   #(application-store/get-haut %)
   #(application-store/get-all-haut)))
