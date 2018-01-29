(ns ataru.virkailija.virkailija-routes
  (:require [ataru.application.review-states :as review-states]
            [ataru.applications.application-access-control :as access-controlled-application]
            [ataru.applications.application-service :as application-service]
            [ataru.applications.application-store :as application-store]
            [ataru.applications.excel-export :as excel]
            [ataru.applications.permission-check :as permission-check]
            [ataru.cache.cache-service :as cache]
            [ataru.config.core :refer [config]]
            [ataru.config.url-helper :as url-helper]
            [ataru.dob :as dob]
            [ataru.files.file-store :as file-store]
            [ataru.forms.form-access-control :as access-controlled-form]
            [ataru.forms.form-store :as form-store]
            [ataru.haku.haku-service :as haku-service]
            [ataru.information-request.information-request-service :as information-request]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.middleware.cache-control :as cache-control]
            [ataru.middleware.session-store :refer [create-store]]
            [ataru.middleware.session-timeout :as session-timeout]
            [ataru.middleware.user-feedback :as user-feedback]
            [ataru.person-service.person-integration :as person-integration]
            [ataru.person-service.person-service :as person-service]
            [ataru.schema.form-schema :as ataru-schema]
            [ataru.statistics.statistics-service :as statistics-service]
            [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [ataru.tarjonta-service.tarjonta-service :as tarjonta-service]
            [ataru.util.client-error :as client-error]
            [ataru.virkailija.authentication.auth-middleware :as auth-middleware]
            [ataru.virkailija.authentication.auth-routes :refer [auth-routes]]
            [ataru.virkailija.authentication.auth-utils :as auth-utils]
            [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit]
            [ataru.virkailija.user.session-organizations :refer [organization-list]]
            [cheshire.core :as json]
            [cheshire.generate :refer [add-encoder]]
            [clojure.core.match :refer [match]]
            [clojure.java.io :as io]
            [clout.core :as clout]
            [com.stuartsierra.component :as component]
            [compojure.api.exception :as ex]
            [compojure.api.sweet :as api]
            [compojure.response :refer [Renderable]]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [manifold.deferred]                             ;; DO NOT REMOVE! extend-protocol below breaks otherwise!
            [medley.core :refer [map-kv]]
            [org.httpkit.client :as http]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.logger :refer [wrap-with-logger] :as middleware-logger]
            [ring.middleware.session :as ring-session]
            [ring.swagger.json-schema :as json-schema]
            [ring.util.http-response :refer [ok internal-server-error not-found bad-request content-type set-cookie] :as response]
            [ring.util.response :refer [redirect header]]
            [schema.core :as s]
            [selmer.parser :as selmer]
            [taoensso.timbre :refer [spy debug error warn info]])
  (:import java.time.ZonedDateTime
           java.time.format.DateTimeFormatter))

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

(add-encoder ZonedDateTime
             (fn [d json-generator]
               (.writeString
                json-generator
                (.format d DateTimeFormatter/ISO_OFFSET_DATE_TIME))))

(defmethod json-schema/convert-class ZonedDateTime [_ _] {:type "string"})

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
  [filename js-config]
  (if (:dev? env)
    (selmer/render-file filename {:config (json/generate-string js-config)})
    (not-found "Not found")))

(defn- wrap-database-backed-session [handler]
  (ring-session/wrap-session handler
                             {:root "/lomake-editori"
                              :cookie-attrs {:secure (not (:dev? env))}
                              :store (create-store)}))

(api/defroutes test-routes
  (api/undocumented
   (api/GET "/virkailija-test.html" []
            (if (:dev? env)
              (render-file-in-dev "templates/virkailija-test.html" {})
              (route/not-found "Not found")))
   (api/GET "/virkailija-question-group-test.html" []
     (if (:dev? env)
       (render-file-in-dev "templates/virkailija-question-group-test.html" {})
       (route/not-found "Not found")))
   (api/GET "/virkailija-question-group-application-handling-test.html" []
     (if (:dev? env)
       (render-file-in-dev "templates/virkailija-question-group-application-handling-test.html" {:form-key (form-store/get-latest-form-by-name "Kysymysryhmä: testilomake")})
       (route/not-found "Not found")))
   (api/GET "/spec/:filename.js" [filename]
            (if (:dev? env)
              (render-file-in-dev (str "spec/" filename ".js") {})
              (route/not-found "Not found")))))

