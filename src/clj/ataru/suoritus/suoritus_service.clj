(ns ataru.suoritus.suoritus-service
  (:require [ataru.suoritus.suoritus-client :as client]
            [com.stuartsierra.component :as component]
            [ataru.cache.cache-service :as cache]
            [clojure.string :as string]))

(defprotocol SuoritusService
  (ylioppilas-ja-ammatilliset-suoritukset-modified-since [this modified-since])
  (ylioppilas-tai-ammatillinen? [this person-oid])
  (oppilaitoksen-opiskelijat [this oppilaitos-oid vuosi])
  (oppilaitoksen-luokat [this oppilaitos-oid vuosi]))

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
  (oppilaitoksen-opiskelijat [_ oppilaitos-oid vuosi]
    (let [cache-key (str oppilaitos-oid "#" vuosi)]
      (cache/get-from oppilaitoksen-opiskelijat-cache cache-key)))
  (oppilaitoksen-luokat [_ oppilaitos-oid vuosi]
    (let [cache-key (str oppilaitos-oid "#" vuosi)]
      (cache/get-from oppilaitoksen-luokat-cache cache-key))))

(defn new-suoritus-service [] (->HttpSuoritusService nil nil nil))

(defrecord OppilaitoksenOpiskelijatCacheLoader [cas-client]
  cache/CacheLoader

  (load [_ key]
    (let [[oid vuosi] (string/split key #"#")]
      (client/oppilaitoksen-opiskelijat cas-client oid vuosi)))

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
    (let [[oid vuosi] (string/split key #"#")]
      (client/oppilaitoksen-luokat cas-client oid vuosi)))

  (load-many [this oppilaitos-oids]
    (cache/default-load-many this oppilaitos-oids))

  (load-many-size [_]
    1)

  (check-schema [_ _]
    nil))
