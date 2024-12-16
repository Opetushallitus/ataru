(ns ataru.background-job.maksut-poller
  (:require
   [com.stuartsierra.component :as component]
   [taoensso.timbre :as log]
   [ataru.config.core :refer [config]]
   [ataru.background-job.maksut-poller-job :as maksut-poller-job]
   [ataru.db.db :as db]
   [yesql.core :refer [defqueries]]
   [clojure.string :as string])
  (:import [java.util.concurrent Executors TimeUnit]))

(declare yesql-get-status-poll-applications)

(defqueries "sql/maksut-queries.sql")

(defn- start-maksut-poller-job [application-service maksut-service _ apps]
   (maksut-poller-job/poll-maksut application-service maksut-service apps))

(defn- find-applications
  [application-service maksut-service job-runner]
  (try
    (if-let [apps (seq (db/exec :db yesql-get-status-poll-applications {:form_keys (string/split (-> config :tutkintojen-tunnustaminen :maksut :form-keys) #",")}))]
      (do
        (log/info "Found " (count apps) " applications in states waiting for Maksut -actions, checking their statuses")
        (start-maksut-poller-job application-service maksut-service job-runner apps))
      (log/info "No applications in need of Maksut-polling found"))
    (catch Exception e
      (log/error e "Maksut polling failed"))))

(defrecord MaksutPollWorker [job-runner
                             application-service
                             maksut-service
                             enabled?
                             executor]
  component/Lifecycle
  (start [this]
    (when (not enabled?)
      (log/warn "MaksutPollWorker disabled"))
    (if (and enabled? (nil? executor))
      (let [executor (Executors/newSingleThreadScheduledExecutor)
            interval (or (-> config :tutkintojen-tunnustaminen :maksut :poll-interval-minutes) 10)]
        (log/warn (str "Starting MaksutPollWorker with " interval "min interval"))
        (.scheduleAtFixedRate
         executor
         (partial find-applications
                  application-service
                  maksut-service
                  job-runner)
         0 interval TimeUnit/MINUTES)
        (assoc this :executor executor))
      this))
  (stop [this]
    (when (some? executor)
      (.shutdown executor))
    (assoc this :executor nil)))
