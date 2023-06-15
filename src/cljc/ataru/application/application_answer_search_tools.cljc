(ns ataru.application.application-answer-search-tools)

(defn get-matching-per-hakukohde-question
  [flattened-form-fields answer]
  (->> flattened-form-fields
      (filter #(or (= (:id %) (:original-question answer))
                   (= (:id %) (:original-followup answer))))
      first))
(defn filter-required-per-hakukohde-answers
  [flattened-form-fields per-hakukohde-answers]
  (filter #(some (fn [validator] (= validator "required"))
                 (:validators (get-matching-per-hakukohde-question flattened-form-fields %)))
          per-hakukohde-answers))

(defn get-matching-parent-field
  [flattened-form-fields followup-field]
  (first (filter #(= (:id %) (:followup-of followup-field)) flattened-form-fields)))

(defn get-matching-per-hakukohde-parent-answer
  [application parent-field followup followup-field]
  (first (filter #(and
                     (= (:original-question %) (:id parent-field))
                     (= (:duplikoitu-kysymys-hakukohde-oid %) (:duplikoitu-followup-hakukohde-oid followup))
                     (= (:value %) (:option-value followup-field)))
                  (:answers application))))
