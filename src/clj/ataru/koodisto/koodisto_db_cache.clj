(ns ataru.koodisto.koodisto-db-cache
  (:require [ataru.config.url-helper :refer [resolve-url]]
            [ataru.koodisto.koodisto-codes :refer [institution-type-codes]]
            [ataru.util.http-util :as http-util]
            [cheshire.core :as cheshire]
            [clojure.string :as str]))

(defn- do-get [url]
  (let [{:keys [status headers body error] :as resp} (http-util/do-get url)]
    (if (= 200 status)
      (cheshire/parse-string body true)
      (throw (ex-info "Error when fetching doing HTTP GET" {:status  status
                                                            :url     url
                                                            :body    body
                                                            :error   error
                                                            :headers headers})))))

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
       (filter #(str/starts-with? (:uri %) "koulutus_"))
       (group-by :value)
       (map (fn [[_ versions]] (apply max-key :version versions)))))

(defn- get-vocational-institutions-by-type [type version]
  (->> {:uri     (str "oppilaitostyyppi_" type)
        :version version}
       add-within
       :within
       (filter #(str/starts-with? (:uri %) "oppilaitosnumero_"))
       (group-by :value)
       (map (fn [[_ versions]] (apply max-key :version versions)))))

(defn- get-vocational-institutions [version]
  (mapcat #(get-vocational-institutions-by-type % version)
          institution-type-codes))

(defn get-koodi-options [koodisto-uri]
  (let [[uri version] (str/split koodisto-uri #"#")]
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
