(ns ataru.haku.haku-access-control
  (:require
   [ataru.virkailija.user.session-organizations :as session-orgs]
   [ataru.applications.application-store :as application-store]))

(defn- add-haku-names
  [tarjonta-service haku-results]
  (map (fn [haku-result]
         (let [haku (.get-haku tarjonta-service (:haku haku-result))]
           (merge haku-result {:haku-name (-> haku :nimi :kieli_fi)})))
       haku-results))

(defn get-haut [session organization-service tarjonta-service]
  (session-orgs/run-org-authorized
   session
   organization-service
   vector
   #(add-haku-names tarjonta-service (application-store/get-haut %))
   #(add-haku-names tarjonta-service (application-store/get-all-haut))))
