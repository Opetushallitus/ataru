(ns ataru.forms.form-store
  (:require [camel-snake-kebab.core :as t :refer [->snake_case ->kebab-case-keyword]]
            [ataru.db.extensions] ; don't remove, timestamp coercion
            [camel-snake-kebab.extras :refer [transform-keys]]
            [clojure.java.jdbc :as jdbc :refer [with-db-transaction]]
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

(defn- languages->vec [form]
  (update form :languages :languages))

(defn- languages->obj [form]
  (update form :languages
    (fn [languages]
      {:languages languages})))

(defn get-forms []
  (->> (execute :db yesql-get-forms-query {})
       (map languages->vec)))

(defn- restructure-form-with-content
  "Unwraps form :content wrapper and transforms all other keys
   to kebab-case"
  [form]
  (let [form-no-content (->> (dissoc form :content)
                             (transform-keys ->kebab-case-keyword))
        form (-> form-no-content
                 (assoc :content (or (get-in form [:content :content]) []))
                 (languages->vec))]
    form))

(defn- update-existing-form
  [existing-form modified-time form]
  (with-db-transaction [conn {:datasource (get-datasource :db)}]
                       (if (= (:modified-time existing-form) modified-time)
                         (do
                           (yesql-update-form-query! form {:connection conn})
                           (-> (yesql-get-by-id form {:connection conn})
                               (first)))
                         (throw (ex-info "form updated in background" {:error "form_updated_in_background"})))))

(defn upsert-form [{:keys [id] :as form-with-modified-time}]
  (let [modified-time (:modified-time form-with-modified-time)
        form (dissoc form-with-modified-time :modified-time)
        content {:content (or (not-empty (:content form))
                              [])}
        f       (-> (transform-keys ->snake_case (dissoc form :content))
                    (assoc :content content)
                    (languages->obj))]
    (restructure-form-with-content
      (let [existing-form (when id (first (execute :db yesql-get-by-id f)))]
        (if (some? existing-form)
          (update-existing-form existing-form modified-time f)
          (exec :db yesql-add-form-query<! f))))))

(defn fetch-form [id]
  (if-let [form (-> (exec :db yesql-get-by-id {:id id})
                    (first))]
    (restructure-form-with-content form)
    nil))
