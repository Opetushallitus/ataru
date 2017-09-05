(ns ataru.db.extensions
  (:require [clojure.java.jdbc :as jdbc]
            [clj-time.coerce :as c]
            [cheshire.core :as json])
  (:import (java.sql PreparedStatement)
           (org.postgresql.util PGobject)))

(extend-protocol jdbc/ISQLValue
  clojure.lang.IPersistentCollection
  (sql-value [value]
    (doto (PGobject.)
      (.setType "jsonb")
      (.setValue (json/generate-string value)))))

(extend-protocol jdbc/IResultSetReadColumn
  PGobject
  (result-set-read-column [pgobj _ _]
    (let [type  (.getType pgobj)
          value (.getValue pgobj)]
      (case type
        "jsonb" (json/parse-string value true)
        :else value))))

(extend-protocol jdbc/IResultSetReadColumn
  java.sql.Date
  (result-set-read-column [v _ _] (c/from-sql-date v))

  java.sql.Timestamp
  (result-set-read-column [v _ _] (c/from-sql-time v))

  org.postgresql.jdbc.PgArray
  (result-set-read-column [v _ _]
    (vec (.getArray v))))

(extend-type org.joda.time.DateTime
  jdbc/ISQLParameter
  (set-parameter [v ^PreparedStatement stmt idx]
    (.setTimestamp stmt idx (c/to-sql-time v))))
