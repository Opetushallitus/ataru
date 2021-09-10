(ns ataru.hakija.demo
  (:require [clojure.zip :as z]))

(defn- is-ssn-question?
  [question]
  (= "ssn" (:id question)))

(defn- remove-validators
  [question]
  (dissoc question :validators))

(defn- questions->zipper
  [questions]
  (let [root-node {:children questions}]
    (z/zipper map? :children #(assoc %1 :children %2) root-node)))

(defn- zipper->questions
  [zipper]
  (:children (z/root zipper)))

(defn- conditional-edit
  [pred f zipper]
  (let [node (z/node zipper)]
    (if (pred node)
      (z/next (z/edit zipper f))
      (z/next zipper))))

(defn- edit-question
  [zipper]
  (->>
    zipper
    (conditional-edit is-ssn-question? remove-validators)))

(defn- edit-questions
  [zipper]
  (loop [current zipper]
    (if (z/end? current)
      current
      (recur (edit-question current)))))

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
