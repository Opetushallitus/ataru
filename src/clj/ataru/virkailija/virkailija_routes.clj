(ns ataru.virkailija.virkailija-routes
  (:require [ataru.log.access-log :as access-log]
            [ataru.applications.automatic-eligibility :as automatic-eligibility]
            [ataru.applications.automatic-payment-obligation :as automatic-payment-obligation]
            [ataru.application.review-states :as review-states]
            [ataru.applications.application-access-control :as access-controlled-application]
            [ataru.applications.application-service :as application-service]
            [ataru.applications.application-store :as application-store]
            [ataru.applications.excel-export :as excel]
            [ataru.applications.permission-check :as permission-check]
            [ataru.background-job.job :as job]
            [ataru.email.application-email-confirmation :as email]
            [ataru.email.email-store :as email-store]
            [ataru.cache.cache-service :as cache]
            [ataru.cache.caches :as caches]
            [ataru.config.core :refer [config]]
            [ataru.config.url-helper :as url-helper]
            [ataru.dob :as dob]
            [ataru.files.file-store :as file-store]
            [ataru.forms.form-access-control :as access-controlled-form]
            [ataru.forms.form-store :as form-store]
            [ataru.forms.hakukohderyhmat :as hakukohderyhmat]
            [ataru.haku.haku-service :as haku-service]
            [ataru.information-request.information-request-service :as information-request]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.middleware.cache-control :as cache-control]
            [ataru.middleware.session-store :refer [create-store]]
            [ataru.middleware.session-timeout :as session-timeout]
            [ataru.middleware.session-client :as session-client]
            [ataru.middleware.user-feedback :as user-feedback]
            [ataru.person-service.person-integration :as person-integration]
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
            [ataru.organization-service.session-organizations :refer [organization-list] :as session-orgs]
            [ataru.organization-service.organization-selection :as organization-selection]
            [ataru.organization-service.organization-client :as organization-client]
            [ataru.organization-service.organization-service :as organization-service]
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
            [manifold.deferred] ;; DO NOT REMOVE! extend-protocol below breaks otherwise!
            [medley.core :refer [map-kv]]
            [org.httpkit.client :as http]
            [ataru.lokalisointi-service.lokalisointi-service :refer [get-virkailija-texts]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.logger :refer [wrap-with-logger] :as middleware-logger]
            [ring.middleware.session :as ring-session]
            [ring.util.io :as ring-io]
            [ring.swagger.json-schema :as json-schema]
            [ring.util.http-response :refer [ok internal-server-error not-found bad-request content-type set-cookie] :as response]
            [ring.util.response :refer [redirect header] :as ring-util]
            [schema.core :as s]
            [selmer.parser :as selmer]
            [taoensso.timbre :refer [spy debug error warn info]]
            [ataru.organization-service.user-rights :as user-rights]
            [ataru.util :as util])
  (:import java.util.Locale
           java.time.ZonedDateTime
           org.joda.time.format.DateTimeFormat
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

(defn- parse-if-unmodified-since
  [s]
  (try
    (.parseDateTime (-> "EEE, dd MMM yyyy HH:mm:ss 'GMT'"
                        DateTimeFormat/forPattern
                        .withZoneUTC
                        (.withLocale Locale/US))
                    s)
    (catch Exception e
      (throw (new IllegalArgumentException
                  (str "Ei voitu jäsentää otsaketta If-Unmodified-Since: " s))))))

(defn- format-last-modified
  [d]
  (.print (-> "EEE, dd MMM yyyy HH:mm:ss 'GMT'"
              DateTimeFormat/forPattern
              .withZoneUTC
              (.withLocale Locale/US))
          (-> d
              .millisOfSecond
              .roundFloorCopy
              (.plusSeconds 1))))

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
   (api/GET "/virkailija-selection-limit-test.html" []
     (if (:dev? env)
       (render-file-in-dev "templates/virkailija-selection-limit-test.html" {})
       (route/not-found "Not found")))
   (api/GET "/virkailija-with-hakukohde-organization-test.html" []
     (if (:dev? env)
       (render-file-in-dev "templates/virkailija-with-hakukohde-organization-test.html" {})
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
                          job-runner
                          ohjausparametrit-service
                          virkailija-tarjonta-service
                          localizations-cache
                          statistics-month-cache
                          statistics-week-cache
                          statistics-day-cache
                          koodisto-cache
                          person-service
                          get-haut-cache]
                   :as dependencies}]
  (api/context "/api" []
    :tags ["form-api"]

    (api/GET "/user-info" {session :session}
      (ok {:organizations         (organization-list session)
           :oid                   (-> session :identity :oid)
           :name                  (format "%s %s" (-> session :identity :first-name) (-> session :identity :last-name))
           :lang                  (-> session :identity :lang)
           :selected-organization (:selected-organization session)
           :superuser?            (-> session :identity :superuser)}))

    (api/GET "/forms" {session :session}
      :summary "Return forms for editor view. Also used by external services.
                             In practice this is Tarjonta system only for now.
                             Return forms authorized with editor right (:form-edit)"
      :return {:forms [ataru-schema/Form]}
      (ok (access-controlled-form/get-forms-for-editor session virkailija-tarjonta-service organization-service)))

    (api/GET "/forms-in-use" {session :session}
      :summary "Return a map of form->hakus-currently-in-use-in-tarjonta-service"
      :return {s/Str {s/Str {:haku-oid s/Str :haku-name ataru-schema/LocalizedStringOptional}}}
      (ok (tarjonta/get-forms-in-use virkailija-tarjonta-service session)))

    (api/GET "/forms/:id" []
      :path-params [id :- Long]
      :return ataru-schema/FormWithContent
      :summary "Get content for form"
      (ok (form-store/fetch-form id)))

    (api/PUT "/forms/:id" {session :session}
      :summary "Edit form content"
      :path-params [id :- Long]
      :body [operations [ataru-schema/Operation]]
      (access-controlled-form/edit-form-with-operations id operations session virkailija-tarjonta-service organization-service)
      (ok {}))

    (api/PUT "/forms/:id/lock/:operation" {session :session}
      :path-params [id :- Long
                    operation :- (s/enum "open" "close")]
      :return {:locked    (s/maybe org.joda.time.DateTime)
               :id        Long}
      :summary "Toggle form locked state"
      (ok (access-controlled-form/update-form-lock id operation session virkailija-tarjonta-service organization-service)))

    (api/DELETE "/forms/:id" {session :session}
      :path-params [id :- Long]
      :summary "Mark form as deleted"
      (ok (access-controlled-form/delete-form id session virkailija-tarjonta-service organization-service)))

    (api/POST "/forms" {session :session}
      :summary "Persist changed form."
      :body [form ataru-schema/FormWithContent]
      (ok (access-controlled-form/post-form form session virkailija-tarjonta-service organization-service)))

    (api/POST "/client-error" []
      :summary "Log client-side errors to server log"
      :body [error-details client-error/ClientError]
      (do
        (client-error/log-client-error error-details)
        (ok {})))

    (api/POST "/email-template/:form-key/previews" []
      :path-params [form-key :- s/Str]
      :body [body {:contents [ataru-schema/EmailTemplate]}]
      (ok (email/preview-submit-emails (:contents body))))

    (api/POST "/email-templates/:form-key" {session :session}
      :path-params [form-key :- s/Str]
      :body [body {:contents [ataru-schema/EmailTemplate]}]
      (ok (email/store-email-templates form-key session (:contents body))))

    (api/GET "/email-templates/:form-key" []
      :path-params [form-key :- s/Str]
      (ok (email/get-email-templates form-key)))

    (api/context "/preview" []
      (api/GET "/haku/:haku-oid" {session :session}
        :path-params [haku-oid :- s/Str]
        :query-params [lang :- s/Str]
        (if-let [secret (virkailija-edit/create-virkailija-create-secret session)]
          (response/temporary-redirect
           (str (-> config :public-config :applicant :service_url)
                "/hakemus/haku/" haku-oid
                "?virkailija-secret=" secret
                "&lang=" lang))
          (response/internal-server-error)))

      (api/GET "/form/:key" {session :session}
        :path-params [key :- s/Str]
        :query-params [lang :- s/Str]
        (if-let [secret (virkailija-edit/create-virkailija-create-secret session)]
          (response/temporary-redirect
           (str (-> config :public-config :applicant :service_url)
                "/hakemus/" key
                "?virkailija-secret=" secret
                "&lang=" lang))
          (response/internal-server-error))))

    (api/context "/background-jobs" []
      :tags ["background-jobs-api"]
      (api/POST "/start-automatic-eligibility-if-ylioppilas-job/:application-id" {session :session}
        :path-params [application-id :- s/Int]
        (if (get-in session [:identity :superuser])
          (do (automatic-eligibility/start-automatic-eligibility-if-ylioppilas-job
               job-runner
               application-id)
              (response/ok {}))
          (response/unauthorized {})))
      (api/POST "/start-automatic-payment-obligation-job/:person-oid" {session :session}
        :path-params [person-oid :- s/Str]
        (if (get-in session [:identity :superuser])
          (do (automatic-payment-obligation/start-automatic-payment-obligation-job
               job-runner
               person-oid)
              (response/ok {}))
          (response/unauthorized {}))))

    (api/context "/applications" []
      :tags ["applications-api"]

      (api/POST "/mass-update" {session :session}
        :body [body {:application-keys [s/Str]
                     :hakukohde-oid    (s/maybe s/Str)
                     :from-state       (apply s/enum (map first review-states/application-hakukohde-processing-states))
                     :to-state         (apply s/enum (map first review-states/application-hakukohde-processing-states))}]
        :summary "Update list of application-hakukohde with given state to new state"
        (if (application-service/mass-update-application-states
             organization-service
             tarjonta-service
             session
             (:application-keys body)
             (:hakukohde-oid body)
             (:from-state body)
             (:to-state body))
          (response/ok {:updated-count (count (:application-keys body))})
          (response/unauthorized {:error (str "Hakemusten "
                                              (clojure.string/join ", " (:application-keys body))
                                              " käsittely ei ole sallittu")})))

      (api/POST "/list" {session :session}
        :body [body ataru-schema/ApplicationQuery]
        :summary "Return applications header-level info for form"
        :return ataru-schema/ApplicationQueryResponse
        (if-let [result (application-service/query-applications-paged
                          organization-service
                          person-service
                          tarjonta-service
                          session
                          body)]
          (response/ok result)
          (response/bad-request)))

      (api/GET "/virkailija-settings" {session :session}
        :return ataru-schema/VirkailijaSettings
        (ok (virkailija-edit/get-review-settings session)))

      (api/GET "/virkailija-texts" {session :session}
        (ok (get-virkailija-texts localizations-cache)))

      (api/POST "/review-setting" {session :session}
        :body [review-setting ataru-schema/ReviewSetting]
        :return ataru-schema/ReviewSetting
        (ok (virkailija-edit/set-review-setting review-setting session)))

      (api/GET "/:application-key" {session :session}
        :path-params [application-key :- String]
        :query-params [{newest-form :- s/Bool false}]
        :summary "Return application details needed for application review, including events and review data"
        :return {:application                       ataru-schema/ApplicationWithPerson
                 :events                            [ataru-schema/Event]
                 :review                            ataru-schema/Review
                 :review-notes                      [ataru-schema/ReviewNote]
                 :attachment-reviews                ataru-schema/AttachmentReviews
                 :hakukohde-reviews                 ataru-schema/HakukohdeReviews
                 :form                              ataru-schema/FormWithContent
                 (s/optional-key :latest-form) ataru-schema/Form
                 :information-requests              [ataru-schema/InformationRequest]}
        (if-let [application (application-service/get-application-with-human-readable-koodis
                              koodisto-cache
                              application-key
                              session
                              organization-service
                              tarjonta-service
                              ohjausparametrit-service
                              person-service
                              newest-form)]
          (response/ok application)
          (response/unauthorized {:error (str "Hakemuksen "
                                              application-key
                                              " käsittely ei ole sallittu")})))

      (api/GET "/:application-key/modify" {session :session}
        :path-params [application-key :- String]
        :summary "Get HTTP redirect response for modifying a single application in Hakija side"
        (let [allowed? (access-controlled-application/applications-access-authorized? organization-service
                         tarjonta-service
                         session
                         [application-key]
                         [:edit-applications])]
          (if allowed?
            (let [virkailija-update-secret (virkailija-edit/create-virkailija-update-secret
                                             session
                                             application-key)
                  modify-url               (str (-> config :public-config :applicant :service_url)
                                             "/hakemus?virkailija-secret="
                                             virkailija-update-secret)]
              (response/temporary-redirect modify-url))
            (response/bad-request))))

      (api/GET "/:application-key/rewrite-modify" {session :session}
        :path-params [application-key :- String]
        :summary "Get HTTP redirect response for modifying a single application in Hakija side"
        (let [allowed? (and (access-controlled-application/applications-access-authorized? organization-service
                              tarjonta-service
                              session
                              [application-key]
                              [:edit-applications])
                            (-> session :identity :superuser))]
          (if allowed?
            (let [virkailija-rewrite-secret (virkailija-edit/create-virkailija-rewrite-secret
                                              session
                                              application-key)
                  modify-url                (str (-> config :public-config :applicant :service_url)
                                              "/hakemus?virkailija-secret="
                                              virkailija-rewrite-secret)]
              (response/temporary-redirect modify-url))
            (response/bad-request))))

      (api/POST "/:application-key/resend-modify-link" {session :session}
        :path-params [application-key :- String]
        :summary "Send the modify application link to the applicant via email"
        :return ataru-schema/Event
        (if-let [resend-event (application-service/send-modify-application-link-email
                                koodisto-cache application-key
                                session
                                organization-service
                                ohjausparametrit-service
                                tarjonta-service
                                job-runner)]
          (response/ok resend-event)
          (response/bad-request)))

      (api/POST "/notes" {session :session}
        :summary "Add new review note for the application"
        :return ataru-schema/ReviewNote
        :body [note {:notes                       s/Str
                     :application-key             s/Str
                     (s/optional-key :hakukohde)  s/Str
                     (s/optional-key :state-name) ataru-schema/HakukohdeReviewTypeNames}]
        (if-let [note (application-service/add-review-note
                       organization-service
                       tarjonta-service
                       session
                       note)]
          (response/ok note)
          (response/unauthorized {:error (str "Hakemuksen "
                                              (:application-key note)
                                              " käsittely ei ole sallittu")})))

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
        :return {:events [ataru-schema/Event]}
        (if-let [result (application-service/save-application-review
                         job-runner
                         organization-service
                         tarjonta-service
                         session
                         review)]
          (response/ok result)
          (response/unauthorized {:error (str "Hakemuksen "
                                              (:application-key review)
                                              " käsittely ei ole sallittu")})))

      (api/POST "/information-request" {session :session}
        :body [information-request ataru-schema/NewInformationRequest]
        :summary "Send an information request to an applicant"
        :return ataru-schema/InformationRequest
        (if (access-controlled-application/applications-access-authorized?
             organization-service
             tarjonta-service
             session
             [(:application-key information-request)]
             [:edit-applications])
          (-> (information-request/store session
                                         (assoc information-request
                                                :message-type "information-request")
                                         job-runner)
              (assoc :first-name (get-in session [:identity :first-name])
                     :last-name (get-in session [:identity :last-name]))
              response/ok)
          (response/unauthorized {:error (str "Hakemuksen "
                                              (:application-key information-request)
                                              " käsittely ei ole sallittu")})))

      (api/POST "/mass-information-request" {session :session}
        :body [body {:message-and-subject {:message s/Str
                                           :subject s/Str}
                     :application-keys    [s/Str]}]
        :summary "Send information requests to multiple applicants"
        :return {}
        (if (access-controlled-application/applications-access-authorized?
             organization-service
             tarjonta-service
             session
             (:application-keys body)
             [:edit-applications])
          (do (information-request/mass-store
               (assoc (:message-and-subject body)
                      :message-type "mass-information-request")
               (:application-keys body)
               (get-in session [:identity :oid])
               job-runner)
              (response/accepted {}))
          (response/unauthorized {:error "Hakemusten käsittely ei ole sallittu"})))

      (api/POST "/excel" {session :session}
        :form-params [application-keys :- s/Str
                      filename :- s/Str
                      {selected-hakukohde :- s/Str nil}
                      {selected-hakukohderyhma :- s/Str nil}
                      {included-ids :- s/Str ""}
                      {skip-answers :- s/Bool false}
                      {CSRF :- s/Str nil}]
        :summary "Generate Excel sheet for applications given by ids (and which the user has rights to view)"
        (let [size-limit 40000
              application-keys (json/parse-string application-keys)]
          (info "Yritetään" (count application-keys) "hakemuksen excelin luontia")
          (if (< size-limit (count application-keys))
            (response/request-entity-too-large
             {:error (str "Cannot create excel for more than " size-limit " applications")})
            (let [included-ids (not-empty (set (remove clojure.string/blank? (clojure.string/split included-ids #"\s+"))))
                  xls          (application-service/get-excel-report-of-applications-by-key
                                application-keys
                                selected-hakukohde
                                selected-hakukohderyhma
                                skip-answers
                                included-ids
                                session
                                organization-service
                                tarjonta-service
                                koodisto-cache
                                ohjausparametrit-service
                                person-service)]
              (if xls
                {:status  200
                 :headers {"Content-Type"        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                           "Content-Disposition" (str "attachment; filename=" (excel/create-filename filename))}
                 :body    xls}
                (response/bad-request))))))

      (api/GET "/:application-key/changes" {session :session}
        :summary "Get changes made to an application in version x"
        :path-params [application-key :- s/Str]
        :return [s/Any]
        (if-let [result (application-service/get-application-version-changes
                         koodisto-cache
                         organization-service
                         tarjonta-service
                         session
                         application-key)]
          (response/ok result)
          (response/unauthorized {:error (str "Hakemuksen "
                                              application-key
                                              " käsittely ei ole sallittu")}))))

    (api/context "/cache" []
      (api/POST "/clear" {session :session}
        :summary "Clear all caches"
        {:status 200
         :body   (do
                   (doseq [[key dep] dependencies
                           :when (clojure.string/ends-with? (name key) "-cache")]
                     (cache/clear-all dep))
                   {})})
      (api/POST "/clear/:cache" {session :session}
        :path-params [cache :- s/Str]
        :summary "Clear an entire cache map of its entries"
        {:status 200
         :body   (do (cache/clear-all (get dependencies (keyword (str cache "-cache"))))
                     {})})
      (api/POST "/remove/:cache/:key" {session :session}
        :path-params [cache :- s/Str
                      key :- s/Str]
        :summary "Remove an entry from cache map"
        {:status 200
         :body   (do (cache/remove-from (get dependencies (keyword (str cache "-cache"))) key)
                     {})}))

    (api/GET "/haut" {session :session}
      :query-params [{show-hakukierros-paattynyt :- s/Bool false}]
      :summary "List haku and hakukohde information found for applications stored in system"
      :return ataru-schema/Haut
      (ok (haku-service/get-haut ohjausparametrit-service
                                 organization-service
                                 tarjonta-service
                                 get-haut-cache
                                 session
                                 show-hakukierros-paattynyt)))

    (api/GET "/haku" {session :session}
      :query-params [{haku-oid :- s/Str nil}
                     {hakukohde-oid :- s/Str nil}]
      :summary "List haku and hakukohde information found for applications stored in system"
      :return ataru-schema/Haut
      (let [haku-oid (or haku-oid (:haku-oid (tarjonta/get-hakukohde tarjonta-service hakukohde-oid)))]
        (if (some? haku-oid)
          (-> {:tarjonta-haut    {}
               :direct-form-haut {}
               :haut             {haku-oid (tarjonta-service/parse-haku
                                            (tarjonta/get-haku tarjonta-service
                                                               haku-oid))}
               :hakukohteet      (util/group-by-first :oid (tarjonta/hakukohde-search
                                                            tarjonta-service
                                                            haku-oid
                                                            nil))
               :hakukohderyhmat  (util/group-by-first :oid (organization-service/get-hakukohde-groups organization-service))}
              response/ok
              (response/header "Cache-Control" "public, max-age=300"))
          (response/bad-request))))

    (api/context "/koodisto" []
      :tags ["koodisto-api"]
      (api/GET "/:koodisto-uri/:version" [koodisto-uri version]
        :path-params [koodisto-uri :- s/Str version :- Long]
        :return s/Any
        (let [koodi-options (koodisto/get-koodisto-options koodisto-cache koodisto-uri version)]
          (ok koodi-options))))

    (api/context "/organization" []
      :tags ["organization-api"]
      (api/GET "/hakukohderyhmat" []
        :return [ataru-schema/Hakukohderyhma]
        (->
         (organization-service/get-hakukohde-groups organization-service)
         ok
         (header "Cache-Control" "public, max-age=300")))

      (api/GET "/user-organizations" {session :session}
        :query-params [{query :- s/Str nil}
                       organizations :- s/Bool
                       hakukohde-groups :- s/Bool
                       results-page :- s/Int]
        (ok (organization-selection/query-organization
              organization-service
              session
              query
              organizations
              hakukohde-groups
              results-page)))

      (api/POST "/user-organization/:oid" {session :session}
        :path-params [oid :- s/Str]
        :query-params [{rights :- [user-rights/Right] nil}]
        (if-let [selected-organization (organization-selection/select-organization organization-service session oid rights)]
          (-> (ok selected-organization)
              (assoc :session (assoc session :selected-organization selected-organization)))
          (bad-request {})))

      (api/DELETE "/user-organization" {session :session}
        (-> (ok {})
            (assoc :session (dissoc session :selected-organization)))))

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
        :return [ataru-schema/HakukohdeSearchResult]
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
      (api/POST "/metadata" []
        :body-params [keys :- (api/describe [s/Str] "File keys")]
        :summary "Get metadata for one or more files"
        :return [ataru-schema/File]
        (if-let [resp (file-store/get-metadata keys)]
          (ok resp)
          (not-found)))
      (api/GET "/content/:key" []
        :path-params [key :- (api/describe s/Str "File key")]
        :summary "Download a file"
        (if-let [file-response (file-store/get-file key)]
          (header (ok (:body file-response))
            "Content-Disposition"
            (:content-disposition file-response))
          (not-found)))
      (api/POST "/zip" []
        :form-params [keys :- s/Str
                      {CSRF :- s/Str nil}]
        :summary "Download files as a ZIP archive"
        (let [keys (json/parse-string keys)]
          (->
           (ring-util/response
            (ring-io/piped-input-stream
             (fn [out] (file-store/get-file-zip keys out))))
           (header "Content-Disposition" (str "attachment; filename=" "attachments.zip"))))))

    (api/context "/statistics" []
      :tags ["statistics-api"]
      (api/GET "/applications/:time-period" []
        :path-params [time-period :- (api/describe (s/enum "month" "week" "day") "One of: month, week, day")]
        :summary "Get info about number of submitted applications for past time period"
        (ok (statistics-service/get-application-stats
             statistics-month-cache
             statistics-week-cache
             statistics-day-cache
             (keyword time-period)))))

    (api/POST "/checkpermission" []
      :body [dto ataru-schema/PermissionCheckDto]
      :return ataru-schema/PermissionCheckResponseDto
      (ok (permission-check/check tarjonta-service dto)))

    (api/context "/rajaavat-hakukohderyhmat" []
      :tags ["rajaavat-hakukohderyhmat"]
      (api/GET "/:haku-oid" {session :session}
        :summary "Get configurations for rajaavat hakukohderyhmät in given haku"
        :path-params [haku-oid :- (api/describe s/Str "Haku OID")]
        :return [ataru-schema/RajaavaHakukohderyhma]
        (let [{:keys [ryhmat last-modified]} (hakukohderyhmat/rajaavat-hakukohderyhmat haku-oid)]
          (cond-> (response/ok ryhmat)
                  (some? last-modified)
                  (response/header "Last-Modified"
                                   (format-last-modified last-modified)))))
      (api/PUT "/:haku-oid/ryhma/:hakukohderyhma-oid" {session :session}
        :summary "Set configuration for rajaava hakukohderyhmä in given haku"
        :path-params [haku-oid :- (api/describe s/Str "Haku OID")
                      hakukohderyhma-oid :- (api/describe s/Str "Hakukohderyhmä OID")]
        :header-params [{if-unmodified-since :- s/Str nil}
                        {if-none-match :- s/Str nil}]
        :body [ryhma ataru-schema/RajaavaHakukohderyhma]
        :return ataru-schema/RajaavaHakukohderyhma
        (try
          (let [if-unmodified-since (if (= "*" if-none-match)
                                      nil
                                      (parse-if-unmodified-since if-unmodified-since))
                put                 (fn []
                                      (if-let [{:keys [ryhma last-modified]}
                                               (if (some? if-unmodified-since)
                                                 (hakukohderyhmat/update-rajaava-hakukohderyhma
                                                  ryhma
                                                  if-unmodified-since)
                                                 (hakukohderyhmat/insert-rajaava-hakukohderyhma
                                                  ryhma))]
                                        (response/header (response/ok ryhma)
                                                         "Last-Modified" (format-last-modified last-modified))
                                        (response/conflict {:error (if (some? if-unmodified-since)
                                                                     (str "Hakukohderyhma modified since " if-unmodified-since)
                                                                     "Hakukohderyhma exists")})))]
            (session-orgs/run-org-authorized
             session
             organization-service
             [:form-edit]
             #(response/unauthorized {:error "Unauthorized"})
             (fn [_] (put))
             put))
          (catch IllegalArgumentException e
            (response/bad-request {:error (.getMessage e)}))))
      (api/DELETE "/:haku-oid/ryhma/:hakukohderyhma-oid" {session :session}
        :summary "Delete configuration of rajaava hakukohderyhmä in given haku"
        :path-params [haku-oid :- (api/describe s/Str "Haku OID")
                      hakukohderyhma-oid :- (api/describe s/Str "Hakukohderyhmä OID")]
        :return ataru-schema/RajaavaHakukohderyhma
        (let [delete (fn []
                       (hakukohderyhmat/delete-rajaava-hakukohderyhma
                        haku-oid
                        hakukohderyhma-oid)
                       (response/no-content))]
          (session-orgs/run-org-authorized
           session
           organization-service
           [:form-edit]
           #(response/unauthorized {:error "Unauthorized"})
           (fn [_] (delete))
           delete))))

    (api/context "/priorisoivat-hakukohderyhmat" []
      :tags ["priorisoivat-hakukohderyhmat"]
      (api/GET "/:haku-oid" {session :session}
        :summary "Get configurations for priorisoivat hakukohderyhmät in given haku"
        :path-params [haku-oid :- (api/describe s/Str "Haku OID")]
        :return [ataru-schema/PriorisoivaHakukohderyhma]
        (let [{:keys [ryhmat last-modified]} (hakukohderyhmat/priorisoivat-hakukohderyhmat
                                              tarjonta-service
                                              haku-oid)]
          (cond-> (response/ok ryhmat)
                  (some? last-modified)
                  (response/header "Last-Modified"
                                   (format-last-modified last-modified)))))
      (api/PUT "/:haku-oid/ryhma/:hakukohderyhma-oid" {session :session}
        :summary "Set configuration for priorisoiva hakukohderyhmä in given haku"
        :path-params [haku-oid :- (api/describe s/Str "Haku OID")
                      hakukohderyhma-oid :- (api/describe s/Str "Hakukohderyhmä OID")]
        :header-params [{if-unmodified-since :- s/Str nil}
                        {if-none-match :- s/Str nil}]
        :body [ryhma ataru-schema/PriorisoivaHakukohderyhma]
        :return ataru-schema/PriorisoivaHakukohderyhma
        (try
          (let [if-unmodified-since (if (= "*" if-none-match)
                                      nil
                                      (parse-if-unmodified-since if-unmodified-since))
                put                 (fn []
                                      (if-let [{:keys [ryhma last-modified]}
                                               (if (some? if-unmodified-since)
                                                 (hakukohderyhmat/update-priorisoiva-hakukohderyhma
                                                  ryhma
                                                  if-unmodified-since)
                                                 (hakukohderyhmat/insert-priorisoiva-hakukohderyhma
                                                  ryhma))]
                                        (response/header (response/ok ryhma)
                                                         "Last-Modified" (format-last-modified last-modified))
                                        (response/conflict {:error (if (some? if-unmodified-since)
                                                                     (str "Hakukohderyhma modified since " if-unmodified-since)
                                                                     "Hakukohderyhma exists")})))]
            (session-orgs/run-org-authorized
             session
             organization-service
             [:form-edit]
             #(response/unauthorized {:error "Unauthorized"})
             (fn [_] (put))
             put))
          (catch IllegalArgumentException e
            (response/bad-request {:error (.getMessage e)}))))
      (api/DELETE "/:haku-oid/ryhma/:hakukohderyhma-oid" {session :session}
        :summary "Delete configuration of priorisoiva hakukohderyhmä in given haku"
        :path-params [haku-oid :- (api/describe s/Str "Haku OID")
                      hakukohderyhma-oid :- (api/describe s/Str "Hakukohderyhmä OID")]
        :return ataru-schema/PriorisoivaHakukohderyhma
        (let [delete (fn []
                       (hakukohderyhmat/delete-priorisoiva-hakukohderyhma
                        haku-oid
                        hakukohderyhma-oid)
                       (response/no-content))]
          (session-orgs/run-org-authorized
           session
           organization-service
           [:form-edit]
           #(response/unauthorized {:error "Unauthorized"})
           (fn [_] (delete))
           delete))))

    (api/context "/external" []
      :tags ["external-api"]
      (api/GET "/omatsivut/applications/:person-oid" {session :session}
        :summary "Get latest versions of every application belonging to a user with given person OID"
        :path-params [person-oid :- (api/describe s/Str "Person OID")]
        :return [ataru-schema/OmatsivutApplication]
        (if-let [applications (application-service/omatsivut-applications
                               organization-service
                               person-service
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
      (api/GET "/hakurekisteri/applications" {session :session} ;; deprecated, use /suoritusrekisteri
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
      (api/POST "/suoritusrekisteri" {session :session}
        :summary "Applications for suoritusrekisteri"
        :body-params [{hakuOid :- s/Str nil}
                      {hakukohdeOids :- [s/Str] nil}
                      {hakijaOids :- [s/Str] nil}
                      {modifiedAfter :- s/Str nil}
                      {offset :- s/Str nil}]
        :return {:applications            [ataru-schema/HakurekisteriApplication]
                 (s/optional-key :offset) s/Str}
        (cond (every? nil? [hakuOid (seq hakukohdeOids) (seq hakijaOids) modifiedAfter])
              (response/bad-request {:error "No query parameter given"})
              (session-orgs/run-org-authorized
               session
               organization-service
               [:view-applications :edit-applications]
               (fn [] true)
               (fn [oids] (not (contains? oids organization-client/oph-organization)))
               (fn [] false))
              (response/unauthorized {:error "Unauthorized"})
              :else
              (response/ok
               (application-service/suoritusrekisteri-applications
                person-service
                hakuOid
                hakukohdeOids
                hakijaOids
                modifiedAfter
                offset))))
      (api/GET "/applications" {session :session} ;; deprecated, use /valinta-tulos-service
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
                                  tarjonta-service
                                  session
                                  hakuOid
                                  hakukohdeOid
                                  hakemusOids)]
            (response/ok applications)
            (response/unauthorized {:error "Unauthorized"}))))
      (api/POST "/valinta-tulos-service" {session :session}
        :summary "Applications for valinta-tulos-service"
        :body-params [{hakuOid :- s/Str nil}
                      {hakukohdeOid :- s/Str nil}
                      {hakemusOids :- [s/Str] nil}
                      {offset :- s/Str nil}]
        :return {:applications            [{:oid           s/Str
                                            :hakuOid       s/Str
                                            :hakukohdeOids [s/Str]
                                            :henkiloOid    s/Str
                                            :asiointikieli s/Str
                                            :email         s/Str}]
                 (s/optional-key :offset) s/Str}
        (cond (and (nil? hakuOid) (nil? hakukohdeOid) (nil? (seq hakemusOids)))
              (response/bad-request {:error "No query parameter given"})
              (session-orgs/run-org-authorized
               session
               organization-service
               [:view-applications :edit-applications]
               (fn [] true)
               (fn [oids] (not (contains? oids organization-client/oph-organization)))
               (fn [] false))
              (response/unauthorized {:error "Unauthorized"})
              :else
              (response/ok
               (application-store/valinta-tulos-service-applications
                hakuOid
                hakukohdeOid
                hakemusOids
                offset))))
      (api/GET "/valinta-ui" {session :session}
        :summary "Applications for valinta-ui"
        :query-params [{hakuOid :- s/Str nil}
                       {hakukohdeOid :- s/Str nil}
                       {hakemusOids :- [s/Str] nil}
                       {name :- s/Str nil}]
        :return [ataru-schema/ValintaUiApplication]
        (if-let [queries (cond-> []
                                 (some? hakuOid)
                                 (conj (application-service/->haku-query
                                        hakuOid))
                                 (some? hakukohdeOid)
                                 (conj (application-service/->hakukohde-query
                                        tarjonta-service
                                        hakukohdeOid
                                        false))
                                 (not-empty hakemusOids)
                                 (conj (application-service/->application-oids-query
                                        hakemusOids))
                                 (some? name)
                                 (conj (application-service/->name-query
                                        name))
                                 true
                                 seq)]
          (if-let [applications (access-controlled-application/valinta-ui-applications
                                 organization-service
                                 tarjonta-service
                                 session
                                 (reduce application-service/->and-query queries))]
            (response/ok
             (->> applications
                  (map #(dissoc % :hakukohde))
                  (map #(clojure.set/rename-keys % {:haku-oid   :hakuOid
                                                    :person-oid :personOid}))))
            (response/unauthorized {:error "Unauthorized"}))
          (response/bad-request {:error "No query parameters given"})))
      (api/GET "/persons" {session :session} ;; deprecated, use /valinta-tulos-service
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
        :summary "Get odw report"
        :query-params [fromDate :- s/Str
                       {offset :- s/Int nil}
                       {limit :- s/Int nil}]
        :return [{s/Keyword s/Any}]
        (if-let [applications (access-controlled-application/get-applications-for-odw
                                organization-service
                                session
                                person-service
                                tarjonta-service
                                fromDate
                                limit
                                offset)]
          (response/ok applications)
          (response/unauthorized {:error "Unauthorized"})))
      (api/GET "/tilastokeskus" {session :session}
        :summary "Get application info for tilastokeskus"
        :query-params [hakuOid :- s/Str
                       {hakukohdeOid :- s/Str nil}]
        :return [ataru-schema/TilastokeskusApplication]
        (if-let [applications (access-controlled-application/get-applications-for-tilastokeskus organization-service
                                                                                                session
                                                                                                hakuOid
                                                                                                hakukohdeOid)]
          (response/ok applications)
          (response/unauthorized {:error "Unauthorized"})))

      (api/POST "/valintalaskenta" {session :session}
        :summary "Get application answers for valintalaskenta"
        :query-params [{hakukohdeOid :- s/Str nil}]
        :body         [applicationOids [s/Str]]
        :return [ataru-schema/ValintaApplication]
        (if (and (nil? hakukohdeOid)
                 (empty? applicationOids))
          (response/bad-request {:error "Either hakukohdeOid or nonempty list of application oids is required"})
          (match (application-service/get-applications-for-valintalaskenta
                  organization-service
                  person-service
                  session
                  hakukohdeOid
                  (not-empty applicationOids))
            {:yksiloimattomat (_ :guard empty?)
             :applications    applications}
            (response/ok applications)
            {:yksiloimattomat yksiloimattomat
             :applications    applications}
            (if (get-in config [:yksiloimattomat :allow] false)
              (do (warn "Yksilöimättömiä hakijoita")
                  (response/ok applications))
              (response/conflict
               {:error      "Yksilöimättömiä hakijoita"
                :personOids yksiloimattomat}))
            {:unauthorized _}
            (response/unauthorized {:error "Unauthorized"}))))

      (api/POST "/siirto" {session :session}
        :summary "Get applications for external systems"
        :query-params [{hakukohdeOid :- s/Str nil}]
        :body         [applicationOids [s/Str]]
        :return [ataru-schema/SiirtoApplication]
        (if (and (nil? hakukohdeOid)
                 (empty? applicationOids))
          (response/bad-request {:error "Either hakukohdeOid or nonempty list of application oids is required"})
          (match (application-service/siirto-applications
                  tarjonta-service
                  organization-service
                  person-service
                  session
                  hakukohdeOid
                  (not-empty applicationOids))
            {:yksiloimattomat (_ :guard empty?)
             :applications    applications}
            (response/ok applications)
            {:yksiloimattomat yksiloimattomat
             :applications    applications}
            (if (get-in config [:yksiloimattomat :allow] false)
              (do (warn "Yksilöimättömiä hakijoita")
                  (response/ok applications))
              (response/conflict
               {:error      "Yksilöimättömiä hakijoita"
                :personOids yksiloimattomat}))
            {:unauthorized _}
            (response/unauthorized {:error "Unauthorized"}))))

      (api/GET "/application-identifier/:application-identifier" {session :session}
        :summary "Get the application oid and person oid matching the
                  application identifier"
        :path-params [application-identifier :- s/Str]
        :return {:applicationOid s/Str
                 :personOid      s/Str}
        (if-let [application (some->> (application-service/unmask-application-key application-identifier)
                                      application-store/get-latest-application-by-key)]
          (response/ok {:applicationOid (:key application)
                        :personOid      (:person-oid application)})
          (response/not-found))))))

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
        response @(http/get (str prefix path) {:headers (dissoc (:headers request) "host")})]
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

