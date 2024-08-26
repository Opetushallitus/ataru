(ns ataru.ovara.siirtotiedosto-spec
  (:require [ataru.fixtures.db.browser-test-db :as test-db]
            [ataru.siirtotiedosto-service :as siirtotiedosto-service]
            [ataru.siirtotiedosto.siirtotiedosto-store :as siirtotiedosto-store]
            [speclj.core :refer :all]
            [ataru.config.core :refer [config]]
            [ataru.log.audit-log :as audit-log])
  (:import
    (ataru.siirtotiedosto_service CommonSiirtotiedostoService)
    (fi.vm.sade.valinta.dokumenttipalvelu SiirtotiedostoPalvelu)
    (fi.vm.sade.valinta.dokumenttipalvelu.dto ObjectMetadata)))

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
                                                               "var6")))]
    mock-client))

(defn create-mock-siirtotiedosto-client-that-always-fails [config error-message]
  (let [mock-client (proxy
                      [SiirtotiedostoPalvelu]
                      [(-> config :siirtotiedostot :aws-region)
                       (-> config :siirtotiedostot :s3-bucket)
                       (-> config :siirtotiedostot :transferFileTargetRoleArn)]
                      (saveSiirtotiedosto [a b c d e f g] (throw (new RuntimeException error-message))))] ;Always fails
    mock-client))

(def audit-logger (audit-log/new-dummy-audit-logger))

(describe "ovara-ajastus"
          (before-all
            (test-db/reset-test-db true)
            (Thread/sleep 1000));This seems neccessary, seems like the test below is run before the applications are persisted otherwise

          (tags :ovara :siirtotiedosto-app)
          (it "should persist a failed result when saving the siirtotiedosto fails unexpectedly"
              (let [previous-success (siirtotiedosto-store/get-latest-successful-data)
                    mocked-error-message "Surprising fail for testing purposes!"
                    siirtotiedosto-client (create-mock-siirtotiedosto-client-that-always-fails config mocked-error-message)
                    siirtotiedosto-service (CommonSiirtotiedostoService. siirtotiedosto-client)
                    result (siirtotiedosto-service/form-next-siirtotiedosto siirtotiedosto-service)]
                (should= false (:success result))
                (should= (:window-end previous-success) (:window-start result))
                (should= (:error-message result) mocked-error-message)))
          (it "should create next siirtotiedosto based on latest success"
              (let [previous-success (siirtotiedosto-store/get-latest-successful-data)
                    siirtotiedosto-client (create-mock-siirtotiedosto-client config)
                    siirtotiedosto-service (CommonSiirtotiedostoService. siirtotiedosto-client)
                    result (siirtotiedosto-service/form-next-siirtotiedosto siirtotiedosto-service)]
                (should= true (:success result))
                (should= (:window-end previous-success) (:window-start result))
                (should= (get-in result [:info :applications]) 5) ;these amounts depend on initial fixture contents
                (should= (get-in result [:info :forms]) 9))))
