(ns ataru.siirtotiedosto.toinenaste-enrichment
  (:require [ataru.util :refer [answers-by-key]]
            [ataru.application.harkinnanvaraisuus.harkinnanvaraisuus-util :refer [get-harkinnanvaraisuus-reason-for-hakukohde]]
            [ataru.component-data.base-education-module-2nd :refer [base-education-choice-key
                                                                    base-education-2nd-language-value-to-lang]]
            [ataru.koodisto.koodisto-codes :refer [finland-country-code]]
            [clojure.edn :as edn]))

(def ^:private urheilija-fields-with-single-key
  [:keskiarvo :peruskoulu :tamakausi :viimekausi :toissakausi :sivulaji
   :valmennusryhma_seurajoukkue :valmennusryhma_piirijoukkue :valmennusryhma_maajoukkue
   :valmentaja_nimi :valmentaja_email :valmentaja_puh :liitto :seura])

(def ^:private option-muu "21")

(defn- to-single-value [value]
  (if (coll? value) (first value) value))

(defn- get-urheilija-laji [answers lang {:keys [laji-dropdown-key muu-laji-key value-to-label]}]
  (when laji-dropdown-key
    (let [dropdown-answer (-> answers laji-dropdown-key :value)
          option-text (if (= dropdown-answer option-muu)
                        (-> answers muu-laji-key :value)
                        (get (get value-to-label dropdown-answer) (keyword lang)))]
      {:laji option-text})))

(defn- get-urheilijan-lisakysymykset [answers keys]
  (when keys
    (into {} (map (fn [field]
                    {field (-> answers (get (-> keys field keyword)) :value to-single-value)})
                  urheilija-fields-with-single-key))))

(defn enrich-with-toinenaste
  "Computes toinenaste-specific data from a siirtotiedosto application and form-derived questions.
   Returns nil if questions are nil."
  [application questions]
  (when questions
    (let [answers (answers-by-key (-> application :content :answers))
          lang (:lang application)
          hakukohde (:hakukohde application)
          person-oid (:person_oid application)
          foreign? (not= finland-country-code (-> answers :country-of-residence :value))
          form-hakukohde-key (fn [id hakukohde-oid] (keyword (str id "_" hakukohde-oid)))
          sports-key (:urheilijan-amm-lisakysymys-key questions)
          interested-in-sports-amm? (when sports-key (-> answers sports-key :value))
          hakukohteet (map (fn [oid]
                             {:oid                                               oid
                              :harkinnanvaraisuus
                              (get-harkinnanvaraisuus-reason-for-hakukohde
                                answers
                                {:oid oid :voiko-hakukohteessa-olla-harkinnanvaraisesti-hakeneita? true})
                              :terveys
                              (= "1" (:value ((form-hakukohde-key (:sora-terveys-key questions) oid) answers)))
                              :aiempiPeruminen
                              (= "1" (:value ((form-hakukohde-key (:sora-aiempi-key questions) oid) answers)))
                              :kiinnostunutKaksoistutkinnosta
                              (->> (:kaksoistutkinto-keys questions)
                                   (map #(:value ((form-hakukohde-key % oid) answers)))
                                   (some #(= "0" %)))
                              :kiinnostunutUrheilijanAmmatillisestaKoulutuksesta
                              (when interested-in-sports-amm?
                                (= "0" interested-in-sports-amm?))})
                             hakukohde)
          first-huoltaja (when (or (-> answers :guardian-name :value)
                                   (-> answers :guardian-firstname :value)
                                   (-> answers :guardian-lastname :value)
                                   (-> answers :guardian-email :value)
                                   (-> answers :guardian-phone :value))
                           {:etunimi      (or (-> answers :guardian-firstname :value first)
                                              (-> answers :guardian-name :value first))
                            :sukunimi     (-> answers :guardian-lastname :value first)
                            :matkapuhelin (-> answers :guardian-phone :value first)
                            :email        (-> answers :guardian-email :value first)})
          second-huoltaja (when (or (-> answers :guardian-name-secondary :value)
                                    (-> answers :guardian-firstname-secondary :value)
                                    (-> answers :guardian-lastname-secondary :value)
                                    (-> answers :guardian-email-secondary :value)
                                    (-> answers :guardian-phone-secondary :value))
                            {:etunimi      (or (-> answers :guardian-firstname-secondary :value first)
                                               (-> answers :guardian-name-secondary :value first))
                             :sukunimi     (-> answers :guardian-lastname-secondary :value first)
                             :matkapuhelin (-> answers :guardian-phone-secondary :value first)
                             :email        (-> answers :guardian-email-secondary :value first)})
          huoltajat (vec (filter some? [first-huoltaja second-huoltaja]))
          base-education-key (keyword base-education-choice-key)
          oppisopimuskoulutus-key (:oppisopimuskoulutus-key questions)
          tutkinto-vuosi-key (->> (:tutkintovuosi-keys questions)
                                  (filter #(some? (% answers)))
                                  first)
          tutkinto-vuosi (when tutkinto-vuosi-key (-> answers tutkinto-vuosi-key :value))
          tutkinto-kieli-key (->> (:tutkintokieli-keys questions)
                                  (filter #(some? (% answers)))
                                  first)
          tutkinto-kieli (when tutkinto-kieli-key
                           (-> answers tutkinto-kieli-key :value base-education-2nd-language-value-to-lang))
          urheilija-laji (get-urheilija-laji answers lang (:urheilijan-lisakysymys-laji-key-and-mapping questions))
          urheilija-laji-ammatillinen (get-urheilija-laji answers lang (:urheilijan-ammatillinen-lisakysymys-laji-key-and-mapping questions))
          urheilijan-lisakysymykset (get-urheilijan-lisakysymykset answers (:urheilijan-lisakysymys-keys questions))
          urheilijan-lisakysymykset-ammatillinen (get-urheilijan-lisakysymykset answers (:urheilijan-amm-lisakysymys-keys questions))]
        {:personOid                           person-oid
         :kieli                               lang
         :hakukohteet                         (vec hakukohteet)
         :email                               (-> answers :email :value)
         :matkapuhelin                        (-> answers :phone :value)
         :lahiosoite                          (-> answers :address :value)
         :postinumero                         (-> answers :postal-code :value)
         :postitoimipaikka                    (if foreign?
                                                (-> answers :city :value)
                                                (-> answers :postal-office :value))
         :asuinmaa                            (-> answers :country-of-residence :value)
         :kotikunta                           (-> answers :home-town :value)
         :huoltajat                           huoltajat
         :pohjakoulutus                       (or (-> answers base-education-key :value) "")
         :tutkintoKieli                       tutkinto-kieli
         :tutkintoVuosi                       (when (not-empty tutkinto-vuosi) (edn/read-string tutkinto-vuosi))
         :kiinnostunutOppisopimusKoulutuksesta (when oppisopimuskoulutus-key
                                                (= "0" (-> answers oppisopimuskoulutus-key :value)))
         :sahkoisenAsioinninLupa              (= "Kyllä" (-> answers :paatos-opiskelijavalinnasta-sahkopostiin :value))
         :valintatuloksenJulkaisulupa         (= "Kyllä" (-> answers :valintatuloksen-julkaisulupa :value))
         :koulutusmarkkinointilupa            (= "Kyllä" (-> answers :koulutusmarkkinointilupa :value))
         :urheilijanLisakysymykset            (merge urheilijan-lisakysymykset urheilija-laji)
         :urheilijanLisakysymyksetAmmatillinen (merge urheilijan-lisakysymykset-ammatillinen urheilija-laji-ammatillinen)})))
