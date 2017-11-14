(ns ataru.odw.odw-service
  (:require [ataru.applications.application-store :as application-store]
            [ataru.util :as util]))

(defn get-applications-for-odw [date]
  (let [applications (application-store/get-applications-by-date date)]
    (map (fn [application]
           (let [answers (-> application :content :answers util/answers-by-key)
                 person-oid             (:person_oid application)]
             (merge {:person_oid             person-oid
                     :student_oid            person-oid
                     :application_system_oid (:haku application)
                     :postinumero (-> answers :postal-code :value)
                     :lahiosoite (-> answers :address :value)
                     :puhelin (-> answers :phone :value)
                     :sahkoposti (-> answers :email :value)
                     :aidinkieli nil
                     :kansalaisuus nil
                     :asuinmaa nil
                     :sukunimi nil
                     :etunimet nil
                     :kutsumanimi nil
                     :kotikunta nil
                     :syntymaaika nil
                     :sukupuoli nil
                     :hetu nil
                     :Ulk_postiosoite nil
                     :Ulk_postinumero nil
                     :Ulk_kunta nil
                     :Turvakielto nil
                     :SahkoinenViestintaLupa nil}
                    (into {}
                          (for [index (range 1 7)] ; Hard coded amount in ODW
                            {(keyword (str "pref" index "_koulutus_oid"))      (str "koulutus" index)
                             (keyword (str "pref" index "_opetuspiste_oid"))   (str "opetuspiste" index)
                             (keyword (str "pref" index "_sora"))              (str "sora" index)
                             (keyword (str "pref" index "_harkinnanvarainen")) (str "harkinnanvarainen" index)})))))
      applications)))