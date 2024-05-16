(ns ataru.hakija.background-jobs.hakija-jobs
  (:require [ataru.applications.automatic-eligibility :as automatic-eligibility]
            [ataru.applications.automatic-payment-obligation :as automatic-payment-obligation]
            [ataru.background-job.email-job :as email-job]
            [ataru.person-service.person-integration :as person-integration]
            [ataru.hakija.background-jobs.attachment-finalizer-job :as attachment-finalizer-job]
            [ataru.tutkintojen-tunnustaminen :as tutkintojen-tunnustaminen]))

(def job-definitions
  {(:type email-job/job-definition)                    email-job/job-definition
   person-integration/job-type                         person-integration/job-definition
   "update-person-info-job"                            {:handler person-integration/update-person-info-job-handler
                                                        :type    "update-person-info-job"}
   "automatic-payment-obligation-job"                  {:handler automatic-payment-obligation/automatic-payment-obligation-job-handler
                                                        :type    "automatic-payment-obligation-job"}
   (:type attachment-finalizer-job/job-definition)     attachment-finalizer-job/job-definition
   "automatic-eligibility-if-ylioppilas-job"           {:handler automatic-eligibility/automatic-eligibility-if-ylioppilas-job-handler
                                                        :type    "automatic-eligibility-if-ylioppilas-job"
                                                        :queue {:proletarian/retry-strategy-fn
                                                                (fn [_ _] {:retries 3
                                                                           :delays [10000]})}}
   "start-automatic-eligibility-if-ylioppilas-job-job" {:handler automatic-eligibility/start-automatic-eligibility-if-ylioppilas-job-job-handler
                                                        :type    "start-automatic-eligibility-if-ylioppilas-job-job"
                                                        :schedule "0 14 * * *"}
   "tutkintojen-tunnustaminen-submit-job"              {:handler tutkintojen-tunnustaminen/tutkintojen-tunnustaminen-submit-job-handler
                                                        :type    "tutkintojen-tunnustaminen-submit-job"}
   "tutkintojen-tunnustaminen-edit-job"                {:handler tutkintojen-tunnustaminen/tutkintojen-tunnustaminen-edit-job-handler
                                                        :type    "tutkintojen-tunnustaminen-edit-job"}})
