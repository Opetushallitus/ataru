(ns ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-send-job
  (:require [ataru.cas.client :as cas]
            [ataru.config.url-helper :refer [resolve-url]]
            [taoensso.timbre :as log]))


(defn handle-send [{:keys [application-id]} {:keys [tutu-cas-client]}]
  (log/info "STARTING sending application" application-id)
  (let [response (cas/cas-authenticated-post tutu-cas-client
                                             (resolve-url :tutu-service.hakemus)
                                             {:hakemusOid application-id})]
    (log/info response)
    (when (not= 200 (:status response))
      (throw (Exception. (str "Sending application " application-id " to Tutu failed"))))
    (log/info (str "Application " application-id " successfully sent to Tutu"))))



(defn tutkintojen-tunnustaminen-send-handler [{:keys [application-id]} job-runner]
  (log/info "Handling send for application" application-id)
  (try (handle-send application-id job-runner)
       (catch Exception e (log/error e (str "Error while sending application " application-id " to Tutu")))))


(def job-definition {:handler tutkintojen-tunnustaminen-send-handler
                     :type    (-> *ns* ns-name str)
                     :schedule "* * * * *"})