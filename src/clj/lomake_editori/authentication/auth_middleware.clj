(ns lomake-editori.authentication.auth-middleware
  (:require
    [buddy.auth :refer [authenticated?]]
    [buddy.auth.middleware :refer [wrap-authentication]]
    [buddy.auth.accessrules :refer [wrap-access-rules success error]]
    [buddy.auth.backends.session :refer [session-backend]]
    [clojure.data.json :as json]))

(defonce opintopolku-login-url "https://testi.virkailija.opintopolku.fi/cas/login?service=")

(defonce ataru-login-success-url "http://localhost:8350/login/cas")

(def backend (session-backend))

(defn any-access [request] true)

(defn check-identity [identity] false) ;; TODO!!

(defn- authenticated-access [request]
  (if (check-identity (-> request :session :identity))
    true
    (error "Authentication required")))

(defn- send-not-authenticated-api-response [request]
  {:status  401
   :headers {"Content-Type" "application/json"}
   :body    (json/write-str {:error-message "Not authenticated"})})

(defn- redirect-to-login [request response]
  {:status  302
   :headers {"Location" (str opintopolku-login-url ataru-login-success-url)
             "Content-Type" "text/plain"}
   :body    (str "Access to " (:uri request) " is not authorized, redirecting to login")})

(def ^:private rules [{:pattern #"/js/.*"
                       :handler any-access}
                      {:pattern #"/images/.*"
                       :handler any-access}
                      {:pattern #"/css/.*"
                       :handler any-access}
                      {:pattern #"^/favicon.ico"
                       :handler any-access}
                      {:pattern #"^/lomake-editori/api"
                       :handler authenticated-access
                       :on-error send-not-authenticated-api-response}
                      {:pattern #".*"
                       :handler authenticated-access
                       :on-error redirect-to-login}])

(defn with-authentication [site]
  (-> site
      (wrap-authentication backend)
      (wrap-access-rules {:rules rules})))
