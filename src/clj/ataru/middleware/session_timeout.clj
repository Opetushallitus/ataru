(ns ataru.middleware.session-timeout
  "Middleware for managing session timeouts.
  \"Forked\" from https://github.com/ring-clojure/ring-session-timeout"
  (:require [ataru.virkailija.authentication.auth-utils :as auth-utils]
            [cheshire.core :as json]
            [oph.soresu.common.config :refer [config]]
            [ring.util.http-response :as response]))

(defn- current-time []
  (quot (System/currentTimeMillis) 1000))

(defn- wrap-session-timeout? [{:keys [uri]}]
  (and (not (clojure.string/starts-with? uri "/lomake-editori/auth"))
       (not (clojure.string/starts-with? uri "/lomake-editori/cas"))
       (not (clojure.string/starts-with? uri "/lomake-editori/js"))
       (not (clojure.string/starts-with? uri "/lomake-editori/css"))))

(defn- timeout-handler [{:keys [uri]}]
  (let [auth-url (auth-utils/cas-auth-url)]
    (if (clojure.string/starts-with? uri "/lomake-editori/api")
      (response/unauthorized (json/generate-string {:redirect auth-url}))
      (response/found auth-url))))

(defn wrap-idle-session-timeout
  "Middleware that times out idle sessions after a specified number of seconds.

  If a session is timed out, the timeout-response option is returned. This is
  usually a redirect to the login page. Alternatively, the timeout-handler
  option may be specified. This should contain a Ring handler function that
  takes the current request and returns a timeout response."
  {:arglists '([handler])}
  [handler]
  (fn [request]
    (let [session  (:session request {})
          end-time (::idle-timeout session)
          timeout  (get-in config [:session :timeout])]
      (if (and (wrap-session-timeout? request) end-time (< end-time (current-time)))
        (assoc (timeout-handler request) :session nil)
        (when-let [response (handler request)]
          (let [session (:session response session)]
            (if (nil? session)
              response
              (let [end-time (+ (current-time) timeout)]
                (assoc response :session (assoc session ::idle-timeout end-time))))))))))
