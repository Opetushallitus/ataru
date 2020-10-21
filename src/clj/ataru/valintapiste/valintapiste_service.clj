(ns ataru.valintapiste.valintapiste-service
  (:require [ataru.applications.application-store :as application-store]))

(defn get-application-info-for-valintapiste
  [haku-oid hakukohde-oid]
  (application-store/get-application-info-for-valintapiste haku-oid hakukohde-oid))