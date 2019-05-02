(ns ataru.applications.automatic-payment-obligation
  (:require [ataru.background-job.job :as job]
            [ataru.applications.application-store :as application-store]))

(defn automatic-payment-obligation-job-step
  [{:keys [person-oid]}
   {:keys []}]
  (let [ids (application-store/get-application-keys-by-person-oid person-oid)]
    (for [application (map application-store/get-application ids)]
      (for [hakukohde-oid (:hakukohde application)]
      (application-store/save-application-hakukohde-review
        (:key application)
        hakukohde-oid
        "payment-obligation"
        "not-obligated"
        nil)))
    {:transition {:id :final}}))