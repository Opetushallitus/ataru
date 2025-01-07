(ns ataru.hakija.hakija-routes
  (:require [ataru.log.audit-log :as audit-log]
            [ataru.middleware.cache-control :as cache-control]
            [clj-ring-db-session.session.session-client :as session-client]
            [ataru.applications.application-store :as application-store]
            [ataru.hakija.hakija-form-service :as form-service]
            [ataru.hakija.hakija-application-service :as hakija-application-service]
            [ataru.files.file-store :as file-store]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.schema.form-schema :as ataru-schema]
            [ataru.schema.koski-tutkinnot-schema :as koski-schema]
            [ataru.hakija.form-role :as form-role]
            [ataru.util.client-error :as client-error]
            [clj-access-logging]
            [clj-stdout-access-logging]
            [clj-timbre-access-logging]
            [clojure.core.match :refer [match]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [ataru.selection-limit.selection-limit-service :as selection-limit]
            [compojure.api.exception :as ex]
            [compojure.api.sweet :as api]
            [ring.swagger.upload]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.cookies :as cook]
            [ring.util.http-response :as response]
            [schema.core :as s]
            [selmer.parser :as selmer]
            [cheshire.core :as json]
            [ataru.config.core :refer [config]]
            [ataru.palaute.palaute-client :as palaute-client]
            [ataru.test-utils :refer [get-test-vars-params get-latest-application-secret
                                      alter-application-to-hakuaikaloppu-for-secret]]
            [ataru.hakija.resumable-file-transfer :as resumable-file]
            [ataru.hakija.signed-direct-upload :as signed-upload]
            [taoensso.timbre :as log]
            [string-normalizer.filename-normalizer-middleware :as normalizer]
            [ataru.util.http-util :as http]
            [ataru.cas-oppija.cas-oppija-session-store :as oss]
            [ataru.cas-oppija.cas-oppija-utils :as cas-oppija-utils]
            [ataru.feature-config :as fc]
            [ataru.koski.koski-service :as koski]
            [ataru.koski.koski-json-parser :refer [parse-koski-tutkinnot]])
  (:import [java.util UUID]))

(def ^:private cache-fingerprint (System/currentTimeMillis))

