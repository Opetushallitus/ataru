(ns ataru.kk-application-payment.kk-application-payment-spec
  (:require [ataru.fixtures.application :as application-fixtures]
            [ataru.fixtures.form :as form-fixtures]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.person-service.person-service :as person-service]
            [clj-time.core :as time]
            [speclj.core :refer [describe tags it should-throw should= should-be-nil should-not-be-nil
                                 before around before-all focus-it]]
            [ataru.kk-application-payment.kk-application-payment :as payment]
            [clojure.java.jdbc :as jdbc]
            [ataru.db.db :as db]
            [ataru.cache.cache-service :as cache-service]
            [ataru.kk-application-payment.fixtures :as fixtures]
            [ataru.fixtures.db.unit-test-db :as unit-test-db]
            [ataru.tarjonta-service.mock-tarjonta-service :as mock-tarjonta-service]
            [ataru.kk-application-payment.utils :as payment-utils]
            [ataru.applications.application-store :as application-store]))

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

(declare conn)
(defn- delete-states-and-events! []
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (jdbc/delete! conn :kk_application_payments [])
                            (jdbc/delete! conn :kk_application_payments_history [])))

(def term-spring "kausi_k")
(def term-fall "kausi_s")
(def term-error "kausi_a")
(def year-ok 2025)
(def year-error 2024)

(def state-awaiting (:awaiting payment/all-states))
(def state-not-required (:not-required payment/all-states))
(def state-paid (:paid payment/all-states))
(def state-overdue (:overdue payment/all-states))
(def state-ok-by-proxy (:ok-by-proxy payment/all-states))

(def reason-eu-citizen (:eu-citizen payment/all-reasons))
(def reason-exemption  (:exemption-field payment/all-reasons))

