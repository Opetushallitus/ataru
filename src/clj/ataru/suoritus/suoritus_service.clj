(ns ataru.suoritus.suoritus-service
  (:require [ataru.suoritus.suoritus-client :as client]
            [com.stuartsierra.component :as component]
            [ataru.cache.cache-service :as cache]
            [clojure.string :as string]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]))

(defn- parse-opiskelija
  [opiskelija]
  {:oppilaitos-oid (:oppilaitosOid opiskelija)
   :luokka         (:luokka opiskelija)
   :luokkataso     (:luokkataso opiskelija)
   :alkupaiva      (:alkuPaiva opiskelija)
   :loppupaiva     (:loppuPaiva opiskelija)})

(defprotocol SuoritusService
  (ylioppilas-ja-ammatilliset-suoritukset-modified-since [this modified-since])
  (ylioppilas-tai-ammatillinen? [this person-oid])
  (oppilaitoksen-opiskelijat [this oppilaitos-oid vuosi luokkatasot])
  (oppilaitoksen-opiskelijat-useammalle-vuodelle [this oppilaitos-oid vuodet luokkatasot])
  (oppilaitoksen-luokat [this oppilaitos-oid vuosi luokkatasot])
  (opiskelijan-luokkatieto [this henkilo-oid vuodet luokkatasot cutoff-timestamp]))

(defrecord HttpSuoritusService [suoritusrekisteri-cas-client oppilaitoksen-opiskelijat-cache oppilaitoksen-luokat-cache]
  component/Lifecycle
  (start [this] this)
  (stop [this] this)

  SuoritusService
  (ylioppilas-ja-ammatilliset-suoritukset-modified-since [_ modified-since]
    (client/ylioppilas-ja-ammatilliset-suoritukset suoritusrekisteri-cas-client nil modified-since))
  (ylioppilas-tai-ammatillinen? [_ person-oid]
    (some #(= :valmis (:tila %))
          (client/ylioppilas-ja-ammatilliset-suoritukset suoritusrekisteri-cas-client person-oid nil)))
  (oppilaitoksen-opiskelijat [_ oppilaitos-oid vuosi luokkatasot]
    (let [luokkatasot-str (string/join "," luokkatasot)
          cache-key (str oppilaitos-oid "#" vuosi "#" luokkatasot-str)]
      (cache/get-from oppilaitoksen-opiskelijat-cache cache-key)))
  (oppilaitoksen-opiskelijat-useammalle-vuodelle [this oppilaitos-oid vuodet luokkatasot]
    (mapcat #(oppilaitoksen-opiskelijat this oppilaitos-oid % luokkatasot) vuodet))
  (oppilaitoksen-luokat [_ oppilaitos-oid vuosi luokkatasot]
    (let [luokkatasot-str (string/join "," luokkatasot)
          cache-key (str oppilaitos-oid "#" vuosi "#" luokkatasot-str)]
      (cache/get-from oppilaitoksen-luokat-cache cache-key)))
  (opiskelijan-luokkatieto [_ henkilo-oid vuodet luokkatasot cutoff-timestamp]
              ; If there's a cutoff timestamp given, only consider luokka data still ongoing on that date.
              (let [cutoff-fn (fn [opiskelija]
                                (let [start-date (coerce/from-string (:alkupaiva opiskelija))
                                      end-date (coerce/from-string (:loppupaiva opiskelija))
                                      cutoff-date (coerce/from-long cutoff-timestamp)]
                                  (and (some? start-date)
                                       (some? end-date)
                                       (time/before? start-date cutoff-date)
                                       (time/after? end-date cutoff-date))))
                    cutoff-filter (if cutoff-timestamp cutoff-fn some?)]
                (->> (mapcat #(client/opiskelijat suoritusrekisteri-cas-client henkilo-oid %) vuodet)
                     (map parse-opiskelija)
                     (filter #(contains? (set luokkatasot) (:luokkataso %)))
                     (filter cutoff-filter)
                     (sort-by :alkupaiva)
                     (last)))))

(defn new-suoritus-service [] (->HttpSuoritusService nil nil nil))

(defrecord OppilaitoksenOpiskelijatCacheLoader [cas-client]
  cache/CacheLoader

  (load [_ key]
    (let [[oid vuosi luokkatasot] (string/split key #"#")]
      (client/oppilaitoksen-opiskelijat cas-client oid vuosi luokkatasot)))

  (load-many [this oppilaitos-oids]
    (cache/default-load-many this oppilaitos-oids))

  (load-many-size [_]
    1)

  (check-schema [_ _]
    nil)
)

(defrecord OppilaitoksenLuokatCacheLoader [cas-client]
  cache/CacheLoader

  (load [_ key]
    (let [[oid vuosi luokkatasot] (string/split key #"#")]
      (client/oppilaitoksen-luokat cas-client oid vuosi luokkatasot)))

  (load-many [this oppilaitos-oids]
    (cache/default-load-many this oppilaitos-oids))

  (load-many-size [_]
    1)

  (check-schema [_ _]
    nil))
