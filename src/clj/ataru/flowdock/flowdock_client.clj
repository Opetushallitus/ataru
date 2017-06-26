(ns ataru.flowdock.flowdock-client
  (:require
    [ataru.config.core :refer [config]]
    [org.httpkit.client :as http]
    [cheshire.core :as json]
    [taoensso.timbre :as log])
  (:import [java.util UUID]))

(def title-max-length 130)

(defn- build-flowdock-request
  [feedback flow-token]
  {:flow_token         flow-token
   :event              "activity"
   :title              (str
                         (apply str (repeat (:stars feedback) "*"))
                         " "
                         (if (< (count (:feedback feedback)) title-max-length)
                           (:feedback feedback)
                           (str (subs (:feedback feedback) 0 title-max-length) "...")))
   :external_thread_id (str (UUID/randomUUID))
   :author             {:name "anonyymi"}
   :tags               ["#palaute"]
   :thread             {:title  (str "Palaute: " (:form-name feedback))
                        :body   (:feedback feedback)
                        :fields [{:label "Rating" :value (:stars feedback)}
                                 {:label "User-Agent" :value (:user-agent feedback)}
                                 {:label "Form" :value (str "Key: " (:form-key feedback) ", ID: " (:form-id feedback))}
                                 {:label "Submitted" :value (:created-time feedback)}
                                 {:label "Feedback ID" :value (:id feedback)}]}})

(defn send-application-feedback
  [feedback]
  (when-let [token (-> config :feedback :application-feedback-flow-token)]
    (log/info "Sending feedback to Flowdock" feedback)
    @(http/post "https://api.flowdock.com/messages"
                {:headers {"content-type" "application/json"}
                 :body    (-> feedback
                              (build-flowdock-request token)
                              (json/generate-string))})))

