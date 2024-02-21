(ns ataru.virkailija.virkailija-routes
  (:require [ataru.applications.automatic-eligibility :as automatic-eligibility]
            [ataru.tutkintojen-tunnustaminen :as tutkintojen-tunnustaminen]
            [ataru.applications.automatic-payment-obligation :as automatic-payment-obligation]
            [ataru.application.review-states :as review-states]
            [ataru.hakija.hakija-application-service :as hakija-application-service]
            [ataru.applications.application-access-control :as access-controlled-application]
            [ataru.applications.application-service :as application-service]
            [ataru.siirtotiedosto-service :as siirtotiedosto-service]
            [ataru.applications.application-store :as application-store]
            [ataru.applications.field-deadline :as field-deadline]
            [ataru.applications.excel-export :as excel]
            [ataru.hakukohde.hakukohde-store :as hakukohde-store]
            [ataru.applications.permission-check :as permission-check]
            [ataru.background-job.job :as job]
            [ataru.email.application-email-jobs :as email]
            [ataru.cache.cache-service :as cache]
            [ataru.config.core :refer [config]]
            [ataru.config.url-helper :as url-helper]
            [ataru.cypress :as cypress]
            [ataru.files.file-store :as file-store]
            [ataru.forms.form-access-control :as access-controlled-form]
            [ataru.forms.form-store :as form-store]
            [ataru.forms.hakukohderyhmat :as hakukohderyhmat]
            [ataru.haku.haku-service :as haku-service]
            [ataru.information-request.information-request-service :as information-request]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.middleware.cache-control :as cache-control]
            [ataru.middleware.session-timeout :as session-timeout]
            [clj-ring-db-session.session.session-client :as session-client]
            [ataru.maksut.maksut-protocol :as maksut-protocol]
            [ataru.middleware.user-feedback :as user-feedback]
            [ataru.schema.form-schema :as ataru-schema]
            [ataru.schema.form-element-schema :as form-schema]
            [ataru.schema.maksut-schema :as maksut-schema]
            [ataru.schema.priorisoiva-hakukohderyhma-schema :as priorisoiva-hakukohderyhma-schema]
            [ataru.statistics.statistics-service :as statistics-service]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [ataru.translations.texts :as texts]
            [ataru.util.client-error :as client-error]
            [ataru.virkailija.authentication.auth-routes :refer [auth-routes]]
            [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit]
            [ataru.organization-service.session-organizations :refer [organization-list] :as session-orgs]
            [ataru.organization-service.organization-selection :as organization-selection]
            [ataru.organization-service.organization-client :as organization-client]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.util.http-util :as http]
            [ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-protocol :as valintalaskentakoostepalvelu]
            [ataru.valintaperusteet.service :as valintaperusteet]
            [ataru.valintaperusteet.client :as valintaperusteet-client]
            [ataru.valinta-laskenta-service.valintalaskentaservice-protocol :as vls]
            [ataru.valinta-tulos-service.valintatulosservice-protocol :as vts]
            [ataru.virkailija.authentication.auth-utils :as auth-utils]
            [clj-ring-db-session.authentication.auth-middleware :as crdsa-auth-middleware]
            [cheshire.core :as json]
            [cheshire.generate :refer [add-encoder]]
            [clj-access-logging]
            [clj-stdout-access-logging]
            [clj-timbre-access-logging]
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
            [ataru.lokalisointi-service.lokalisointi-service :refer [get-virkailija-texts]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.session :as ring-session]
            [ring.util.io :as ring-io]
            [ring.swagger.json-schema :as json-schema]
            [ring.util.http-response :refer [ok internal-server-error not-found bad-request] :as response]
            [ring.util.response :refer [redirect header content-type] :as ring-util]
            [schema.core :as s]
            [selmer.parser :as selmer]
            [taoensso.timbre :as log]
            [ataru.user-rights :as user-rights]
            [ataru.util :as util]
            [ataru.hakija.hakija-form-service :as hakija-form-service]
            [ataru.temp-file-storage.temp-file-store :as temp-file-store]
            [ataru.suoritus.suoritus-service :as suoritus-service]
            [clj-time.core :as time]
            [ataru.applications.suoritus-filter :as suoritus-filter]
            [ataru.person-service.person-service :as person-service]
            [ataru.valintalaskentakoostepalvelu.pohjakoulutus-toinen-aste :as pohjakoulutus-toinen-aste]
            [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
            [cuerdas.core :as str]
            [clj-time.format :as f]
            [ataru.virkailija.virkailija-application-service :as virkailija-application-service])
  (:import java.util.Locale
           java.time.ZonedDateTime
           org.joda.time.DateTime
           org.joda.time.format.DateTimeFormat
           java.time.format.DateTimeFormatter
           (java.text SimpleDateFormat)
           (java.util Date)))

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
    (catch Exception _
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
  (let [public-config (json/generate-string (or (:public-config config) {}))]
    (-> (selmer/render-file "templates/virkailija.html"
                            {:cache-fingerprint cache-fingerprint
                             :config            public-config
                             :front-properties  (url-helper/front-json)
                             :js-bundle-name    (or (-> config :server :js-bundle-names :virkailija)
                                                    "virkailija-app.js")})
        (ok)
        (content-type "text/html"))))
(api/defroutes app-routes
  (api/undocumented
    (api/GET "/" [] (render-virkailija-page))
    (api/GET client-routes [] (render-virkailija-page))
    (api/GET client-sub-routes [] (render-virkailija-page))
    (api/GET "/virhe" [] (render-virkailija-page))))

(defn- render-file-in-dev
  [filename js-config]
  (if (:dev? env)
    (selmer/render-file filename {:config (json/generate-string js-config)})
    (not-found "Not found")))

