(ns ataru.hakukohderyhmapalvelu-service.hakukohderyhmapalvelu-service-spec
  (:require [ataru.hakukohderyhmapalvelu-service.hakukohderyhmapalvelu-service :as hk-service]
            [speclj.core :refer [describe it should=]]
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
              (with-redefs [cas/cas-authenticated-get (constantly fake-hakukohderyhmapalvelu-response)]
                (let [cas-client nil
                      hk-service-instance (hk-service/->IntegratedHakukohderyhmapalveluService cas-client)
                      result (hk-service/get-hakukohderyhma-oids-for-hakukohde hk-service-instance "1.2.3.4.5.6")]
                  (should= expected-result result))))

          (it "Return settings for hakukohderyhma"
              (with-redefs [cas/cas-authenticated-get (constantly fake-hakukohderyhmapalvelu-settings-response)]
                (let [cas-client nil
                      hk-service-instance (hk-service/->IntegratedHakukohderyhmapalveluService cas-client)
                      result (hk-service/get-settings-for-hakukohderyhma hk-service-instance "1.2.3.4.5.6")]
                  (should= expected-settings-result result)))))
