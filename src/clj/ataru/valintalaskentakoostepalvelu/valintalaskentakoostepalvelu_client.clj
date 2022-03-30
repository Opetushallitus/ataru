(ns ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-client
  (:require [ataru.cache.cache-service :as cache]
            [ataru.cas.client :as cas]
            [ataru.config.url-helper :as url]
            [cheshire.core :as json]
            [clojure.core.match :as match]
            [schema.core :as s]))

(defn throw-error [msg]
  (throw (Exception. msg)))

(s/defschema HakukohdeValintalaskentaResponse
  {:kayttaaValintalaskentaa s/Bool})

(def ^:private hakukohde-valintalaskenta-checker (s/checker HakukohdeValintalaskentaResponse))

(defn- hakukohde-uses-valintalaskenta? [valintalaskentakoostepalvelu-cas-client
                                        hakukohde-oid]
  (let [url    (url/resolve-url :valintalaskentakoostepalvelu-service.hakukohde-uses-valintalaskenta hakukohde-oid)
        result (cas/cas-authenticated-get
                 valintalaskentakoostepalvelu-cas-client
                 url)]
    (match/match result
                 {:status 200 :body body}
                 (json/parse-string body true)

                 :else (throw-error (str "Could not get hakukohde by oid " hakukohde-oid ", "
                                         "status: " (:status result)
                                         "response body: "
                                         (:body result))))))

(defn opiskelijan-suoritukset
  [valintalaskentakoostepalvelu-cas-client haku-oid hakemus-oid]
  (let [url          (url/resolve-url :valintalaskentakoostepalvelu-service.opiskelijan-suoritukset haku-oid)
        request-body [hakemus-oid]
        result       (cas/cas-authenticated-post
                       valintalaskentakoostepalvelu-cas-client
                       url
                       request-body)]
    (match/match result
      {:status 200 :body response-body}
      (json/parse-string response-body true)

      :else (throw-error (str "Could not get " url ", with body " request-body ", "
                           "status: " (:status result) ", "
                           "response body: "
                           (:body result))))))

(defrecord HakukohdeValintalaskentaCacheLoader [valintalaskentakoostepalvelu-cas-client]
  cache/CacheLoader

  (load [_ hakukohde-oid]
    (hakukohde-uses-valintalaskenta? valintalaskentakoostepalvelu-cas-client
                                     hakukohde-oid))

  (load-many [_ hakukohde-oids]
    (reduce (fn [acc hakukohde-oid]
              (let [result (hakukohde-uses-valintalaskenta? valintalaskentakoostepalvelu-cas-client
                                                            hakukohde-oid)]
                (assoc acc hakukohde-oid result)))
            {}
            hakukohde-oids))

  (load-many-size [_]
    1)

  (check-schema [_ response]
    (hakukohde-valintalaskenta-checker response)))
