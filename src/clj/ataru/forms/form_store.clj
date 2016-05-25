(ns ataru.forms.form-store
  (:require [camel-snake-kebab.core :as t :refer [->snake_case ->kebab-case-keyword]]
            [ataru.db.extensions] ; don't remove, timestamp coercion
            [camel-snake-kebab.extras :refer [transform-keys]]
            [clojure.java.jdbc :as jdbc]
            [oph.soresu.common.db :refer [exec get-datasource]]
            [yesql.core :refer [defqueries]]
            [taoensso.timbre :refer [spy debug]]))

(defqueries "sql/form-queries.sql")

(defn execute [db query params]
  (->> params
       (transform-keys ->snake_case)
       (exec db query)
       (transform-keys ->kebab-case-keyword)
       vec))

(defn get-forms []
  (execute :db get-forms-query {}))

(defn- restructure-form-with-content
  "Unwraps form :content wrapper and transforms all other keys
   to kebab-case"
  [form]
  (assoc (transform-keys ->kebab-case-keyword (dissoc form :content))
         :content (-> form :content :content)))

(defn upsert-form [{:keys [id] :as form}]
  (let [content {:content (or (not-empty (:content form))
                              [])}
        f       (-> (transform-keys ->snake_case (dissoc form :content))
                    (assoc :content content))]
    (restructure-form-with-content
      (if (some? (when id
                   (first (execute :db form-exists-query f))))
        (do
          (jdbc/with-db-transaction [conn {:datasource (get-datasource :db)}]
            (update-form-query! f {:connection conn})
            (first (get-by-id f {:connection conn}))))
        (exec :db add-form-query<! f)))))

(defn fetch-form [id]
  (if-let [form (first (exec :db get-by-id {:id id}))]
    (restructure-form-with-content form)
    nil))
