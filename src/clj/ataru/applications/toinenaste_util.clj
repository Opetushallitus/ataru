(ns ataru.applications.toinenaste-util
  (:require [ataru.application.harkinnanvaraisuus.harkinnanvaraisuus-util :refer [get-harkinnanvaraisuus-reason-for-hakukohde]]
            [ataru.component-data.base-education-module-2nd :refer [base-education-choice-key
                                                                    base-education-2nd-language-value-to-lang]]
            [ataru.koodisto.koodisto-codes :refer [finland-country-code]]
            [clojure.edn :as edn]))

(def urheilija-fields-with-single-key
  [:keskiarvo :peruskoulu :tamakausi :viimekausi :toissakausi :sivulaji
   :valmennusryhma_seurajoukkue :valmennusryhma_piirijoukkue :valmennusryhma_maajoukkue
   :valmentaja_nimi :valmentaja_email :valmentaja_puh :liitto :seura])

(def ^:private option-muu-urheilulaji "21")

(defn- to-single-value
  "Valmentajan yhteystietokentissä vastaukset ovat arrayn sisällä, mutta niitä voi nykytilanteessa olla vain yksi."
  [value]
  (if (coll? value) (first value) value))

(defn get-urheilija-laji [answers lang {:keys [laji-dropdown-key muu-laji-key value-to-label]}]
  (when laji-dropdown-key
    (let [dropdown-answer (-> answers laji-dropdown-key :value)
          option-text (if (= dropdown-answer option-muu-urheilulaji)
                        (-> answers muu-laji-key :value)
                        (get (get value-to-label dropdown-answer) (keyword lang)))]
      {:laji option-text})))

(defn get-urheilijan-lisakysymykset [answers keys]
  (when keys
    (into {} (map (fn [field]
                    {field (-> answers (get (-> keys field keyword)) :value to-single-value)})
                  urheilija-fields-with-single-key))))

(defn- form-hakukohde-key [id hakukohde-oid]
  (keyword (str id "_" hakukohde-oid)))

