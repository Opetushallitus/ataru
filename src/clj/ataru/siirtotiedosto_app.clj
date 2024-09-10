(ns ataru.siirtotiedosto-app
  (:require [ataru.db.flyway-migration :as migration]
            [ataru.log.audit-log :as audit-log]
            [ataru.siirtotiedosto-service :as siirtotiedosto-service]
            [cheshire.generate :refer [add-encoder]]
            [clojure.tools.logging :as log]
            [ataru.config.core :refer [config]]
            [taoensso.timbre :as timbre])
  (:import java.time.ZonedDateTime
           java.time.format.DateTimeFormatter
           (ataru.siirtotiedosto_service CommonSiirtotiedostoService)
           (fi.vm.sade.valinta.dokumenttipalvelu SiirtotiedostoPalvelu))
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

(defn- run-migrations! []
  (let [audit-logger (audit-log/new-audit-logger "ataru-editori")]
    (migration/migrate audit-logger)))

(defn -main [& _]
  (run-migrations!)
  (timbre/set-min-level! :info)
  (log/info "Ovara-ataru up, creating client and service")
  (try
    (let [siirtotiedosto-client (create-siirtotiedosto-client config)
          siirtotiedosto-service (CommonSiirtotiedostoService. siirtotiedosto-client)
          result (siirtotiedosto-service/create-next-siirtotiedosto siirtotiedosto-service)]
      (log/info "Ready!" result))
    (catch Throwable t
      (log/error "Siirtotiedosto operation failed unexpectedly:" t))))