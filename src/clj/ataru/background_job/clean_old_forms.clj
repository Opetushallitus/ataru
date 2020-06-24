(ns ataru.background-job.clean-old-forms
  (:require [taoensso.timbre :as log]
            [ataru.db.db :as db]
            [yesql.core :refer [defqueries]]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]))

(defqueries "sql/form-cleanup-queries.sql")

(defn clean-old-forms-job-step [state _]
  (let [now (time/now)
        next-activation (time/plus (time/with-time-at-start-of-day now) (time/hours 26))
        result (db/exec :db yesql-clean-up-old-forms! {:limit 500})]
    (log/info (str "clean-old-forms-job: total of " result " forms cleaned. Next activation at " next-activation))
    {:transition      {:id :to-next :step :initial}
     :updated-state   {:last-run-long (coerce/to-long now)}
     :next-activation next-activation}))



