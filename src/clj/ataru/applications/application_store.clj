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
  {:sort :by-date})

(defn- exec-db
  [ds-key query params]
  (db/exec ds-key query params))

(def ^:private ->kebab-case-kw (partial transform-keys ->kebab-case-keyword))

(defn- find-value-from-answers [key answers]
  (:value (first (filter #(= key (:key %)) answers))))

(defn add-new-application
  "Add application and also initial metadata (event for receiving application, and initial review record)"
  [application]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [connection           {:connection conn}
          answers              (:answers application)
          application-to-store {:form_id        (:form application)
                                :key            (str (java.util.UUID/randomUUID))
                                :lang           (:lang application)
                                :preferred_name (find-value-from-answers "preferred-name" answers)
                                :last_name (find-value-from-answers "last-name" answers)
                                :hakukohde (:hakukohde application)
                                :hakukohde_name (:hakukohde-name application)
                                :content {:answers answers}}
          app-id               (:id (yesql-add-application-query<! application-to-store connection))]
      (yesql-add-application-event! {:application_id   app-id
                                     :event_type       "review-state-change"
                                     :new_review_state "received"}
                                    connection)
      (yesql-add-application-review! {:application_id app-id
                                      :state          "received"}
                                     connection)
      app-id)))

(defn unwrap-application [{:keys [lang]} application]
  (assoc (->kebab-case-kw (dissoc application :content))
         :answers
         (mapv (fn [answer]
                 (update answer :label (fn [label]
                                         (or
                                           (:fi label)
                                           (:sv label)
                                           (:en label)))))
               (-> application :content :answers))))

(defn get-application-list
  "Only list with header-level info, not answers"
  [form-key]
  (mapv ->kebab-case-kw (exec-db :db yesql-get-application-list {:form_key form-key})))

(defn get-application [application-id]
  (unwrap-application {:lang "fi"} (first (exec-db :db yesql-get-application-by-id {:application_id application-id}))))

(defn get-application-events [application-id]
  (mapv ->kebab-case-kw (exec-db :db yesql-get-application-events {:application_id application-id})))

(defn get-application-review [application-id]
  (->kebab-case-kw (first (exec-db :db yesql-get-application-review {:application_id application-id}))))

(defn get-application-organization-oid [application-id]
  (:organization_oid (first (exec-db :db yesql-get-application-organization-by-id {:application_id application-id}))))

(defn get-application-review-organization-oid [review-id]
  (:organization_oid (first (exec-db :db yesql-get-application-review-organization-by-id {:review_id review-id}))))

(defn save-application-review [review]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [connection      {:connection conn}
          app-id          (:application-id review)
          old-review      (first (yesql-get-application-review {:application_id app-id} connection))
          review-to-store (transform-keys ->snake_case review)]
      (yesql-save-application-review! review-to-store connection)
      (when (not= (:state old-review) (:state review-to-store))
        (yesql-add-application-event!
         {:application_id app-id :event_type "review-state-change" :new_review_state (:state review-to-store)}
         connection)))))

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

(defn add-person-oid
  "Add person OID to an application"
  [application-id person-oid]
  (exec-db :db yesql-add-person-oid!
    {:id application-id :person_oid person-oid}))
