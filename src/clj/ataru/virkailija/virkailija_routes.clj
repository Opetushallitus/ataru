(ns ataru.virkailija.virkailija-routes
  (:require [ataru.middleware.cache-control :as cache-control]
            [ataru.middleware.user-feedback :as user-feedback]
            [ataru.middleware.session-store :refer [create-store]]
            [ataru.buildversion :refer [buildversion-routes]]
            [ataru.schema.form-schema :as ataru-schema]
            [ataru.virkailija.authentication.auth-middleware :as auth-middleware]
            [ataru.virkailija.authentication.auth-routes :refer [auth-routes]]
            [ataru.applications.application-service :as application-service]
            [ataru.forms.form-store :as form-store]
            [ataru.util.client-error :as client-error]
            [ataru.forms.form-access-control :as access-controlled-form]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.applications.excel-export :as excel]
            [ataru.tarjonta-service.tarjonta-service :as tarjonta-service]
            [cheshire.core :as json]
            [clojure.core.match :refer [match]]
            [clojure.java.io :as io]
            [compojure.api.sweet :as api]
            [compojure.api.exception :as ex]
            [compojure.response :refer [Renderable]]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [manifold.deferred] ;; DO NOT REMOVE! extend-protocol below breaks otherwise!
            [oph.soresu.common.config :refer [config]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.logger :refer [wrap-with-logger] :as middleware-logger]
            [ring.util.http-response :refer [ok internal-server-error not-found bad-request content-type]]
            [ring.util.response :refer [redirect]]
            [schema.core :as s]
            [selmer.parser :as selmer]
            [taoensso.timbre :refer [spy debug error warn info]]
            [com.stuartsierra.component :as component]
            [clout.core :as clout]))

;; Compojure will normally dereference deferreds and return the realized value.
;; This unfortunately blocks the thread. Since aleph can accept the un-realized
;; deferred, we extend compojure's Renderable protocol to pass the deferred
;; through unchanged so that the thread won't be blocked.
(extend-protocol Renderable
                 manifold.deferred.Deferred
                 (render [d _] d))

(def ^:private cache-fingerprint (System/currentTimeMillis))

(def client-page-patterns #"(editor|applications)")

(def client-routes
  (clout/route-compile "/:page" {:page client-page-patterns}))

(def client-sub-routes
  (clout/route-compile "/:page/*" {:page client-page-patterns}))

(defn render-virkailija-page
  []
  (let [config (json/generate-string (or (:public-config config) {}))]
    (selmer/render-file "templates/virkailija.html"
                        {:cache-fingerprint cache-fingerprint
                         :config            config})))

(api/defroutes app-routes
  (api/undocumented
    (api/GET "/" [] (render-virkailija-page))
    (api/GET client-routes [] (render-virkailija-page))
    (api/GET client-sub-routes [] (render-virkailija-page))))

(defn- render-file-in-dev
  [filename]
  (if (:dev? env)
    (selmer/render-file filename {})
    (not-found "Not found")))

(api/defroutes test-routes
  (api/undocumented
    (api/GET "/virkailija-test.html" []
      (render-file-in-dev "templates/virkailija-test.html"))
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
                   :query-params [{include-deleted :- s/Bool false}]
                   :summary "Return all forms."
                   :return {:forms [ataru-schema/Form]}
                   (ok (access-controlled-form/get-forms include-deleted session organization-service)))

                 (api/GET "/forms-in-use" {session :session}
                          :summary "Return a map of form->haku currently in use in tarjonta-service"
                          :return {s/Str {s/Str {:haku-oid s/Str :haku-name s/Str}}}
                          (ok (tarjonta-service/get-forms-in-use)))

                 (api/GET "/forms/:id" []
                          :path-params [id :- Long]
                          :return ataru-schema/FormWithContent
                          :summary "Get content for form"
                          (ok (form-store/fetch-form id)))

                 (api/DELETE "/forms/:id" {session :session}
                   :path-params [id :- Long]
                   :summary "Mark form as deleted"
                   (ok (access-controlled-form/delete-form id session organization-service)))

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

                  (api/GET "/list" {session :session}
                           :query-params [{formKey :- s/Str nil}
                                          {hakukohdeOid :- s/Str nil}]
                           :summary "Return applications header-level info for form"
                           :return {:applications [ataru-schema/ApplicationInfo]}
                           (if formKey
                             (ok (application-service/get-application-list-by-form formKey session organization-service))
                             (ok (application-service/get-application-list-by-hakukohde hakukohdeOid session organization-service))))

                  (api/GET "/:application-key" {session :session}
                    :path-params [application-key :- String]
                    :summary "Return application details needed for application review, including events and review data"
                    :return {:application ataru-schema/Application
                             :events      [ataru-schema/Event]
                             :review      ataru-schema/Review
                             :form        ataru-schema/FormWithContent}
                    (ok (application-service/get-application-with-human-readable-koodis application-key session organization-service)))

                   (api/PUT "/review" {session :session}
                            :summary "Update existing application review"
                            :body [review ataru-schema/Review]
                            :return {:review ataru-schema/Review
                                     :events [ataru-schema/Event]}
                            (ok
                             (application-service/save-application-review
                                 review
                                 session
                                 organization-service)))

                   (api/GET "/excel/:form-key" {session :session}
                            :path-params [form-key :- s/Str]
                            :query-params [{state :- [s/Str] nil}]
                            :summary  "Return Excel export of the form and applications for it."
                            {:status  200
                             :headers {"Content-Type"        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                       "Content-Disposition" (str "attachment; filename=" (excel/filename form-key))}
                             :body    (application-service/get-excel-report-of-applications-by-form
                                       form-key
                                       state
                                       session
                                       organization-service)})

                   (api/GET "/excel/:form-key/:hakukohde-oid" {session :session}
                            :path-params [form-key :- s/Str
                                          hakukohde-oid :- s/Str]
                            :query-params [{state :- [s/Str] nil}]
                            :summary "Return Excel export of the form and hakukohde and applications for it."
                            {:status  200
                             :headers {"Content-Type"        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                       "Content-Disposition" (str "attachment; filename=" (excel/filename form-key hakukohde-oid))}
                             :body    (application-service/get-excel-report-of-applications-by-hakukohde
                                       form-key
                                       state
                                       hakukohde-oid
                                       session
                                       organization-service)}))

                 (api/GET "/hakukohteet" []
                          :summary "List hakukohde information found for applications stored in system"
                          :return [{:hakukohde      s/Str
                                    :hakukohde-name s/Str
                                    :form-key       s/Str}]
                          (ok (ataru.applications.application-store/get-hakukohteet)))

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
                              :error (fn [x] (error x))
                              :pre-logger (fn [_ _] nil)
                              :post-logger (fn [options {:keys [uri] :as request} {:keys [status] :as response} totaltime]
                                             (when (or
                                                     (>= status 400)
                                                     (clojure.string/starts-with? uri "/lomake-editori/api/"))
                                               (#'middleware-logger/post-logger options request response totaltime))))
                            (wrap-gzip)
                            (cache-control/wrap-cache-control))))

  (stop [this]
    (when
      (contains? this :routes)
      (assoc this :routes nil))))

(defn new-handler []
  (->Handler))
