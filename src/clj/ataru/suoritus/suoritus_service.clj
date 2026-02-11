(ns ataru.suoritus.suoritus-service
  (:require [ataru.applications.suoritus-filter :as suoritus-filter]
            [ataru.ohjausparametrit.ohjausparametrit-protocol :as ohjausparametrit-service]
            [ataru.tarjonta.haku :as haku]
            [ataru.applications.lahtokoulu-util :as lahtokoulu-util]
            [ataru.suoritus.suoritus-client :as client]
            [clj-time.core :as time]
            [clj-time.format :as format]
            [clj-time.coerce :as coerce]
            [com.stuartsierra.component :as component]
            [ataru.cache.cache-service :as cache]
            [clojure.string :as string]
            [taoensso.timbre :as log]))

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
  (opiskelijan-luokkatieto-for-hakemus [this henkilo-oid luokkatasot hakemus-datetime tarjonta-info])

  (opiskelijan-leikkuripvm-lahtokoulut [this henkilo-oid haku-oid])

  (opiskelijan-lahtokoulut [this henkilo-oid paivamaara]))

(defn filter-lahtokoulut-active-on-ajanhetki [lahtokoulut ajanhetki]
  (let [paivamaara (coerce/to-local-date ajanhetki)
        lahtokoulut (filter #(let [alkupvm (format/parse-local-date (:alkuPaivamaara %))
                                   loppupvm (format/parse-local-date (:loppuPaivamaara %))
                                   alkanut? (not (time/before? paivamaara alkupvm))
                                   loppunut? (and (some? loppupvm) (not (time/before? paivamaara loppupvm)))]
                              (and alkanut? (not loppunut?))) lahtokoulut)
        oppilaitos-oids (map :oppilaitosOid lahtokoulut)]
    (set oppilaitos-oids)))

(defrecord HttpSuoritusService [suoritusrekisteri-cas-client oppilaitoksen-opiskelijat-cache oppilaitoksen-luokat-cache lahtokoulut-cache ohjausparametrit-service]
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
           (last))))

  (opiskelijan-lahtokoulut [_ henkilo-oid ajanhetki]
    (let [lahtokoulut (cache/get-from lahtokoulut-cache henkilo-oid)
          oppilaitos-oids (filter-lahtokoulut-active-on-ajanhetki (:lahtokoulut lahtokoulut) ajanhetki)]
      (log/info "haettiin henkilön" henkilo-oid "ajanhetken" (str ajanhetki) "lähtökoulut" oppilaitos-oids)
      oppilaitos-oids))

  (opiskelijan-leikkuripvm-lahtokoulut [this henkilo-oid haku-oid]
    (let [ohjausparametrit (ohjausparametrit-service/get-parametri ohjausparametrit-service haku-oid)
          leikkuripaivamaara (or (coerce/to-local-date
                                   (coerce/from-long
                                     (:date (:suoritustenVahvistuspaiva ohjausparametrit))))
                                 (time/today))              ; jos leikkuripäivää ei vielä määritelty käytetään nykyhetkeä
          oppilaitos-oids (opiskelijan-lahtokoulut this henkilo-oid leikkuripaivamaara)]
      (log/info "haettiin henkilön" henkilo-oid "haun" haku-oid "lähtökoulut" oppilaitos-oids)
      oppilaitos-oids)))

(defn new-suoritus-service [] (->HttpSuoritusService nil nil nil nil nil))

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
