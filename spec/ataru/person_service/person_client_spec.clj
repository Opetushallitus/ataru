(ns ataru.person-service.person-client-spec
  (:require [ataru.cas.client :as cas-client]
            [ataru.person-service.person-client :as person-client]
            [cheshire.core :as json]
            [oph.soresu.common.config :refer [config]]
            [ring.util.http-response :as response]
            [speclj.core :refer [describe it tags should= should-be-nil]])
  (:import [java.io ByteArrayInputStream]))

(def fake-config {:authentication-service {:base-address "dummy"} :cas {}})

(def person-before {:email          "aku@ankkalinna.com",
                    :personId       "130580-327L",
                    :nativeLanguage "sv",
                    :idpEntitys     [],
                    :firstName      "Roope",
                    :lastName       "Ankka"})

(def person-after {:email          "aku@ankkalinna.com",
                   :personId       "130580-327L",
                   :nativeLanguage nil,
                   :nationality    "fi",
                   :birthDate      nil,
                   :firstName      "Roope",
                   :lastName       "Ankka",
                   :gender         nil,
                   :idpEntitys     [],
                   :personOid      "1.2.246.562.24.56818753409"})

(defn- fake-cas-get [resp]
  (fn [client url body]
    (should= "dummy/resources/s2s/hakuperusteet"
             url)
    (should= person-before
             body)
    (-> resp
        json/generate-string
        response/ok)))

(describe "person-client/upsert-person"
  (tags :unit)

  (it "upserts person to remote service"
    (with-redefs [config fake-config
                  cas-client/cas-authenticated-post (fake-cas-get person-after)]
      (should= person-after
               (person-client/upsert-person nil person-before)))))
