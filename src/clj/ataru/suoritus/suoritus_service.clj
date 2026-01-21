(ns ataru.suoritus.suoritus-service
  (:require [ataru.ohjausparametrit.ohjausparametrit-protocol :as ohjausparametrit-service]
            [ataru.suoritus.suorituspalvelu-client :as suorituspalvelu-client]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-service]
            [ataru.tarjonta.haku :as haku]
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

(defprotocol SuoritusService
  (ylioppilas-ja-ammatilliset-suoritukset-modified-since [this modified-since])
  (ylioppilas-tai-ammatillinen? [this person-oid])
  (oppilaitoksen-opiskelijat [this oppilaitos-oid vuosi luokkatasot])
  (oppilaitoksen-opiskelijat-useammalle-vuodelle [this oppilaitos-oid vuodet luokkatasot])

  (opiskelijan-luokkatieto [this henkilo-oid vuodet luokkatasot])

  (oppilaitoksen-luokat [this oppilaitos-oid vuosi])
  (hakemuksen-lahtokoulut [this hakemus])
  (hakemuksen-avainarvot [this hakemus-oid]))

(defn filter-lahtokoulut-active-on-ajanhetki [lahtokoulut ajanhetki]
  (let [paivamaara (coerce/to-local-date ajanhetki)
        lahtokoulut (filter #(let [alkupvm (format/parse-local-date (:alkuPaivamaara %))
                                   loppupvm (format/parse-local-date (:loppuPaivamaara %))
                                   alkanut? (not (time/before? paivamaara alkupvm))
                                   loppunut? (and (some? loppupvm) (not (time/before? paivamaara loppupvm)))]
                              (and alkanut? (not loppunut?))) lahtokoulut)]
    (set lahtokoulut)))

(defn- get-leikkuripvm [ohjausparametrit-service haku-oid]
  (let [ohjausparametrit (ohjausparametrit-service/get-parametri ohjausparametrit-service haku-oid)
        leikkuripaivamaara (or (coerce/to-local-date
                                 (coerce/from-long
                                   (:date (:suoritustenVahvistuspaiva ohjausparametrit))))
                               (time/today))]              ; jos leikkuripäivää ei vielä määritelty käytetään nykyhetkeä
    leikkuripaivamaara))

(defrecord HttpSuoritusService [suoritusrekisteri-cas-client oppilaitoksen-opiskelijat-cache oppilaitoksen-luokat-cache lahtokoulut-cache ohjausparametrit-service tarjonta-service]
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

  (oppilaitoksen-luokat [_ oppilaitos-oid vuosi]
    (let [cache-key (str oppilaitos-oid "#" vuosi)
          luokat (:luokat (cache/get-from oppilaitoksen-luokat-cache cache-key))]
      (log/info "haettiin oppilaitoksen" oppilaitos-oid "luokat vuonna" vuosi)
      luokat))

  (opiskelijan-luokkatieto [_ henkilo-oid vuodet luokkatasot]
    (->> (mapcat #(client/opiskelijat suoritusrekisteri-cas-client henkilo-oid %) vuodet)
         (map parse-opiskelija)
         (filter #(contains? (set luokkatasot) (:luokkataso %)))
         (sort-by :alkupaiva)
         (last)))

  (hakemuksen-lahtokoulut [_ hakemus]
    (let [haku (tarjonta-service/get-haku tarjonta-service (:haku hakemus))
          lahtokoulut (:lahtokoulut (cache/get-from lahtokoulut-cache (:person-oid hakemus)))
          ajanhetki (cond
                      (haku/jatkuva-haku? haku) (:created-time hakemus)
                      (:yhteishaku haku) (get-leikkuripvm ohjausparametrit-service (:haku hakemus))
                      :else nil)
          aktiiviset (if (some? ajanhetki)
                       (filter-lahtokoulut-active-on-ajanhetki lahtokoulut ajanhetki)
                       #{})]
      (log/info "Haettiin lähtökoulut henkilölle" (:person-oid hakemus) "haussa" (:haku hakemus) "ajanhetkellä" (str ajanhetki))
      aktiiviset))

  (hakemuksen-avainarvot [_ hakemus-oid]
    (let [avainarvot (suorituspalvelu-client/hakemuksen-avainarvot hakemus-oid)]
      (log/info "haettiin hakemuksen" hakemus-oid "avainarvot")
      (:avainarvot avainarvot))))

(defn new-suoritus-service [] (->HttpSuoritusService nil nil nil nil nil nil))

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
    (let [[oid vuosi] (string/split key #"#")]
      (suorituspalvelu-client/oppilaitoksen-luokat oid vuosi)))

  (load-many [this oppilaitos-oids]
    (cache/default-load-many this oppilaitos-oids))

  (load-many-size [_]
    1)

  (check-schema [_ _]
    nil))