(defn status-routes
  [system]
  (api/context "/status" []
    :tags ["status-api"]
    (api/GET "/background-jobs" []
      :return {s/Str {:total   s/Int
                      :fail    s/Int
                      :error   s/Int
                      :waiting s/Int}}
      (let [status (job/status)]
        (cond-> (dissoc status :ok)
                (:ok status)       response/ok
                (not (:ok status)) response/internal-server-error)))
    (api/GET "/caches" []
      :return s/Any
      (response/ok
       (reduce (fn [m [_ component]]
                 (if (satisfies? cache/Stats component)
                   (assoc m (:name component) (cache/stats component))
                   m))
               {}
               system)))))

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
                                (status-routes this)
                                (api/middleware [user-feedback/wrap-user-feedback
                                                 wrap-database-backed-session
                                                 auth-middleware/with-authentication]
                                  (api/middleware [session-client/wrap-session-client-headers
                                                   session-timeout/wrap-idle-session-timeout]
                                    app-routes
                                    (api-routes this))
                                  (auth-routes (:kayttooikeus-service this)
                                               (:person-service this)
                                               (:organization-service this))))
                              (api/undocumented
                                (route/not-found "Not found")))
                            (wrap-defaults (-> site-defaults
                                               (assoc :session nil)
                                               (update :responses dissoc :content-types)
                                               (update :security dissoc :content-type-options :anti-forgery)))
                            (wrap-with-logger
                              :debug identity
                              :info (fn [x] (access-log/info x))
                              :warn (fn [x] (access-log/warn x))
                              :error (fn [x] (access-log/error x))
                              :pre-logger (fn [_ _] nil)
                              :post-logger (fn [options {:keys [uri] :as request} {:keys [status] :as response} totaltime]
                                             (when (or
                                                     (>= status 400)
                                                     (clojure.string/starts-with? uri "/lomake-editori/api/"))
                                               (access-log/log options request response totaltime))))
                            (wrap-gzip)

                            (cache-control/wrap-cache-control))))

  (stop [this]
    (when
      (contains? this :routes)
      (assoc this :routes nil))))

(defn new-handler []
  (->Handler))
