(ns ataru.hakija.hakija-routes
  (:require [ataru.buildversion :refer [buildversion-routes]]
            [ataru.hakija.email :as email]
            [ataru.forms.form-store :as form-store]
            [ataru.applications.application-store :as application-store]
            [compojure.core :refer [routes defroutes wrap-routes context GET]]
            [schema.core :as s]
            [ataru.schema.clj-schema :as ataru-schema]
            [ataru.util.client-error :as client-error]
            [compojure.api.sweet :as api]
            [ring.util.http-response :refer [ok not-found]]
            [compojure.route :as route]
            [selmer.parser :as selmer]
            [taoensso.timbre :refer [info]]))

(def ^:private cache-fingerprint (System/currentTimeMillis))

(defn- fetch-form [id]
  (let [form (form-store/fetch-form id)]
    (if form
      (ok form)
      (not-found form))))

(defn- handle-application [application]
  (info "Received application:")
  (info application)
  (let [stored-app-id (application-store/insert-application application)]
    (info "Stored application with id:" stored-app-id)
    (email/send-email-verification application)
    (ok {})))

(defn- handle-client-error [error-details]
  (client-error/log-client-error error-details)
  (ok {}))

(def api-routes
  (api/api
    {:swagger {:spec "/swagger.json"
               :ui "/api-docs"
               :data {:info {:version "1.0.0"
                             :title "Ataru Hakija API"
                             :description "Specifies the Hakija API for Ataru"}}
               :tags [{:name "application-api" :description "Application handling"}]}}
    (api/context "/api" []
                 :tags ["application-api"]
                 (api/GET "/form/:id" []
                          :path-params [id :- Long]
                          :return ataru-schema/FormWithContent
                          (fetch-form id))
                 (api/POST "/application" []
                           :summary "Submit application"
                           :body [application ataru-schema/Application]
                           (handle-application application))
                 (api/POST "/client-error" []
                           :summary "Log client-side errors to server log"
                           :body [error-details client-error/ClientError]
                           (handle-client-error error-details)))))

(def hakija-routes
  (-> (routes
        (context "/hakemus" []
          buildversion-routes
          api-routes
          (route/resources "/")
          (GET "/:id" []
            (selmer/render-file "templates/hakija.html" {:cache-fingerprint cache-fingerprint})))
        (route/not-found "<h1>Page not found</h1>"))))
