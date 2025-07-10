(ns ataru.maksut.maksut-reminder-job-spec
  (:require [ataru.applications.application-store :as application-store]
            [ataru.db.db :as db]
            [ataru.maksut.maksut-protocol :as maksut-protocol]
            [ataru.maksut.maksut-reminder-job :as maksut-reminder-job]
            [ataru.maksut.maksut-store :as maksut-store]
            [clj-time.core :as c]
            [clojure.java.jdbc :as jdbc]
            [ataru.fixtures.db.unit-test-db :as unit-test-db]
            [ataru.fixtures.form :refer [minimal-form]]
            [ataru.background-job.job :as job]
            [com.stuartsierra.component :as component]
            [ataru.log.audit-log :as audit-log]
            [speclj.core :refer [describe tags it should= before-all after around with-stubs stub should-have-invoked should-not-have-invoked]]
            [yesql.core :as sql]))

(declare conn)
(declare spec)

(defn- clear! []
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (jdbc/delete! conn :payment-reminders [])))

(declare yesql-upsert-virkailija<!)
(sql/defqueries "sql/virkailija-queries.sql")

(defn start-runner-job [_ _ _ _])

(defrecord FakeJobRunner []
  component/Lifecycle

  job/JobRunner
  (start-job [this conn job-type initial-state]
    (start-runner-job this conn job-type initial-state)))

(def runner
  (map->FakeJobRunner {}))

(def application-key (atom nil))

(describe "maksut-reminder-job"
          (tags :unit :maksut-reminder)
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


          (it "should set handled_at for handled reminders"
              (with-redefs [maksut-protocol/list-laskut-by-application-key (stub
                                                                             :list-laskut
                                                                             {:return
                                                                              [{:order_id "TTUtesti-2"
                                                                                :first_name "Timo"
                                                                                :last_name "Testi"
                                                                                :amount 123
                                                                                :status :active
                                                                                :due_date (str (c/today))
                                                                                :origin "tutu"
                                                                                :reference @application-key}]})
                            maksut-reminder-job/start-email-job (stub :start-email-job)]
                (maksut-store/add-payment-reminder {:application-key @application-key
                                                    :message ""
                                                    :lang ""
                                                    :send_reminder_time (c/today-at 10 0)
                                                    :order_id "TTUtesti-2"})
                (maksut-reminder-job/handler nil runner)
                (should= 0 (count (maksut-store/get-payment-reminders)))
                (should-have-invoked :start-email-job {:times 1})))

          (it "should not send reminder if invoice has been paid"
              (with-redefs [maksut-protocol/list-laskut-by-application-key (stub
                                                                             :list-laskut
                                                                             {:return
                                                                              [{:order_id "TTUtesti-2"
                                                                                :first_name "Timo"
                                                                                :last_name "Testi"
                                                                                :amount 123
                                                                                :status :paid
                                                                                :due_date (str (c/today))
                                                                                :origin "tutu"
                                                                                :reference @application-key}]})
                            maksut-reminder-job/start-email-job (stub :start-email-job)]
                (maksut-reminder-job/handler nil runner)
                (should-not-have-invoked :start-email-job))))