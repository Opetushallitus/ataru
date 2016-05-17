(ns ataru.hakija.hakija-routes
  (:require [compojure.core :refer [routes context GET]]
            [ring.util.http-response :refer [ok]]
            [compojure.route :as route]))

(def hakija-routes
  (-> (routes
        (context "/hakemus" []
           (GET "/" [] (ok "<h1>Hakija ui placeholder</h1>")))
        (route/not-found "<h1>Page not found</h1>"))))
