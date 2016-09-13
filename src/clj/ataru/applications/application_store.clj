(ns ataru.applications.application-store
  (:require [ataru.schema.form-schema :as schema]
            [camel-snake-kebab.core :as t :refer [->snake_case ->kebab-case-keyword]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [schema.core :as s]
            [oph.soresu.common.db :as db]
            [yesql.core :refer [defqueries]]
            [clojure.java.jdbc :as jdbc]))

(defqueries "sql/application-queries.sql")

(defonce default-application-request
  {:sort :by-date
   :lang "fi"})

(defn- exec-db
  [ds-key query params]
  (db/exec ds-key query params))

(defn- find-value-from-answers [key answers]
  (:value (first (filter #(= key (:key %)) answers))))

(defn add-new-application
  "Add application and also initial metadata (event for receiving application, and initial review record)"
  [application]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [connection           {:connection conn}
          answers              (:answers application)
          application-to-store {:form_id (:form application)
                                :key (str (java.util.UUID/randomUUID))
                                :lang (:lang application)
                                :preferred_name (find-value-from-answers "preferred-name" answers)
                                :last_name (find-value-from-answers "last-name" answers)
                                :content {:answers answers}}
          app-id               (:id (yesql-add-application-query<! application-to-store connection))]
      (yesql-add-application-event! {:application_id app-id :event_type "received"} connection)
      (yesql-add-application-review! {:application_id app-id :state "received"} connection)
      app-id)))

(defn unwrap-application [{:keys [lang]} application]
  (assoc (transform-keys ->kebab-case-keyword (dissoc application :content))
         :answers
         (mapv (fn [answer]
                 (update answer :label (keyword lang)))
               (-> application :content :answers))))

(defn get-application-list
  "Only list with header-level info, not answers"
  [form-key]
  (mapv #(transform-keys ->kebab-case-keyword %) (exec-db :db yesql-get-application-list {:form_key form-key})))

(defn get-application [application-id]
  (unwrap-application {:lang "fi"} (first (exec-db :db yesql-get-application-by-id {:application_id application-id}))))

(defn get-application-events [application-id]
  (mapv #(transform-keys ->kebab-case-keyword %) (exec-db :db yesql-get-application-events {:application_id application-id})))

(defn get-application-review [application-id]
  (transform-keys ->kebab-case-keyword (first (exec-db :db yesql-get-application-review {:application_id application-id}))))

(defn save-application-review [review]
  (exec-db :db yesql-save-application-review! (transform-keys ->snake_case review)))

(s/defn get-applications :- [schema/Application]
  [form-key :- s/Str application-request :- schema/ApplicationRequest]
  (let [request (merge
                  {:form-key form-key}
                  default-application-request
                  application-request)]
    (mapv (partial unwrap-application request)
          (exec-db :db (case (:sort request)
                         :by-date yesql-application-query-by-modified
                         yesql-application-query-by-modified)
                   (dissoc (transform-keys ->snake_case request)
                           :sort)))))
