(ns ataru.valinta-tulos-service.valintatulosservice-protocol)

(defprotocol ValintaTulosService
  (hakukohteen-ehdolliset [this hakukohde-oid])
  (valinnan-tulos-hakemukselle [this haku-oid hakemus-oid])
  (valinnantulos-hakemukselle-tilahistorialla [this hakemus-oid])
  (valinnantulos-monelle-tilahistorialla [this hakemus-oids])
  (change-kevyt-valinta-property [this valintatapajono-oid body unmodified-since]))