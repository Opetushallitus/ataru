(ns ataru.hakija.hakija-routes
  (:require [ataru.buildversion :refer [buildversion-routes]]
            [ataru.applications.application-store :as application-store]
            [ataru.forms.form-store :as form-store]
            [ataru.hakija.email-store :as email-store]
            [ataru.hakija.validator :as validator]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.schema.form-schema :as ataru-schema]
            [ataru.util.client-error :as client-error]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [compojure.api.exception :as ex]
            [compojure.api.sweet :as api]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [ring.util.http-response :as response]
            [schema.core :as s]
            [selmer.parser :as selmer]
            [taoensso.timbre :refer [info warn error]]))

(def ^:private cache-fingerprint (System/currentTimeMillis))

(defn- fetch-form-by-key [key]
  (let [form (form-store/fetch-by-key key)]
    (if form
      (-> form
          (koodisto/populate-form-koodisto-fields)
          (response/ok))
      (response/not-found form))))

(defn- handle-application [application]
  (info "Received application:" application)
  (if (validator/valid-application? application)
    (let [stored-app-id (application-store/add-new-application application)]
      (info "Stored application with id:" stored-app-id)
      (email-store/store-email-verification application stored-app-id)
      (response/ok {:id stored-app-id}))
    (do
      (error "Invalid application!")
      (response/bad-request))))

(defn- handle-client-error [error-details]
  (client-error/log-client-error error-details)
  (response/ok {}))

(api/defroutes james-routes
  (api/undocumented
    (api/GET "/favicon.ico" []
      (-> "public/images/james.jpg" io/resource))))

(defn api-routes []
  (api/context "/api" []
    :tags ["application-api"]
    (api/GET "/form/:key" []
      :path-params [key :- s/Str]
      :return ataru-schema/FormWithContent
      (fetch-form-by-key key))
    (api/POST "/application" []
      :summary "Submit application"
      :body [application ataru-schema/Application]
      (handle-application application))
    (api/POST "/client-error" []
      :summary "Log client-side errors to server log"
      :body [error-details client-error/ClientError]
      (handle-client-error error-details))
    (api/GET "/postal-codes/:postal-code" [postal-code]
      :summary "Get name of postal office by postal code"
             (let [code (koodisto/get-postal-office-by-postal-code postal-code)]
               (if-let [labels (:label code)]
                 (response/ok labels)
                 (response/not-found))))))

(defrecord Handler []
  component/Lifecycle

  (start [this]
    (assoc this :routes (-> (api/api
                              {:swagger    {:spec "/hakemus/swagger.json"
                                            :ui   "/hakemus/api-docs"
                                            :data {:info {:version     "1.0.0"
                                                          :title       "Ataru Hakija API"
                                                          :description "Specifies the Hakija API for Ataru"}}
                                            :tags [{:name "application-api" :description "Application handling"}]}
                               :exceptions {:handlers {::ex/request-parsing
                                                       (ex/with-logging ex/request-parsing-handler :warn)
                                                       ::ex/request-validation
                                                       (ex/with-logging ex/request-validation-handler :warn)
                                                       ::ex/response-validation
                                                       (ex/with-logging ex/response-validation-handler :error)
                                                       ::ex/default
                                                       (ex/with-logging ex/safe-handler :error)}}}
                              (when (:dev? env) james-routes)
                              (api/routes
                                (api/context "/hakemus" []
                                             buildversion-routes
                                             (api-routes)
                                             (route/resources "/")
                                             (api/undocumented
                                             (api/GET "/:key" []
                                                      (selmer/render-file "templates/hakija.html" {:cache-fingerprint cache-fingerprint}))))
                                (route/not-found "<h1>Page not found</h1>")))
                            (wrap-with-logger
                              :debug identity
                              :info (fn [x] (info x))
                              :warn (fn [x] (warn x))
                              :error (fn [x] (error x)))
                            (wrap-gzip))))

  (stop [this]
    this))

(defn new-handler []
  (->Handler))
