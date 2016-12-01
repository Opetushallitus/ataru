(ns ataru.virkailija.authentication.auth-routes
  (:require [ataru.virkailija.authentication.auth :refer [login logout cas-initiated-logout]]
            [cemerick.url :as url]
            [oph.soresu.common.config :refer [config]]
            [compojure.api.sweet :as api]
            [environ.core :refer [env]]
            [taoensso.timbre :refer [spy debug info]]
            [clojure.core.match :refer [match]]))

(defn- rewrite-url-for-environment [url-from-session]
  "Ensure that https is used when available (due to https termination on non-development environments)"
  (let [{:keys [host path query anchor]} (url/url url-from-session)]
    (if (:dev? env)
      url-from-session
      (str "https://"
           host
           path
           (when (some? query) (url/map->query query))
           (when (some? anchor) (str "#" anchor))))))

(defn auth-routes [organization-service]
  (api/context "/auth" []
    (api/undocumented
      (api/GET "/cas" [ticket :as request]
               (let [redirect-url (if-let [url-from-session (get-in request [:session :original-url])]
                                    (rewrite-url-for-environment url-from-session)
                                    (get-in config [:public-config :virkailija :service-url]))]
                 (login (if (-> config :dev :fake-dependencies)
                          (str (System/currentTimeMillis))
                          ticket)
                        organization-service redirect-url)))
      (api/POST "/cas" [logoutRequest]
                (cas-initiated-logout logoutRequest))
      (api/GET "/logout" {session :session}
                (logout session)))))
