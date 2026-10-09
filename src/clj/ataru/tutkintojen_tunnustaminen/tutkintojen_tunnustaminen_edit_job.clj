(ns ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-edit-job
  (:require
    [ataru.applications.application-store :as application-store]
    [ataru.cas.client :as cas]
    [ataru.config.url-helper :refer [resolve-url]]
    [ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-utils :refer [get-form tutu-form?]]
    [taoensso.timbre :as log]))

(def forwarded-application-fields [:form_id :content :created :modified :submitted :application-hakukohde-reviews :information-request-timestamp])

(defn tutkintojen-tunnustaminen-edit-handler [{:keys [application-key]} {:keys [form-by-id-cache koodisto-cache attachment-deadline-service tutu-cas-client]}]
  (let [tutu-application (application-store/get-tutu-application application-key)]
    (if (some? tutu-application))
      (when (tutu-form? (get-form form-by-id-cache koodisto-cache attachment-deadline-service
                                                                (assoc tutu-application :form-id (:form_id tutu-application))))
        (let [url (resolve-url :tutu-service.hakemus-update application-key)
              req (select-keys tutu-application forwarded-application-fields)
              response (cas/cas-authenticated-put tutu-cas-client url req)]
          (when (not (<= 200 (:status response) 299))
            (throw (Exception. (str "Sending edit message for application " application-key " to Tutu failed, status: " (:status response) ", body: " (:body response)))))
          (log/info (str "Sending edit message for application " application-key " successfully sent to Tutu"))))
      (log/warn (str "No tutu application found for application key " application-key))))
