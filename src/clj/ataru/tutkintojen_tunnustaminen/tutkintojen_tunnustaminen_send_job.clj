(ns ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-send-job
  (:require
    [ataru.cas.client :as cas]
    [ataru.config.url-helper :refer [resolve-url]]
    [ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-utils :refer [apply-reason-lopullinen-paatos]]
    [taoensso.timbre :as log]))


(defn tutkintojen-tunnustaminen-send-handler [{:keys [key apply-reason]} {:keys [tutu-cas-client]}]
  (let [url (resolve-url :tutu-service.hakemus)
        req {:hakemusOid    key
             :hakemusKoskee (or apply-reason apply-reason-lopullinen-paatos)} ;; Default to Lopullinen päätös if apply-reason is nil
        response (cas/cas-authenticated-post tutu-cas-client url req)
        status (:status response)]

    (log/info "Response" response)
    (when (and (not= 200 status)(not= 204 status))
      (throw (Exception. (str "Sending application " (:application-key key) " to Tutu failed"))))
    (log/info (str "Application " (:application-key key) " successfully sent to Tutu")))
  )

(def job-definition {:handler tutkintojen-tunnustaminen-send-handler
                     :type    (-> *ns* ns-name str)})