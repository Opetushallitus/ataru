(ns ataru.virkailija.authentication.auth-routes
  (:require [ataru.virkailija.authentication.auth :refer [login logout cas-initiated-logout]]
            [cemerick.url :as url]
            [oph.soresu.common.config :refer [config]]
            [compojure.api.sweet :as api]
            [environ.core :refer [env]]
            [taoensso.timbre :refer [spy debug]]
            [clojure.core.match :refer [match]]))

(defn- url->relative [url-from-session]
  (let [{:keys [path query anchor]} (url/url url-from-session)]
    (str path
         (when (some? query) (url/map->query query))
         (when (some? anchor) (str "#" anchor)))))

(defn auth-routes [organization-service]
  (api/context "/auth" []
    (api/undocumented
      (api/GET "/cas" [ticket :as request]
               (let [redirect-url (if-let [url-from-session (get-in request [:session :original-url])]
                                    (url->relative url-from-session)
                                    "/lomake-editori")]
                 (login (if (-> config :dev :fake-dependencies)
                          (str (System/currentTimeMillis))
                          ticket)
                        organization-service redirect-url)))
      (api/POST "/cas" [logoutRequest]
                (cas-initiated-logout logoutRequest))
      (api/GET "/logout" {session :session}
                (logout session)))))