(defn- build-hakukohde-info
  [answers questions oid get-hakukohde-fn urheilija-amm-hakukohde? interested-in-sports-amm?]
  {:oid                                               oid
   :harkinnanvaraisuus
   (get-harkinnanvaraisuus-reason-for-hakukohde answers (get-hakukohde-fn oid))
   :terveys
   (= "1" (:value ((form-hakukohde-key (:sora-terveys-key questions) oid) answers)))
   :aiempiPeruminen
   (= "1" (:value ((form-hakukohde-key (:sora-aiempi-key questions) oid) answers)))
   :kiinnostunutKaksoistutkinnosta
   (->> (:kaksoistutkinto-keys questions)
        (map #(:value ((form-hakukohde-key % oid) answers)))
        (some #(= "0" %)))
   :kiinnostunutUrheilijanAmmatillisestaKoulutuksesta
   (when (and interested-in-sports-amm? (urheilija-amm-hakukohde? oid))
     (= "0" interested-in-sports-amm?))})

(defn- build-huoltaja [answers suffix]
  (let [k #(keyword (str % suffix))]
    (when (or (-> answers (get (k "guardian-name")) :value)
              (-> answers (get (k "guardian-firstname")) :value)
              (-> answers (get (k "guardian-lastname")) :value)
              (-> answers (get (k "guardian-email")) :value)
              (-> answers (get (k "guardian-phone")) :value))
      {:etunimi      (or (-> answers (get (k "guardian-firstname")) :value first)
                         (-> answers (get (k "guardian-name")) :value first))
       :sukunimi     (-> answers (get (k "guardian-lastname")) :value first)
       :matkapuhelin (-> answers (get (k "guardian-phone")) :value first)
       :email        (-> answers (get (k "guardian-email")) :value first)})))

(defn- build-huoltajat [answers]
  (vec (filter some? [(build-huoltaja answers "")
                      (build-huoltaja answers "-secondary")])))

(defn- find-first-answered-key [answer-keys answers]
  (->> answer-keys
       (filter #(some? (% answers)))
       first))

(defn build-toinenaste-payload
  "Build the shared toinenaste application data payload. Callers merge their own
   extras (e.g. :oid, :createdTime, :email, :attachments) on top.

   Params:
   - :answers                  - keyworded answers map from util/answers-by-key
   - :hakukohde-oids           - seq of hakukohde OIDs the applicant applied to
   - :lang                     - application language code (\"fi\"/\"sv\"/\"en\")
   - :person-oid               - person OID
   - :questions                - toinenaste questions metadata
   - :get-hakukohde-fn         - function that returns hakukohde by hakukohde oid (used by harkinnanvaraisuus)
   - :urheilija-amm-hakukohde? - predicate fn from hakukohde OID -> boolean
                                  (true if it offers urheilija-amm training)"
  [{:keys [answers hakukohde-oids lang person-oid questions
           get-hakukohde-fn urheilija-amm-hakukohde?]}]
  (let [foreign? (not= finland-country-code (-> answers :country-of-residence :value))
        sports-key (:urheilijan-amm-lisakysymys-key questions)
        interested-in-sports-amm? (when sports-key (-> answers sports-key :value))
        hakukohteet (mapv #(build-hakukohde-info answers questions % get-hakukohde-fn
                                                 urheilija-amm-hakukohde?
                                                 interested-in-sports-amm?)
                          hakukohde-oids)
        base-education-key (keyword base-education-choice-key)
        oppisopimuskoulutus-key (:oppisopimuskoulutus-key questions)
        tutkinto-vuosi-key (find-first-answered-key (:tutkintovuosi-keys questions) answers)
        tutkinto-vuosi (when tutkinto-vuosi-key (-> answers tutkinto-vuosi-key :value))
        tutkinto-kieli-key (find-first-answered-key (:tutkintokieli-keys questions) answers)
        tutkinto-kieli (when tutkinto-kieli-key
                         (-> answers tutkinto-kieli-key :value base-education-2nd-language-value-to-lang))
        urheilija-laji (get-urheilija-laji answers lang (:urheilijan-lisakysymys-laji-key-and-mapping questions))
        urheilija-laji-amm (get-urheilija-laji answers lang (:urheilijan-ammatillinen-lisakysymys-laji-key-and-mapping questions))
        urheilijan-lisakysymykset (get-urheilijan-lisakysymykset answers (:urheilijan-lisakysymys-keys questions))
        urheilijan-lisakysymykset-amm (get-urheilijan-lisakysymykset answers (:urheilijan-amm-lisakysymys-keys questions))]
    {:personOid                            person-oid
     :kieli                                lang
     :hakukohteet                          hakukohteet
     :matkapuhelin                         (-> answers :phone :value)
     :lahiosoite                           (-> answers :address :value)
     :postinumero                          (-> answers :postal-code :value)
     :postitoimipaikka                     (if foreign?
                                             (-> answers :city :value)
                                             (-> answers :postal-office :value))
     :asuinmaa                             (-> answers :country-of-residence :value)
     :kotikunta                            (-> answers :home-town :value)
     :huoltajat                            (build-huoltajat answers)
     :pohjakoulutus                        (or (-> answers base-education-key :value) "")
     :tutkintoKieli                        tutkinto-kieli
     :tutkintoVuosi                        (when (not-empty tutkinto-vuosi) (edn/read-string tutkinto-vuosi))
     :kiinnostunutOppisopimusKoulutuksesta (when oppisopimuskoulutus-key
                                             (= "0" (-> answers oppisopimuskoulutus-key :value)))
     :sahkoisenAsioinninLupa               (= "Kyllä" (-> answers :paatos-opiskelijavalinnasta-sahkopostiin :value))
     :valintatuloksenJulkaisulupa          (= "Kyllä" (-> answers :valintatuloksen-julkaisulupa :value))
     :koulutusmarkkinointilupa             (= "Kyllä" (-> answers :koulutusmarkkinointilupa :value))
     :urheilijanLisakysymykset             (merge urheilijan-lisakysymykset urheilija-laji)
     :urheilijanLisakysymyksetAmmatillinen (merge urheilijan-lisakysymykset-amm urheilija-laji-amm)}))
