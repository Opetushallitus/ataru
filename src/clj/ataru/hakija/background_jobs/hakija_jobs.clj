(ns ataru.hakija.background-jobs.hakija-jobs
  (:require [ataru.hakija.background-jobs.example-job :as example-job]
            [ataru.person-service.person-integration :as person-integration]))

(def job-definitions {(:type example-job/job-definition) example-job/job-definition
                      person-integration/job-type        person-integration/job-definition})
