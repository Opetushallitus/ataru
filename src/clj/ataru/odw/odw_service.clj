(ns ataru.odw.odw-service
  (:require [ataru.applications.application-store :as application-store]
            [ataru.util :as util]
            [ataru.koodisto.koodisto-codes :refer [finland-country-code]]
            [ataru.person-service.person-service :as person-service]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-protocol]
            [taoensso.timbre :as log]))

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

(defn- parse-year
  [application-key s]
  (try
    (let [[_ _ vuosi] (re-matches #"(\d?\d\.\d?\d\.)?([12]\d{3})" s)]
      (Integer/valueOf vuosi))
    (catch Exception e
      (log/warn "Failed to parse year of completion" s "in hakemus" application-key)
      nil)))

(defn- suoritusvuosi-one-of
  ([application-key answers ids]
   (suoritusvuosi-one-of application-key answers ids false))
  ([application-key answers ids mute-logging]
    (if-let [values (some #(let [v (get-in answers [% :value])]
                            (cond (string? v)
                              [v]
                              (some? v)
                              (map first v)))
                          ids)]
      (mapv (partial parse-year application-key) values)
      (do (when-not mute-logging
            (log/warn (str "No answers to questions " (clojure.string/join ", " ids)) "in hakemus" application-key))
        [nil]))))

(defn any-answers-match?
  [answers value ids]
  (some
   #(= value (get-in answers [% :value]))
   ids))

(defn- kaksoistutkinto-suoritusvuosi
  "Double degree (secondary level) completion year resolving. General idea as follows:

   - If completion year is in future, it is unknown if it matches both matriculation and vocational completions or just
     either - it's an estimation, not a hard fact. So in case the completion is some time in future, the application
     term year is picked directly as value
   - Otherwise we can assume completion year is in past, in which case we compare the completion years of both
     matriculation and vocational parts of the degree and pick later as that matches the final completion year in
     secondary level double degree. If only one of these is present, `nil` is returned to indicate the degree is not
     fully complete yet."
  [haku answers application-key]
  (if (any-answers-match? answers "1" [:22df6790-588f-4c45-8238-3ecfccdf6d93    ; 1. yhteishaun tunniste, nyt
                                       :dfeb9d56-4d53-4087-9473-1b2d9437e47f])  ; 2. yhteishaun tunniste, nyt
    [(:hakukausiVuosi haku)]
    (let [expected-completion-years [:0a6ba6b1-616c-492b-a501-8b6656900ebd   ; 1. yhteishaun tunniste, 2017-nyt
                                     :86c7cc27-e1b3-4b3a-863c-1719b424370f]  ; 2. yhteishaun tunniste, 2017-nyt
          upcoming-completion-year (suoritusvuosi-one-of application-key answers expected-completion-years true)]
    (if-not (empty? (remove nil? upcoming-completion-year))
      upcoming-completion-year
      (mapv #(when (and (some? %1) (some? %2))
              (max %1 %2))
            (suoritusvuosi-one-of
             application-key
             answers
             [:pohjakoulutus_yo_ammatillinen--marticulation-year-of-completion
              :487bea81-a6bc-43a2-8802-d6d57bbbe8cb])
            (suoritusvuosi-one-of
             application-key
             answers
             [:pohjakoulutus_yo_ammatillinen--vocational-completion-year
              :60ce79f9-b37a-4b7e-a7e0-f25ba430f055]))))))

