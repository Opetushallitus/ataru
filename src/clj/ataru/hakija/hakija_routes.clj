(ns ataru.hakija.hakija-routes
  (:require [ataru.middleware.cache-control :as cache-control]
            [ataru.buildversion :refer [buildversion-routes]]
            [ataru.applications.application-store :as application-store]
            [ataru.hakija.application-email-confirmation :as application-email]
            [ataru.background-job.job :as job]
            [ataru.forms.form-store :as form-store]
            [ataru.hakija.validator :as validator]
            [ataru.hakija.background-jobs.hakija-jobs :as hakija-jobs]
            [ataru.hakija.hakija-form-service :as form-service]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.schema.form-schema :as ataru-schema]
            [ataru.person-service.person-integration :as person-integration]
            [ataru.tarjonta-service.tarjonta-client :as tarjonta-client]
            [ataru.util.client-error :as client-error]
            [clojure.core.match :refer [match]]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clj-time.core :as t]
            [com.stuartsierra.component :as component]
            [compojure.api.exception :as ex]
            [compojure.api.sweet :as api]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.logger :refer [wrap-with-logger] :as middleware-logger]
            [ring.util.http-response :as response]
            [schema.core :as s]
            [selmer.parser :as selmer]
            [taoensso.timbre :refer [info warn error]]
            [cheshire.core :as json]
            [oph.soresu.common.config :refer [config]]))

(def ^:private cache-fingerprint (System/currentTimeMillis))

(defn- deleted? [{:keys [deleted]}]
  (true? deleted))

(defn- handle-application [application]
  (info "Received application:" application)
  (let [form (form-store/fetch-latest-version (:form application))
        validatorf (fn [f] (validator/valid-application? application f))]
    (match form
      (_ :guard deleted?)
      (do (warn (str "Form " (:id form) " deleted!"))
          (response/bad-request {:passed? false :failures {:response "Lomake on poistettu"}}))

      ({:passed?  false
        :failures failures} :<< validatorf)
      (response/bad-request failures)

      :else
      (let [application-id        (application-store/add-new-application application)
            person-service-job-id (job/start-job hakija-jobs/job-definitions
                                    (:type person-integration/job-definition)
                                    {:application-id application-id})]
        (application-email/start-email-confirmation-job application-id)
        (info "Stored application with id:" application-id)
        (info "Started person creation job (to person service) with job id" person-service-job-id)
        (response/ok {:id application-id})))))

(defn- get-application [key secret]
  (let [application (application-store/get-latest-application-by-key key)]
    (if (= (:secret application) secret)
      (do
        (info (str "Getting application " (:id application) " with answers"))
        (response/ok (dissoc application :secret)))
      (do
        (info (str "Not returning application " (:id application)))
        (info (str "Provided secret: " secret))
        (info (str "Application secret: " (:secret application)))
        (response/not-found)))))

(defn- handle-client-error [error-details]
  (client-error/log-client-error error-details)
  (response/ok {}))

(defn- is-dev-env?
  []
  (boolean (:dev? env)))

(defn- render-file-in-dev
  [filename]
  (if (is-dev-env?)
    (selmer/render-file filename {})
    (response/not-found "Not found")))

(api/defroutes test-routes
  (api/undocumented
   (api/GET "/hakija-test.html" []
            (render-file-in-dev "templates/hakija-test.html"))
   (api/GET "/spec/:filename.js" [filename]
            (render-file-in-dev (str "spec/" filename ".js")))))

(api/defroutes james-routes
  (api/undocumented
    (api/GET "/favicon.ico" []
      (-> "public/images/james.jpg" io/resource))))

(defn api-routes []
  (api/context "/api" []
    :tags ["application-api"]
    (api/GET ["/hakukohde/:hakukohde-oid", :hakukohde-oid #"[0-9\.]+"] []
      :summary "Gets form by hakukohde (assumes 1:1 mapping for form and hakukohde)"
      :path-params [hakukohde-oid :- s/Str]
      :return ataru-schema/FormWithContent
      (if-let [form-with-hakukohde (form-service/fetch-form-by-hakukohde-oid hakukohde-oid)]
        (response/ok form-with-hakukohde)
        (response/not-found)))
    (api/GET "/form/:key" []
      :path-params [key :- s/Str]
      :return ataru-schema/FormWithContent
      (if-let [form (form-service/fetch-form-by-key key)]
        (response/ok form)
        (response/not-found)))
    (api/POST "/application" []
      :summary "Submit application"
      :body [application ataru-schema/Application]
      (handle-application application))
    (api/GET "/application/:application-key/:secret" []
      :summary "Get submitted application"
      :path-params [application-key :- s/Str
                    secret :- s/Str]
      :return ataru-schema/Application
      (get-application application-key secret))
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

(defn- render-application []
  (let [config (json/generate-string (or (:public-config config) {}))]
    (selmer/render-file "templates/hakija.html" {:cache-fingerprint cache-fingerprint
                                                 :config            config})))

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
                              (when (is-dev-env?) james-routes)
                              (api/routes
                                (api/context "/hakemus" []
                                             buildversion-routes
                                             test-routes
                                             (api-routes)
                                             (route/resources "/")
                                             (api/undocumented
                                             (api/GET "/hakukohde/:oid" []
                                               (render-application))
                                             (api/GET "/hakukohde/:oid/:lang" []
                                               (render-application))
                                             (api/GET "/:key" []
                                               (render-application))
                                             (api/GET "/:key/:lang" []
                                               (render-application))))
                                (route/not-found "<h1>Page not found</h1>")))
                            (wrap-with-logger
                              :debug identity
                              :info (fn [x] (info x))
                              :warn (fn [x] (warn x))
                              :error (fn [x] (error x))
                              :pre-logger (fn [_ _] nil)
                              :post-logger (fn [options {:keys [uri] :as request} {:keys [status] :as response} totaltime]
                                             (when (or
                                                     (>= status 400)
                                                     (clojure.string/starts-with? uri "/hakemus/api/"))
                                               (#'middleware-logger/post-logger options request response totaltime))))
                            (wrap-gzip)
                            (cache-control/wrap-cache-control))))

  (stop [this]
    this))

(defn new-handler []
  (->Handler))
