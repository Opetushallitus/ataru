(ns ataru.hakija.handlers-util
  (:require [ataru.application-common.application-field-common :refer [required-validators]]
            [ataru.util :as util]))

(defn- is-hakukohde-in-hakukohderyhma-of-question
       [tarjonta-hakukohteet hakukohde-oid question]
       (let [is-ryhma-in-hakukohderyhmat (fn [hakukohderyhma] (some #(= hakukohderyhma %) (:belongs-to-hakukohderyhma question)))
             selected-hakukohde (some #(when (= (:oid %) hakukohde-oid) %) tarjonta-hakukohteet)]
         (some is-ryhma-in-hakukohderyhmat (:hakukohderyhmat selected-hakukohde))))

(declare change-followups-for-question)

(defn- change-followup-id
  [followup hakukohde-oid]
  (-> followup
    (assoc :id (str (:id followup) "_" hakukohde-oid)
           :duplikoitu-followup-hakukohde-oid hakukohde-oid
           :original-followup (:id followup))
    (change-followups-for-question hakukohde-oid)))

(defn- change-followups-for-option
  [option hakukohde-oid]
  (if-let [followups (seq (:followups option))]
    (assoc option :followups (map #(change-followup-id % hakukohde-oid) followups))
    option))

(defn- change-followups-for-question
  [question hakukohde-oid]
  (if-let [options (seq (:options question))]
    (assoc question :options (map #(change-followups-for-option % hakukohde-oid) options))
    question))

(defn- create-duplicate-question
  [hakukohde-oid question]
  (-> question
      (change-followups-for-question hakukohde-oid)
      (dissoc :per-hakukohde)
      (assoc :id (str (:id question) "_" hakukohde-oid)
             :duplikoitu-kysymys-hakukohde-oid hakukohde-oid
             :original-question (:id question))))

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

(defn- duplicate-questions-for-hakukohde-inner
  [tarjonta-hakukohteet hakukohde-oids questions question]
  (if (and (:per-hakukohde question) (some #(is-hakukohde-in-hakukohderyhma-of-question tarjonta-hakukohteet % question) hakukohde-oids))
    (let [valid-hakukohde-oids (filter #(is-hakukohde-in-hakukohderyhma-of-question tarjonta-hakukohteet % question) hakukohde-oids)
          questions-to-add (map #(create-duplicate-question % question) valid-hakukohde-oids)]
        (concat questions [question] questions-to-add))
    (concat questions [question])))

(defn duplicate-questions-for-hakukohteet
  [tarjonta-hakukohteet hakukohde-oids questions]
  (let [duplicate-questions-fn (partial reduce (partial duplicate-questions-for-hakukohde-inner tarjonta-hakukohteet hakukohde-oids) [])
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

