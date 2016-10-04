(ns ataru.virkailija.background-jobs.example-job
  (:require
   [taoensso.timbre :as log]))

(defn initial [state _]
  {:transition {:id    :to-next
                :step  :log-hello}
   :updated-state {:initialized true :hello-log-count 0}})

(defn log-hello [state _]
  (log/info (str "Hello " (:name state) "from example job!"))
  (if (> (:hello-log-count state) 0)
    {:transition {:id :final}}
    {:transition {:id :retry}
     :updated-state (assoc state :hello-log-count (inc (:hello-log-count state)))}))

(def steps {:initial   initial
            :log-hello log-hello})

(def job-definition {:steps steps
                     :type    (str (ns-name *ns*))})
