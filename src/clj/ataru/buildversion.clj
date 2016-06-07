(ns ataru.buildversion
  (:require [compojure.core :refer [GET defroutes routes]]))

(defonce now (System/currentTimeMillis))

(defroutes buildversion-routes
  (GET "/buildversion.txt" []
    {:status 200
     :body (str now)}))
