(ns ataru.applications.automatic-payment-obligation
  (:require [ataru.background-job.job :as job]
            [ataru.koodisto.koodisto-codes :as codes]
            [ataru.applications.application-store :as application-store]))

(defn nationality-finland? [{:keys [kansalaisuus]}]
  (some true? (map #(= codes/finland-country-code %) kansalaisuus)))

(defn automatic-payment-obligation-job-step
  [{:keys [person-oid]}
   {:keys []}]
  (let [oids (->> (application-store/onr-applications person-oid)
                  (filter nationality-finland?)
                  (map :oid))]
    (for [application (map application-store/get-latest-application-by-key oids)]
      (for [hakukohde-oid (:hakukohde application)]
        (application-store/save-application-hakukohde-review
          (:key application)
          hakukohde-oid
          "payment-obligation"
          "not-obligated"
          nil
          false)))
    {:transition {:id :final}}))