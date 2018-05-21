(ns ataru.suoritus.suoritus-service
  (:require [ataru.cas.client :as cas-client]
            [ataru.config.url-helper :as url]
            [cheshire.core :as json]
            [clojure.core.match :refer [match]]
            [com.stuartsierra.component :as component]))

(def yo-komo "1.2.246.562.5.2013061010184237348007")

(defn- ->suoritus-tila
  [data]
  (case (:tila data)
    "VALMIS"      :valmis
    "KESKEN"      :kesken
    "KESKEYTYNYT" :keskeytynyt
    :else         (throw
                   (new RuntimeException
                        (str "Unknown suorituksen tila " (:tila data))))))

(defn- ->suoritus
  [data]
  {:tila (->suoritus-tila data)})

(defn- ylioppilas-suoritukset
  [cas-client person-oid]
  (match [(cas-client/cas-authenticated-get
           cas-client
           (url/resolve-url "suoritusrekisteri.suoritukset"
                            {"henkilo" person-oid
                             "komo"    yo-komo}))]
    [{:status 200 :body s}]
    (map ->suoritus (json/parse-string s true))
    [r]
    (throw (new RuntimeException
                (str "Fetching ylioppilas suoritukset failed: " r)))))

(defprotocol SuoritusService
  (ylioppilas? [this person-oid]))

(defrecord HttpSuoritusService [cas-client]
  component/Lifecycle
  (start [this]
    (assoc this :cas-client (cas-client/new-client "/suoritusrekisteri")))
  (stop [this]
    (assoc this :cas-client nil))

  SuoritusService
  (ylioppilas? [this person-oid]
    (some #(= :valmis (:tila %))
          (ylioppilas-suoritukset cas-client person-oid))))

(defn new-suoritus-service [] (->HttpSuoritusService nil))
