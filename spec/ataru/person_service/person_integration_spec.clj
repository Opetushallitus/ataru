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

(def person-service (.start (person-service/->IntegratedPersonService)))

(def application application-fixtures/application-with-person-info-module)
(def fake-config {:authentication-service {:base-address "dummy"}})
(def finnish-person {:email          "aku@ankkalinna.com",
                     :personId       "120496-924J",
                     :nativeLanguage "FI",
                     :nationality    "246",
                     :firstName      "Aku",
                     :lastName       "Ankka",
                     :gender         "2",
                     :idpEntitys     []})

(def finnish-person-with-oid (assoc finnish-person :personOid "1.2.246.562.24.56818753409"))

(describe
 "person-integration/upsert-person"
 (tags :unit)

 (it "updates person oid to application in local database"
     (let [oid-in-db? (atom false)]
       (with-redefs [application-store/add-person-oid (fn [application-id person-oid]
                                                        (should= (:id application) application-id)
                                                        (should= (:personOid finnish-person-with-oid) person-oid)
                                                        (reset! oid-in-db? true))
                     application-store/get-application (fn [id]
                                                         (should= (:id application) id)
                                                         application)
                     config fake-config
                     cas-client/cas-authenticated-post (fn [client url body]
                                                         {:status 200
                                                          :body
                                                          (-> finnish-person-with-oid
                                                              json/generate-string)})]
         (should= {:transition {:id :final}}
                  (person-integration/upsert-person
                   {:application-id (:id application)}
                   {:person-service person-service}))
         (should @oid-in-db?)))))

;; Only relevant fields here
(def foreign-application {:answers [{:key "email",:value "roger.moore@ankkalinna.com"}
                                    {:key "first-name" :value "Roger"}
                                    {:key "last-name" :value "Moore"}
                                    {:key "birth-date" :value "29.10.1984"}
                                    {:key "language" :value "SV"}
                                    {:key "nationality" :value "247"}
                                    {:key "gender" :value "2"}]}) ;; TODO use 1 when it's possible to do so

(def expected-foreign-person {:email          "roger.moore@ankkalinna.com"
                              :nativeLanguage "SV"
                              :nationality    "247"
                              :firstName      "Roger"
                              :lastName       "Moore"
                              :birthDate      "1984-10-29"
                              :gender         "2"
                              :idpEntitys     []})

(describe
 "extract person"
 (tags :unit :extract-person)
 (it "extracts finnish person correctly"
     (should=
      finnish-person
      (person-integration/extract-person  application-fixtures/application-with-person-info-module)))
 (it "extracts foreign person correctly"
     (should=
      expected-foreign-person
      (person-integration/extract-person  foreign-application))))
