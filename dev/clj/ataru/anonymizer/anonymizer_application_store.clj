(ns ataru.anonymizer.anonymizer-application-store
  (:require [ataru.db.db :as db]
            [ataru.util.random :as crypto]
            [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc]
            [yesql.core :as sql])
  (:import org.postgresql.util.PGobject))

(sql/defqueries "sql/anonymizer-application-queries.sql")

(defn get-all-application-ids []
  (map :id (db/exec :db sql-get-all-applications {})))

(defn get-application [id]
  (first (db/exec :db sql-get-application {:id id})))

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

(defn regenerate-application-secrets []
  (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
    (doseq [id-chunk (->> (sql-application-secret-ids {} {:connection connection})
                          (map :id)
                          (partition 1000 1000 nil))]
      (jdbc/db-do-prepared connection false
                           (into ["UPDATE application_secrets
                                   SET secret = ?
                                   WHERE id = ?"]
                                 (map vector (repeatedly (fn [] (crypto/url-part 34))) id-chunk))
                           {:multi? true}))))
