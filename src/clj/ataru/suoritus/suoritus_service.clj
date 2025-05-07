(ns ataru.suoritus.suoritus-service
  (:require [ataru.applications.suoritus-filter :as suoritus-filter]
            [ataru.suoritus.suoritus-client :as client]
            [ataru.tarjonta.haku :as haku]
            [clj-time.format :as format]
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

; In case of peruskoulun jälkeisen koulutuksen yhteishaku, it's specified
; that lähtökoulu information should be based on whatever school the student
; was in up to 1st of June of the application year. In other applications,
; we use application end date.
(defn get-lahtokoulu-cutoff-timestamp
  [hakuvuosi tarjonta-info]
  (let [haku-end (get-in tarjonta-info [:tarjonta :hakuaika :end])
        lahtokoulu-yhteishaku-cutoff-date (time/date-time hakuvuosi 5 30)]
    (if (haku/toisen-asteen-yhteishaku? (:tarjonta tarjonta-info))
      (coerce/to-timestamp lahtokoulu-yhteishaku-cutoff-date)
      haku-end)))

; If there's a cutoff timestamp given, only consider luokka data still ongoing on that date.
(defn- filter-opiskelija-by-cutoff-timestamp
  [cutoff-timestamp opiskelija]
  (let [start-date (coerce/from-string (:alkupaiva opiskelija))
        end-date (coerce/from-string (:loppupaiva opiskelija))
        cutoff-date (coerce/from-long cutoff-timestamp)]
    (and (some? start-date)
         (some? end-date)
         (time/before? start-date cutoff-date)
         (time/after? end-date cutoff-date))))

(defn- in-datetime-period
  [period-start period-end checked-datetime]
  (and (or (time/after? checked-datetime period-start)
           (time/equal? checked-datetime period-start))
       (or (time/before? checked-datetime period-end)
           (time/equal? checked-datetime period-end))))

(defn- filter-opiskelija-by-hakemus-hakukausi
  [hakemus-datetime opiskelija]
  (let [school-end-date (coerce/from-string (:loppupaiva opiskelija))
        school-end-year (time/year school-end-date)
        spring-period-start (time/date-time school-end-year 1 1)
        spring-period-end (time/date-time school-end-year 7 31)
        hakemus-spring-period-end (time/date-time school-end-year 8 31)]
    (if (in-datetime-period spring-period-start spring-period-end school-end-date)
        (in-datetime-period spring-period-start hakemus-spring-period-end hakemus-datetime)
        (in-datetime-period (time/date-time school-end-year 8 1)(time/date-time (+ 1 school-end-year) 1 31)
                            hakemus-datetime))))

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
          hakuvuosi                  (suoritus-filter/year-for-suoritus-filter hakemus-datetime-formatted)
          cutoff-filter (cond (haku/jatkuva-haku? (:tarjonta tarjonta-info))
                              (partial filter-opiskelija-by-hakemus-hakukausi hakemus-datetime-formatted)
                              :else
                              (partial filter-opiskelija-by-cutoff-timestamp
                                       (get-lahtokoulu-cutoff-timestamp hakuvuosi tarjonta-info)))
          ]
      (->> (client/opiskelijat suoritusrekisteri-cas-client henkilo-oid hakuvuosi)
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
