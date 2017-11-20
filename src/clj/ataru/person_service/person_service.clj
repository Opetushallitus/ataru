(ns ataru.person-service.person-service
  (:require [ataru.cas.client :as cas]
            [ataru.person-service.person-client :as person-client]
            [com.stuartsierra.component :as component]
            [ataru.config.core :refer [config]]))

(defprotocol PersonService
  (create-or-find-person [this person]
    "Create or find a person in Oppijanumerorekisteri.")

  (get-persons [this oids]
    "Find multiple persons from Oppijanumerorekisteri.")

  (get-person [this oid]
    "Find a person from ONR."))

(defrecord IntegratedPersonService []
  component/Lifecycle

  (start [this]
    (assoc
     this
     :oppijanumerorekisteri-cas-client (cas/new-client "/oppijanumerorekisteri-service")))

  (stop [this]
    (assoc
     this
     :oppijanumerorekisteri-cas-client nil))

  PersonService

  (create-or-find-person [{:keys [oppijanumerorekisteri-cas-client]} application]
    (person-client/create-or-find-person oppijanumerorekisteri-cas-client application))

  (get-persons [{:keys [oppijanumerorekisteri-cas-client]} oids]
    (person-client/get-persons oppijanumerorekisteri-cas-client oids))

  (get-person [{:keys [oppijanumerorekisteri-cas-client]} oid]
    (person-client/get-person oppijanumerorekisteri-cas-client oid)))

(def fake-person {:personOid  "1.2.3.4.5.6"
                  :firstName  "Foo"
                  :lastName   "Bar"
                  :email      "foo.bar@mailinator.com"
                  :idpEntitys []})

(defrecord FakePersonService []
  component/Lifecycle
  PersonService

  (start [this] this)
  (stop [this] this)

  (create-or-find-person [this person] fake-person)

  (get-persons [this oids] [fake-person])

  (get-person [this oid] fake-person))

(defn new-person-service []
  (if (-> config :dev :fake-dependencies) ;; Ui automated test mode
    (->FakePersonService)
    (->IntegratedPersonService)))
