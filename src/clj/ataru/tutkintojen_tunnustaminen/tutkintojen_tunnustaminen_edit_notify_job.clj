(ns ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-edit-notify-job
  (:require
    [ataru.cas.client :as cas]
    [ataru.applications.application-store :as application-store]
    [ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-utils :refer [get-form tutu-form?]]
    [ataru.config.url-helper :refer [resolve-url]]
    [taoensso.timbre :as log]))

(defn tutkintojen-tunnustaminen-edit-handler [{:keys [application-key]} {:keys [form-by-id-cache koodisto-cache attachment-deadline-service tutu-cas-client]}]
  (let [tutu-application (application-store/get-tutu-application application-key)
        form (get-form form-by-id-cache koodisto-cache attachment-deadline-service
                       (assoc tutu-application :form-id (:form_id tutu-application)))]
    (when (tutu-form? form)
      (let [url (resolve-url :tutu-service.hakemus-update-notification application-key)
            response (cas/cas-authenticated-get tutu-cas-client url)]
        (when (not= 200 (:status response))
          (throw (Exception. (str "Sending edit for application " application-key " to Tutu failed"))))
        (log/info (str "Sending edit notification for application " application-key " successfully sent to Tutu"))))))
