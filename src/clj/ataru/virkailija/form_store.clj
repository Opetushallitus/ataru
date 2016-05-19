(ns ataru.virkailija.form-store
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

(defn upsert-form [{:keys [id] :as form}]
  (if (some? (when id
               (first (execute :db form-exists-query form))))
    (do
      (let [f (-> (transform-keys ->snake_case form)
                  (update-in [:content] (fn [content]
                                          (if (not-empty content)
                                            content
                                            nil))))]
        (jdbc/with-db-transaction [conn {:datasource (get-datasource :db)}]
          (update-form-query! f {:connection conn})
          (first (get-by-id f {:connection conn})))))
    (execute :db add-form-query<! form)))

(defn fetch-form [id]
  (first (execute :db get-by-id {:id id})))
