(ns ataru.hakija.background-jobs.email-job
  "You can send any email with this, it's not tied to any particular email-type"
  (:require
    [ataru.config.url-helper :refer [resolve-url]]
    [taoensso.timbre :as log]
    [org.httpkit.client :as http]
    [cheshire.core :as json]
    [ataru.config.core :refer [config]]))

(defn- viestintapalvelu-address []
  (resolve-url :email-service))

(defn- send-email [from recipients subject body]
  (let [url                 (viestintapalvelu-address)
        wrapped-recipients  (mapv (fn [rcp] {:email rcp}) recipients)
        response            @(http/post url {:headers {"content-type" "application/json"}
                                             :body (json/generate-string {:email {:from from
                                                                                  :subject subject
                                                                                  :isHtml true
                                                                                  :body body}
                                                                          :recipient wrapped-recipients})})]
    (if (not= 200 (:status response))
      (throw (Exception. (str "Could not send email to " (apply str recipients)))))))

(defn send-email-step [{:keys [from recipients subject body]} _]
  {:pre [(every? #(identity %) [from recipients subject body])]}
  (log/info "Trying to send email" subject "to" recipients "via viestintäpalvelu at address" (viestintapalvelu-address))
  (send-email from recipients subject body)
  (log/info "Successfully sent email to" recipients)
  {:transition {:id :final}})

(def job-definition {:steps {:initial send-email-step}
                     :type  (str (ns-name *ns*))})
