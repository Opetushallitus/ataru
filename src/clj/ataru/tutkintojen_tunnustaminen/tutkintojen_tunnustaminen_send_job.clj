(ns ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-send-job
  (:require
    ;[ataru.cas.client :as cas]
    [ataru.config.url-helper :refer [resolve-url]]
    [clojure.data.json :as json]
    [taoensso.timbre :as log]
    [clj-http.client :as client]
    ))


(defn tutkintojen-tunnustaminen-send-handler [{:keys [application-id]} {:keys [tutu-cas-client]}]
  (log/info "STARTING sending application" application-id)
  (log/info tutu-cas-client)
  (let [url (resolve-url :tutu-service.hakemus)
        req {:hakemusOid application-id}
        ;TODO: Switch to tutu-cas-client
        ;response (cas/cas-authenticated-post tutu-cas-client url req)
        response (client/post url {:as           :auto
                                   :coerce       :always
                                   :content-type :application/json
                                   :body         (json/write-str req)})
        ]
    (log/info "Response" response)
    (when (not= 200 (:status response))
      (throw (Exception. (str "Sending application " application-id " to Tutu failed"))))
    (log/info (str "Application " application-id " successfully sent to Tutu"))))

(def job-definition {:handler tutkintojen-tunnustaminen-send-handler
                     :type    (-> *ns* ns-name str)})