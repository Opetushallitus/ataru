(ns ataru.hakija.background-jobs.hakija-jobs
  (:require [ataru.applications.automatic-eligibility :as automatic-eligibility]
            [ataru.applications.automatic-payment-obligation :as automatic-payment-obligation]
            [ataru.background-job.email-job :as email-job]
            [ataru.person-service.person-integration :as person-integration]
            [ataru.hakija.background-jobs.attachment-finalizer-job :as attachment-finalizer-job]
            [ataru.tutkintojen-tunnustaminen :as tutkintojen-tunnustaminen])
  (:import  (java.time Duration)))

(def default-retry-strategy {:proletarian/retry-strategy-fn
                    (fn [_ _] {:retries 20
                               :delays [5000 10000 30000 60000 120000]})})

(def job-definitions
  {(:type email-job/job-definition)                    (merge email-job/job-definition
                                                              {:queue default-retry-strategy})
   person-integration/job-type                         (merge person-integration/job-definition
                                                              {:queue default-retry-strategy})
   "update-person-info-job"                            {:handler person-integration/update-person-info-job-handler
                                                        :type    "update-person-info-job"
                                                        :queue   default-retry-strategy}
   "automatic-payment-obligation-job"                  {:handler automatic-payment-obligation/automatic-payment-obligation-job-handler
                                                        :type    "automatic-payment-obligation-job"
                                                        :queue   default-retry-strategy}
   (:type attachment-finalizer-job/job-definition)     attachment-finalizer-job/job-definition
   "automatic-eligibility-if-ylioppilas-job"           {:handler automatic-eligibility/automatic-eligibility-if-ylioppilas-job-handler
                                                        :type    "automatic-eligibility-if-ylioppilas-job"
                                                        :queue   default-retry-strategy}
   "start-automatic-eligibility-if-ylioppilas-job-job" {:handler  automatic-eligibility/start-automatic-eligibility-if-ylioppilas-job-job-handler
                                                        :type     "start-automatic-eligibility-if-ylioppilas-job-job"
                                                        :schedule "0 4 * * *"
                                                        :queue    default-retry-strategy}
   "tutkintojen-tunnustaminen-submit-job"              {:handler    tutkintojen-tunnustaminen/tutkintojen-tunnustaminen-submit-job-handler
                                                        :type       "tutkintojen-tunnustaminen-submit-job"
                                                        ; 30s viive jotta liitteet ehtivät skannautua
                                                        :process-in (Duration/ofSeconds 30)
                                                        :queue      default-retry-strategy}
   "tutkintojen-tunnustaminen-edit-job"                {:handler    tutkintojen-tunnustaminen/tutkintojen-tunnustaminen-edit-job-handler
                                                        :type       "tutkintojen-tunnustaminen-edit-job"
                                                        ; 30s viive jotta liitteet ehtivät skannautua
                                                        :process-in (Duration/ofSeconds 30)
                                                        :queue      default-retry-strategy}})
