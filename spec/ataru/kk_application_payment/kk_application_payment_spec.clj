(ns ataru.kk-application-payment.kk-application-payment-spec
  (:require [ataru.fixtures.application :as application-fixtures]
            [ataru.fixtures.form :as form-fixtures]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.person-service.person-service :as person-service]
            [clj-time.core :as time]
            [speclj.core :refer [describe tags it should-throw should= should-be-nil should-not-be-nil
                                 before around before-all]]
            [ataru.kk-application-payment.kk-application-payment :as payment]
            [clojure.java.jdbc :as jdbc]
            [ataru.db.db :as db]
            [ataru.cache.cache-service :as cache-service]
            [ataru.kk-application-payment.fixtures :as fixtures]
            [ataru.fixtures.db.unit-test-db :as unit-test-db]
            [ataru.tarjonta-service.mock-tarjonta-service :as mock-tarjonta-service]
            [ataru.kk-application-payment.utils :as payment-utils]))

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
(def state-pending "awaiting-payment")
(def state-not-required "payment-not-required")
(def state-paid "payment-paid")
(def state-overdue "payment-overdue")
(def state-ok-via-linked-oid "payment-ok-via-linked-oid")
(def event-updated "state-updated")

(defn- should-be-matching-state
  [example state]
  (should= example (dissoc state :id :created-time :modified-time)))

(defn- should-be-matching-event
  [example event]
  (should= example (dissoc event :id :created-time)))

(declare spec)

(describe "get-haut-for-update"
          (tags :unit :kk-application-payment)

          (it "should return haku ending in the future regardless of start date"
              ; FWIW first-application-payment-hakuaika-start redef will not be needed in tests after 1.1.2025.
              (with-redefs [payment-utils/first-application-payment-hakuaika-start (time/date-time 2024 1 1)
                            payment/get-haut-with-tarjonta-data (constantly [(fixtures/haku-with-hakuajat
                                                                               (time/date-time 2025 1 1 6)
                                                                               (time/date-time 2025 1 10 4 3 27 456))])]
                (let [haut (payment/get-haut-for-update fake-haku-cache fake-tarjonta-service)]
                  (should= 1 (count haut)))))

          (it "should return haku ending today at the end of day"
              (with-redefs [payment-utils/first-application-payment-hakuaika-start (time/date-time 2024 1 1)
                            payment/get-haut-with-tarjonta-data (constantly [(fixtures/haku-with-hakuajat
                                                                               (time/now)
                                                                               (time/today-at 23 59))])]
                (let [haut (payment/get-haut-for-update fake-haku-cache fake-tarjonta-service)]
                  (should= 1 (count haut)))))

          (it "should return haku that ended in grace days"
              (with-redefs [payment-utils/first-application-payment-hakuaika-start (time/date-time 2024 1 1)
                            payment/get-haut-with-tarjonta-data
                            (constantly [(fixtures/haku-with-hakuajat
                                           (-> (* payment-utils/haku-update-grace-days 2) time/days time/ago)
                                           (time/with-time-at-start-of-day
                                             (-> payment-utils/haku-update-grace-days time/days time/ago)))])]
                (let [haut (payment/get-haut-for-update fake-haku-cache fake-tarjonta-service)]
                  (should= 1 (count haut)))))

          (it "should not return haku that ended before grace days"
              (with-redefs [payment-utils/first-application-payment-hakuaika-start (time/date-time 2024 1 1)
                            payment/get-haut-with-tarjonta-data
                            (constantly [(fixtures/haku-with-hakuajat
                                           (-> (* payment-utils/haku-update-grace-days 2) time/days time/ago)
                                           (-> (+ payment-utils/haku-update-grace-days 1) time/days time/ago))])]
                (let [haut (payment/get-haut-for-update fake-haku-cache fake-tarjonta-service)]
                  (should= 0 (count haut))))))

