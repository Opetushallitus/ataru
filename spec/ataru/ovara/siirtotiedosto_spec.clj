(ns ataru.ovara.siirtotiedosto-spec
  (:require [ataru.fixtures.db.browser-test-db :as test-db]
            [ataru.siirtotiedosto-service :as siirtotiedosto-service]
            [ataru.siirtotiedosto.siirtotiedosto-store :as siirtotiedosto-store]
            [speclj.core :refer :all]
            [ataru.config.core :refer [config]]
            [ataru.log.audit-log :as audit-log]
            [ataru.siirtotiedosto-app :as siirtotiedosto-app])
  (:import
    (ataru.siirtotiedosto_service CommonSiirtotiedostoService)
    (fi.vm.sade.valinta.dokumenttipalvelu SiirtotiedostoPalvelu)
    (fi.vm.sade.valinta.dokumenttipalvelu.dto ObjectMetadata)
    (java.util Date UUID)
    (java.text SimpleDateFormat)))

(defn create-mock-siirtotiedosto-client [config]
  (let [mock-client (proxy
                      [SiirtotiedostoPalvelu]
                      [(-> config :siirtotiedostot :aws-region)
                       (-> config :siirtotiedostot :s3-bucket)
                       (-> config :siirtotiedostot :transferFileTargetRoleArn)]
                      ;very raw and uninteresting override, but as long as it doesn't throw...
                      (saveSiirtotiedosto [a b c d e f g] (new ObjectMetadata
                                                               "key"
                                                               "documentId"
                                                               ["tag"]
                                                               nil
                                                               0
                                                               "var6")))
        ]
    mock-client))

(def audit-logger (audit-log/new-dummy-audit-logger))

(describe "ovara-ajastus"
          (before
            (test-db/reset-test-db true)
            (Thread/sleep 1000));This seems neccessary, seems like the test below is run before the applications are persisted otherwise

          (tags :ovara :siirtotiedosto-app)
          (it "should form siirtotiedosto and persist counts and information based on previous success"
              (let [siirtotiedosto-client (create-mock-siirtotiedosto-client config)
                    siirtotiedosto-service (CommonSiirtotiedostoService. siirtotiedosto-client)
                    pre-op-last-siirtotiedosto-data (siirtotiedosto-store/get-latest-data)
                    upsert-data-fn (fn [data] (siirtotiedosto-store/persist-siirtotiedosto-data data))
                    current-datetime (.format
                                       (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ssZZZ")
                                       (Date.))
                    execution-id (str (UUID/randomUUID))
                    new-siirtotiedosto-data (siirtotiedosto-app/siirtotiedosto-data pre-op-last-siirtotiedosto-data execution-id current-datetime)]
                (let [siirtotiedosto-data-after-operation (->> (siirtotiedosto-service/siirtotiedosto-everything siirtotiedosto-service new-siirtotiedosto-data)
                                                               (siirtotiedosto-app/update-siirtotiedosto-data new-siirtotiedosto-data))
                      dummy (upsert-data-fn siirtotiedosto-data-after-operation)
                      latest-successful-after-operation (siirtotiedosto-store/get-latest-data)];this should now be the after-operation data
                  (should= (:window_end pre-op-last-siirtotiedosto-data) (:window_start latest-successful-after-operation))
                  (should= (get-in latest-successful-after-operation [:info :applications]) 5)
                  (should= (get-in latest-successful-after-operation [:info :forms]) 9)
                  (upsert-data-fn siirtotiedosto-data-after-operation)))))
