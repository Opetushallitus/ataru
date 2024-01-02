(ns ataru.valinta-tulos-service.valintatulosservice-protocol)

(defprotocol ValintaTulosService
  (hakukohteen-ehdolliset [this hakukohde-oid])
  (valinnan-tulos-hakemukselle [this haku-oid hakemus-oid])
  (valinnantulos-hakemukselle-tilahistorialla [this hakemus-oid])
  (valinnantulos-monelle-tilahistorialla [this hakemus-oids])
  (change-kevyt-valinta-property [this valintatapajono-oid body if-unmodified-since])
  (hyvaksynnan-ehto-hakukohteessa-hakemus [this hakukohde-oid application-key])
  (add-hyvaksynnan-ehto-hakukohteessa-hakemus [this ehto hakukohde-oid application-key if-unmodified-since])
  (delete-hyvaksynnan-ehto-hakukohteessa-hakemus [this hakukohde-oid application-key if-unmodified-since])
  (hyvaksynnan-ehto-valintatapajonoissa-hakemus [this hakukohde-oid application-key])
  (hyvaksynnan-ehto-hakemukselle [this application-key])
  (hyvaksynnan-ehto-hakukohteessa-muutoshistoria [this hakukohde-oid application-key]))