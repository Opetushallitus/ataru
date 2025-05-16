(ns ataru.hakija.application-hakukohde-util
  (:require [ataru.util :as util]
            [clojure.string :as string]
            [clojure.set :as s]
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

(defn filter-take-hakukohteet-and-ryhmat [data]
  (filter (fn [v] (or
                   (contains? v :belongs-to-hakukohteet)
                   (contains? v :belongs-to-hakukohderyhma)
                   (some (fn [opt]
                           (or (contains? opt :belongs-to-hakukohteet)
                               (contains? opt :belongs-to-hakukohderyhma)))
                         (:options v))))
          data))

(defn collect-root-ids-related-to-removable-hakukohde
  [hakukohde-oid flat-content selected-hakukohteet]
  (let [removable-ryhmat (:removable-ryhmat selected-hakukohteet)]
    (->> flat-content
         (filter
          (fn [{:keys [belongs-to-hakukohteet belongs-to-hakukohderyhma options]}]
            (or
             (some #{hakukohde-oid} belongs-to-hakukohteet)
             (seq (s/intersection removable-ryhmat (set belongs-to-hakukohderyhma)))
             (some #(some #{hakukohde-oid} (:belongs-to-hakukohteet %)) options))))
         (map :id)
         set)))

(defn filter-by-children-id
  [questions id-set]
  (filter (fn [item]
            (or (contains? id-set (:id item))
                (some (fn [child]
                        (contains? id-set (:id child)))
                      (:children item))))
          questions))


(defn prepare-hakukohteet-data [hakukohde-oid hakukohteet selected-hakukohteet-oids]
  (let [removable-hakukohteet (->> hakukohteet
                                   (filter #(= (:oid %) hakukohde-oid))
                                   (map #(select-keys % [:hakukohderyhmat :oid :name])))
        selected-hakukohteet  (->> hakukohteet
                                   (filter #(some (fn [oid] (= (:oid %) oid)) selected-hakukohteet-oids))
                                   (map #(select-keys % [:hakukohderyhmat :oid :name])))
        selected-ryhmat       (->> selected-hakukohteet (mapcat :hakukohderyhmat)
                                   set)
        removable-ryhmat      (->> removable-hakukohteet
                                   (mapcat :hakukohderyhmat)
                                   (filter #(not (contains? selected-ryhmat %)))
                                   set)]
    {:selected-ryhmat selected-ryhmat
     :removable-hakukohteet removable-hakukohteet
     :removable-ryhmat removable-ryhmat}))

(defn removable-ids-from-answers
  [hakukohde-oid form-content-filtered hakukohteet-form-data root-ids]
  (->> (util/flatten-form-fields form-content-filtered)
       (filter #(contains? root-ids (:id %)))
       (mapcat
        (fn [child]
          (let [options (:options child)
                valid-option-ids
                (->> options
                     (filter
                      (fn [opt]
                        (let [belongs (set (:belongs-to-hakukohderyhma opt))
                              belongs-to-hakukohteet (:belongs-to-hakukohteet opt)]
                          (or
                           (= [hakukohde-oid] belongs-to-hakukohteet)
                           (and (not (empty? (s/intersection belongs (:removable-ryhmat hakukohteet-form-data))))
                                (empty? (s/intersection belongs (:selected-ryhmat hakukohteet-form-data))))))))
                     (map
                      (fn [opt]
                        (or (:id opt) (:id child)))))] ;fallback to parent :id
            (when (seq valid-option-ids)
              valid-option-ids))))
       (remove nil?)
       set))




(defn invalidate-answers [answers keys root-keys]
  (reduce
   (fn [acc k]
     (if-let [entry (get acc k)]
       (let [original-value (:value entry)
             cleared-value (cond
                             (sequential? original-value) []
                             (string? original-value) ""
                             :else nil)
             updated-entry (-> entry
                               (assoc :valid (not-any? #(= % k) root-keys))
                               (assoc :value cleared-value)
                               (update :values
                                       (fn [v]
                                         (cond
                                           (and (map? v) (contains? v :valid)) (assoc v :valid false)
                                           (sequential? v) []
                                           :else v))))]
         (assoc acc k updated-entry))
       acc))
   answers
   keys))