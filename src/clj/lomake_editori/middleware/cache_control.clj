(ns lomake-editori.middleware.cache-control
  (:require [ring.util.response :as response]))

(defn wrap-cache-control
  [handler]
  (fn [req]
    (let [resp (handler req)
          uri  (:uri req)]
      (if
        (some #(= uri %) ["/" "/lomake-editori" "/lomake-editori/"])
        (response/header resp "Cache-Control" "no-cache")
        (response/header resp "Cache-Control" "max-age=86400")))))
