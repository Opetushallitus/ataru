(ns ataru.background-job.clean-old-sessions
  (:require [taoensso.timbre :as log]
            [ataru.db.db :as db]
            [yesql.core :refer [defqueries]]))

(declare yesql-clean-up-old-sessions!)
(defqueries "sql/session-cleanup-queries.sql")

(defn clean-old-sessions-job-step [_ _]
  (log/info "clean-old-sessions-job: starting cleanup of old sessions.")
  (let [result (db/exec :db yesql-clean-up-old-sessions! {})]
    (log/info (str "clean-old-sessions-job: total of " result " sessions cleaned."))))
