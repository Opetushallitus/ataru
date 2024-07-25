(ns ataru.hakija.application-hakukohde-util
  (:require
   [ataru.util :as util]
   [clojure.string :as string]))

(defn query-hakukohteet [hakukohde-query lang virkailija? tarjonta-hakukohteet hakukohteet-field]
  (let [non-archived-hakukohteet (filter #(not (:archived %)) tarjonta-hakukohteet)
        non-archived-hakukohteet-oids (set (map :oid non-archived-hakukohteet))
        order-by-hakuaika (if virkailija?
                            #{}
                            (->> non-archived-hakukohteet
                                 (remove #(:on (:hakuaika %)))
                                 (map :oid)
                                 set))
        order-by-name #(util/non-blank-val (:label %) [lang :fi :sv :en])
        hakukohde-options (->> hakukohteet-field
                               :options
                               (filter #(contains? non-archived-hakukohteet-oids (:value %)))
                               (sort-by (juxt (comp order-by-hakuaika :value)
                                              order-by-name)))
        query-parts (map string/lower-case (string/split hakukohde-query #"\s+"))
        results (if (or (string/blank? hakukohde-query)
                        (< (count hakukohde-query) 2))
                  (map :value hakukohde-options)
                  (->> hakukohde-options
                       (filter
                        (fn [option]
                          (let [haystack (string/lower-case
                                          (str (get-in option [:label lang] (get-in option [:label :fi] ""))
                                               (get-in option [:description lang] "")))]
                            (every? #(string/includes? haystack %) query-parts))))
                       (map :value)))
        [hakukohde-hits rest-results] (split-at 15 results)]
    {:hakukohde-hits  hakukohde-hits
     :rest-results    rest-results
     :hakukohde-query hakukohde-query}))