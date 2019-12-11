(ns ataru.tarjonta-service.kouta.kouta-client
  (:require [ataru.cache.cache-service :as cache-service]
            [ataru.cas.client :as cas-client]
            [ataru.config.url-helper :as url-helper]
            [ataru.schema.form-schema :as form-schema]
            [cheshire.core :as json]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [schema.core :as s]
            [taoensso.timbre :as log]))

(def haku-checker (s/checker form-schema/Haku))
(def hakus-by-form-key-checker (s/checker [s/Str]))

(defn- parse-date-time
  [s]
  (let [tz  (t/time-zone-for-id "Europe/Helsinki")
        fmt (f/formatter "yyyy-MM-dd'T'HH:mm" tz)]
    (t/to-time-zone (f/parse fmt s) tz)))

(defn- parse-haku
  [haku]
  (let [hakuajat (mapv (fn [hakuaika]
                         {:hakuaika-id "kouta-hakuaika-id"
                          :start       (parse-date-time (:alkaa hakuaika))
                          :end         (parse-date-time (:paattyy hakuaika))})
                       (:hakuajat haku))]
    {:can-submit-multiple-applications           true
     :hakuajat                                   hakuajat
     :hakukausi-vuosi                            (->> hakuajat
                                                      (map #(t/year (:start %)))
                                                      (apply max))
     :hakukohteet                                (:hakukohteet haku)
     :hakutapa-uri                               (:hakutapaKoodiUri haku)
     :kohdejoukko-uri                            (:kohdejoukkoKoodiUri haku)
     :name                                       (:nimi haku)
     :oid                                        (:oid haku)
     :prioritize-hakukohteet                     false
     :sijoittelu                                 false
     :yhteishaku                                 (clojure.string/starts-with?
                                                  (:hakutapaKoodiUri haku)
                                                  "hakutapa_01#")
     :ylioppilastutkinto-antaa-hakukelpoisuuden? false}))

(defn- get-result
  [url cas-client]
  (log/debug "get-result" url)
  (let [{:keys [status body]} (cas-client/cas-authenticated-get
                                cas-client
                                url)]
    (case status
      200 (json/parse-string body true)
      404 nil
      (throw (new RuntimeException (str "Could not get " url ", "
                                        "status: " status ", "
                                        "body: " body))))))

(s/defn ^:always-validate get-haku :- (s/maybe form-schema/Haku)
  [haku-oid :- s/Str
   cas-client]
  (some-> :kouta-internal.haku
          (url-helper/resolve-url haku-oid)
          (get-result cas-client)
          parse-haku))

(s/defn ^:always-validate get-hakus-by-form-key :- (s/maybe [s/Str])
  [cas-client
   form-key :- s/Str]
  (-> :kouta-internal.haku-search
      (url-helper/resolve-url {"ataruId" form-key})
      (get-result cas-client)
      ((fn [result] (mapv :oid result)))))

(defrecord CacheLoader [cas-client]
  cache-service/CacheLoader

  (load [_ haku-oid]
    (get-haku haku-oid cas-client))

  (load-many [this haku-oids]
    (cache-service/default-load-many this haku-oids))

  (load-many-size [_]
    1)

  (check-schema [_ response]
    (haku-checker response)))

(defrecord HakusByFormKeyCacheLoader [cas-client]
  cache-service/CacheLoader

  (load [_ form-key]
    (get-hakus-by-form-key cas-client form-key))

  (load-many [this form-keys]
    (cache-service/default-load-many this form-keys))

  (load-many-size [_]
    1)

  (check-schema [_ response]
    (hakus-by-form-key-checker response)))
