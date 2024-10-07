(ns ataru.hakija.validator
  (:require [ataru.hakija.application-validators :as validator]
            [ataru.util :as util]
            [clojure.core.async :as async]
            [taoensso.timbre :as log]
            [ataru.koodisto.koodisto :as koodisto]
            [clojure.string :as str]
            [clojure.set :as set]
            [ataru.application.application-answer-search-tools :as answer-tools]))

(defn- nationalities-value-contains-finland?
  [value]
  (some true? (map
                (fn [[country-code]]
                  (= country-code "246"))
                value)))

(defn- validate-birthdate-and-gender-component
  [answers-by-key failed _]
  (and (not (contains? failed :birth-date))
       (not (contains? failed :gender))
       (or (not (contains? failed :ssn))
           (and (str/blank? (-> answers-by-key :ssn :value))
                (not (nationalities-value-contains-finland? (-> answers-by-key :nationality :value)))))))

(defn- validate-ssn-or-birthdate-component
  [answers-by-key failed _]
  (and (not (contains? failed :birth-date))
       (or (not (contains? failed :ssn))
           (and (str/blank? (-> answers-by-key :ssn :value))
                (not (nationalities-value-contains-finland? (-> answers-by-key :nationality :value)))))))

(defn- validate-oppiaine-a1-or-a2-component
  "Validoi pakollinen A1- tai A2 riippuen valitusta äidinkielestä. Kun äidinkielenä ruotsi, validoitava
   pakollisena kielenä A2, muuten pakollisena kielenä validoitava B1.

   Mikäli ei-pakollisella kielellä ei arvosanaa, ignoorataan se, eli ei vaadita vastausta. Mikäli vastaus
   on annettu ja validaatiovirhe löytyy, säilytetään validaatiovirhe."
  [answers-by-key failed _]
  (let [language                                 (-> answers-by-key :language :value)
        ignore-answer-key                        (if (= language "SV")
                                                   :arvosana-B1
                                                   :arvosana-A2)
        pakollinen-kielioppiaine?                (fn keep-mandatory-language-validation-errors? [answer-key]
                                                   (not= answer-key ignore-answer-key))
        valinnaisella-kielioppiaineella-vastaus? (fn valinnaisella-kielioppiaineella-vastaus? [answer-key]
                                                   (and (= answer-key ignore-answer-key)
                                                        (-> answers-by-key ignore-answer-key :value seq some?)))]
    (reduce-kv (fn [kaikki-validaatiovirheet answer-key v]
                 (cond-> kaikki-validaatiovirheet
                         (or (pakollinen-kielioppiaine? answer-key)
                             (valinnaisella-kielioppiaineella-vastaus? answer-key))
                         (assoc answer-key v)))
               {}
               failed)))

