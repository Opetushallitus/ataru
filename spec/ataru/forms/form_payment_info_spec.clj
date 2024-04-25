(ns ataru.forms.form-payment-info-spec
  (:require
    [ataru.forms.form-payment-info :as payment-info]
    [speclj.core :refer [describe it should= should-be-nil tags]]
    [ataru.tarjonta-service.mock-tarjonta-service :as mts]))

(def expected-payment-info
  {:payment-type :payment-type-kk,
   :processing-fee (str payment-info/kk-processing-fee),
   :decision-fee nil})

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

(describe "set-payment-info-if-higher-education"
          (tags :unit)

          (describe "old tarjonta-service"
                    (it "sets payment info if higher education"
                        (let [tarjonta-service (mts/->MockTarjontaService)
                              form-with-payment-info (payment-info/set-payment-info-if-higher-education
                                                       tarjonta-service test-kk-form)]
                          (should= expected-payment-info (:properties form-with-payment-info))))

                    (it "doesn't set payment info if not higher education"
                        (let [tarjonta-service (mts/->MockTarjontaService)
                              form-with-payment-info (payment-info/set-payment-info-if-higher-education
                                                       tarjonta-service test-non-kk-form)]
                          (should-be-nil (:properties form-with-payment-info)))))

          (describe "kouta tarjonta-service"
                    (it "sets payment info if higher education"
                        (let [tarjonta-service (mts/->MockTarjontaKoutaService)
                              form-with-payment-info (payment-info/set-payment-info-if-higher-education
                                                       tarjonta-service test-kk-form)]
                          (should= expected-payment-info (:properties form-with-payment-info))))

                    (it "doesn't set payment info if not higher education"
                        (let [tarjonta-service (mts/->MockTarjontaKoutaService)
                              form-with-payment-info (payment-info/set-payment-info-if-higher-education
                                                       tarjonta-service test-non-kk-form)]
                          (should-be-nil (:properties form-with-payment-info))))

                    (it "doesn't set payment info if not tutkintoon johtava"
                        (let [tarjonta-service (mts/->MockTarjontaKoutaService)
                              form-with-payment-info (payment-info/set-payment-info-if-higher-education
                                                       tarjonta-service test-kk-no-tutkinto-form)]
                          (should-be-nil (:properties form-with-payment-info))))

                    (it "doesn't set payment info if non-siirtohaku"
                        (let [tarjonta-service (mts/->MockTarjontaKoutaService)
                              form-with-payment-info (payment-info/set-payment-info-if-higher-education
                                                       tarjonta-service test-kk-jatko-form)]
                          (should-be-nil (:properties form-with-payment-info))))))
