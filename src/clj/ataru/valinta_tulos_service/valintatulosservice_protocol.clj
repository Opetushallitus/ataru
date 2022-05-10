(ns ataru.valinta-tulos-service.valintatulosservice-protocol)

(defprotocol ValintaTulosService
  (hakukohteen-ehdolliset [this hakukohde-oid]))