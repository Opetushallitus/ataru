(ns ataru.virkailija.authentication.auth-routes
  (:require [ataru.virkailija.authentication.auth :refer [login logout cas-initiated-logout]]
            [compojure.core :refer [GET POST routes context]]))
(defn auth-routes []
  (context "/lomake-editori/auth" []
           (GET "/cas" [ticket]
             (login ticket))
           (POST "/cas" [logoutRequest]
                 (cas-initiated-logout logoutRequest))
           (GET "/logout" {session :session}
                (logout session))))
