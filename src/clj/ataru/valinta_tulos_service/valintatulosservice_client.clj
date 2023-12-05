(ns ataru.valinta-tulos-service.valintatulosservice-client
  (:require [ataru.cas.client :as cas-client]
            [ataru.config.url-helper :as url-helper]
            [cheshire.core :as json]
            [cuerdas.core :as str]))

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
              :valinta-tulos-service.valinnan-tulos.hakemus)
        {:keys [status body]} (cas-client/cas-authenticated-post cas-client url hakemus-oids)]
    (if (= 200 status)
      (json/parse-string body true)
      (throw (new RuntimeException (str "Could not get " url ", "
                                        "status: " status ", "
                                        "parameters: hakemus-oids " (str/join hakemus-oids ", ") ", "
                                        "body: " body))))))

(defn get-valinnantulos-with-tilahistoria
  [cas-client hakemus-oid]
  (let [url (str (url-helper/resolve-url
                   :valinta-tulos-service.valinnan-tulos.hakemus) "?hakemusOid=" hakemus-oid)
        {:keys [status body]} (cas-client/cas-authenticated-get cas-client url)]
    (if (= 200 status)
      (json/parse-string body true)
      (throw (new RuntimeException (str "Could not get " url ", "
                                        "status: " status ", "
                                        "parameters: hakemus-oid " hakemus-oid ", "
                                        "body: " body))))))

(defn patch-valinnantulos-kevyt-valinta-property
  [cas-client valintatapajono-oid body unmodified-since]
  (let [url (str (url-helper/resolve-url
                   :valinta-tulos-service.valinnan-tulos) "/" valintatapajono-oid "?erillishaku=true")
        {:keys [status]} (cas-client/cas-authenticated-patch
                           cas-client url body (fn [] {:headers {"X-If-Unmodified-Since" unmodified-since}}))]
    (if (not= 200 status)
      (throw (new RuntimeException (str "Could not get " url ", "
                                        "status: " status ", "
                                        "parameters: valintatapajono-oid " valintatapajono-oid
                                        ", X-If-Unmodified-Since" unmodified-since ", request body " body)))
      nil)))
