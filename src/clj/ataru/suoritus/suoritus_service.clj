(ns ataru.suoritus.suoritus-service
  (:require [ataru.applications.suoritus-filter :as suoritus-filter]
            [ataru.tarjonta.haku :as haku]
            [ataru.applications.lahtokoulu-util :as lahtokoulu-util]
            [ataru.suoritus.suoritus-client :as client]
            [ataru.time.format :as format]
            [com.stuartsierra.component :as component]
            [ataru.cache.cache-service :as cache]
            [clojure.string :as string]))

(defn- parse-opiskelija
  [opiskelija]
  {:oppilaitos-oid (:oppilaitosOid opiskelija)
   :luokka         (:luokka opiskelija)
   :luokkataso     (:luokkataso opiskelija)
   :alkupaiva      (:alkuPaiva opiskelija)
   :loppupaiva     (:loppuPaiva opiskelija)})

(defn- filter-by-jatkuva-haku-hakemus-hakukausi
  [hakemus-datetime opiskelija]
  (lahtokoulu-util/filter-by-jatkuva-haku-hakemus-hakukausi hakemus-datetime (:loppupaiva opiskelija)))

(defn- filter-opiskelija-by-cutoff-timestamp
  [cutoff-timestamp opiskelija]
  (lahtokoulu-util/filter-opiskelija-by-cutoff-timestamp cutoff-timestamp (:alkupaiva opiskelija) (:loppupaiva opiskelija)))

(defprotocol SuoritusService
  (ylioppilas-ja-ammatilliset-suoritukset-modified-since [this modified-since])
  (ylioppilas-tai-ammatillinen? [this person-oid])
  (oppilaitoksen-opiskelijat [this oppilaitos-oid vuosi luokkatasot])
  (oppilaitoksen-opiskelijat-useammalle-vuodelle [this oppilaitos-oid vuodet luokkatasot])
  (oppilaitoksen-luokat [this oppilaitos-oid vuosi luokkatasot])
  (opiskelijan-luokkatieto [this henkilo-oid vuodet luokkatasot])
  (opiskelijan-luokkatieto-for-hakemus [this henkilo-oid luokkatasot hakemus-datetime tarjonta-info]))

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

  (opiskelijan-luokkatieto [_ henkilo-oid vuodet luokkatasot]
    (->> (mapcat #(client/opiskelijat suoritusrekisteri-cas-client henkilo-oid %) vuodet)
         (map parse-opiskelija)
         (filter #(contains? (set luokkatasot) (:luokkataso %)))
         (sort-by :alkupaiva)
         (last)))

  (opiskelijan-luokkatieto-for-hakemus [_ henkilo-oid luokkatasot hakemus-datetime tarjonta-info]
    (let [hakemus-datetime-formatted (format/parse (:date-time format/formatters) hakemus-datetime)
          [hakuvuodet cutoff-filter] (cond (haku/jatkuva-haku? (:tarjonta tarjonta-info))
                                           [(lahtokoulu-util/resolve-lahtokoulu-vuodet-jatkuva-haku hakemus-datetime-formatted)
                                            (partial filter-by-jatkuva-haku-hakemus-hakukausi hakemus-datetime-formatted)]
                                           :else
                                           (let [hakuvuosi (suoritus-filter/year-for-suoritus-filter hakemus-datetime-formatted)]
                                             [[hakuvuosi]
                                              (partial filter-opiskelija-by-cutoff-timestamp
                                                       (lahtokoulu-util/get-lahtokoulu-cutoff-timestamp hakuvuosi tarjonta-info))]))]
      (->> (mapcat #(client/opiskelijat suoritusrekisteri-cas-client henkilo-oid %) hakuvuodet)
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
