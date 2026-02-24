(ns ataru.applications.field-deadline
  (:require [ataru.applications.application-access-control :as aac]
            [ataru.db.db :as db]
            [ataru.log.audit-log :as audit-log]
            [cheshire.core :as json]
            [ataru.time.format :as f]
            [clojure.java.jdbc :as jdbc]
            [yesql.core :as yesql])
  (:import org.postgresql.util.PSQLException))

(declare yesql-get-field-deadline)
(declare yesql-add-application-event<!)
(yesql/defqueries "sql/field-deadline-queries.sql")
(yesql/defqueries "sql/application-queries.sql")

(def unique-violation "23505")

(def iso-formatter (f/formatter "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"))

(defn get-field-deadline
  [organization-service tarjonta-service audit-logger session application-key field-id]
  (if (aac/applications-access-authorized?
       organization-service
       tarjonta-service
       session
       [application-key]
       [:view-applications :edit-applications])
    (when-let [r (first (db/exec :db yesql-get-field-deadline {:application_key application-key
                                                               :field_id        field-id}))]
      (audit-log/log audit-logger
                     {:new       r
                      :id        {:applicationOid application-key
                                  :fieldId        field-id}
                      :session   session
                      :operation audit-log/operation-read})
      r)
    :unauthorized))

(defn- insert-event! [session connection type application-key field-id deadline]
  (let [deadline-string (some->> deadline
                                 (f/unparse iso-formatter))]
    (yesql-add-application-event<!
     {:application_key          application-key
      :event_type               type
      :new_review_state         deadline-string
      :virkailija_oid           (get-in session [:identity :oid])
      :virkailija_organizations (->> (-> session :identity :user-right-organizations :edit-applications)
                                     (map :oid)
                                     (json/generate-string))
      :hakukohde                nil
      :review_key               field-id}
     {:connection connection})))

(defn put-field-deadline
  [organization-service tarjonta-service audit-logger session application-key field-id deadline if-unmodified-since]
  (if (aac/applications-access-authorized?
       organization-service
       tarjonta-service
       session
       [application-key]
       [:edit-applications])
    (when-let [r (try
                   (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                     (some->> (if (some? if-unmodified-since)
                                (first (yesql-update-field-deadline
                                        {:application_key     application-key
                                         :field_id            field-id
                                         :deadline            deadline
                                         :if_unmodified_since if-unmodified-since}
                                        {:connection connection}))
                                (first (yesql-insert-field-deadline
                                        {:application_key application-key
                                         :field_id        field-id
                                         :deadline        deadline}
                                        {:connection connection})))
                              (do (insert-event!
                                   session
                                   connection
                                   "field-deadline-set"
                                   application-key
                                   field-id
                                   deadline))))
                   (catch PSQLException e
                     (if (= unique-violation (.getSQLState e))
                       nil
                       (throw e))))]
      (audit-log/log audit-logger
                     {:new       r
                      :id        {:applicationOid application-key
                                  :fieldId        field-id}
                      :session   session
                      :operation (if (some? if-unmodified-since)
                                   audit-log/operation-modify
                                   audit-log/operation-new)})
      r)
    :unauthorized))

(defn delete-field-deadline
  [organization-service tarjonta-service audit-logger session application-key field-id if-unmodified-since]
  (if (aac/applications-access-authorized?
       organization-service
       tarjonta-service
       session
       [application-key]
       [:edit-applications])
    (when-let [r (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                   (some->> (first (yesql-delete-field-deadline
                                    {:application_key     application-key
                                     :field_id            field-id
                                     :if_unmodified_since if-unmodified-since}
                                    {:connection connection}))
                            (do (insert-event!
                                 session
                                 connection
                                 "field-deadline-unset"
                                 application-key
                                 field-id
                                 nil))))]
      (audit-log/log audit-logger
                     {:old       r
                      :id        {:applicationOid application-key
                                  :fieldId        field-id}
                      :session   session
                      :operation audit-log/operation-delete})
      r)
    :unauthorized))
