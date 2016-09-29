(ns ataru.person-service.person-service
  (:require [ataru.cas.client :as cas]
            [ataru.person-service.person-client :as person-client]
            [ataru.person-service.person-service-fixtures :as fixtures]
            [com.stuartsierra.component :as component]
            [oph.soresu.common.config :refer [config]]))

(defprotocol PersonService
  (get-person [this search-term]
    "Get a person using a SSN or OID as a search term. Returns
     nil if no search results is produced by the search term."))

(defrecord IntegratedPersonService []
  component/Lifecycle
  PersonService

  (get-person [{:keys [cas-client]} search-term]
    (person-client/get-person cas-client search-term))

  (start [this]
    (assoc this :cas-client (cas/new-client "/authentication-service")))

  (stop [this]
    (assoc this :cas-client nil)))

(defrecord FakePersonService []
  component/Lifecycle
  PersonService

  (get-person [this search-term]
    fixtures/fake-person)

  (start [this]
    this)

  (stop [this]
    this))

(defn new-person-service []
  (if (get-in config [:dev :fake-dependencies])
    (->FakePersonService)
    (->IntegratedPersonService)))
