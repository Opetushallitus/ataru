(ns ataru.odw.odw-service
  (:require [ataru.applications.application-store :as application-store]
            [ataru.applications.answer-util :as answer-util]
            [ataru.util :as util]
            [clojure.set]
            [clj-time.core :as t]
            [ataru.tarjonta.haku :as h]
            [ataru.applications.suoritus-filter :as suoritus-filter]
            [ataru.koodisto.koodisto-codes :refer [finland-country-code]]
            [ataru.person-service.person-service :as person-service]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-protocol]
            [taoensso.timbre :as log]
            [ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-protocol :as valintalaskentakoostepalvelu]
            [ataru.suoritus.suoritus-service :as suoritus-service]))

(defn- get-hakukelpoisuus
  [application hakukohde-oid]
  (case (get-in application [:eligibilities (keyword hakukohde-oid)] "unreviewed")
    "unreviewed" "NOT_CHECKED"
    "eligible" "ELIGIBLE"
    "uneligible" "INELIGIBLE"
    "conditionally-eligible" "CONDITIONALLY_ELIGIBLE"))

(defn- get-maksuvelvollisuus
  [application hakukohde-oid]
  (case (get-in application [:payment-obligations (keyword hakukohde-oid)] "unreviewed")
    "unreviewed" "NOT_CHECKED"
    "obligated" "REQUIRED"
    "not-obligated" "NOT_REQUIRED"))

(defn get-yksiloimaton-tai-aidinkieleton-henkilo-oids
  [persons]
  (set (map :oidHenkilo (filter (fn [person] (or (and (not (:yksiloity person)) (not (:yksiloityVTJ person)))
                                                 (empty? (get-in person [:aidinkieli :kieliKoodi]))))
                                (vals persons)))))

