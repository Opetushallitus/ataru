(ns ataru.virkailija.authentication.auth-routes
  (:require [ataru.virkailija.authentication.auth :refer [login cas-login logout cas-initiated-logout]]
            [ataru.config.url-helper :refer [resolve-url]]
            [ataru.config.core :refer [config]]
            [compojure.api.sweet :as api]
            [environ.core :refer [env]]
            [taoensso.timbre :refer [spy debug info]]
            [clojure.core.match :refer [match]]
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
      (let [username      (if (string/starts-with? ticket "ST-")
                            "DEVELOPER"
                            ticket)
            unique-ticket (str (System/currentTimeMillis))]
        [username unique-ticket])))

(defn auth-routes [organization-service]
  (api/context "/auth" []
    (api/undocumented
      (api/GET "/cas" [ticket :as request]
               (let [redirect-url (if-let [url-from-session (get-in request [:session :original-url])]
                                    (rewrite-url-for-environment url-from-session)
                                    (get-in config [:public-config :virkailija :service_url]))
                     login-provider (if (-> config :dev :fake-dependencies)
                                      (fake-login-provider ticket)
                                      (cas-login ticket))]
                 (login login-provider
                        organization-service
                        redirect-url)))
      (api/POST "/cas" [logoutRequest]
                (cas-initiated-logout logoutRequest))
      (api/GET "/logout" {session :session}
                (logout session)))))
