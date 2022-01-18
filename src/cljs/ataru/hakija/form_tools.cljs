(ns ataru.hakija.form-tools
  (:require [clojure.zip :as z]))

(defn- questions->zipper
  [questions]
  (let [root-node {:children questions}]
    (z/zipper map? :children #(assoc %1 :children %2) root-node)))

(defn- zipper->questions
  [zipper]
  (:children (z/root zipper)))

(defn- conditional-replace
  [zipper pred updated-question]
  (let [node (z/node zipper)]
    (if (pred node)
      (z/replace zipper updated-question)
      zipper)))

(defn- edit-field
  [zipper id updated-question]
  (->
    zipper
    (conditional-replace #(= id (:id %)) updated-question)))

(defn- edit-fields
  [zipper id updated-field]
  (loop [current zipper]
    (if (z/end? current)
      current
      (recur (z/next (edit-field current id updated-field))))))

(defn- find-from-fields
  [zipper id]
  (loop [current zipper]
    (cond
      (= (:id (z/node current)) id)
      (z/node current)

      (z/end? current)
      nil

      :else
      (recur (z/next current)))))


(defn- update-fields
  [questions id updated-field]
  (-> questions
      questions->zipper
      (edit-fields id updated-field)
      zipper->questions))

(defn update-field-in-db
  [db updated-field]
  (let [id (:id updated-field)
        fields (get-in db [:form :content])
        updated-flat-form-fields (update-fields (get-in db [:flat-form-content]) id updated-field)
        updated-fields (update-fields fields id updated-field)]
    (as-> db db'
          (assoc-in db' [:flat-form-content] updated-flat-form-fields)
          (assoc-in db' [:form :content] updated-fields))))

(defn get-field-from-flat-form-content
  [db id]
  (some #(when (= id (:id %)) %)
        (:flat-form-content db)))

(defn get-field-from-content
  [db id]
  (let [fields (get-in db [:form :content])]
    (-> fields
        questions->zipper
        (find-from-fields id))))
