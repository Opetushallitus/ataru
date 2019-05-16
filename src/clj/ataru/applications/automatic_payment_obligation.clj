(ns ataru.applications.automatic-payment-obligation
  (:require [ataru.background-job.job :as job]
            [ataru.koodisto.koodisto-codes :as codes]
            [ataru.person-service.person-service :as person-service]
            [ataru.applications.application-store :as application-store]))

(defn nationality-finland? [{:keys [kansalaisuus]}]
  (some true? (map #(= codes/finland-country-code (:kansalaisuusKoodi %)) kansalaisuus)))

(defn automatic-payment-obligation-job-step
  [{:keys [person-oid]}
   {:keys [person-service]}]
  (let [person (person-service/get-person person-service person-oid)]
    (when (:yksiloityVTJ person)
      (let [finnish-nationality? (nationality-finland? person)
            applications         (->> (application-store/get-application-keys-for-person-oid person-oid)
                                      (map :key)
                                      (map application-store/get-latest-application-by-key))]
        (doseq [application applications]
          (doseq [hakukohde-oid (:hakukohde application)]
            (application-store/save-automatic-application-hakukohde-review
              (:key application)
              hakukohde-oid
              "payment-obligation"
              (if finnish-nationality? "not-obligated" "obligated")
              nil))))))
  {:transition {:id :final}})
