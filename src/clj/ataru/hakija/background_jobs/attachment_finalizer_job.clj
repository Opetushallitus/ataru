(ns ataru.hakija.background-jobs.attachment-finalizer-job
  (:require [ataru.applications.application-store :as application-store]
            [ataru.cas.client :as cas]
            [ataru.config.url-helper :refer [resolve-url]]
            [taoensso.timbre :as log]))

(def origin-system "ataru")

(defn finalize-attachments [{:keys [application-id]} {:keys [liiteri-cas-client]}]
  (let [application    (application-store/get-application application-id)
        attachment-ids (->> application
                            :answers
                            (filter #(= (:fieldType %) "attachment"))
                            (map :value)
                            flatten
                            (remove nil?))
        application-key (:key application)]
    (when (> (count attachment-ids) 0)
      (let [response (cas/cas-authenticated-post liiteri-cas-client
                                                 (resolve-url :liiteri.finalize)
                                                 {:keys attachment-ids}
                                                 (fn [] {:query-params {:origin-system origin-system
                                                                       :origin-reference application-key}}))]
        (when (not= 200 (:status response))
          (throw (Exception. (str "Could not finalize attachments for application " application-id))))
        (log/info (str "Finalized attachments for application " application-id))))))

(def job-definition {:handler finalize-attachments
                     :type  (str (ns-name *ns*))})
