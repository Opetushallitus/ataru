(ns ataru.virkailija.background-jobs.virkailija-jobs
  (:require [ataru.applications.automatic-payment-obligation :as automatic-payment-obligation]
            [ataru.background-job.email-job :as email-job]
            [ataru.information-request.information-request-job :as information-request-job]
            [ataru.information-request.information-request-service :as information-request-service]
            [ataru.person-service.person-integration :as person-integration]
            [ataru.tutkintojen-tunnustaminen :as tutkintojen-tunnustaminen]
            [ataru.background-job.clean-old-forms :as clean-old-forms]
            [ataru.harkinnanvaraisuus.harkinnanvaraisuus-job :as harkinnanvaraisuus-job]
            [ataru.harkinnanvaraisuus.harkinnanvaraisuus-email-job :as harkinnanvaraisuus-email-job]))

(def job-definitions
  {(:type email-job/job-definition)                      email-job/job-definition
   (:type information-request-job/job-definition)        information-request-job/job-definition
   (:type harkinnanvaraisuus-job/job-definition)         harkinnanvaraisuus-job/job-definition
   (:type harkinnanvaraisuus-job/recheck-job-definition) harkinnanvaraisuus-job/recheck-job-definition
   (:type harkinnanvaraisuus-email-job/job-definition)   harkinnanvaraisuus-email-job/job-definition
   "automatic-payment-obligation-job"                    {:handler automatic-payment-obligation/automatic-payment-obligation-job-handler
                                                          :type  "automatic-payment-obligation-job"}
   "mass-information-request-job"                        {:handler information-request-service/mass-information-request-job-step
                                                          :type  "mass-information-request-job"}
   "tutkintojen-tunnustaminen-review-state-changed-job"  {:handler tutkintojen-tunnustaminen/tutkintojen-tunnustaminen-review-state-changed-job-step
                                                          :type  "tutkintojen-tunnustaminen-review-state-changed-job"}
   "tutkintojen-tunnustaminen-information-request-sent-job" {:handler tutkintojen-tunnustaminen/tutkintojen-tunnustaminen-information-request-sent-job-step
                                                             :type  "tutkintojen-tunnustaminen-information-request-sent-job"}
   "update-person-info-job" {:handler person-integration/update-person-info-job-handler
                             :type  "update-person-info-job"}
   "clean-old-forms-job" {:handler clean-old-forms/clean-old-forms-job-step
                          :type  "clean-old-forms-job"
                          :schedule "0 3 * * *"}})