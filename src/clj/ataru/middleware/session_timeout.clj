(ns ataru.middleware.session-timeout
  (:require [ataru.virkailija.authentication.auth-utils :as auth-utils]
            [cheshire.core :as json]
            [oph.soresu.common.config :refer [config]]
            [ring.middleware.session-timeout :as session-timeout]
            [ring.util.http-response :as response]))

(defn- timeout-handler [{:keys [uri]}]
  (let [auth-url (auth-utils/cas-auth-url)]
    (if (clojure.string/starts-with? uri "/lomake-editori/api")
      (response/unauthorized (json/generate-string {:redirect auth-url}))
      (response/found auth-url))))

(defn- timeout-options []
  {:timeout         (get-in config [:session :timeout])
   :timeout-handler ataru.middleware.session-timeout/timeout-handler})

(defn wrap-idle-session-timeout [handler]
  (let [options (timeout-options)]
    (session-timeout/wrap-idle-session-timeout handler options)))
