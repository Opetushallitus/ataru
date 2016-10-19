(ns ataru.middleware.cache-control
  (:require [ring.util.response :as response]))

(defn wrap-cache-control
  [handler]
  (fn [req]
    (let [resp  (handler req)
          uri   (:uri req)
          cache (cond
                  (= uri "/") "no-cache"
                  (or (= uri "/lomake-editori") (= uri "/lomake-editori/")) "no-cache"
                  (clojure.string/starts-with? uri "/lomake-editori/auth") "no-store"
                  (clojure.string/starts-with? uri "/lomake-editori/api") "no-store"
                  (clojure.string/starts-with? uri "/lomake-editori/editor") "no-cache"
                  (clojure.string/starts-with? uri "/lomake-editori/applications") "no-cache"
                  :else "max-age=86400")]
      (response/header resp "Cache-Control" cache))))
