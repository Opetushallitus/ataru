(ns ataru.hakija.background-jobs.hakija-jobs
  (:require [ataru.applications.automatic-eligibility :as automatic-eligibility]
            [ataru.background-job.email-job :as email-job]
            [ataru.person-service.person-integration :as person-integration]
            [ataru.hakija.background-jobs.attachment-finalizer-job :as attachment-finalizer-job]))

(def job-definitions
  {(:type email-job/job-definition)                    email-job/job-definition
   person-integration/job-type                         person-integration/job-definition
   "update-person-info-job"                            {:steps {:initial person-integration/update-person-info-job-step}
                                                        :type  "update-person-info-job"}
   (:type attachment-finalizer-job/job-definition)     attachment-finalizer-job/job-definition
   "automatic-eligibility-if-ylioppilas-job"           {:steps {:initial automatic-eligibility/automatic-eligibility-if-ylioppilas-job-step}
                                                        :type  "automatic-eligibility-if-ylioppilas-job"}
   "start-automatic-eligibility-if-ylioppilas-job-job" {:steps {:initial automatic-eligibility/start-automatic-eligibility-if-ylioppilas-job-job-step}
                                                        :type  "start-automatic-eligibility-if-ylioppilas-job-job"}})
