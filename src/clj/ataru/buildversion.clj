(ns ataru.buildversion
  (:require [compojure.api.sweet :as api]))

(defonce now (System/currentTimeMillis))

(api/defroutes buildversion-routes
  (api/GET "/buildversion.txt" []
    {:status 200
     :body (str now)}))
