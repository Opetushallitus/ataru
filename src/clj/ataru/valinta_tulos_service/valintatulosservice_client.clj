(ns ataru.valinta-tulos-service.valintatulosservice-client
  (:require [ataru.cas.client :as cas-client]
            [ataru.config.url-helper :as url-helper]
            [cheshire.core :as json]))

(defn get-hyvaksynnan-ehto-hakukohteessa
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

(defn get-hyvaksynnan-ehto-valintatapajonoissa
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

(defn get-valinnan-tulos-hakemukselle
  [cas-client haku-oid hakemus-oid]
  (let [url (url-helper/resolve-url
              :valinta-tulos-service.valinnan-tulos.hakemukselle
              haku-oid hakemus-oid)
        {:keys [status body]} (cas-client/cas-authenticated-get cas-client url)]
    (if (= 200 status)
      (json/parse-string body true)
      (throw (new RuntimeException (str "Could not get " url ", "
                                        "status: " status ", "
                                        "parameters: haku-oid " haku-oid ", hakemus-oid " hakemus-oid ", "
                                        "body: " body))))))