(ns ataru.suoritus.suoritus-service
  (:require [ataru.cache.cache-service :as cache]
            [ataru.ohjausparametrit.ohjausparametrit-protocol :as ohjausparametrit-service]
            [ataru.suoritus.suoritus-client :as client]
            [ataru.suoritus.suorituspalvelu-client :as suorituspalvelu-client]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-service]
            [ataru.tarjonta.haku :as haku]
            [clj-time.coerce :as coerce]
            [clj-time.core :as time]
            [clj-time.format :as format]
            [clojure.set :as set]
            [clojure.string :as string]
            [com.stuartsierra.component :as component]
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
  (oppilaitoksen-opiskelijat [this oppilaitos-oid vuosi])
  (oppilaitoksen-opiskelijat-useammalle-vuodelle [this oppilaitos-oid vuodet])

  (opiskelijan-luokkatieto [this henkilo-oid vuodet luokkatasot])

  (oppilaitoksen-luokat [this oppilaitos-oid vuosi])
  (hakemuksen-lahtokoulut [this hakemus])
  (hakemuksen-avainarvot [this hakemus-oid])

  (hakemusten-harkinnanvaraisuus-suorituspalvelusta [this hakemus-oids])
  (hakemusten-harkinnanvaraisuus-suorituspalvelusta-no-cache [this hakemus-oids]))


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

(defn- get-hakemusten-harkinnanvaraisuudet-map [hakemus-oids]
                                         (let [harkinnanvaraisuudet (suorituspalvelu-client/hakemusten-harkinnanvaraisuustiedot hakemus-oids)]
                                           (log/info "Saatiin harkinnanvaraisuudet" harkinnanvaraisuudet)
                                           (reduce (fn [acc hakemuksen-harkinnanvaraisuus]
                                                     (assoc acc (:hakemusOid hakemuksen-harkinnanvaraisuus) hakemuksen-harkinnanvaraisuus)) {} harkinnanvaraisuudet)))

(defrecord HttpSuoritusService [suoritusrekisteri-cas-client
                                oppilaitoksen-opiskelijat-cache
                                oppilaitoksen-luokat-cache
                                lahtokoulut-cache
                                hakemuksen-harkinnanvaraisuus-cache
                                ohjausparametrit-service
                                tarjonta-service]
  component/Lifecycle
  (start [this] this)
  (stop [this] this)

  SuoritusService
  (ylioppilas-ja-ammatilliset-suoritukset-modified-since [_ modified-since]
    (client/ylioppilas-ja-ammatilliset-suoritukset suoritusrekisteri-cas-client nil modified-since))

  (ylioppilas-tai-ammatillinen? [_ person-oid]
    (let [supa-result (suorituspalvelu-client/automaattinen-hakukelpoisuus person-oid)
          automaattisesti-hakukelpoinen (= true (:automaattisestiHakukelpoinen supa-result))]
      (log/info "Automaattinen hakukelpoisuus oppijalle " person-oid ":" automaattisesti-hakukelpoinen "(" supa-result ")")
      automaattisesti-hakukelpoinen))

  (oppilaitoksen-opiskelijat [_ oppilaitos-oid vuosi]
    (let [cache-key (str oppilaitos-oid "#" vuosi)
          opiskelijat (->> (cache/get-from oppilaitoksen-opiskelijat-cache cache-key)
                           :henkilot
                           (map #(set/rename-keys % {:henkiloOid :person-oid})))]
      (log/info "haettiin oppilaitoksen" oppilaitos-oid "opiskelijat" opiskelijat)
      opiskelijat))

  (oppilaitoksen-opiskelijat-useammalle-vuodelle [this oppilaitos-oid vuodet]
    (mapcat #(oppilaitoksen-opiskelijat this oppilaitos-oid %) vuodet))

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
    (if-let [haku-oid (:haku hakemus)]
      (let [haku (tarjonta-service/get-haku tarjonta-service haku-oid)
            lahtokoulut (:lahtokoulut (cache/get-from lahtokoulut-cache (:person-oid hakemus)))
            ajanhetki (cond
                        (haku/jatkuva-haku? haku) (:created-time hakemus)
                        (:yhteishaku haku) (get-leikkuripvm ohjausparametrit-service (:haku hakemus))
                        :else nil)
            aktiiviset (if (some? ajanhetki)
                         (filter-lahtokoulut-active-on-ajanhetki lahtokoulut ajanhetki)
                         #{})]
        (log/info "Haettiin lähtökoulut henkilölle" (:person-oid hakemus) "haussa" (:haku hakemus) "ajanhetkellä" (str ajanhetki))
        aktiiviset)
      (do
        (log/info "Henkilön" (:person-oid hakemus) "Hakemuksella ei hakua, lähtökouluja ei haeta.")
        #{})))

  (hakemuksen-avainarvot [_ hakemus-oid]
    (let [avainarvot (suorituspalvelu-client/hakemuksen-avainarvot hakemus-oid)]
      (log/info "haettiin hakemuksen" hakemus-oid "avainarvot")
      (:avainarvot avainarvot)))

  (hakemusten-harkinnanvaraisuus-suorituspalvelusta [_ hakemus-oids]
    (log/info "Haetaan cachen kautta harkinnanvaraisuustiedot")
    (let [result (cache/get-many-from hakemuksen-harkinnanvaraisuus-cache hakemus-oids)]
      (log/info "Saatiin cachen kautta harkinnanvaraisuustiedot" result)
      result))

  (hakemusten-harkinnanvaraisuus-suorituspalvelusta-no-cache [_ hakemus-oids]
    (let [result (get-hakemusten-harkinnanvaraisuudet-map hakemus-oids)]
      (log/info "Saatiin harkinnanvaraisuudet" result)
      result)))

(defn new-suoritus-service [] (->HttpSuoritusService nil nil nil nil nil nil nil))

(defrecord OppilaitoksenOpiskelijatCacheLoader [cas-client]
  cache/CacheLoader

  (load [_ key]
    (let [[oid vuosi] (string/split key #"#")]
      (suorituspalvelu-client/oppilaitoksen-opiskelijat oid vuosi)))

  (load-many [this keys]
    (cache/default-load-many this keys))

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

  (load-many [this keys]
    (cache/default-load-many this keys))

  (load-many-size [_]
    1)

  (check-schema [_ _]
    nil))

(defrecord HakemuksenHarkinnanvaraisuusCacheLoader [cas-client]
  cache/CacheLoader

  (load [_ oid]
    (get-hakemusten-harkinnanvaraisuudet-map [oid]))

  (load-many [_ oids]
    (get-hakemusten-harkinnanvaraisuudet-map oids))

  (load-many-size [_]
    5000)

  (check-schema [_ _]
    nil))
