(ns ataru.virkailija.virkailija-routes
  (:require [ataru.middleware.cache-control :as cache-control]
            [ataru.middleware.user-feedback :as user-feedback]
            [ataru.middleware.session-store :refer [create-store]]
            [ataru.buildversion :refer [buildversion-routes]]
            [ataru.schema.form-schema :as ataru-schema]
            [ataru.applications.excel-export :as excel]
            [ataru.virkailija.authentication.auth-middleware :as auth-middleware]
            [ataru.virkailija.authentication.auth-routes :refer [auth-routes]]
            [ataru.applications.application-store :as application-store]
            [ataru.forms.form-store :as form-store]
            [ataru.util.client-error :as client-error]
            [ataru.virkailija.user.form-access-control :as access-controlled-form]
            [ataru.koodisto.koodisto :as koodisto]
            [cheshire.core :as json]
            [clojure.core.match :refer [match]]
            [clojure.java.io :as io]
            [compojure.api.sweet :as api]
            [compojure.api.exception :as ex]
            [compojure.response :refer [Renderable]]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [manifold.deferred :as d]
            [oph.soresu.common.config :refer [config]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [ring.util.http-response :refer [ok internal-server-error not-found bad-request content-type]]
            [ring.util.response :refer [redirect]]
            [schema.core :as s]
            [selmer.parser :as selmer]
            [taoensso.timbre :refer [spy debug error warn info]]
            [com.stuartsierra.component :as component]))

;; Compojure will normally dereference deferreds and return the realized value.
;; This unfortunately blocks the thread. Since aleph can accept the un-realized
;; deferred, we extend compojure's Renderable protocol to pass the deferred
;; through unchanged so that the thread won't be blocked.
(extend-protocol Renderable
                 manifold.deferred.Deferred
                 (render [d _] d))

(def ^:private cache-fingerprint (System/currentTimeMillis))

(api/defroutes app-routes
  (api/undocumented
    (api/GET "/" [] (selmer/render-file "templates/virkailija.html"
                                        {:cache-fingerprint cache-fingerprint
                                         :config (-> config
                                                     :public-config
                                                     json/generate-string)}))))

(defn- render-file-in-dev
  [filename]
  (if (:dev? env)
    (selmer/render-file filename {})
    (not-found "Not found")))

(api/defroutes test-routes
  (api/undocumented
    (api/GET "/test.html" []
      (render-file-in-dev "templates/test.html"))
    (api/GET "/spec/:filename.js" [filename]
      (render-file-in-dev (str "spec/" filename ".js")))))

(defn- org-names [session] (map :name (-> session :identity :organizations)))

(defn api-routes [{:keys [organization-service]}]
    (api/context "/api" []
                 :tags ["form-api"]

                 (api/GET "/user-info" {session :session}
                          (ok {:username (-> session :identity :username)
                               :organization-names (org-names session)}))

                 (api/GET "/forms" {session :session}
                   :summary "Return all forms."
                   :return {:forms [ataru-schema/Form]}
                   (ok (access-controlled-form/get-forms session organization-service)))

                 (api/GET "/forms/:id" []
                          :path-params [id :- Long]
                          :return ataru-schema/FormWithContent
                          :summary "Get content for form"
                          (ok (form-store/fetch-form id)))

                 (api/POST "/forms" {session :session}
                   :summary "Persist changed form."
                   :body [form ataru-schema/FormWithContent]
                   (ok (access-controlled-form/post-form form session organization-service)))

                 (api/POST "/client-error" []
                           :summary "Log client-side errors to server log"
                           :body [error-details client-error/ClientError]
                           (do
                             (client-error/log-client-error error-details)
                             (ok {})))

                 (api/context "/applications" []
                   :tags ["applications-api"]

                  (api/GET "/list" []
                           :query-params [formKey :- s/Str]
                           :summary "Return applications header-level info for form"
                           :return {:applications [ataru-schema/ApplicationInfo]}
                           (ok {:applications (application-store/get-application-list formKey)}))

                  (api/GET "/:application-id" []
                           :path-params [application-id :- Long]
                           :summary "Return application details needed for application review, including events and review data"
                           :return {:application ataru-schema/Application
                                    :events      [ataru-schema/Event]
                                    :review      ataru-schema/Review
                                    :form        ataru-schema/FormWithContent}
                           (ok
                            (let [application (application-store/get-application application-id)
                                  form        (form-store/fetch-by-id (:form application))]
                              {:application application
                               :form        form
                               :events      (application-store/get-application-events application-id)
                               :review      (application-store/get-application-review application-id)})))

                   (api/PUT "/review" []
                            :summary "Update existing application review"
                            :body [review s/Any]
                            (ok
                             (application-store/save-application-review review)
                             {}))

                   (api/GET "/excel/:form-key" []
                     :path-params [form-key :- s/Str]
                     :summary  "Return Excel export of the form and applications for it."
                     {:status  200
                      :headers {"Content-Type" "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                "Content-Disposition" (str "attachment; filename=" (excel/filename form-key))}
                      :body    (java.io.ByteArrayInputStream. (excel/export-all-applications form-key))}))

                 (api/context "/koodisto" []
                              :tags ["koodisto-api"]
                              (api/GET "/" []
                                       :return s/Any
                                       (let [koodisto-list (koodisto/list-all-koodistos)]
                                         (ok koodisto-list)))
                              (api/GET "/:koodisto-uri/:version" [koodisto-uri version]
                                       :path-params [koodisto-uri :- s/Str version :- Long]
                                       :return s/Any
                                       (let [koodi-options (koodisto/get-koodisto-options koodisto-uri version)]
                                         (ok koodi-options))))))

(api/defroutes resource-routes
  (api/undocumented
    (route/resources "/")))

(api/defroutes rich-routes
  (api/undocumented
    (api/GET "/favicon.ico" []
      (-> "public/images/rich.jpg" io/resource))))

(api/defroutes redirect-routes
  (api/undocumented
    (api/GET "/" [] (redirect "/lomake-editori/"))
    ;; NOTE: This is now needed because of the way web-server is
    ;; Set up on test and other environments. If you want
    ;; to remove this, test the setup with some local web server
    ;; with proxy_pass /lomake-editori -> <clj server>/lomake-editori
    ;; and verify that it works on test environment as well.
    (api/GET "/lomake-editori" [] (redirect "/lomake-editori/"))))

(defrecord Handler []
  component/Lifecycle

  (start [this]
    (assoc this :routes (-> (api/api
                              {:swagger {:spec "/lomake-editori/swagger.json"
                                         :ui "/lomake-editori/api-docs"
                                         :data {:info {:version "1.0.0"
                                                       :title "Ataru Clerk API"
                                                       :description "Specifies the clerk API for Ataru"}
                                                :tags [{:name "form-api" :description "Form handling"}
                                                       {:name "applications-api" :description "Application handling"}
                                                       {:name "postal-code-api" :description "Postal code service"}]}}
                               :exceptions {:handlers {::ex/request-parsing
                                                       (ex/with-logging ex/request-parsing-handler :warn)
                                                       ::ex/request-validation
                                                       (ex/with-logging ex/request-validation-handler :warn)
                                                       ::ex/response-validation
                                                       (ex/with-logging ex/response-validation-handler :error)
                                                       ::ex/default
                                                       (ex/with-logging ex/safe-handler :error)}}}
                              redirect-routes
                              (when (:dev? env) rich-routes)
                              (api/context "/lomake-editori" []
                                buildversion-routes
                                test-routes
                                (api/middleware [auth-middleware/with-authentication user-feedback/wrap-user-feedback]
                                  resource-routes
                                  app-routes
                                  (api-routes this)
                                  (auth-routes (:organization-service this))))
                              (api/undocumented
                                (route/not-found "Not found")))
                            (wrap-defaults (-> site-defaults
                                               (update :session assoc :store (create-store))
                                               (update :security dissoc :content-type-options :anti-forgery)
                                               (update :responses dissoc :content-types)))
                            (wrap-with-logger
                              :debug identity
                              :info  (fn [x] (info x))
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
