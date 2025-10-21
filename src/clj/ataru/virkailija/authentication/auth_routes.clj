(ns ataru.virkailija.authentication.auth-routes
  (:require [ataru.virkailija.authentication.auth :refer [login cas-login cas-initiated-logout]]
            [ataru.config.url-helper :refer [resolve-url]]
            [clj-ring-db-session.session.session-client :as session-client]
            [ataru.config.core :refer [config]]
            [clj-ring-db-session.authentication.login :as crdsa-login]
            [compojure.api.sweet :as api]
            [environ.core :refer [env]]
            [clojure.string :as string]))

(defn- rewrite-url-for-environment
  "Ensure that https is used when available (due to https termination on
   non-development environments)"
  [url-from-session]
  (if (:dev? env)
    url-from-session
    (string/replace url-from-session #"^http://" "https://")))

(defn- fake-login-provider [ticket]
  (fn []
    (let [[user henkiloOid roles]
          (case ticket
            "USER-WITH-HAKUKOHDE-ORGANIZATION"
            ["1.2.246.562.11.22222222222"
             "1.2.246.562.11.11111111000"
             #{"ROLE_APP_ATARU_EDITORI_CRUD_1.2.246.562.10.0439846"
               "ROLE_APP_ATARU_HAKEMUS_CRUD_1.2.246.562.10.0439846"
               "ROLE_APP_ATARU_EDITORI_CRUD_1.2.246.562.28.2"
               "ROLE_APP_ATARU_HAKEMUS_CRUD_1.2.246.562.28.2"
               "ROLE_APP_ATARU_EDITORI_CRUD_1.2.246.562.10.10826252480"}]

            "OPINTO-OHJAAJA"
            ["1.2.246.562.11.33333333333"
             "1.2.246.562.11.11111111013"
             #{"ROLE_APP_ATARU_EDITORI_CRUD_1.2.246.562.10.0439845"
               "ROLE_APP_ATARU_HAKEMUS_CRUD_1.2.246.562.10.0439845"
               "ROLE_APP_ATARU_HAKEMUS_opinto-ohjaaja_1.2.246.562.10.0439845"
               "ROLE_APP_ATARU_EDITORI_CRUD_1.2.246.562.28.1"
               "ROLE_APP_ATARU_HAKEMUS_CRUD_1.2.246.562.28.1"
               "ROLE_APP_ATARU_HAKEMUS_opinto-ohjaaja_1.2.246.562.28.1"}]

            "SUPERUSER"
            ["1.2.246.562.11.44444444444"
             "1.2.246.562.11.11111111014"
             #{"ROLE_APP_ATARU_EDITORI_CRUD_1.2.246.562.10.00000000001"
               "ROLE_APP_ATARU_HAKEMUS_CRUD_1.2.246.562.10.00000000001"}]

            ;; default (if unknown ticket)
            ["1.2.246.562.11.11111111111"
             "1.2.246.562.11.11111111012"
             #{"ROLE_APP_ATARU_EDITORI_CRUD_1.2.246.562.10.0439845"
               "ROLE_APP_ATARU_HAKEMUS_CRUD_1.2.246.562.10.0439845"
               "ROLE_APP_ATARU_EDITORI_CRUD_1.2.246.562.28.1"
               "ROLE_APP_ATARU_HAKEMUS_CRUD_1.2.246.562.28.1"}])
          unique-ticket (str (System/currentTimeMillis) "-" (rand-int Integer/MAX_VALUE))]
      [{:user        user
        :henkiloOid  henkiloOid
        :kayttajaTyyppi nil
        :idpEntityId nil
        :roles       roles}
       unique-ticket])))

(defn auth-routes [{:keys [login-cas-client
                           person-service
                           organization-service
                           audit-logger
                           session-store]}]
  (api/context "/auth" []
    (api/middleware [session-client/wrap-session-client-headers]
                    (api/undocumented
                      (api/GET "/cas" [ticket :as request]
                        (let [redirect-url   (if-let [url-from-session (get-in request [:session :original-url])]
                                               (rewrite-url-for-environment url-from-session)
                                               (get-in config [:public-config :virkailija :service_url]))
                              login-provider (if (-> config :dev :fake-dependencies)
                                               (fake-login-provider ticket)
                                               (cas-login login-cas-client ticket))]
                          (login login-provider
                                 person-service
                                 organization-service
                                 audit-logger
                                 redirect-url
                                 (:session request))))
                      (api/POST "/cas" [logoutRequest]
                        (cas-initiated-logout logoutRequest session-store))
                      (api/GET "/logout" {session :session}
                        (crdsa-login/logout session (resolve-url :cas.logout)))))))
