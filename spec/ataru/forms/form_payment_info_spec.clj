(ns ataru.forms.form-payment-info-spec
  (:require
    [ataru.forms.form-payment-info :as payment-info]
    [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-service]
    [clj-time.core :as t]
    [speclj.core :refer [describe it should= should-be tags]]
    [ataru.tarjonta-service.mock-tarjonta-service :as mts]))

(def expected-payment-info
  {:payment {:type "payment-type-kk",
             :processing-fee (str payment-info/kk-processing-fee),
             :decision-fee nil}})

(def test-kk-form
  {:key              "payment-info-test-kk-form"
   :organization-oid "1.2.246.562.10.1234334543"})

(def test-non-kk-form
  {:key              "payment-info-test-non-kk-form"
   :organization-oid "1.2.246.562.10.1234334543"})

(def test-kk-no-tutkinto-form
  {:key              "payment-info-test-kk-no-tutkinto-form"
   :organization-oid "1.2.246.562.10.1234334543"})

(def test-kk-jatko-form
  {:key              "payment-info-test-kk-jatko-form"
   :organization-oid "1.2.246.562.10.1234334543"})

(def test-payment-info
  {:payment {:type :payment-type-tutu :processing-fee 100 :decision-fee nil}})

(def test-kk-form-with-existing-payment-info
  (merge test-kk-form {:properties test-payment-info}))

(def test-non-kk-form-with-existing-payment-info
  (merge test-non-kk-form {:properties test-payment-info}))

(defn start-of-day-in-finland [year month day]
  (t/from-time-zone (t/date-time year month day)
                    (t/time-zone-for-id "Europe/Helsinki")))

(describe "populate-form-with-payment-info"
          (tags :unit)

          (describe "with kouta tarjonta-service"
                    (it "returns existing payment info when no haku given"
                        (let [tarjonta-service (mts/->MockTarjontaKoutaService)
                              form-with-payment-info (payment-info/add-payment-info-if-higher-education
                                                       test-non-kk-form-with-existing-payment-info
                                                       nil)
                              payment-info (:properties form-with-payment-info)]
                          (should= test-payment-info payment-info)))

                    (it "returns existing payment info when hakemusmaksu criteria is not met"
                        (let [tarjonta-service (mts/->MockTarjontaKoutaService)
                              form-with-payment-info (payment-info/add-payment-info-if-higher-education
                                                       test-non-kk-form-with-existing-payment-info
                                                       (tarjonta-service/get-haku tarjonta-service
                                                                                  "payment-info-test-non-kk-haku"))
                              payment-info (:properties form-with-payment-info)]
                          (should= test-payment-info payment-info)))

                    (it "sets payment info dynamically if higher education"
                        (let [tarjonta-service (mts/->MockTarjontaKoutaService)
                              form-with-payment-info (payment-info/add-payment-info-if-higher-education
                                                       test-kk-form
                                                       (tarjonta-service/get-haku tarjonta-service
                                                                                  "payment-info-test-kk-haku"))
                              payment-info (:properties form-with-payment-info)]
                          (should= expected-payment-info payment-info)))

                    (it "doesn't set payment info dynamically if not higher education"
                        (let [tarjonta-service (mts/->MockTarjontaKoutaService)
                              form-with-payment-info (payment-info/add-payment-info-if-higher-education
                                                       test-non-kk-form
                                                       (tarjonta-service/get-haku tarjonta-service
                                                                                  "payment-info-test-non-kk-haku"))
                              payment-info (:properties form-with-payment-info)]
                          (should-be empty? payment-info)))

                    (it "doesn't set payment info dynamically if not tutkintoon johtava"
                        (let [tarjonta-service (mts/->MockTarjontaKoutaService)
                              form-with-payment-info (payment-info/add-payment-info-if-higher-education
                                                       test-kk-no-tutkinto-form
                                                       (tarjonta-service/get-haku tarjonta-service
                                                                                  "payment-info-test-kk-no-tutkinto-haku"))
                              payment-info (:properties form-with-payment-info)]
                          (should-be empty? payment-info)))

                    (it "doesn't set payment info dynamically if non-siirtohaku"
                        (let [tarjonta-service (mts/->MockTarjontaKoutaService)
                              form-with-payment-info (payment-info/add-payment-info-if-higher-education
                                                       test-kk-jatko-form
                                                       (tarjonta-service/get-haku tarjonta-service
                                                                                  "payment-info-test-kk-jatko-haku"))
                              payment-info (:properties form-with-payment-info)]
                          (should-be empty? payment-info)))

                    (it "overrides payment info on the form when hakemusmaksu criteria is met"
                        (let [tarjonta-service (mts/->MockTarjontaKoutaService)
                              form-with-payment-info (payment-info/add-payment-info-if-higher-education
                                                       test-kk-form-with-existing-payment-info
                                                       (tarjonta-service/get-haku tarjonta-service
                                                                                  "payment-info-test-kk-haku"))
                              payment-info (:properties form-with-payment-info)]
                          (should= expected-payment-info payment-info)))))
