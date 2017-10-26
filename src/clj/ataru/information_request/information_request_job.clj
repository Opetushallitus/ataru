(ns ataru.information-request.information-request-job
  (:require [ataru.background-job.email-job :as email-job]))

(def job-definition {:steps {:initial email-job/send-email-step}
                     :type  (-> *ns* ns-name str)})
