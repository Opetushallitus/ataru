(ns ataru.virkailija.virkailija-socket-routes
  (:require [ataru.log.access-log :as access-log]
            [aleph.http :as http]
            [ring.util.http-response :refer [bad-request] :as response]
            [compojure.api.sweet :as api]
            [manifold.executor :as e]
            [manifold.stream :as s]))

(def executor (e/fixed-thread-executor 1))

(def socket-and-oid (atom []))

(defn broadcast-colliding-reviews []
  (doall
    (map (fn [[socket oid]]
             (prn "checking status for " oid)
             ) @socket-and-oid)))

(def scheduled-review-update
  (s/periodically 1000 broadcast-colliding-reviews))

(defn application-review-socket-handler
  [oid]
  (fn [req]
      ;(->> socket(s/onto executor))
      (if-let [socket (try
                        @(http/websocket-connection req)
                        (catch Exception e
                          nil))]
        (do (swap! socket-and-oid (fn [all]
                                      (remove #(s/closed? (first %)) (cons [socket oid] all))))
            nil)
        (response/bad-request))))
