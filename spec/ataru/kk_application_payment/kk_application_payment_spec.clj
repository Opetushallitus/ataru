(ns ataru.kk-application-payment.kk-application-payment-spec
  (:require [ataru.fixtures.application :as application-fixtures]
            [ataru.fixtures.form :as form-fixtures]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.person-service.person-service :as person-service]
            [speclj.core :refer [describe tags it should-throw should= should-be-nil should-not-be-nil before-all around]]
            [ataru.kk-application-payment.kk-application-payment :as payment]
            [clojure.java.jdbc :as jdbc]
            [ataru.db.db :as db]
            [ataru.cache.cache-service :as cache-service]
            [ataru.kk-application-payment.fixtures :as fixtures]
            [ataru.fixtures.db.unit-test-db :as unit-test-db]
            [ataru.tarjonta-service.mock-tarjonta-service :as mock-tarjonta-service]))

(def fake-person-service (person-service/->FakePersonService))
(def fake-tarjonta-service (mock-tarjonta-service/->MockTarjontaKoutaService))

(def fake-koodisto-cache (reify cache-service/Cache
                           (get-from [_ _])
                           (get-many-from [_ _])
                           (remove-from [_ _])
                           (clear-all [_])))

(def fake-haku-cache (reify cache-service/Cache
                       (get-from [_ _]
                         [{:haku "payment-info-test-kk-haku"}])
                       (get-many-from [_ _])
                       (remove-from [_ _])
                       (clear-all [_])))

(defn- delete-states-and-events! []
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (jdbc/delete! conn :kk_application_payment_events [])
                            (jdbc/delete! conn :kk_application_payment_states [])))

(def term-spring "kausi_k")
(def term-fall "kausi_s")
(def term-error "kausi_a")
(def year-ok 2025)
(def year-error 2024)
(def state-pending "payment-pending")
(def state-not-required "payment-not-required")
(def state-paid "payment-paid")
(def event-updated "state-updated")

(defn- should-be-matching-state
  [example state]
  (should= example (dissoc state :id :created_time :modified_time)))

(defn- should-be-matching-event
  [example event]
  (should= example (dissoc event :id :created_time)))

(declare spec)

(describe "update-payment-status"
          (tags :unit :kk-application-payment)

          (before-all
            (delete-states-and-events!))

          (around [spec]
                  (with-redefs [koodisto/get-koodisto-options (fn [_ uri _ _]
                                                                (case uri
                                                                  "valtioryhmat"
                                                                  fixtures/koodisto-valtioryhmat-response))]
                    (spec)))

          (it "should throw an error when EU country codes could not be read"
              (with-redefs [koodisto/get-koodisto-options (constantly [])]
                (should-throw
                  (payment/update-payment-status fake-person-service fake-tarjonta-service
                                                 fake-koodisto-cache fake-haku-cache
                                                 "1.1.1" term-fall year-ok nil))))

          (it "should return existing paid (terminal) state without state changes"
              (let [oid "1.2.3.4.5.6"
                    linked-oid (str oid "2")                ; See FakePersonService
                    _ (payment/set-application-fee-paid linked-oid term-fall year-ok nil nil)
                    _ (payment/update-payment-status fake-person-service fake-tarjonta-service
                                                     fake-koodisto-cache fake-haku-cache
                                                     linked-oid term-fall year-ok nil)
                    state (first (payment/get-payment-states [linked-oid] term-fall year-ok))]
                (should-not-be-nil state)
                (should-be-matching-state {:person_oid linked-oid, :start_term term-fall,
                                           :start_year year-ok, :state state-paid} state)))

          (it "should set payment status for eu citizen as not required"
              (let [oid "1.2.3.4.5.7"                       ; FakePersonService returns Finnish nationality by default
                    _ (payment/update-payment-status fake-person-service fake-tarjonta-service
                                                     fake-koodisto-cache fake-haku-cache
                                                     oid term-fall year-ok nil)
                    state (first (payment/get-payment-states [oid] term-fall year-ok))]
                (should-not-be-nil state)
                (should-be-matching-state {:person_oid oid, :start_term term-fall,
                                           :start_year year-ok, :state state-not-required} state)))

          (it "should set payment status for non eu citizen without exemption as required"
              (with-redefs [payment/exempt-via-applications? (constantly false)]
                (let [oid "1.2.3.4.5.303"                       ; FakePersonService returns non-EU nationality for this one
                      _ (payment/update-payment-status fake-person-service fake-tarjonta-service
                                                       fake-koodisto-cache fake-haku-cache
                                                       oid term-fall year-ok nil)
                      state (first (payment/get-payment-states [oid] term-fall year-ok))]
                  (should-not-be-nil state)
                  (should-be-matching-state {:person_oid oid, :start_term term-fall,
                                             :start_year year-ok, :state state-pending} state))))

          (it "should set payment status for non eu citizen with exemption as not required"
              (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                            application-fixtures/application-with-hakemusmaksu-exemption
                                            nil
                                            [{:state "active"}])
              (let [oid "1.2.3.4.5.303"                       ; FakePersonService returns non-EU nationality for this one
                    _ (payment/update-payment-status fake-person-service fake-tarjonta-service
                                                     fake-koodisto-cache fake-haku-cache
                                                     oid term-fall year-ok nil)
                    state (first (payment/get-payment-states [oid] term-fall year-ok))]
                (should-not-be-nil state)
                (should-be-matching-state {:person_oid oid, :start_term term-fall,
                                           :start_year year-ok, :state state-not-required} state))))

