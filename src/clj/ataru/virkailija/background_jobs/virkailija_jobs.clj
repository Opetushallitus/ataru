(ns ataru.virkailija.background-jobs.virkailija-jobs
  (:require [ataru.information-request.information-request-job :as information-request-job]))

(def job-definitions {(:type information-request-job/job-definition) information-request-job/job-definition})
