(ns ataru.hakija.hakija-routes
  (:require [ataru.middleware.cache-control :as cache-control]
            [ataru.applications.application-store :as application-store]
            [ataru.hakija.hakija-form-service :as form-service]
            [ataru.hakija.hakija-application-service :as application-service]
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
            [ataru.virkailija.authentication.virkailija-edit :refer [virkailija-secret-valid?]])
  (:import [ring.swagger.upload Upload]
           [java.io InputStream]))

(def ^:private cache-fingerprint (System/currentTimeMillis))

(defn- deleted? [{:keys [deleted]}]
  (true? deleted))

(defn- attachment-metadata->answer [{:keys [fieldType] :as answer}]
  (cond-> answer
    (= fieldType "attachment")
    (update :value (partial file-store/get-metadata))))

(defn- attachments-metadata->answers [application]
  (update application :answers (partial map attachment-metadata->answer)))

(defn- get-application [secret]
  (let [application (-> secret
                        (application-store/get-latest-application-by-secret)
                        (application-service/remove-person-info-module-from-application-answers)
                        (attachments-metadata->answers))]
    (if application
      (do
        (info (str "Getting application " (:id application) " with answers"))
        (response/ok application))
      (do
        (info (str "Failed to get application belonging by secret, returning HTTP 404"))
        (response/not-found {})))))

(defn- get-application-by-virkailija-secret [virkailija-secret]
  (let [hakija-secret (application-store/get-hakija-secret-by-virkailija-secret virkailija-secret)]
    (get-application hakija-secret)))

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
    (api/GET ["/hakija-:testname{[A-Za-z]+}-test.html"] [testname]
      (render-file-in-dev (str "templates/hakija-" testname "-test.html")))
    (api/GET "/spec/:filename.js" [filename]
      (render-file-in-dev (str "spec/" filename ".js")))))

(api/defroutes james-routes
  (api/undocumented
    (api/GET "/favicon.ico" []
      (-> "public/images/james.jpg" io/resource))))

(defn- not-blank? [x]
  (not (clojure.string/blank? x)))

(defn api-routes [tarjonta-service]
  (api/context "/api" []
    :tags ["application-api"]
    (api/GET ["/haku/:haku-oid" :haku-oid #"[0-9\.]+"] []
      :summary "Gets form for haku"
      :path-params [haku-oid :- s/Str]
      :return ataru-schema/FormWithContentAndTarjontaMetadata
      (if-let [form-with-tarjonta (form-service/fetch-form-by-haku-oid
                                     tarjonta-service
                                     haku-oid)]
        (response/ok form-with-tarjonta)
        (response/not-found)))
    (api/GET ["/hakukohde/:hakukohde-oid", :hakukohde-oid #"[0-9\.]+"] []
      :summary "Gets form for hakukohde"
      :path-params [hakukohde-oid :- s/Str]
      :return ataru-schema/FormWithContentAndTarjontaMetadata
      (if-let [form-with-tarjonta (form-service/fetch-form-by-hakukohde-oid
                                    tarjonta-service
                                    hakukohde-oid)]
        (response/ok form-with-tarjonta)
        (response/not-found)))
    (api/GET "/form/:key" []
      :path-params [key :- s/Str]
      :return ataru-schema/FormWithContent
      (if-let [form (form-service/fetch-form-by-key key)]
        (response/ok form)
        (response/not-found)))
    (api/POST "/feedback" []
      :summary "Add feedback sent by applicant"
      :body [feedback ataru-schema/ApplicationFeedback]
      (if-let [saved-application (application-service/save-application-feedback feedback)]
        (do
          (flowdock-client/send-application-feedback saved-application)
          (response/ok {:id (:id saved-application)}))
        (response/bad-request)))
    (api/POST "/application" []
      :summary "Submit application"
      :body [application ataru-schema/Application]
      (match (application-service/handle-application-submit
              tarjonta-service
              application)
        {:passed? false :failures failures}
        (response/bad-request {:failures failures})

        {:passed? true :id application-id}
        (response/ok {:id application-id})))
    (api/PUT "/application" []
      :summary "Edit application"
      :body [application ataru-schema/Application]
      (match (application-service/handle-application-edit
              tarjonta-service
              application)
        {:passed? false :failures failures}
        (response/bad-request {:failures failures})

        {:passed? true :id application-id}
        (response/ok {:id application-id})))
    (api/GET "/application" []
      :summary "Get submitted application by secret"
      :query-params [{secret :- s/Str nil}
                     {virkailija-secret :- s/Str nil}]
      :return ataru-schema/Application
      (cond (not-blank? secret)
            (get-application secret)

            (and (not-blank? virkailija-secret)
                 (virkailija-secret-valid? virkailija-secret))
            (get-application-by-virkailija-secret virkailija-secret)

            :else
            (response/bad-request {:error "Invalid secret."})))
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
        :return [ataru-schema/ApplicationInfo]
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
                                  (api-routes (:tarjonta-service this))
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
