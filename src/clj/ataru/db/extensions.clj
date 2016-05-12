(ns ataru.db.extensions
  (:import [java.sql PreparedStatement])
  (:require [oph.soresu.common.jdbc.extensions] ; don't remove! imports JSONB handling
            [clojure.java.jdbc :as jdbc]
            [clj-time.coerce :as c]))

(extend-protocol jdbc/IResultSetReadColumn
                 java.sql.Date
                 (result-set-read-column [v _ _] (c/from-sql-date v))

                 java.sql.Timestamp
                 (result-set-read-column [v _ _] (c/from-sql-time v)))

(extend-type org.joda.time.DateTime
             jdbc/ISQLParameter
             (set-parameter [v ^PreparedStatement stmt idx]
                            (.setTimestamp stmt idx (c/to-sql-time v))))
