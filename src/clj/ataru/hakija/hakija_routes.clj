(ns ataru.hakija.hakija-routes
  (:require [compojure.core :refer [routes defroutes wrap-routes context GET]]
            [ring.util.http-response :refer [ok]]
            [compojure.route :as route]
            [selmer.parser :as selmer]))

(def ^:private cache-fingerprint (System/currentTimeMillis))

(def hakija-routes
  (-> (routes
        (context "/hakemus" []
          (route/resources "/")
          (GET "/" []
            (selmer/render-file "templates/hakija.html" {:cache-fingerprint cache-fingerprint})))
        (route/not-found "<h1>Page not found</h1>"))))