(defn- kk-pohjakoulutus-suoritusvuosi
  [haku answers pohjakoulutus application-key]
  (case pohjakoulutus
    "pohjakoulutus_yo"                         (suoritusvuosi-one-of
                                                application-key
                                                answers
                                                [:pohjakoulutus_yo--no-year-of-completion
                                                 :pohjakoulutus_yo--yes-year-of-completion])
    "pohjakoulutus_lk"                         (suoritusvuosi-one-of
                                                application-key
                                                answers
                                                [:pohjakoulutus_lk--year-of-completion
                                                 :c157cbde-3904-46b7-95e1-641fb8314a11])
    "pohjakoulutus_yo_kansainvalinen_suomessa" (if (any-answers-match?
                                                    answers
                                                    "0"
                                                    [:pohjakoulutus_yo_kansainvalinen_suomessa--ib--year-of-completion-this-year
                                                     :pohjakoulutus_yo_kansainvalinen_suomessa--eb--year-of-completion-this-year
                                                     :pohjakoulutus_yo_kansainvalinen_suomessa--rb--year-of-completion-this-year
                                                     :32b5f6a9-1ccb-4227-8c68-3c0a82fb0a73
                                                     :64d561e2-20f7-4143-9ad8-b6fa9a8f6fed
                                                     :6b7119c9-42ec-467d-909c-6d1cc555b823])
                                                 [(:hakukausiVuosi haku)]
                                                 (suoritusvuosi-one-of
                                                  application-key
                                                  answers
                                                  [:pohjakoulutus_yo_kansainvalinen_suomessa--year-of-completion
                                                   :pohjakoulutus_yo_kansainvalinen_suomessa--ib--year-of-completion
                                                   :pohjakoulutus_yo_kansainvalinen_suomessa--eb--year-of-completion
                                                   :pohjakoulutus_yo_kansainvalinen_suomessa--rb--year-of-completion
                                                   :a2bdac0a-e994-4fda-aa59-4ab4af2384a2
                                                   :6e2ad9bf-5f3a-41de-aada-a939aeda3e87
                                                   :c643447c-b667-42ab-9fd6-66b40a722a3c]))
    "pohjakoulutus_yo_ammatillinen"            (kaksoistutkinto-suoritusvuosi haku answers application-key)
    "pohjakoulutus_am"                         (if (any-answers-match?
                                                    answers
                                                    "1"
                                                    [:f9340e89-4a1e-4626-9246-2a77a32b22ed
                                                     :b6fa0257-c1fd-4107-b151-380e02c56fa9])
                                                 [(:hakukausiVuosi haku)]
                                                 (suoritusvuosi-one-of
                                                  application-key
                                                  answers
                                                  [:pohjakoulutus_am--year-of-completion
                                                   :f3a87aa7-b782-4947-a4a0-0f126147f7b5
                                                   :5e5a0f04-f04d-478c-b093-3f47d33ba1a4
                                                   :75d3d13c-5865-4924-8a69-d22b8a8aea65]))
    "pohjakoulutus_amt"                        (suoritusvuosi-one-of
                                                application-key
                                                answers
                                                [:pohjakoulutus_amt--year-of-completion
                                                 :c8d351ad-cd95-4f40-a128-530585fa0c0d])
    "pohjakoulutus_kk"                         (suoritusvuosi-one-of
                                                application-key
                                                answers
                                                [:pohjakoulutus_kk--completion-date
                                                 :124a0215-e358-47e1-ab02-f1cc7c831e0e])
    "pohjakoulutus_yo_ulkomainen"              (if (any-answers-match?
                                                    answers
                                                    "0"
                                                    [:pohjakoulutus_yo_ulkomainen--ib--year-of-completion-this-year
                                                     :pohjakoulutus_yo_ulkomainen--eb--year-of-completion-this-year
                                                     :pohjakoulutus_yo_ulkomainen--rb--year-of-completion-this-year
                                                     :d037fa56-6354-44fc-87d6-8b774b95dcdf
                                                     :6e980e4d-257a-49ba-a5e6-5424220e6f08
                                                     :220c3b47-1ca6-47e7-8af2-2f6ff823e07b])
                                                 [(:hakukausiVuosi haku)]
                                                 (suoritusvuosi-one-of
                                                  application-key
                                                  answers
                                                  [:pohjakoulutus_yo_ulkomainen--year-of-completion
                                                   :pohjakoulutus_yo_ulkomainen--ib--year-of-completion
                                                   :pohjakoulutus_yo_ulkomainen--eb--year-of-completion
                                                   :pohjakoulutus_yo_ulkomainen--rb--year-of-completion
                                                   :77ea3ff1-6c04-4b3f-87d2-72bbe7db12e2
                                                   :2c85ef9c-d6c2-448d-ac56-f8da4ca5c1fc
                                                   :e70041ff-e6f4-4dc5-a87f-3267543cced4]))
    "pohjakoulutus_kk_ulk"                     (suoritusvuosi-one-of
                                                application-key
                                                answers
                                                [:pohjakoulutus_kk_ulk--year-of-completion])
    "pohjakoulutus_ulk"                        (suoritusvuosi-one-of
                                                application-key
                                                answers
                                                [:pohjakoulutus_ulk--year-of-completion])
    "pohjakoulutus_muu"                        (suoritusvuosi-one-of
                                                application-key
                                                answers
                                                [:pohjakoulutus_muu--year-of-completion])
    (do (log/warn "Form for haku" (:oid haku)
                  "has the question higher-completed-base-education"
                  "but the answer" pohjakoulutus "is unknown")
        [])))

(defn- get-kk-pohjakoulutus
  [haku answers application-key]
  (vec
   (mapcat (fn [pohjakoulutus]
             (if (= "pohjakoulutus_avoin" pohjakoulutus)
               [{:pohjakoulutuskklomake pohjakoulutus}]
               (mapv (fn [suoritusvuosi]
                       (merge {:pohjakoulutuskklomake pohjakoulutus}
                              (when (some? suoritusvuosi)
                                {:suoritusvuosi suoritusvuosi})))
                     (kk-pohjakoulutus-suoritusvuosi haku answers pohjakoulutus application-key))))
           (get-in answers [:higher-completed-base-education :value]))))

(defn get-applications-for-odw [person-service tarjonta-service date limit offset]
  (let [applications (application-store/get-applications-newer-than date limit offset)
        haut         (->> (keep :haku applications)
                          distinct
                          (map (fn [oid] [oid (tarjonta-protocol/get-haku tarjonta-service oid)]))
                          (into {}))
        persons      (person-service/get-persons person-service (distinct (keep :person_oid applications)))
        results      (map (fn [application]
                            (try
                              [nil (let [answers     (-> application :content :answers util/answers-by-key)
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
                                             :kk_pohjakoulutus                 (get-kk-pohjakoulutus (get haut (:haku application)) answers (:key application))}
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
                                                     (keyword (str "pref" index "_maksuvelvollisuus")) (get-maksuvelvollisuus application hakukohde-oid)}))))]
                              (catch Exception e
                                [[e (:key application)] nil])))
                          applications)]
    (doseq [[e application-key] (keep first results)]
      (log/error e "Failed to parse ODW data of application" application-key))
    (if (some (comp some? first) results)
      (throw (new RuntimeException "Failed to parse ODW data"))
      (map second results))))
