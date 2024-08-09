(ns ataru.hakija.application-hakukohde-util
  (:require [ataru.util :as util]
            [clojure.string :as string]
            [medley.core :refer [index-by map-vals]]))

(defn opetuskielet-to-lang-codes [opetuskielet]
  (set (mapcat #(case %
                  "oppilaitoksenopetuskieli_1" [:fi]
                  "oppilaitoksenopetuskieli_2" [:sv]
                  "oppilaitoksenopetuskieli_3" [:fi :sv] ;suomi/ruotsi
                  "oppilaitoksenopetuskieli_4" [:en]
                  []) opetuskielet)))

(defn query-hakukohteet
  [hakukohde-query lang virkailija? order-hakukohteet-by-opetuskieli? tarjonta-hakukohteet hakukohteet-field]
  (let [non-archived-hakukohteet-by-oid (->> tarjonta-hakukohteet
                                             (remove :archived)
                                             (index-by :oid))
        hakuaika-kaynnissa-mask (map-vals #(:on (:hakuaika %)) non-archived-hakukohteet-by-oid)
        matching-opetuskieli-mask (map-vals #(-> (get non-archived-hakukohteet-by-oid (:oid %))
                                                 :opetuskieli-koodi-urit
                                                 opetuskielet-to-lang-codes
                                                 (contains? lang))
                                            non-archived-hakukohteet-by-oid)
        ; sort-by järjestää falset ennen true-arvoja, joten negatoidaan maskien arvot
        order-hakuaika-kaynnissa-first (comp not hakuaika-kaynnissa-mask :value)
        order-opetuskieli-matches-first (comp not matching-opetuskieli-mask :value)
        order-by-label #(util/non-blank-val (:label %) [lang :fi :sv :en])
        options-order (apply juxt (concat (when (not virkailija?) [order-hakuaika-kaynnissa-first])
                                          (when order-hakukohteet-by-opetuskieli? [order-opetuskieli-matches-first])
                                          [order-by-label]))
        hakukohde-options (->> hakukohteet-field
                               :options
                               (filter #(contains? non-archived-hakukohteet-by-oid (:value %)))
                               (sort-by options-order))
        query-parts (map string/lower-case (string/split hakukohde-query #"\s+"))
        results (if (or (string/blank? hakukohde-query)
                        (< (count hakukohde-query) 2))
                  (map :value hakukohde-options)
                  (->> hakukohde-options
                       (filter
                        (fn [option]
                          (let [haystack (string/lower-case
                                          (str (util/non-blank-val (:label option) [lang :fi :sv :en])
                                               (util/non-blank-val (:description option) [lang :fi :sv :en])))]
                            (every? #(string/includes? haystack %) query-parts))))
                       (map :value)))
        [hakukohde-hits rest-results] (split-at 15 results)]
    {:hakukohde-hits  hakukohde-hits
     :rest-results    rest-results
     :hakukohde-query hakukohde-query}))