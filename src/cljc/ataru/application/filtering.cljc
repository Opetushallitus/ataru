(ns ataru.application.filtering
  (:require [ataru.application.review-states :as review-states]
            [clojure.set :as set]))

(defn filter-by-hakukohde-review
  [application selected-hakukohteet requirement-name states-to-include]
  (let [all-states-count (-> review-states/hakukohde-review-types-map
                             (get (keyword requirement-name))
                             (last)
                             (count))
        selected-count   (count states-to-include)]
    (if (= all-states-count selected-count)
      true
      (let [relevant-states (->> (:application-hakukohde-reviews application)
                                 (filter #(and (= requirement-name (:requirement %))
                                               (or (not selected-hakukohteet) (contains? selected-hakukohteet (:hakukohde %)))))
                                 (map :state)
                                 (set))]
        (not (empty? (set/intersection states-to-include relevant-states)))))))

(defn filter-by-kevyt-valinta-selection-state
  [db
   application-key
   hakukohde-oids]
  (let [all-states-count  (-> review-states/kevyt-valinta-valinnan-tila-selection-states
                              vals
                              count)
        states-to-include (-> db :application :kevyt-valinta-selection-state-filter set)
        selected-count    (count states-to-include)]
    (if (= all-states-count selected-count)
      true
      (let [valinnan-tulokset (-> db :valinta-tulos-service (get application-key))
            relevant-states   (->>
                                hakukohde-oids
                                (map (fn [hakukohde-oid]
                                       (or (-> valinnan-tulokset
                                               (get hakukohde-oid)
                                               :valinnantulos
                                               :valinnantila)
                                           "KESKEN")))
                                set)]
        (not (empty? (set/intersection states-to-include relevant-states)))))))

(defn add-review-state-counts
  [counts applications selected-hakukohde-oids review-type]
  (reduce (fn [counts application]
            (->> (:application-hakukohde-reviews application)
                 (filter #(and (or (empty? selected-hakukohde-oids)
                                   (contains? selected-hakukohde-oids (:hakukohde %)))
                               (= review-type (:requirement %))))
                 (map :state)
                 distinct
                 (reduce #(update %1 %2 inc) counts)))
          counts
          applications))

(defn add-kevyt-valinta-selection-state-counts
  [counts db applications selected-hakukohde-oids]
  (reduce (fn [counts
               {application-key :key
                hakukohde-oids  :hakukohde}]
            (->> hakukohde-oids
                 (filter (fn [hakukohde-oid]
                           (contains? selected-hakukohde-oids hakukohde-oid)))
                 (map (fn [hakukohde-oid]
                        (let [valinnan-tila (or (-> db
                                                    :valinta-tulos-service
                                                    (get application-key)
                                                    (get hakukohde-oid)
                                                    :valinnantulos
                                                    :valinnantila)
                                                "KESKEN")]
                          valinnan-tila)))
                 (reduce (fn [acc valinnan-tila]
                           (update acc
                                   valinnan-tila
                                   (fnil inc 0)))
                         counts)))
          counts
          applications))
