(ns ataru.background-job.clean-old-forms
  (:require [taoensso.timbre :as log]
            [ataru.db.db :as db]
            [yesql.core :refer [defqueries]]))

(declare yesql-clean-up-old-forms!)
(defqueries "sql/form-cleanup-queries.sql")

(defn clean-old-forms-job-step [_ _]
  (let [result (db/exec :db yesql-clean-up-old-forms! {:limit 1000})]
    (log/info (str "clean-old-forms-job: total of " result " forms cleaned."))))



