(ns ataru.suoritus.suoritus-client
  (:require [ataru.cas.client :as cas-client]
            [ataru.config.url-helper :as url]
            [cheshire.core :as json]
            [clj-time.format :as format]
            [clojure.core.match :refer [match]]
            [clojure.string :as string]))

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
  (when (string/blank? (:henkiloOid data))
    (throw (new RuntimeException (str "No henkiloOid in suoritus " data))))
  (:henkiloOid data))

(defn- ->suoritus
  [data]
  {:tila       (->suoritus-tila data)
   :person-oid (->suoritus-person-oid data)})

(defn- ->student-ja-luokka
  [data]
  {:person-oid (:henkiloOid data)
   :luokka (:luokka data)})

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

(defn oppilaitoksen-opiskelijat
  [cas-client oppilaitos-oid vuosi luokkatasot]
  (let [url (url/resolve-url
              "suoritusrekisteri.oppilaitoksenopiskelijat"
              oppilaitos-oid
              (cond-> {}
                (some? vuosi)
                (assoc "vuosi" vuosi)
                (some? luokkatasot)
                (assoc "luokkaTasot" luokkatasot)))]
    (match [(cas-client/cas-authenticated-get
              cas-client
              url)]
      [{:status 200 :body body}]
      (map ->student-ja-luokka (json/parse-string body true))
      [r]
      (throw (new RuntimeException
               (str "Fetching oppilaitoksen opiskelijat failed: " r))))))

(defn ylioppilas-ja-ammatilliset-suoritukset [cas-client person-oid modified-since]
  (mapcat (partial suoritukset-for-komo cas-client person-oid modified-since)
          [yo-komo
           ammatillinen-perustutkinto-komo
           ammattitutkinto-komo
           erikoisammattitutkinto-komo]))

(defn opiskelijat [cas-client henkilo-oid vuosi]
  (let [url (url/resolve-url
              "suoritusrekisteri.opiskelijat"
              (cond-> {"henkilo" henkilo-oid}
                (some? vuosi)
                (assoc "vuosi" vuosi)))]
    (match [(cas-client/cas-authenticated-get
              cas-client
              url)]
      [{:status 200 :body body}]
      (json/parse-string body true)
      [r]
      (throw (new RuntimeException
               (str "Fetching opiskelijat failed: " r))))))
