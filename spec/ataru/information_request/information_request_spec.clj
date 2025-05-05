(ns ataru.information-request.information-request-spec
  (:require [ataru.db.db :as db]
            [clj-time.core :as c]
            [clojure.java.jdbc :as jdbc]
            [ataru.fixtures.db.unit-test-db :as unit-test-db]
            [ataru.fixtures.form :refer [minimal-form]]
            [ataru.background-job.job :as job]
            [com.stuartsierra.component :as component]
            [ataru.config.core :refer [config]]
            [ataru.tarjonta-service.mock-tarjonta-service :as tarjonta-service]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.ohjausparametrit.ohjausparametrit-service :as ohjausparametrit-service]
            [ataru.applications.application-store :as application-store]
            [ataru.cache.cache-service :as cache-service]
            [ataru.information-request.fixtures :refer [information-requests-to-remind]]
            [ataru.information-request.information-request-store :as ir-store]
            [ataru.information-request.information-request-service :as ir-service]
            [ataru.information-request.information-request-reminder-job :refer [handler]]
            [ataru.log.audit-log :as audit-log]
            [speclj.core :refer [describe tags it should should= before-all after around with-stubs stub should-have-invoked should-not-have-invoked]]
            [yesql.core :as sql])
  (:import (org.joda.time DateTime)))

(declare conn)
(declare spec)

(defn- clear! []
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (jdbc/delete! conn :information_requests [])))

(defn start-runner-job [_ _ _ _])

(def fake-tarjonta-service (tarjonta-service/->MockTarjontaKoutaService))
(def fake-organization-service (organization-service/->FakeOrganizationService))
(def fake-ohjausparametrit-service (ohjausparametrit-service/new-ohjausparametrit-service))
(def fake-koodisto-cache (reify cache-service/Cache
                                (get-from [_ _])
                                (get-many-from [_ _])
                                (remove-from [_ _])
                                (clear-all [_])))

(defrecord FakeJobRunner []
  component/Lifecycle

  job/JobRunner
  (start-job [this conn job-type initial-state]
    (start-runner-job this conn job-type initial-state)))

(def runner
  (map->FakeJobRunner {:tarjonta-service         fake-tarjonta-service
                       :organization-service     fake-organization-service
                       :ohjausparametrit-service fake-ohjausparametrit-service
                       :koodisto-cache           fake-koodisto-cache}))

(declare yesql-upsert-virkailija<!)
(sql/defqueries "sql/virkailija-queries.sql")

(def application-key (atom nil))

(describe "information-request"
          (tags :unit :information-request)
          (with-stubs)

          (before-all
            (db/exec :db yesql-upsert-virkailija<! {:oid        "1.2.246.562.11.11111111111"
                                                    :first_name "Testi"
                                                    :last_name  "Täydennyspyytäjä"})
            (let [id (unit-test-db/init-db-fixture
                       minimal-form
                       {:form       (:id minimal-form),
                        :lang       "fi"
                        :person-oid "1.2.3.4.5.6"
                        :answers [{:key "first-name" :value "Aku Petteri" :fieldType "textField" :label {:fi "Etunimet" :sv "Förnamn"}}]}
                       nil)]
              (reset! application-key (:key (application-store/get-application id)))))

          (after
            (clear!))

          (around [spec]
                  (with-redefs [audit-log/log (fn [_ _])]
                    (spec)))

          (describe "service"
                    (it "should calculate correct reminder send time"
                        (let [information-request {:subject         "Täydennyspyyntö"
                                                   :recipient-target "hakija"
                                                   :message         "Täydennyspyyntö viesti"
                                                   :application-key @application-key
                                                   :add-update-link true
                                                   :send-reminder? true
                                                   :reminder-days 12
                                                   :message-type "information-request"}
                              send-reminder-time (:send-reminder-time
                                                   (ir-service/store
                                                     {:identity {:oid "1.2.246.562.11.11111111111"}}
                                                     information-request
                                                     runner))]
                          (should (c/equal?
                                    (c/plus
                                      (-> (new DateTime)
                                          (.withTime
                                            (get-in config [:public-config :information-request-reminder-job-hour])
                                            0 0 0))
                                      (c/days 12))
                                    send-reminder-time)))))

          (describe "store"
                    (it "should return correct information requests to remind"
                        (doseq [ir (information-requests-to-remind @application-key)]
                          (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                                                    (ir-store/add-information-request ir "1.2.246.562.11.11111111111" conn)))
                        (let [requests-to-remind (ir-store/get-information-requests-to-remind)]
                          (should= 4 (count requests-to-remind))
                          (ir-store/set-information-request-reminder-processed-time-by-id! (:id (first requests-to-remind)))
                          (should= 3 (count (ir-store/get-information-requests-to-remind))))))

          (describe "reminder-job"
                    (it "should set reminder_processed_time for handled information requests"
                        (with-redefs [ir-service/start-email-job (stub :start-email-job)]
                          (doseq [ir (information-requests-to-remind @application-key)]
                            (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                                                      (ir-store/add-information-request ir "1.2.246.562.11.11111111111" conn)))
                          (handler nil runner)
                          (should= 0 (count (ir-store/get-information-requests-to-remind)))
                          (should-have-invoked :start-email-job {:times 4})))
                    (it "should not send reminder if application has changed"
                        (with-redefs [ir-service/start-email-job (stub :start-email-job)]
                          (doseq [ir (information-requests-to-remind @application-key)]
                            (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                                                      (ir-store/add-information-request ir "1.2.246.562.11.11111111111" conn)))
                          (application-store/update-application
                            (application-store/get-application (:id (application-store/get-latest-application-by-key @application-key)))
                            []
                            minimal-form
                            {}
                            nil
                            nil)
                          (handler nil runner)
                          (should-not-have-invoked :start-email-job)))))