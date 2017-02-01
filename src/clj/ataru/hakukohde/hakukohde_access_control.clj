(ns ataru.hakukohde.hakukohde-access-control
  (:require
   [ataru.virkailija.user.session-organizations :as session-orgs]
   [ataru.tarjonta-service.tarjonta-client :as tarjonta-client]
   [ataru.applications.application-store :as application-store]))

(defn- add-hakukohde-names
  [tarjonta-service hakukohde-results]
  (map (fn [hakukohde-result]
         (let [hakukohde (.get-hakukohde tarjonta-service (:hakukohde hakukohde-result))]
           (merge hakukohde-result {:hakukohde-name (-> hakukohde :hakukohteenNimet :kieli_fi)})))
       hakukohde-results))

(defn get-hakukohteet [session organization-service tarjonta-service]
  (session-orgs/run-org-authorized
   session
   organization-service
   vector
   #(add-hakukohde-names tarjonta-service (application-store/get-hakukohteet %))
   #(add-hakukohde-names tarjonta-service (application-store/get-all-hakukohteet))))
