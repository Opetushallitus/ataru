(ns ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-client
  (:require [ataru.cas.client :as cas]
            [ataru.config.url-helper :as url]
            [cheshire.core :as json]
            [clojure.core.match :refer [match]]))

(defn throw-error [msg]
  (throw (Exception. msg)))

(defn hakukohde-uses-valintalaskenta? [valintalaskentakoostepalvelu-cas-client
                                        hakukohde-oid]
  (let [url    (url/resolve-url :valintalaskentakoostepalvelu-service.hakukohde-uses-valintalaskenta hakukohde-oid)
        result (cas/cas-authenticated-get
                 valintalaskentakoostepalvelu-cas-client
                 url)]
    (match result
           {:status 200 :body body}
           (json/parse-string body true)

           :else (throw-error (str "Could not get hakukohde by oid " hakukohde-oid ", "
                                   "status: " (:status result)
                                   "response body: "
                                   (:body result))))))

(defn hakemusten-harkinnanvaraisuus-valintalaskennasta
  [valintalaskentakoostepalvelu-cas-client hakemukset-with-harkinnanvaraisuus]
  (let [url    (url/resolve-url :valintalaskentakoostepalvelu-service.hakemusten-harkinnanvaraisuus)
        result (cas/cas-authenticated-post
                 valintalaskentakoostepalvelu-cas-client
                 url
                 hakemukset-with-harkinnanvaraisuus)]
    (match result
           {:status 200 :body body}
           (json/parse-string body true)

           :else (throw (new RuntimeException (str "Could not fetch harkinnanvaraisuustieto for "
                                                   (count hakemukset-with-harkinnanvaraisuus)
                                                   " applications from valintalaskentakoostepalvelu. "
                                                   "One of the application oids this operation was done for: "
                                                   (:oid (first hakemukset-with-harkinnanvaraisuus))))))))

(defn opiskelijan-suoritukset
  [valintalaskentakoostepalvelu-cas-client haku-oid hakemus-oid]
  (let [url          (url/resolve-url :valintalaskentakoostepalvelu-service.opiskelijan-suoritukset haku-oid)
        request-body [hakemus-oid]
        result       (cas/cas-authenticated-post
                       valintalaskentakoostepalvelu-cas-client
                       url
                       request-body)]
    (match result
      {:status 200 :body response-body}
      (->> (json/parse-string response-body true)
           vals
           first)

      {:status 204}
      nil

      :else (throw-error (str "Could not get " url ", with body " request-body ", "
                           "status: " (:status result) ", "
                           "response body: "
                           (:body result))))))