(defn api-routes [{:keys [organization-service
                          tarjonta-service
                          ohjausparametrit-service
                          virkailija-tarjonta-service
                          cache-service
                          person-service]}]
    (api/context "/api" []
                 :tags ["form-api"]

                 (api/GET "/user-info" {session :session}
                          (ok {:username (-> session :identity :username)
                               :organizations (organization-list session)}))

                 (api/GET "/forms" {session :session}
                   :summary "Return forms for editor view. Also used by external services.
                             In practice this is Tarjonta system only for now.
                             Return forms authorized with editor right (:form-edit)"
                   :return {:forms [ataru-schema/Form]}
                   (ok (access-controlled-form/get-forms-for-editor session organization-service)))

                 (api/GET "/forms-in-use" {session :session}
                          :summary "Return a map of form->hakus-currently-in-use-in-tarjonta-service"
                          :return {s/Str {s/Str {:haku-oid s/Str :haku-name ataru-schema/LocalizedStringOptional}}}
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

                 (api/GET "/update-persons" []
                   (doseq [application-id (map :id (application-store/get-application-keys))]
                     (person-integration/upsert-and-log-person person-service application-id))
                   (ok (str "Updated persons for applications")))

                 (api/context "/applications" []
                   :tags ["applications-api"]

                   (api/POST "/mass-update" {session :session}
                     :body [body {:application-keys [s/Str]
                                  :hakukohde-oid    (s/maybe s/Str)
                                  :from-state       (apply s/enum (map first review-states/application-hakukohde-processing-states))
                                  :to-state         (apply s/enum (map first review-states/application-hakukohde-processing-states))}]
                     :summary "Update list of application-hakukohde with given state to new state"
                     (ok (application-service/mass-update-application-states session
                                                                             organization-service
                                                                             (:application-keys body)
                                                                             (:hakukohde-oid body)
                                                                             (:from-state body)
                                                                             (:to-state body))))

                  (api/GET "/list" {session :session}
                           :query-params [{formKey      :- s/Str nil}
                                          {hakukohdeOid :- s/Str nil}
                                          {hakuOid      :- s/Str nil}
                                          {ssn          :- s/Str nil}
                                          {dob          :- s/Str nil}
                                          {email        :- s/Str nil}
                                          {name         :- s/Str nil}]
                           :summary "Return applications header-level info for form"
                           :return {:applications [ataru-schema/ApplicationInfo]}
                           (let [applications (-> (cond
                                                    (some? formKey)
                                                    (application-service/get-application-list-by-form formKey session organization-service)

                                                    (some? hakukohdeOid)
                                                    (access-controlled-application/get-application-list-by-hakukohde hakukohdeOid session organization-service)

                                                    (some? hakuOid)
                                                    (access-controlled-application/get-application-list-by-haku hakuOid session organization-service)

                                                    (some? ssn)
                                                    (access-controlled-application/get-application-list-by-ssn ssn session organization-service)

                                                    (some? dob)
                                                    (let [dob (dob/str->dob dob)]
                                                      (access-controlled-application/get-application-list-by-dob dob session organization-service))

                                                    (some? email)
                                                    (access-controlled-application/get-application-list-by-email email session organization-service)

                                                    (some? name)
                                                    (access-controlled-application/get-application-list-by-name name session organization-service))
                                                  :applications)
                                 persons      (->> (person-service/get-persons person-service (distinct (keep :person-oid applications)))
                                                   (reduce (fn [res person]
                                                             (assoc res (:oidHenkilo person) person))
                                                           {}))]
                             (response/ok {:applications (for [application applications
                                                               :let [person      (get persons (:person-oid application))
                                                                     yksiloity   (or (-> person :yksiloity)
                                                                                     (-> person :yksiloityVTJ))
                                                                     person-info (if yksiloity
                                                                                   {:preferred-name (:kutsumanimi person)
                                                                                    :last-name      (:sukunimi person)}
                                                                                   (select-keys application [:preferred-name :last-name]))]]
                                                           (-> application
                                                               (assoc :person person-info)
                                                               (dissoc :person-oid :preferred-name :last-name)))})))
                  (api/GET "/virkailija-settings" {session :session}
                    :return ataru-schema/VirkailijaSettings
                    (ok (virkailija-edit/get-review-settings session)))

                  (api/POST "/review-setting" {session :session}
                    :body [review-setting ataru-schema/ReviewSetting]
                    :return ataru-schema/ReviewSetting
                    (ok (virkailija-edit/set-review-setting review-setting session)))

                  (api/GET "/:application-key" {session :session}
                    :path-params [application-key :- String]
                    :summary "Return application details needed for application review, including events and review data"
                    :return {:application          ataru-schema/ApplicationWithPerson
                             :events               [ataru-schema/Event]
                             :review               ataru-schema/Review
                             :review-notes         [ataru-schema/ReviewNote]
                             :hakukohde-reviews    ataru-schema/HakukohdeReviews
                             :form                 ataru-schema/FormWithContent
                             :information-requests [ataru-schema/InformationRequest]}
                    (ok (application-service/get-application-with-human-readable-koodis application-key
                                                                                        session
                                                                                        organization-service
                                                                                        tarjonta-service
                                                                                        ohjausparametrit-service
                                                                                        person-service)))

                   (api/GET "/:application-key/modify" {session :session}
                     :path-params [application-key :- String]
                     :summary "Get HTTP redirect response for modifying a single application in Hakija side"
                     (if-let [virkailija-credentials (virkailija-edit/create-virkailija-credentials session application-key)]
                       (let [modify-url (str (-> config :public-config :applicant :service_url)
                                             "/hakemus?virkailija-secret="
                                             (:secret virkailija-credentials))]
                         (response/temporary-redirect modify-url))
                       (response/bad-request)))

                   (api/POST "/:application-key/resend-modify-link" {session :session}
                     :path-params [application-key :- String]
                     :summary "Send the modify application link to the applicant via email"
                     :return ataru-schema/Event
                     (if-let [resend-event (application-service/send-modify-application-link-email
                                            application-key
                                            session
                                            organization-service
                                            tarjonta-service)]
                       (response/ok resend-event)
                       (response/bad-request)))

                   (api/POST "/notes" {session :session}
                     :summary "Add new review note for the application"
                     :return ataru-schema/ReviewNote
                     :body [note {:notes           s/Str
                                  :application-key s/Str}]
                     (if-let [note (application-service/add-review-note note session organization-service)]
                       (response/ok note)
                       (response/bad-request)))

                   (api/DELETE "/notes/:note-id" []
                     :summary "Remove note"
                     :return {:id s/Int}
                     :path-params [note-id :- s/Int]
                     (if-let [note-id (application-service/remove-review-note note-id)]
                       (response/ok {:id note-id})
                       (response/bad-request)))

                   (api/PUT "/review" {session :session}
                     :summary "Update existing application review"
                     :body [review ataru-schema/Review]
                     :return {:review            ataru-schema/Review
                              :events            [ataru-schema/Event]
                              :hakukohde-reviews ataru-schema/HakukohdeReviews}
                     (ok
                       (application-service/save-application-review
                         review
                         session
                         organization-service)))

                   (api/POST "/information-request" {session :session}
                     :body [information-request ataru-schema/InformationRequest]
                     :summary "Send an information request to an applicant"
                     :return ataru-schema/InformationRequest
                     (ok (information-request/store information-request
                                                    session)))

                   (api/POST "/excel" {session :session}
                     :form-params [application-keys :- s/Str
                                   filename :- s/Str
                                   {selected-hakukohde :- s/Str nil}
                                   {skip-answers :- s/Bool false}
                                   {CSRF :- s/Str nil}]
                     :summary "Generate Excel sheet for applications given by ids (and which the user has rights to view)"
                     {:status  200
                      :headers {"Content-Type"        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                "Content-Disposition" (str "attachment; filename=" (excel/create-filename filename))}
                      :body    (application-service/get-excel-report-of-applications-by-key
                                 (clojure.string/split application-keys #",")
                                 selected-hakukohde
                                 skip-answers
                                 session
                                 organization-service
                                 tarjonta-service
                                 ohjausparametrit-service)}))

                 (api/context "/cache" []
                   (api/POST "/clear/:cache" {session :session}
                     :path-params [cache :- s/Str]
                     :summary "Clear an entire cache map of its entries"
                     {:status 200
                      :body   (do (cache/cache-clear cache-service (keyword cache))
                                  {})})
                   (api/POST "/remove/:cache/:key" {session :session}
                     :path-params [cache :- s/Str
                                   key :- s/Str]
                     :summary "Remove an entry from cache map"
                     {:status 200
                      :body   (do (cache/cache-remove cache-service (keyword cache) key)
                                  {})}))

                 (api/GET "/haut" {session :session}
                          :summary "List haku and hakukohde information found for applications stored in system"
                          :return ataru-schema/Haut
                          (ok {:tarjonta-haut (haku-service/get-haut session organization-service tarjonta-service)
                               :direct-form-haut (haku-service/get-direct-form-haut session organization-service)}))

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

                 (api/context "/tarjonta" []
                              :tags ["tarjonta-api"]
                              (api/GET "/haku/:oid" []
                                       :path-params [oid :- (api/describe s/Str "Haku OID")]
                                       :return ataru-schema/Haku
                                       (if-let [haku (tarjonta/get-haku
                                                      tarjonta-service
                                                      oid)]
                                         (-> (tarjonta-service/parse-haku haku)
                                             ok
                                             (header "Cache-Control" "public, max-age=300"))
                                         (internal-server-error {:error "Internal server error"})))
                              (api/GET "/hakukohde" []
                                       :query-params [organizationOid :- (api/describe s/Str "Organization OID")
                                                      hakuOid :- (api/describe s/Str "Haku OID")]
                                       :return [ataru-schema/Hakukohde]
                                       (if-let [hakukohteet (tarjonta/hakukohde-search
                                                             tarjonta-service
                                                             hakuOid
                                                             organizationOid)]
                                         (-> hakukohteet
                                             ok
                                             (header "Cache-Control" "public, max-age=300"))
                                         (internal-server-error {:error "Internal server error"}))))

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
                     (if-let [file-response (file-store/get-file key)]
                       (header (ok (:body file-response))
                               "Content-Disposition"
                               (:content-disposition file-response))
                       (not-found))))

                 (api/context "/statistics" []
                   :tags ["statistics-api"]
                   (api/GET "/applications/:time-period" []
                            :path-params [time-period :- (api/describe (s/enum "month" "week" "day") "One of: month, week, day")]
                            :summary "Get info about number of submitted applications for past time period"
                            (ok (statistics-service/get-application-stats cache-service (keyword time-period)))))

                 (api/POST "/checkpermission" []
                           :body [dto ataru-schema/PermissionCheckDto]
                           :return ataru-schema/PermissionCheckResponseDto
                           (ok (permission-check/check dto)))

                 (api/context "/external" []
                   :tags ["external-api"]
                   (api/GET "/omatsivut/applications/:person-oid" {session :session}
                            :summary "Get latest versions of every application belonging to a user with given person OID"
                            :path-params [person-oid :- (api/describe s/Str "Person OID")]
                            :return [ataru-schema/OmatsivutApplication]
                            (if-let [applications (access-controlled-application/omatsivut-applications
                                                   organization-service
                                                   session
                                                   person-oid)]
                              (response/ok applications)
                              (response/unauthorized {:error "Unauthorized"})))
                   (api/GET "/onr/applications/:person-oid" {session :session}
                            :path-params [person-oid :- (api/describe s/Str "Person OID")]
                            :return [ataru-schema/OnrApplication]
                            (if-let [applications (access-controlled-application/onr-applications
                                                   organization-service
                                                   session
                                                   person-oid)]
                              (response/ok applications)
                              (response/unauthorized {:error "Unauthorized"})))
                   (api/GET "/hakurekisteri/applications" {session :session}
                     :summary "Get the latest versions of applications."
                     :query-params [{hakuOid :- s/Str nil}
                                    {hakukohdeOids :- [s/Str] nil}
                                    {hakijaOids :- [s/Str] nil}
                                    {modifiedAfter :- s/Str nil}]
                     :return [ataru-schema/HakurekisteriApplication]
                     (if (every? nil? [hakuOid hakukohdeOids hakijaOids modifiedAfter])
                       (response/bad-request {:error "No search terms provided."})
                       (if-let [applications (access-controlled-application/hakurekisteri-applications
                                               organization-service
                                               session
                                               hakuOid
                                               hakukohdeOids
                                               hakijaOids
                                               modifiedAfter)]
                         (response/ok applications)
                         (response/unauthorized {:error "Unauthorized"}))))
                   (api/GET "/applications" {session :session}
                            :summary "Get the latest versions of applications in haku or hakukohde or by oids."
                            :query-params [{hakuOid :- s/Str nil}
                                           {hakukohdeOid :- s/Str nil}
                                           {hakemusOids :- [s/Str] nil}]
                            :return [ataru-schema/VtsApplication]
                            (if (and (nil? hakuOid)
                                     (nil? hakemusOids))
                              (response/bad-request {:error "No haku or application oid provided."})
                              (if-let [applications (access-controlled-application/external-applications
                                                     organization-service
                                                     session
                                                     hakuOid
                                                     hakukohdeOid
                                                     hakemusOids)]
                                (response/ok applications)
                                (response/unauthorized {:error "Unauthorized"}))))
                   (api/GET "/persons" {session :session}
                            :summary "Get application-oid <-> person-oid mapping for haku or hakukohdes"
                            :query-params [hakuOid :- s/Str
                                           {hakukohdeOids :- [s/Str] nil}]
                            :return {s/Str s/Str}
                            (if-let [mapping (access-controlled-application/application-key-to-person-oid
                                              organization-service
                                              session
                                              hakuOid
                                              hakukohdeOids)]
                              (response/ok mapping)
                              (response/unauthorized {:error "Unauthorized"})))
                   (api/GET "/odw" {session :session}
                     :summary "Gst odw report"
                     :query-params [fromDate :- s/Str]
                     :return [{s/Keyword s/Any}]
                     (if-let [applications (access-controlled-application/get-applications-for-odw
                                             organization-service
                                             session
                                             person-service
                                             fromDate)]
                       (response/ok applications)
                       (response/unauthorized {:error "Unauthorized"})))
                   (api/GET "/tilastokeskus" {session :session}
                     :summary "Get application info for tilastokeskus"
                     :query-params [hakuOid :- s/Str]
                     :return [ataru-schema/TilastokeskusApplication]
                     (if-let [applications (access-controlled-application/get-applications-for-tilastokeskus organization-service
                                                                                                             session
                                                                                                             hakuOid)]
                       (response/ok applications)
                       (response/unauthorized {:error "Unauthorized"}))))))

