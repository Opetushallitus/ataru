(ns ataru.suoritus.suoritus-service
  (:require [ataru.cas.client :as cas-client]
            [ataru.config.url-helper :as url]
            [cheshire.core :as json]
            [clj-time.format :as format]
            [clojure.core.match :refer [match]]
            [com.stuartsierra.component :as component]))

(def yo-komo "1.2.246.562.5.2013061010184237348007")
(def erikoisammattitutkinto-komo "erikoisammattitutkinto komo oid")
(def ammattitutkinto-komo "ammatillinentutkinto komo oid")
(def ammatillinen-perustutkinto-komo "TODO ammatillinen komo oid")

(defn- ->suoritus-tila
  [data]
  (case (:tila data)
    "VALMIS"      :valmis
    "KESKEN"      :kesken
    "KESKEYTYNYT" :keskeytynyt
    (throw
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

(defn- suoritukset-for-komo
  [cas-client person-oid modified-since komo]
  (match [(cas-client/cas-authenticated-get
           cas-client
           (url/resolve-url
            "suoritusrekisteri.suoritukset"
            (cond-> {"komo" komo}
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

(defn- ylioppilas-ja-ammatilliset-suoritukset [cas-client person-oid modified-since]
  (mapcat (partial suoritukset-for-komo cas-client person-oid modified-since)
          [yo-komo
           ammatillinen-perustutkinto-komo
           ammattitutkinto-komo
           erikoisammattitutkinto-komo]))

(defprotocol SuoritusService
  (ylioppilas-ja-ammatilliset-suoritukset-modified-since [this modified-since])
  (ylioppilas-tai-ammatillinen? [this person-oid]))

(defrecord HttpSuoritusService [suoritusrekisteri-cas-client]
  component/Lifecycle
  (start [this] this)
  (stop [this] this)

  SuoritusService
  (ylioppilas-ja-ammatilliset-suoritukset-modified-since [this modified-since]
    (ylioppilas-ja-ammatilliset-suoritukset suoritusrekisteri-cas-client nil modified-since))
  (ylioppilas-tai-ammatillinen? [this person-oid]
    (some #(= :valmis (:tila %))
          (ylioppilas-ja-ammatilliset-suoritukset suoritusrekisteri-cas-client person-oid nil))))

(defn new-suoritus-service [] (->HttpSuoritusService nil))
