(ns ataru.odw.odw-service
  (:require [ataru.applications.application-store :as application-store]
            [ataru.util :as util]
            [ataru.tarjonta-service.tarjonta-client :as tarjonta-client]
            [ataru.person-service.person-service :as person-service]))

(defn get-applications-for-odw [person-service date]
  (let [applications (application-store/get-applications-newer-than date)
        persons      (->> (person-service/get-persons person-service (distinct (map :person_oid applications)))
                          (reduce (fn [res person]
                                    (assoc res (:oidHenkilo person) person))
                                  {}))]
    (map (fn [application]
           (let [answers     (-> application :content :answers util/answers-by-key)
                 hakukohteet (:hakukohde application)
                 person-oid  (:person_oid application)
                 person      (get persons person-oid)]
             (merge {:person_oid             person-oid
                     :application_system_oid (:haku application)
                     :postinumero            (-> answers :postal-code :value)
                     :lahiosoite             (-> answers :address :value)
                     :puhelin                (-> answers :phone :value)
                     :sahkoposti             (-> answers :email :value)
                     :asuinmaa               (-> answers :country-of-residence :value)
                     :kotikunta              (-> answers :home-town :value)
                     :student_oid            (-> person :oppijanumero)
                     :aidinkieli             (-> person :aidinkieli :kieliKoodi)
                     :kansalaisuus           (-> person :kansalaisuus first :kansalaisuusKoodi)
                     :sukunimi               (-> person :sukunimi)
                     :etunimet               (-> person :etunimet)
                     :kutsumanimi            (-> person :kutsumanimi)
                     :syntymaaika            (-> person :syntymaaika)
                     :turvakielto            (-> person :turvakielto)
                     :hetu                   (-> person :hetu)
                     :sukupuoli              (util/gender-int-to-string (-> person :sukupuoli))
                     :Ulk_postiosoite        nil
                     :Ulk_postinumero        nil
                     :Ulk_kunta              nil
                     :SahkoinenViestintaLupa nil}
                    (into {}
                          (for [index (range 1 7) ; Hard-coded amount in ODW 1-6
                                :let [hakukohde-oid (nth hakukohteet index nil)
                                      hakukohde     (when hakukohde-oid (tarjonta-client/get-hakukohde hakukohde-oid))
                                      tarjoaja-oid  (-> hakukohde :tarjoajaOids first)]]
                            {(keyword (str "pref" index "_hakukohde_oid"))     hakukohde-oid
                             (keyword (str "pref" index "_opetuspiste_oid"))   tarjoaja-oid
                             (keyword (str "pref" index "_sora"))              nil
                             (keyword (str "pref" index "_harkinnanvarainen")) nil})))))
      applications)))