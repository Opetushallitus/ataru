(ns ataru.tarjonta-service.tarjonta-protocol)

(defprotocol TarjontaService
  (get-hakukohde [this hakukohde-oid])
  (get-hakukohteet [this hakukohde-oids])
  (get-hakukohde-name [this hakukohde-oid])
  (hakukohde-search [this haku-oid organization-oid])
  (get-haku [this haku-oid])
  (hakus-by-form-key [this form-key])
  (get-haku-name [this haku-oid])
  (get-koulutus [this haku-oid])
  (get-koulutukset [this koulutus-oids])
  (clear-haku-caches [this haku-oid])
  (get-haku-oids [this]))
