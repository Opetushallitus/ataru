(ns ataru.palaute.palaute-client
  (:require [ataru.config.core :refer [config]]
            [cheshire.core :as json]
            [ataru.aws.sqs :as sqs]
            [taoensso.timbre :as log]))

(defn- build-palaute-request
  [feedback]
  {:stars      (:rating feedback)
   :feedback   (:feedback feedback)
   :user-agent (:user-agent feedback)
   :created-at (.getTime (java.util.Date.))
   :data       {}
   :key        (if (:haku-oid feedback)
                 (str "https://" (get-in config [:urls :hakija-host]) "/hakemus/haku/" (:haku-oid feedback))
                 (str "https://" (get-in config [:urls :hakija-host]) "/hakemus/" (:form-key feedback)))})

(defn send-application-feedback
  [amazon-sqs feedback]
  (try
    (log/info "Sending feedback to Palautepalvelu" feedback)
    (sqs/send-message amazon-sqs
                      (-> config :aws :feedback-queue :queue-url)
                      (-> feedback
                          (build-palaute-request)
                          (json/generate-string)))
    (catch Exception e
      (log/warn e "Feedback didn't go through"))))

