(ns ataru.kk-application-payment.kk-application-payment-store-spec
  (:require [speclj.core :refer [describe tags it should-not-be-nil should= should-not before-all]]
            [ataru.kk-application-payment.kk-application-payment-store :as store]
            [ataru.db.db :as db]
            [clojure.java.jdbc :as jdbc]))

(def test-person-oid "1.2.3.4.5.6")
(def test-term-spring "kausi_k")
(def test-term-fall "kausi_s")
(def test-year 2025)
(def test-year-2 2026)
(def test-state-pending "awaiting-payment")
(def test-state-paid "payment-paid")
(def test-event-updated "state-updated")
(def test-event-comment "comment")

(defn- delete-states-and-events! []
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (jdbc/delete! conn :kk_application_payment_events [])
                            (jdbc/delete! conn :kk_application_payment_states [])))

(describe "kk application payment states"
          (tags :unit :kk-application-payment)

          (before-all
            (delete-states-and-events!))

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
                    expected-spring-state {:id spring-state-id :person-oid test-person-oid :start-term test-term-spring
                                           :start-year test-year :state test-state-pending}
                    expected-fall-state {:id fall-state-id :person-oid test-person-oid :start-term test-term-fall
                                         :start-year test-year :state test-state-paid}]
                (should= expected-spring-state (dissoc spring-state :created-time :modified-time))
                (should= expected-fall-state (dissoc fall-state :created-time :modified-time)))))

(describe "kk application payment events"
          (tags :unit :kk-application-payment)

          (before-all
            (delete-states-and-events!))

          (it "should store and retrieve payment events for payment state"
              (let [state-id (:id (store/create-or-update-kk-application-payment-state!
                                    test-person-oid test-term-spring test-year-2 test-state-pending))
                    event-1-id (store/create-kk-application-payment-event!
                              state-id test-state-paid test-event-updated nil nil)
                    event-2-id (store/create-kk-application-payment-event!
                              state-id nil test-event-comment nil "Kommentti")
                    events (store/get-kk-application-payment-events [state-id])]
                (should-not-be-nil event-1-id)
                (should-not-be-nil event-2-id)
                (should-not (= event-1-id event-2-id))
                (should= 2 (count events)))))