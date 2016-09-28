(ns ataru.virkailija.user.person-client
  (:require [ataru.cas.client :as cas]
            [cheshire.core :as json]
            [oph.soresu.common.config :refer [config]]))

(defn- base-address []
  (get-in config [:authentication-service :base-address]))

(defn list-persons [cas-client search-term]
  {:pre [(some? (base-address))]}
  (let [url (str (base-address)
                 "/resources/henkilo?p=false&q="
                 search-term)]
    (-> (cas/cas-authenticated-get cas-client url)
        :body
        (json/parse-string true)
        :results
        first)))
