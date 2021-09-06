(ns ataru.hakija.handlers-util-test
  (:require [cljs.test :refer-macros [deftest is]]
            [ataru.hakija.handlers-util :as util]))

(def hakukohde-in-ryhma
  {:oid "hk1" :hakukohderyhmat ["a1"]})

(def hakukohde-in-another-ryhma
  {:oid "hk2" :hakukohderyhmat ["a2"]})

(def per-hakukohde-question
  {:id 1 :per-hakukohde true :belongs-to-hakukohderyhma ["a1"]})

(def question
  {:id 2 :per-hakukohde false :belongs-to-hakukohderyhma ["a1"]})

(defn first-duplicated-question
  [questions]
  (first (filter #(:original-question %) questions)))

(deftest duplicates-questions-for-hakukohde
  (let [duplicated-questions (util/duplicate-questions-for-hakukohde [hakukohde-in-ryhma] "hk1" [] per-hakukohde-question)
        duplicated-question (first-duplicated-question duplicated-questions)]
    (is (= 2 (count duplicated-questions)))
    (is (= 1 (:original-question duplicated-question)))
    (is (nil? (:per-hakukohde duplicated-question)))
    (is (= "hk1" (:duplikoitu-kysymys-hakukohde-oid duplicated-question)))
    (is (= "1_hk1" (:id duplicated-question)))))

(deftest does-not-duplicates-questions-which-do-not-have-hakukohde-in-ryhma
  (let [duplicated-questions (util/duplicate-questions-for-hakukohde [hakukohde-in-another-ryhma] "hk2" [] question)]
    (is (= 1 (count duplicated-questions)))))

(deftest does-not-duplicates-questions-which-do-not-have-per-hakukohde-attribute
  (let [duplicated-questions (util/duplicate-questions-for-hakukohde [hakukohde-in-ryhma] "hk1" [] question)]
    (is (= 1 (count duplicated-questions)))))

(deftest duplicates-child-questions
  (let [duplicated-questions (util/duplicate-questions-for-hakukohde
                               [hakukohde-in-ryhma] "hk1" [] {:children [per-hakukohde-question]})
        children (:children (first duplicated-questions))
        duplicated-question (first-duplicated-question children)]
    (is (= 1 (count duplicated-questions)))
    (is (= 2 (count children)))
    (is (= 1 (:original-question duplicated-question)))
    (is (nil? (:per-hakukohde duplicated-question)))
    (is (= "hk1" (:duplikoitu-kysymys-hakukohde-oid duplicated-question)))
    (is (= "1_hk1" (:id duplicated-question)))))

(deftest does-not-duplicates-child-without-per-hakukohde
  (let [duplicated-questions (util/duplicate-questions-for-hakukohde
                               [hakukohde-in-ryhma] "hk1" [] {:children [question]})
        children (:children (first duplicated-questions))]
    (is (= 1 (count duplicated-questions)))
    (is (= 1 (count children)))
    (is (nil? (:original-question (first children))))
    (is (nil? (:duplikoitu-kysymys-hakukohde-oid (first children))))))

(deftest does-not-duplicates-child-which-do-not-have-hakukohde-in-ryhma
  (let [duplicated-questions (util/duplicate-questions-for-hakukohde
                               [hakukohde-in-another-ryhma] "hk2" [] {:children [per-hakukohde-question]})
        children (:children (first duplicated-questions))]
    (is (= 1 (count duplicated-questions)))
    (is (= 1 (count children)))
    (is (nil? (:original-question (first children))))
    (is (nil? (:duplikoitu-kysymys-hakukohde-oid (first children))))))

(deftest correctly-duplicates-questions-with-combined-cases
  (let [duplicated-questions (util/duplicate-questions-for-hakukohteet
                               [hakukohde-in-ryhma hakukohde-in-another-ryhma] ["hk1" "hk2"]
                               [question per-hakukohde-question {:children [{:id 3} {:id 4 :per-hakukohde true :belongs-to-hakukohderyhma ["a2"]}]}])
        [_ _ duplicated-question {[_ original-child duplicated-child :as children] :children}] duplicated-questions]
    (is (= 4 (count duplicated-questions)))
    (is (= 3 (count children)))
    (is (= 1 (:original-question duplicated-question)))
    (is (nil? (:per-hakukohde duplicated-question)))
    (is (= "hk1" (:duplikoitu-kysymys-hakukohde-oid duplicated-question)))
    (is (= "1_hk1" (:id duplicated-question)))
    (is (= 4 (:original-question duplicated-child)))
    (is (nil? (:per-hakukohde duplicated-child)))
    (is (= "hk2" (:duplikoitu-kysymys-hakukohde-oid duplicated-child)))
    (is (= "4_hk2" (:id duplicated-child)))
    (is (:per-hakukohde original-child))
    (is (nil? (:original-question original-child)))
    (is (nil? (::duplikoitu-kysymys-hakukohde-oid original-child)))))

(deftest fill-missing-answer-for-hakukohde
  (let [answers {:q1 {:value ""}}
        questions [{:id "q1"} {:id "q2" :original-question "q1"}]
        result (util/fill-missing-answer-for-hakukohde answers questions)]
    (is (= {:q1 {:value ""} :q2 {:value "" :valid true}} result))))

(deftest copied-answer-should-have-valid-false-when-question-is-required
  (let [answers {:q1 {:value ""}}
        questions [{:id "q1"} {:id "q2" :original-question "q1" :validators ["required"]}]
        result (util/fill-missing-answer-for-hakukohde answers questions)]
    (is (= false (:valid (:q2 result))))))