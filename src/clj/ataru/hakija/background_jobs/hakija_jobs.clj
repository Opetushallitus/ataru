(ns ataru.hakija.background-jobs.hakija-jobs
  (:require [ataru.hakija.background-jobs.example-job :as example-job]))

(def jobs {(:type example-job/job-definition) example-job/job-definition})
