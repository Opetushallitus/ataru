(ns ataru.background-job.example-job
  (:require
   [taoensso.timbre :as log]))


(defn initial [state]
  {:transition {:id    :to-next
                :step  :log-hello}
   :updated-state (assoc state :initialized true)})

(defn log-hello [state]
  (log/info (str "Hello " (:name state) "from example job!"))
  {:transition {:id :final}
   :updated-state state})

(def steps {:initial   initial
            :log-hello log-hello})

(def job-definition {:steps steps
                     :id    (ns-name *ns*)})
