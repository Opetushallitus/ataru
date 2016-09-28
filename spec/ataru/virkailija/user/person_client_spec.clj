(ns ataru.virkailija.user.person-client-spec
  (:require [ataru.cas.client :as cas-client]
            [ataru.virkailija.user.person-client :as person-client]
            [cheshire.core :as json]
            [oph.soresu.common.config :refer [config]]
            [ring.util.http-response :as response]
            [speclj.core :refer [describe it tags should= should-be-nil]])
  (:import [java.io ByteArrayInputStream]))

(def fake-config {:authentication-service {:base-address "dummy"} :cas {}})

(def person1 {:etunimet   "Aku"
              :hetu       "010101-123N"
              :sukunimi   "Ankka"
              :oidHenkilo "1.2.246.562.24.96282369159"})

(def person2 {:etunimet   "Iines"
              :hetu       "010101+123N"
              :sukunimi   "Ankka"
              :oidHenkilo "1.2.246.562.24.96282369160"})

(def person-response {:totalCount 2 :results [person1 person2]}) ; just verify that first one is returned

(def empty-response {:totalCount 0 :results []})

(defn- fake-cas-get [resp]
  (fn [client url]
    (-> resp
        json/generate-string
        .getBytes
        ByteArrayInputStream.
        response/ok)))

(describe "person client"
  (tags :unit)

  (it "searches person by a search parameter"
    (with-redefs [config fake-config
                  cas-client/cas-authenticated-get (fake-cas-get person-response)]
      (should= person1
               (person-client/get-person nil "010101-123N"))))

  (it "returns nil when search produces no results"
    (with-redefs [config fake-config
                  cas-client/cas-authenticated-get (fake-cas-get empty-response)]
      (should-be-nil (person-client/get-person nil "010101-123N")))))
