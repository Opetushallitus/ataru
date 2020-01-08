(ns ataru.valinta-tulos-service.service
  (:require [ataru.cas.client :as cas-client]
            [ataru.config.url-helper :as url-helper]
            [cheshire.core :as json]))

(defprotocol ValintaTulosService
  (hakukohteen-ehdolliset [this hakukohde-oid]))

(defn- get-hyvaksynnan-ehto-hakukohteessa
  [cas-client hakukohde-oid]
  (let [url                   (url-helper/resolve-url
                               :valinta-tulos-service.hyvaksynnan-ehto.hakukohteessa
                               hakukohde-oid)
        {:keys [status body]} (cas-client/cas-authenticated-get cas-client url)]
    (if (= 200 status)
      (json/parse-string body false)
      (throw (new RuntimeException (str "Could not get " url ", "
                                        "status: " status ", "
                                        "body: " body))))))

(defn- get-hyvaksynnan-ehto-valintatapajonoissa
  [cas-client hakukohde-oid]
  (let [url                   (url-helper/resolve-url
                               :valinta-tulos-service.hyvaksynnan-ehto.valintatapajonoissa
                               hakukohde-oid)
        {:keys [status body]} (cas-client/cas-authenticated-get cas-client url)]
    (if (= 200 status)
      (json/parse-string body false)
      (throw (new RuntimeException (str "Could not get " url ", "
                                        "status: " status ", "
                                        "body: " body))))))

(defn- get-hakukohteen-ehdolliset
  [cas-client hakukohde-oid]
  (let [hakukohteessa       (future
                              (get-hyvaksynnan-ehto-hakukohteessa
                               cas-client
                               hakukohde-oid))
        valintatapajonoissa (get-hyvaksynnan-ehto-valintatapajonoissa
                             cas-client
                             hakukohde-oid)]
    (apply clojure.set/union
           (set (keys @hakukohteessa))
           (map #(set (keys %))
                (vals valintatapajonoissa)))))

(defrecord RemoteValintaTulosService [cas-client]
  ValintaTulosService
  (hakukohteen-ehdolliset [_ hakukohde-oid]
    (get-hakukohteen-ehdolliset cas-client hakukohde-oid)))
