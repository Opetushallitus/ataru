(ns ataru.person-service.person-service
  (:require [taoensso.timbre :as log]
            [ataru.cas.client :as cas]
            [ataru.person-service.person-client :as person-client]
            [ataru.person-service.oppijanumerorekisteri-person-extract :as orpe]
            [com.stuartsierra.component :as component]
            [ataru.config.core :refer [config]]
            [ataru.cache.cache-service :as cache]
            [ataru.util :as util]))

(defprotocol PersonService
  (create-or-find-person [this person]
    "Create or find a person in Oppijanumerorekisteri.")

  (get-persons [this oids]
    "Find multiple persons from Oppijanumerorekisteri.")

  (get-person [this oid]
    "Find a person from ONR.")

  (linked-oids [this oid]))

(defrecord IntegratedPersonService [henkilo-cache]
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
    (person-client/create-or-find-person
     oppijanumerorekisteri-cas-client
     (orpe/extract-person-from-application application)))

  (get-persons [{:keys [oppijanumerorekisteri-cas-client]} oids]
    (if (seq oids)
      (let [persons-from-cache  (cache/get-many-from henkilo-cache oids)
            uncached-oids       (clojure.set/difference
                                 (set oids)
                                 (set (keys persons-from-cache)))
            persons-from-client (person-client/get-persons oppijanumerorekisteri-cas-client uncached-oids)]
        (log/info "Using" (count persons-from-cache) "persons from cache")
        (when (not-empty persons-from-client)
          (log/info "Caching" (count persons-from-client) "persons")
          (cache/put-many-to henkilo-cache persons-from-client))
        (merge persons-from-cache persons-from-client))
      {}))

  (get-person [{:keys [oppijanumerorekisteri-cas-client]} oid]
    (cache/get-from-or-fetch
     henkilo-cache
     (partial person-client/get-person oppijanumerorekisteri-cas-client)
     oid))

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
                      :aidinkieli   {:id          "742310"
                                     :kieliKoodi  "fi"
                                     :kieliTyyppi "suomi"}
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
    (->IntegratedPersonService nil)))
