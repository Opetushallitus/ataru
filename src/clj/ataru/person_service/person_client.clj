(ns ataru.person-service.person-client
  (:require
   [taoensso.timbre :as log]
   [cheshire.core :as json]
   [oph.soresu.common.config :refer [config]]
   [schema.core :as s]
   [clojure.core.match :refer [match]]
   [ataru.cas.client :as cas]
   [ataru.person-service.person-schema :refer [Person]])
  (:import
   [java.net URLEncoder]))

(defn- base-address []
  (get-in config [:authentication-service :base-address]))

(defn- oppijanumerorekisteri-base-address []
  (get-in config [:oppijanumerorekisteri-service :base-address]))

(defn throw-error [msg]
  (throw (Exception. msg)))

(defn create-person [cas-client person]
  (let [result (cas/cas-authenticated-post cas-client (str (oppijanumerorekisteri-base-address) "/henkilo") person)]
    (match result
      {:status 201 :body body}
      {:status :created :oid body}

      {:status 400} ;;Request data was invalid, no reason to retry
      {:status :failed-permanently :message (:body result)}

      ;; Assume a temporary error and throw exception, the job will continue to retry
      :else (throw-error (str
                          "Could not create person, status: "
                          (:status result)
                          "response body: "
                          (:body result))))))

(defn check-actual-not-found [body]
  ;; Let's check if the 404 is from the actual service telling us that the person doesn't exist
  ;; instead of just network config issues
  (when (not= 404 (:status (json/parse-string body true)))
    (throw-error (str "Got 404 Not found but not from oppijanumerorekisteri-service. Body: " body))))

(defn person-with-hetu-not-found [cas-client person body]
  (check-actual-not-found body)
  (log/info "Person with id" (:hetu person) "didn't exist in oppijanumerorekisteri yet, trying to create")
  (create-person cas-client person))

(defn person-without-hetu-not-found [cas-client person body]
  (check-actual-not-found body)
  (log/info "Person with email" (:email person) "didn't exist in oppijanumerorekisteri yet, trying to create")
  (create-person cas-client person))

(defn exists-response [body]
  {:status :exists :oid (:oidHenkilo (json/parse-string body true))})

(defn upsert-person-with-hetu [cas-client person]
  (let [url    (str (oppijanumerorekisteri-base-address)
                    "/henkilo/hetu=" (:hetu person))
        result (cas/cas-authenticated-get cas-client url)]
    (match result
      {:status 200 :body body} (exists-response body)
      {:status 404 :body body} (person-with-hetu-not-found cas-client person body)
      :else (throw-error (str "Got error while querying person" result)))))

(defn upsert-person-without-hetu [cas-client person]
  (let [email  (-> person :yhteystieto first :yhteystietoArvo)
        url    (str (oppijanumerorekisteri-base-address)
                    "/henkilo/identification?idp=email&id="
                    (URLEncoder/encode email))
        result (cas/cas-authenticated-get cas-client url)]
    (match result
      {:status 200 :body body} (exists-response body)
      {:status 404 :body body} (person-without-hetu-not-found cas-client person body)
      :else (throw-error (str "Got error while querying person" result)))))

(s/defschema Response
  {:status                   s/Keyword
   (s/optional-key :message) (s/maybe s/Str)
   (s/optional-key :oid)     (s/maybe s/Str)})

(s/defn ^:always-validate upsert-person :- Response
  [cas-client :- s/Any
   person     :- Person]
  {:pre [(some? (oppijanumerorekisteri-base-address))]}
  (if (:hetu person)
    (upsert-person-with-hetu cas-client person)
    (upsert-person-without-hetu cas-client person)))
