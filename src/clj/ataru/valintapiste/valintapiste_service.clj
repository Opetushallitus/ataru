(ns ataru.valintapiste.valintapiste-service
  (:require [ataru.applications.application-store :as application-store]
            [ataru.kk-application-payment.kk-application-payment :as kk-application-payment]))

(defn get-application-info-for-valintapiste
  [haku-oid hakukohde-oid]
  (kk-application-payment/remove-kk-applications-with-unapproved-payments
    (application-store/get-application-info-for-valintapiste haku-oid hakukohde-oid)
    :hakemus_oid))