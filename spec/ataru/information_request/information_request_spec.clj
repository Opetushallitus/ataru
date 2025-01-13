(ns ataru.information-request.information-request-spec
  (:require [ataru.db.db :as db]
            [ataru.fixtures.application :as application-fixtures]
            [clj-time.core :as c]
            [clojure.java.jdbc :as jdbc]
            [ataru.fixtures.db.unit-test-db :as unit-test-db]
            [ataru.fixtures.form :refer [minimal-form]]
            [ataru.background-job.job :as job]
            [com.stuartsierra.component :as component]
            [ataru.applications.application-store :as application-store]
            [ataru.information-request.fixtures :refer [information-requests-to-remind]]
            [ataru.information-request.information-request-store :as ir-store]
            [ataru.information-request.information-request-service :as ir-service]
            [ataru.information-request.information-request-reminder-job :refer [handler]]
            [ataru.log.audit-log :as audit-log]
            [speclj.core :refer [describe tags it should= before-all after around with-stubs stub should-not-have-invoked]]
            [yesql.core :as sql]))

(declare conn)
(declare spec)

(defn- clear! []
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (jdbc/delete! conn :information_requests [])))

(defn start-runner-job [_ _ _ _])

(defrecord FakeJobRunner []
  component/Lifecycle

  job/JobRunner
  (start-job [this conn job-type initial-state]
    (start-runner-job this conn job-type initial-state)))

(def runner
  (map->FakeJobRunner {}))

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
                       (assoc
                         application-fixtures/application-without-hakemusmaksu-exemption
                         :form
                         (:id minimal-form))
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
                          (should= (c/plus (c/today-at 6 0) (c/days 12))
                                   send-reminder-time))))

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
                        (doseq [ir (information-requests-to-remind @application-key)]
                          (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                                                    (ir-store/add-information-request ir "1.2.246.562.11.11111111111" conn)))
                        (handler nil runner)
                        (should= 0 (count (ir-store/get-information-requests-to-remind))))
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