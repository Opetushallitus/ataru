(ns ataru.background-job.job-execution-spec
  (:require
   [clj-time.core :as time]
   [speclj.core :refer [tags describe it should=]]
   [ataru.background-job.job-execution :as job-exec]))

(defn call-fakeservice [service]
  (swap! service assoc :call-count (inc (:call-count @service)))
  (if (> (:call-count @service) 1)
    true
    false))

(defn job1-initial [state context]
  {:transition {:id    :to-next
                :step  :fake-remote-call}
   :updated-state (assoc state :initialized true)})

(defn fake-remote-call [state context]
  (let [fake-service (:fake-service context)
        service-call-result (call-fakeservice fake-service)]
    (if-not service-call-result
      {:transition {:id :retry} :updated-state (assoc state :damn (inc (:damn state)))}
      {:transition {:id :final}})))

(def job1-steps {:initial job1-initial
                 :fake-remote-call fake-remote-call})

(def job1-definition {:steps job1-steps
                      :type "job1"})

(def job-definitions {(:type job1-definition) job1-definition})

(defn fixed-now [] (time/date-time 2016 10 10))

(describe
 "job execution"
 (tags :unit :dev)
 (it "exec job runs steps and produces iterations until there is a final transition"
     (with-redefs [time/now fixed-now]
       (let [runner            {:job-definitions job-definitions
                                :fake-service    (atom {:call-count  0})}
             job               {:job-type "job1"
                                :iteration {:state {:damn 0}
                                            :step :initial
                                            :retry-count 0}}
             result-iterations (job-exec/exec-job runner job)]
         (should= [{:step :fake-remote-call, :transition :to-next, :final false, :retry-count 0, :next-activation (fixed-now), :state {:damn 0, :initialized true}, :executed true, :error nil}
                   {:step :fake-remote-call, :transition :retry, :final false, :retry-count 1, :next-activation (time/plus (fixed-now) (time/minutes 1)), :state {:damn 1, :initialized true}, :executed false, :error nil}]
                  result-iterations)))))
