(ns ataru.forms.form-store
  (:require [camel-snake-kebab.core :refer [->snake_case ->kebab-case-keyword]]
            [ataru.log.audit-log :as audit-log]
            [ataru.middleware.user-feedback :refer [user-feedback-exception]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [clojure.java.jdbc :refer [with-db-transaction]]
            [ataru.db.db :refer [exec get-datasource]]
            [yesql.core :refer [defqueries]]
            [taoensso.timbre :as log]
            [ataru.util :as util])
  (:import [java.util UUID]))

(defqueries "sql/form-queries.sql")

;; Declare queries to keep linting happy
(declare yesql-get-forms)
(declare yesql-get-latest-version-organization-by-key)
(declare yesql-get-latest-version-organization-by-id)
(declare yesql-fetch-latest-version-by-id)
(declare yesql-fetch-latest-version-by-id-lock-for-update)
(declare yesql-get-by-id)
(declare yesql-get-by-ids)
(declare yesql-get-siirtotiedosto-form-ids)
(declare yesql-form-by-key-has-applications)
(declare yesql-fetch-latest-version-by-key)
(declare yesql-latest-id-by-key)
(declare yesql-add-form<!)
(declare yesql-get-latest-form-by-name)

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

(defn assoc-flattened-content [form]
  (assoc form :flat-content (util/flatten-form-fields (:content form))))


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

(defn get-all-forms
  ([]
   (get-all-forms nil))
  ([hakukohderyhma-oid]
   (execute yesql-get-forms {:hakukohderyhma_oid hakukohderyhma-oid})))

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

(defn fetch-forms-by-ids [ids]
  (log/info "Fetching forms for" (count ids) "ids.")
  (->> (execute yesql-get-by-ids {:ids ids} nil)
       (map assoc-flattened-content)))

(def fetch-form fetch-latest-version)

(defn form-has-applications [key & [conn]]
  (let [res (first (execute yesql-form-by-key-has-applications {:key key} conn))]
    (boolean (:exists res))))

(defn fetch-by-key [key & [conn]]
  (first (execute yesql-fetch-latest-version-by-key {:key key} conn)))

(defn latest-id-by-key [key]
  (:id (first (execute yesql-latest-id-by-key {:key key}))))

(defn latest-version-same? [form latest-version]
  (= (:id form) (:id latest-version)))

(defn form-used-hakukohderyhmas [form]
  (->> form
       :content
       util/flatten-form-fields
       (mapcat :belongs-to-hakukohderyhma)
       (remove nil?)
       distinct))

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
              (assoc :used_hakukohderyhmas (form-used-hakukohderyhmas form))
              (update :deleted identity)
              (update :properties (fnil identity {})))))))

(defn increment-version [{:keys [key id] :as form} _]
  {:pre [(some? key)
         (some? id)]}
  (first
    (execute yesql-add-form<! (-> form
                                  (dissoc :created-time :id)
                                  (assoc :used_hakukohderyhmas (form-used-hakukohderyhmas form))
                                  (update :properties (fnil identity {}))))))

(defn create-form-or-increment-version! [{:keys [id] :as form} session audit-logger]
  (or
    (with-db-transaction [conn {:datasource (get-datasource :db)}]
      (when-let [latest-version (not-empty (and id (fetch-latest-version-and-lock-for-update id conn)))]
        (if (not (latest-version-same? form latest-version))
          (do
            (log/warn (str "Form with id "
                           (:id latest-version)
                           " created-time "
                           (:created-time latest-version)
                           " already exists. Supplied form id was "
                           (:id form)
                       " created-time "
                       (:created-time form)))
            (throw (user-feedback-exception "Lomakkeen sisältö on muuttunut. Lataa sivu uudelleen.")))
          (let [new-form (increment-version
                          (-> form
                              ;; use :key set in db just to be sure it never is nil
                              (assoc :key (:key latest-version))
                              (update :deleted identity))
                          conn)]
            (audit-log/log audit-logger
                           {:new       (dissoc new-form :content)
                            :old       (dissoc latest-version :content)
                            :id        {:formKey (:key new-form)}
                            :session   session
                            :operation (if (:deleted new-form)
                                         audit-log/operation-delete
                                         audit-log/operation-modify)})
            new-form))))
    (let [new-form (if (:key form)
                     (create-new-form! form (:key form))
                     (create-new-form! form))]
      (audit-log/log audit-logger
                     {:new       (dissoc new-form :content)
                      :id        {:formKey (:key new-form)}
                      :session   session
                      :operation audit-log/operation-new})
      new-form)))

(defn get-latest-form-by-name [form-name]
  (->> (execute-with-db :db yesql-get-latest-form-by-name {:form_name form-name})
       (map (comp keyword :key))
       (first)))

(defn get-form-by-application [application]
  (->> application
       :form_id
       fetch-by-id))

(defn siirtotiedosto-form-ids [{:keys [modified-before modified-after] :as params}]
  (log/info "Fetching changed form ids for siirtotiedosto with params" params)
  (execute-with-db :db yesql-get-siirtotiedosto-form-ids {:modified_before modified-before
                                                          :modified_after modified-after}))