(api/defroutes resource-routes
  (api/undocumented
    (route/resources "/lomake-editori")))

(api/defroutes rich-routes
  (api/undocumented
    (api/GET "/favicon.ico" []
      (-> "public/images/rich.jpg" io/resource))))

(defn- proxy-request [service-path request]
  (let [prefix   (str "https://" (get-in config [:urls :virkailija-host]) service-path)
        path     (-> request :params :*)
        response @(http/get (str prefix path) {:headers (:headers request)})]
    (assoc
     response
     ;; http-kit returns header names as keywords, but Ring requires strings :(
     :headers (map-kv
               (fn [header-kw header-value] [(name header-kw) header-value])
               (:headers request)))))

;; All these paths are required to be proxied by raamit when running locally
;; in your dev-environment. They will get proxied to the correct test environment
;; (e.g. untuva or qa)
(api/defroutes local-raami-routes
  (api/undocumented
   (api/GET "/virkailija-raamit/*" request
            :query-params [{fingerprint :- [s/Str] nil}]
            (proxy-request "/virkailija-raamit/" request))
   (api/GET "/authentication-service/*" request
            (proxy-request "/authentication-service/" request))
   (api/GET "/cas/*" request
            (proxy-request "/cas/" request))
   (api/GET "/lokalisointi/*" request
            (proxy-request "/lokalisointi/" request))))

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

(api/defroutes dashboard-routes
  (api/undocumented
    (api/GET "/dashboard" []
      (selmer/render-file "templates/dashboard.html" {}))))

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
                              (when (:dev? env) local-raami-routes)
                              resource-routes
                              (api/context "/lomake-editori" []
                                test-routes
                                dashboard-routes
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
