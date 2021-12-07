(ns ataru.maksut.maksut-service
  (:require [ataru.maksut.maksut-protocol :refer [MaksutServiceProtocol]]
            [ataru.cas.client :as cas]
            [ataru.config.url-helper :as url]
            [cheshire.core :as json]
            [clojure.core.match :as match]
            [schema.core :as s]))

(s/defschema TutuLaskuCreate
  {:application-key s/Str
   :first-name s/Str
   :last-name s/Str
   :email s/Str
   :amount s/Str
   :index (s/constrained s/Int #(<= 1 % 2) 'valid-tutu-maksu-index)
   })

;(def ^:private hakukohde-valintalaskenta-checker (s/checker HakukohdeValintalaskentaResponse))

(defn throw-error [msg]
  (throw (Exception. msg)))

;   (http (aget js/config "virkailija-caller-id")
;         {:method        :get
;          :url           (str "http://localhost:9099/maksut/api/lasku-tutu/" "1.20.561.0.0.55555" ;application-key
;                              )
;          :handler       [:tutu-payment/handle-fetch-payments
;                          application-key]
;          ;:error-handler [:liitepyynto-information-request/unset-deadline application-key]
;          })

(defn- request-post [maksut-cas-client lasku]
  (let [url       (url/resolve-url :maksut-service.virkailija-create)
        result    (cas/cas-authenticated-post maksut-cas-client url lasku)]
    (match/match result
                 {:status 200 :body body}
                 (json/parse-string body true)

                 :else (throw-error (str "Could not create lasku " lasku ", "
                                         "status: " (:status result)
                                         "response body: "
                                         (:body result))))))

(defn- list-get [maksut-cas-client application-key]
  (let [url    (url/resolve-url :maksut-service.virkailija-list application-key)
        result (cas/cas-authenticated-get maksut-cas-client url)]
    (match/match result
                 {:status 200 :body body}
                 (json/parse-string body true)

                 :else (throw-error (str "Could not create list laskut for " application-key ", "
                                         "status: " (:status result)
                                         "response body: " (:body result))))))


(defrecord MaksutService [maksut-cas-client]
  MaksutServiceProtocol

  (create-kasittely-lasku [this lasku]
    (request-post maksut-cas-client
                  (assoc lasku :index 1)))

  (create-paatos-lasku [this lasku]
    (request-post maksut-cas-client
                  (assoc lasku :index 2)))

  (list-laskut-by-application-key [this application-key]
      (list-get maksut-cas-client application-key)))

(defn new-maksut-service []
  (map->MaksutService {}))

