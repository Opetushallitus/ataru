(ns ataru.virkailija.user.person-client-spec
  (:require [ataru.cas.client :as cas-client]
            [ataru.virkailija.user.person-client :as person-client]
            [cheshire.core :as json]
            [oph.soresu.common.config :refer [config]]
            [ring.util.http-response :as response]
            [speclj.core :refer [describe it tags should=]]))

(def fake-config {:authentication-service {:base-address "dummy"} :cas {}})

(def person {:etunimet   "Aku"
             :hetu       "010101-123N"
             :sukunimi   "Ankka"
             :oidHenkilo "1.2.246.562.24.96282369159"})

(defn- fake-cas-get [client url]
  (-> {:totalCount 1 :results [person]}
      json/generate-string
      response/ok))

(describe "person client"
  (tags :unit)

  (it "searches person by a search parameter"
    (with-redefs [config fake-config
                  cas-client/cas-authenticated-get fake-cas-get]
      (should= person
               (person-client/list-persons nil "010101-123N")))))
