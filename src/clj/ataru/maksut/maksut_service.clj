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

(defn- request [maksut-cas-client service lasku]
  ;url-key required for CAS ticket validation, as same service is accessed from two hostnames - and tickets are not valid between them
  (let [url-key   (case service
                      :hakija  :maksut-service.hakija-create
                      :editori :maksut-service.virkailija-create)
        url       (url/resolve-url url-key)
        result    (cas/cas-authenticated-post maksut-cas-client url lasku)]
    (match/match result
                 {:status 200 :body body}
                 (json/parse-string body true)

                 :else (throw-error (str "Could not create lasku " lasku ", "
                                         "status: " (:status result)
                                         "response body: "
                                         (:body result))))))

(defrecord MaksutService [maksut-cas-client]
  MaksutServiceProtocol

  (create-kasittely-lasku [this lasku]
    (request maksut-cas-client :hakija
             (assoc lasku :index 1)))

  (create-paatos-lasku [this lasku]
    (request maksut-cas-client :editori
             (assoc lasku :index 2))))

(defn new-maksut-service []
  (map->MaksutService {}))

