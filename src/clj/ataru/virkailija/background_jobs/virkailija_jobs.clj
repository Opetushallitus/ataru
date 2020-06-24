(ns ataru.virkailija.background-jobs.virkailija-jobs
  (:require [ataru.applications.automatic-eligibility :as automatic-eligibility]
            [ataru.applications.automatic-payment-obligation :as automatic-payment-obligation]
            [ataru.background-job.email-job :as email-job]
            [ataru.information-request.information-request-job :as information-request-job]
            [ataru.information-request.information-request-service :as information-request-service]
            [ataru.person-service.person-integration :as person-integration]
            [ataru.tutkintojen-tunnustaminen :as tutkintojen-tunnustaminen]
            [ataru.background-job.clean-old-forms :as clean-old-forms]))

(def job-definitions
  {(:type email-job/job-definition)                     email-job/job-definition
   (:type information-request-job/job-definition)       information-request-job/job-definition
   "automatic-eligibility-if-ylioppilas-job"            {:steps {:initial automatic-eligibility/automatic-eligibility-if-ylioppilas-job-step}
                                                         :type  "automatic-eligibility-if-ylioppilas-job"}
   "automatic-payment-obligation-job"                   {:steps {:initial automatic-payment-obligation/automatic-payment-obligation-job-step}
                                                         :type  "automatic-payment-obligation-job"}
   "mass-information-request-job"                       {:steps {:initial information-request-service/mass-information-request-job-step}
                                                         :type  "mass-information-request-job"}
   "tutkintojen-tunnustaminen-review-state-changed-job" {:steps {:initial tutkintojen-tunnustaminen/tutkintojen-tunnustaminen-review-state-changed-job-step}
                                                         :type  "tutkintojen-tunnustaminen-review-state-changed-job"}
   "update-person-info-job"                             {:steps {:initial person-integration/update-person-info-job-step}
                                                         :type  "update-person-info-job"}
   "clean-old-forms-job"                                {:steps {:initial clean-old-forms/clean-old-forms-job-step}
                                                         :type  "clean-old-forms-job"}})
