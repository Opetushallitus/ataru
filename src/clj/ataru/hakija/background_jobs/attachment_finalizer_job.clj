(ns ataru.hakija.background-jobs.attachment-finalizer-job
  (:require
    [ataru.config.url-helper :refer [resolve-url]]
    [taoensso.timbre :as log]
    [org.httpkit.client :as http]
    [cheshire.core :as json]
    [ataru.config.core :refer [config]]
    [ataru.applications.application-store :as application-store]))

(defn finalize-attachments [{:keys [application-id]} _]
  (log/info "Finalize attachments")
  (let [application (application-store/get-application application-id)]
    (log/info application)
    {:transition {:id :final}}))

(def job-definition {:steps {:initial finalize-attachments}
                     :type  (str (ns-name *ns*))})
