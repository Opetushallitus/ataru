(ns ataru.odw.odw-service
  (:require [ataru.applications.application-store :as application-store]
            [ataru.applications.answer-util :as answer-util]
            [ataru.util :as util]
            [ataru.tarjonta.haku :as h]
            [ataru.koodisto.koodisto-codes :refer [finland-country-code]]
            [ataru.person-service.person-service :as person-service]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-protocol]
            [taoensso.timbre :as log]
            [ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-protocol :as valintalaskentakoostepalvelu]))

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

(defn get-applications-for-odw [person-service tarjonta-service valintalaskentakoostepalvelu-service suoritus-service date limit offset application-key]
  (let [applications (if application-key
                       [(application-store/get-latest-application-by-key application-key)]
                       (application-store/get-applications-newer-than date limit offset))
        haut (->> (keep :haku applications)
                  distinct
                  (map (fn [oid] [oid (tarjonta-protocol/get-haku tarjonta-service oid)]))
                  (into {}))
        persons (person-service/get-persons person-service (distinct (keep :person-oid applications)))
        harkinnanvaraisuus-by-hakemus (valintalaskentakoostepalvelu/hakemusten-harkinnanvaraisuus-valintalaskennasta valintalaskentakoostepalvelu-service (vec (map :key applications)))
        results (map (fn [application]
                       (try
                         [nil (let [application-key (:key application)
                                    answers (if application-key
                                              (-> application :answers util/answers-by-key)
                                              (-> application :content :answers util/answers-by-key))
                                    haku (get haut (:haku application))
                                    toinen-aste? (h/toisen-asteen-yhteishaku? (get haut (:haku application)))
                                    hakukohteet (:hakukohde application)
                                    person-oid (:person-oid application)
                                    person (get persons person-oid)
                                    state (:state application)
                                    foreign? (not= finland-country-code (-> answers :country-of-residence :value))
                                    hakutoiveiden-harkinnanvaraisuudet (get-in harkinnanvaraisuus-by-hakemus ["1.2.246.562.11.00000000000001081362" :hakutoiveet] [])]
                                (merge {:oid                              application-key
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
                                       (when toinen-aste?
                                         (let [koosteData (valintalaskentakoostepalvelu/opiskelijan-suoritukset valintalaskentakoostepalvelu-service (:oid haku) (:key application))
                                               pohjakoulutus (:POHJAKOULUTUS koosteData)
                                               opetuskieli (:perusopetuksen_kieli koosteData)
                                               suoritusvuosi (:pohjakoulutus_vuosi koosteData) ;ehkä joku fallback nykyiseen vuoteen jos ei löydy, tms.
                                               luokkatieto (ataru.suoritus.suoritus-service/opiskelija suoritus-service person-oid (vector suoritusvuosi) ["9" "10" "VALMA" "TELMA" "ML" "OPISTOVUOSI"])
                                               lahtoluokka (:luokka luokkatieto)
                                               luokkataso (:luokkataso luokkatieto)
                                               lahtokoulu-oid (:oppilaitos-oid luokkatieto)
                                               ]
                                           {:debug-luokkatieto                 luokkatieto
                                            :debug-koostedata                  koosteData
                                            :pohjakoulutus-2nd                 pohjakoulutus
                                            :pohjakoulutus-2nd-suoritusvuosi   suoritusvuosi
                                            :pohjakoulutus-2nd-suorituskieli   opetuskieli
                                            :pohjakoulutus-2nd-lahtoluokka     lahtoluokka
                                            :pohjakoulutus-2nd-luokkataso      luokkataso
                                            :pohjakoulutus-2nd-lahtokoulu-oid  lahtokoulu-oid
                                            :debug-harkinnanvaraisuudes-source harkinnanvaraisuus-by-hakemus
                                            :debug-harkinnanvaraisuudes        hakutoiveiden-harkinnanvaraisuudet
                                            }))
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
                                                (keyword (str "pref" index "_maksuvelvollisuus")) (get-maksuvelvollisuus application hakukohde-oid)}))
                                       ))]
                         (catch Exception e
                           [[e (:key application)] nil])))
                     applications)]
    (doseq [[e application-key] (keep first results)]
      (log/error e "Failed to parse ODW data of application" application-key))
    (if (some (comp some? first) results)
      (throw (new RuntimeException "Failed to parse ODW data"))
      (map second results))))
