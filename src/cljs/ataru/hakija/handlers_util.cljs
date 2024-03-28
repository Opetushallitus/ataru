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

; Returns the dimension (length of the answer set) for this answer as a part of its question group
(defn- get-question-group-answer-dimension
  [question-group-fields answer]
  (let [question-group-id (get-in question-group-fields 
                                  [(keywordized-key answer) :params :question-group-id])
        dimension         (count (:value answer))]
    [question-group-id dimension]))

; Pads an (empty) answer to the maximum question group dimension. Returns nil when no padding needed.
(defn- pad-to-matching-length-if-found
  [question-group-fields question-group-max-dimensions answer]
  (let [dimension (->> (get-in question-group-fields 
                               [(keywordized-id answer) :params :question-group-id])
                       keyword
                       (get question-group-max-dimensions))]
    (when dimension
      (-> answer
          (assoc :value (pad dimension (:value answer) nil))
          (assoc :values (pad dimension (:values answer) nil))))))

; Answers that come from the server don't include empty ones, but they are reconstructed in db.
; Make sure all empty answers that belong to question group have the same cardinality as non-empty ones
; inside the question group in db. Pad with nils in case of mismatches.
(defn reinitialize-question-group-empty-answers [db answers flat-form-content]
  (if (empty? answers)
    db
    (let [question-group-fields                 (->> flat-form-content
                                                     (filter #(and (some? (get-in % [:params :question-group-id]))
                                                              (= :formField (keyword (:fieldClass %)))
                                                              (not (contains? (set person-info-fields/person-info-field-ids) (keywordized-id %)))))
                                                     (map #(vector (keywordized-id %) %))
                                                     (into {}))
          question-group-answers-with-values    (->> answers
                                                     (filter #(contains? question-group-fields (keywordized-key %)))
                                                     (map #(vector (keywordized-key %) %))
                                                     (into {}))
          question-group-answers-without-values (->> (vals question-group-fields)
                                                     (map #(assoc (get-in db [:application :answers (keywordized-id %)]) :id (:id %)))
                                                     (remove nil?)
                                                     (remove #(contains? question-group-answers-with-values (keywordized-id %))))
          question-group-max-dimensions         (->> (vals question-group-answers-with-values)
                                                     (map (partial get-question-group-answer-dimension question-group-fields))
                                                     (filter #(> (last %) 1))
                                                     distinct
                                                     (into {}))
          updated-answers                       (->> question-group-answers-without-values
                                                     (map (partial pad-to-matching-length-if-found question-group-fields question-group-max-dimensions))
                                                     (remove nil?))]
      (-> (reduce (fn [db answer]
                    (assoc-in db 
                              [:application :answers (keywordized-id answer)] 
                              (dissoc answer :id)))
                  db
                  updated-answers)))))
