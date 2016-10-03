(ns ataru.person-service.person-client-spec
  (:require [ataru.cas.client :as cas-client]
            [ataru.person-service.person-client :as person-client]
            [cheshire.core :as json]
            [oph.soresu.common.config :refer [config]]
            [ring.util.http-response :as response]
            [speclj.core :refer [describe it tags should= should-be-nil]])
  (:import [java.io ByteArrayInputStream]))

(def fake-config {:authentication-service {:base-address "dummy"} :cas {}})

(def person {:etunimet   "Aku"
             :hetu       "010101-123N"
             :sukunimi   "Ankka"
             :oidHenkilo "1.2.246.562.24.96282369159"
             :email      "aku@ankkalinna.com"})

(defn- fake-cas-get [resp]
  (fn [client url body]
    (should= "dummy/resources/s2s/hakuperusteet"
             url)
    (should= {:personId "010101-123N"
              :email    "aku@ankkalinna.com"}
             body)
    (-> resp
        json/generate-string
        .getBytes
        ByteArrayInputStream.
        response/ok)))

(describe "person client"
  (tags :unit)

  (it "searches person by a search parameter"
    (with-redefs [config fake-config
                  cas-client/cas-authenticated-post (fake-cas-get person)]
      (should= person
               (person-client/get-person nil "010101-123N" "aku@ankkalinna.com")))))