(describe "resolve-payment-status"
          (tags :unit :kk-application-payment)

          (before-all
            (delete-states-and-events!))

          (it "should return nil when no states found"
              (let [oid "1.2.3.4.5.6"
                    state (payment/resolve-payment-status fake-person-service oid term-fall year-ok)]
                (should-be-nil state)))

          (it "should return a simple single state when no states for linked oids"
              (let [oid "1.2.3.4.5.7"
                    _ (payment/set-application-fee-required oid term-fall year-ok nil nil)
                    state (payment/resolve-payment-status fake-person-service oid term-fall year-ok)]
                (should-be-matching-state {:person_oid oid, :start_term term-fall,
                                           :start_year year-ok, :state state-pending}
                  state)))

          (it "should resolve a simple single state for linked oid"
              (let [oid "1.2.3.4.5.8"
                    linked-oid (str oid "2")                ; See FakePersonService
                    _ (payment/set-application-fee-paid linked-oid term-fall year-ok nil nil)
                    state (payment/resolve-payment-status fake-person-service oid term-fall year-ok)]
                (should-be-matching-state {:person_oid linked-oid, :start_term term-fall,
                                           :start_year year-ok, :state state-paid}
                                          state)))

          (it "should resolve a possible paid state with linked oids and conflicting states"
              (let [oid "1.2.3.4.5.9"
                    linked-oid (str oid "2")                ; See FakePersonService
                    _ (payment/set-application-fee-required oid term-fall year-ok nil nil)
                    _ (payment/set-application-fee-paid linked-oid term-fall year-ok nil nil)
                    state (payment/resolve-payment-status fake-person-service oid term-fall year-ok)]
                (should-be-matching-state {:person_oid linked-oid, :start_term term-fall,
                                           :start_year year-ok, :state state-paid}
                  state)))

          (it "should resolve a possible not required state with linked oids, conflicting states and no paid state"
              (let [oid "1.2.3.4.5.10"
                    linked-oid (str oid "2")                ; See FakePersonService
                    _ (payment/set-application-fee-required oid term-fall year-ok nil nil)
                    _ (payment/set-application-fee-not-required linked-oid term-fall year-ok nil nil)
                    state (payment/resolve-payment-status fake-person-service oid term-fall year-ok)]
                (should-be-matching-state {:person_oid linked-oid, :start_term term-fall,
                                           :start_year year-ok, :state state-not-required}
                                          state)))

          (it "should resolve a required state with linked oids whenever no paid or not required states found"
              (let [oid "1.2.3.4.5.11"
                    linked-oid (str oid "2")                ; See FakePersonService
                    _ (payment/set-application-fee-required oid term-fall year-ok nil nil)
                    _ (payment/set-application-fee-required linked-oid term-fall year-ok nil nil)
                    state (payment/resolve-payment-status fake-person-service oid term-fall year-ok)]
                (should-be-matching-state {:person_oid oid, :start_term term-fall,
                                           :start_year year-ok, :state state-pending}
                                          state))))

(defn save-and-check-single-state-and-event
  [oid term year state-func desired-state]
  (let [state-id (state-func oid term-fall year-ok nil nil)
        states   (payment/get-payment-states [oid] term year)
        events   (payment/get-payment-events [state-id])
        state    (first states)
        event    (first events)]
    (should= 1 (count states))
    (should= 1 (count events))
    (should-be-matching-state {:person_oid oid, :start_term term, :start_year year, :state desired-state} state)
    (should-be-matching-event {:kk_application_payment_state_id state-id, :new_state desired-state,
                               :event_type event-updated, :virkailija_oid nil, :message nil} event)))

(describe "application payment states"
          (tags :unit :kk-application-payment)

          (before-all
            (delete-states-and-events!))

          (describe "payment state validation"
                    (it "should not allow setting fee for spring 2025 (starts from fall 2025)"
                        (should-throw (payment/set-application-fee-required
                                        "1.2.3.4.5.6" term-spring year-ok nil nil)))

                    (it "should not allow setting fee for year earlier than 2025"
                        (should-throw (payment/set-application-fee-required
                                        "1.2.3.4.5.6" term-spring year-error nil nil)))

                    (it "should not allow setting fee for invalid term"
                        (should-throw (payment/set-application-fee-required
                                        "1.2.3.4.5.6" term-error year-ok nil nil))))

          (describe "payment state setting"
                    (it "should set and get application fee required for a person with oid"
                        (save-and-check-single-state-and-event
                          "1.2.3.4.5.6" term-fall year-ok payment/set-application-fee-required state-pending))

                    (it "should set and get application fee not required for a person with oid"
                        (save-and-check-single-state-and-event
                          "1.2.3.4.5.7" term-fall year-ok payment/set-application-fee-not-required state-not-required))

                    (it "should set and get application fee paid for a person with oid"
                        (save-and-check-single-state-and-event
                          "1.2.3.4.5.8" term-fall year-ok payment/set-application-fee-paid state-paid))))
