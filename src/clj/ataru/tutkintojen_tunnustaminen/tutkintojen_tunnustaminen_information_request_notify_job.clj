(ns ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-information-request-notify-job
  (:require
    [ataru.cas.client :as cas]
    [ataru.applications.application-store :as application-store]
    [ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-utils :refer [get-form tutu-form?]]
    [ataru.config.url-helper :refer [resolve-url]]
    [taoensso.timbre :as log]))

(defn tutkintojen-tunnustaminen-information-request-handler [{:keys [information-request]} {:keys [form-by-id-cache koodisto-cache attachment-deadline-service tutu-cas-client]}]
  (let [application-key (:application-key information-request)
        tutu-application (application-store/get-tutu-application application-key)
        form (get-form form-by-id-cache koodisto-cache attachment-deadline-service
                       (assoc tutu-application :form-id (:form_id tutu-application)))
        timestamp (:information-request-timestamp tutu-application)]
    (when (and (tutu-form? form)
               (= "information-request" (:message-type information-request))
               timestamp)
      (let [url (resolve-url :tutu-service.state-change)
            req {:hakemusOid application-key
                 :tila "information-request"
                 :timestamp timestamp
                 :submitted (:submitted tutu-application)
                 :latestVersionCreated (:created tutu-application)}
            response (cas/cas-authenticated-put tutu-cas-client url req)]
        (when (not (<= 200 (:status response) 299))
          (throw (Exception. (str "Sending information-request notification for application " application-key " to Tutu failed"))))
        (log/info (str "Information-request notification for application " application-key " successfully sent to Tutu"))))))
