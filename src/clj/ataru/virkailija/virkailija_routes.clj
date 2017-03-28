(ns ataru.virkailija.virkailija-routes
  (:require [ataru.config.url-helper :refer [resolve-url]]
            [ataru.middleware.cache-control :as cache-control]
            [ataru.middleware.user-feedback :as user-feedback]
            [ataru.middleware.session-store :refer [create-store]]
            [ataru.middleware.session-timeout :as session-timeout]
            [ataru.schema.form-schema :as ataru-schema]
            [ataru.virkailija.authentication.auth-middleware :as auth-middleware]
            [ataru.virkailija.authentication.auth-routes :refer [auth-routes]]
            [ataru.virkailija.authentication.auth-utils :as auth-utils]
            [ataru.applications.application-service :as application-service]
            [ataru.forms.form-store :as form-store]
            [ataru.files.file-store :as file-store]
            [ataru.util.client-error :as client-error]
            [ataru.applications.application-access-control :as access-controlled-application]
            [ataru.forms.form-access-control :as access-controlled-form]
            [ataru.hakukohde.hakukohde-access-control :as access-controlled-hakukohde]
            [ataru.haku.haku-access-control :as access-controlled-haku]
            [ataru.haku.haku-service :as haku-service]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.applications.excel-export :as excel]
            [ataru.virkailija.user.session-organizations :refer [organization-list]]
            [cheshire.core :as json]
            [clojure.core.match :refer [match]]
            [clojure.java.io :as io]
            [compojure.api.sweet :as api]
            [compojure.api.exception :as ex]
            [compojure.response :refer [Renderable]]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [manifold.deferred] ;; DO NOT REMOVE! extend-protocol below breaks otherwise!
            [ataru.config.core :refer [config]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.session :as ring-session]
            [ring.middleware.logger :refer [wrap-with-logger] :as middleware-logger]
            [ring.util.http-response :refer [ok internal-server-error not-found bad-request content-type set-cookie]]
            [ring.util.response :refer [redirect]]
            [schema.core :as s]
            [selmer.parser :as selmer]
            [taoensso.timbre :refer [spy debug error warn info]]
            [com.stuartsierra.component :as component]
            [clout.core :as clout]
            [ring.util.http-response :as response]))

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
    (-> (selmer/render-file "templates/virkailija.html"
                            {:cache-fingerprint cache-fingerprint
                             :config            config})
        (ok)
        (content-type "text/html"))))

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

(defn- wrap-database-backed-session [handler]
  (ring-session/wrap-session handler
                             {:root "/lomake-editori"
                              :cookie-attrs {:secure (not (:dev? env))}
                              :store (create-store)}))

(api/defroutes test-routes
  (api/undocumented
    (api/GET "/virkailija-test.html" []
      (render-file-in-dev "templates/virkailija-test.html"))
    (api/GET "/spec/:filename.js" [filename]
      (render-file-in-dev (str "spec/" filename ".js")))))

