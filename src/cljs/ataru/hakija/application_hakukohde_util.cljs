(ns ataru.hakija.application-hakukohde-util
  (:require
   [ataru.util :as util]
   [clojure.string :as string]))

(defn options-comparator [a b]
  (let [a-user-trans (first a)
        b-user-trans (first b)]
    (cond (= a-user-trans b-user-trans) (compare a b)
          (empty? a-user-trans) 1
          (empty? b-user-trans) -1
          :else (compare a b))))

; TODO: Mistä saan tänne jokaisen hakukohteen opetuskielen?
(defn query-hakukohteet [hakukohde-query lang virkailija? tarjonta-hakukohteet hakukohteet-field order-hakukohteet-by-opetuskieli?]
  (let [non-archived-hakukohteet (filter #(not (:archived %)) tarjonta-hakukohteet)
        non-archived-hakukohteet-oids (set (map :oid non-archived-hakukohteet))
        order-by-hakuaika (if virkailija?
                            #{}
                            (->> non-archived-hakukohteet
                                 (remove #(:on (:hakuaika %)))
                                 (map :oid)
                                 set))
        order-user-lang-first (fn [item] (get-in item [:label lang]))
        order-by-name #(util/non-blank-val (:label %) [lang :fi :sv :en])
        hakukohde-options (->> hakukohteet-field
                               :options
                               (filter #(contains? non-archived-hakukohteet-oids (:value %)))
                               (sort-by (apply juxt (concat (if order-hakukohteet-by-opetuskieli? [order-user-lang-first] [])
                                                            [order-by-name
                                                             (comp order-by-hakuaika :value)]))
                                        (if order-hakukohteet-by-opetuskieli? options-comparator nil)))
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