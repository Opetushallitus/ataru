(ns ataru.suoritus.suoritus-service
  (:require [ataru.suoritus.suoritus-client :as client]
            [com.stuartsierra.component :as component]
            [ataru.cache.cache-service :as cache]))

(defprotocol SuoritusService
  (ylioppilas-ja-ammatilliset-suoritukset-modified-since [this modified-since])
  (ylioppilas-tai-ammatillinen? [this person-oid])
  (oppilaitoksen-opiskelijat [this oppilaitos-oid]))

(defrecord HttpSuoritusService [suoritusrekisteri-cas-client oppilaitoksen-opiskelijat-cache]
  component/Lifecycle
  (start [this] this)
  (stop [this] this)

  SuoritusService
  (ylioppilas-ja-ammatilliset-suoritukset-modified-since [this modified-since]
    (client/ylioppilas-ja-ammatilliset-suoritukset suoritusrekisteri-cas-client nil modified-since))
  (ylioppilas-tai-ammatillinen? [this person-oid]
    (some #(= :valmis (:tila %))
          (client/ylioppilas-ja-ammatilliset-suoritukset suoritusrekisteri-cas-client person-oid nil)))
  (oppilaitoksen-opiskelijat [this oppilaitos-oid]
    (cache/get-from oppilaitoksen-opiskelijat-cache oppilaitos-oid)))

(defn new-suoritus-service [] (->HttpSuoritusService nil nil))

(defrecord OppilaitoksenOpiskelijatCacheLoader [cas-client]
  cache/CacheLoader

  (load [_ oid]
    (client/oppilaitoksen-opiskelijat cas-client oid))

  (load-many [this oppilaitos-oids]
    (cache/default-load-many this oppilaitos-oids))

  (load-many-size [_]
    1)

  (check-schema [_ _]
    nil)
)