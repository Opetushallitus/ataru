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
        review-targets          (if has-hakukohteet?
                                  application-hakukohteet
                                  #{"form"})
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
                                       :hakukohde   oid
                                       :state       default-state-name})
                            unreviewed-targets))))

(defn get-all-reviews-for-all-requirements
  [application selected-hakukohde-oid]
  (mapcat
    #(get-all-reviews-for-requirement % application selected-hakukohde-oid)
    review-states/hakukohde-review-type-names))

(defn generate-labels-for-hakukohde-selection-reviews
  [review-requirement-name states application selected-hakukohde-oid]
  (map (fn [[state reviews]]
         [(get-review-state-label-by-name states state)
          (count reviews)])
       (group-by :state
                 (get-all-reviews-for-requirement
                   review-requirement-name
                   application
                   selected-hakukohde-oid))))

