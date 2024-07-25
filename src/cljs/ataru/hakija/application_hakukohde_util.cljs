(ns ataru.hakija.application-hakukohde-util
  (:require
   [ataru.util :as util]
   [clojure.string :as string]))

(defn opetuskielet-to-lang-codes [opetuskielet]
  (set (mapcat #(case %
                  "oppilaitoksenopetuskieli_1" [:fi]
                  "oppilaitoksenopetuskieli_2" [:sv]
                  "oppilaitoksenopetuskieli_3" [:fi :sv] ;suomi/ruotsi
                  "oppilaitoksenopetuskieli_4" [:en]
                  []) opetuskielet)))

(defn query-hakukohteet [hakukohde-query lang virkailija? tarjonta-hakukohteet hakukohteet-field order-hakukohteet-by-opetuskieli?]
  (let [non-archived-hakukohteet (filter #(not (:archived %)) tarjonta-hakukohteet)
        non-archived-hakukohteet-by-oids (zipmap (map :oid non-archived-hakukohteet) non-archived-hakukohteet)
        order-by-hakuaika (if virkailija?
                            #{}
                            (->> non-archived-hakukohteet
                                 (remove #(:on (:hakuaika %)))
                                 (map :oid)
                                 set))
        order-by-opetuskieli (fn [item]
                               (let [opetuskielet (some-> (get non-archived-hakukohteet-by-oids (:value item))
                                                          :opetuskieli-koodi-urit
                                                          opetuskielet-to-lang-codes)]
                                 ; 
                                 (not (contains? opetuskielet lang))))
        order-by-name #(util/non-blank-val (:label %) [lang :fi :sv :en])
        hakukohde-options (->> hakukohteet-field
                               :options
                               (filter #(contains? non-archived-hakukohteet-by-oids (:value %)))
                               (sort-by (apply juxt (concat (if order-hakukohteet-by-opetuskieli? [order-by-opetuskieli] [])
                                                            [order-by-name
                                                             (comp order-by-hakuaika :value)]))))
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