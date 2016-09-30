(ns ataru.virkailija.background-jobs.virkailija-jobs
  (:require [ataru.virkailija.background-jobs.example-job :as example-job]))

(def jobs {(:type example-job/job-definition) example-job/job-definition})
