(ns ataru.suoritus.suoritus-service
  (:require [ataru.cas.client :as cas-client]
            [ataru.config.url-helper :as url]
            [cheshire.core :as json]
            [clj-time.format :as format]
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
                        (str "Unknown suorituksen tila " (:tila data)
                             " in suoritus " data)))))

(defn- ->suoritus-person-oid
  [data]
  (when (clojure.string/blank? (:henkiloOid data))
    (throw (new RuntimeException (str "No henkiloOid in suoritus " data))))
  (:henkiloOid data))

(defn- ->suoritus
  [data]
  {:tila       (->suoritus-tila data)
   :person-oid (->suoritus-person-oid data)})

(defn- format-modified-since
  [modified-since]
  (format/unparse (:date-time format/formatters)
                  modified-since))

(defn- ylioppilas-suoritukset
  [cas-client person-oid modified-since]
  (match [(cas-client/cas-authenticated-get
           cas-client
           (url/resolve-url
            "suoritusrekisteri.suoritukset"
            (cond-> {"komo" yo-komo}
                    (some? person-oid)
                    (assoc "henkilo" person-oid)
                    (some? modified-since)
                    (assoc "muokattuJalkeen"
                           (format-modified-since modified-since)))))]
    [{:status 200 :body s}]
    (map ->suoritus (json/parse-string s true))
    [r]
    (throw (new RuntimeException
                (str "Fetching ylioppilas suoritukset failed: " r)))))

(defprotocol SuoritusService
  (ylioppilas-suoritukset-modified-since [this modified-since])
  (ylioppilas? [this person-oid]))

(defrecord HttpSuoritusService [cas-client]
  component/Lifecycle
  (start [this]
    (assoc this :cas-client (cas-client/new-client "/suoritusrekisteri")))
  (stop [this]
    (assoc this :cas-client nil))

  SuoritusService
  (ylioppilas-suoritukset-modified-since [this modified-since]
    (ylioppilas-suoritukset cas-client nil modified-since))
  (ylioppilas? [this person-oid]
    (some #(= :valmis (:tila %))
          (ylioppilas-suoritukset cas-client person-oid nil))))

(defn new-suoritus-service [] (->HttpSuoritusService nil))
