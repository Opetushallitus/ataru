(ns ataru.virkailija.authentication.auth-routes
  (:require [ataru.virkailija.authentication.auth :refer [login logout cas-initiated-logout]]
            [oph.soresu.common.config :refer [config]]
            [compojure.api.sweet :as api]
            [environ.core :refer [env]]
            [taoensso.timbre :refer [spy debug]]
            [clojure.core.match :refer [match]]))

(defn auth-routes [organization-service]
  (api/context "/auth" []
    (api/undocumented
           (api/GET "/cas" [ticket]
             (login (if (-> config :dev:fake-dependencies)
                      (str (System/currentTimeMillis))
                      ticket)))
           (api/POST "/cas" [logoutRequest]
                 (cas-initiated-logout logoutRequest))
           (api/GET "/logout" {session :session}
                (logout session)))))
