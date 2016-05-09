(ns lomake-editori.authentication.auth-routes
  (:require [lomake-editori.authentication.login :refer [login]]
            [compojure.api.sweet :as api]
            [taoensso.timbre :refer [info error]]
            [ring.util.http-response :refer [ok]]
            [ring.util.response :as resp]))

(defn- redirect-to-loggged-out-page []
  ;; TODO implement logged-out page
  (resp/redirect (str "/lomake-editori/logged-out")))

(def opintopolku-logout-url "https://testi.virkailija.opintopolku.fi/cas/logout?service=")
(def ataru-login-success-url "http://localhost:8350/lomake-editori/auth/cas")

(defn auth-routes []
  (api/api
    (api/context "/lomake-editori/auth" []
                 (api/GET "/cas" [ticket :as request]
                          :summary "Handle login ticket and logout callback from cas"
                          (try
                            (if ticket
                              (if-let [username (login ticket "http://localhost:8350/lomake-editori/auth/cas")]
                                (do
                                  (info "username" username "logged in")
                                  (-> (resp/redirect "http://localhost:8350/lomake-editori")
                                      (assoc :session {:identity {:username username :ticket ticket}})))
                                (redirect-to-loggged-out-page))
                              (redirect-to-loggged-out-page))
                            (catch Exception e
                              (error "Error in login ticket handling" e)
                              (redirect-to-loggged-out-page))))
                 (api/GET "/logout" {session :session}
                          (-> (resp/redirect (str opintopolku-logout-url ataru-login-success-url))
                           (assoc :session {:identity nil}))))))
