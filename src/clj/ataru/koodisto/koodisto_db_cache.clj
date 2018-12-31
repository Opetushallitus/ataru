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
  (->> {:uri     "ammatillisetopsperustaiset_1"
        :version version}
       add-within
       :within
       (filter #(clojure.string/starts-with? (:uri %) "koulutus_"))
       (group-by :value)
       (map (fn [[_ versions]] (apply max-key :version versions)))))

(defn- get-vocational-institutions-by-type [type version]
  (->> {:uri     (str "oppilaitostyyppi_" type)
        :version version}
       add-within
       :within
       (filter #(and (= version (:version %))
                     (clojure.string/starts-with? (:uri %) "oppilaitosnumero_")))))

(defn- get-vocational-institutions [version]
  (mapcat #(get-vocational-institutions-by-type % version)
          institution-type-codes))

(defn get-koodi-options [koodisto-uri]
  (let [[uri version] (clojure.string/split koodisto-uri #"#")]
    (condp = uri

           "AmmatillisetOPSperustaiset" (get-vocational-degree-options version)

           "oppilaitostyyppi" (get-vocational-institutions version)

           "pohjakoulutusvaatimuskorkeakoulut" (->> (resolve-url :koodisto-service.koodi uri
                                                      {"koodistoVersio"  (str version)
                                                       "onlyValidKoodis" "true"})
                                                    do-get
                                                    (mapv koodi-value->soresu-option)
                                                    (mapv add-within))

           (let [url (resolve-url :koodisto-service.koodi uri
                       {"koodistoVersio"  (str version)
                        "onlyValidKoodis" "true"})]
             (->> (do-get url)
                  (mapv koodi-value->soresu-option)
                  (sort-by (comp :fi :label) compare-case-insensitively))))))

(defn list-koodistos []
  (->> (fetch-all-koodisto-groups)
       (koodisto-groups->uris-and-latest)
       (mapv koodisto-version->uri-and-name)
       (sort compare-case-insensitively)))