(defn validator-keyword->fn [validator-keyword]
  (case (keyword validator-keyword)
    :birthdate-and-gender-component
    validate-birthdate-and-gender-component

    :ssn-or-birthdate-component
    validate-ssn-or-birthdate-component

    :one-of ; one of the answers of a group of fields must validate to true - used in old versions of person info module
    (fn [_ failed children]
      (some #(not (contains? failed (keyword (:id %)))) children))

    :oppiaine-a1-or-a2-component
    validate-oppiaine-a1-or-a2-component))

(defn extra-answers-not-in-original-form [form-keys answer-keys]
  (apply disj (set answer-keys) form-keys))

(defn- passed? [has-applied form answer validators answers-by-key field-descriptor virkailija?]
  (every? (fn [validator]
              (first (async/<!! (validator/validate {:has-applied                  has-applied
                                                     :try-selection                (constantly (async/go [true []]))
                                                     :validator                    validator
                                                     :value                        answer
                                                     :answers-by-key               answers-by-key
                                                     :tarjonta-hakukohteet         (get-in form [:tarjonta :hakukohteet])
                                                     :priorisoivat-hakukohderyhmat (:priorisoivat-hakukohderyhmat form)
                                                     :rajaavat-hakukohderyhmat     (:rajaavat-hakukohderyhmat form)
                                                     :virkailija?                  virkailija?
                                                     :field-descriptor             field-descriptor}))))
          validators))

; Use the code set version embedded in the form by default, try looking up from koodisto if not found.
(defn allowed-values [koodisto-cache koodisto-source options]
  ; If "" or nil is allowed for the field type, it will be added by the caller.
  (let [koodi-values (set (remove empty? (map :value options)))]
    (cond
      (not-empty koodi-values)    koodi-values
      (not-empty koodisto-source) (koodisto/all-koodisto-values koodisto-cache
                                                                (:uri koodisto-source)
                                                                (:version koodisto-source)
                                                                (:allow-invalid? koodisto-source))
      :else #{})))

(defn- field-belongs-to-hakukohde-or-hakukohderyhma? [field]
  (or (not-empty (:belongs-to-hakukohteet field))
      (not-empty (:belongs-to-hakukohderyhma field))))

(defn- belongs-to-correct-hakukohde? [field hakukohteet]
  (not-empty (set/intersection (-> field :belongs-to-hakukohteet set) hakukohteet)))

(defn- belongs-to-correct-hakukohderyhma? [field hakukohderyhmat]
  (not-empty (set/intersection (-> field :belongs-to-hakukohderyhma set) hakukohderyhmat)))

(defn- belongs-to-existing-hakukohde-or-hakukohderyma? [field hakukohteet hakukohderyhmat]
  (or (belongs-to-correct-hakukohde? field hakukohteet)
      (belongs-to-correct-hakukohderyhma? field hakukohderyhmat)))

(defn build-results
  [koodisto-cache has-applied answers-by-key form fields hakukohderyhmat virkailija?]
  (let [hakukohteet (-> answers-by-key :hakukohteet :value set)]
    (loop [fields  (map (fn [f] [nil false f]) fields)
           results {}]
      (if-let [[idx hidden? field] (first fields)]
        (let [id    (keyword (:id field))
              value (if (some? idx)
                      (get-in answers-by-key [id :value idx])
                      (get-in answers-by-key [id :value]))]
          (cond (or hidden?
                    (get-in field [:params :hidden] false)
                    (get-in field [:hidden] false)
                    (and (field-belongs-to-hakukohde-or-hakukohderyhma? field)
                         (not (belongs-to-existing-hakukohde-or-hakukohderyma? field hakukohteet hakukohderyhmat))))
                (recur (rest fields)
                       (->> (util/flatten-form-fields [field])
                            (keep (fn [field]
                                    (let [id (keyword (:id field))
                                          answer (get answers-by-key id)]
                                      (when (if (some? idx)
                                              (get-in answer [:value idx])
                                              answer)
                                        [id answer]))))
                            (into results)))

                (or (:exclude-from-answers field)
                    (= "infoElement" (:fieldClass field))
                    (= "modalInfoElement" (:fieldClass field)))
                (recur (rest fields)
                       (if-let [answer (get answers-by-key id)]
                         (assoc results id answer)
                         results))

                (some? (:child-validator field))
                (recur (rest fields)
                       (if ((validator-keyword->fn (:child-validator field))
                            answers-by-key
                            (build-results koodisto-cache has-applied answers-by-key form (:children field) hakukohderyhmat virkailija?)
                            (:children field))
                         results
                         (->> (:children field)
                              (map (fn [child]
                                     (let [id (keyword (:id child))]
                                       [id (get answers-by-key id)])))
                              (into results))))

                (= "questionGroup" (:fieldClass field))
                (let [descendants  (util/flatten-form-fields (:children field))
                      child-counts (->> descendants
                                        (keep #(get answers-by-key (keyword (:id %))))
                                        (map #(count (:value %)))
                                        distinct)]
                  (cond (empty? descendants)
                        (recur (rest fields) results)

                        (empty? (rest child-counts))
                        (recur (concat (for [idx   (range (or (first child-counts) 1))
                                             field (:children field)]
                                         [idx false field])
                                       (rest fields))
                               results)

                        :else
                        (recur (rest fields)
                               (->> descendants
                                    (map (fn [field]
                                           (let [id (keyword (:id field))]
                                             [id (get answers-by-key id)])))
                                    (into results)))))

                (= "wrapperElement" (:fieldClass field))
                (recur (concat (map (fn [field] [idx false field])
                                    (:children field))
                               (rest fields))
                       results)

                (or (= "dropdown" (:fieldType field))
                    (= "singleChoice" (:fieldType field)))
                (let [value           (if (vector? value) (first value) value)
                      options         (:options field)
                      koodisto-source (:koodisto-source field)
                      allowed-values  (cond-> (allowed-values koodisto-cache koodisto-source options)
                                              (= "dropdown" (:fieldType field))
                                              (conj "")
                                              true
                                              (conj nil))]
                  (recur (concat (for [option   options
                                       followup (:followups option)]
                                   [idx (not (= value (:value option))) followup])
                                 (rest fields))
                         (if (or (and (contains? allowed-values value)
                                  (passed? has-applied form value (:validators field) answers-by-key field virkailija?))
                                 (boolean (:per-hakukohde field)))
                           results
                           (assoc results id (get answers-by-key id)))))

                (= "multipleChoice" (:fieldType field))
                (let [options         (:options field)
                      koodisto-source (:koodisto-source field)
                      allowed-values  (allowed-values koodisto-cache koodisto-source options)]
                  (recur (concat (for [option   options
                                       followup (:followups option)]
                                   [idx (not (contains? (set value) (:value option))) followup])
                                 (rest fields))
                         (if (or (and (every? #(contains? allowed-values %) value)
                                  (passed? has-applied form value (:validators field) answers-by-key field virkailija?))
                                 (boolean (:per-hakukohde field)))
                           results
                           (assoc results id (get answers-by-key id)))))

                (or (= :hakukohteet id)
                    (= "attachment" (:fieldType field)))
                (recur (rest fields)
                       (if (passed? has-applied form value (:validators field) answers-by-key field virkailija?)
                         results
                         (assoc results id (get answers-by-key id))))

                :else
                (recur (rest fields)
                       (if (if (vector? value)
                             (and (not-empty value)
                                  (every? #(passed? has-applied form % (:validators field) answers-by-key field virkailija?)
                                          value))
                             (passed? has-applied form value (:validators field) answers-by-key field virkailija?))
                         results
                         (assoc results id (get answers-by-key id))))))
        results))))

(defn build-failed-results [answers-by-key failed-results]
  (merge-with merge
    (select-keys answers-by-key (keys failed-results))
    failed-results))

(defn- validate-meta-fields [application]
  (when (not (contains? #{"fi" "sv" "en"} (:lang application)))
    {:lang (:lang application)}))

(defn- validate-per-hakukohde-fields [answers-by-key application flattened-form-fields]
  (let [hakukohteet (->> (:hakukohteet answers-by-key)
                         :value
                         set)
        get-matching-parent (fn [followup]
                              (let [followup-field (answer-tools/get-matching-per-hakukohde-question flattened-form-fields followup)
                                    parent-field (answer-tools/get-matching-parent-field flattened-form-fields followup-field)]
                                (answer-tools/get-matching-per-hakukohde-parent-answer application parent-field followup followup-field)))
        per-hakukohde-answers (filter #(or (:duplikoitu-kysymys-hakukohde-oid %)
                                           (:duplikoitu-followup-hakukohde-oid %)) (:answers application))
        missing-hakukohteet (filter #(not (or (contains? hakukohteet (:duplikoitu-kysymys-hakukohde-oid %))
                                              (contains? hakukohteet (:duplikoitu-followup-hakukohde-oid %)))) per-hakukohde-answers)
        required-answers (answer-tools/filter-required-per-hakukohde-answers flattened-form-fields per-hakukohde-answers)
        missing-parents (->> per-hakukohde-answers
                            (filter #(:duplikoitu-followup-hakukohde-oid %))
                            (filter #(not (get-matching-parent %))))
        invalid-answers (->> required-answers
                             (filter #(or (nil? (:value %)) (empty? (:value %)))))]
    (when (or (seq missing-hakukohteet) (seq missing-parents) (seq invalid-answers))
      {:missing-hakukohteet missing-hakukohteet
       :missing-parents missing-parents
       :invalid-answers invalid-answers})))

(defn- non-duplicated-answer?
  [answer]
  (and
    (nil? (:duplikoitu-kysymys-hakukohde-oid answer))
    (nil? (:duplikoitu-followup-hakukohde-oid answer))))

(defn validate-tunnistautunut-oppija-fields [answers-by-key oppija-session]
  (when oppija-session
    (let [validation-result (for [[key answer] answers-by-key]
                              (let [session-data (get-in oppija-session [:data :fields key])
                                    oppija-session-value (:value session-data)
                                    hakemus-value (:value answer)
                                    locked? (:locked session-data)]
                                (when (and locked?
                                           (not= oppija-session-value hakemus-value))
                                  (format "Hakemus-answer '%s' does not equal session-answer '%s' for key %s"
                                          hakemus-value oppija-session-value key))))]
      (filter #(some? %) validation-result))))

; Hakukohteet (ja niiden järjestys) on hakemuksessa tallennettu kahteen paikkaan (miksi oi miksi?).
; Näiden pitää aina olla synkassa.
(defn validate-hakukohteet-synced [application]
  (let [
        answers-hakukohteet (:value (first (filter #(= (:key %) "hakukohteet") (:answers application))))
        hakukohteet (:hakukohde application)]
    (or (empty? hakukohteet)                                ; tämä tekee yksikkötesteistä yksinkertaisempia kun
                                                            ; hakukohteita ei tarvitse määritellä kahteen paikkaan
        (= answers-hakukohteet hakukohteet))))

(defn valid-application?
  "Verifies that given application is valid by validating each answer
   against their associated validators."
  [koodisto-cache has-applied application form applied-hakukohderyhmat virkailija? application-id application-key]
  {:pre [(not-empty form)]}
  (let [flattened-form-fields     (util/flatten-form-fields (:content form))
        answers-by-key            (util/answers-by-key (:answers application))
        answers-no-duplicates     (util/answers-by-key (filter non-duplicated-answer? (:answers application)))
        extra-answers             (extra-answers-not-in-original-form
                                    (map (comp keyword :id) flattened-form-fields)
                                    (keys answers-no-duplicates))
        hakukohteet-in-sync       (validate-hakukohteet-synced application)
        failed-results            (build-results koodisto-cache has-applied answers-by-key form (:content form) applied-hakukohderyhmat virkailija?)
        failed-meta-fields        (validate-meta-fields application)
        failed-per-hakukohde-fields (validate-per-hakukohde-fields answers-by-key application flattened-form-fields)
        failed-haku-oid           (:haku application)
        failed-hakukohteet        (:hakukohde application)
        failed-person-oid         (:person-oid application)
        failed-person-first-name  (get-in answers-by-key [:first-name :value])
        failed-person-last-name   (get-in answers-by-key [:last-name :value])
        failed-person-email       (get-in answers-by-key [:email :value])
        failed-form-id            (:id form)
        failed-form-key           (:key form)]
    (when (not (empty? extra-answers))
      (log/warnf "Extra answers in application (id: %s, key: %s, haku: %s, hakukohde: %s)
      fields for person (oid: %s, name: %s %s, email: %s). Form id: %s, key: %s. Answers: %s"
                 application-id
                 application-key
                 failed-haku-oid
                 failed-hakukohteet
                 failed-person-oid
                 failed-person-first-name
                 failed-person-last-name
                 failed-person-email
                 failed-form-id
                 failed-form-key
                 (apply str extra-answers)))
    (when (not (empty? failed-results))
      (log/warnf "Validation failed in application (id: %s, key: %s, haku: %s, hakukohde: %s)
      fields for person (oid: %s, name: %s %s, email: %s). Form id: %s, key: %s. Failed results: %s"
                 application-id
                 application-key
                 failed-haku-oid
                 failed-hakukohteet
                 failed-person-oid
                 failed-person-first-name
                 failed-person-last-name
                 failed-person-email
                 failed-form-id
                 failed-form-key
                 failed-results))
    (when (not (empty? failed-meta-fields))
      (log/warnf "Validation failed in application (id: %s, key: %s, haku: %s, hakukohde: %s)
      fields for person (oid: %s, name: %s %s, email: %s). Form id: %s, key: %s. Failed meta fields: %s"
                 application-id
                 application-key
                 failed-haku-oid
                 failed-hakukohteet
                 failed-person-oid
                 failed-person-first-name
                 failed-person-last-name
                 failed-person-email
                 failed-form-id
                 failed-form-key
                 (str failed-meta-fields)))
    (when (not (empty? failed-per-hakukohde-fields))
      (log/warnf "Validation failed in application (id: %s, key: %s, haku: %s, hakukohde: %s)
      fields for person (oid: %s, name: %s %s, email: %s). Form id: %s, key: %s. Failed per-hakukohde fields: %s"
                 application-id
                 application-key
                 failed-haku-oid
                 failed-hakukohteet
                 failed-person-oid
                 failed-person-first-name
                 failed-person-last-name
                 failed-person-email
                 failed-form-id
                 failed-form-key
                 (str failed-per-hakukohde-fields)))
    (when (not hakukohteet-in-sync)
      (log/warnf "Validation failed in application (id: %s, key: %s, haku: %s, hakukohde: %s)
      fields for person (oid: %s, name: %s %s, email: %s). Form id: %s, key: %s. Hakukohteet not in sync!"
                 application-id
                 application-key
                 failed-haku-oid
                 failed-hakukohteet
                 failed-person-oid
                 failed-person-first-name
                 failed-person-last-name
                 failed-person-email
                 failed-form-id
                 failed-form-key))
    {:passed?
     (and
       (empty? extra-answers)
       (empty? failed-results)
       (empty? failed-per-hakukohde-fields)
       (empty? failed-meta-fields)
       hakukohteet-in-sync)
     :failures
     (merge
       (when (not-empty extra-answers) {:extra-answers extra-answers})
       failed-results
       failed-meta-fields
       failed-per-hakukohde-fields)
     :code :application-validation-failed-error}))
