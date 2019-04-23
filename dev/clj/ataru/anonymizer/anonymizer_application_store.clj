(ns ataru.anonymizer.anonymizer-application-store
  (:require [ataru.db.db :as db]
            [ataru.util.random :as crypto]
            [clojure.java.jdbc :as jdbc]
            [yesql.core :as sql]))

(sql/defqueries "sql/anonymizer-application-queries.sql")

(defn get-all-applications []
  (db/exec :db sql-get-all-applications {}))

(defn update-application [application]
  (db/exec :db sql-update-application! application))

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
