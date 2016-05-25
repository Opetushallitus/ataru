(ns ataru.hakija.hakija-routes
  (:require [ataru.forms.form-store :as form-store]
            [compojure.core :refer [routes defroutes wrap-routes context GET]]
            [schema.core :as s]
            [compojure.api.sweet :as api]
            [ring.util.http-response :refer [ok not-found]]
            [compojure.route :as route]
            [selmer.parser :as selmer]))

(def ^:private cache-fingerprint (System/currentTimeMillis))

(defn- fetch-form [id]
  (let [form (form-store/fetch-form id)]
    (if form
      (ok form)
      (not-found form))))

(def api-routes
  (api/api
    {:swagger {:spec "/hakemus/swagger.json"
               :ui "/hakemus/api-docs"
               :data {:info {:version "1.0.0"
                             :title "Ataru Hakija API"
                             :description "Specifies the Hakija API for Ataru"}}
               :tags [{:name "application-api" :description "Application handling"}]}}
    (api/context "/api" []
                 :tags ["application-api"]
                 (api/GET "/form/:id" []
                          :path-params [id :- Long]
                          :return s/Any
                          (fetch-form id)))))

(def hakija-routes
  (-> (routes
        (context "/hakemus" []
          api-routes
          (route/resources "/")
          (GET "/:id" []
            (selmer/render-file "templates/hakija.html" {:cache-fingerprint cache-fingerprint})))
        (route/not-found "<h1>Page not found</h1>"))))
