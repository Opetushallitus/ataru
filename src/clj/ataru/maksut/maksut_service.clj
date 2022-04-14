(ns ataru.maksut.maksut-service
  (:require [ataru.maksut.maksut-protocol :refer [MaksutServiceProtocol]]
            [ataru.schema.maksut-schema :as maksut-schema]
            [ataru.cas.client :as cas]
            [ataru.config.url-helper :as url]
            [cheshire.core :as json]
            [clojure.core.match :as match]
            [schema.coerce :as c]))

(defn throw-error [msg]
  (throw (Exception. msg)))


(defn- parse-and-validate [body schema]
  (let [parsed (json/parse-string body true)
        coercer (c/coercer! schema c/json-coercion-matcher)]
    (coercer parsed)))

(defn- create-lasku-post [maksut-cas-client lasku]
  (let [url       (url/resolve-url :maksut-service.virkailija-create)
        result    (cas/cas-authenticated-post maksut-cas-client url lasku nil)]
    (match/match result
                 {:status 200 :body body}
                 (parse-and-validate body maksut-schema/Lasku)

                 :else (throw-error (str "Could not create lasku " lasku ", "
                                         "status: " (:status result)
                                         "response body: "
                                         (:body result))))))

(defn- list-get [maksut-cas-client application-key]
  (let [url    (url/resolve-url :maksut-service.virkailija-list application-key)
        result (cas/cas-authenticated-get maksut-cas-client url)]
    (match/match result
                 {:status 200 :body body}
                 (parse-and-validate body maksut-schema/Laskut)

                 :else (throw-error (str "Could not list laskut for " application-key ", "
                                         "status: " (:status result)
                                         "response body: " (:body result))))))


(defn- list-statuses [maksut-cas-client keys]
  (let [url    (url/resolve-url :maksut-service.background-lasku-status)
        req    {:keys keys}
        result (cas/cas-authenticated-post maksut-cas-client url req nil)]
    (match/match result
                 {:status 200 :body body}
                 (parse-and-validate body [maksut-schema/LaskuStatus])

                 :else (throw-error (str "Could not poll Maksut-services for keys " (apply str keys)
                                         " status: " (:status result)
                                         " response body: " (:body result))))))

(defrecord MaksutService [maksut-cas-client]
  MaksutServiceProtocol

  (create-kasittely-lasku [this lasku]
    (create-lasku-post maksut-cas-client
                  (assoc lasku :index 1)))

  (create-paatos-lasku [this lasku]
    (create-lasku-post maksut-cas-client
                  (assoc lasku :index 2)))

  (list-laskut-by-application-key [this application-key]
      (list-get maksut-cas-client application-key))

  (list-lasku-statuses [this keys]
    (list-statuses maksut-cas-client keys)))

(defn new-maksut-service []
  (map->MaksutService {}))

