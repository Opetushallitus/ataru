(ns ataru.kk-application-payment.kk-application-payment-spec
  (:require [speclj.core :refer [describe tags it should-throw should= before-all]]
            [ataru.kk-application-payment.kk-application-payment :as payment]
            [clojure.java.jdbc :as jdbc]
            [ataru.db.db :as db]))

(defn- delete-states-and-events! []
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (jdbc/delete! conn :kk_application_payment_events [])
                            (jdbc/delete! conn :kk_application_payment_states [])))

(def test-term-spring "kausi_k")
(def test-term-fall "kausi_s")
(def test-term-error "kausi_a")
(def test-year-ok 2025)
(def test-year-error 2024)
(def test-state-pending "payment-pending")
(def test-state-not-required "payment-not-required")
(def test-state-paid "payment-paid")
(def test-event-updated "state-updated")

(describe "application payment states"
          (tags :unit :kk-application-payment)

          (before-all
            (delete-states-and-events!))

          (describe "payment state validation"
                    (it "should not allow setting fee for spring 2025 (starts from fall 2025)"
                        (should-throw (payment/set-application-fee-required
                                        "1.2.3.4.5.6" test-term-spring test-year-ok nil nil)))

                    (it "should not allow setting fee for year earlier than 2025"
                        (should-throw (payment/set-application-fee-required
                                        "1.2.3.4.5.6" test-term-spring test-year-error nil nil)))

                    (it "should not allow setting fee for invalid term"
                        (should-throw (payment/set-application-fee-required
                                        "1.2.3.4.5.6" test-term-error test-year-ok nil nil))))

          (describe "payment state setting"
                    (it "should set and get application fee required for a person with oid"
                        (let [oid "1.2.3.4.5.6"
                              state-id (payment/set-application-fee-required
                                         oid test-term-fall test-year-ok nil nil)
                              states (payment/get-payment-states [oid] test-term-fall test-year-ok)
                              events (payment/get-payment-events [state-id])
                              state (first states)
                              event (first events)]
                          (should= 1 (count states))
                          (should= 1 (count events))
                          (should= {:id state-id, :person_oid oid, :start_term test-term-fall,
                                    :start_year test-year-ok, :state test-state-pending}
                                   (dissoc state :created_time :modified_time))
                          (should= {:kk_application_payment_state_id state-id, :new_state test-state-pending,
                                    :event_type test-event-updated, :virkailija_oid nil, :message nil}
                                   (dissoc event :id :created_time))))

                    (it "should set and get application fee not required for a person with oid"
                        (let [oid "1.2.3.4.5.7"
                              state-id (payment/set-application-fee-not-required
                                         oid test-term-fall test-year-ok nil nil)
                              states (payment/get-payment-states [oid] test-term-fall test-year-ok)
                              events (payment/get-payment-events [state-id])
                              state (first states)
                              event (first events)]
                          (should= 1 (count states))
                          (should= 1 (count events))
                          (should= {:id state-id, :person_oid oid, :start_term test-term-fall,
                                    :start_year test-year-ok, :state test-state-not-required}
                                   (dissoc state :created_time :modified_time))
                          (should= {:kk_application_payment_state_id state-id, :new_state test-state-not-required,
                                    :event_type test-event-updated, :virkailija_oid nil, :message nil}
                                   (dissoc event :id :created_time))))

                    (it "should set and get application fee paid for a person with oid"
                        (let [oid "1.2.3.4.5.8"
                              state-id (payment/set-application-fee-paid
                                         oid test-term-fall test-year-ok nil nil)
                              states (payment/get-payment-states [oid] test-term-fall test-year-ok)
                              events (payment/get-payment-events [state-id])
                              state (first states)
                              event (first events)]
                          (should= 1 (count states))
                          (should= 1 (count events))
                          (should= {:id state-id, :person_oid oid, :start_term test-term-fall,
                                    :start_year test-year-ok, :state test-state-paid}
                                   (dissoc state :created_time :modified_time))
                          (should= {:kk_application_payment_state_id state-id, :new_state test-state-paid,
                                    :event_type test-event-updated, :virkailija_oid nil, :message nil}
                                   (dissoc event :id :created_time))))))
