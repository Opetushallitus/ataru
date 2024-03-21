(ns ataru.background-job.job
  "Public API of the Background Job system"
  (:require
    [ataru.config.core :refer [config]]
    [taoensso.timbre :as log]
    [com.stuartsierra.component :as component]
    [clojure.java.jdbc :as jdbc]
    [proletarian.worker :as worker]
    [proletarian.job :as job]
    [ataru.db.db :as db]))

(defonce handlers (atom {}))

(defn- as-keyword [kv]
       (if (keyword? kv) kv (keyword kv)))

(defprotocol JobRunner
  (start-job [this connection job-type initial-state]
    "Start a new background job of type <job-type>.
     initial-state is the initial data map needed to start the job
     (can be anything)"))

(def proletarian-options {:proletarian/job-table "proletarian_jobs"
                          :proletarian/archived-job-table "proletarian_archived_jobs"})

(defn- get-handler [job-definition]
  (if (:steps job-definition)
    (-> (:steps job-definition)
        (:initial))
    (:handler job-definition)))

(defrecord PersistentJobRunner [job-definitions]
  component/Lifecycle
  (start [this]
     (let [ds (db/get-datasource :job-db)
           worker (worker/create-queue-worker
                    ds
                    (fn [job-type _payload]
                        (log/info "Running" job-type (job-type @handlers))
                        (try
                          ; historiallisista syistä konventiona on että jobille annetaan toiseksi
                          ; parametriksi jobrunner-komponentti joka sisältää erilaisia jobien
                          ; tarvitsemia palveluita (ks. virkailija-system)
                          ((job-type @handlers) _payload this)
                          (catch Exception e
                            (log/error e "Failed job" job-type)
                            (throw e))))
                    proletarian-options)]

          (swap! handlers
                 (fn [_] (into {} (for [[_ v] job-definitions]
                                       [(keyword (:type v)) (get-handler v)]))))
          (worker/start! worker)
          (assoc this :worker worker)))

  (stop [this]
     (worker/stop! (:worker this))
     (swap! handlers (fn [_] nil))
     (assoc this :worker nil))

  JobRunner
  (start-job [_ _ job-type payload]
             (log/info "Registering job" job-type)
             (jdbc/with-db-transaction [tx {:datasource (db/get-datasource :job-db)}] ; toistaiseksi testausta varten
                                       (job/enqueue! (:connection tx) (as-keyword job-type) payload proletarian-options))))

(defrecord FakeJobRunner []
  component/Lifecycle
  (start [this] this)
  (stop [this] this)

  JobRunner
  (start-job [_ _ _ _]))

(defn new-job-runner [job-definitions]
  (if (-> config :dev :fake-dependencies) ;; Ui automated test mode
    (->FakeJobRunner)
    (->PersistentJobRunner job-definitions)))
