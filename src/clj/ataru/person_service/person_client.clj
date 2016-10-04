(ns ataru.person-service.person-client
  (:require [ataru.cas.client :as cas]
            [cheshire.core :as json]
            [oph.soresu.common.config :refer [config]]
            [schema.core :as s]))

(defn- base-address []
  (get-in config [:authentication-service :base-address]))

(s/defschema Person
  {:email                        s/Str
   :personId                     s/Str
   :nativeLanguage               (s/maybe s/Str)
   :idpEntitys                   [{:idpEntityId s/Str
                                   :identifier  s/Str}]
   :firstName                    s/Str
   :lastName                     s/Str
   (s/optional-key :nationality) (s/maybe s/Str)
   (s/optional-key :birthDate)   (s/maybe s/Str)
   (s/optional-key :gender)      (s/maybe s/Str)
   (s/optional-key :personOid)   (s/maybe s/Str)})

(s/defn ^:always-validate upsert-person :- Person
  [cas-client :- s/Any
   person :- Person]
  {:pre [(some? (base-address))]}
  (let [url (str (base-address) "/resources/s2s/hakuperusteet")]
    (-> (cas/cas-authenticated-post cas-client url person)
        :body
        slurp
        (json/parse-string true))))
