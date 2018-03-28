(ns ataru.db.migrations.application-migration-store
  (:require [camel-snake-kebab.core :as k]
            [camel-snake-kebab.extras :as t]
            [ataru.db.db :as db]
            [yesql.core :as sql]))

(sql/defqueries "sql/migration-1.25-queries.sql")
(sql/defqueries "sql/migration-1.28-queries.sql")
(sql/defqueries "sql/migration-1.36-queries.sql")
(sql/defqueries "sql/migration-1.71-queries.sql")
(sql/defqueries "sql/migration-1.75-queries.sql")
(sql/defqueries "sql/migration-1.80-queries.sql")
(sql/defqueries "sql/migration-1.82-queries.sql")
(sql/defqueries "sql/migration-1.86-queries.sql")
(sql/defqueries "sql/migration-1.88-queries.sql")

(defn get-ids-of-latest-applications
  []
  (map :id (db/exec :db yesql-get-ids-of-latest-applications {})))

(defn get-all-applications
  []
  (mapv (fn [application]
          (assoc (t/transform-keys k/->kebab-case-keyword application)
                 :content (:content application)))
        (db/exec :db yesql-get-all-applications {})))

(defn get-latest-versions-of-all-applications
  []
  (mapv (fn [application]
          (assoc (t/transform-keys k/->kebab-case-keyword application)
            :content (:content application)))
        (db/exec :db yesql-get-latest-versions-of-all-applications {})))

(defn set-application-key-to-application-review
  [review-id key]
  (db/exec :db yesql-set-application-key-to-application-review! {:application_key key :id review-id}))

(defn set-application-key-to-application-event
  [event-id key]
  (db/exec :db yesql-set-application-key-to-application-events! {:application_key key :id event-id}))

(defn get-application-confirmation-emails
  [application-id]
  (mapv (partial t/transform-keys k/->kebab-case-keyword)
        (db/exec :db yesql-get-application-confirmation-emails {:application_id application-id})))

(defn set-application-key-to-application-confirmation-email
  [confirmation-id key]
  (db/exec :db yesql-set-application-key-to-application-confirmation-emails! {:application_key key :id confirmation-id}))

(defn get-application-events-by-application-id
  [application-id]
  (mapv (partial t/transform-keys k/->kebab-case-keyword)
        (db/exec :db yesql-get-application-events-by-application-id {:application_id application-id})))

(defn get-application-review-by-application-id
  [application-id]
  (->> (db/exec :db yesql-get-application-review-by-application-id {:application_id application-id})
       (first)
       (t/transform-keys k/->kebab-case-keyword)))

(defn get-application-secret [{:keys [id]}]
  (db/exec :db yesql-get-application-secret {:id id}))

(defn set-application-secret [{:keys [id]} secret]
  (db/exec :db yesql-set-application-secret! {:id     id
                                              :secret secret}))

(defn get-applications-without-haku
  []
  (db/exec :db yesql-get-applications-with-hakukohde-and-without-haku {}))

(defn update-application-add-haku
  [application-id haku]
  (db/exec :db yesql-add-haku-to-application! {:application_id application-id
                                               :haku           (:oid haku)
                                               :haku_name      (-> haku :nimi :kieli_fi)}))

(defn update-application-content [application-id content]
  (db/exec :db yesql-update-application-content! {:id      application-id
                                                  :content content}))

(defn get-all-application-reviews []
  (->> (db/exec :db yesql-get-all-application-reviews {})
       (map (partial t/transform-keys k/->kebab-case-keyword))))

(defn create-application-review-note [note]
  {:pre [(-> note :application-key clojure.string/blank? not)
         (-> note :notes clojure.string/blank? not)]}
  (->> note
       (t/transform-keys k/->snake_case_keyword)
       (db/exec :db yesql-create-application-review-note!)))

(defn set-application-state
  [application-key state]
  (db/exec :db yesql-update-application-state! {:key application-key :state state}))

(defn get-1.86-forms [connection]
  (yesql-get-1_86-forms {} {:connection connection}))

(defn insert-1.86-form [connection form]
  (yesql-insert-1_86-form<! form {:connection connection}))

(defn get-1.86-applications [connection form-key]
  (yesql-get-1_86-applications {:form_key form-key} {:connection connection}))

(defn insert-1.86-application [connection application]
  (yesql-insert-1_86-application! application {:connection connection})
  (yesql-insert-1_86-application-event<! {:application_key  (:key application)
                                          :event_type       "updated-by-virkailija"
                                          :new_review_state nil
                                          :virkailija_oid   "1.2.246.562.24.56933707220"
                                          :hakukohde        nil
                                          :review_key       nil}
                                         {:connection connection}))

(defn get-1.88-forms [connection]
  (yesql-get-1_88-forms {} {:connection connection}))

(defn insert-1.88-form [connection form]
  (yesql-insert-1_88-form<! form {:connection connection}))