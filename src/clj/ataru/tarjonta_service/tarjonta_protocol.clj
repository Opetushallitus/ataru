(ns ataru.tarjonta-service.tarjonta-protocol)

(defprotocol TarjontaService
  (get-hakukohde [this hakukohde-oid])
  (get-hakukohde-name [this hakukohde-oid])
  (hakukohteet-by-organization [this organization-oid])
  (get-haku [this haku-oid])
  (get-haku-name [this haku-oid])
  (get-koulutus [this haku-oid]))
