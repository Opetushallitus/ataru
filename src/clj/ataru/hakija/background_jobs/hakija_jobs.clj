(ns ataru.hakija.background-jobs.hakija-jobs
  (:require [ataru.hakija.background-jobs.example-job :as example-job]
            [ataru.person-service.person-integration :as person-integration]))

(def jobs {(:type example-job/job-definition)        example-job/job-definition
           (:type person-integration/job-definition) person-integration/job-definition})
