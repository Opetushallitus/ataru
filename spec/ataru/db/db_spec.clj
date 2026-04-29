(ns ataru.db.db-spec
  (:require [speclj.core :refer :all]
            [ataru.db.db :as db])
  (:import (java.sql SQLException Connection)
           (javax.sql DataSource)
           (software.amazon.jdbc.util SqlState)))

(defn- failover-exception [sql-state]
  (SQLException. "Simulated failover" sql-state))

(defn- mock-datasource []
  (let [conn (reify Connection
               (getAutoCommit [_] false)
               (setAutoCommit [_ _])
               (getTransactionIsolation [_] Connection/TRANSACTION_READ_COMMITTED)
               (setTransactionIsolation [_ _])
               (isReadOnly [_] false)
               (setReadOnly [_ _])
               (commit [_])
               (rollback [_])
               (rollback [_ _])
               (close [_]))]
    (reify DataSource
      (getConnection [_] conn))))

(defn- failing-then-succeeding [fail-times return-val]
  (let [calls (atom 0)]
    (fn [_]
      (let [attempt (swap! calls inc)]
        (if (<= attempt fail-times)
          (throw (failover-exception (.getState SqlState/COMMUNICATION_LINK_CHANGED)))
          return-val)))))

(describe "with-failover-retry"
  (tags :unit)

  (it "returns result immediately when no exception occurs"
    (let [ds (#'db/with-failover-retry (mock-datasource) false (fn [_] :ok))]
      (should= :ok ds)))

  (it "retries on COMMUNICATION_LINK_CHANGED and returns result"
    (let [datasource (mock-datasource)
          f          (failing-then-succeeding 1 :recovered)]
      (should= :recovered (#'db/with-failover-retry datasource false f))))

  (it "retries on CONNECTION_FAILURE_DURING_TRANSACTION and returns result"
    (let [datasource (mock-datasource)
          calls      (atom 0)
          result     (#'db/with-failover-retry datasource true
                       (fn [_]
                         (let [attempt (swap! calls inc)]
                           (if (= attempt 1)
                             (throw (failover-exception (.getState SqlState/CONNECTION_FAILURE_DURING_TRANSACTION)))
                             :tx-recovered))))]
      (should= :tx-recovered result)
      (should= 2 @calls)))

  (it "retries up to max-failover-retries times"
    (let [datasource (mock-datasource)
          calls      (atom 0)
          f          (fn [_]
                       (let [attempt (swap! calls inc)]
                         (if (<= attempt 2)
                           (throw (failover-exception (.getState SqlState/COMMUNICATION_LINK_CHANGED)))
                           :after-two-failures)))]
      (should= :after-two-failures (#'db/with-failover-retry datasource false f))
      (should= 3 @calls)))

  (it "throws ex-info when retries are exhausted"
    (let [datasource (mock-datasource)
          f          (fn [_] (throw (failover-exception (.getState SqlState/COMMUNICATION_LINK_CHANGED))))]
      (should-throw clojure.lang.ExceptionInfo
        (#'db/with-failover-retry datasource false f))))

  (it "does not retry on non-failover SQLException"
    (let [datasource (mock-datasource)
          calls      (atom 0)]
      (should-throw SQLException
        (#'db/with-failover-retry datasource false
          (fn [_]
            (swap! calls inc)
            (throw (SQLException. "syntax error" "42601")))))
      (should= 1 @calls))))
