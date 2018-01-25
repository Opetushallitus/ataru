(ns ataru.hakija.hakija-routes
  (:require [ataru.middleware.cache-control :as cache-control]
            [ataru.applications.application-store :as application-store]
            [ataru.hakija.hakija-form-service :as form-service]
            [ataru.hakija.hakija-application-service :as hakija-application-service]
            [ataru.files.file-store :as file-store]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.schema.form-schema :as ataru-schema]
            [ataru.util.client-error :as client-error]
            [clojure.core.match :refer [match]]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [compojure.api.exception :as ex]
            [compojure.api.sweet :as api]
            [compojure.api.upload :as upload]
            [ring.swagger.upload]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.logger :refer [wrap-with-logger] :as middleware-logger]
            [ring.util.http-response :as response]
            [schema.core :as s]
            [selmer.parser :as selmer]
            [taoensso.timbre :refer [info warn error]]
            [cheshire.core :as json]
            [ataru.config.core :refer [config]]
            [ataru.flowdock.flowdock-client :as flowdock-client]
            [ataru.test-utils :refer [get-test-vars-params]])
  (:import [ring.swagger.upload Upload]
           [java.io InputStream]))

(def ^:private cache-fingerprint (System/currentTimeMillis))

(defn- deleted? [{:keys [deleted]}]
  (true? deleted))

(defn- get-application
  [secret tarjonta-service ohjausparametrit-service person-client]
  (let [application (hakija-application-service/get-latest-application-by-secret secret
                                                                                 tarjonta-service
                                                                                 ohjausparametrit-service
                                                                                 person-client)]
    (cond (some? application)
          (response/ok application)
          (:virkailija secret)
          (response/bad-request {:error "Invalid virkailija secret"})
          :else
          (response/not-found {:error "No application found"}))))

(defn- handle-client-error [error-details]
  (client-error/log-client-error error-details)
  (response/ok {}))

(defn- is-dev-env?
  []
  (boolean (:dev? env)))

(defn- render-file-in-dev
  ([filename]
   (render-file-in-dev filename {}))
  ([filename opts]
   (if (is-dev-env?)
     (selmer/render-file filename opts)
     (response/not-found "Not found"))))

(api/defroutes test-routes
  (api/undocumented
    (api/GET ["/hakija-:testname{[A-Za-z\\-]+}-test.html"] [testname]
      (if (is-dev-env?)
        (render-file-in-dev (str "templates/hakija-" testname "-test.html"))
        (response/not-found "Not found")))
    (api/GET "/virkailija-hakemus-edit-test.html" []
      (if (is-dev-env?)
        (render-file-in-dev "templates/virkailija-hakemus-edit-test.html")
        (response/not-found "Not found")))
    (api/GET "/spec/:filename.js" [filename]
      ;; Test vars params is a hack to get form ids from fixtures to the test file
      ;; without having to pass them as url params. Also enables tests to be run
      ;; individually when navigationg to any test file.
      (if (is-dev-env?)
        (render-file-in-dev (str "spec/" filename ".js")
                            (when (= "hakijaCommon" filename)
                              (get-test-vars-params)))
        (response/not-found "Not found")))))

(api/defroutes james-routes
  (api/undocumented
    (api/GET "/favicon.ico" []
      (-> "public/images/james.jpg" io/resource))))

(defn- not-blank? [x]
  (not (clojure.string/blank? x)))