(defn- create-wrap-database-backed-session [session-store]
  (fn [handler]
    (ring-session/wrap-session handler
                               {:root         "/lomake-editori"
                                :cookie-attrs {:secure (not (:dev? env))}
                                :store        session-store})))

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
                          valintalaskentakoostepalvelu-service
                          valintaperusteet-service
                          liiteri-cas-client
                          job-runner
                          ohjausparametrit-service
                          localizations-cache
                          maksut-service
                          statistics-month-cache
                          statistics-week-cache
                          statistics-day-cache
                          temp-file-store
                          koodisto-cache
                          person-service
                          get-haut-cache
                          audit-logger
                          application-service
                          siirtotiedosto-service
                          suoritus-service
                          valinta-laskenta-service
                          valinta-tulos-service
                          form-by-id-cache]
                   :as   dependencies}]
  (api/context "/api" []
    :tags ["form-api"]

    (api/POST "/synthetic-applications" {session :session}
      :summary "Store one or more synthetic applications"
      :body [applications [ataru-schema/SyntheticApplication]]
      (if (get-in session [:identity :superuser])
        (let [submit-results (virkailija-application-service/batch-submit-synthetic-applications
                              applications
                                {:form-by-id-cache form-by-id-cache
                                 :koodisto-cache koodisto-cache
                                 :tarjonta-service tarjonta-service
                                 :organization-service organization-service
                                 :ohjausparametrit-service ohjausparametrit-service
                                 :person-service person-service
                                 :audit-logger audit-logger
                                 :job-runner job-runner
                                 :session session})]
        (if (:success submit-results)
          (ok (:applications submit-results))
          (response/bad-request (:applications submit-results))))
         (response/unauthorized {})))

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
                             Return forms authorized with editor right (:form-edit).
                             If parameter hakukohderyhma-oid is supplied returns
                             only forms using that hakukohderyhma."
      :query-params [{hakukohderyhma-oid :- s/Str nil}
                     {include-closed :- s/Bool false}]
      :return {:forms [form-schema/Form]}
      (ok
        (access-controlled-form/get-forms-for-editor session tarjonta-service organization-service
                                                     hakukohderyhma-oid include-closed)))

    (api/GET "/forms/latest/:key" []
      :path-params [key :- s/Str]
      :return ataru-schema/FormWithContent
      :summary "Get content for latest version of form"
      (ok (->> (form-store/fetch-by-key key)
               (koodisto/populate-form-koodisto-fields koodisto-cache))))

    (api/GET "/forms/latest-by-haku/:haku-oid" []
      :path-params [haku-oid :- s/Str]
      :return ataru-schema/FormWithContent
      :summary "Get latest version of form using haku"
      (if-let [key (:ataru-form-key (tarjonta/get-haku tarjonta-service haku-oid))]
        (ok (->> (form-store/fetch-by-key key)
                 (koodisto/populate-form-koodisto-fields koodisto-cache)))
        (response/not-found {:error (str "Form not found on haku " haku-oid)})))

    (api/GET "/forms/:id" []
      :path-params [id :- Long]
      :return ataru-schema/FormWithContent
      :summary "Get content for form" 
      (ok (form-store/fetch-form id)))

    (api/PUT "/forms/:id" {session :session}
      :summary "Edit form content"
      :path-params [id :- Long]
      :body [operations [ataru-schema/Operation]]
      (access-controlled-form/edit-form-with-operations id operations session tarjonta-service organization-service audit-logger)
      (ok {}))

    (api/PUT "/forms/:form-key/change-field-id" {session :session}
      :summary "Change id for form field."
      :path-params [form-key :- s/Str]
      :query-params [old-field-id :- s/Str
                    new-field-id :- s/Str]
      (access-controlled-form/update-field-id-in-form form-key old-field-id new-field-id session tarjonta-service organization-service audit-logger)
      (ok {}))

    (api/PUT "/forms/:id/lock/:operation" {session :session}
      :path-params [id :- Long
                    operation :- (s/enum "open" "close")]
      :return {:locked (s/maybe DateTime)
               :id     Long}
      :summary "Toggle form locked state"
      (ok (access-controlled-form/update-form-lock id operation session tarjonta-service organization-service audit-logger)))

    (api/DELETE "/forms/:id" {session :session}
      :path-params [id :- Long]
      :summary "Mark form as deleted"
      (ok (access-controlled-form/delete-form id session tarjonta-service organization-service audit-logger)))

    (api/POST "/forms" {session :session}
      :summary "Persist changed form."
      :body [form ataru-schema/FormWithContent]
      (ok (access-controlled-form/post-form form session tarjonta-service organization-service audit-logger)))

    (api/POST "/client-error" []
      :summary "Log client-side errors to server log"
      :body [error-details client-error/ClientError]
      (do
        (client-error/log-client-error error-details)
        (ok {})))

    (api/POST "/email-template/:form-key/previews" []
      :path-params [form-key :- s/Str]
      :query-params [{form-allows-ht :- s/Bool false}]
      :body [body {:contents [ataru-schema/EmailTemplate]}]
      (ok (email/preview-submit-emails (:contents body) form-allows-ht)))

    (api/POST "/email-templates/:form-key" {session :session}
      :path-params [form-key :- s/Str]
      :query-params [{form-allows-ht :- s/Bool false}]
      :body [body {:contents [ataru-schema/EmailTemplate]}]
      (ok (email/store-email-templates form-key session (:contents body) form-allows-ht)))

    (api/GET "/email-templates/:form-key" []
      :path-params [form-key :- s/Str]
      :query-params [{form-allows-ht :- s/Bool false}]
      (ok (email/get-email-templates form-key form-allows-ht)))

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
      (api/POST "/start-tutkintojen-tunnustaminen-submit-job/:application-id" {session :session}
        :path-params [application-id :- s/Int]
        (if (get-in session [:identity :superuser])
          (do (tutkintojen-tunnustaminen/start-tutkintojen-tunnustaminen-submit-job
                job-runner
                application-id)
              (response/ok {}))
          (response/unauthorized {})))
      (api/POST "/start-tutkintojen-tunnustaminen-edit-job/:application-id" {session :session}
        :path-params [application-id :- s/Int]
        (if (get-in session [:identity :superuser])
          (do (tutkintojen-tunnustaminen/start-tutkintojen-tunnustaminen-edit-job
                job-runner
                application-id)
              (response/ok {}))
          (response/unauthorized {})))
      (api/POST "/start-automatic-eligibility-if-ylioppilas-job/:application-id" {session :session}
        :path-params [application-id :- s/Int]
        (if (get-in session [:identity :superuser])
          (do (automatic-eligibility/start-automatic-eligibility-if-ylioppilas-job
                job-runner
                application-id)
              (response/ok {}))
          (response/unauthorized {})))
      (api/POST "/start-automatic-eligibility-if-ylioppilas-job-for-haku/:haku-oid" {session :session}
        :path-params [haku-oid :- s/Str]
        (if (get-in session [:identity :superuser])
          (do (application-service/start-automatic-eligibility-if-ylioppilas-job-for-haku
                job-runner
                haku-oid)
              (response/ok {}))
          (response/unauthorized {})))
      (api/POST "/start-automatic-payment-obligation-job/:person-oid" {session :session}
        :path-params [person-oid :- s/Str]
        (if (get-in session [:identity :superuser])
          (do (automatic-payment-obligation/start-automatic-payment-obligation-job
                job-runner
                person-oid)
              (response/ok {}))
          (response/unauthorized {})))
      (api/POST "/start-automatic-payment-obligation-job-for-haku/:haku-oid" {session :session}
        :path-params [haku-oid :- s/Str]
        (if (get-in session [:identity :superuser])
          (do (automatic-payment-obligation/start-automatic-payment-obligation-job-for-haku
                job-runner
                haku-oid)
              (response/ok {}))
          (response/unauthorized {})))
      (api/POST "/start-submit-jobs/:application-id" {session :session}
        :path-params [application-id :- s/Int]
        (if (get-in session [:identity :superuser])
          (do (hakija-application-service/start-submit-jobs
                koodisto-cache
                tarjonta-service
                organization-service
                ohjausparametrit-service
                job-runner
                application-id
                nil)
              (response/ok {}))
          (response/unauthorized {}))))

    (api/context "/post-process" []
      :tags ["post-process-api"]
      (api/POST "/application-attachments/:application-key" {session :session}
        :summary "Post process application attachments"
        :path-params [application-key :- s/Str]
        (if (get-in session [:identity :superuser])
          (do
            (hakija-application-service/handle-application-attachment-post-process
              koodisto-cache
              tarjonta-service
              organization-service
              ohjausparametrit-service
              application-key
              audit-logger
              session)
            (response/ok {}))
          (response/unauthorized {}))))

    (api/context "/applications" []
      :tags ["applications-api"]

      (api/POST "/mass-update" {session :session}
        :body [body {:application-keys [s/Str]
                     :hakukohde-oid    (s/maybe s/Str)
                     :hakukohde-oids-for-hakukohderyhma (s/maybe [s/Str])
                     :from-state       (apply s/enum (map first review-states/application-hakukohde-processing-states))
                     :to-state         (apply s/enum (map first review-states/application-hakukohde-processing-states))}]
        :summary "Update list of application-hakukohde with given state to new state"
        (if (application-service/mass-update-application-states
              application-service
              session
              (:application-keys body)
              (if-let [hakukohde-oid (:hakukohde-oid body)]
                  [hakukohde-oid]
                  (:hakukohde-oids-for-hakukohderyhma body))
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
                          application-service
                          session
                          body)]
          (response/ok result)
          (response/bad-request)))

      (api/GET "/oppilaitos/:oppilaitos-oid/luokat" {session :session}
        :path-params [oppilaitos-oid :- String]
        :summary "Returns classes of given school"
        :return [String]
        (let [year (suoritus-filter/year-for-suoritus-filter (time/now))
              luokkatasot (suoritus-filter/luokkatasot-for-suoritus-filter)]
          (ok (suoritus-service/oppilaitoksen-luokat suoritus-service oppilaitos-oid year luokkatasot))))

      (api/GET "/opiskelija/:henkilo-oid" {session :session}
        :path-params [henkilo-oid :- String]
        :query-params [haku-oid :- String
                       hakemus-datetime :- String]
        :summary "Returns opiskelija information from suoritusrekisteri"
        :return ataru-schema/OpiskelijaResponse
        (let [hakuvuosi   (->> hakemus-datetime
                               (f/parse (:date-time f/formatters))
                               (suoritus-filter/year-for-suoritus-filter))
              luokkatasot (suoritus-filter/luokkatasot-for-suoritus-filter)
              tarjonta-info (when haku-oid
                              (tarjonta-parser/parse-tarjonta-info-by-haku
                               koodisto-cache
                               tarjonta-service
                               organization-service
                               ohjausparametrit-service
                               haku-oid))
              haku-end    (get-in tarjonta-info [:tarjonta :hakuaika :end])
              linked-oids (get (person-service/linked-oids person-service [henkilo-oid]) henkilo-oid)
              aliases     (conj (:linked-oids linked-oids) (:master-oid linked-oids))
              opiskelijat (map #(suoritus-service/opiskelija suoritus-service % [hakuvuosi] luokkatasot haku-end) aliases)]
          (if-let [opiskelija (last (sort-by :alkupaiva opiskelijat))]
            (let [[organization] (organization-service/get-organizations-for-oids organization-service [(:oppilaitos-oid opiskelija)])]
              (response/ok
                {:oppilaitos-name (:name organization)
                 :luokka          (:luokka opiskelija)}))
            (response/not-found {:error (str "Opiskelija information not found for henkilo-oid " henkilo-oid)}))))

      (api/GET "/virkailija-settings" {session :session}
        :return ataru-schema/VirkailijaSettings
        (ok (virkailija-edit/get-review-settings session)))

      (api/GET "/virkailija-texts" {session :session}
        (ok (if (:dev? env)
              (merge texts/general-texts texts/state-translations texts/virkailija-texts)
              (get-virkailija-texts localizations-cache))))

      (api/POST "/review-setting" {session :session}
        :body [review-setting ataru-schema/ReviewSetting]
        :return ataru-schema/ReviewSetting
        (ok (virkailija-edit/set-review-setting review-setting session)))

      (api/GET "/:application-key" {session :session}
        :path-params [application-key :- String]
        :query-params [{newest-form :- s/Bool false}]
        :summary "Return application details needed for application review, including events and review data"
        :return {:application                  ataru-schema/ApplicationWithPerson
                 :events                       [ataru-schema/Event]
                 :review                       ataru-schema/Review
                 :review-notes                 [ataru-schema/ReviewNote]
                 :attachment-reviews           ataru-schema/AttachmentReviews
                 :hakukohde-reviews            ataru-schema/HakukohdeReviews
                 :form                         ataru-schema/FormWithContent
                 (s/optional-key :latest-form) form-schema/Form
                 :information-requests         [ataru-schema/InformationRequest]
                 (s/optional-key :master-oid)  (s/maybe s/Str)}
        (if-let [application (application-service/get-application-with-human-readable-koodis
                              application-service
                              application-key
                              session
                              newest-form)]
          (response/ok application)
          (response/unauthorized {:error (str "Hakemuksen "
                                              application-key
                                              " käsittely ei ole sallittu")})))

      (api/GET "/:application-key/modify" {session :session}
        :path-params [application-key :- String]
        :summary "Get HTTP redirect response for modifying a single application in Hakija side"
        (let [allowed? (access-controlled-application/application-edit-authorized?
                         organization-service
                         tarjonta-service
                         suoritus-service
                         person-service
                         session
                         application-key)]
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
                                application-service
                                application-key
                                nil
                                session)]
          (response/ok resend-event)
          (response/bad-request)))

      (api/GET "/:application-key/field-deadline" {session :session}
        :path-params [application-key :- s/Str]
        :return [ataru-schema/FieldDeadline]
        (let [response (field-deadline/get-field-deadlines
                         organization-service
                         tarjonta-service
                         audit-logger
                         session
                         application-key)]
          (case response
            :unauthorized (response/unauthorized {:error "Unauthorized"})
            nil (response/not-found {:error "Not found"})
            (cond-> (response/ok (map #(dissoc % :last-modified) response))
                    (not-empty response)
                    (response/header
                      "Last-Modified"
                      (->> response
                           (map :last-modified)
                           (apply max-key #(.getMillis %))
                           format-last-modified))))))

      (api/GET "/:application-key/field-deadline/:field-id" {session :session}
        :path-params [application-key :- s/Str
                      field-id :- s/Str]
        :return ataru-schema/FieldDeadline
        (let [response (field-deadline/get-field-deadline
                         organization-service
                         tarjonta-service
                         audit-logger
                         session
                         application-key
                         field-id)]
          (case response
            :unauthorized (response/unauthorized {:error "Unauthorized"})
            nil (response/not-found {:error "Not found"})
            (-> (response/ok (dissoc response :last-modified))
                (response/header
                  "Last-Modified"
                  (format-last-modified (:last-modified response)))))))

      (api/PUT "/:application-key/field-deadline/:field-id" {session :session}
        :path-params [application-key :- s/Str
                      field-id :- s/Str]
        :body [body {:deadline DateTime}]
        :header-params [{if-unmodified-since :- s/Str nil}
                        {if-none-match :- s/Str nil}]
        :return ataru-schema/FieldDeadline
        (let [if-unmodified-since (when (not= "*" if-none-match)
                                    (parse-if-unmodified-since if-unmodified-since))
              response            (field-deadline/put-field-deadline
                                    organization-service
                                    tarjonta-service
                                    audit-logger
                                    session
                                    application-key
                                    field-id
                                    (:deadline body)
                                    if-unmodified-since)]
          (case response
            :unauthorized (response/unauthorized {:error "Unauthorized"})
            nil (response/conflict {:error (if (some? if-unmodified-since)
                                             (str "Field deadline modified since " if-unmodified-since)
                                             "Field deadline exists")})
            (-> (response/ok (dissoc response :last-modified))
                (response/header
                  "Last-Modified"
                  (format-last-modified (:last-modified response)))))))

      (api/DELETE "/:application-key/field-deadline/:field-id" {session :session}
        :path-params [application-key :- s/Str
                      field-id :- s/Str]
        :header-params [if-unmodified-since :- s/Str]
        :return ataru-schema/FieldDeadline
        (let [response (field-deadline/delete-field-deadline
                         organization-service
                         tarjonta-service
                         audit-logger
                         session
                         application-key
                         field-id
                         (parse-if-unmodified-since if-unmodified-since))]
          (case response
            :unauthorized (response/unauthorized {:error "Unauthorized"})
            nil (response/conflict {:error (str "Field deadline modified since " if-unmodified-since)})
            (response/no-content))))

      (api/POST "/notes/:application-key" {session :session}
        :summary "Add new review note for the application"
        :return ataru-schema/ReviewNote
        :body [note {:notes                       s/Str
                     :application-key             s/Str
                     (s/optional-key :hakukohde)  s/Str
                     (s/optional-key :state-name) ataru-schema/HakukohdeReviewTypeNames}]
        (if-let [note (application-service/add-review-note
                        application-service
                        session
                        note)]
          (response/ok note)
          (response/unauthorized {:error (str "Hakemuksen "
                                              (:application-key note)
                                              " käsittely ei ole sallittu")})))
      (api/POST "/mass-notes" {session :session}
        :summary "Add new review notes for applications, mass operation"
        :return [ataru-schema/ReviewNote]
        :body [body {:notes                       s/Str
                     :application-keys            [s/Str]
                     (s/optional-key :hakukohde)  (s/maybe s/Str)
                     (s/optional-key :state-name) ataru-schema/HakukohdeReviewTypeNames}]
        (if-let [notes (application-service/add-review-notes
                        application-service
                        session
                        body)]
          (response/ok notes)
          (response/unauthorized {:error "Hakemuksien käsittely ei ole sallittu"})))

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
        (let [result (application-service/save-application-review
                          application-service
                          session
                          review)]
          (case result
            :forbidden (response/forbidden {:error "Sinulla on valittuna hakukohde jonka käsittelyyn ei ole oikeuksia, käsittelytietoja ei tallennettu."})
            nil (response/unauthorized {:error (str "Hakemuksen "
                                                 (:application-key review)
                                                 " käsittely ei ole sallittu")})
            (response/ok result))))

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
                                           :message-type
                                                      (if (information-request :single-message)
                                                        (-> "single-information-request")
                                                        (-> "information-request")))
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
                     :add-update-link     s/Bool
                     :recipient-target    s/Str
                     :application-keys    [s/Str]}]
        :summary "Send information requests to multiple applicants"
        :return {}
        (if (access-controlled-application/applications-access-authorized-including-opinto-ohjaaja?
              organization-service
              tarjonta-service
              suoritus-service
              person-service
              session
              (:application-keys body)
              [:edit-applications])
          (do (information-request/mass-store
               (assoc (:message-and-subject body)
                      :recipient-target (:recipient-target body)
                      :add-update-link (:add-update-link body)
                      :message-type "mass-information-request")
                (:application-keys body)
                (get-in session [:identity :oid])
                job-runner)
              (response/accepted {}))
          (response/unauthorized {:error "Hakemusten käsittely ei ole sallittu"})))

      (api/POST "/excel" {session :session}
        :body-params [application-keys :- [s/Str]
                      filename :- s/Str
                      {selected-hakukohde :- s/Str nil}
                      {selected-hakukohderyhma :- s/Str nil}
                      {export-mode :- (s/enum "ids-only" "with-defaults") "ids-only"}
                      {included-ids :- [s/Str] []}
                      {sort-by-field :- s/Str "created-time"}
                      {sort-order :- (s/enum "asc" "desc") "desc"}
                      {CSRF :- s/Str nil}]
        :summary "Generate Excel sheet for applications given by ids (and which the user has rights to view)"
        (let [size-limit       40000]
          (log/info "Yritetään" (count application-keys) "hakemuksen excelin luontia")
          (if (< size-limit (count application-keys))
            (response/request-entity-too-large
              {:error (str "Cannot create excel for more than " size-limit " applications")})
            (let [included-ids (set included-ids)
                  ids-only? (= export-mode "ids-only")
                  xls          (application-service/get-excel-report-of-applications-by-key
                                application-service
                                application-keys
                                selected-hakukohde
                                selected-hakukohderyhma
                                included-ids
                                ids-only?
                                sort-by-field
                                sort-order
                                session)]
              (if xls
                {:status  200
                 :headers {"Content-Type"        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                           "Content-Disposition" (str "attachment; filename=" (excel/create-filename filename))}
                 :body    xls}
                (response/unauthorized))))))

      (api/GET "/:application-key/changes" {session :session}
        :summary "Get changes made to an application in version x"
        :path-params [application-key :- s/Str]
        :return [s/Any]
        (if-let [result (application-service/get-application-version-changes
                          application-service
                          koodisto-cache
                          session
                          application-key)]
          (response/ok result)
          (response/unauthorized {:error (str "Hakemuksen "
                                              application-key
                                              " käsittely ei ole sallittu")})))

      (api/POST "/mass-delete" {session :session}
        :query-params [ataru-delete-secret :- s/Str
                       delete-ordered-by :- s/Str
                       reason-of-delete :- s/Str]
        :body [body {:application-keys [s/Str]}]
        :summary "Delete all application data by list of application keys"
        (if (= ataru-delete-secret (get-in config [:application-delete-key :secret-key]))
          (if-let [result (application-service/mass-delete-application-data
                          application-service
                          session
                          (:application-keys body)
                          delete-ordered-by
                          reason-of-delete)]
          (response/ok {:not-deleted-keys result})
          (response/unauthorized {:error (str "Hakemusten "
                                              (clojure.string/join ", " (:application-keys body))
                                              " poisto ei ole sallittu")}))
          (response/unauthorized {:error (str "Delete-secret ei vastaa haluttua. Hakemusten "
                                              (clojure.string/join ", " (:application-keys body))
                                              " poisto ei ole sallittu")})
          )))

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
                     {})})
      (api/POST "/haku/:haku-oid/clear" {session :session}
        :path-params [haku-oid :- s/Str]
        :summary "Remove haku, hakukohde and form cache entries related to haku"
        {:status 200
         :body   (do
                   (tarjonta/clear-haku-caches tarjonta-service haku-oid)
                   (hakija-form-service/clear-form-by-haku-oid-str-cache
                     (:form-by-haku-oid-str-cache dependencies)
                     haku-oid
                     [:hakija])
                   (hakija-form-service/clear-form-by-haku-oid-str-cache
                     (:form-by-haku-oid-str-cache dependencies)
                     haku-oid
                     [:virkailija])
                   {})}))

    (api/GET "/haut" {session :session}
      :query-params [{show-hakukierros-paattynyt :- s/Bool false}]
      :summary "List haku and hakukohde information found for applications stored in system"
      :return ataru-schema/Haut
      (ok (haku-service/get-haut ohjausparametrit-service
                                 organization-service
                                 tarjonta-service
                                 suoritus-service
                                 application-service
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
               :haut             {haku-oid (tarjonta/get-haku tarjonta-service haku-oid)}
               :hakukohteet      (->> (tarjonta/hakukohde-search
                                        tarjonta-service
                                        haku-oid
                                        nil)
                                      (map (fn [{hakukohde-oid :oid :as hakukohde}]
                                             (assoc
                                               hakukohde
                                               :selection-state-used
                                               (hakukohde-store/selection-state-used-in-hakukohde? hakukohde-oid))))
                                      (util/group-by-first :oid))
               :hakukohderyhmat  (util/group-by-first :oid (organization-service/get-hakukohde-groups organization-service))}
              response/ok
              (response/header "Cache-Control" "public, max-age=300"))
          (response/bad-request))))

    (api/context "/valintalaskentakoostepalvelu" []
      :tags ["valintalaskentakoostepalvelu-api"]
      (api/GET "/valintaperusteet/hakukohde/:hakukohde-oid/kayttaa-valintalaskentaa" {session :session}
        :path-params [hakukohde-oid :- s/Str]
        :return ataru-schema/KayttaaValintalaskentaaResponse
        :summary "Tarkistaa valintalaskentakoostepalvelusta käyttääkö annettu hakukohde valintalaskentaa"
        (let [valintalaskenta-enabled? (:kayttaaValintalaskentaa
                                         (valintalaskentakoostepalvelu/hakukohde-uses-valintalaskenta? valintalaskentakoostepalvelu-service
                                                                                                       hakukohde-oid))]
          (response/ok {:hakukohde-oid   hakukohde-oid
                        :valintalaskenta valintalaskenta-enabled?})))

      (api/GET "/suoritukset/haku/:haku-oid/hakemus/:application-key" {session :session}
        :path-params [haku-oid :- String
                      application-key :- s/Str]
        :summary "Returns pohjakoulutus for application's applicant"
        :return ataru-schema/PohjakoulutusResponse
        (if (access-controlled-application/applications-access-authorized-including-opinto-ohjaaja?
              organization-service tarjonta-service suoritus-service person-service session [application-key] [:view-applications :edit-applications])
          (letfn [(get-koodi-label [koodi-uri version koodi-value]
                    (->> (koodisto/get-koodisto-options koodisto-cache koodi-uri version false)
                         (filter #(= koodi-value (:value %)))
                         first
                         :label))]
            (let [suoritus (valintalaskentakoostepalvelu/opiskelijan-suoritukset valintalaskentakoostepalvelu-service haku-oid application-key)]
              (response/ok
                (pohjakoulutus-toinen-aste/pohjakoulutus-for-application get-koodi-label suoritus))))
          (response/unauthorized)))

      (api/GET "/harkinnanvaraisuus/hakemus/:application-key" {session :session}
        :path-params [application-key :- s/Str]
        :return [ataru-schema/HakutoiveHarkinnanvaraisuudella]
        :summary "Tarkistaa valintalaskentakoostepalvelusta annetun hakemuksen hakukohteiden harkinnanvaraisuuden"
        (if (access-controlled-application/applications-access-authorized-including-opinto-ohjaaja?
              organization-service tarjonta-service suoritus-service person-service session [application-key] [:view-applications :edit-applications])
          (let [hakemukset-harkinnanvaraisuudella (valintalaskentakoostepalvelu/hakemusten-harkinnanvaraisuus-valintalaskennasta
                                                     valintalaskentakoostepalvelu-service
                                                     [application-key])
                hakukohteet-harkinnanvaraisuudella (get-in hakemukset-harkinnanvaraisuudella [application-key :hakutoiveet])]
            (response/ok hakukohteet-harkinnanvaraisuudella))
          (response/unauthorized))))

    (api/context "/maksut" []
      :tags ["maksut-api"]
      (api/GET "/list/:application-key" {session :session}
        :path-params [application-key :- s/Str]
        :return maksut-schema/Laskut
        :summary "Listaa hakemukseen liittyvät maksut"
        (if-let [list (maksut-protocol/list-laskut-by-application-key maksut-service application-key)]
          (response/ok list)
          (response/not-found
              {:error (str "Hakemukseen " application-key " liittyvien laskujen listaus epäonnistui")})))

      (api/GET "/kuitti/:order-id" {session :session}
        :path-params [order-id :- s/Str]
        :summary "Lataa maksuun liittyvän kuitin"
        (if-let [resp (maksut-protocol/download-receipt maksut-service order-id)]
          (-> (ok (:body resp))
              (header "Content-Disposition" (str "attachment; filename=\"" order-id ".html\"")))
          (not-found {:error (str "Kuittia ei löytynyt annetulla viitteellä")})))

      (api/POST "/resend-maksu-link" {session :session}
        :body [input maksut-schema/TutuProcessingEmailRequest]
        :return ataru-schema/Event
        :summary "Uudelleenlähettää maksu-linkin sähköpostilla käyttäjälle"
        (let [{:keys [application-key locale]} input
              secret (->> (maksut-protocol/list-laskut-by-application-key maksut-service application-key)
                          (filter #(= (:status %) :active))
                          first
                          :secret)
              payment-url (url-helper/resolve-url :maksut-service.hakija-get-by-secret secret locale)]
          (log/info "Sending Maksut-link email again")
          (if secret
            (if-let [resend-event (application-service/send-modify-application-link-email
                                   application-service
                                   application-key
                                   payment-url
                                   session)]
              (response/ok resend-event)
              (response/bad-request))
            (response/not-found
              {:error (str "Hakemukseen " application-key " liittyviä laskuja ei löydy")}))))

      (api/POST "/maksupyynto" {session :session}
        :body [input maksut-schema/TutuLaskuCreate]
        :summary "Välittää maksunluonti-pyynnön Maksut -palvelulle"

        (let [{:keys [application-key locale message]} input
              lasku-input (-> input
                              (dissoc :message)
                              (dissoc :locale))
              invoice     (maksut-protocol/create-paatos-lasku maksut-service lasku-input)
              secret      (:secret invoice)
              lang        (or locale "fi")
              payment-url (url-helper/resolve-url :maksut-service.hakija-get-by-secret secret lang)]

          (if-let [result (application-service/payment-triggered-processing-state-change
                            application-service
                            session
                            application-key
                            message
                            payment-url
                            "decision-fee-outstanding")]
            (do
              (log/warn "Review result" result)
              (response/ok result))
            (response/unauthorized {:error (str "Hakemuksen "
                                                (:application-key application-key)
                                                " käsittely ei ole sallittu")})))))

    (api/context "/tulos-service" []
      :tags ["tulos-service-api"]

      (api/GET "/haku/:haku-oid/hakemus/:hakemus-oid" {session :session}
        :path-params [haku-oid :- s/Str hakemus-oid :- s/Str]
        :return s/Any
        :summary "Hakee hakemuksen tulokset valinnoista"
        (if (access-controlled-application/applications-access-authorized-including-opinto-ohjaaja?
              organization-service tarjonta-service suoritus-service person-service session [hakemus-oid] [:valinnat-valilehti])
          (let [haku (tarjonta/get-haku tarjonta-service haku-oid)
                superuser? (user-rights/is-super-user? session)]
            (if (vls/valinnan-tuloksien-hakeminen-sallittu? valinta-laskenta-service superuser? haku)
              (if-let [tulokset (vls/hakemuksen-tulokset valinta-laskenta-service haku-oid hakemus-oid)]
                (ok tulokset)
                (not-found))
              (response/unauthorized {:error "Valinnan tulokset kesken"})))
          (response/unauthorized {:error "Unauthorized"}))))

    (api/context "/valintaperusteet" []
      :tags ["valintaperusteet-api"]

      (api/GET "/valintatapajono/:oid" {session :session}
        :path-params [oid :- s/Str]
        :return valintaperusteet-client/Valintatapajono
        :summary "Hae valintatapajonon tiedot"
        (if-let [jono (valintaperusteet/valintatapajono valintaperusteet-service oid)]
          (response/ok jono)
          (response/not-found))))

    (api/context "/koodisto" []
      :tags ["koodisto-api"]
      (api/GET "/:koodisto-uri/:version" {session :session}
        :path-params [koodisto-uri :- s/Str version :- Long]
        :query-params [allow-invalid :- s/Bool]
        :return s/Any
        (let [koodi-options (koodisto/get-koodisto-options
                              koodisto-cache
                              koodisto-uri
                              version
                              allow-invalid)]
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
                       {lahtokoulu-only :- s/Bool false}
                       results-page :- s/Int]
        (ok (organization-selection/query-organization
              organization-service
              session
              query
              {:include-organizations?    organizations
               :include-hakukohde-groups? hakukohde-groups
               :lahtokoulu-only?          lahtokoulu-only}
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
      (api/GET "/haku" []
        :query-params [form-key :- (api/describe s/Str "Form key")]
        :return [ataru-schema/Haku]
        (-> (tarjonta/hakus-by-form-key tarjonta-service form-key)
            response/ok
            (header "Cache-Control" "public, max-age=300")))
      (api/GET "/haku/:oid" []
        :path-params [oid :- (api/describe s/Str "Haku OID")]
        :return ataru-schema/Haku
        (if-let [haku (tarjonta/get-haku tarjonta-service oid)]
          (-> (response/ok haku)
              (header "Cache-Control" "public, max-age=300"))
          (internal-server-error {:error "Internal server error"})))
      (api/GET "/hakukohde" []
        :query-params [organizationOid :- (api/describe s/Str "Organization OID")
                       hakuOid :- (api/describe s/Str "Haku OID")]
        :return [ataru-schema/HakukohdeSearchResult]
        (-> (tarjonta/hakukohde-search
              tarjonta-service
              hakuOid
              organizationOid)
            ok
            (header "Cache-Control" "public, max-age=300"))))

    (api/context "/files" []
      :tags ["files-api"]
      (api/GET "/metadata" []
        :query-params [key :- (api/describe [s/Str] "File key")]
        :summary "Get metadata for one or more files"
        :return [ataru-schema/File]
        (if-let [resp (file-store/get-metadata liiteri-cas-client key)]
          (ok resp)
          (not-found)))
      (api/POST "/metadata" []
        :body-params [keys :- (api/describe [s/Str] "File keys")]
        :summary "Get metadata for one or more files"
        :return [ataru-schema/File]
        (if-let [resp (file-store/get-metadata liiteri-cas-client keys)]
          (ok resp)
          (not-found)))
      (api/GET "/content/:key" []
        :path-params [key :- (api/describe s/Str "File key")]
        :summary "Download a file"
        (if-let [resp (file-store/get-metadata liiteri-cas-client [key])]
          (let [metadata            (first resp)
                content-type        (:content-type metadata)
                content-disposition (:content-disposition metadata)
                file-url            (temp-file-store/signed-download-url temp-file-store key content-type content-disposition)]
            (redirect file-url))
          (not-found)))
      (api/POST "/zip" []
        :form-params [keys :- s/Str
                      {CSRF :- s/Str nil}]
        :summary "Download files as a ZIP archive"
        (let [keys (json/parse-string keys)]
          (->
            (ring-util/response
              (ring-io/piped-input-stream
                (fn [out] (file-store/get-file-zip liiteri-cas-client keys out))))
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
        :return [priorisoiva-hakukohderyhma-schema/PriorisoivaHakukohderyhma]
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
        :body [ryhma priorisoiva-hakukohderyhma-schema/PriorisoivaHakukohderyhma]
        :return priorisoiva-hakukohderyhma-schema/PriorisoivaHakukohderyhma
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
        :return priorisoiva-hakukohderyhma-schema/PriorisoivaHakukohderyhma
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
                                application-service
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
                                [person-oid])]
          (response/ok applications)
          (response/unauthorized {:error "Unauthorized"})))

      (api/POST "/onr/applications" {session :session}
        :body [person-oids [s/Str]]
        :return [ataru-schema/OnrApplication]
        (if (or (empty? person-oids) (> (count person-oids) 1000))
          (response/bad-request {:error "Nonempty list of person oids is required and maximum amount of person oids is 1000"})
          (if-let [applications (access-controlled-application/onr-applications
                                organization-service
                                session
                                person-oids)]
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
                  application-service
                  hakuOid
                  hakukohdeOids
                  hakijaOids
                  modifiedAfter
                  offset))))

      (api/POST "/suoritusrekisteri/henkilot" {session :session}
        :summary "Personoid and hetu from applications"
        :body-params [{hakuOid :- s/Str nil}
                      {hakukohdeOids :- [s/Str] nil}
                      {offset :- s/Str nil}]
        :return {:applications            [ataru-schema/ApplicationPersonInfo]
                 (s/optional-key :offset) s/Str}
        (cond (every? nil? [hakuOid (seq hakukohdeOids)])
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
                (application-service/suoritusrekisteri-person-info
                  application-service
                  hakuOid
                  hakukohdeOids
                  offset))))

      (api/POST "/suoritusrekisteri/haku/:haku-oid/toinenaste" {session :session}
        :summary "Toisen asteen hakemukset for suoritusrekisteri"
        :path-params [haku-oid :- (api/describe s/Str "Haun OID")]
        :body-params [{hakukohdeOids :- [s/Str] nil}
                      {hakijaOids :- [s/Str] nil}
                      {modifiedAfter :- s/Str nil}
                      {offset :- s/Str nil}]
        :return {:applications [ataru-schema/HakurekisteriApplicationToinenAste]
                 (s/optional-key :offset) s/Str}
        (cond (str/empty-or-nil? haku-oid)
              (response/bad-request {:error "No haku-oid path parameter given"})
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
                (application-service/suoritusrekisteri-toinenaste-applications
                  application-service
                  (:form-by-haku-oid-str-cache dependencies)
                  haku-oid
                  hakukohdeOids
                  hakijaOids
                  modifiedAfter
                  offset))))

      (api/GET "/applications" {session :session}           ;; deprecated, use /valinta-tulos-service
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
        :return {:applications            [{:oid                  s/Str
                                            :hakuOid              s/Str
                                            :hakukohdeOids        [s/Str]
                                            :henkiloOid           s/Str
                                            :asiointikieli        s/Str
                                            :email                s/Str
                                            :paymentObligations   {s/Keyword s/Str}}]
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
                                  person-service
                                  session
                                  (reduce application-service/->and-query queries))]
            (response/ok
              (->> applications
                   (map #(dissoc % :hakukohde))
                   (map #(clojure.set/rename-keys % {:haku-oid      :hakuOid
                                                     :person-oid    :personOid
                                                     :asiointikieli :asiointiKieli}))))
            (response/unauthorized {:error "Unauthorized"}))
          (response/bad-request {:error "No query parameters given"})))

      (api/GET "/persons" {session :session}                ;; deprecated, use /valinta-tulos-service
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
        :query-params [{fromDate :- s/Str nil}
                       {offset :- s/Int nil}
                       {limit :- s/Int nil}
                       {toDate :- s/Str nil}
                       {hakuOid :- s/Str nil}]
        :return [{s/Keyword s/Any}]
        (if-let [applications (access-controlled-application/get-applications-for-odw
                                organization-service
                                session
                                person-service
                                tarjonta-service
                                valintalaskentakoostepalvelu-service
                                suoritus-service
                                fromDate
                                limit
                                offset
                                toDate
                                hakuOid
                                nil)]
          (response/ok applications)
          (response/unauthorized {:error "Unauthorized"})))
      (api/GET "/odw/:application-key" {session :session}
        :summary "get odw report for a single application"
        :path-params [application-key :- String]
        :return [{s/Keyword s/Any}]
        (if-let [applications (access-controlled-application/get-applications-for-odw
                                organization-service
                                session
                                person-service
                                tarjonta-service
                                valintalaskentakoostepalvelu-service
                                suoritus-service
                                nil
                                nil
                                nil
                                nil
                                nil
                                application-key)]
          (response/ok applications)
          (response/unauthorized {:error "Unauthorized"})))
      (api/GET "/tilastokeskus" {session :session}
        :summary "Get application info for tilastokeskus"
        :query-params [hakuOid :- s/Str
                       {hakukohdeOid :- s/Str nil}]
        :return [ataru-schema/TilastokeskusApplication]
        (if-let [applications (access-controlled-application/get-applications-for-tilastokeskus organization-service
                                                                                                session
                                                                                                tarjonta-service
                                                                                                hakuOid
                                                                                                hakukohdeOid)]
          (response/ok applications)
          (response/unauthorized {:error "Unauthorized"})))

      (api/POST "/valintalaskenta" {session :session}
        :summary "Get application answers for valintalaskenta"
        :query-params [{hakukohdeOid :- s/Str nil}
                       {harkinnanvaraisuustiedotHakutoiveille :- s/Bool false}]
        :body [applicationOids [s/Str]]
        :return [ataru-schema/ValintaApplication]
        (if (and (nil? hakukohdeOid)
                 (empty? applicationOids))
          (response/bad-request {:error "Either hakukohdeOid or nonempty list of application oids is required"})
          (match (application-service/get-applications-for-valintalaskenta
                   application-service
                   (:form-by-haku-oid-str-cache dependencies)
                   session
                   hakukohdeOid
                   (not-empty applicationOids)
                   harkinnanvaraisuustiedotHakutoiveille)
                 {:unauthorized _}
                 (response/unauthorized {:error "Unauthorized"})
                 {:yksiloimattomat (_ :guard empty?)
                  :applications    applications}
                 (response/ok applications)
                 {:yksiloimattomat yksiloimattomat
                  :applications    applications}
                 (if (get-in config [:yksiloimattomat :allow] false)
                   (do (log/warn "Yksilöimättömiä hakijoita")
                       (response/ok applications))
                   (response/conflict
                     {:error      "Yksilöimättömiä hakijoita"
                      :personOids yksiloimattomat})))))

      (api/GET "/valintapiste" {session :session}
        :summary "Get application answers for Valintapiste Service"
        :query-params [hakuOid :- s/Str
                       {hakukohdeOid :- s/Str nil}]
        :return [ataru-schema/ValintapisteApplication]
        (if-let [applications (access-controlled-application/get-applications-for-valintapiste organization-service
                                                                                               session
                                                                                               hakuOid
                                                                                               hakukohdeOid)]
          (response/ok applications)
          (response/unauthorized {:error "Unauthorized"})))

      (api/POST "/siirtotiedosto" {session :session}
        :summary "Create siirtotiedostos containing appications and forms from a time period"
        :query-params [{modifiedAfter :- s/Str nil}
                       {modifiedBefore :- s/Str nil}]
        :return s/Any
        (if (and (nil? modifiedBefore)
                 (nil? modifiedAfter))
          (response/bad-request {:error "Either modifiedAfter or modifiedBefore param required!"})
          (if (boolean (-> session :identity :superuser))
            (let [siirtotiedosto-params {:modified-after  modifiedAfter
                                         :modified-before (or modifiedBefore (.format
                                                                               (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ssZZZ")
                                                                               (Date.)))}]
              (log/info "Siirtotiedosto params: " siirtotiedosto-params)
              (let [{forms-success :success} (siirtotiedosto-service/siirtotiedosto-forms siirtotiedosto-service siirtotiedosto-params)
                    {applications-success :success} (siirtotiedosto-service/siirtotiedosto-applications siirtotiedosto-service siirtotiedosto-params)
                    combined-result {:success (and forms-success applications-success)}]
                (log/info "Siirtotiedosto success" (:success combined-result))
                (match combined-result
                       {:success true}
                       (response/ok combined-result) ;todo, what do we actually want to return here? Maybe some timestamp information at least.
                       {:success false}
                       (response/internal-server-error "Siirtotiedoston muodostamisessa meni jotain vikaan."))))
            (response/unauthorized "Vain rekisterinpitäjille!"))))

      (api/POST "/siirto" {session :session}
        :summary "Get applications for external systems"
        :query-params [{hakukohdeOid :- s/Str nil}]
        :body [applicationOids [s/Str]]
        :return [ataru-schema/SiirtoApplication]
        (if (and (nil? hakukohdeOid)
                 (empty? applicationOids))
          (response/bad-request {:error "Either hakukohdeOid or nonempty list of application oids is required"})
          (match (application-service/siirto-applications
                   application-service
                   session
                   hakukohdeOid
                   (not-empty applicationOids))
                 {:unauthorized _}
                 (response/unauthorized {:error "Unauthorized"})
                 {:yksiloimattomat (_ :guard empty?)
                  :applications    applications}
                 (response/ok applications)
                 {:yksiloimattomat yksiloimattomat
                  :applications    applications}
                 (if (get-in config [:yksiloimattomat :allow] false)
                   (do (log/warn "Yksilöimättömiä hakijoita")
                       (response/ok applications))
                   (response/conflict
                     {:error      "Yksilöimättömiä hakijoita"
                      :personOids yksiloimattomat})))))

      (api/GET "/kouta/hakukohde/:hakukohde-oid" {session :session}
        :summary "get hakukohde info for kouta"
        :path-params [hakukohde-oid :- String]
        :return {:applicationCount s/Int}
        (if (nil? hakukohde-oid)
          (response/bad-request {:error "HakukohdeOid is required"})
          (if-let [response (application-service/kouta-application-count-for-hakukohde
                              application-service
                              session
                              hakukohde-oid)]
            (response/ok response)
            (response/unauthorized {:error "Unauthorized"}))))

      (api/GET "/kouta/haku/:haku-oid/hakukohde-ensisijainen-application-counts" {session :session}
        :summary "Return counts for ensisijainen applications for given haku, grouped by hakukohde oids"
        :path-params [haku-oid :- String]
        :return {s/Keyword s/Int}
        (if-let [result (application-service/get-ensisijainen-application-counts-for-haku application-service haku-oid)]
          (response/ok result)
          (response/bad-request)))

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
          (response/not-found))))

    (api/context "/valinta-tulos-service"  []
      :tags ["valinta-tulos-service-api"]
      (api/context "/valinnan-tulos" []
        (api/POST "/hakemus" {session :session}
          :summary "Applications for valinta-tulos-service"
          :body [hakemusOids [s/Str]]
          (cond (empty? hakemusOids)
                (response/bad-request {:error "No hakemusOids given"})
                (not (access-controlled-application/applications-access-authorized-including-opinto-ohjaaja?
                          organization-service tarjonta-service suoritus-service person-service session hakemusOids [:view-applications :edit-applications]))
                (response/unauthorized {:error "Unauthorized"})
                :else
                (vts/valinnantulos-monelle-tilahistorialla valinta-tulos-service hakemusOids)))

        (api/GET "/hakemus" {session :session}
          :summary "Valinnantulos for hakemus"
          :query-params [{hakemusOid :- s/Str nil}]
          (cond (nil? hakemusOid)
                (response/bad-request {:error "No query parameter given"})
                (not (access-controlled-application/application-view-authorized?
                       organization-service tarjonta-service suoritus-service person-service session hakemusOid))
                (response/unauthorized {:error "Unauthorized"})
                :else
                (vts/valinnantulos-hakemukselle-tilahistorialla valinta-tulos-service hakemusOid)))

        (api/PATCH "/:valintatapajono-oid" {session :session}
          :summary "Patch valinnantulos"
          :path-params [{valintatapajono-oid :- s/Str nil}]
          :header-params [{if-unmodified-since :- s/Str nil}]
          :body [body s/Any]
          (cond (or (nil? valintatapajono-oid) (nil? if-unmodified-since) (or (nil? body) (empty? body)))
                (response/bad-request {:error "Missing parameters"})
                (not (access-controlled-application/applications-access-authorized-including-opinto-ohjaaja?
                       organization-service
                       tarjonta-service
                       suoritus-service
                       person-service
                       session
                       (map #(get-in % [:hakemusOid :s]) body)
                       [:edit-applications]))
                (response/unauthorized {:error "Unauthorized"})
                :else
                (vts/change-kevyt-valinta-property
                  valinta-tulos-service valintatapajono-oid body if-unmodified-since))))

      (api/context "/hyvaksynnan-ehto" []
        (api/GET "/hakukohteessa/:hakukohde-oid/hakemus/:application-key" {session :session}
          :summary "Get hyvaksynnan-ehto hakukohteessa"
          :path-params [{hakukohde-oid :- s/Str nil}
                        {application-key :- s/Str nil}]
          (cond (or (nil? hakukohde-oid) (nil? application-key))
                (response/bad-request {:error "Missing parameters"})
                (not (access-controlled-application/application-view-authorized?
                       organization-service tarjonta-service suoritus-service person-service session application-key))
                (response/unauthorized {:error "Unauthorized"})
                :else
                (vts/hyvaksynnan-ehto-hakukohteessa-hakemus
                  valinta-tulos-service hakukohde-oid application-key)))

        (api/PUT "/hakukohteessa/:hakukohde-oid/hakemus/:application-key" {session :session}
          :summary "Save hyvaksynnan-ehto hakukohteessa"
          :path-params [{hakukohde-oid :- s/Str nil}
                        {application-key :- s/Str nil}]
          :header-params [{if-unmodified-since :- s/Str nil}]
          :body [ehto s/Any]
          (cond (or (nil? hakukohde-oid) (nil? application-key) (or (nil? ehto) (empty? ehto)))
                (response/bad-request {:error "Missing parameters"})
                (not (access-controlled-application/application-edit-authorized?
                       organization-service tarjonta-service suoritus-service person-service session application-key))
                (response/unauthorized {:error "Unauthorized"})
                :else
                (vts/add-hyvaksynnan-ehto-hakukohteessa-hakemus
                  valinta-tulos-service ehto hakukohde-oid application-key if-unmodified-since)))

        (api/DELETE "/hakukohteessa/:hakukohde-oid/hakemus/:application-key" {session :session}
          :summary "Delete hyvaksynnan-ehto hakukohteessa"
          :path-params [{hakukohde-oid :- s/Str nil}
                        {application-key :- s/Str nil}]
          :header-params [{if-unmodified-since :- s/Str nil}]
          (cond (or (nil? hakukohde-oid) (nil? application-key) (nil? if-unmodified-since))
                (response/bad-request {:error "Missing parameters"})
                (not (access-controlled-application/application-edit-authorized?
                       organization-service tarjonta-service suoritus-service person-service session application-key))
                (response/unauthorized {:error "Unauthorized"})
                :else
                (vts/delete-hyvaksynnan-ehto-hakukohteessa-hakemus
                  valinta-tulos-service hakukohde-oid application-key if-unmodified-since)))

        (api/GET "/valintatapajonoissa/:hakukohde-oid/hakemus/:application-key" {session :session}
          :summary "Get hyvaksynnan-ehto valintatapajonoissa"
          :path-params [{hakukohde-oid :- s/Str nil}
                        {application-key :- s/Str nil}]
          (cond (or (nil? hakukohde-oid) (nil? application-key))
                (response/bad-request {:error "Missing parameters"})
                (not (access-controlled-application/application-view-authorized?
                       organization-service tarjonta-service suoritus-service person-service session application-key))
                (response/unauthorized {:error "Unauthorized"})
                :else
                (vts/hyvaksynnan-ehto-valintatapajonoissa-hakemus
                  valinta-tulos-service hakukohde-oid application-key)))

        (api/GET "/muutoshistoria/hakukohteessa/:hakukohde-oid/hakemus/:application-key" {session :session}
          :summary "Get hyvaksynnan-ehto hakukohteessa muutoshistoria"
          :path-params [{hakukohde-oid :- s/Str nil}
                        {application-key :- s/Str nil}]
          (cond (or (nil? hakukohde-oid) (nil? application-key))
                (response/bad-request {:error "Missing parameters"})
                (not (access-controlled-application/application-view-authorized?
                       organization-service tarjonta-service suoritus-service person-service session application-key))
                (response/unauthorized {:error "Unauthorized"})
                :else
                (vts/hyvaksynnan-ehto-hakukohteessa-muutoshistoria
                  valinta-tulos-service hakukohde-oid application-key)))

        (api/GET "/hakemukselle/:application-key" {session :session}
          :summary "Get hyvaksynnan-ehto hakumukselle"
          :path-params [{application-key :- s/Str nil}]
          (cond (nil? application-key)
                (response/bad-request {:error "Missing parameters"})
                (not (access-controlled-application/application-view-authorized?
                       organization-service tarjonta-service suoritus-service person-service session application-key))
                (response/unauthorized {:error "Unauthorized"})
                :else
                (vts/hyvaksynnan-ehto-hakemukselle valinta-tulos-service application-key)))))

    (when (:dev? env)
      (api/context "/cypress" []
        :tags ["cypress-api"]

        (api/DELETE "/form" []
          :summary "Delete form and all related content"
          :body [{form-key :formKey} {:formKey s/Str}]
          (cypress/delete-form form-key)
          (response/no-content))))))

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
        response (http/do-request {:url     (str prefix path)
                                   :method  :get
                                   :headers (dissoc (:headers request) "host")})]
    {:status  (:status response)
     :body    (:body response)
     :headers (:headers response)}))

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
      :return {s/Keyword {:total   {:week s/Int
                                    :day  s/Int
                                    :hour s/Int}
                          :failed  {:week s/Int
                                    :day  s/Int
                                    :hour s/Int}
                          :errored {:week s/Int
                                    :day  s/Int
                                    :hour s/Int}
                          :queued  s/Int
                          :late    s/Int}}
      (let [status (job/status)]
        (cond-> (dissoc status :ok)
                (:ok status) response/ok
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
                              {:swagger    {:spec "/lomake-editori/swagger.json"
                                            :ui   "/lomake-editori/api-docs"
                                            :data {:info {:version     "1.0.0"
                                                          :title       "Ataru Clerk API"
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
                                                 (create-wrap-database-backed-session (:session-store this))
                                                 clj-access-logging/wrap-session-access-logging
                                                 #(crdsa-auth-middleware/with-authentication % (auth-utils/cas-auth-url))]
                                                (api/middleware [session-client/wrap-session-client-headers
                                                                 session-timeout/wrap-idle-session-timeout]
                                                                app-routes
                                                  (api-routes this))
                                  (auth-routes (select-keys this [:login-cas-client
                                                                  :kayttooikeus-service
                                                                  :person-service
                                                                  :organization-service
                                                                  :audit-logger
                                                                  :session-store]))))
                              (api/undocumented
                                (route/not-found "Not found")))
                            (wrap-defaults (-> site-defaults
                                               (assoc :session nil)
                                               (update :responses dissoc :content-types)
                                               (update :security dissoc :content-type-options :anti-forgery)))
                            (clj-access-logging/wrap-access-logging)
                            (clj-timbre-access-logging/wrap-timbre-access-logging
                             {:path (str (-> config :log :virkailija-base-path)
                                         "/access_ataru-editori"
                                         (when (:hostname env) (str "_" (:hostname env))))})
                            (wrap-gzip)
                            (cache-control/wrap-cache-control))))

  (stop [this]
    (when
      (contains? this :routes)
      (assoc this :routes nil))))

(defn new-handler []
  (->Handler))
