(ns ataru.koodisto.koodisto-db-cache
  (:require [ataru.config.url-helper :refer [resolve-url]]
            [ataru.db.db :as db]
            [ataru.koodisto.koodisto-codes :refer [institution-type-codes]]
            [ataru.organization-service.organization-client :as organization-client]
            [ataru.util.http-util :as http-util]
            [cheshire.core :as cheshire]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [pandect.algo.sha256 :refer :all]
            [yesql.core :refer [defqueries]]))

; TODO url.config
(def koodisto-base-url "https://virkailija.opintopolku.fi:443/koodisto-service/rest/")
(def all-koodisto-groups-path "codes")
(def all-koodistos-group-uri "http://kaikkikoodistot")

(defqueries "sql/koodisto.sql")

(defn json->map [body] (cheshire/parse-string body true))

(defn- do-get [url]
  (let [{:keys [status headers body error] :as resp} (http-util/do-get url)]
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
  {:uri     (:koodiUri koodi-value)
   :version (:versio koodi-value)
   :value   (:koodiArvo koodi-value)
   :label   {:fi (->> koodi-value :metadata (extract-name-with-language "FI"))
             :sv (->> koodi-value :metadata (extract-name-with-language "SV"))
             :en (->> koodi-value :metadata (extract-name-with-language "EN"))}})

(defn- code-element->soresu-option [element]
  {:uri     (:codeElementUri element)
   :version (:codeElementVersion element)
   :value   (:codeElementValue element)
   :label   {:fi (->> element :relationMetadata (extract-name-with-language "FI"))
             :sv (->> element :relationMetadata (extract-name-with-language "SV"))
             :en (->> element :relationMetadata (extract-name-with-language "EN"))}})

(defn list-koodistos []
  (->> (fetch-all-koodisto-groups)
       (koodisto-groups->uris-and-latest)
       (mapv koodisto-version->uri-and-name)
       (sort compare-case-insensitively)))

(defn- add-within
  [koodi-option]
  (->> (do-get (resolve-url :koodisto-service.koodi-detail
                            (:uri koodi-option)
                            (:version koodi-option)))
       :withinCodeElements
       (filter #(not (:passive %)))
       (map code-element->soresu-option)
       (assoc koodi-option :within)))

(defn- get-vocational-degree-options [version]
  (let [koodisto-uri (str koodisto-base-url "codeelement/ammatillisetopsperustaiset_1/" version)]
    (->> (do-get koodisto-uri)
         :withinCodeElements
         (filter #(-> % :passive not))
         (sort-by :codeElementVersion)
         (group-by :codeElementValue)
         (map (fn [[key values]]
                (-> values last code-element->soresu-option))))))

(defn- get-vocational-institutions-by-type [type version]
  (let [koodisto-uri (str koodisto-base-url "codeelement/oppilaitostyyppi_" type "/" version)]
    (->> (do-get koodisto-uri)
         :withinCodeElements
         (filter #(-> % :passive not))
         (map :codeElementValue))))

(defn- get-institution [number]
  (when-let [institution (organization-client/get-organization-by-oid-or-number number)]
    {:value number
     :label (:nimi institution)}))

(defn get-vocational-institutions [version]
  (->> institution-type-codes
       (pmap #(get-vocational-institutions-by-type % version))
       flatten
       set
       (pmap get-institution)
       (filter some?)))

(defn get-koodi-options [koodisto-uri version]
  (condp = koodisto-uri

    "AmmatillisetOPSperustaiset" (get-vocational-degree-options version)

    "oppilaitostyyppi" (get-vocational-institutions version)

    "pohjakoulutusvaatimuskorkeakoulut" (->> (resolve-url :koodisto-service.koodi koodisto-uri
                                                          {"koodistoVersio"  (str version)
                                                           "onlyValidKoodis" "true"})
                                             do-get
                                             (mapv koodi-value->soresu-option)
                                             (mapv add-within))

    (let [url (resolve-url :koodisto-service.koodi koodisto-uri
                           {"koodistoVersio"  (str version)
                            "onlyValidKoodis" "true"})]
      (->> (do-get url)
           (mapv koodi-value->soresu-option)
           (sort-by (comp :fi :label) compare-case-insensitively)))))

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