(defn- should-be-matching-state
  [example state]
  (should= example (select-keys state [:application-key :state :reason])))

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
                              states (payment/update-payments-for-person-term-and-year fake-person-service fake-tarjonta-service
                                                                                       fake-koodisto-cache fake-haku-cache
                                                                                       oid term-fall year-ok)]
                          (should= 0 (count states))))

                    (it "should throw an error when EU country codes could not be read"
                        (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                      (merge
                                                        application-fixtures/application-without-hakemusmaksu-exemption
                                                        {:person-oid "1.1.1"}) nil)
                        (with-redefs [koodisto/get-koodisto-options (constantly [])]
                          (should-throw
                            (payment/update-payments-for-person-term-and-year fake-person-service fake-tarjonta-service
                                                                              fake-koodisto-cache fake-haku-cache
                                                                              "1.1.1" term-fall year-ok))))

                    (it "should return existing paid (terminal) state"
                        (let [oid "1.2.3.4.5.6"
                              application-id (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                                           (merge
                                                                             application-fixtures/application-without-hakemusmaksu-exemption
                                                                             {:person-oid oid}) nil)
                              application-key (:key (application-store/get-application application-id))
                              initial-payment (payment/set-application-fee-paid application-key nil)
                              changed (payment/update-payments-for-person-term-and-year fake-person-service fake-tarjonta-service
                                                                                        fake-koodisto-cache fake-haku-cache
                                                                                        oid term-fall year-ok)
                              payment (first (payment/get-raw-payments [application-key]))]
                          (should= 0 (count changed))
                          (should= initial-payment payment)
                          (should-be-matching-state {:application-key application-key, :state state-paid :reason nil} payment)))

                    (it "should set ok via proxy for application when another application by linked oid has been paid for"
                        (let [oid "1.2.3.4.5.303"                     ; FakePersonService returns non-EU nationality for this one
                              linked-oid (str oid "2")                ; See FakePersonService
                              application-ids (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                                            [(merge
                                                                               application-fixtures/application-without-hakemusmaksu-exemption
                                                                               {:person-oid oid})
                                                                             (merge
                                                                               application-fixtures/application-without-hakemusmaksu-exemption
                                                                               {:person-oid linked-oid})])
                              primary-application-key (:key (application-store/get-application (first application-ids)))
                              linked-application-key (:key (application-store/get-application (second application-ids)))
                              _ (payment/set-application-fee-paid linked-application-key nil)
                              changed (payment/update-payments-for-person-term-and-year fake-person-service fake-tarjonta-service
                                                                                        fake-koodisto-cache fake-haku-cache
                                                                                        oid term-fall year-ok)
                              primary-payment (first (payment/get-raw-payments [primary-application-key]))
                              linked-payment (first (payment/get-raw-payments [linked-application-key]))]
                          (should= 1 (count changed))
                          (should= primary-payment (first changed))
                          (should-be-matching-state {:application-key primary-application-key, :state state-ok-by-proxy
                                                     :reason nil}
                                                    primary-payment)
                          (should-be-matching-state {:application-key linked-application-key, :state state-paid
                                                     :reason nil}
                                                    linked-payment)))

                    (it "should reset paid via linked oid status to person's normal status when the linking has been removed"
                        (let [oid "1.2.3.4.5.303"                     ; FakePersonService returns non-EU nationality for this one
                              application-id (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                                           (merge
                                                                             application-fixtures/application-without-hakemusmaksu-exemption
                                                                             {:person-oid oid}) nil)
                              application-key (:key (application-store/get-application application-id))
                              initial-payment (payment/set-application-fee-ok-by-proxy application-key nil)
                              changed (payment/update-payments-for-person-term-and-year fake-person-service fake-tarjonta-service
                                                                                        fake-koodisto-cache fake-haku-cache
                                                                                        oid term-fall year-ok)
                              payment (first (payment/get-raw-payments [application-key]))]
                          (should= 1 (count changed))
                          (should= (first changed) payment)
                          (should-be-matching-state {:application-key application-key, :state state-ok-by-proxy
                                                     :reason nil} initial-payment)
                          (should-be-matching-state {:application-key application-key, :state state-awaiting
                                                     :reason nil} payment)))

                    (it "should set payment status for eu citizen as not required"
                        (let [oid "1.2.3.4.5.7"                       ; FakePersonService returns Finnish nationality by default
                              application-id (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                                           (merge
                                                                             application-fixtures/application-without-hakemusmaksu-exemption
                                                                             {:person-oid oid}) nil)
                              application-key (:key (application-store/get-application application-id))
                              changed (payment/update-payments-for-person-term-and-year fake-person-service fake-tarjonta-service
                                                                                        fake-koodisto-cache fake-haku-cache
                                                                                        oid term-fall year-ok)
                              payment (first (payment/get-raw-payments [application-key]))]
                          (should= 1 (count changed))
                          (should= (first changed) payment)
                          (should-be-matching-state {:application-key application-key, :state state-not-required
                                                     :reason reason-eu-citizen} payment)))

                    (it "should set payment status for non eu citizen without exemption as required"
                        (with-redefs [payment/exemption-in-application? (constantly false)]
                          (let [oid "1.2.3.4.5.303"                       ; FakePersonService returns non-EU nationality for this one
                                application-id (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                                             (merge
                                                                               application-fixtures/application-without-hakemusmaksu-exemption
                                                                               {:person-oid oid}) nil)
                                application-key (:key (application-store/get-application application-id))
                                changed (payment/update-payments-for-person-term-and-year fake-person-service fake-tarjonta-service
                                                                                          fake-koodisto-cache fake-haku-cache
                                                                                          oid term-fall year-ok)
                                payment (first (payment/get-raw-payments [application-key]))]
                            (should= 1 (count changed))
                            (should= (first changed) payment)
                            (should-be-matching-state {:application-key application-key, :state state-awaiting
                                                       :reason nil} payment))))

                    (it "should set payment status for non eu citizen with existing linked overdue payment as required"
                        (with-redefs [payment/exemption-in-application? (constantly false)]
                          (let [oid "1.2.3.4.5.303"                       ; FakePersonService returns non-EU nationality for this one
                                linked-oid (str oid "2")                  ; See FakePersonService
                                application-ids (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                                              [(merge
                                                                                 application-fixtures/application-without-hakemusmaksu-exemption
                                                                                 {:person-oid oid})
                                                                               (merge
                                                                                 application-fixtures/application-without-hakemusmaksu-exemption
                                                                                 {:person-oid linked-oid})])
                                primary-application-key (:key (application-store/get-application (first application-ids)))
                                linked-application-key (:key (application-store/get-application (second application-ids)))
                                _ (payment/set-application-fee-overdue linked-application-key nil)
                                changed (payment/update-payments-for-person-term-and-year fake-person-service fake-tarjonta-service
                                                                                          fake-koodisto-cache fake-haku-cache
                                                                                          oid term-fall year-ok)
                                primary-payment (first (payment/get-raw-payments [primary-application-key]))
                                linked-payment (first (payment/get-raw-payments [linked-application-key]))]
                            (should= 1 (count changed))
                            (should= primary-payment (first changed))
                            (should-be-matching-state {:application-key primary-application-key, :state state-awaiting
                                                       :reason nil} primary-payment)
                            (should-be-matching-state {:application-key linked-application-key, :state state-overdue
                                                       :reason nil} linked-payment)))))

          (describe "with exemption"
                    (before (delete-states-and-events!))

                    (it "should set payment status for non eu citizen with exemption as not required"
                        (let [oid "1.2.3.4.5.303"                       ; FakePersonService returns non-EU nationality for this one
                              application-id (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                                           (merge
                                                                             application-fixtures/application-with-hakemusmaksu-exemption
                                                                             {:person-oid oid}) nil)
                              application-key (:key (application-store/get-application application-id))
                              changed (payment/update-payments-for-person-term-and-year fake-person-service fake-tarjonta-service
                                                                                        fake-koodisto-cache fake-haku-cache
                                                                                        oid term-fall year-ok)
                              payment (first (payment/get-raw-payments [application-key]))]
                          (should= 1 (count changed))
                          (should= (first changed) payment)
                          (should-be-matching-state {:application-key application-key, :state state-not-required
                                                     :reason reason-exemption} payment)))))

(defn save-and-check-single-state
  [application-key state-func desired-state desired-reason]
  (let [state-data (state-func application-key nil)]
    (should= (:state state-data) desired-state)
    (should= (:reason state-data) desired-reason)))

(describe "application payment states"
          (tags :unit :kk-application-payment)

          (before-all
            (delete-states-and-events!))

          (describe "payment state validation"
                    (it "should not allow setting fee with no application key"
                        (should-throw (payment/set-application-fee-required "" nil))))

          ; TODO test with previous states
          (describe "payment state setting"
                    (it "should set and get application fee required"
                        (save-and-check-single-state
                          "1.2.3.4.5.6" payment/set-application-fee-required state-awaiting nil))

                    (it "should set and get application fee not required for eu citizen"
                        (save-and-check-single-state
                          "1.2.3.4.5.7" payment/set-application-fee-not-required-for-eu-citizen
                          state-not-required reason-eu-citizen))

                    (it "should set and get application fee not required due to exemption"
                        (save-and-check-single-state
                          "1.2.3.4.5.7" payment/set-application-fee-not-required-for-exemption
                          state-not-required reason-exemption))

                    (it "should set and get application fee paid"
                        (save-and-check-single-state
                          "1.2.3.4.5.8" payment/set-application-fee-paid state-paid nil))

                    (it "should set and get application fee overdue"
                        (save-and-check-single-state
                          "1.2.3.4.5.9" payment/set-application-fee-overdue state-overdue nil))))
