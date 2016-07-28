(ns ataru.hakija.hakija-routes
  (:require [ataru.buildversion :refer [buildversion-routes]]
            [ataru.hakija.email :as email]
            [ataru.hakija.validator :as validator]
            [ataru.forms.form-store :as form-store]
            [ataru.applications.application-store :as application-store]
            [com.stuartsierra.component :as component]
            [schema.core :as s]
            [ataru.schema.form-schema :as ataru-schema]
            [ataru.util.client-error :as client-error]
            [compojure.api.sweet :as api]
            [ring.util.http-response :as response]
            [compojure.route :as route]
            [selmer.parser :as selmer]
            [taoensso.timbre :refer [info]]))

(def ^:private cache-fingerprint (System/currentTimeMillis))

(defn- fetch-form [id]
  (let [form (form-store/fetch-form id)]
    (if form
      (response/ok form)
      (response/not-found form))))

(defn- handle-application [application]
  (info "Received application:")
  (info application)
  (if (validator/valid-application application)
    (let [stored-app-id (application-store/add-new-application application)]
      (info "Stored application with id:" stored-app-id)
      (email/send-email-verification application)
      (response/ok {:id stored-app-id}))
    (response/bad-request)))

(defn- handle-client-error [error-details]
  (client-error/log-client-error error-details)
  (response/ok {}))

(def api-routes
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
      (handle-client-error error-details))))

(defrecord Handler []
  component/Lifecycle

  (start [this]
    (assoc this :routes (api/api
                          {:swagger {:spec "/hakemus/swagger.json"
                                     :ui "/hakemus/api-docs"
                                     :data {:info {:version "1.0.0"
                                                   :title "Ataru Hakija API"
                                                   :description "Specifies the Hakija API for Ataru"}}
                                     :tags [{:name "application-api" :description "Application handling"}]}}
                          (api/routes
                            (api/context "/hakemus" []
                              buildversion-routes
                              api-routes
                              (route/resources "/")
                              (api/undocumented
                                (api/GET "/:id" []
                                  (selmer/render-file "templates/hakija.html" {:cache-fingerprint cache-fingerprint}))))
                            (route/not-found "<h1>Page not found</h1>")))))

  (stop [this]
    this))

(defn new-handler []
  (->Handler))
