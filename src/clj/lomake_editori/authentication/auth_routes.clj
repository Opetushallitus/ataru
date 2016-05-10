(ns lomake-editori.authentication.auth-routes
  (:require [lomake-editori.authentication.login :refer [login]]
            [compojure.core :refer [GET routes context]]
            [oph.soresu.common.config :refer [config]]
            [taoensso.timbre :refer [info error]]
            [ring.util.http-response :refer [ok]]
            [ring.util.response :as resp]))

(def opintopolku-login-url (-> config :authentication :opintopolku-login-url))
(def opintopolku-logout-url (-> config :authentication :opintopolku-logout-url))
(def ataru-login-success-url (-> config :authentication :ataru-login-success-url))

(defn- redirect-to-logged-out-page []
  (resp/redirect (str opintopolku-login-url ataru-login-success-url)))

(defn auth-routes []
  (context "/lomake-editori/auth" []
           (GET "/cas" [ticket]
             (try
               (if ticket
                 (if-let [username (login ticket ataru-login-success-url)]
                   (do
                        (info "username" username "logged in")
                        (-> (resp/redirect "/lomake-editori")
                            (assoc :session {:identity {:username username :ticket ticket}})))
                   (redirect-to-logged-out-page))
                 (redirect-to-logged-out-page))
               (catch Exception e
                 (error "Error in login ticket handling" e)
                 (redirect-to-logged-out-page))))
           (GET "/logout" {session :session}
                (info "username" (-> session :identity :username) "logged out")
                (-> (resp/redirect (str opintopolku-logout-url ataru-login-success-url))
                    (assoc :session {:identity nil})))))
