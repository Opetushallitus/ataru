(ns ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-send-job
  (:require
    [ataru.cas.client :as cas]
    [ataru.config.url-helper :refer [resolve-url]]
    [taoensso.timbre :as log]))


(defn tutkintojen-tunnustaminen-send-handler [{:keys [key country apply-reason]} {:keys [tutu-cas-client]}]
  (let [url (resolve-url :tutu-service.hakemus)
        req {:hakemusOid    key
             :maakoodi      country
             :hakemusKoskee apply-reason}
        response (cas/cas-authenticated-post tutu-cas-client url req)]

    (log/info "Response" response)
    (when (not= 200 (:status response))
      (throw (Exception. (str "Sending application " (:application-key key) " to Tutu failed"))))
    (log/info (str "Application " (:application-key key) " successfully sent to Tutu")))
  )

(def job-definition {:handler tutkintojen-tunnustaminen-send-handler
                     :type    (-> *ns* ns-name str)})