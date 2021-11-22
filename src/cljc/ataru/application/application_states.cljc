(ns ataru.application.application-states
  (:require [ataru.application.review-states :as review-states]
            [clojure.set :as set]))

(defn get-review-state-label-by-name
  [states name lang]
  (->> states
       (filter #(= (first %) name))
       first
       second
       lang))

(defn get-all-reviews-for-requirement
  "Adds default (incomplete) reviews where none have yet been created"
  [review-requirement-name application selected-hakukohde-oids]
  (let [application-hakukohteet (set (:hakukohde application))
        hakukohde-filter        (set selected-hakukohde-oids)
        has-hakukohteet?        (not (empty? application-hakukohteet))
        review-targets          (cond
                                  (not-empty selected-hakukohde-oids) hakukohde-filter
                                  has-hakukohteet? application-hakukohteet
                                  :else #{"form"})
        relevant-states         (filter #(and
                                           (= (:requirement %) review-requirement-name)
                                           (= has-hakukohteet? (not= (:hakukohde %) "form"))
                                           (or (empty? hakukohde-filter)
                                               (contains? hakukohde-filter (:hakukohde %))))
                                        (:application-hakukohde-reviews application))
        unreviewed-targets      (set/difference review-targets (set (map :hakukohde relevant-states)))
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
  ([application selected-hakukohde-oids]
   (mapcat
     #(get-all-reviews-for-requirement % application selected-hakukohde-oids)
     review-states/hakukohde-review-type-names))
  ([application]
    (get-all-reviews-for-all-requirements application nil)))

(defn attachment-reviews-with-no-requirements [application]
  (let [reviews (:application-attachment-reviews application)
        no-reqs (set/difference (set (:hakukohde application))
                  (set (map :hakukohde reviews)))]
    (concat reviews (map (fn [oid] {:hakukohde oid :state review-states/no-attachment-requirements}) no-reqs))))