(defn api-routes [{:keys [organization-service tarjonta-service virkailija-tarjonta-service cache-service]}]
    (api/context "/api" []
                 :tags ["form-api"]

                 (api/GET "/user-info" {session :session}
                          (ok {:username (-> session :identity :username)
                               :organizations (organization-list session)}))

                 (api/GET "/forms-for-editor" {session :session}
                   :summary "Return forms for editor view"
                   :return {:forms [ataru-schema/Form]}
                   (ok (access-controlled-form/get-forms-for-editor session organization-service)))

                 (api/GET "/forms-for-application-listing" {session :session}
                   :summary "Return for application viewing purposes"
                   :return {:forms [ataru-schema/Form]}
                   (ok (haku-service/get-forms-for-application-listing session organization-service)))

                 (api/GET "/forms" {session :session}
                   :summary "Used by external services. In practice this is Tarjonta system only for now.
                             Return forms authorized with editor right (:form-edit)"
                   :return {:forms [ataru-schema/Form]}
                   (ok (access-controlled-form/get-forms-for-editor session organization-service)))

                 (api/GET "/forms-in-use" {session :session}
                          :summary "Return a map of form->hakus-currently-in-use-in-tarjonta-service"
                          :return {s/Str {s/Str {:haku-oid s/Str :haku-name s/Str}}}
                          (ok (.get-forms-in-use virkailija-tarjonta-service (-> session :identity :username))))

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
                                          {hakukohdeOid :- s/Str nil}
                                          {hakuOid :- s/Str nil}]
                           :summary "Return applications header-level info for form"
                           :return {:applications [ataru-schema/ApplicationInfo]}
                           (cond
                             (some? formKey)
                             (ok (application-service/get-application-list-by-form formKey session organization-service))

                             (some? hakukohdeOid)
                             (ok (access-controlled-application/get-application-list-by-hakukohde hakukohdeOid session organization-service))

                             (some? hakuOid)
                             (ok (access-controlled-application/get-application-list-by-haku hakuOid session organization-service))))

                  (api/GET "/:application-key" {session :session}
                    :path-params [application-key :- String]
                    :summary "Return application details needed for application review, including events and review data"
                    :return {:application ataru-schema/Application
                             :events      [ataru-schema/Event]
                             :review      ataru-schema/Review
                             :form        ataru-schema/FormWithContent}
                    (ok (application-service/get-application-with-human-readable-koodis application-key session organization-service tarjonta-service)))

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

                   (api/context "/excel" []
                     (api/GET "/form/:form-key" {session :session}
                              :path-params [form-key :- s/Str]
                              :query-params [{state :- [s/Str] nil}]
                              :summary "Return Excel export of the form and applications for it."
                              {:status  200
                               :headers {"Content-Type"        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                         "Content-Disposition" (str "attachment; filename=" (excel/filename-by-form form-key))}
                               :body    (application-service/get-excel-report-of-applications-by-form
                                          form-key
                                          state
                                          session
                                          organization-service
                                          tarjonta-service)})

                     (api/GET "/hakukohde/:hakukohde-oid" {session :session}
                              :path-params [hakukohde-oid :- s/Str]
                              :query-params [{state :- [s/Str] nil}]
                              :summary "Return Excel export of the hakukohde and applications for it."
                              {:status  200
                               :headers {"Content-Type"        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                         "Content-Disposition" (str "attachment; filename=" (excel/filename-by-hakukohde hakukohde-oid session organization-service tarjonta-service))}
                               :body    (application-service/get-excel-report-of-applications-by-hakukohde
                                          hakukohde-oid
                                          state
                                          session
                                          organization-service
                                          tarjonta-service)})

                     (api/GET "/haku/:haku-oid" {session :session}
                              :path-params [haku-oid :- s/Str]
                              :query-params [{state :- [s/Str] nil}]
                              :summary "Return Excel export of the haku and applications for it."
                              {:status  200
                               :headers {"Content-Type"        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                         "Content-Disposition" (str "attachment; filename=" (excel/filename-by-haku haku-oid session organization-service tarjonta-service))}
                               :body    (application-service/get-excel-report-of-applications-by-haku
                                          haku-oid
                                          state
                                          session
                                          organization-service
                                          tarjonta-service)})))

                 (api/context "/cache" []
                   (api/POST "/clear/:cache" {session :session}
                     :path-params [cache :- s/Str]
                     :summary "Clear an entire cache map of its entries"
                     {:status 200
                      :body   (do (.cache-clear cache-service (keyword cache))
                                  {})})
                   (api/POST "/remove/:cache/:key" {session :session}
                     :path-params [cache :- s/Str
                                   key :- s/Str]
                     :summary "Remove an entry from cache map"
                     {:status 200
                      :body   (do (.cache-remove cache-service (keyword cache) key)
                                  {})}))

                 (api/GET "/hakukohteet" {session :session}
                          :summary "List hakukohde information found for applications stored in system"
                          :return [{:hakukohde         s/Str
                                    :hakukohde-name    s/Str
                                    :application-count s/Int}]
                          (ok (access-controlled-hakukohde/get-hakukohteet session organization-service tarjonta-service)))

                 (api/GET "/haut" {session :session}
                          :summary "List haku information found for applications stored in system"
                          :return [ataru-schema/Haku]
                          (ok (access-controlled-haku/get-haut session organization-service tarjonta-service)))

                 (api/GET "/haut2" {session :session}
                          :summary "List haku and hakukohde information found for applications stored in system"
                          :return s/Any
                          (ok {:tarjonta-haut (haku-service/get-haut session organization-service tarjonta-service)
                               :direct-form-haut []}))

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
                                         (ok koodi-options))))

                 (api/context "/files" []
                   :tags ["files-api"]
                   (api/GET "/metadata" []
                     :query-params [key :- (api/describe [s/Str] "File key")]
                     :summary "Get metadata for one or more files"
                     :return [ataru-schema/File]
                     (if-let [resp (file-store/get-metadata key)]
                       (ok resp)
                       (not-found)))
                   (api/GET "/content/:key" []
                     :path-params [key :- (api/describe s/Str "File key")]
                     :summary "Download a file"
                     (if-let [file-stream (file-store/get-file key)]
                       (ok file-stream)
                       (not-found))))))

(api/defroutes resource-routes
  (api/undocumented
    (route/resources "/lomake-editori")))

(api/defroutes rich-routes
  (api/undocumented
    (api/GET "/favicon.ico" []
      (-> "public/images/rich.jpg" io/resource))))

(defn redirect-to-service-url
  []
  (redirect (get-in config [:public-config :virkailija :service_url])))

(api/defroutes redirect-routes
  (api/undocumented
    (api/GET "/" [] (redirect-to-service-url))
    ;; NOTE: This is now needed because of the way web-server is
    ;; Set up on test and other environments. If you want
    ;; to remove this, test the setup with some local web server
    ;; with proxy_pass /lomake-editori -> <clj server>/lomake-editori
    ;; and verify that it works on test environment as well.
    (api/GET "/lomake-editori" [] (redirect-to-service-url))))

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
                                                       {:name "koodisto-api" :description "Koodisto service"}
                                                       {:name "files-api" :description "File service"}]}}
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
                              resource-routes
                              (api/context "/lomake-editori" []
                                test-routes
                                (api/middleware [user-feedback/wrap-user-feedback
                                                 wrap-database-backed-session
                                                 auth-middleware/with-authentication]
                                  (api/middleware [session-timeout/wrap-idle-session-timeout]
                                    app-routes
                                    (api-routes this))
                                  (auth-routes (:organization-service this))))
                              (api/undocumented
                                (route/not-found "Not found")))
                            (wrap-defaults (-> site-defaults
                                               (assoc :session nil)
                                               (update :responses dissoc :content-types)
                                               (update :security dissoc :content-type-options :anti-forgery)))
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
