(ns ataru.koodisto.koodisto-db-cache
  (:require [ataru.config.url-helper :refer [resolve-url]]
            [ataru.koodisto.koodisto-codes :refer [institution-type-codes]]
            [ataru.organization-service.organization-client :as organization-client]
            [ataru.schema.form-schema :as schema]
            [ataru.util.http-util :as http-util]
            [cheshire.core :as cheshire]
            [clojure.string :as str]
            [schema.core :as s]
            [ataru.util :as util])
  (:import [java.time LocalDate LocalTime ZonedDateTime ZoneId]
           java.time.format.DateTimeFormatter))

(def koodisto-checker (s/checker [schema/Koodi]))

(defn- do-get [url]
  (let [{:keys [status headers body error]} (http-util/do-get url)]
    (if (= 200 status)
      (cheshire/parse-string body true)
      (throw (ex-info "Error when fetching doing HTTP GET" {:status  status
                                                            :url     url
                                                            :body    body
                                                            :error   error
                                                            :headers headers})))))

(defn- extract-name-with-language [metadata language]
  (->> metadata
       (keep #(when (= language (:kieli %))
                (:nimi %)))
       first))

(defn- koodi-value->soresu-option [koodi-value]
  {:uri     (:koodiUri koodi-value)
   :version (:versio koodi-value)
   :value   (:koodiArvo koodi-value)
   :label   (into {}
                  (for [[ik ok] [["FI" :fi]
                                 ["SV" :sv]
                                 ["EN" :en]]
                        :let    [name (-> koodi-value :metadata (extract-name-with-language ik))]
                        :when   (not (str/blank? name))]
                    [ok name]))
   :valid   (merge {}
                   (when-let [start (:voimassaAlkuPvm koodi-value)]
                     {:start (ZonedDateTime/of (LocalDate/parse start (DateTimeFormatter/ofPattern "yyyy-MM-dd"))
                                               LocalTime/MIDNIGHT
                                               (ZoneId/of "Europe/Helsinki"))})
                   (when-let [end (:voimassaLoppuPvm koodi-value)]
                     {:end (ZonedDateTime/of (LocalDate/parse end (DateTimeFormatter/ofPattern "yyyy-MM-dd"))
                                             LocalTime/MIDNIGHT
                                             (ZoneId/of "Europe/Helsinki"))}))})

(defn- get-koodisto
  [koodisto version]
  (->> (resolve-url :koodisto-service.koodi koodisto
                    {"koodistoVersio" (str version)})
       do-get
       (mapv koodi-value->soresu-option)))

(defn- add-within
  [koodisto koodit]
  (let [get-koodi     (memoize (fn [koodi versio]
                                 (-> (resolve-url :koodisto-service.koodi-detail
                                                  koodi
                                                  versio)
                                     do-get
                                     koodi-value->soresu-option)))]
    (mapv (fn [koodi]
            (->> (resolve-url :koodisto-service.koodi-detail
                              (:uri koodi)
                              (:version koodi))
                 do-get
                 :withinCodeElements
                 (keep #(when (str/starts-with? (:codeElementUri %)
                                                (str koodisto "_"))
                          (get-koodi (:codeElementUri %)
                                     (:codeElementVersion %))))
                 (assoc koodi :within)))
          koodit)))

(defn- get-vocational-degree-options [version]
  (->> [{:uri     "ammatillisetopsperustaiset_1"
         :version version}]
       (add-within "koulutus")
       (mapcat :within)))

(defn- get-vocational-institutions [version]
  (->> institution-type-codes
       (map (fn [type]
              {:uri     (str "oppilaitostyyppi_" type)
               :version version}))
       (add-within "oppilaitosnumero")
       (mapcat :within)
       (util/distinct-by :uri)
       (map #(assoc % :label (or (-> (:value %) (organization-client/get-single-organization-cached) :nimi)
                                 (:label %))))))

(s/defn ^:always-validate get-koodi-options :- [schema/Koodi]
  [koodisto-uri :- s/Str]
  (let [[uri version] (str/split koodisto-uri #"#")]
    (condp = uri

      "valtioryhmat" (add-within "maatjavaltiot2"
                                 (get-koodisto uri version))

      "AmmatillisetOPSperustaiset" (get-vocational-degree-options version)

      "oppilaitostyyppi" (get-vocational-institutions version)

      "pohjakoulutusvaatimuskorkeakoulut" (add-within "pohjakoulutuskklomake"
                                                      (get-koodisto uri version))

      "pohjakoulutusvaatimuskouta" (add-within "pohjakoulutuskklomake"
                                               (get-koodisto uri version))

      (get-koodisto uri version))))
