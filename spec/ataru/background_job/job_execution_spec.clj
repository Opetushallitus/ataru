(ns ataru.background-job.job-execution-spec
  (:require
   [clj-time.core :as time]
   [speclj.core :refer [tags describe it should= should-contain]]
   [ataru.background-job.email-job :as email-job]
   [ataru.background-job.job-execution :as job-exec]))

;; mocked time for test execution
(defn fixed-now [] (time/date-time 2016 10 10))

;; mocked email sending with parameter check
(defn send-email-assert-error [_ recipients _ body]
  (should= recipients ["testi@example.org"])
  (should-contain "&quot;type&quot;:&quot;java.lang.Error&quot;,&quot;message&quot;:&quot;INSTANT FATAL ISSUE&quot;" body))

(defn send-email-assert-max-retry [_ recipients _ body]
  (should= recipients ["testi@example.org"])
  (should-contain "Retry limit exceeded for step :throwing in job id 3 type always-exception-throwing-job" body))

(def
  job1
  (letfn [(call-fakeservice [service]
            (swap! service assoc :call-count (inc (:call-count @service)))
            (if (> (:call-count @service) 1)
              true
              false))

          (initial [state _]
            {:transition {:id    :to-next
                          :step  :fake-remote-call}
             :updated-state (assoc state :initialized true)})

          (fake-remote-call [state context]
            (let [fake-service (:fake-service context)
                  service-call-result (call-fakeservice fake-service)]
              (if-not service-call-result
                {:transition {:id :retry} :updated-state (assoc state :damn (inc (:damn state)))}
                {:transition {:id :final}})))]

    {:steps {:initial initial
             :fake-remote-call fake-remote-call}
     :type "job1"}))

(def expected-job1-iterations [{:step
                                :fake-remote-call,
                                :transition :to-next,
                                :final false,
                                :retry-count 0,
                                :next-activation (fixed-now),
                                :state {:damn 0, :initialized true},
                                :caused-by-error nil}
                               {:step
                                :fake-remote-call,
                                :transition :retry,
                                :final false,
                                :retry-count 1,
                                :next-activation (time/plus (fixed-now) (time/minutes 1)),
                                :state {:damn 1, :initialized true},
                                :caused-by-error nil}
                               {:step :fake-remote-call,
                                :transition :final,
                                :final true,
                                :retry-count 0,
                                :next-activation nil,
                                :state {:damn 1, :initialized true},
                                :caused-by-error nil}])

(def
  fatally-failing-job
  (letfn [(fatally-flawed-step [_ _]
            (throw (Error. "INSTANT FATAL ISSUE")))]

    {:steps {:initial fatally-flawed-step}
     :type "fatally-failing-job"}))

(def
  always-exception-throwing-job
  (letfn [(initial [_ _]
            {:transition {:id  :to-next
                          :step  :throwing}})
            (throwing-step [_ _]
                           (throw (Exception. "This exception is normal on test-run! This should happen, so don't be alarmed by it when you see it on test-runs.")))]

    {:steps {:initial initial
             :throwing throwing-step}
     :type "always-exception-throwing-job"}))

(def job-definitions {(:type job1) job1
                      (:type fatally-failing-job) fatally-failing-job
                      (:type always-exception-throwing-job) always-exception-throwing-job})

(def default-start-iteration {:state {} :step :initial :retry-count 0 :iteration-id 1})

(defn exec-all-iterations [runner job]
  (loop [iteration (:iteration job)
         result-iterations []]
    (let [result-iteration (job-exec/exec-job-step runner (assoc job :iteration iteration))
          new-results (conj result-iterations result-iteration)]
      (if (:final result-iteration)
        new-results
        (recur result-iteration new-results)))))

(describe
 "job execution"
 (tags :unit)

 (it "exec-job-step runs steps from job with manual retry and produces correct steps up until final iteration"
     (with-redefs [time/now fixed-now]
       (let [runner            {:job-definitions job-definitions
                                :fake-service    (atom {:call-count  0})}
             job               {:job-type "job1"
                                :job-id 1
                                :iteration {:state {:damn 0}
                                            :step :initial
                                            :retry-count 0}}
             result-iterations (exec-all-iterations runner job)]
         (should= expected-job1-iterations
                  result-iterations))))

 (it "exec-job-step immediately produces final transition with error description"
     (with-redefs [email-job/send-email send-email-assert-error]
     (let [runner           {:job-definitions job-definitions}
           job              {:job-type "fatally-failing-job"
                             :job-id 2
                             :iteration default-start-iteration}
           result-iterations (exec-all-iterations runner job)
           first-iteration (first result-iterations)
           caused-by-error (:caused-by-error first-iteration)]
       (should= 1 (count result-iterations))
       (should= {:step :initial,
                  :state {},
                  :final true,
                  :retry-count 0,
                  :next-activation nil,
                  :transition :fail}
                (dissoc first-iteration :caused-by-error))
       (should-contain "\"type\":\"java.lang.Error\",\"message\":\"INSTANT FATAL ISSUE\"" caused-by-error)
       (should-contain "\"trace\":" caused-by-error))))

 (it "exec-job-step retries the maximum amount when an ordinary exception is thrown from the same step"
     (with-redefs [email-job/send-email send-email-assert-max-retry]
     (let [runner          {:job-definitions job-definitions}
           job             {:job-type "always-exception-throwing-job"
                            :job-id 3
                            :iteration default-start-iteration}
           result-iterations (exec-all-iterations runner job)
           last-iteration    (last result-iterations)]
       (should= 102 ;; Limit is 100, it goes one over and the other 1 is the initial step's transition
                (count result-iterations))
       (should= {:final true,
                 :retry-count 101,
                 :next-activation nil,
                 :transition :fail,
                 :caused-by-error "Retry limit exceeded for step :throwing in job id 3 type always-exception-throwing-job"}
                (select-keys last-iteration [:final :retry-count :next-activation :transition :caused-by-error]))))))
