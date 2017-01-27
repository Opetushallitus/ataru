(ns ataru.person-service.person-client
  (:require [ataru.cas.client :as cas]
            [cheshire.core :as json]
            [oph.soresu.common.config :refer [config]]
            [schema.core :as s]))

(defn- base-address []
  (get-in config [:authentication-service :base-address]))

(defn- oppijanumerorekisteri-base-address []
  (get-in config [:oppijanumerorekisteri-service :base-address]))

; This schema is just "internal" part of the person-client ns
; and therefore it isn't placed into form-schema.cljc
(s/defschema Person
  {(s/optional-key :personId)    (s/maybe s/Str)
   (s/optional-key :birthDate)   (s/maybe s/Str)
   :nativeLanguage               (s/maybe s/Str)
   :email                        s/Str
   :idpEntitys                   [{:idpEntityId s/Str
                                   :identifier  s/Str}]
   :firstName                    s/Str
   :lastName                     s/Str
   (s/optional-key :nationality) (s/maybe s/Str)
   (s/optional-key :gender)      (s/maybe s/Str)
   (s/optional-key :personOid)   (s/maybe s/Str)})

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

(defn upsert-person2 [cas-client person]
  {:pre [(some? (oppijanumerorekisteri-base-address))]}
  (println (cas/cas-authenticated-get
             cas-client
             (str (oppijanumerorekisteri-base-address)
                  "/henkilo/hetu=" (:personId person)))))
