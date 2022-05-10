(ns ataru.valinta-laskenta-service.valintalaskentaservice-service
  (:require [ataru.valinta-laskenta-service.valintalaskentaservice-protocol :refer [ValintaLaskentaService]]
            [ataru.valinta-tulos-service.valintatulosservice-protocol :as vts]
            [ataru.valinta-laskenta-service.valintalaskentaservice-client :as client]))

(defn- get-valinnan-ja-laskennan-tulos-hakemukselle
  [cas-client valinta-tulos-service haku-oid hakemus-oid]
  (let [pisteet (future
                (client/hakemuksen-laskennan-tiedot cas-client haku-oid hakemus-oid))
        tulos (vts/valinnan-tulos-hakemukselle valinta-tulos-service haku-oid hakemus-oid)]
    (prn tulos)
    (prn @pisteet)
    (apply merge {:pisteet @pisteet} {:tulos tulos})))

(defrecord RemoteValintaLaskentaService [cas-client valinta-tulos-service]
  ValintaLaskentaService

  (hakemuksen-tulokset [_ hakukohde-oid haku-oid]
    (get-valinnan-ja-laskennan-tulos-hakemukselle cas-client valinta-tulos-service hakukohde-oid haku-oid)))