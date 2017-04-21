(ns ataru.hakija.background-jobs.hakija-jobs
  (:require [ataru.hakija.background-jobs.email-job :as email-job]
            [ataru.person-service.person-integration :as person-integration]
            [ataru.hakija.background-jobs.attachment-finalizer-job :as attachment-finalizer-job]))

(def job-definitions {(:type email-job/job-definition)                email-job/job-definition
                      person-integration/job-type                     person-integration/job-definition
                      (:type attachment-finalizer-job/job-definition) attachment-finalizer-job/job-definition})
