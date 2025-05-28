(ns ataru.virkailija.background-jobs.virkailija-jobs
  (:require [ataru.applications.automatic-payment-obligation :as automatic-payment-obligation]
            [ataru.background-job.email-job :as email-job]
            [ataru.information-request.information-request-job :as information-request-job]
            [ataru.information-request.information-request-service :as information-request-service]
            [ataru.information-request.information-request-reminder-job :as information-request-reminder-job]
            [ataru.person-service.person-integration :as person-integration]
            [ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-service :as tutkintojen-tunnustaminen-service]
            [ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-send-job :as tutkintojen-tunnustaminen-send-job]
            [ataru.background-job.clean-old-forms :as clean-old-forms]
            [ataru.harkinnanvaraisuus.harkinnanvaraisuus-job :as harkinnanvaraisuus-job]
            [ataru.harkinnanvaraisuus.harkinnanvaraisuus-email-job :as harkinnanvaraisuus-email-job]
            [ataru.background-job.job :refer [report-job cleanup-job]]
            [ataru.kk-application-payment.kk-application-payment-status-updater-job :as kk-updater-job]
            [ataru.kk-application-payment.kk-application-payment-maksut-poller-job :as kk-maksut-poller-job]
            [ataru.kk-application-payment.kk-application-payment-email-job :as kk-email-job]
            [ataru.kk-application-payment.kk-application-payment-module-job :as kk-payment-module-job]))

(def default-retry-strategy {:proletarian/retry-strategy-fn
                    (fn [_ _] {:retries 20
                               :delays [5000 10000 30000 60000 120000]})})

(def job-definitions
  {(:type email-job/job-definition)                      (merge email-job/job-definition
                                                                {:queue default-retry-strategy})
   (:type information-request-job/job-definition)        (merge information-request-job/job-definition
                                                                {:queue default-retry-strategy})
   (:type information-request-reminder-job/job-definition) (merge information-request-reminder-job/job-definition
                                                                  {:queue default-retry-strategy})
   (:type harkinnanvaraisuus-job/job-definition)         (merge harkinnanvaraisuus-job/job-definition
                                                                {:queue default-retry-strategy})
   (:type harkinnanvaraisuus-job/recheck-job-definition) (merge harkinnanvaraisuus-job/recheck-job-definition
                                                                {:queue default-retry-strategy})
   (:type harkinnanvaraisuus-email-job/job-definition)   (merge harkinnanvaraisuus-email-job/job-definition
                                                                {:queue default-retry-strategy})
   (:type kk-maksut-poller-job/job-definition)           (merge kk-maksut-poller-job/job-definition
                                                                {:queue default-retry-strategy})
   (:type kk-updater-job/scheduler-job-definition)       (merge kk-updater-job/scheduler-job-definition
                                                                {:queue default-retry-strategy})
   (:type kk-updater-job/updater-job-definition)         (merge kk-updater-job/updater-job-definition
                                                                {:queue default-retry-strategy})
   (:type kk-updater-job/periodical-updater-job-definition) (merge kk-updater-job/periodical-updater-job-definition
                                                                   {:queue default-retry-strategy})
   (:type kk-payment-module-job/job-definition)          (merge kk-payment-module-job/job-definition
                                                                {:queue default-retry-strategy})
   (:type kk-email-job/job-definition)                   (merge kk-email-job/job-definition
                                                                {:queue default-retry-strategy})
   (:type tutkintojen-tunnustaminen-send-job/job-definition) (merge tutkintojen-tunnustaminen-send-job/job-definition
                                                                {:queue default-retry-strategy})
   "automatic-payment-obligation-job"                    {:handler automatic-payment-obligation/automatic-payment-obligation-job-handler
                                                          :type    "automatic-payment-obligation-job"
                                                          :queue   default-retry-strategy}
   "mass-information-request-job"                        {:handler information-request-service/mass-information-request-job-step
                                                          :type    "mass-information-request-job"
                                                          :queue   default-retry-strategy}
   "tutkintojen-tunnustaminen-review-state-changed-job"  {:handler tutkintojen-tunnustaminen-service/tutkintojen-tunnustaminen-review-state-changed-job-step
                                                          :type    "tutkintojen-tunnustaminen-review-state-changed-job"
                                                          :queue   default-retry-strategy}
   "tutkintojen-tunnustaminen-information-request-sent-job" {:handler tutkintojen-tunnustaminen-service/tutkintojen-tunnustaminen-information-request-sent-job-step
                                                             :type    "tutkintojen-tunnustaminen-information-request-sent-job"
                                                             :queue   default-retry-strategy}
   "update-person-info-job" {:handler person-integration/update-person-info-job-handler
                             :type    "update-person-info-job"
                             :queue   default-retry-strategy}
   "clean-old-forms-job" {:handler  clean-old-forms/clean-old-forms-job-step
                          :type     "clean-old-forms-job"
                          :schedule "0 3 * * *"
                          :queue    default-retry-strategy}
   (:type report-job) report-job
   (:type cleanup-job) cleanup-job})