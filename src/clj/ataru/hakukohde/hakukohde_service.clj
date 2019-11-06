(ns ataru.hakukohde.hakukohde-service
  (:require [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]))

(defn get-hakukohteet [tarjonta-service haku-oid organization-oid]
  (tarjonta/hakukohde-search
    tarjonta-service
    haku-oid
    organization-oid))
