(ns ataru.anonymizer.anonymizer-application-store
  (:require [ataru.db.db :as db]
            [ataru.util.random :as crypto]
            [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as log]
            [yesql.core :as sql])
  (:import org.postgresql.util.PGobject))

(sql/defqueries "sql/anonymizer-application-queries.sql")

(defn get-all-application-ids []
  (map :id (db/exec :db sql-get-all-applications {})))

(defn delete-application [id]
  (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
    (sql-delete-application! {:id id} {:connection connection})))

(defn get-application [id]
  (first (db/exec :db sql-get-application {:id id})))

(defn anonymize-guardian! []
  (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
    (sql-update-multi-by-key! {:key "guardian-name" :val "Testi Huoltaja"}
                              {:connection connection})
    (sql-update-multi-by-key! {:key "guardian-name-secondary" :val "Testi Huoltaja"}
                              {:connection connection})
    (sql-update-multi-by-key! {:key "guardian-firstname" :val "Testi"}
                              {:connection connection})
    (sql-update-multi-by-key! {:key "guardian-firstname-secondary" :val "Testi"}
                              {:connection connection})
    (sql-update-multi-by-key! {:key "guardian-lastname" :val "Huoltaja"}
                              {:connection connection})
    (sql-update-multi-by-key! {:key "guardian-lastname-secondary" :val "Huoltaja"}
                              {:connection connection})
    (sql-update-multi-by-key! {:key "guardian-phone" :val "0501234567"}
                              {:connection connection})
    (sql-update-multi-by-key! {:key "guardian-phone-secondary" :val "0501234567"}
                              {:connection connection})
    (sql-update-multi-by-key! {:key "guardian-email" :val "testi1.huoltaja@testiopintopolku.fi"}
                              {:connection connection})
    (sql-update-multi-by-key! {:key "guardian-email-secondary" :val "testi2.huoltaja@testiopintopolku.fi"}
                              {:connection connection})))

(defn anonymize-long-textareas-group! []
  (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
    (sql-anonymize-long-textareas-group! {} {:connection connection}))
  (log/info "Done anonymizing long textareas in group answers"))

(defn anonymize-long-textareas-multi! []
  (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
    (sql-anonymize-long-textareas-multi! {} {:connection connection}))
  (log/info "Done anonymizing long textareas in multi answers"))

(defn anonymize-long-textareas! []
  (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
    (sql-anonymize-long-textareas! {} {:connection connection}))
  (log/info "Done anonymizing long textareas in answers"))

(defn update-application [application]
  (let [answers             (:answers (:content application))
        update-answers-args {:application_id (:id application)
                             :answers        (doto (new PGobject)
                                               (.setType "jsonb")
                                               (.setValue (json/generate-string answers)))}]
    (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
      (sql-update-application! application {:connection connection})
      (sql-update-application-answers! update-answers-args {:connection connection})
      (sql-update-application-multi-answer-values! update-answers-args {:connection connection})
      (sql-update-application-group-answer-values! update-answers-args {:connection connection}))))

(defn regenerate-application-secrets! []
  (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
    (doseq [id-chunk (->> (sql-application-secret-ids {} {:connection connection})
                          (map :id)
                          (partition 1000 1000 nil))]
      (jdbc/db-do-prepared connection true
                           (into ["UPDATE application_secrets
                                   SET secret = ?
                                   WHERE id = ?"]
                                 (map vector (repeatedly (fn [] (crypto/url-part 34))) id-chunk))
                           {:multi? true})))
  (log/info "Removing non-anonymized application secrets")
  (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
    (delete-non-anonymized-secrets! {} {:connection connection})))
