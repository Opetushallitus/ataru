(ns ataru.virkailija.authentication.auth-routes
  (:require [ataru.virkailija.authentication.auth :refer [login logout cas-initiated-logout]]
            [compojure.api.sweet :as api]
            [environ.core :refer [env]]
            [taoensso.timbre :refer [spy debug]]
            [clojure.core.match :refer [match]]))

(def auth-routes
  (api/context "/auth" []
    (api/undocumented
           (api/GET "/cas" [ticket]
             (match [(login (if (:dev? env)
                              (str (System/currentTimeMillis))
                              ticket))]
                    [{:body "" :status 302 :headers {"Location" ""}}]
                    {:status 503 :body "ERROR"}
                    [response] response))
           (api/POST "/cas" [logoutRequest]
                 (cas-initiated-logout logoutRequest))
           (api/GET "/logout" {session :session}
                (logout session)))))
