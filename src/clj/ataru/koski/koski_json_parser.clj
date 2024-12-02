(ns ataru.koski.koski-json-parser
  (:require [schema.core :as s]
            [clj-time.format :as f]
            [ataru.schema.koski-tutkinnot-schema :as koski-schema]))


(def koski-date-format (f/formatter "yyyy-MM-dd"))
(def ataru-date-format (f/formatter "dd.MM.yyyy"))

(defn- parse-valmistumispvm [koski-suoritus]
  (when-let [pvm (get-in koski-suoritus [:vahvistus :päivä])]
    (f/unparse ataru-date-format (f/parse koski-date-format pvm))))

(defn- parse-id [koski-suoritus]
  (str (get-in koski-suoritus [:toimipiste :oid])
       "_"
       (get-in koski-suoritus [:koulutusmoduuli :tunniste :koodiarvo])
       "_"
       (get-in koski-suoritus [:vahvistus :päivä])))

(defn- is-perusopetus? [koski-opiskeluoikeus]
  (let [tutkinto-type (get-in koski-opiskeluoikeus [:tyyppi :koodiarvo] "")]
    (or (= "perusopetus" tutkinto-type) (= "aikuistenperusopetus" tutkinto-type))))

(defn- parse-common-fields [koski-suoritus]
  {:id             (parse-id koski-suoritus)
   :valmistumispvm (parse-valmistumispvm koski-suoritus)
   :toimipistenimi (get-in koski-suoritus [:toimipiste :nimi])})

(defn- parse-perusopetukset
  [koski-opiskeluoikeus]
  (map (fn [suoritus] (-> (parse-common-fields suoritus)
                          (assoc :level "perusopetus")
                          (assoc :tutkintonimi (get-in koski-opiskeluoikeus [:tyyppi :nimi]))))
       (:suoritukset koski-opiskeluoikeus)))

