(ns ataru.virkailija.authentication.auth-routes
  (:require [ataru.virkailija.authentication.auth :refer [login cas-login logout cas-initiated-logout]]
            [ataru.middleware.session-client :as session-client]
            [ataru.config.core :refer [config]]
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
      (let [username      (if (= ticket "USER-WITH-HAKUKOHDE-ORGANIZATION")
                            "1.2.246.562.11.22222222222"
                            "1.2.246.562.11.11111111111")
            unique-ticket (str (System/currentTimeMillis) "-" (rand-int (Integer/MAX_VALUE)))]
        [username unique-ticket])))

(defn auth-routes [cas-client
                   kayttooikeus-service
                   person-service
                   organization-service]
  (api/context "/auth" []
    (api/middleware [session-client/wrap-session-client-headers]
    (api/undocumented
      (api/GET "/cas" [ticket :as request]
               (let [redirect-url (if-let [url-from-session (get-in request [:session :original-url])]
                                    (rewrite-url-for-environment url-from-session)
                                    (get-in config [:public-config :virkailija :service_url]))
                     login-provider (if (-> config :dev :fake-dependencies)
                                      (fake-login-provider ticket)
                                      (cas-login cas-client ticket))]
                 (login login-provider
                        kayttooikeus-service
                        person-service
                        organization-service
                        redirect-url
                        (:session request))))
      (api/POST "/cas" [logoutRequest]
                (cas-initiated-logout logoutRequest))
      (api/GET "/logout" {session :session}
                (logout session))))))
