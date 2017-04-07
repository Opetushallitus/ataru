(ns ataru.koodisto.koodisto-db-cache
  (:require [org.httpkit.client :as http]
            [cheshire.core :as cheshire]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [pandect.algo.sha256 :refer :all]
            [ataru.db.db :as db]
            [yesql.core :refer [defqueries]]))

; TODO url.config
(def koodisto-base-url "https://virkailija.opintopolku.fi:443/koodisto-service/rest/")
(def all-koodisto-groups-path "codes")
(def all-koodistos-group-uri "http://kaikkikoodistot")

(def koodisto-version-path "codeelement/codes/")

(defqueries "sql/koodisto.sql")

(defn json->map [body] (cheshire/parse-string body true))

(defn- do-get [url]
  (let [{:keys [status headers body error] :as resp} @(http/get url)]
    (if (= 200 status)
      (let [body (json->map body)]
        (log/info (str "Fetched koodisto from URL: " url))
        body)
      (throw (ex-info "Error when fetching doing HTTP GET" {:status  status
                                                            :url     url
                                                            :body    body
                                                            :error   error
                                                            :headers headers})))))

(defn- fetch-all-koodisto-groups []
  (do-get (str koodisto-base-url all-koodisto-groups-path)))

(defn- koodisto-groups->uris-and-latest [koodisto-groups]
  (->> koodisto-groups
       (filter #(= all-koodistos-group-uri (:koodistoRyhmaUri %)))
       (first)
       (:koodistos)
       (mapv #(select-keys % [:koodistoUri :latestKoodistoVersio]))))

(defn- nil-to-empty-string [x]
  (or x ""))

(defn- extract-name-with-language [language metadata]
  (->> metadata
       (filter #(= language (:kieli %)))
       (filter :nimi)
       (set)
       (mapv :nimi)
       (first)
       nil-to-empty-string))

(defn- extract-name [koodisto-version]
  (->> koodisto-version
       (:latestKoodistoVersio)
       (:metadata)
       (extract-name-with-language "FI")))

(defn- koodisto-version->uri-and-name [koodisto-version]
  {:uri     (:koodistoUri koodisto-version)
   :name    (extract-name koodisto-version)
   :version (-> koodisto-version :latestKoodistoVersio :versio)})

(defn- compare-case-insensitively [s1 s2]
  (compare (str/upper-case s1) (str/upper-case s2)))

(defn- koodi-value->soresu-option [koodi-value]
  {:value (:koodiArvo koodi-value)
   :label {:fi (->> koodi-value :metadata (extract-name-with-language "FI"))
           :sv (->> koodi-value :metadata (extract-name-with-language "SV"))
           :en (->> koodi-value :metadata (extract-name-with-language "EN"))}})


(defn list-koodistos []
                     (->> (fetch-all-koodisto-groups)
                          (koodisto-groups->uris-and-latest)
                          (mapv koodisto-version->uri-and-name)
                          (sort compare-case-insensitively)))

(defn get-koodi-options [koodisto-uri version]
                        (let [koodisto-version-url (str koodisto-base-url koodisto-version-path koodisto-uri "/" version)]
                          (->> (do-get koodisto-version-url)
                               (mapv koodi-value->soresu-option)
                               (sort-by (fn [x] (-> x :label :fi)) compare-case-insensitively))))

(defn- get-cached-koodisto [db-key koodisto-uri version checksum]
  (->> {:koodisto_uri koodisto-uri
        :version      version
        :checksum     checksum}
       (db/exec db-key yesql-get-koodisto)
       first))

(defn get-cached-koodi-options [db-key koodisto-uri version]
                               (if-let [cached-koodisto (get-cached-koodisto db-key koodisto-uri version nil)]
                                 cached-koodisto
                                 (let [koodisto (get-koodi-options koodisto-uri version)
                                       checksum (->> (cheshire/generate-string koodisto)
                                                     (sha256))]
                                   (db/exec db-key yesql-create-koodisto<! {:koodisto_uri koodisto-uri
                                                                            :version      version
                                                                            :checksum     checksum
                                                                            :content      [koodisto]})
                                   (get-cached-koodisto db-key koodisto-uri version checksum))))