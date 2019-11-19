(ns ataru.valintaperusteet.client
  (:require [ataru.cache.cache-service :as cache]
            [ataru.cas.client :as cas]
            [ataru.config.url-helper :as url]
            [cheshire.core :as json]
            [schema.core :as s]))

(s/defschema Valintatapajono
  {:oid      s/Str
   :name     s/Str
   :priority s/Int})

(def ^:private valintatapajono-checker (s/checker Valintatapajono))

(defn- parse-valintatapajono
  [data]
  {:oid      (:oid data)
   :name     (:nimi data)
   :priority (:prioriteetti data)})

(defn- get-valintatapajono
  [cas-client oid]
  (let [url    (url/resolve-url :valintaperusteet-service.valintatapajono oid)
        result (cas/cas-authenticated-get cas-client url)]
    (if (= 200 (:status result))
      (parse-valintatapajono (json/parse-string (:body result) true))
      (throw (new RuntimeException (str "Could not get " url ", "
                                        "status: " (:status result) ", "
                                        "body: " (:body result)))))))

(defrecord ValintatapajonoCacheLoader [valintaperusteet-cas-client]
  cache/CacheLoader

  (load [_ oid]
    (get-valintatapajono valintaperusteet-cas-client oid))

  (load-many [this oids]
    (into {} (keep #(when-let [v (cache/load this %)] [% v]) oids)))

  (load-many-size [_]
    1)

  (check-schema [_ response]
    (valintatapajono-checker response)))
