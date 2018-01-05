(ns ataru.application.application-states
  (:require [ataru.application.review-states :as review-states]))

(defn get-review-state-label-by-name
  [states name]
  (->> states (filter #(= (first %) name)) first second))

(defn get-all-reviews-for-requirement
  "Adds default (incomplete) reviews where none have yet been created"
  [review-requirement-name application selected-hakukohde-oid]
  (let [application-hakukohteet (set (:hakukohde application))
        has-hakukohteet?        (not (empty? application-hakukohteet))
        review-targets          (cond
                                  (some? selected-hakukohde-oid) #{selected-hakukohde-oid}
                                  has-hakukohteet? application-hakukohteet
                                  :else #{"form"})
        relevant-states         (filter #(and
                                           (= (:requirement %) review-requirement-name)
                                           (= has-hakukohteet? (not= (:hakukohde %) "form"))
                                           (or (nil? selected-hakukohde-oid)
                                               (= (:hakukohde %) selected-hakukohde-oid)))
                                        (:application-hakukohde-reviews application))
        unreviewed-targets      (clojure.set/difference review-targets (set (map :hakukohde relevant-states)))
        default-state-name      (-> (filter #(= (keyword review-requirement-name) (first %))
                                            review-states/hakukohde-review-types)
                                    (first)
                                    (last)
                                    (ffirst)
                                    (name))]
    (into relevant-states (map
                            (fn [oid] {:requirement review-requirement-name
                                       :state       default-state-name
                                       :hakukohde   oid})
                            unreviewed-targets))))

(defn get-all-reviews-for-all-requirements
  [application selected-hakukohde-oid]
  (mapcat
    #(get-all-reviews-for-requirement % application selected-hakukohde-oid)
    review-states/hakukohde-review-type-names))
