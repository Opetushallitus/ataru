(ns ataru.kk-application-payment.kk-application-payment-store-spec
  (:require [speclj.core :refer [describe tags it should-not-be-nil should= should-not]]
            [ataru.kk-application-payment.kk-application-payment-store :as store]))

(def test-person-oid "1.2.3.4.5.6")
(def test-term-spring "kausi_k")
(def test-term-fall "kausi_s")
(def test-year "2025")
(def test-state-pending "kk-application-payment-pending")
(def test-state-paid "kk-application-payment-paid")

(describe "kk application payment states"
          (tags :unit :kk-application-payment)

          (it "should store payment states for person and terms"
              (let [spring-state (store/create-or-update-kk-application-payment-state!
                                   test-person-oid test-term-spring test-year test-state-pending)
                    fall-state (store/create-or-update-kk-application-payment-state!
                                 test-person-oid test-term-fall test-year test-state-paid)]
                (should-not-be-nil (:id spring-state))
                (should-not-be-nil (:id fall-state))
                (should-not (= (:id spring-state) (:id fall-state)))))

          (it "should update a payment state for person"
              (let [new-state (store/create-or-update-kk-application-payment-state!
                                test-person-oid test-term-spring test-year test-state-pending)
                    updated-state (store/create-or-update-kk-application-payment-state!
                                    test-person-oid test-term-spring test-year test-state-paid)]
                (should= (:id new-state) (:id updated-state))))

          (it "should get payment states for person"
              (let [spring-state-id (:id (store/create-or-update-kk-application-payment-state!
                                           test-person-oid test-term-spring test-year test-state-pending))
                    fall-state-id (:id (store/create-or-update-kk-application-payment-state!
                                         test-person-oid test-term-fall test-year test-state-paid))
                    spring-state (first (store/get-kk-application-payment-states [test-person-oid] test-term-spring test-year))
                    fall-state (first (store/get-kk-application-payment-states [test-person-oid] test-term-fall test-year))
                    expected-spring-state {:id spring-state-id :person_oid test-person-oid :start_term test-term-spring
                                           :start_year test-year :state test-state-pending}
                    expected-fall-state {:id fall-state-id :person_oid test-person-oid :start_term test-term-fall
                                         :start_year test-year :state test-state-paid}]
                (should= expected-spring-state (dissoc spring-state :created_time :modified_time))
                (should= expected-fall-state (dissoc fall-state :created_time :modified_time)))))

(describe "kk application payment events"
          (tags :unit :kk-application-payment))