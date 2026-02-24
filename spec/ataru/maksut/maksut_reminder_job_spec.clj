(ns ataru.maksut.maksut-reminder-job-spec
  (:require [ataru.applications.application-store :as application-store]
            [ataru.db.db :as db]
            [ataru.maksut.maksut-protocol :as maksut-protocol]
            [ataru.maksut.maksut-reminder-job :as maksut-reminder-job]
            [ataru.maksut.maksut-store :as maksut-store]
            [ataru.time :as c]
            [clojure.java.jdbc :as jdbc]
            [ataru.background-job.job :as job]
            [com.stuartsierra.component :as component]
            [ataru.log.audit-log :as audit-log]
            [speclj.core :refer [describe tags it should= after around with-stubs stub should-have-invoked should-not-have-invoked]]))

(declare conn)
(declare spec)

(defn- clear! []
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (jdbc/delete! conn :payment_reminders [])))

(defn start-runner-job [_ _ _ _])

(defrecord FakeJobRunner []
  component/Lifecycle

  job/JobRunner
  (start-job [this conn job-type initial-state]
    (start-runner-job this conn job-type initial-state)))

(def runner
  (map->FakeJobRunner {}))

(describe "maksut-reminder-job"
          (tags :unit :maksut-reminder)
          (with-stubs)

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
                                                                                :reference "testihakemus"}]})
                            maksut-reminder-job/start-email-job (stub :start-email-job)
                            application-store/add-application-event-in-tx (stub :add-application-event)
                            application-store/get-application (stub :get-application
                                                                    {:invoke #(if (= %1 123)
                                                                                {:lang "fi"
                                                                                 :answers [{:key "email" :value "testi@testi.fi"}]}
                                                                                (throw (Exception. (str %1 " :get-application stub"))))})]
                (maksut-store/add-payment-reminder {:application-key "testihakemus"
                                                    :application-id 123
                                                    :message ""
                                                    :lang ""
                                                    :send-reminder-time (c/today-at 10 0)
                                                    :order-id "TTUtesti-2"})
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
                                                                                :reference "testihakemus"}]})
                            maksut-reminder-job/start-email-job (stub :start-email-job)
                            maksut-store/set-reminder-handled (stub :set-reminder-handled)]
                (maksut-store/add-payment-reminder {:application-key "testihakemus"
                                                    :application-id 123
                                                    :message ""
                                                    :lang ""
                                                    :send-reminder-time (c/today-at 10 0)
                                                    :order-id "TTUtesti-2"})
                (maksut-reminder-job/handler nil runner)
                (should-have-invoked :set-reminder-handled {:with [:* "paid"]})
                (should-not-have-invoked :start-email-job))))