(defn api-routes [tarjonta-service ohjausparametrit-service person-service]
  (api/context "/api" []
    :tags ["application-api"]
    (api/GET ["/haku/:haku-oid" :haku-oid #"[0-9\.]+"] []
      :summary "Gets form for haku"
      :path-params [haku-oid :- s/Str]
      :query-params [{virkailija-secret :- s/Str nil}]
      :return ataru-schema/FormWithContentAndTarjontaMetadata
      (if-let [form-with-tarjonta (form-service/fetch-form-by-haku-oid
                                   tarjonta-service
                                   ohjausparametrit-service
                                   haku-oid
                                   (some? virkailija-secret))]
        (response/ok form-with-tarjonta)
        (response/not-found)))
    (api/GET ["/hakukohde/:hakukohde-oid", :hakukohde-oid #"[0-9\.]+"] []
      :summary "Gets form for hakukohde"
      :path-params [hakukohde-oid :- s/Str]
      :query-params [{virkailija-secret :- s/Str nil}]
      :return ataru-schema/FormWithContentAndTarjontaMetadata
      (if-let [form-with-tarjonta (form-service/fetch-form-by-hakukohde-oid
                                   tarjonta-service
                                   ohjausparametrit-service
                                   hakukohde-oid
                                   (some? virkailija-secret))]
        (response/ok form-with-tarjonta)
        (response/not-found)))
    (api/GET "/form/:key" []
      :path-params [key :- s/Str]
      :query-params [{virkailija-secret :- s/Str nil}]
      :return ataru-schema/FormWithContent
      (if-let [form (form-service/fetch-form-by-key key nil (some? virkailija-secret))]
        (response/ok form)
        (response/not-found)))
    (api/POST "/feedback" []
      :summary "Add feedback sent by applicant"
      :body [feedback ataru-schema/ApplicationFeedback]
      (if-let [saved-application (hakija-application-service/save-application-feedback feedback)]
        (do
          (flowdock-client/send-application-feedback saved-application)
          (response/ok {:id (:id saved-application)}))
        (response/bad-request)))
    (api/POST "/application" []
      :summary "Submit application"
      :body [application ataru-schema/Application]
      (match (hakija-application-service/handle-application-submit
              tarjonta-service
              ohjausparametrit-service
              application)
        {:passed? false :failures failures}
        (response/bad-request {:failures failures})

        {:passed? true :id application-id}
        (response/ok {:id application-id})))
    (api/PUT "/application" []
      :summary "Edit application"
      :body [application ataru-schema/Application]
      (match (hakija-application-service/handle-application-edit
              tarjonta-service
              ohjausparametrit-service
              application)
        {:passed? false :failures failures}
        (response/bad-request {:failures failures})

        {:passed? true :id application-id}
        (response/ok {:id application-id})))
    (api/GET "/application" []
      :summary "Get submitted application by secret"
      :query-params [{secret :- s/Str nil}
                     {virkailija-secret :- s/Str nil}]
      :return ataru-schema/ApplicationWithPerson
      (cond (not-blank? secret)
            (get-application {:hakija secret}
                             tarjonta-service
                             ohjausparametrit-service
                             person-service)

            (not-blank? virkailija-secret)
            (get-application {:virkailija virkailija-secret}
                             tarjonta-service
                             ohjausparametrit-service
                             person-service)

            :else
            (response/bad-request {:error "No secret given"})))
    (api/context "/files" []
      (api/POST "/" []
        :summary "Upload a file"
        :multipart-params [file :- upload/TempFileUpload]
        :middleware [upload/wrap-multipart-params]
        :return ataru-schema/File
        (try
          (if-let [resp (file-store/upload-file file)]
            (response/ok resp)
            (response/bad-request {:failures "Failed to upload file"}))
          (finally
            (io/delete-file (:tempfile file) true))))
      (api/DELETE "/:key" []
        :summary "Delete a file"
        :path-params [key :- s/Str]
        :return {:key s/Str}
        (if-let [resp (file-store/delete-file key)]
          (response/ok resp)
          (response/bad-request {:failures (str "Failed to delete file with key " key)}))))
    (api/context "/secure" []
      :tags ["secure-application-api"]
      (api/GET "/applications/:person-oid" []
        :summary "Get latest versions of every application belonging to a user with given person OID"
        :path-params [person-oid :- (api/describe s/Str "Person OID")]
        :return [ataru-schema/OmatsivutApplication]
        (response/ok (application-store/get-full-application-list-by-person-oid-for-omatsivut person-oid))))
    (api/POST "/client-error" []
      :summary "Log client-side errors to server log"
      :body [error-details client-error/ClientError]
      (handle-client-error error-details))
    (api/GET "/postal-codes/:postal-code" [postal-code]
      :summary "Get name of postal office by postal code"
             (let [code (koodisto/get-postal-office-by-postal-code postal-code)]
               (if-let [labels (:label code)]
                 (response/ok labels)
                 (response/not-found))))
    (api/GET "/has-applied" []
             :summary "Check if a person has already applied"
             :query-params [hakuOid :- (api/describe s/Str "Haku OID")
                            {ssn :- (api/describe s/Str "SSN") nil}
                            {email :- (api/describe s/Str "Email address") nil}]
             (cond (some? ssn)
                   (response/ok (application-store/has-ssn-applied hakuOid ssn))
                   (some? email)
                   (response/ok (application-store/has-email-applied hakuOid email))
                   :else
                   (response/bad-request {:error "Either ssn or email is required"})))))

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
                                            :tags [{:name "application-api" :description "Application handling"}
                                                   {:name "secure-application-api" :description "Secure application handling"}]}
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
                                  test-routes
                                  (api-routes (:tarjonta-service this)
                                              (:ohjausparametrit-service this)
                                              (:person-service this))
                                  (route/resources "/")
                                  (api/undocumented
                                    (api/GET "/haku/:oid" []
                                      (render-application))
                                    (api/GET "/hakukohde/:oid" []
                                      (render-application))
                                    (api/GET "/:key" []
                                      (render-application))
                                    (api/GET "/" []
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
