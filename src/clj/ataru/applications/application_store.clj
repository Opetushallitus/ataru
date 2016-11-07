(ns ataru.applications.application-store
  (:require [ataru.schema.form-schema :as schema]
            [camel-snake-kebab.core :as t :refer [->snake_case ->kebab-case-keyword]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [clj-time.core :as time]
            [schema.core :as s]
            [oph.soresu.common.db :as db]
            [yesql.core :refer [defqueries]]
            [clojure.java.jdbc :as jdbc]
            [crypto.random :as crypto]
            [taoensso.timbre :refer [info]]))

(defqueries "sql/application-queries.sql")

(defonce default-application-request
  {:sort :by-date})

(defn- exec-db
  [ds-key query params]
  (db/exec ds-key query params))

(def ^:private ->kebab-case-kw (partial transform-keys ->kebab-case-keyword))

(defn- find-value-from-answers [key answers]
  (:value (first (filter #(= key (:key %)) answers))))

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

(defn add-new-application
  "Add application and also initial metadata (event for receiving application, and initial review record)"
  [application conn]
  (let [connection           {:connection conn}
        answers              (:answers application)
        secret               (:secret application)
        application-to-store {:form_id        (:form application)
                              :key            (or (:key application)
                                                  (str (java.util.UUID/randomUUID)))
                              :lang           (:lang application)
                              :preferred_name (find-value-from-answers "preferred-name" answers)
                              :last_name      (find-value-from-answers "last-name" answers)
                              :hakukohde      (:hakukohde application)
                              :hakukohde_name (:hakukohde-name application)
                              :content        {:answers answers}
                              :secret         (or secret (crypto/url-part 128))}
        application          (yesql-add-application-query<! application-to-store connection)
        app-id               (:id application)
        app-key              (:key application)]
    (when-not secret
      (yesql-add-application-event! {:application_id   app-id
                                     :application_key  app-key
                                     :event_type       "review-state-change"
                                     :new_review_state "received"}
                                    connection)
      (yesql-add-application-review! {:application_id  app-id
                                      :application_key app-key
                                      :state           "received"}
                                     connection))
    app-id))

(defn- get-latest-version-and-lock-for-update [secret lang conn]
  (when-let [application (first (yesql-get-latest-version-by-secret-lock-for-update {:secret secret} {:connection conn}))]
    (unwrap-application {:lang lang} application)))

(defn add-application-or-increment-version! [{:keys [lang secret] :as new-application}]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [old-application (get-latest-version-and-lock-for-update secret lang conn)]
      (cond
        (some? old-application)
        (do
          (info (str "Updating application with key " (:key old-application) " based on valid application secret, retaining key and secret from previous version"))
          (add-new-application (merge new-application (select-keys old-application [:key :secret])) conn))

        (nil? old-application)
        (do
          (info (str "Inserting completely new application"))
          (add-new-application (dissoc new-application :key :secret) conn))))))

(defn- older?
  "Check if application given as first argument is older than
   application given as second argument by comparing :created-time."
  [a1 a2]
  (time/before? (:created-time a1)
                (:created-time a2)))

(defn- latest-versions-only [applications]
  (->> applications
       (reduce (fn [applications {:keys [key] :as a1}]
                 (let [a2 (get applications key)]
                   (if (or (nil? a2)
                           (older? a2 a1))
                     (assoc applications key a1)
                     applications)))
               {})
       (vals)
       (sort-by :created-time)
       (reverse)
       (vec)))

(defn get-application-list
  "Only list with header-level info, not answers"
  [form-key]
  (->> (exec-db :db yesql-get-application-list {:form_key form-key})
       (map ->kebab-case-kw)
       (latest-versions-only)))

(defn get-application-list-by-hakukohde
  [form-key hakukohde-oid]
  (->> (exec-db :db yesql-get-application-list-by-hakukohde {:hakukohde_oid hakukohde-oid :form_key form-key})
       (map ->kebab-case-kw)
       (latest-versions-only)))

(defn get-application [application-id]
  (unwrap-application {:lang "fi"} (first (exec-db :db yesql-get-application-by-id {:application_id application-id}))))

(defn get-latest-application-by-key [application-key]
  (unwrap-application {:lang "fi"} (first (exec-db :db yesql-get-latest-application-by-key {:application_key application-key}))))

(defn get-latest-application-by-secret [secret]
  (->> (exec-db :db yesql-get-latest-application-by-secret {:secret secret})
       (first)
       (unwrap-application {:lang "fi"})))

(defn get-application-events [application-key]
  (mapv ->kebab-case-kw (exec-db :db yesql-get-application-events {:application_key application-key})))

(defn get-application-review [application-key]
  (->kebab-case-kw (first (exec-db :db yesql-get-application-review {:application_key application-key}))))

(defn get-application-organization-oid [application-key]
  (:organization_oid (first (exec-db :db yesql-get-application-organization-by-key {:application_key application-key}))))

(defn get-application-review-organization-oid [review-id]
  (:organization_oid (first (exec-db :db yesql-get-application-review-organization-by-id {:review_id review-id}))))

(defn save-application-review [review]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [connection      {:connection conn}
          app-id          (:application-id review)
          app-key         (:application-key review)
          old-review      (first (yesql-get-application-review {:application_key app-key} connection))
          review-to-store (transform-keys ->snake_case review)]
      (println (str "saving application review: " review-to-store))
      (yesql-save-application-review! review-to-store connection)
      (when (not= (:state old-review) (:state review-to-store))
        (yesql-add-application-event!
         {:application_id app-id
          :application_key app-key
          :event_type "review-state-change"
          :new_review_state (:state review-to-store)}
         connection)))))

(s/defn get-applications :- [schema/Application]
  [form-key :- s/Str application-request :- schema/ApplicationRequest]
  (let [request (merge
                  {:form-key form-key}
                  default-application-request
                  application-request)]
    (->> (dissoc (transform-keys ->snake_case request) :sort)
         (exec-db :db yesql-application-query-by-modified)
         (mapv (partial unwrap-application request))
         (latest-versions-only))))

(defn add-person-oid
  "Add person OID to an application"
  [application-id person-oid]
  (exec-db :db yesql-add-person-oid!
    {:id application-id :person_oid person-oid}))

(defn get-hakukohteet
  []
  (mapv ->kebab-case-kw (exec-db :db yesql-get-hakukohteet-from-applications {})))
