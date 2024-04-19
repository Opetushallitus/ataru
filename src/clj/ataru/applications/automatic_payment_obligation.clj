(ns ataru.applications.automatic-payment-obligation
  (:require [ataru.background-job.job :as job]
            [ataru.cache.cache-service :as cache]
            [ataru.db.db :as db]
            [ataru.koodisto.koodisto-codes :as codes]
            [ataru.person-service.person-service :as person-service]
            [ataru.applications.application-store :as application-store]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [taoensso.timbre :as log]
            [clojure.java.jdbc :as jdbc]
            [clojure.string]))

(defn nationality-finland? [person]
  (some #(= codes/finland-country-code (:kansalaisuusKoodi %)) (:kansalaisuus person)))

(defn- korkeakouluhaku? [tarjonta-service haku-oid]
  (clojure.string/starts-with?
   (:kohdejoukko-uri (tarjonta/get-haku tarjonta-service haku-oid))
   "haunkohdejoukko_12#"))

(defn automatic-payment-obligation-job-handler
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
             (if finnish-nationality? "not-obligated" "unreviewed"))))))))

(defn start-automatic-payment-obligation-job
  [job-runner person-oid]
  (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
    (job/start-job job-runner
                   connection
                   "automatic-payment-obligation-job"
                   {:person-oid person-oid})))

(defn start-automatic-payment-obligation-job-for-haku
  [job-runner haku-oid]
  (log/info (str "Running automatic payment obligation job for haku " haku-oid))
  (let [person-oids  (application-store/get-application-person-oids-for-haku haku-oid)]
    (log/info (str "Found " (count person-oids) " active applications for haku " haku-oid))
    (doall
      (for [person-oid person-oids]
        (do
          (log/info (str "Starting automatic payment obligation job for person " person-oid))
          (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                                  (job/start-job job-runner
                                                 connection
                                                 "automatic-payment-obligation-job"
                                                 {:person_oid person-oid})))))))
