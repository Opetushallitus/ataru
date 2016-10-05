(ns ataru.person-service.person-integration-spec
  (:require [ataru.applications.application-store :as application-store]
            [ataru.cas.client :as cas-client]
            [ataru.fixtures.application :as application-fixtures]
            [ataru.person-service.person-integration :as person-integration]
            [ataru.person-service.person-service :as person-service]
            [cheshire.core :as json]
            [com.stuartsierra.component :as component]
            [oph.soresu.common.config :refer [config]]
            [speclj.core :refer [describe it tags should should=]])
  (:import [java.io ByteArrayInputStream]))

(def person-service (.start (person-service/new-person-service)))

(def application application-fixtures/application-with-person-info-module)
(def fake-config {:authentication-service {:base-address "dummy"}})
(def person {:email          "aku@ankkalinna.com",
             :personId       "130580-327L",
             :nativeLanguage nil,
             :nationality    "fi",
             :birthDate      nil,
             :firstName      "Roope",
             :lastName       "Ankka",
             :gender         nil,
             :idpEntitys     [],
             :personOid      "1.2.246.562.24.56818753409"})

(describe "person-integration/upsert-person"
  (tags :unit)

  (it "updates person oid to application in local database"
    (let [oid-in-db? (atom false)]
      (with-redefs [application-store/add-person-oid (fn [application-id person-oid]
                                                       (should= (:id application) application-id)
                                                       (should= (:personOid person) person-oid)
                                                       (reset! oid-in-db? true))
                    application-store/get-application (fn [id]
                                                        (should= (:id application) id)
                                                        application)
                    config fake-config
                    cas-client/cas-authenticated-post (fn [client url body]
                                                        {:body
                                                         (-> person
                                                             json/generate-string
                                                             .getBytes
                                                             ByteArrayInputStream.)})]
        (should= {:transition {:id :final}}
                 (person-integration/upsert-person
                   {:application-id (:id application)}
                   {:person-service person-service}))
        (should @oid-in-db?)))))
