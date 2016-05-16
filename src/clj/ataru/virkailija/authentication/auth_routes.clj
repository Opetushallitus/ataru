(ns ataru.virkailija.authentication.auth-routes
  (:require [ataru.virkailija.authentication.auth :refer [login logout cas-initiated-logout]]
            [compojure.core :refer [GET POST routes context]]
            [environ.core :refer [env]]
            [clojure.core.match :refer [match]]))

(def auth-routes
  (context "/auth" []
           (GET "/cas" [ticket]
             (match [(login (if (:dev? env)
                              (str (System/currentTimeMillis))
                              ticket))]
                    [{:body "" :status 302 :headers {"Location" ""}}]
                    {:status 503 :body "ERROR"}
                    [response] response))
           (POST "/cas" [logoutRequest]
                 (cas-initiated-logout logoutRequest))
           (GET "/logout" {session :session}
                (logout session))))
