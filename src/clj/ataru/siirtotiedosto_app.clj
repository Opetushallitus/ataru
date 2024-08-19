(ns ataru.siirtotiedosto-app
  (:require [ataru.db.flyway-migration :as migration]
            [ataru.log.audit-log :as audit-log]
            [ataru.siirtotiedosto-service :as siirtotiedosto-service]
            [clj-time.core :as t]
            [cheshire.generate :refer [add-encoder]]
            [clojure.tools.logging :as log]
            [cheshire.core :as json]
            [ataru.config.core :refer [config]]
            [ataru.siirtotiedosto.siirtotiedosto-store :as siirtotiedosto-store]
            [clj-time.jdbc]                                 ; for java.sql.Timestamp / org.joda.time.DateTime coercion
            )
  (:import java.time.ZonedDateTime
           java.time.format.DateTimeFormatter
           (ataru.siirtotiedosto_service CommonSiirtotiedostoService)
           (fi.vm.sade.valinta.dokumenttipalvelu SiirtotiedostoPalvelu)
           (java.text SimpleDateFormat)
           (java.util Date)
           (java.util UUID))
  (:gen-class))

(add-encoder ZonedDateTime
             (fn [d json-generator]
               (.writeString
                 json-generator
                 (.format d DateTimeFormatter/ISO_OFFSET_DATE_TIME))))

(defn create-siirtotiedosto-client [config]
  (let [region (-> config :siirtotiedostot :aws-region)
        bucket (-> config :siirtotiedostot :s3-bucket)
        role-arn (-> config :siirtotiedostot :transferFileTargetRoleArn)
        client (new SiirtotiedostoPalvelu
                    region
                    bucket
                    role-arn)]
    (log/info "Created siirtotiedosto-client" client "with params" {:region   region
                                                                    :bucket   bucket
                                                                    :role-arn role-arn})
    client))


(defn siirtotiedosto-data [last-siirtotiedosto-data execution-id current-date-str]
  (let [base-data {:id execution-id :window_end current-date-str :run_start current-date-str
                   :window_start (.format
                                   (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ssZZZ")
                                   (Date. 0)) :run_end nil :success nil :info nil :error_message nil}]
    (if (not (boolean (:success last-siirtotiedosto-data)))
      (do
        (log/info "No successful siirtotiedosto data found, using base-data " base-data)
        base-data)
      (merge base-data {:window_start (:window_end last-siirtotiedosto-data)}))))

(defn update-siirtotiedosto-data [orig-data operation-results]
  (let [base-data (merge orig-data {:run_end (t/now)})]
    (if (= true (:success operation-results))
      (merge base-data {:success true :info (:info operation-results)})
      (merge base-data {:success false :error-message (:error-msg operation-results)}))))

(defn- run-migrations! []
  (let [audit-logger (audit-log/new-audit-logger "ataru-editori")]
    (migration/migrate audit-logger)))

(defn -main [& _]
  (run-migrations!)
  (let [siirtotiedosto-client (create-siirtotiedosto-client config)
        siirtotiedosto-service (CommonSiirtotiedostoService. siirtotiedosto-client)
        execution-id (str (UUID/randomUUID))
        current-datetime (.format
                           (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ssZZZ")
                           (Date.))
        last-siirtotiedosto-data (siirtotiedosto-store/get-latest-data)
        upsert-data-fn (fn [data] (siirtotiedosto-store/persist-siirtotiedosto-data data))
        new-siirtotiedosto-data (siirtotiedosto-data last-siirtotiedosto-data execution-id current-datetime)]
    (log/info (str "Launching siirtotiedosto operation " execution-id ". Previous data: " last-siirtotiedosto-data ", new data " new-siirtotiedosto-data))
    (upsert-data-fn new-siirtotiedosto-data)
    (try
      (let [siirtotiedosto-data-after-operation (->> (siirtotiedosto-service/siirtotiedosto-everything siirtotiedosto-service new-siirtotiedosto-data)
                                                     (update-siirtotiedosto-data new-siirtotiedosto-data))]
        (if (= true (:success siirtotiedosto-data-after-operation))
          (log/info (str "Created siirtotiedostot " (json/generate-string siirtotiedosto-data-after-operation)))
          (log/error (str "Siirtotiedosto operation failed: " (json/generate-string siirtotiedosto-data-after-operation))))
        (upsert-data-fn siirtotiedosto-data-after-operation))
      (catch Exception e
        (let [failed-data (-> new-siirtotiedosto-data
                              (assoc :success false)
                              (assoc :error-message (.getMessage e)))]
          (log/error "Siirtotiedosto operation failed: " e)
          (upsert-data-fn failed-data))))))