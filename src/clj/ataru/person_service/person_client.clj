(ns ataru.person-service.person-client
  (:require [ataru.cas.client :as cas]
            [cheshire.core :as json]
            [oph.soresu.common.config :refer [config]]))

(defn- base-address []
  (get-in config [:authentication-service :base-address]))

(defn get-person [cas-client ssn email]
  {:pre [(some? (base-address))]}
  (let [url  (str (base-address) "/resources/s2s/hakuperusteet")
        body {:personId ssn
              :email    email}]
    (-> (cas/cas-authenticated-post cas-client url body)
        :body
        slurp
        (json/parse-string true))))
