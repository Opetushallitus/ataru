(ns ataru.forms.form-store
  (:require [camel-snake-kebab.core :as t :refer [->snake_case ->kebab-case-keyword]]
            [ataru.db.extensions] ; don't remove, timestamp/jsonb coercion
            [camel-snake-kebab.extras :refer [transform-keys]]
            [clojure.java.jdbc :as jdbc :refer [with-db-transaction]]
            [oph.soresu.common.db :refer [exec get-datasource]]
            [yesql.core :refer [defqueries]]
            [taoensso.timbre :refer [spy debug]])
  (:import [java.util UUID]))

(defqueries "sql/form-queries.sql")

(defn- unwrap-form-content
  "Unwraps form :content wrapper and transforms all other keys
   to kebab-case"
  [form]
  (assoc (transform-keys ->kebab-case-keyword (dissoc form :content))
    :content (or (-> form :content :content)
               [])))

(defn- wrap-form-content
  "Wraps form :content and transforms all keys tosnake_case"
  [{:keys [content] :as form}]
  (assoc (transform-keys ->snake_case (dissoc form :content))
    :content content))


(defn execute-with-db [db yesql-query-fn params]
  (->> params
       (transform-keys ->snake_case)
       (exec db yesql-query-fn)
       (transform-keys ->kebab-case-keyword)
       (map unwrap-form-content)
       vec))

(defn execute-with-connection [conn yesql-query-fn params]
  (->>
    (yesql-query-fn
      (transform-keys ->snake_case params)
      {:connection conn})
    (transform-keys ->kebab-case-keyword)
    (map unwrap-form-content)
    vec))

(defn execute [yesql-query-fn params & [conn]]
  (if conn
    (execute-with-connection conn yesql-query-fn params)
    (execute-with-db :db yesql-query-fn params)))

(defn get-forms []
  (execute-with-db :db yesql-get-forms {}))

(defn fetch-latest-version [id & [conn]]
  (first (execute conn yesql-fetch-latest-version-by-id {:id id})))

(defn fetch-latest-version-and-lock-for-update [id conn]
  (first (execute conn yesql-fetch-latest-version-by-id-lock-for-update {:id id})))

(defn fetch-by-id [id & [conn]]
  (first (execute conn yesql-get-by-id {:id id})))

(def fetch-form fetch-latest-version)

(defn throw-if-latest-version-author-changed! [form latest-version]
  (when
      (or
        (nil? (:created-by latest-version))
        (nil? (:created-by form))
        (not= (:created-by latest-version) (:created-by form)))
    (throw (Exception. (str "Form" form "modified by " (:created-by latest-version))))))

(defn create-new-form! [form]
  (first
    (execute yesql-add-form<!
      (->
        form
        (dissoc :created-time :id)
        (assoc :key (str (UUID/randomUUID)))))))


(defn increment-version [{:keys [key id] :as form} conn]
  {:pre [(some? key)
         (some? id)]}
  (first
    (execute yesql-add-form<! (dissoc form :created-time :id))))

(defn create-form-or-increment-version! [{:keys [id] :as form}]
  (or
    (with-db-transaction [conn {:datasource (get-datasource :db)}]
      (when-let [latest-version (not-empty (and id (fetch-latest-version-and-lock-for-update id conn)))]
        (do
          (throw-if-latest-version-author-changed! form latest-version)
          (debug "contents have changed?" (not= (:content form) (:content latest-version)))
          (increment-version
            ; use :key set in db just to be sure it never is nil
            (assoc form :key (:key latest-version))
            conn))))
    (create-new-form! (dissoc form :key))))
