(ns ataru.person-service.person-service
  (:require [ataru.cas.client :as cas]
            [ataru.person-service.person-client :as person-client]
            [com.stuartsierra.component :as component]
            [oph.soresu.common.config :refer [config]]))

(defprotocol PersonService
  (upsert-person [this person]
    "Upsert a person to person service. Returns the possibly updated
     person as returned to the PersonService by the remote person
     service."))

(defrecord IntegratedPersonService []
  component/Lifecycle
  PersonService

  (upsert-person [{:keys [cas-client]} person]
    (person-client/upsert-person cas-client person))

  (start [this]
    (assoc this :cas-client (cas/new-client "/authentication-service")))

  (stop [this]
    (assoc this :cas-client nil)))

(defrecord FakePersonService []
  component/Lifecycle
  PersonService

  (start [this] this)
  (stop [this] this)

  (upsert-person [this person] {:personOid  "1.2.3.4.5.6"
                                :firstName  "Foo"
                                :lastName   "Bar"
                                :email      "foo.bar@mailinator.com"
                                :idpEntitys []}))

(defn new-person-service []
  (if (-> config :dev :fake-dependencies) ;; Ui automated test mode
    (->FakePersonService)
    (->IntegratedPersonService)))
