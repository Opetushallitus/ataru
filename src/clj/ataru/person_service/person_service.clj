(ns ataru.person-service.person-service
  (:require [ataru.cas.client :as cas]
            [ataru.person-service.person-client :as person-client]
            [com.stuartsierra.component :as component]
            [ataru.config.core :refer [config]]))

(defprotocol PersonService
  (create-or-find-person [this person]
    "Create or find a person in Oppijanumerorekisteri."))

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
    (person-client/create-or-find-person oppijanumerorekisteri-cas-client application)))

(defrecord FakePersonService []
  component/Lifecycle
  PersonService

  (start [this] this)
  (stop [this] this)

  (create-or-find-person [this person] {:personOid  "1.2.3.4.5.6"
                                        :firstName  "Foo"
                                        :lastName   "Bar"
                                        :email      "foo.bar@mailinator.com"
                                        :idpEntitys []}))

(defn new-person-service []
  (if (-> config :dev :fake-dependencies) ;; Ui automated test mode
    (->FakePersonService)
    (->IntegratedPersonService)))
