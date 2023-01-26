(ns ataru.hakija.background-jobs.hakija-jobs
  (:require [ataru.applications.automatic-eligibility :as automatic-eligibility]
            [ataru.applications.automatic-payment-obligation :as automatic-payment-obligation]
            [ataru.background-job.email-job :as email-job]
            [ataru.person-service.person-integration :as person-integration]
            [ataru.hakija.background-jobs.attachment-finalizer-job :as attachment-finalizer-job]
            [ataru.tutkintojen-tunnustaminen :as tutkintojen-tunnustaminen]
            [ataru.harkinnanvaraisuus.harkinnanvaraisuus-job :as harkinnanvaraisuus-job]))

(def job-definitions
  {(:type harkinnanvaraisuus-job/job-definition)       harkinnanvaraisuus-job/job-definition
   (:type email-job/job-definition)                    email-job/job-definition
   person-integration/job-type                         person-integration/job-definition
   "update-person-info-job"                            {:steps {:initial person-integration/update-person-info-job-step}
                                                        :type  "update-person-info-job"}
   "automatic-payment-obligation-job"                  {:steps {:initial automatic-payment-obligation/automatic-payment-obligation-job-step}
                                                        :type  "automatic-payment-obligation-job"}
   (:type attachment-finalizer-job/job-definition)     attachment-finalizer-job/job-definition
   "automatic-eligibility-if-ylioppilas-job"           {:steps {:initial automatic-eligibility/automatic-eligibility-if-ylioppilas-job-step}
                                                        :type  "automatic-eligibility-if-ylioppilas-job"}
   "start-automatic-eligibility-if-ylioppilas-job-job" {:steps {:initial automatic-eligibility/start-automatic-eligibility-if-ylioppilas-job-job-step}
                                                        :type  "start-automatic-eligibility-if-ylioppilas-job-job"}
   "tutkintojen-tunnustaminen-submit-job"              {:steps {:initial tutkintojen-tunnustaminen/tutkintojen-tunnustaminen-submit-job-step}
                                                        :type  "tutkintojen-tunnustaminen-submit-job"}
   "tutkintojen-tunnustaminen-edit-job"                {:steps {:initial tutkintojen-tunnustaminen/tutkintojen-tunnustaminen-edit-job-step}
                                                        :type  "tutkintojen-tunnustaminen-edit-job"}})
