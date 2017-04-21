(ns ataru.person-service.person-client
  (:require
   [taoensso.timbre :as log]
   [cheshire.core :as json]
   [ataru.config.core :refer [config]]
   [schema.core :as s]
   [clojure.core.match :refer [match]]
   [ataru.cas.client :as cas]
   [ataru.config.url-helper :refer [resolve-url]]
   [ataru.person-service.person-schema :refer [Person]]
   [ataru.person-service.oppijanumerorekisteri-person-extract :as orpe])
  (:import
   [java.net URLEncoder]))

(defn throw-error [msg]
  (throw (Exception. msg)))

(defn create-person [cas-client person]
  (let [result (cas/cas-authenticated-post
                 cas-client
                 (resolve-url :oppijanumerorekisteri-service.person-create) person)]
    (match result
      {:status 201 :body body}
      {:status :created :oid (:oidHenkilo (json/parse-string body true))}

      {:status 200 :body body}
      {:status :created :oid (:oidHenkilo (json/parse-string body true))}

      {:status 400} ;;Request data was invalid, no reason to retry
      {:status :failed-permanently :message (:body result)}

      ;; Assume a temporary error and throw exception, the job will continue to retry
      :else (throw-error (str
                          "Could not create person, status: "
                          (:status result)
                          "response body: "
                          (:body result))))))

(s/defschema Response
  {:status                   s/Keyword
   (s/optional-key :message) (s/maybe s/Str)
   (s/optional-key :oid)     (s/maybe s/Str)})

(s/defn ^:always-validate upsert-person :- Response
  [cas-client :- s/Any
   person     :- Person]
  (log/info "Sending person to oppijanumerorekisteri" person)
  (create-person cas-client person))

(defn find-or-create-person [oppijanumerorekisteri-cas-client application]
  (upsert-person oppijanumerorekisteri-cas-client (orpe/extract-person-from-application application)))
