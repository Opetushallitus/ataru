(ns ataru.middleware.cache-control
  (:require [ring.util.response :as response]))

(defn wrap-cache-control
  [handler]
  (fn [req]
    (let [resp  (handler req)
          uri   (:uri req)
          cache (cond
                  (some #(= uri %) ["/" "/lomake-editori" "/lomake-editori/"]) "no-cache"
                  (clojure.string/starts-with? uri "/lomake-editori/api") "no-store"
                  :else "max-age=86400")]
      (response/header resp "Cache-Control" cache))))
