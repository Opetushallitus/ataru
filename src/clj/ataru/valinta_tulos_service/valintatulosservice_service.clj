(ns ataru.valinta-tulos-service.valintatulosservice_service
  (:require [ataru.valinta-tulos-service.valintatulosservice-client :as client]
            [ataru.valinta-tulos-service.valintatulosservice-protocol :refer [ValintaTulosService]]
            [clojure.set :refer [union]]))

(defn- get-hakukohteen-ehdolliset
  [cas-client hakukohde-oid]
  (let [hakukohteessa       (future
                              (client/get-hyvaksynnan-ehto-hakukohteessa
                               cas-client
                               hakukohde-oid))
        valintatapajonoissa (client/get-hyvaksynnan-ehto-valintatapajonoissa
                             cas-client
                             hakukohde-oid)]
    (apply union
           (set (keys @hakukohteessa))
           (map #(set (keys %))
                (vals valintatapajonoissa)))))

(defrecord RemoteValintaTulosService [cas-client valinta-laskenta-service]
  ValintaTulosService
  (hakukohteen-ehdolliset [_ hakukohde-oid]
    (get-hakukohteen-ehdolliset cas-client hakukohde-oid))
  (valinnan-tulos-hakemukselle [_ haku-oid hakemus-oid]
    (client/get-valinnan-tulos-hakemukselle cas-client haku-oid hakemus-oid))
  (valinnantulos-hakemukselle-tilahistorialla [_ hakemus-oid]
    (client/get-valinnantulos-with-tilahistoria cas-client hakemus-oid))
  (valinnantulos-monelle-tilahistorialla [_ hakemus-oids]
    (client/get-valinnantulos-with-tilahistoria-monelle cas-client hakemus-oids))
  (change-kevyt-valinta-property [_ valintatapajono-oid body unmodified-since]
    (client/patch-valinnantulos-kevyt-valinta-property cas-client valintatapajono-oid body unmodified-since)))
