(ns ataru.applications.automatic-payment-obligation
  (:require [ataru.background-job.job :as job]
            [ataru.cache.cache-service :as cache]
            [ataru.db.db :as db]
            [ataru.koodisto.koodisto-codes :as codes]
            [ataru.person-service.person-service :as person-service]
            [ataru.applications.application-store :as application-store]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [clojure.java.jdbc :as jdbc]))

(defn nationality-finland? [person]
  (some #(= codes/finland-country-code (:kansalaisuusKoodi %)) (:kansalaisuus person)))

(defn- korkeakouluhaku? [tarjonta-service haku-oid]
  (clojure.string/starts-with?
   (:kohdejoukko-uri (tarjonta/get-haku tarjonta-service haku-oid))
   "haunkohdejoukko_12#"))

(defn automatic-payment-obligation-job-step
  [{:keys [person-oid]}
   {:keys [person-service tarjonta-service henkilo-cache]}]
  (cache/remove-from henkilo-cache person-oid)
  (let [person (person-service/get-person person-service person-oid)]
    (when (or (:yksiloity person)
              (:yksiloityVTJ person))
      (let [finnish-nationality? (nationality-finland? person)
            applications         (->> (application-store/get-application-keys-for-person-oid person-oid)
                                      (map :key)
                                      (map application-store/get-latest-application-by-key))]
        (doseq [application applications
                :when       (and (some? (:haku application))
                                 (korkeakouluhaku? tarjonta-service
                                                   (:haku application)))]
          (doseq [hakukohde-oid (:hakukohde application)]
            (application-store/save-payment-obligation-automatically-changed
             (:key application)
             hakukohde-oid
             "payment-obligation"
             (if finnish-nationality? "not-obligated" "unreviewed")))))))
  {:transition {:id :final}})

(defn start-automatic-payment-obligation-job
  [job-runner person-oid]
  (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
    (job/start-job job-runner
                   connection
                   "automatic-payment-obligation-job"
                   {:person-oid person-oid})))