(defn- is-yotutkinto? [koski-opiskeluoikeus]
  (let [tutkinto-type (get-in koski-opiskeluoikeus [:tyyppi :koodiarvo] "")]
    (some? (some #(when (= tutkinto-type %) %) ["ylioppilastutkinto" "diatutkinto" "ebtutkinto"]))))

(defn- parse-yotutkinnot [koski-opiskeluoikeus]
  (let [tutkinto-type (get-in koski-opiskeluoikeus [:tyyppi :koodiarvo] "")]
    (map (fn [suoritus] (cond-> (-> (parse-common-fields suoritus)
                                    (assoc :level "yo")
                                    (assoc :tutkintonimi (get-in suoritus [:koulutusmoduuli :tunniste :nimi]))
                                    (assoc :koulutusohjelmanimi (get-in koski-opiskeluoikeus [:tyyppi :nimi])))
                                (= "ylioppilastutkinto" tutkinto-type)
                                (assoc :tutkintonimi (get-in koski-opiskeluoikeus [:tyyppi :nimi]))
                                (= "ylioppilastutkinto" tutkinto-type)
                                (dissoc :toimipistenimi     ; For ylioppilastutkinto this seems to be always "Ylioppilastutkintolautakunta"
                                        :koulutusohjelmanimi)
                                (= "ebtutkinto" tutkinto-type)
                                (dissoc :koulutusohjelmanimi)))
         (:suoritukset koski-opiskeluoikeus))))

(defn- is-ammatillinen? [koski-opiskeluoikeus]
  (= "ammatillinenkoulutus" (get-in koski-opiskeluoikeus [:tyyppi :koodiarvo] "")))

(defn- resolve-subtype-of-ammatillinen [koulutus-tyyppi-koodi]
  (cond (some #(when (= koulutus-tyyppi-koodi %) %) ["1" "26" "4" "13"]) "amm-perus"
        (some #(when (= koulutus-tyyppi-koodi %) %) ["12"]) "amm-erikois"
        (some #(when (= koulutus-tyyppi-koodi %) %) ["11"]) "amm"))

(defn- parse-ammatilliset [koski-opiskeluoikeus]
  (map (fn [suoritus] (-> (parse-common-fields suoritus)
                          (assoc :level (resolve-subtype-of-ammatillinen (get-in suoritus [:koulutusmoduuli :koulutustyyppi :koodiarvo])))
                          (assoc :tutkintonimi (get-in suoritus [:koulutusmoduuli :tunniste :nimi]))
                          (assoc :koulutusohjelmanimi
                                 (get-in suoritus [:koulutusmoduuli :koulutustyyppi :nimi]))))
       (:suoritukset koski-opiskeluoikeus)))

(defn- is-korkeakoulututkinto? [koski-opiskeluoikeus]
  (= "korkeakoulutus" (get-in koski-opiskeluoikeus [:tyyppi :koodiarvo] "")))

(defn- resolve-subtype-of-korkeakoulututkinto [virta-tyyppi-koodi]
  (cond (some #(when (= virta-tyyppi-koodi %) %) ["2"]) "kk-alemmat"
        (some #(when (= virta-tyyppi-koodi %) %) ["4"]) "kk-ylemmat"
        (some #(when (= virta-tyyppi-koodi %) %) ["7"]) "tohtori"))

(defn- parse-korkeakoulututkinnot [koski-opiskeluoikeus]
  (let [tutkinto-koodi (get-in koski-opiskeluoikeus [:lisätiedot :virtaOpiskeluoikeudenTyyppi :koodiarvo])
        tutkinto-type (resolve-subtype-of-korkeakoulututkinto tutkinto-koodi)]
    (if tutkinto-type
      (map (fn [suoritus]
             (let [virtanimi (get-in suoritus [:koulutusmoduuli :virtaNimi])]
               (cond-> (-> (parse-common-fields suoritus)
                           (assoc :level tutkinto-type)
                           (assoc :tutkintonimi virtanimi)
                           (assoc :koulutusohjelmanimi (get-in suoritus [:koulutusmoduuli :tunniste :nimi])))
                       (nil? virtanimi) (assoc :tutkintonimi (get-in suoritus [:koulutusmoduuli :tunniste :nimi])
                                               :koulutusohjelmanimi (get-in koski-opiskeluoikeus [:tyyppi :nimi])))))
           (:suoritukset koski-opiskeluoikeus))
      [])))


(defn- any-requested-levels? [requested-levels checked-levels]
  (some? (some (set checked-levels) requested-levels)))

(defn- parse-tutkinnot-by-level [koski-opiskeluoikeus requested-levels]
  (let [any-requested? (fn [& levels] (any-requested-levels? requested-levels levels))
        list-fn (cond (and (is-perusopetus? koski-opiskeluoikeus) (any-requested? "perusopetus"))
                      parse-perusopetukset
                      (and (is-yotutkinto? koski-opiskeluoikeus) (any-requested? "yo"))
                      parse-yotutkinnot
                      (and (is-ammatillinen? koski-opiskeluoikeus)
                           (any-requested? "amm" "amm-perus" "amm-erikois"))
                      parse-ammatilliset
                      (and (is-korkeakoulututkinto? koski-opiskeluoikeus)
                           (any-requested? "kk-alemmat" "kk-ylemmat" "tohtori"))
                      parse-korkeakoulututkinnot)]
    (if list-fn
      (filter #(and (:tutkintonimi %) (:valmistumispvm %)) (list-fn koski-opiskeluoikeus))
      [])))


(defn- filter-by-levels [tutkinto requested-levels]
  (some #(when (= (:level tutkinto) %) %) requested-levels))

(s/defn ^:always-validate parse-koski-tutkinnot :- [koski-schema/AtaruKoskiTutkinto]
  [koski-opiskelu-oikeudet requested-levels]
  (filterv #(filter-by-levels % requested-levels)
           (flatten (map #(parse-tutkinnot-by-level % requested-levels) koski-opiskelu-oikeudet))))