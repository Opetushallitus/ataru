(ns ataru.virkailija.virkailija-routes
  (:require [ataru.middleware.cache-control :as cache-control]
            [ataru.middleware.session-store :refer [create-store]]
            [ataru.buildversion :refer [buildversion-routes]]
            [ataru.schema.form-schema :as ataru-schema]
            [ataru.applications.excel-export :as excel]
            [ataru.virkailija.authentication.auth-middleware :as auth-middleware]
            [ataru.virkailija.authentication.auth-routes :refer [auth-routes]]
            [ataru.applications.application-store :as application-store]
            [ataru.forms.form-store :as form-store]
            [ataru.util.client-error :as client-error]
            [cheshire.core :as json]
            [compojure.api.sweet :as api]
            [compojure.response :refer [Renderable]]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [manifold.deferred :as d]
            [oph.soresu.common.config :refer [config]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [ring.util.http-response :refer [ok internal-server-error not-found bad-request content-type]]
            [ring.util.response :refer [file-response resource-response redirect]]
            [ring.util.response :refer [response]]
            [schema.core :as s]
            [selmer.parser :as selmer]
            [taoensso.timbre :refer [spy debug error warn info]]
            [schema.spec.core :as spec]
            [clj-time.coerce :as time-coerce]
            [com.stuartsierra.component :as component])
  (:import [manifold.deferred.Deferred]
           (org.joda.time DateTime)
           (clojure.lang ExceptionInfo)))

;; Compojure will normally dereference deferreds and return the realized value.
;; This unfortunately blocks the thread. Since aleph can accept the un-realized
;; deferred, we extend compojure's Renderable protocol to pass the deferred
;; through unchanged so that the thread won't be blocked.
(extend-protocol Renderable
                 manifold.deferred.Deferred
                 (render [d _] d))

;https://github.com/ztellman/aleph/blob/master/examples%2Fsrc%2Faleph%2Fexamples%2Fhttp.clj

(defn trying [f]
  (try (if-let [result (f)]
         (ok result)
         (not-found))
       (catch ExceptionInfo e
         (bad-request (-> e ex-data)))
       (catch Exception e
         (error e)
         (internal-server-error))))

(def ^:private cache-fingerprint (System/currentTimeMillis))

(api/defroutes app-routes
  (api/GET "/" [] (selmer/render-file "templates/virkailija.html"
                                      {:cache-fingerprint cache-fingerprint
                                       :config (-> config
                                                   :public-config
                                                   json/generate-string)})))

(defn- render-file-in-dev
  [filename]
  (if (:dev? env)
    (selmer/render-file filename {})
    (not-found "Not found")))

(api/defroutes test-routes
  (api/GET "/test.html" []
    (render-file-in-dev "templates/test.html"))
  (api/GET "/spec/:filename.js" [filename]
    (render-file-in-dev (str "spec/" filename ".js"))))

(def api-routes
    (api/context "/api" []
                 :tags ["form-api"]

                 (api/GET "/user-info" {session :session}
                   (ok {:username (-> session :identity :username)}))

                 (api/GET "/forms" []
                   :summary "Return all forms."
                   :return {:forms [ataru-schema/Form]}
                   (ok
                     {:forms (form-store/get-forms)}))

                 (api/GET "/forms/:id" []
                          :path-params [id :- Long]
                          :return ataru-schema/FormWithContent
                          :summary "Get content for form"
                          (trying #(form-store/fetch-form id)))

                 (api/POST "/forms" {session :session}
                   :summary "Persist changed form."
                   :body [form ataru-schema/FormWithContent]
                   (trying #(form-store/upsert-form
                             (assoc form :modified-by (-> session :identity :username)))))

                 (api/POST "/client-error" []
                           :summary "Log client-side errors to server log"
                           :body [error-details client-error/ClientError]
                           (do
                             (client-error/log-client-error error-details)
                             (ok {})))

                 (api/context "/applications" []
                   :tags ["applications-api"]

                  (api/GET "/list" []
                           :query-params [formId :- Long]
                           :summary "Return applications header-level info for form"
                           :return {:applications [ataru-schema/ApplicationInfo]}
                           (trying (fn [] {:applications (application-store/get-application-list formId)})))

                   (api/GET "/excel/:form-id" []
                     :path-params [form-id :- Long]
                     :summary "Return Excel export of the form and applications for it."
                     {:status 200
                      :headers {"Content-Type" "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                "Content-Disposition" (str "attachment; filename=" (excel/filename form-id))}
                      :body (java.io.ByteArrayInputStream. (excel/export-all-applications form-id))}))))

(api/defroutes resource-routes
  (route/resources "/"))

(api/defroutes redirect-routes
  (api/GET "/" [] (redirect "/lomake-editori/"))
  ;; NOTE: This is now needed because of the way web-server is
  ;; Set up on test and other environments. If you want
  ;; to remove this, test the setup with some local web server
  ;; with proxy_pass /lomake-editori -> <clj server>/lomake-editori
  ;; and verify that it works on test environment as well.
  (api/GET "/lomake-editori" [] (redirect "/lomake-editori/")))

(defrecord Handler []
  component/Lifecycle

  (start [this]
    (assoc this :routes (-> (api/api
                              {:swagger {:spec "/lomake-editori/swagger.json"
                                         :ui "/lomake-editori/api-docs"
                                         :data {:info {:version "1.0.0"
                                                       :title "Ataru Clerk API"
                                                       :description "Specifies the clerk API for Ataru"}}
                                         :tags [{:name "form-api" :description "Form handling"}]}}
                              (api/undocumented
                                redirect-routes)
                              (api/context "/lomake-editori" []
                                (api/undocumented
                                  buildversion-routes
                                  test-routes)
                                (api/middleware [auth-middleware/with-authentication]
                                  (api/undocumented
                                    resource-routes
                                    app-routes)
                                  api-routes
                                  (api/undocumented
                                    auth-routes)))
                              (api/undocumented
                                (route/not-found "Not found")))
                            (wrap-defaults (-> site-defaults
                                               (update-in [:session] assoc :store (create-store))
                                               (update-in [:security] dissoc :content-type-options)
                                               (update-in [:security] dissoc :anti-forgery)
                                               (update-in [:responses] dissoc :content-types)))
                            (wrap-with-logger
                              :debug identity
                              :info  identity
                              :warn  (fn [x] (warn x))
                              :error (fn [x] (error x)))
                            (wrap-gzip)
                            (cache-control/wrap-cache-control))))

  (stop [this]
    (when
      (contains? this :routes)
      (assoc this :routes nil))))

(defn new-handler []
  (->Handler))
