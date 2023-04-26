(ns ataru.background-job.email-job
  "You can send any email with this, it's not tied to any particular email-type"
  (:require [ataru.config.core :refer [config]]
            [ataru.config.url-helper :refer [resolve-url]]
            [ataru.util.http-util :as http-util]
            [cheshire.core :as json]
            [taoensso.timbre :as log]
            [clojure.string :as str]))

(defn- viestintapalvelu-address []
  (resolve-url :ryhmasahkoposti-service))

(defn send-email [from recipients subject body]
  (let [url                (viestintapalvelu-address)
        wrapped-recipients (mapv (fn [rcp] {:email rcp}) recipients)
        response           (http-util/do-post url {:headers      {"content-type" "application/json"}
                                                   :query-params {:sanitize "false"}
                                                   :body         (json/generate-string {:email     {:from    from
                                                                                                    :subject subject
                                                                                                    :isHtml  true
                                                                                                    :body    body}
                                                                                        :recipient wrapped-recipients})})]
    (when (not= 200 (:status response))
      (throw (Exception. (str "Could not send email to " (apply str recipients)))))))

(defn send-email-step [{:keys [from recipients subject body]} _]
  (throw (new RuntimeException
           "Tilapäinen virhe testausta varten"))
  {:pre [(every? #(identity %) [from recipients subject body])]}
  (log/info "Trying to send email" subject "to" recipients "via viestintäpalvelu at address" (viestintapalvelu-address))
  (send-email from recipients subject body)
  (log/info "Successfully sent email to" recipients)
  {:transition {:id :final}})

(def job-definition {:steps {:initial send-email-step}
                     :type  (str (ns-name *ns*))})
