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

(defn get-valinnantulos-with-tilahistoria-monelle
  [cas-client hakemus-oids]
  (let [url (url-helper/resolve-url
              :valinta-tulos-service.valinnan-tulos.hakemus)]
    (cas-client/cas-authenticated-post cas-client url hakemus-oids)))

(defn get-valinnantulos-with-tilahistoria
  [cas-client hakemus-oid]
  (let [url (str (url-helper/resolve-url
                   :valinta-tulos-service.valinnan-tulos.hakemus) "?hakemusOid=" hakemus-oid)]
    (cas-client/cas-authenticated-get cas-client url)))

(defn patch-valinnantulos-kevyt-valinta-property
  [cas-client valintatapajono-oid body if-unmodified-since]
  (let [url (url-helper/resolve-url
              :valinta-tulos-service.valinnan-tulos valintatapajono-oid)]
       (cas-client/cas-authenticated-patch
         cas-client url body (fn [] {:headers {"X-If-Unmodified-Since" if-unmodified-since}}))))

(defn get-hyvaksynnan-ehto-hakukohteessa-hakemus
  [cas-client hakukohde-oid application-key]
  (let [url (url-helper/resolve-url
              :valinta-tulos-service.hyvaksynnan-ehto.hakukohteessa.hakemus hakukohde-oid application-key)]
        (cas-client/cas-authenticated-get cas-client url)))

(defn put-hyvaksynnan-ehto-hakukohteessa-hakemus
  [cas-client ehto hakukohde-oid application-key if-unmodified-since]
  (let [url (url-helper/resolve-url
              :valinta-tulos-service.hyvaksynnan-ehto.hakukohteessa.hakemus hakukohde-oid application-key)]
    (cas-client/cas-authenticated-put
      cas-client url ehto (fn []
                              {:headers (if (some? if-unmodified-since)
                                          {"If-Unmodified-Since" if-unmodified-since}
                                          {"If-None-Match" "*"}) }))))

(defn delete-hyvaksynnan-ehto-hakukohteessa-hakemus
  [cas-client hakukohde-oid application-key if-unmodified-since]
  (let [url (url-helper/resolve-url
              :valinta-tulos-service.hyvaksynnan-ehto.hakukohteessa.hakemus hakukohde-oid application-key)]
    (cas-client/cas-authenticated-delete
      cas-client url (fn [] {:headers {"If-Unmodified-Since" if-unmodified-since}}))))

(defn get-hyvaksynnan-ehto-valintatapajonoissa-hakemus
  [cas-client hakukohde-oid application-key]
  (let [url (url-helper/resolve-url
              :valinta-tulos-service.hyvaksynnan-ehto.valintatapajonoissa.hakemus hakukohde-oid application-key)]
    (cas-client/cas-authenticated-get cas-client url)))

(defn get-hyvaksynnan-ehto-hakemukselle
  [cas-client application-key]
  (let [url (url-helper/resolve-url
              :valinta-tulos-service.hyvaksynnan-ehto.hakemukselle application-key)]
    (cas-client/cas-authenticated-get cas-client url)))

(defn get-hyvaksynnan-ehto-hakukohteessa-muutoshistoria
  [cas-client hakukohde-oid application-key]
  (let [url (url-helper/resolve-url
              :valinta-tulos-service.hyvaksynnan-ehto.muutoshistoria hakukohde-oid application-key)]
    (cas-client/cas-authenticated-get cas-client url)))
