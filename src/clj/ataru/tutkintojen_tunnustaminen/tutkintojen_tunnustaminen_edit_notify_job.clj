(ns ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-edit-notify-job
  (:require
    [ataru.applications.application-store :as application-store]
    [ataru.cas.client :as cas]
    [ataru.config.url-helper :refer [resolve-url]]
    [ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-utils :refer [get-form tutu-form?]]
    [taoensso.timbre :as log]))

(def forwarded-application-fields [:form_id :content :created :modified :submitted :application-hakukohde-reviews :information-request-timestamp])

(defn tutkintojen-tunnustaminen-edit-handler [{:keys [application-key]} {:keys [form-by-id-cache koodisto-cache attachment-deadline-service tutu-cas-client]}]
  (let [tutu-application (application-store/get-tutu-application application-key)
        form (get-form form-by-id-cache koodisto-cache attachment-deadline-service
                       (assoc tutu-application :form-id (:form_id tutu-application)))]
    (when (tutu-form? form)
      (let [url (resolve-url :tutu-service.hakemus-update application-key)
            req (select-keys tutu-application forwarded-application-fields)
            response (cas/cas-authenticated-put tutu-cas-client url req)]
        (when (not (<= 200 (:status response) 299))
          (throw (Exception. (str "Sending edit notification for application " application-key " to Tutu failed"))))
        (log/info (str "Sending edit notification for application " application-key " successfully sent to Tutu"))))))
