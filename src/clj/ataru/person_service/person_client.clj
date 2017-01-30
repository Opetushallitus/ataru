(ns ataru.person-service.person-client
  (:require
   [taoensso.timbre :as log]
   [cheshire.core :as json]
   [oph.soresu.common.config :refer [config]]
   [schema.core :as s]
   [clojure.core.match :refer [match]]
   [ataru.cas.client :as cas]
   [ataru.person-service.person-schema :refer [Person]]))

(defn- base-address []
  (get-in config [:authentication-service :base-address]))

(defn- oppijanumerorekisteri-base-address []
  (get-in config [:oppijanumerorekisteri-service :base-address]))

(s/defn ^:always-validate upsert-person :- Person
  [cas-client :- s/Any
   person :- Person]
  {:pre [(some? (base-address))]}
  (let [url      (str (base-address) "/resources/s2s/hakuperusteet")
        response (cas/cas-authenticated-post cas-client url person)]
    (if (= 200 (:status response))
      (json/parse-string (:body response) true)
      (throw (Exception.
              (str "Failed to upsert person, got status code "
                   (:status response)
                   " body: "
                   (:body response)))))))

(defn throw-error [msg]
  (throw (Exception. msg)))

(defn create-person [cas-client person]
  (let [result (cas/cas-authenticated-post cas-client (str (oppijanumerorekisteri-base-address) "/henkilo") person)]
    (match result
      {:status 201 :body body}
      {:status :created :oid body}

      {:status 400} ;;Request data was invalid, no reason to retry
      {:status :failed-permanently :message (get-in result [:body :message])}

      :else (throw-error (str
                          "Could not create person, status: "
                          (:status result)
                          "response body: "
                          (:body result))))))

(defn person-with-hetu-not-found [cas-client person body]
  (let [parsed-response (json/parse-string body true)]
    ;; Let's check if the 404 is from the actual service telling us that the person doesn't exist
    ;; instead of just network config issues
    (if (= 404 (:status parsed-response))
      (do
        (log/info "Person with id" (:hetu person) "didn't exist in oppijanumerorekisteri yet, trying to create")
        (create-person cas-client person))
      (throw-error (str "Got 404 Not found but not from oppijanumerorekisteri-service. Body: " body)))))

(defn upsert-person-with-hetu [cas-client person]
  (let [url    (str (oppijanumerorekisteri-base-address)
                    "/henkilo/hetu=" (:hetu person))
        result (cas/cas-authenticated-get cas-client url)]
    (match result
      {:status 200 :body body} {:status :exists :oid (:oidHenkilo (json/parse-string body true))}
      {:status 404 :body body} (person-with-hetu-not-found cas-client person body)
      :else (throw-error (str "Got error while querying person" result)))))

(s/defschema Response
  {:status                   s/Keyword
   (s/optional-key :message) (s/maybe s/Str)
   (s/optional-key :oid)     (s/maybe s/Str)})

(s/defn ^:always-validate upsert-person2 :- Response
  [cas-client :- s/Any
   person     :- Person]
  {:pre [(some? (oppijanumerorekisteri-base-address))]}
  (if (:hetu person)
    (upsert-person-with-hetu cas-client person)
    (println "No personid, implement!")))
