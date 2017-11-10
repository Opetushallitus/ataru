(ns ataru.application.application-states)

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
        unreviewed-targets      (clojure.set/difference review-targets (set (map :hakukohde relevant-states)))]
    (into relevant-states (map
                            (fn [oid] {:requirement review-requirement-name
                                       :hakukohde   oid
                                       :state       "incomplete"})
                            unreviewed-targets))))

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