(defn- get-application
  [form-by-id-cache
   koodisto-cache
   ohjausparametrit-service
   application-service
   organization-service
   tarjonta-service
   hakukohderyhma-settings-cache
   audit-logger
   session
   secret
   liiteri-cas-client]
  (let [[application-form-and-person secret-expired? lang-override inactivated?]
        (hakija-application-service/get-latest-application-by-secret form-by-id-cache
                                                                     koodisto-cache
                                                                     ohjausparametrit-service
                                                                     application-service
                                                                     organization-service
                                                                     tarjonta-service
                                                                     hakukohderyhma-settings-cache
                                                                     secret
                                                                     liiteri-cas-client)]
    (cond inactivated?
          (response/bad-request {:code :inactivated :error "Inactivated" :lang lang-override})

          (some? application-form-and-person)
          (let [application (:application application-form-and-person)]
            (audit-log/log audit-logger
                           {:new       application
                            :operation audit-log/operation-read
                            :session   session
                            :id        {:applicationOid (:key application)}})
            (response/ok application-form-and-person))

          secret-expired?
          (response/unauthorized {:code :secret-expired
                                  :lang lang-override})
          (:virkailija secret)
          (response/bad-request {:code  :secret-expired
                                 :error "Invalid virkailija secret"})

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
    (api/GET "/latest-application-secret" []
      (if (is-dev-env?)
        (get-latest-application-secret)
        (response/not-found "Not found")))
    (api/GET "/alter-application-to-hakuaikaloppu-for-secret/:secret" [secret]
      (if (is-dev-env?)
        (do (alter-application-to-hakuaikaloppu-for-secret secret)
            (response/ok {}))
        (response/not-found "Not found")))
    (api/GET "/virkailija-hakemus-edit-test.html" []
      (if (is-dev-env?)
        (render-file-in-dev "templates/virkailija-hakemus-edit-test.html")
        (response/not-found "Not found")))
    (api/GET "/spec/:filename.js" [filename]
      ;; Test vars params is a hack to get form ids from fixtures to the test file
      ;; without having to pass them as url params. Also enables tests to be run
      ;; individually when navigating to any test file.
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

(defn generate-new-random-key [] (str (UUID/randomUUID)))

(defn hakija-auth-routes [{:keys [audit-logger]}]
  (api/context "/auth" []
    :tags ["hakija-auth-api"]
    (api/GET "/oppija" [:as request]
      :query-params [{ticket :- s/Str nil}
                     {target :- s/Str nil}
                     {lang :- s/Str nil}]
      (log/info "login with lang" lang ",ticket" ticket ". Redirect-to" target ". Cookies" (get-in request [:cookies "oppija-session" :value]))
      (try
        (if (nil? ticket)
          (response/found
            (cas-oppija-utils/parse-cas-oppija-login-url (or lang "fi") target))
        (let [rs (-> (http/do-get (cas-oppija-utils/parse-cas-oppija-ticket-validation-url ticket target))
                     (:body))
              parsed-attributes (cas-oppija-utils/parse-oppija-attributes-if-successful rs)]
          (if parsed-attributes
            (let [new-session-key (generate-new-random-key)]
              (audit-log/log audit-logger
                             {:new       parsed-attributes
                              :operation audit-log/operation-oppija-login
                              :session   (:session request)
                              :id        {:oppija-session new-session-key}})
              (oss/persist-session! new-session-key ticket parsed-attributes)
              (-> (response/found target)
                  (update :cookies (fn [c] (assoc c :oppija-session {:value     new-session-key
                                                                     :path      "/hakemus"
                                                                     :http-only true
                                                                     :secure    true})))))
            ;fixme, mitä tehdään jos tiketin validointi epäonnistui?
            (response/bad-request))))
      (catch Exception e
        (log/error e "Virhe oppijan tunnistautumisessa.")
        (response/internal-server-error))))
  (api/POST "/oppija" [:as request]
    (let [logout-request (get-in request [:params :logoutRequest])]
      (log/info "Received request for logout:" logout-request)
      (if-let [ticket (cas-oppija-utils/parse-ticket-from-lockout-request logout-request)]
        (let [res (oss/delete-session-by-ticket! ticket)]
          (log/info ticket ": db result" res)
          (if (= res 1)
            (response/ok)
            (response/not-found)))
        (log/warn "Something went wong when processing logout request..."))))
  (api/GET "/oppija/logout" [:as request]
    :query-params [{lang :- s/Str nil}]
    (try
      (let [oppija-session-key (get-in request [:cookies "oppija-session" :value])
            result (oss/delete-session-by-key! oppija-session-key)
            destination (cas-oppija-utils/parse-cas-oppija-logout-url (or (keyword lang) :fi))]
        (log/info "LOGOUT for session " oppija-session-key "; result" result ", dest" destination)
        (audit-log/log audit-logger
                       {:new       {}
                        :operation audit-log/operation-oppija-logout
                        :session   (:session request)
                        :id        {:oppija-session oppija-session-key}})
        (response/found destination))
      (catch Exception e
        (log/error e "Virhe oppijan uloskirjautumisessa.")
        (response/internal-server-error))))
  (api/GET "/session" [:as request]
    (try
      (let [oppija-session (get-in request [:cookies "oppija-session" :value])
            session (oss/read-session oppija-session)
            trimmed-session {:fields       (get-in session [:data :fields])
                             :display-name (get-in session [:data :display-name])
                             :auth-type    (get-in session [:data :auth-type])
                             :logged-in    (boolean (:logged-in session))
                             :eidas-id     (get-in session [:data :eidas-id])
                             :person-oid   (get-in session [:data :person-oid])
                             :seconds-left (or (:seconds_left session) 0)}]
        (response/ok trimmed-session))
      (catch Exception e
        (log/error e "Virhe haettaessa oppijan sessiota.")
        (response/internal-server-error))))))

(defn api-routes [{:keys [tarjonta-service
                          job-runner
                          maksut-service
                          organization-service
                          ohjausparametrit-service
                          application-service
                          koski-service
                          koodisto-cache
                          form-by-id-cache
                          form-by-haku-oid-str-cache
                          temp-file-store
                          amazon-sqs
                          audit-logger
                          liiteri-cas-client
                          hakukohderyhma-settings-cache]}]
  (api/context "/api" []
    :tags ["application-api"]
    (api/GET ["/haku/:haku-oid" :haku-oid #"[0-9\.]+"] []
      :summary "Gets form for haku"
      :path-params [haku-oid :- s/Str]
      :query-params [role :- [form-role/FormRole]]
      (if-let [form-with-tarjonta (form-service/fetch-form-by-haku-oid-str-cached
                                    form-by-haku-oid-str-cache
                                    haku-oid
                                    role)]
        (response/content-type (response/ok form-with-tarjonta)
                               "application/json")
        (response/not-found {})))
    (api/GET ["/hakukohde/:hakukohde-oid", :hakukohde-oid #"[0-9\.]+"] []
      :summary "Gets form for hakukohde"
      :path-params [hakukohde-oid :- s/Str]
      :query-params [role :- [form-role/FormRole]]
      (if-let [form-with-tarjonta (form-service/fetch-form-by-hakukohde-oid-str-cached
                                    tarjonta-service
                                    form-by-haku-oid-str-cache
                                    hakukohde-oid
                                    role)]
        (response/content-type (response/ok form-with-tarjonta)
                               "application/json")
        (response/not-found {})))
    (api/GET "/form/:key" []
      :path-params [key :- s/Str]
      :query-params [role :- [form-role/FormRole]]
      :return ataru-schema/FormWithContent
      (if-let [form (form-service/fetch-form-by-key key role form-by-id-cache koodisto-cache nil false {})]
        (response/ok form)
        (response/not-found {})))
    (api/POST "/feedback" []
      :summary "Add feedback sent by applicant"
      :body [feedback ataru-schema/ApplicationFeedback]
      (if-let [saved-application (hakija-application-service/save-application-feedback feedback)]
        (do
          (palaute-client/send-application-feedback amazon-sqs feedback)
          (response/ok {:id (:id saved-application)}))
        (response/bad-request {})))
    (api/POST "/application" [:as request]
      :summary "Submit application"
      :body [application ataru-schema/Application]
      (let [session {:session request}
            tunnistautunut? (and (fc/feature-enabled? :hakeminen-tunnistautuneena)
                                 (:tunnistautunut application))
            oppija-session-from-db (when tunnistautunut?
                                     (some-> (get-in request [:cookies "oppija-session" :value])
                                             (oss/read-session)))]
        (log/info "Submit application, tunnistautunut" tunnistautunut? ", session" oppija-session-from-db)
        (if (and tunnistautunut? (not (:logged-in oppija-session-from-db)))
          (response/bad-request {:passed? false :failures ["No active oppija-session found"] :code :oppija-session-not-found})
          (match (hakija-application-service/handle-application-submit
                   form-by-id-cache
                   koodisto-cache
                   tarjonta-service
                   job-runner
                   organization-service
                   ohjausparametrit-service
                   hakukohderyhma-settings-cache
                   audit-logger
                   application
                   session
                   liiteri-cas-client
                   maksut-service
                   oppija-session-from-db)
                 {:passed? false :failures failures :code code}
                 (response/bad-request {:failures failures :code code})

                 {:passed? true :id application-id :payment payment}
                 (response/ok {:id application-id :payment payment})

                 {:passed? true :id application-id}
                 (response/ok {:id application-id})))))
    (api/PUT "/application" {session :session}
      :summary "Edit application"
      :body [application ataru-schema/Application]
      (match (hakija-application-service/handle-application-edit
               form-by-id-cache
               koodisto-cache
               tarjonta-service
               job-runner
               organization-service
               ohjausparametrit-service
               hakukohderyhma-settings-cache
               audit-logger
               application
               session
               liiteri-cas-client
               maksut-service)
             {:passed? false :failures failures :code code}
             (response/bad-request {:failures failures :code code})

             {:passed? true :id application-id}
             (response/ok {:id application-id})))
    (api/GET "/application" {session :session}
      :summary "Get submitted application by secret"
      :query-params [{secret :- s/Str nil}
                     {virkailija-secret :- s/Str nil}]
      :return ataru-schema/ApplicationWithPersonFormAndPayment
      (cond (not-blank? secret)
            (get-application form-by-id-cache
                             koodisto-cache
                             ohjausparametrit-service
                             application-service
                             organization-service
                             tarjonta-service
                             hakukohderyhma-settings-cache
                             audit-logger
                             session
                             {:hakija secret}
                             liiteri-cas-client)

            (not-blank? virkailija-secret)
            (get-application form-by-id-cache
                             koodisto-cache
                             ohjausparametrit-service
                             application-service
                             organization-service
                             tarjonta-service
                             hakukohderyhma-settings-cache
                             audit-logger
                             session
                             {:virkailija virkailija-secret}
                             liiteri-cas-client)

            :else
            (response/bad-request {:code  :secret-expired
                                   :error "No secret given"})))
    (api/POST "/send-application-secret" []
      :summary "Sends application link with fresh secret to applicant"
      :body [request {:old-secret s/Str}]
      (do
        (hakija-application-service/create-new-secret-and-send-link
          koodisto-cache tarjonta-service organization-service ohjausparametrit-service
          job-runner
          (:old-secret request))
        (response/ok {})))
    (api/GET "/omat-tutkinnot" [:as request]
      :summary "Returns exams from Koski for strongly authenticated applicant"
      :query-params [tutkinto-levels :- s/Str]
      :return [koski-schema/AtaruKoskiTutkinto]
      (let [oppija-session (get-in request [:cookies "oppija-session" :value])
            session (oss/read-session oppija-session)
            tutkinto-level-list (str/split tutkinto-levels #",")]
         (if-let [henkilo-oid (get-in session [:data :person-oid])]
          (if-let [oppija-response (koski/get-tutkinnot-for-oppija koski-service henkilo-oid)]
            (response/ok (parse-koski-tutkinnot (:opiskeluoikeudet oppija-response) tutkinto-level-list))
            (response/not-found {}))
          (response/unauthorized {}))))
    (api/context "/files" []
      (api/GET "/signed-upload" []
        :summary "Permission to upload"
        :query-params [file-size :- s/Int
                       file-name :- s/Str]
        :middleware [normalizer/wrap-query-params-filename-normalizer]
        (response/ok
          (let [key (str (UUID/randomUUID))]
            {:key        key
             :signed-url (signed-upload/signed-url-for-direct-upload temp-file-store file-name file-size key)})))
      (api/PUT "/mark-upload-delivered" []
        :summary "Permission to upload"
        :query-params [file-id :- s/Str
                       file-name :- s/Str]
        :middleware [normalizer/wrap-query-params-filename-normalizer]
        (let [[status stored-file] (resumable-file/mark-upload-delivered-to-liiteri liiteri-cas-client file-id file-name)]
          (log/info "Upload delivered" file-name "bytes:" status)
          (case status
            :complete (response/created "" {:stored-file stored-file})
            :bad-request (response/bad-request {})
            :liiteri-error (response/internal-server-error {}))))
      (api/GET "/:key" []
        :summary "Download a file"
        :path-params [key :- s/Str]
        :query-params [{secret :- s/Str nil}
                       {virkailija-secret :- s/Str nil}]
        (if (hakija-application-service/can-access-attachment?
              secret virkailija-secret key)
          (if-let [file (file-store/get-file liiteri-cas-client key)]
            (-> (:body file)
                response/ok
                (response/header "Content-Disposition"
                                 (:content-disposition file)))
            (response/not-found {}))
          (response/unauthorized {})))
      (api/DELETE "/:key" []
        :summary "Delete a file"
        :path-params [key :- s/Str]
        :return {:key s/Str}
        (if-let [resp (file-store/delete-file liiteri-cas-client key)]
          (response/ok resp)
          (response/bad-request {:failures (str "Failed to delete file with key " key)}))))
    (api/POST "/client-error" []
      :summary "Log client-side errors to server log"
      :body [error-details client-error/ClientError]
      (handle-client-error error-details))
    (api/GET "/postal-codes/:postal-code" [postal-code]
      :summary "Get name of postal office by postal code"
      (let [code (koodisto/get-postal-office-by-postal-code koodisto-cache postal-code)]
        (if-let [labels (:label code)]
          (response/ok labels)
          (response/not-found {}))))
    (api/GET "/koulutustyypit" []
      :summary "Get cached koulutustyypit from koodisto"
      (let [codes (koodisto/get-koulutustyypit koodisto-cache)]
        (response/ok codes)))
    (api/POST "/has-applied" [:as request]
      :summary "Check if a person has already applied"
      :body [has-applied-params ataru-schema/HasAppliedParams]
      (let [{:keys [haku-oid ssn email eidas-id]} has-applied-params
            session (:session request)
            oppija-session-from-db (some-> (get-in request [:cookies "oppija-session" :value])
                                           (oss/read-session))]
        (log/info "Checking has-applied" has-applied-params "session" session "logged-in" (boolean (:logged-in oppija-session-from-db)))
        (cond (some? ssn)
              (response/ok (application-store/has-ssn-applied haku-oid ssn))
              (some? eidas-id)
              (response/ok (application-store/has-eidas-applied haku-oid eidas-id))
              (some? email)
              (response/ok (application-store/has-email-applied haku-oid email))
              :else
              (response/bad-request {:error "Either ssn, email or eidas-id is required"}))))
    (api/PUT "/selection-limit" []
      :summary "Selection limits"
      :query-params [{form-key :- s/Str nil}
                     {selection-id :- s/Str nil}
                     {selection-group-id :- s/Str nil}
                     {question-id :- s/Str nil}
                     {answer-id :- s/Str nil}]
      :return ataru-schema/FormSelectionLimit
      (try
        (response/ok
          (cond (and form-key selection-id question-id answer-id selection-group-id)
                (selection-limit/swap-selection form-key selection-id question-id answer-id selection-group-id)

                (and form-key question-id answer-id selection-group-id)
                (selection-limit/new-selection form-key question-id answer-id selection-group-id)

                (and form-key selection-id question-id selection-group-id)
                (selection-limit/remove-initial-selection form-key selection-id question-id selection-group-id)

                form-key
                (selection-limit/query-available-selections form-key)))
        (catch clojure.lang.ExceptionInfo _
          (response/conflict
            (selection-limit/query-available-selections form-key)))))))

(defn- render-application [lang]
  (let [public-config (json/generate-string (or (:public-config config) {}))]
    (selmer/render-file "templates/hakija.html" {:cache-fingerprint cache-fingerprint
                                                 :lang              (or lang "fi")
                                                 :config            public-config
                                                 :js-bundle-name    (or (-> config :server :js-bundle-names :hakija)
                                                                        "hakija-app.js")})))

(defn- wrap-referrer-policy
  [handler policy]
  (fn [request]
    (response/header (handler request) "Referrer-Policy" policy)))

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
                                (cook/wrap-cookies
                                  (api/context "/hakemus" []
                                    (api/middleware [session-client/wrap-session-client-headers]
                                                    test-routes
                                      (hakija-auth-routes this)
                                      (api-routes this)
                                      (api/GET ["/haku/:haku-oid/demo" :haku-oid #"[0-9\.]+"] []
                                        :path-params [haku-oid :- s/Str]
                                        :query-params [lang :- s/Str]
                                        (if (form-service/is-demo-allowed (:form-by-haku-oid-str-cache this) haku-oid)
                                          (response/temporary-redirect
                                            (str (-> config :public-config :applicant :service_url)
                                                 "/hakemus/haku/" haku-oid
                                                 "?demo=true"
                                                 "&lang=" lang))
                                          (response/not-found {})))
                                      (route/resources "/")
                                      (api/undocumented
                                        (api/GET "/haku/:oid" []
                                          :query-params [{lang :- s/Str nil}]
                                          (render-application lang))
                                        (api/GET "/hakukohde/:oid" []
                                          :query-params [{lang :- s/Str nil}]
                                          (render-application lang))
                                        (api/GET "/:key" []
                                          :query-params [{lang :- s/Str nil}]
                                          (render-application lang))
                                        (api/GET "/" []
                                          :query-params [{lang :- s/Str nil}]
                                          (render-application lang))))
                                    (route/not-found "<h1>Page not found</h1>")))))
                            (clj-access-logging/wrap-access-logging)
                            (clj-timbre-access-logging/wrap-timbre-access-logging
                              {:path (str (-> config :log :hakija-base-path)
                                          "/access_ataru-hakija"
                                          (when (:hostname env) (str "_" (:hostname env))))})
                            (wrap-gzip)
                            (wrap-referrer-policy "same-origin")
                            (cache-control/wrap-cache-control))))

  (stop [this]
    this))

(defn new-handler []
  (->Handler))
