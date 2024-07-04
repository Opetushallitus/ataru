(ns ataru.forms.form-payment-info-spec
  (:require
    [ataru.forms.form-payment-info :as payment-info]
    [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-service]
    [clj-time.core :as t]
    [speclj.core :refer [describe it should= should-be tags]]
    [speclj.core :refer [describe it should= should-be-nil tags]]
    [ataru.tarjonta-service.mock-tarjonta-service :as mts]))

(def expected-payment-info
  {:type "payment-type-kk",
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

(def test-payment-info
  {:type :payment-type-tutu :processing-fee 100 :decision-fee nil})

(def test-kk-form-with-existing-payment-info
  (merge test-kk-form {:properties test-payment-info}))

(def test-non-kk-form-with-existing-payment-info
  (merge test-non-kk-form {:properties test-payment-info}))

(describe "add-admission-payment-info-for-haku"
          (tags :unit)

          (it "sets payment needed as true with higher education"
              (let [tarjonta-service (mts/->MockTarjontaKoutaService)
                    haku {:name { :fi "Testihaku 1"}
                          :oid "payment-info-test-kk-haku"
                          :kohdejoukko-uri "haunkohdejoukko_12#1"
                          :hakukohteet ["payment-info-test-kk-hakukohde"]
                          :hakuajat [{:start (t/date-time 2025 10 14)
                                      :end   (t/date-time 2025 10 15)}]
                          :alkamiskausi "kausi_s#1"
                          :alkamisvuosi 2025}
                    haku-with-payment-flag (payment-info/add-admission-payment-info-for-haku
                                             tarjonta-service haku)]
                (should= true (:admission-payment-required? haku-with-payment-flag))))

          (it "sets payment needed as false for non higher education"
              (let [tarjonta-service (mts/->MockTarjontaKoutaService)
                    haku {:name { :fi "Testihaku 2" }
                          :oid "payment-info-test-non-kk-haku"
                          :kohdejoukko-uri "haunkohdejoukko_11#1"
                          :hakukohteet ["payment-info-test-non-kk-hakukohde"]
                          :hakuajat [{:start (t/date-time 2025 10 14)
                                      :end   (t/date-time 2025 10 15)}]
                          :alkamiskausi "kausi_s#1"
                          :alkamisvuosi 2025}
                    haku-with-payment-flag (payment-info/add-admission-payment-info-for-haku
                                             tarjonta-service haku)]
                (should= false (:admission-payment-required? haku-with-payment-flag))))

          (it "sets payment needed as false for unknown haku"
              (let [tarjonta-service (mts/->MockTarjontaKoutaService)
                    haku {:name { :fi "Testihaku 2" }
                          :oid "payment-info-test-unknown-haku"
                          :kohdejoukko-uri "haunkohdejoukko_12#1"
                          :hakukohteet ["payment-info-test-unknown-hakukohde"]
                          :hakuajat [{:start (t/date-time 2025 10 14)
                                      :end   (t/date-time 2025 10 15)}]
                          :alkamiskausi "kausi_s#1"
                          :alkamisvuosi 2025}
                    haku-with-payment-flag (payment-info/add-admission-payment-info-for-haku
                                             tarjonta-service haku)]
                (should= false (:admission-payment-required? haku-with-payment-flag))))

          (it "sets payment needed as false with higher education haku starting before 2025"
              (let [tarjonta-service (mts/->MockTarjontaKoutaService)
                    haku {:name { :fi "Testihaku 1"}
                          :oid "payment-info-test-kk-haku"
                          :kohdejoukko-uri "haunkohdejoukko_12#1"
                          :hakukohteet ["payment-info-test-kk-hakukohde"]
                          :hakuajat [{:start (t/date-time 2024 10 14)
                                      :end   (t/date-time 2025 10 15)}]
                          :alkamiskausi "kausi_s#1"
                          :alkamisvuosi 2025}
                    haku-with-payment-flag (payment-info/add-admission-payment-info-for-haku
                                             tarjonta-service haku)]
                (should= false (:admission-payment-required? haku-with-payment-flag))))

          (it "sets payment needed as false with higher education studies starting before fall 2025"
              (let [tarjonta-service (mts/->MockTarjontaKoutaService)
                    haku {:name { :fi "Testihaku 1"}
                          :oid "payment-info-test-kk-haku"
                          :kohdejoukko-uri "haunkohdejoukko_12#1"
                          :hakukohteet ["payment-info-test-kk-hakukohde"]
                          :hakuajat [{:start (t/date-time 2025 10 14)
                                      :end   (t/date-time 2025 10 15)}]
                          :alkamiskausi "kausi_k#1"
                          :alkamisvuosi 2025}
                    haku-with-payment-flag (payment-info/add-admission-payment-info-for-haku
                                             tarjonta-service haku)]
                (should= false (:admission-payment-required? haku-with-payment-flag)))))

(describe "populate-form-with-payment-info"
          (tags :unit)

          (describe "with old tarjonta-service (basic sanity checks)"
                    (it "returns existing payment info when no haku given"
                        (let [tarjonta-service (mts/->MockTarjontaService)
                              form-with-payment-info (payment-info/populate-form-with-payment-info
                                                       test-non-kk-form-with-existing-payment-info tarjonta-service nil)
                              payment-info (:properties form-with-payment-info)]
                          (should= test-payment-info payment-info)))

                    (it "sets payment info dynamically if higher education"
                        (let [tarjonta-service (mts/->MockTarjontaService)
                              form-with-payment-info (payment-info/populate-form-with-payment-info
                                                       test-kk-form
                                                       tarjonta-service
                                                       (tarjonta-service/get-haku tarjonta-service
                                                                                  "payment-info-test-kk-haku"))
                              payment-info (:properties form-with-payment-info)]
                          (should= expected-payment-info payment-info)))

                    (it "doesn't set payment info dynamically if not higher education"
                        (let [tarjonta-service (mts/->MockTarjontaService)
                              form-with-payment-info (payment-info/populate-form-with-payment-info
                                                       test-non-kk-form
                                                       tarjonta-service
                                                       (tarjonta-service/get-haku tarjonta-service
                                                                                  "payment-info-test-non-kk-haku"))
                              payment-info (:properties form-with-payment-info)]
                          (should-be empty? payment-info))))

          (describe "with kouta tarjonta-service"
                    (it "returns existing payment info when no haku given"
                        (let [tarjonta-service (mts/->MockTarjontaKoutaService)
                              form-with-payment-info (payment-info/populate-form-with-payment-info
                                                       test-non-kk-form-with-existing-payment-info
                                                       tarjonta-service
                                                       nil)
                              payment-info (:properties form-with-payment-info)]
                          (should= test-payment-info payment-info)))

                    (it "returns existing payment info when hakemusmaksu criteria is not met"
                        (let [tarjonta-service (mts/->MockTarjontaKoutaService)
                              form-with-payment-info (payment-info/populate-form-with-payment-info
                                                       test-non-kk-form-with-existing-payment-info
                                                       tarjonta-service
                                                       (tarjonta-service/get-haku tarjonta-service
                                                                                  "payment-info-test-non-kk-haku"))
                              payment-info (:properties form-with-payment-info)]
                          (should= test-payment-info payment-info)))

                    (it "sets payment info dynamically if higher education"
                        (let [tarjonta-service (mts/->MockTarjontaKoutaService)
                              form-with-payment-info (payment-info/populate-form-with-payment-info
                                                       test-kk-form
                                                       tarjonta-service
                                                       (tarjonta-service/get-haku tarjonta-service
                                                                                  "payment-info-test-kk-haku"))
                              payment-info (:properties form-with-payment-info)]
                          (should= expected-payment-info payment-info)))

                    (it "doesn't set payment info dynamically if not higher education"
                        (let [tarjonta-service (mts/->MockTarjontaKoutaService)
                              form-with-payment-info (payment-info/populate-form-with-payment-info
                                                       test-non-kk-form
                                                       tarjonta-service
                                                       (tarjonta-service/get-haku tarjonta-service
                                                                                  "payment-info-test-non-kk-haku"))
                              payment-info (:properties form-with-payment-info)]
                          (should-be empty? payment-info)))

                    (it "doesn't set payment info dynamically if not tutkintoon johtava"
                        (let [tarjonta-service (mts/->MockTarjontaKoutaService)
                              form-with-payment-info (payment-info/populate-form-with-payment-info
                                                       test-kk-no-tutkinto-form
                                                       tarjonta-service
                                                       (tarjonta-service/get-haku tarjonta-service
                                                                                  "payment-info-test-kk-no-tutkinto-haku"))
                              payment-info (:properties form-with-payment-info)]
                          (should-be empty? payment-info)))

                    (it "doesn't set payment info dynamically if non-siirtohaku"
                        (let [tarjonta-service (mts/->MockTarjontaKoutaService)
                              form-with-payment-info (payment-info/populate-form-with-payment-info
                                                       test-kk-jatko-form
                                                       tarjonta-service
                                                       (tarjonta-service/get-haku tarjonta-service
                                                                                  "payment-info-test-kk-jatko-haku"))
                              payment-info (:properties form-with-payment-info)]
                          (should-be empty? payment-info)))

                    (it "overrides payment info on the form when hakemusmaksu criteria is met"
                        (let [tarjonta-service (mts/->MockTarjontaKoutaService)
                              form-with-payment-info (payment-info/populate-form-with-payment-info
                                                       test-kk-form-with-existing-payment-info
                                                       tarjonta-service
                                                       (tarjonta-service/get-haku tarjonta-service
                                                                                  "payment-info-test-kk-haku"))
                              payment-info (:properties form-with-payment-info)]
                          (should= expected-payment-info payment-info)))))
