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
    "Find a person from ONR.")

  (linked-oids [this oid]))

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
    (person-client/get-person oppijanumerorekisteri-cas-client oid))

  (linked-oids [{:keys [oppijanumerorekisteri-cas-client]} oid]
    (person-client/linked-oids oppijanumerorekisteri-cas-client oid)))

(def fake-person-from-creation {:personOid    "1.2.3.4.5.6"
                  :firstName    "Foo"
                  :lastName     "Bar"
                  :email        "foo.bar@mailinator.com"
                  :idpEntitys   []})

(def fake-onr-person {:oidHenkilo   "1.2.3.4.5.6"
                      :hetu         "020202A0202"
                      :etunimet     "Testi"
                      :kutsumanimi  "Testi"
                      :sukunimi     "Ihminen"
                      :syntymaaika  "1941-06-16"
                      :sukupuoli    "2"
                      :kansalaisuus [{:kansalaisuusKoodi "246"}]
                      :turvakielto  false
                      :yksiloity    false
                      :yksiloityVTJ false})

(defrecord FakePersonService []
  component/Lifecycle
  PersonService

  (start [this] this)
  (stop [this] this)

  (create-or-find-person [this person] fake-person-from-creation)

  (get-persons [this oids]
    (reduce #(assoc %1 %2 (.get-person this %2))
            {}
            oids))

  (get-person [this oid]
    (condp = oid
      "2.2.2" (merge fake-onr-person
                     {:oidHenkilo "2.2.2"
                      :turvakielto true
                      :yksiloity   true
                      :etunimet    "Ari"
                      :kutsumanimi "Ari"
                      :sukunimi    "Vatanen"
                      :hetu         "141196-933S"})
      (merge fake-onr-person
             {:oidHenkilo oid})))

  (linked-oids [this oid]
    {}))

(defn new-person-service []
  (if (-> config :dev :fake-dependencies) ;; Ui automated test mode
    (->FakePersonService)
    (->IntegratedPersonService)))
