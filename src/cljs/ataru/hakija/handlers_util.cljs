(ns ataru.hakija.handlers-util
  (:require [ataru.application-common.application-field-common :refer [required-validators pad]]
            [ataru.util :as util]
            [ataru.application-common.hakukohde-specific-questions :as hsq]
            [ataru.application-common.comparators :as comparators]
            [ataru.hakija.person-info-fields :as person-info-fields]))

(defn- is-hakukohde-in-hakukohderyhma-of-question
  [tarjonta-hakukohteet hakukohde-oid question]
  (let [is-ryhma-in-hakukohderyhmat (fn [hakukohderyhma] (some #(= hakukohderyhma %) (:belongs-to-hakukohderyhma question)))
        selected-hakukohde          (some #(when (= (:oid %) hakukohde-oid) %) tarjonta-hakukohteet)]
    (some is-ryhma-in-hakukohderyhmat (:hakukohderyhmat selected-hakukohde))))

(defn- create-duplicate-question
  ([hakukohde-oid question]
   (create-duplicate-question hakukohde-oid question false))
  ([hakukohde-oid question called-during-form-load?]
  (-> question
      (hsq/change-followups-for-question hakukohde-oid called-during-form-load?)
      (dissoc :per-hakukohde)
      (assoc :id (str (:id question) "_" hakukohde-oid)
             :duplikoitu-kysymys-hakukohde-oid hakukohde-oid
             :original-question (:id question)
             :created-during-form-load called-during-form-load?))))

(defn- duplicate-question
  [tarjonta-hakukohteet hakukohde-oid questions question]
  (if (and (:per-hakukohde question) (is-hakukohde-in-hakukohderyhma-of-question tarjonta-hakukohteet hakukohde-oid question))
    (conj questions question (create-duplicate-question hakukohde-oid question))
    (conj questions question)))

(defn duplicate-questions-for-hakukohde
  [tarjonta-hakukohteet hakukohde-oid questions question]
  (if-let [children (seq (:children question))]
    (let [copied-children (reduce (partial duplicate-question tarjonta-hakukohteet hakukohde-oid) [] children)
          updated-question (assoc question :children copied-children)]
      (conj questions updated-question))
    (duplicate-question tarjonta-hakukohteet hakukohde-oid questions question)))

(defn- duplicate-questions-for-hakukohde-inner-during-form-load
  [tarjonta-hakukohteet hakukohde-oids questions question]
  (if (and (:per-hakukohde question) (some #(is-hakukohde-in-hakukohderyhma-of-question tarjonta-hakukohteet % question) hakukohde-oids))
    (let [called-during-form-load? true
          valid-hakukohde-oids (filter #(is-hakukohde-in-hakukohderyhma-of-question tarjonta-hakukohteet % question) hakukohde-oids)
          questions-to-add (map #(create-duplicate-question % question called-during-form-load?) valid-hakukohde-oids)]
        (concat questions [question] questions-to-add))
    (concat questions [question])))

(defn duplicate-questions-for-hakukohteet-during-form-load
  [tarjonta-hakukohteet hakukohde-oids questions]
  (let [duplicate-questions-fn (partial reduce (partial duplicate-questions-for-hakukohde-inner-during-form-load tarjonta-hakukohteet hakukohde-oids) [])
        duplicated-questions (duplicate-questions-fn questions)
        duplicate-children (fn [question]
                             (if (:children question)
                               (assoc question :children (duplicate-questions-fn (:children question)))
                               question))]
    (map duplicate-children duplicated-questions)))


(defn- is-duplicated-required
  [question]
  (some #(contains? required-validators %)
        (:validators question)))

(defn- original-answer-id
  [question]
  (keyword (or (:original-question question) (:original-followup question))))

(defn- duplicated-question?
  [question]
  (boolean (original-answer-id question)))

(defn- no-answer?
  [answers question]
  (not (get answers (keyword (:id question)))))

(defn- duplicated-questions-without-answer
  [questions answers]
  (filter #(and (duplicated-question? %) (no-answer? answers %)) questions))

(defn- add-missing-answer
  [answers question]
  (let [answer-id (keyword (:id question))
        answer    (assoc
                    (get answers (original-answer-id question))
                    :valid
                    (not (is-duplicated-required question)))]
    (assoc answers answer-id answer)))

(defn fill-missing-answer-for-hakukohde
  [answers questions]
    (let [flat-questions (util/flatten-form-fields questions)
          missing-questions (duplicated-questions-without-answer flat-questions answers)]
      (if (seq missing-questions)
        (reduce add-missing-answer answers missing-questions)
        answers)))

(defn- apply-to-children
  [f question]
  (if (:children question)
    (update question :children f)
    question))

(defn- apply-to-questions-and-first-level-children
  [f questions]
  (f (map (partial apply-to-children f) questions)))

(defn sort-questions-and-first-level-children
  [selected-hakukohteet questions]
  (apply-to-questions-and-first-level-children
    #(sort (comparators/duplikoitu-kysymys-hakukohde-comparator selected-hakukohteet) %)
    questions))

(def keywordized-id  (comp keyword :id))
(def keywordized-key (comp keyword :key))

; Pads an (empty) answer to the maximum question group dimension.
; Returns nil when there are no answers with values to match to.
(defn- pad-to-matching-length-if-necessary
  [question-group-field-group-ids question-group-max-dimensions answer]
  (let [dimension (->> (get question-group-field-group-ids (:key answer))
                       (get question-group-max-dimensions))]
    (when dimension
      (-> answer
          (assoc :value  (pad dimension (:value answer)  nil))
          (assoc :values (pad dimension (:values answer) nil))))))

; Answers returned from the backend don't explicitly include empty ones, but they are reconstructed in db.
; Makes sure all empty answers that belong to question group have the same cardinality as non-empty ones
; inside the question group in db. Pads with nils in case of mismatches.
; Skips standard person info fields and other than actually fillable form fields.
(defn reinitialize-question-group-empty-answers [db answers flat-form-content]
  (if (empty? answers)
    db
    (let [question-group-field-group-ids        (->> flat-form-content
                                                     (filter #(and (some? (get-in % [:params :question-group-id]))
                                                                   (= :formField (keyword (:fieldClass %)))
                                                                   (not (contains? (set person-info-fields/person-info-field-ids) (keywordized-id %)))))
                                                     (map #(vector (keywordized-id %)
                                                                   (keyword (get-in % [:params :question-group-id]))))
                                                     (into {}))
          question-group-answer-value-counts    (->> answers
                                                     (filter #(contains? question-group-field-group-ids (keywordized-key %)))
                                                     (map #(vector (keywordized-key %) (count (:value %))))
                                                     (into {}))
          question-group-max-dimensions         (->> question-group-answer-value-counts
                                                     (map #(vector (get question-group-field-group-ids (first %))
                                                                   (last %)))
                                                     (filter #(> (last %) 1)) ; We only need to consider padding when group dimension >1
                                                     distinct ; Assume all answers with values inside question group have matching dimensions
                                                     (into {}))
          question-group-answers-without-values (->> (keys question-group-field-group-ids)
                                                     (remove #(contains? question-group-answer-value-counts %))
                                                     (map #(assoc (get-in db [:application :answers %]) :key %)))
          answers-to-update                     (->> question-group-answers-without-values
                                                     (map (partial pad-to-matching-length-if-necessary
                                                                   question-group-field-group-ids question-group-max-dimensions))
                                                     (remove nil?))]
      (-> (reduce (fn [db answer]
                    (assoc-in db 
                              [:application :answers (:key answer)]
                              (dissoc answer :key)))
                  db
                  answers-to-update)))))
