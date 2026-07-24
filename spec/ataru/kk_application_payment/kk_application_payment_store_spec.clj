(ns ataru.kk-application-payment.kk-application-payment-store-spec
  (:require [ataru.kk-application-payment.kk-application-payment :as payment]
            [speclj.core :refer [describe tags it should-not-be-nil should= before should]]
            [ataru.kk-application-payment.kk-application-payment-store :as store]
            [ataru.fixtures.db.unit-test-db :as unit-test-db]
            [ataru.time :as time]))

(def test-application-key "1.2.3.4.5.6")
(def test-application-key-2 "1.2.3.4.5.7")
(def test-state-not-required (:not-required payment/all-states))
(def test-state-awaiting (:awaiting payment/all-states))
(def test-state-paid (:paid payment/all-states))

(describe "kk application payments"
          (tags :unit :kk-application-payment)

          (before
            (unit-test-db/nuke-kk-payment-data))

          (it "should store and retrieve due date in correct time zone"
              (let [data               (payment/set-application-fee-required "1.2.3.4.5.12" nil)
                    due-date-stored    (:due-date data)
                    due-date-generated (time/from-time-zone
                                          (time/plus (time/today-at 23 59 0)
                                                     (time/days payment/kk-application-payment-due-days))
                                          (time/time-zone-for-id "Europe/Helsinki"))]
                (should= due-date-stored due-date-generated)))

          (it "should do a roundtrip of store, retrieve and store due date without the date changing."
              (let [old-data        (payment/set-application-fee-required "1.2.3.4.5.12" nil)
                    due-date-old    (:due-date old-data)
                    new-data        (store/create-or-update-kk-application-payment!
                                      {:application-key "1.2.3.4.5.12"
                                       :state           test-state-paid
                                       :due-date        due-date-old})
                    due-date-new    (:due-date new-data)
                    new-data-fetch  (first (store/get-kk-application-payments ["1.2.3.4.5.12"]))
                    due-date-fetch  (:due-date new-data-fetch)]
                (should= due-date-old due-date-new)
                (should= due-date-old due-date-fetch)))

          ; Regression for OPHHAMA-1: set-application-fee-paid/set-application-fee-overdue forward an
          ; already-read (and thus already-DST-corrected) :due-date straight back into a resave. This
          ; proves that specific resave pattern preserves the calendar date across the spring DST
          ; transition day, i.e. that the fix holds through a full store -> retrieve -> resave -> retrieve
          ; round trip via the real database, not just in-memory.
          (it "should preserve the due date across a resave on the spring DST transition day"
              (let [application-key "1.2.3.4.5.3030"
                    created         (store/create-or-update-kk-application-payment!
                                      {:application-key application-key :state test-state-awaiting
                                       :due-date        "2025-03-30"})
                    due-date-fixed  (:due-date created)
                    resaved         (store/create-or-update-kk-application-payment!
                                      {:application-key application-key :state test-state-paid
                                       :due-date        due-date-fixed})
                    due-date-resaved   (:due-date resaved)
                    refetched          (first (store/get-kk-application-payments [application-key]))
                    due-date-refetched (:due-date refetched)]
                (should= 30 (time/day due-date-fixed))
                (should= 30 (time/day due-date-resaved))
                (should= 30 (time/day due-date-refetched))))

          (it "should do nothing to due-date if it is not set."
              (let [old-data        (payment/set-application-fee-not-required-for-exemption "1.2.3.4.5.12" nil)
                    due-date        (:due-date old-data)]
                (should= due-date nil)))

          (it "should store payment state for application key"
              (let [payment-data {:application-key test-application-key :state test-state-awaiting
                                  :due-date "2025-01-01" :total-sum "100.0" :maksut-secret "12345678ABCDEFGH"
                                  :required-at "now()"}
                    payment      (store/create-or-update-kk-application-payment! payment-data)]
                (should-not-be-nil (:id payment))
                (should-not-be-nil (:created-at payment))
                (should-not-be-nil (:modified-at payment))
                (should-not-be-nil (:required-at payment))
                (should-not-be-nil (:due-date payment))
                (should= (select-keys payment-data [:application-key :state :total-sum :maksut-secret])
                         (select-keys payment [:application-key :state :total-sum :maksut-secret]))))

          (it "should update a payment state for application key"
              (let [new-payment (store/create-or-update-kk-application-payment!
                                  {:application-key test-application-key :state test-state-awaiting})
                    updated-payment (store/create-or-update-kk-application-payment!
                                      {:application-key test-application-key :state test-state-paid})]
                (should= (:id new-payment) (:id updated-payment))
                (should= test-state-awaiting (:state new-payment))
                (should= test-state-paid (:state updated-payment))
                (should-not-be-nil (:created-at new-payment))
                (should-not-be-nil (:modified-at new-payment))
                (should-not-be-nil (:created-at updated-payment))
                (should-not-be-nil (:modified-at updated-payment))
                (should= (:created-at new-payment) (:created-at updated-payment))
                (should (or
                          (time/before? (:modified-at new-payment) (:modified-at updated-payment))
                          (time/equal?  (:modified-at new-payment) (:modified-at updated-payment))))))

          (it "should get payment states for applications"
              (let [payment-data-1 {:application-key test-application-key   :state test-state-awaiting}
                    payment-data-2 {:application-key test-application-key-2 :state test-state-paid}
                    payment-1 (store/create-or-update-kk-application-payment! payment-data-1)
                    payment-2 (store/create-or-update-kk-application-payment! payment-data-2)
                    payments (store/get-kk-application-payments [test-application-key test-application-key-2])
                    payment-get-1 (first (filter #(= test-application-key (:application-key %)) payments))
                    payment-get-2 (first (filter #(= test-application-key-2 (:application-key %)) payments))]
                (should= 2 (count payments))
                (should= payment-1 payment-get-1)
                (should= payment-2 payment-get-2)
                (should= payment-data-1 (select-keys payment-get-1 [:application-key :state]))
                (should= payment-data-2 (select-keys payment-get-2 [:application-key :state]))))

          (it "should get awaiting payments (for maksut polling)"
              (let [_ (store/create-or-update-kk-application-payment!
                        {:application-key "1.2.3.4.5.600" :state test-state-awaiting})
                    _ (store/create-or-update-kk-application-payment!
                        {:application-key "1.2.3.4.5.700" :state test-state-not-required})
                    _ (store/create-or-update-kk-application-payment!
                        {:application-key "1.2.3.4.5.800" :state test-state-paid})
                    awaiting-payments (store/get-awaiting-kk-application-payments)]
                (should= 1 (count awaiting-payments))
                (should= "1.2.3.4.5.600" (->> awaiting-payments first :application-key)))))

(describe "kk application payments history"
          (tags :unit :kk-application-payment)

          (before
            (unit-test-db/nuke-kk-payment-data))

          (it "should store and retrieve payment history for payment state"
              (let [application-key "1.2.3.4.5.6.1234"
                    not-required (store/create-or-update-kk-application-payment!
                                   {:application-key application-key :state test-state-not-required})
                    awaiting (store/create-or-update-kk-application-payment!
                               {:application-key application-key :state test-state-awaiting})
                    _ (store/create-or-update-kk-application-payment!
                        {:application-key application-key :state test-state-paid})
                    history (store/get-kk-application-payments-history [application-key])
                    not-required-history (filter #(= (:state %) test-state-not-required) history)
                    awaiting-history (filter #(= (:state %) test-state-awaiting) history)
                    paid-history     (filter #(= (:state %) test-state-paid) history)]
                (should= 2 (count history))
                (should= 1 (count not-required-history))
                (should= 1 (count awaiting-history))
                (should= 0 (count paid-history))
                (should= (:modified-at (first not-required-history)) (:modified-at not-required))
                (should= (:modified-at (first awaiting-history)) (:modified-at awaiting))
                (should (or (time/before? (:modified-at (first not-required-history))
                                          (:modified-at (first awaiting-history)))
                            (time/equal?  (:modified-at (first not-required-history))
                                          (:modified-at (first awaiting-history)))))
                (should= (:created-at (first not-required-history)) (:created-at awaiting))
                (should= (:created-at (first awaiting-history)) (:created-at not-required)))))

(describe "due-date-to-full-time-in-finnish-tz"
          (tags :unit :kk-application-payment)

          (it "should do nothing when due-date is not set"
              (should= {:foo "bar"} (store/due-date-to-full-time-in-finnish-tz {:foo "bar"})))

          (it "should set due date to 23:59 Helsinki time on a normal day"
              (let [due-date (time/date-time 2025 4 15)
                    result   (:due-date (store/due-date-to-full-time-in-finnish-tz {:due-date due-date}))]
                (should= 2025 (time/year result))
                (should= 4 (time/month result))
                (should= 15 (time/day result))
                (should= 23 (time/hour result))
                (should= 59 (time/minute result))))

          ; Finland's clocks are moved forward on the last Sunday of March (kesäaika alkaa), so this
          ; particular day only has 23 hours. Adding a 23h59m Duration (elapsed real time) to midnight
          ; used to overflow into the next calendar day (due date shown as 31.3. instead of 30.3.).
          (it "should set due date to 23:59 Helsinki time without shifting the date across the spring DST transition"
              (let [due-date (time/date-time 2025 3 30)
                    result   (:due-date (store/due-date-to-full-time-in-finnish-tz {:due-date due-date}))]
                (should= 2025 (time/year result))
                (should= 3 (time/month result))
                (should= 30 (time/day result))
                (should= 23 (time/hour result))
                (should= 59 (time/minute result))))

          ; Finland's clocks are moved back on the last Sunday of October (kesäaika päättyy), so this
          ; particular day has 25 hours. The old Duration-based calculation didn't shift the date on
          ; this transition, but did produce the wrong time of day (22:59 instead of 23:59).
          (it "should set due date to 23:59 Helsinki time without shifting the date across the autumn DST transition"
              (let [due-date (time/date-time 2025 10 26)
                    result   (:due-date (store/due-date-to-full-time-in-finnish-tz {:due-date due-date}))]
                (should= 2025 (time/year result))
                (should= 10 (time/month result))
                (should= 26 (time/day result))
                (should= 23 (time/hour result))
                (should= 59 (time/minute result)))))
