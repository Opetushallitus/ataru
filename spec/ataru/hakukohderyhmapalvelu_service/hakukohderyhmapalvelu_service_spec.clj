(ns ataru.hakukohderyhmapalvelu-service.hakukohderyhmapalvelu_service_spec
  (:require [ataru.hakukohderyhmapalvelu-service.hakukohderyhmapalvelu-service :as hk-service]
            [speclj.core :refer [describe it should=]]
            [clj-http.client :as http]
            [ataru.cas.client :as cas]))


(def fake-hakukohderyhmapalvelu-response
  {:status 200 :body "[\"1.2.246.562.28.12341\",\"1.2.246.562.28.12342\"]"})

(def fake-hakukohderyhmapalvelu-settings-response
  {:status 200 :body "{\"rajaava\": true, \"max-hakukohteet\": 3, \"jos-ylioppilastutkinto-ei-muita-pohjakoulutusliitepyyntoja\": false, \"yo-amm-autom-hakukelpoisuus\": true}"})

(def expected-result
  ["1.2.246.562.28.12341" "1.2.246.562.28.12342"])

(def expected-settings-result
  {:rajaava true
   :max-hakukohteet 3
   :jos-ylioppilastutkinto-ei-muita-pohjakoulutusliitepyyntoja false
   :yo-amm-autom-hakukelpoisuus true})

(describe "HakukohderyhmapalveluService"
          (it "Return list of hakukohderyhmas for hakukohde"
              (with-redefs [http/request (constantly fake-hakukohderyhmapalvelu-response)]
                (let [cas-client (cas/new-cas-client "ataru-test")
                      hk-service-instance (hk-service/->IntegratedHakukohderyhmapalveluService cas-client)
                      result (hk-service/get-hakukohderyhma-oids-for-hakukohde hk-service-instance "1.2.3.4.5.6")]
                  (should= expected-result result))))

          (it "Return settings for hakukohderyhma"
              (with-redefs [http/request (constantly fake-hakukohderyhmapalvelu-settings-response)]
                (let [cas-client (cas/new-cas-client "ataru-test")
                      hk-service-instance (hk-service/->IntegratedHakukohderyhmapalveluService cas-client)
                      result (hk-service/get-settings-for-hakukohderyhma hk-service-instance "1.2.3.4.5.6")]
                  (should= expected-settings-result result)))))
