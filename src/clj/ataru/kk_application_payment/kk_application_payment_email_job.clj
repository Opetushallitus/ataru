(ns ataru.kk-application-payment.kk-application-payment-email-job
  (:require [ataru.background-job.email-job :as email-job]))

(def job-definition {:handler email-job/send-email-handler
                     :type  (-> *ns* ns-name str)})
