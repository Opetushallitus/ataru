(ns ataru.background-job.job-execution-spec
  "This tests how everything works together with database. More detailed tests are in job_execution_spec.clj."
  (:require
   [yesql.core :refer [defqueries]]
   [clj-time.core :as time]
   [speclj.core :refer [tags describe it should= before-all after-all]]
   [ataru.background-job.job :as job]
   [camel-snake-kebab.extras :refer [transform-keys]]
   [camel-snake-kebab.core :refer [->kebab-case-keyword]]
   [oph.soresu.common.db :as db]))

(defqueries "sql/dev-job-queries.sql")

;; mock time to be in the the past so that delayed retries
;; still happen immediately. We don't want to wait minutes here.
(defn fixed-now [] (time/date-time 2015 10 10))

(def
  retrying-job
  (letfn [(initial [state context]
            {:transition {:id    :to-next
                          :step  :second-step}})

          (second-step [state context]
            {:transition {:id    :to-next
                          :step  :try-three-times}
             :updated-state (assoc state :second-step-ok true)})

          (try-three-times [state context]
            (if (= 3 (:counter state))
              {:transition {:id :final}}
              {:transition {:id :retry}
               :updated-state (assoc state :counter (inc (:counter state)))}))]

    {:steps {:initial initial
             :second-step second-step
             :try-three-times try-three-times}
     :type "retrying-job"}))

(def job-definitions {(:type retrying-job) retrying-job})

(def expected-final-iteration {:step "try-three-times"
                               :state {:counter 3, :second-step-ok true},
                               :next-activation nil,
                               :transition "final"
                               :retry-count 0,
                               :final true,
                               :error nil})

(defn start-job-runner []
  (let [runner (assoc (job/->JobRunner) :job-definitions job-definitions)]
    (.start runner)))

(defn get-final-iteration-for-job [job-id]
  (transform-keys ->kebab-case-keyword (first (db/exec :db yesql-get-final-iteration-for-job {:job_id job-id}))))

(def max-poll-count 100)

(defn wait-for-final-iteration [job-id]
  (loop [poll-count 1]
    (if (= max-poll-count poll-count)
      (throw (Exception. (str "Didn't find final iteration after polling " max-poll-count " times"))))
    (let [final-iteration (get-final-iteration-for-job job-id)]
      (if final-iteration
        final-iteration
        (do
          (Thread/sleep 200) ;; Let's not busy-loop while polling DB
          (println "Waiting for final iteration of job" job-id)
          (recur (inc poll-count)))))))

(describe
 "background job runner"
 (tags :unit :dev2)

 (it "Check that job finishes without errors and with correct state"
     (with-redefs [time/now fixed-now]
       (let [job-runner      (start-job-runner)
             job-id          (job/start-job job-definitions (:type retrying-job) {:counter 0})
             final-iteration (wait-for-final-iteration job-id)]
         (should= expected-final-iteration
                  final-iteration)
         (.stop job-runner)))))
