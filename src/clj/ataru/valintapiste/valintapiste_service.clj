(ns ataru.valintapiste.valintapiste-service
  (:require [ataru.applications.application-store :as application-store]
            [ataru.kk-application-payment.kk-application-payment :as kk-application-payment]))

(defn get-application-info-for-valintapiste
  [tarjonta-service haku-oid hakukohde-oid]
  (kk-application-payment/filter-out-unpaid-kk-applications
    tarjonta-service
    (application-store/get-application-info-for-valintapiste haku-oid hakukohde-oid)
    :henkilo_oid :haku_oid))