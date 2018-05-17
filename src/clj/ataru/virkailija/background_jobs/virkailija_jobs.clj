(ns ataru.virkailija.background-jobs.virkailija-jobs
  (:require [ataru.background-job.email-job :as email-job]
            [ataru.information-request.information-request-job :as information-request-job]))

(def job-definitions {(:type email-job/job-definition)               email-job/job-definition
                      (:type information-request-job/job-definition) information-request-job/job-definition})
