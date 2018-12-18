(ns ataru.forms.form-store
  (:require [camel-snake-kebab.core :refer [->snake_case ->kebab-case-keyword]]
            [ataru.log.audit-log :as audit-log]
            [ataru.middleware.user-feedback :refer [user-feedback-exception]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [clojure.java.jdbc :as jdbc :refer [with-db-transaction]]
            [ataru.db.db :refer [exec get-datasource]]
            [yesql.core :refer [defqueries]]
            [taoensso.timbre :refer [warn]])
  (:import [java.util UUID]))

(defqueries "sql/form-queries.sql")

(defn- languages->vec [form]
  (update form :languages :languages))

(defn- languages->obj [form]
  (update form :languages
    (fn [languages]
      {:languages languages})))

(defn- unwrap-form-content
  "Unwraps form :content wrapper and transforms all other keys
   to kebab-case"
  [form]
  (let [form-no-content (->> (dissoc form :content)
                             (transform-keys ->kebab-case-keyword))
        form            (-> form-no-content
                            (assoc :content (or (get-in form [:content :content]) []))
                            (languages->vec))]
    form))

(defn- wrap-form-content
  "Wraps form :content and transforms all keys to snake_case"
  [{:keys [content] :as form}]
  (let [form-no-content (->> (dissoc form :content)
                             (transform-keys ->snake_case))
        form            (-> form-no-content
                            (assoc :content {:content (or content [])})
                            (languages->obj))]
    form))

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

(defn get-forms-by-keys [keys]
  (if (seq keys)
    (execute-with-db :db yesql-get-forms-by-keys {:keys keys})))

(defn get-forms [organization-oids]
  (execute-with-db :db yesql-get-forms-query {:authorized_organization_oids organization-oids
                                              :query-type "ORGS"}))

(defn get-all-forms []
  (execute yesql-get-forms-query {:authorized_organization_oids [""]
                                  :query-type                   "ALL"}))

(defn get-organization-oid-by-key [key]
  (:organization-oid (first (execute yesql-get-latest-version-organization-by-key {:key key}))))

(defn get-organization-oid-by-id [id]
  (:organization-oid (first (execute yesql-get-latest-version-organization-by-id {:id id}))))

(defn fetch-latest-version [id & [conn]]
  (first (execute yesql-fetch-latest-version-by-id {:id id} conn)))

(defn fetch-latest-version-and-lock-for-update [id conn]
  (first (execute yesql-fetch-latest-version-by-id-lock-for-update {:id id} conn)))

(defn fetch-by-id [id & [conn]]
  (first (execute yesql-get-by-id {:id id} conn)))

(def fetch-form fetch-latest-version)

(defn fetch-by-key [key & [conn]]
  (first (execute yesql-fetch-latest-version-by-key {:key key} conn)))

(defn latest-id-by-key [key]
  (:id (first (execute yesql-latest-id-by-key {:key key}))))

(defn latest-version-same? [form latest-version]
  (= (:id form) (:id latest-version)))

(defn create-new-form!
  ([form]
   (create-new-form! form (str (UUID/randomUUID))))
  ([form key]
   (first
    (execute yesql-add-form<!
             (->
              form
              (dissoc :created-time :id)
              (assoc :key key)
              (update :deleted identity))))))

(defn increment-version [{:keys [key id] :as form} conn]
  {:pre [(some? key)
         (some? id)]}
  (first
    (execute yesql-add-form<! (dissoc form :created-time :id))))

(defn create-form-or-increment-version! [{:keys [id organization-oid] :as form}]
  (or
    (with-db-transaction [conn {:datasource (get-datasource :db)}]
      (when-let [latest-version (not-empty (and id (fetch-latest-version-and-lock-for-update id conn)))]
        (if (not (latest-version-same? form latest-version))
          (do
            (warn (str "Form with id "
                       (:id latest-version)
                       " created-time "
                       (:created-time latest-version)
                       " already exists. Supplied form id was "
                       (:id form)
                       " created-time "
                       (:created-time form)))
            (throw (user-feedback-exception "Lomakkeen sisältö on muuttunut. Lataa sivu uudelleen.")))
          (let [new-form         (increment-version
                                  (-> form
                                        ; use :key set in db just to be sure it never is nil
                                      (assoc :key (:key latest-version))
                                      (update :deleted identity))
                                  conn)
                log-id           (:created-by new-form)]
            (audit-log/log {:new              (dissoc new-form :content)
                            :old              (dissoc latest-version :content)
                            :id               log-id
                            :operation        (if (:deleted new-form) audit-log/operation-delete audit-log/operation-modify)
                            :organization-oid organization-oid})
            new-form))))
    (let [new-form         (create-new-form! (dissoc form :key))
          log-id           (:created-by new-form)
          organization-oid (:organization-oid new-form)]
      (audit-log/log {:new              (dissoc new-form :content)
                      :id               log-id
                      :operation        audit-log/operation-new
                      :organization-oid organization-oid})
      new-form)))

(defn get-latest-form-by-name [form-name]
  (->> (execute-with-db :db yesql-get-latest-form-by-name {:form_name form-name})
       (map (comp keyword :key))
       (first)))

(defn get-form-by-application [application]
  (->> application
       :form_id
       fetch-by-id))
