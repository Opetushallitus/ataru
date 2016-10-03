(ns ataru.person-service.person-integration-spec
  (:require [ataru.applications.application-store :as application-store]
            [ataru.fixtures.application :as application-fixtures]
            [ataru.person-service.person-integration :as person-integration]
            [ataru.person-service.person-service :as person-service]
            [ataru.person-service.person-service-fixtures :as person-fixtures]
            [com.stuartsierra.component :as component]
            [speclj.core :refer [describe it tags should should=]]))

(def system (component/start-system
              (component/system-map
                :person-service (person-service/new-person-service))))

(def application application-fixtures/application-with-person-info-module)
(def oid (:oidHenkilo person-fixtures/fake-person))

(describe "store-person-oid"
  (tags :unit)

  (it "should find person oid by ssn"
    (let [oid-in-db? (atom false)]
      (with-redefs [application-store/add-person-oid (fn [application-id person-oid]
                                                       (should= (:id application) application-id)
                                                       (should= oid person-oid)
                                                       (reset! oid-in-db? true))
                    application-store/get-application (fn [id]
                                                        (should= (:id application) id)
                                                        application)]
        (should= {:transition    {:id :final}}
                 (person-integration/store-person-oid
                   {:application-id (:id application)}
                   system))
        (should @oid-in-db?)))))