(describe "update-payment-status"
          (tags :unit :kk-application-payment)

          (around [spec]
                  (with-redefs [koodisto/get-koodisto-options (fn [_ uri _ _]
                                                                (case uri
                                                                  "valtioryhmat"
                                                                  fixtures/koodisto-valtioryhmat-response))]
                    (spec)))

          (describe "without exemption"
                    (before (delete-states-and-events!))

                    (it "should return nil without any updates when the person has no applications"
                        (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                      application-fixtures/application-without-hakemusmaksu-exemption
                                                      nil)
                        (let [oid "1.2.3.4.5.1234"                       ; Should have no applications
                              resp (payment/update-payment-status fake-person-service fake-tarjonta-service
                                                                  fake-koodisto-cache fake-haku-cache
                                                                  oid term-fall year-ok nil)
                              state (first (payment/get-raw-payment-states [oid] term-fall year-ok))]
                          (should-be-nil state)
                          (should-be-nil (:old-state resp))
                          (should-be-nil (:new-state resp))
                          (should-be-nil (:id resp))))

                    (it "should throw an error when EU country codes could not be read"
                        (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                      (merge
                                                        application-fixtures/application-without-hakemusmaksu-exemption
                                                        {:person-oid "1.1.1"}) nil)
                        (with-redefs [koodisto/get-koodisto-options (constantly [])]
                          (should-throw
                            (payment/update-payment-status fake-person-service fake-tarjonta-service
                                                           fake-koodisto-cache fake-haku-cache
                                                           "1.1.1" term-fall year-ok nil))))

                    (it "should return existing paid (terminal) state without state changes"
                        (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                      (merge
                                                        application-fixtures/application-without-hakemusmaksu-exemption
                                                        {:person-oid "1.2.3.4.5.62"}) nil)
                        (let [oid "1.2.3.4.5.6"
                              linked-oid (str oid "2")                ; See FakePersonService
                              _ (payment/set-application-fee-paid linked-oid term-fall year-ok nil nil)
                              resp (payment/update-payment-status fake-person-service fake-tarjonta-service
                                                                  fake-koodisto-cache fake-haku-cache
                                                                  linked-oid term-fall year-ok nil)
                              state (first (payment/get-raw-payment-states [linked-oid] term-fall year-ok))]
                          (should-not-be-nil (:id resp))
                          (should= state-paid (:old-state resp))
                          (should= state-paid (:new-state resp))
                          (should-be-matching-state {:person-oid linked-oid, :start-term term-fall,
                                                     :start-year year-ok, :state state-paid} state)))

                    (it "should set ok via linked oid status when linked oid has payment info"
                        (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                      (merge
                                                        application-fixtures/application-without-hakemusmaksu-exemption
                                                        {:person-oid "1.2.3.4.5.123"}) nil)
                        (let [oid "1.2.3.4.5.123"
                              linked-oid (str oid "2")                ; See FakePersonService
                              _ (payment/set-application-fee-paid linked-oid term-fall year-ok nil nil)
                              resp (payment/update-payment-status fake-person-service fake-tarjonta-service
                                                                  fake-koodisto-cache fake-haku-cache
                                                                  oid term-fall year-ok nil)
                              state (first (payment/get-raw-payment-states [oid] term-fall year-ok))]
                          (should-not-be-nil (:id resp))
                          (should-be-nil (:old-state resp))
                          (should= state-ok-via-linked-oid (:new-state resp))
                          (should-be-matching-state {:person-oid oid, :start-term term-fall,
                                                     :start-year year-ok, :state state-ok-via-linked-oid} state)))

                    (it "should reset paid via linked oid status when the linking has been removed"
                        (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                      (merge
                                                        application-fixtures/application-without-hakemusmaksu-exemption
                                                        {:person-oid "1.2.3.4.5.300"}) nil)
                        (let [oid "1.2.3.4.5.300"
                              _ (payment/set-application-fee-ok-via-linked-oid oid term-fall year-ok nil nil)
                             resp (payment/update-payment-status fake-person-service fake-tarjonta-service
                                                                 fake-koodisto-cache fake-haku-cache
                                                                 oid term-fall year-ok nil)
                              state (first (payment/get-raw-payment-states [oid] term-fall year-ok))]
                          (should-not-be-nil (:id resp))
                          (should= state-ok-via-linked-oid (:old-state resp))
                          (should= state-not-required (:new-state resp))
                          (should-be-matching-state {:person-oid oid, :start-term term-fall,
                                                     :start-year year-ok, :state state-not-required} state)))

                    (it "should set payment status for eu citizen as not required"
                        (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                      (merge
                                                        application-fixtures/application-without-hakemusmaksu-exemption
                                                        {:person-oid "1.2.3.4.5.7"}) nil)
                        (let [oid "1.2.3.4.5.7"                       ; FakePersonService returns Finnish nationality by default
                              resp (payment/update-payment-status fake-person-service fake-tarjonta-service
                                                                  fake-koodisto-cache fake-haku-cache
                                                                  oid term-fall year-ok nil)
                              state (first (payment/get-raw-payment-states [oid] term-fall year-ok))]
                          (should-not-be-nil (:id resp))
                          (should-be-nil (:old-state resp))
                          (should= state-not-required (:new-state resp))
                          (should-be-matching-state {:person-oid oid, :start-term term-fall,
                                                     :start-year year-ok, :state state-not-required} state)))

                    (it "should set payment status for non eu citizen without exemption as required"
                        (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                      application-fixtures/application-without-hakemusmaksu-exemption
                                                      nil)
                        (with-redefs [payment/exemption-in-application? (constantly false)]
                          (let [oid "1.2.3.4.5.303"                       ; FakePersonService returns non-EU nationality for this one
                                resp (payment/update-payment-status fake-person-service fake-tarjonta-service
                                                                    fake-koodisto-cache fake-haku-cache
                                                                    oid term-fall year-ok nil)
                                state (first (payment/get-raw-payment-states [oid] term-fall year-ok))]
                            (should-not-be-nil (:id resp))
                            (should-be-nil (:old-state resp))
                            (should= state-pending (:new-state resp))
                            (should-be-matching-state {:person-oid oid, :start-term term-fall,
                                                       :start-year year-ok, :state state-pending} state))))

                    (it "should set payment status for non eu citizen with existing linked overdue payment as required"
                        (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                      application-fixtures/application-without-hakemusmaksu-exemption
                                                      nil)
                        (with-redefs [payment/exemption-in-application? (constantly false)]
                          (let [oid "1.2.3.4.5.303"                       ; FakePersonService returns non-EU nationality for this one
                                linked-oid (str oid "2")                ; See FakePersonService
                                _ (payment/set-application-fee-overdue linked-oid term-fall year-ok nil nil)
                                resp (payment/update-payment-status fake-person-service fake-tarjonta-service
                                                                    fake-koodisto-cache fake-haku-cache
                                                                    oid term-fall year-ok nil)
                                state (first (payment/get-raw-payment-states [oid] term-fall year-ok))]
                            (should-not-be-nil (:id resp))
                            (should-be-nil (:old-state resp))
                            (should= state-pending (:new-state resp))
                            (should-be-matching-state {:person-oid oid, :start-term term-fall,
                                                       :start-year year-ok, :state state-pending} state)))))

          (describe "with exemption"
                    (before (delete-states-and-events!))

                    (it "should set payment status for non eu citizen with exemption as not required"
                        (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                      application-fixtures/application-with-hakemusmaksu-exemption
                                                      nil
                                                      [{:state "active"}])
                        (let [oid "1.2.3.4.5.303"                       ; FakePersonService returns non-EU nationality for this one
                              resp (payment/update-payment-status fake-person-service fake-tarjonta-service
                                                                  fake-koodisto-cache fake-haku-cache
                                                                  oid term-fall year-ok nil)
                              state (first (payment/get-raw-payment-states [oid] term-fall year-ok))]
                          (should-not-be-nil (:id resp))
                          (should-be-nil (:old-state resp))
                          (should= state-not-required (:new-state resp))
                          (should-be-matching-state {:person-oid oid, :start-term term-fall,
                                                     :start-year year-ok, :state state-not-required} state)))))

(defn save-and-check-single-state-and-event
  [oid term year state-func desired-state]
  (let [state-id (state-func oid term-fall year-ok nil nil)
        states   (payment/get-raw-payment-states [oid] term year)
        events   (payment/get-raw-payment-events [state-id])
        state    (first states)
        event    (first events)]
    (should= 1 (count states))
    (should= 1 (count events))
    (should-be-matching-state {:person-oid oid, :start-term term, :start-year year, :state desired-state} state)
    (should-be-matching-event {:kk-application-payment-state-id state-id, :new-state desired-state,
                               :event-type event-updated, :virkailija-oid nil, :message nil} event)))

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
                          "1.2.3.4.5.8" term-fall year-ok payment/set-application-fee-paid state-paid))

                    (it "should set and get application fee overdue for a person with oid"
                        (save-and-check-single-state-and-event
                          "1.2.3.4.5.9" term-fall year-ok payment/set-application-fee-overdue state-overdue))))
