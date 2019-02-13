(ns ataru.virkailija.virkailija-socket-routes
  (:require [ataru.log.access-log :as access-log]
            [ataru.cache.application-review-cache :refer [get-reviewers-for-application upsert-reviewer]]
            [aleph.http :as http]
            [ring.util.http-response :refer [bad-request] :as response]
            [compojure.api.sweet :as api]
            [cheshire.core :as json]
            [manifold.executor :as e]
            [manifold.stream :as s])
  (:import
   (java.util.concurrent Executors TimeUnit)))

(defn distinct-by [f sockets]
  (map first (vals (group-by f sockets))))

(defn new-virkailija-socket-executor []
  (let [executor      (e/fixed-thread-executor 1)
        review-master-stream (s/stream)]
    (->> review-master-stream
         (s/onto executor)
         (s/batch 1000 1000)
         (s/consume (fn [batch]
                        (doseq [[_ sockets] (group-by :hakemus-oid batch)]
                          (doseq [{:keys [callback]} (distinct-by :user-oid sockets)]
                            (callback))))))
    [executor review-master-stream]))

(defn application-review-socket-handler
  [hakemus-oid [executor review-master-stream] redis]
  (fn [req]
      (if-let [socket-stream (try
                               @(http/websocket-connection req)
                               (catch Exception e
                                 nil))]
        (let [user-oid (-> req :session :identity :oid)
              name     (str (-> req :session :identity :first-name) " " (-> req :session :identity :last-name))
              callback (fn []
                           (try
                             (let [reviewers (->> (get-reviewers-for-application redis hakemus-oid)
                                                  (remove #(= user-oid (:user-oid %)))
                                                  (map :name))]
                               (s/put! socket-stream (json/generate-string reviewers))
                               (upsert-reviewer redis hakemus-oid user-oid name))
                             (catch Exception e #(do))))]
          (callback)
          (s/connect (s/map (fn [_] {:hakemus-oid hakemus-oid
                                     :user-oid    user-oid
                                     :callback    callback
                                     }) socket-stream) review-master-stream
            {:upstream? true
             :downstream? false})
          nil)
        (response/bad-request))))
