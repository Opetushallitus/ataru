(ns ataru.valinta-laskenta-service.valintalaskentaservice-service
  (:require [ataru.valinta-laskenta-service.valintalaskentaservice-protocol :refer [ValintaLaskentaService]]
            [ataru.valinta-laskenta-service.valintalaskentaservice-client :as client]))

(defrecord RemoteValintaLaskentaService [cas-client]
  ValintaLaskentaService

  (hakemuksen-tulokset [_ hakukohde-oid haku-oid]
    (client/hakemuksen-laskennan-tiedot cas-client hakukohde-oid haku-oid)))