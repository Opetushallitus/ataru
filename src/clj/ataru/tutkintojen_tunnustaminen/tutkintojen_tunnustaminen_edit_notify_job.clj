(ns ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-edit-notify-job
  (:require
    [ataru.cas.client :as cas]
    [ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-store :as tutkintojen-tunnustaminen-store]
    [ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-utils :refer [get-form tutu-form?]]
    [ataru.config.url-helper :refer [resolve-url]]
    [taoensso.timbre :as log]))

(defn tutkintojen-tunnustaminen-edit-handler [{:keys [application-id]} {:keys [form-by-id-cache koodisto-cache attachment-deadline-service tutu-cas-client]}]
  (let [tutu-application (tutkintojen-tunnustaminen-store/get-application "" application-id)
        application-key (:key tutu-application)
        form (get-form form-by-id-cache koodisto-cache attachment-deadline-service tutu-application)]
    (when (tutu-form? form)
      (let [url (resolve-url :tutu-service.hakemus-update-notification application-key)
            response (cas/cas-authenticated-get tutu-cas-client url)]
        (when (not= 200 (:status response))
          (throw (Exception. (str "Sending edit notification for application " application-key " to Tutu failed"))))
        (log/info (str "Sending edit notification for application " application-key " successfully sent to Tutu"))))))
