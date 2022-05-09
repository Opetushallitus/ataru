(ns ataru.valinta-laskenta-service.valintalaskentaservice-client
  (:require [ataru.cas.client :as cas]
            [ataru.config.url-helper :as url]
            [cheshire.core :as json]
            [clojure.core.match :refer [match]]))

(defn hakemuksen-laskennan-tiedot
  [cas-client haku-oid hakemus-oid]
  (let [url    (url/resolve-url :valintalaskenta-laskenta-service.hakemuksen-tulokset haku-oid hakemus-oid)
        result (cas/cas-authenticated-get
                 cas-client
                 url)]
    (match result
           {:status 200 :body body}
           (json/parse-string body true)

           :else (throw (RuntimeException. (str "Could not get hakemuksen tulokset by oid " hakemus-oid ", "
                                   "status: " (:status result)
                                   "response body: "
                                   (:body result)))))))
