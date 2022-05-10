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

(defrecord RemoteValintaTulosService [cas-client]
  ValintaTulosService
  (hakukohteen-ehdolliset [_ hakukohde-oid]
    (get-hakukohteen-ehdolliset cas-client hakukohde-oid)))
