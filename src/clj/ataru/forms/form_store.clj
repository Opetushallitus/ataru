(ns ataru.forms.form-store
  (:require [camel-snake-kebab.core :refer [->snake_case ->kebab-case-keyword]]
            [ataru.db.extensions] ; don't remove, timestamp/jsonb coercion
            [camel-snake-kebab.extras :refer [transform-keys]]
            [clojure.java.jdbc :as jdbc :refer [with-db-transaction]]
            [clj-time.core :as t]
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
  "Wraps form :content and transforms all keys to snake_case"
  [{:keys [content] :as form}]
  (assoc (transform-keys ->snake_case (dissoc form :content))
    :content {:content (or content [])}))


(defn- postprocess [result]
  (->> (if (or (seq? result) (list? result) (vector? result)) result [result])
    (mapv unwrap-form-content)))

(defn execute-with-db [db yesql-query-fn params]
  (->> params
       wrap-form-content
       (exec db yesql-query-fn)
       postprocess))

(defn execute-with-connection [conn yesql-query-fn params]
  (->>
    (yesql-query-fn
      (wrap-form-content params)
      {:connection conn})
    postprocess))

(defn execute [yesql-query-fn params & [conn]]
  (if conn
    (execute-with-connection conn yesql-query-fn params)
    (execute-with-db :db yesql-query-fn params)))

(defn get-forms []
  (execute-with-db :db yesql-get-forms {}))

(defn fetch-latest-version [id & [conn]]
  (first (execute yesql-fetch-latest-version-by-id {:id id} conn)))

(defn fetch-latest-version-and-lock-for-update [id conn]
  (first (execute yesql-fetch-latest-version-by-id-lock-for-update {:id id} conn)))

(defn fetch-by-id [id & [conn]]
  (first (execute yesql-get-by-id {:id id} conn)))

(def fetch-form fetch-latest-version)

(defn fetch-by-key [key & [conn]]
  (first (execute yesql-fetch-latest-version-by-key {:key key} conn)))

(defn throw-if-latest-version-not-same [form latest-version]
  (when
      (or
        (not= (:id form) (:id latest-version))
        (not= (:created-by latest-version) (:created-by form)) ; should never go here because rows are not updated anymore
        (t/after? (:created-time latest-version) (:created-time form)))
    (do
      (throw (Exception. (str "Form with id " (:id latest-version) " created-time " (:created-time latest-version)
                           " already exists."))))))

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
          (throw-if-latest-version-not-same form latest-version)
          (increment-version
            ; use :key set in db just to be sure it never is nil
            (assoc form :key (:key latest-version))
            conn))))
    (create-new-form! (dissoc form :key))))
