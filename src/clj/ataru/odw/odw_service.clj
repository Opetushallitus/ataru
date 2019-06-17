(ns ataru.odw.odw-service
  (:require [ataru.applications.application-store :as application-store]
            [ataru.util :as util]
            [ataru.koodisto.koodisto-codes :refer [finland-country-code]]
            [ataru.person-service.person-service :as person-service]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-protocol]))

(defn- get-hakukelpoisuus
  [application hakukohde-oid]
  (case (get-in application [:eligibilities (keyword hakukohde-oid)] "unreviewed")
    "unreviewed"             "NOT_CHECKED"
    "eligible"               "ELIGIBLE"
    "uneligible"             "INELIGIBLE"
    "conditionally-eligible" "CONDITIONALLY_ELIGIBLE"))

(defn- get-maksuvelvollisuus
  [application hakukohde-oid]
  (case (get-in application [:payment-obligations (keyword hakukohde-oid)] "unreviewed")
    "unreviewed"    "NOT_CHECKED"
    "obligated"     "REQUIRED"
    "not-obligated" "NOT_REQUIRED"))

(defn- get-kk-pohjakoulutus
  [answers]
  (map #(hash-map :pohjakoulutuskklomake %)
       (get-in answers [:higher-completed-base-education :value])))

(defn get-applications-for-odw [person-service tarjonta-service date limit offset]
  (let [applications (application-store/get-applications-newer-than date limit offset)
        persons      (person-service/get-persons person-service (distinct (keep :person_oid applications)))]
    (map (fn [application]
           (let [answers     (-> application :content :answers util/answers-by-key)
                 hakukohteet (:hakukohde application)
                 person-oid  (:person_oid application)
                 person      (get persons person-oid)
                 state       (:state application)
                 foreign?    (not= finland-country-code (-> answers :country-of-residence :value))]
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
                     :kk_pohjakoulutus                 (get-kk-pohjakoulutus answers)}
                    (if foreign?
                      {:Ulk_postiosoite (-> answers :address :value)
                       :Ulk_postinumero (-> answers :postal-code :value)
                       :Ulk_kunta       (-> answers :city :value)}
                      {:lahiosoite  (-> answers :address :value)
                       :postinumero (-> answers :postal-code :value)
                       :kotikunta   (-> answers :home-town :value)})
                    (into {}
                          (for [index (range 1 7) ; Hard-coded amount in ODW 1-6
                                :let  [hakukohde-oid (nth hakukohteet (dec index) nil)
                                       hakukohde     (when hakukohde-oid
                                                       (tarjonta-protocol/get-hakukohde tarjonta-service hakukohde-oid))
                                       tarjoaja-oid  (-> hakukohde :tarjoaja-oids first)]]
                            {(keyword (str "pref" index "_hakukohde_oid"))     hakukohde-oid
                             (keyword (str "pref" index "_opetuspiste_oid"))   tarjoaja-oid
                             (keyword (str "pref" index "_sora"))              nil
                             (keyword (str "pref" index "_harkinnanvarainen")) nil
                             (keyword (str "pref" index "_hakukelpoisuus"))    (get-hakukelpoisuus application hakukohde-oid)
                             (keyword (str "pref" index "_maksuvelvollisuus")) (get-maksuvelvollisuus application hakukohde-oid)})))))
      applications)))
