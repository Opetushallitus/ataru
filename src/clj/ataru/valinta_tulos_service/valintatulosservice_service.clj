(ns ataru.valinta-tulos-service.valintatulosservice-service
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
  (change-kevyt-valinta-property [_ valintatapajono-oid body if-unmodified-since]
    (client/patch-valinnantulos-kevyt-valinta-property cas-client valintatapajono-oid body if-unmodified-since))
  (hyvaksynnan-ehto-hakukohteessa-hakemus [_ hakukohde-oid application-key]
    (client/get-hyvaksynnan-ehto-hakukohteessa-hakemus cas-client hakukohde-oid application-key))
  (add-hyvaksynnan-ehto-hakukohteessa-hakemus [_ ehto hakukohde-oid application-key if-unmodified-since]
    (client/put-hyvaksynnan-ehto-hakukohteessa-hakemus cas-client ehto hakukohde-oid application-key if-unmodified-since))
  (delete-hyvaksynnan-ehto-hakukohteessa-hakemus [_ hakukohde-oid application-key if-unmodified-since]
    (client/delete-hyvaksynnan-ehto-hakukohteessa-hakemus cas-client hakukohde-oid application-key if-unmodified-since))
  (hyvaksynnan-ehto-valintatapajonoissa-hakemus [_ hakukohde-oid application-key]
    (client/get-hyvaksynnan-ehto-valintatapajonoissa-hakemus cas-client hakukohde-oid application-key))
  (hyvaksynnan-ehto-hakemukselle [_ application-key]
    (client/get-hyvaksynnan-ehto-hakemukselle cas-client application-key))
  (hyvaksynnan-ehto-hakukohteessa-muutoshistoria [_ hakukohde-oid application-key]
    (client/get-hyvaksynnan-ehto-hakukohteessa-muutoshistoria cas-client hakukohde-oid application-key)))
