(ns ataru.hakija.background-jobs.attachment-finalizer-job
  (:require
    [ataru.config.url-helper :refer [resolve-url]]
    [taoensso.timbre :as log]
    [org.httpkit.client :as http]
    [cheshire.core :as json]
    [ataru.config.core :refer [config]]
    [ataru.applications.application-store :as application-store]))

(defn finalize-attachments [{:keys [application-id]} _]
  (let [application    (application-store/get-application application-id)
        attachment-ids (->> application
                            :answers
                            (filter #(= (:fieldType %) "attachment"))
                            (mapcat :value))
        response       @(http/post (resolve-url :liiteri.finalize)
                                   {:headers {"Content-Type" "application/json"}
                                    :body    (json/generate-string {:keys attachment-ids})})]
    (when (not= 200 (:status response))
      (throw (Exception. (str "Could not finalize attachments for application " application-id))))
    (log/info (str "Finalized attachments for application " application-id))
    {:transition {:id :final}}))

(def job-definition {:steps {:initial finalize-attachments}
                     :type  (str (ns-name *ns*))})