(defn- filter-applications-for-koostedata [toinen-aste-applications persons]
  (let [yksiloimaton-tai-aidinkieleton-henkilo-oids (get-yksiloimaton-tai-aidinkieleton-henkilo-oids persons)
        persons-from-applications (set (map :person-oid toinen-aste-applications))
        dropped-oids (clojure.set/intersection yksiloimaton-tai-aidinkieleton-henkilo-oids persons-from-applications)
        wanted-applications (filter #(not (contains? yksiloimaton-tai-aidinkieleton-henkilo-oids (:person-oid %))) toinen-aste-applications)]
    (when (not-empty dropped-oids)
      (log/info (str "ODW Ei haeta koosteDataa yksilöimättömille tai äidinkielettömille hakijoille: " dropped-oids)))
    wanted-applications))

(defn get-applications-for-odw [person-service tarjonta-service valintalaskentakoostepalvelu-service suoritus-service date limit offset to-date haku-oid application-key]
  (let [applications (cond
                       application-key
                       (application-store/get-latest-application-by-key-for-odw application-key)

                       haku-oid
                       (application-store/get-latest-applications-by-haku haku-oid limit offset)

                       (and date to-date)
                       (application-store/get-applications-between-start-and-end date to-date limit offset)

                       date
                       (application-store/get-applications-newer-than date limit offset)

                       :else
                       [])
        active-applications (filter #(not= (:state %) "inactivated") applications)
        haut (->> (keep :haku applications)
                  distinct
                  (map (fn [oid] [oid (tarjonta-protocol/get-haku tarjonta-service oid)]))
                  (into {}))
        persons (person-service/get-persons person-service (distinct (keep :person-oid applications)))
        toisen-asteen-yhteishaut (into {} (filter #(->> % val h/toisen-asteen-yhteishaku?) haut))
        toisen-asteen-yhteishaku-oids (set (map key toisen-asteen-yhteishaut))
        toisen-asteen-yhteishakujen-hakemukset (filter (fn [application] (contains? toisen-asteen-yhteishaku-oids (:haku application))) active-applications)
        toisen-asteen-yhteishakujen-hakemusten-oidit (map :key toisen-asteen-yhteishakujen-hakemukset)
        harkinnanvaraisuus-by-hakemus (if (not-empty toisen-asteen-yhteishakujen-hakemusten-oidit)
                                        (valintalaskentakoostepalvelu/hakemusten-harkinnanvaraisuus-valintalaskennasta valintalaskentakoostepalvelu-service toisen-asteen-yhteishakujen-hakemusten-oidit)
                                        {})
        applications-for-koostedata (filter-applications-for-koostedata toisen-asteen-yhteishakujen-hakemukset persons)
        kooste-data-toinen-aste (into {} (map (fn [hakuOid] (let [hakemus-oids (vec (doall (map :key (filter (fn [application] (= hakuOid (:haku application)))
                                                                                                             applications-for-koostedata))))]
                                                              (if (not-empty hakemus-oids)
                                                                (do (log/info "ODW Haetaan koosteData haulle" hakuOid ",   hakemusOids " hakemus-oids)
                                                                    (valintalaskentakoostepalvelu/opiskelijoiden-suoritukset valintalaskentakoostepalvelu-service hakuOid hakemus-oids))
                                                                (do (log/warn "ODW Ei haeta koosteDataa haulle" hakuOid "koska on vain passiivisia tai yksilöimättömiä hakemuksia")
                                                                    {}))))
                                              toisen-asteen-yhteishaku-oids))
        results (map (fn [application]
                       (try
                         [nil (let [answers (-> application :content :answers util/answers-by-key)
                                    application-year (-> application :submitted t/year)
                                    haku (get haut (:haku application))
                                    toinen-aste? (h/toisen-asteen-yhteishaku? haku)
                                    hakukohteet (:hakukohde application)
                                    person-oid (:person-oid application)
                                    person (get persons person-oid)
                                    state (:state application)
                                    foreign? (not= finland-country-code (-> answers :country-of-residence :value))
                                    hakutoiveiden-harkinnanvaraisuudet (get-in harkinnanvaraisuus-by-hakemus [(:key application) :hakutoiveet] [])]
                                (merge {:oid                              (:key application)
                                        :person_oid                       person-oid
                                        :application_system_oid           (:haku application)
                                        :puhelin                          (-> answers :phone :value)
                                        :sahkoposti                       (-> answers :email :value)
                                        :asuinmaa                         (-> answers :country-of-residence :value)
                                        :student_oid                      (-> person :oppijanumero)
                                        :aidinkieli                       (-> person :aidinkieli :kieliKoodi)
                                        :kansalaisuus                     (-> person :kansalaisuus first :kansalaisuusKoodi)
                                        :sukunimi                         (-> person :sukunimi)
                                        :etunimet                         (-> person :etunimet)
                                        :kutsumanimi                      (-> person :kutsumanimi)
                                        :syntymaaika                      (-> person :syntymaaika)
                                        :turvakielto                      (-> person :turvakielto)
                                        :hetu                             (-> person :hetu)
                                        :sukupuoli                        (-> person :sukupuoli util/gender-int-to-string)
                                        :SahkoinenViestintaLupa           (-> answers :sahkoisen-asioinnin-lupa :value (= "Kyllä"))
                                        :julkaisulupa                     (-> answers :valintatuloksen-julkaisulupa :value (= "Kyllä"))
                                        :koulutusmarkkinointilupa         (-> answers :koulutusmarkkinointilupa :value (= "Kyllä"))
                                        :pohjakoulutuksen_maa_toinen_aste (-> answers :secondary-completed-base-education--country :value)
                                        :state                            (if (= state "inactivated")
                                                                            "PASSIVE"
                                                                            "ACTIVE")
                                        :kk_pohjakoulutus                 (answer-util/get-kk-pohjakoulutus (get haut (:haku application)) answers (:key application))}
                                       (when (and toinen-aste? (not= state "inactivated"))
                                         (let [koosteData (get kooste-data-toinen-aste (keyword person-oid))
                                               pohjakoulutus (:POHJAKOULUTUS koosteData)
                                               opetuskieli (:perusopetuksen_kieli koosteData)
                                               suoritusvuosi (:pohjakoulutus_vuosi koosteData)
                                               luokkatieto (suoritus-service/opiskelijan-luokkatieto suoritus-service person-oid (vector application-year) (suoritus-filter/luokkatasot-for-suoritus-filter))
                                               lahtoluokka (:luokka luokkatieto)
                                               luokkataso (:luokkataso luokkatieto)
                                               lahtokoulu-oid (:oppilaitos-oid luokkatieto)]
                                           {:pohjakoulutus-2nd                 pohjakoulutus
                                            :pohjakoulutus-2nd-suoritusvuosi   suoritusvuosi
                                            :pohjakoulutus-2nd-suorituskieli   opetuskieli
                                            :pohjakoulutus-2nd-lahtoluokka     lahtoluokka
                                            :pohjakoulutus-2nd-luokkataso      luokkataso
                                            :pohjakoulutus-2nd-lahtokoulu-oid  lahtokoulu-oid}))
                                       (if foreign?
                                         {:Ulk_postiosoite (-> answers :address :value)
                                          :Ulk_postinumero (-> answers :postal-code :value)
                                          :Ulk_kunta       (-> answers :city :value)}
                                         {:lahiosoite  (-> answers :address :value)
                                          :postinumero (-> answers :postal-code :value)
                                          :kotikunta   (-> answers :home-town :value)})
                                       (into {}
                                             (for [index (range 1 (inc (count hakukohteet)))
                                                   :let [hakukohde-oid (nth hakukohteet (dec index) nil)
                                                         hakukohde (when hakukohde-oid
                                                                     (tarjonta-protocol/get-hakukohde tarjonta-service hakukohde-oid))
                                                         harkinnanvaraisuuden-syy (first (filter #(= (:hakukohdeOid %) hakukohde-oid) hakutoiveiden-harkinnanvaraisuudet))
                                                         tarjoaja-oid (-> hakukohde :tarjoaja-oids first)]]
                                               {(keyword (str "pref" index "_hakukohde_oid"))     hakukohde-oid
                                                (keyword (str "pref" index "_opetuspiste_oid"))   tarjoaja-oid
                                                (keyword (str "pref" index "_sora"))              nil
                                                (keyword (str "pref" index "_harkinnanvarainen")) (get harkinnanvaraisuuden-syy :harkinnanvaraisuudenSyy)
                                                (keyword (str "pref" index "_hakukelpoisuus"))    (get-hakukelpoisuus application hakukohde-oid)
                                                (keyword (str "pref" index "_maksuvelvollisuus")) (get-maksuvelvollisuus application hakukohde-oid)}))))]
                         (catch Exception e
                           [[e (:key application)] nil])))
                     applications)]
    (doseq [[e application-key] (keep first results)]
      (log/error e "Failed to parse ODW data of application" application-key))
    (if (some (comp some? first) results)
      (throw (new RuntimeException "Failed to parse ODW data"))
      (map second results))))
