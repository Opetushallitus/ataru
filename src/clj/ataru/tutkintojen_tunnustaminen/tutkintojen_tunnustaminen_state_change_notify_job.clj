(ns ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-state-change-notify-job
  (:require
    [ataru.cas.client :as cas]
    [ataru.applications.application-store :as application-store]
    [ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-utils :refer [get-form tutu-form?]]
    [ataru.config.url-helper :refer [resolve-url]]
    [taoensso.timbre :as log]))

(defn tutkintojen-tunnustaminen-state-change-handler [{:keys [application-key]} {:keys [form-by-id-cache koodisto-cache attachment-deadline-service tutu-cas-client]}]
  (let [tutu-application (application-store/get-tutu-application application-key)
        form (get-form form-by-id-cache koodisto-cache attachment-deadline-service
                       (assoc tutu-application :form-id (:form_id tutu-application)))
        new-state (:state (first (filter #(= "processing-state" (:requirement %)) (:application-hakukohde-reviews tutu-application))))]
    (when (and (tutu-form? form)
                new-state)
      (let [url (resolve-url :tutu-service.state-change)
            req {:hakemusOid application-key
                 :tila new-state
                 :submitted (:submitted tutu-application)
                 :latestVersionCreated (:created tutu-application)}
            response (cas/cas-authenticated-put tutu-cas-client url req)]
        (when (not (<= 200 (:status response) 299))
          (throw (Exception. (str "Sending notification of state change to " new-state " for application " application-key " to Tutu failed"))))
        (log/info (str "Sending notification of state change to " new-state "for application " application-key " successfully sent to Tutu"))))))
