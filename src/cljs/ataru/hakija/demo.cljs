(ns ataru.hakija.demo
  (:require [clojure.zip :as z]))

(defn- is-ssn-question?
  [question]
  (= "ssn" (:id question)))

(defn- is-email-question?
  [question]
  (= "email" (:id question)))

(defn- remove-validators
  [question]
  (dissoc question :validators))

(defn- remove-email-validator
  [question]
  (update question :validators (comp vec (partial filter #(not= "email" %)))))

(defn- questions->zipper
  [questions]
  (let [root-node {:children questions}]
    (z/zipper map? :children #(assoc %1 :children %2) root-node)))

(defn- zipper->questions
  [zipper]
  (:children (z/root zipper)))

(defn- conditional-edit
  [zipper pred f]
  (let [node (z/node zipper)]
    (if (pred node)
      (z/edit zipper f)
      zipper)))

(defn- edit-question
  [zipper]
  (->
    zipper
    (conditional-edit is-ssn-question? remove-validators)
    (conditional-edit is-email-question? remove-email-validator)))

(defn- edit-questions
  [zipper]
  (loop [current zipper]
    (if (z/end? current)
      current
      (recur (z/next (edit-question current))))))

(defn demo?
  ([db]
   (and (get db :demo-requested) (get-in db [:form :properties :demo-allowed])))
  ([db form]
   (and (get db :demo-requested) (get-in form [:properties :demo-allowed]))))

(defn apply-when-demo
  [db form f x]
  (if (demo? db form)
    (f x)
    x))

(defn remove-unwanted-validators
  [questions]
  (->
    questions
    questions->zipper
    edit-questions
    zipper->questions))
