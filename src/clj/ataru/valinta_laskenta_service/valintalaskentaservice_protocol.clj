(ns ataru.valinta-laskenta-service.valintalaskentaservice-protocol)

(defprotocol ValintaLaskentaService
  (hakemuksen-tulokset [this hakukohde-oid haku-oid])
  (valinnan-tuloksien-hakeminen-sallittu? [this superuser? haku]))