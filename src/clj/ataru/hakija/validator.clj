(ns ataru.hakija.validator
  (:require [ataru.forms.form-store :as form-store]
            [ataru.hakija.application-validators :as validator]
            [ataru.util :as util :refer [collect-ids]]
            [clojure.core.async :as asyncm]
            [clojure.set :refer [difference]]
            [clojure.core.match :refer [match]]
            [clojure.core.async :as async]
            [taoensso.timbre :as log]
            [ataru.koodisto.koodisto :as koodisto]))

(defn allowed-values [options]
  (set (map :value options)))

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
           (and (clojure.string/blank? (-> answers-by-key :ssn :value))
                (not (nationalities-value-contains-finland? (-> answers-by-key :nationality :value)))))))

(defn- validate-ssn-or-birthdate-component
  [answers-by-key failed _]
  (and (not (contains? failed :birth-date))
       (or (not (contains? failed :ssn))
           (and (clojure.string/blank? (-> answers-by-key :ssn :value))
                (not (nationalities-value-contains-finland? (-> answers-by-key :nationality :value)))))))

(defn validator-keyword->fn [validator-keyword]
  (case (keyword validator-keyword)
    :birthdate-and-gender-component
    validate-birthdate-and-gender-component

    :ssn-or-birthdate-component
    validate-ssn-or-birthdate-component

    :one-of ; one of the answers of a group of fields must validate to true - used in old versions of person info module
    (fn [_ failed children]
      (some #(not (contains? failed (keyword (:id %)))) children))))

(defn extra-answers-not-in-original-form [form-keys answer-keys]
  (apply disj (set answer-keys) form-keys))

(defn- passed? [has-applied form answer validators answers-by-key field-descriptor virkailija?]
  (every? (fn [validator]
              (first (async/<!! (validator/validate {:has-applied                  has-applied
                                                     :try-selection                (constantly (asyncm/go [true []]))
                                                     :validator                    validator
                                                     :value                        answer
                                                     :answers-by-key               answers-by-key
                                                     :tarjonta-hakukohteet         (get-in form [:tarjonta :hakukohteet])
                                                     :priorisoivat-hakukohderyhmat (:priorisoivat-hakukohderyhmat form)
                                                     :rajaavat-hakukohderyhmat     (:rajaavat-hakukohderyhmat form)
                                                     :virkailija?                  virkailija?
                                                     :field-descriptor             field-descriptor}))))
          validators))

(defn- wrap-coll [xs]
  (if (coll? xs)
    xs
    [xs]))

(def answers-to-validate-as-vector
  "Do not validate these answers a collection of individual items, but pass the whole list instead"
  #{:hakukohteet})

(defn- passes-all?
  [has-applied form validators answers answers-by-key field-descriptor virkailija?]
  (every? true? (map
                  #(passed? has-applied form % validators answers-by-key field-descriptor virkailija?)
                  (or
                    (when (empty? answers) [nil])
                    (when (contains? answers-to-validate-as-vector (-> field-descriptor :id (keyword))) [answers])
                    answers))))

(defn- field-belongs-to-hakukohde-or-hakukohderyhma? [field]
  (or (not-empty (:belongs-to-hakukohteet field))
      (not-empty (:belongs-to-hakukohderyhma field))))

(defn- get-followup-questions [options answers]
  (not-empty (eduction (comp
                        (filter (fn [option]
                                  (and (not-empty (:followups option))
                                       (= (seq answers) (wrap-coll (:value option))))))
                        (mapcat :followups))
                       options)))

(defn- is-question-group-answer?
  [answer]
  (letfn [(l? [answer] (or (vector? answer) (set? answer) (seq? answer)))]
    (and (l? answer)
         (not-empty answer)
         (every? l? answer))))

(defn- get-non-empty-answers [field answers]
  (set
    (if (is-question-group-answer? answers)
      (->> answers
           (map not-empty)
           (filter not-empty))
      (->> (if (= "multipleChoice" (:fieldType field))
             (filter not-empty answers)
             answers)
           (filter (comp not clojure.string/blank?))))))

(defn get-allowed-values [koodisto-cache koodisto-source options]
  (if koodisto-source
    (koodisto/all-koodisto-values koodisto-cache
                                  (:uri koodisto-source)
                                  (:version koodisto-source)
                                  (:allow-invalid? koodisto-source))
    (allowed-values options)))

(defn- all-answers-allowed? [all-answers allowed-values]
  (or (nil? allowed-values)
      (clojure.set/subset? all-answers allowed-values)))

(defn- belongs-to-correct-hakukohde? [field hakukohteet]
  (not-empty (clojure.set/intersection (-> field :belongs-to-hakukohteet set) hakukohteet)))

(defn- belongs-to-correct-hakukohderyhma? [field hakukohderyhmat]
  (not-empty (clojure.set/intersection (-> field :belongs-to-hakukohderyhma set) hakukohderyhmat)))

(defn- belongs-to-existing-hakukohde-or-hakukohderyma? [field hakukohteet hakukohderyhmat]
  (or (belongs-to-correct-hakukohde? field hakukohteet)
      (belongs-to-correct-hakukohderyhma? field hakukohderyhmat)))

(defn- every-followup-nil? [answers-by-key followups]
  (every? clojure.string/blank? (map #(-> answers-by-key (keyword (:id %)) :value) followups)))

(defn- all-answers-nil? [non-empty-answers answers-by-key followups]
  (and (empty? non-empty-answers)
       (every-followup-nil? answers-by-key followups)))

(defn- answers-nil? [answers-by-key children]
  (let [answer-keys (reduce collect-ids [] children)]
    (every? (fn [id] (nil? (get answers-by-key (keyword id)))) answer-keys)))

(defn build-results
  [koodisto-cache has-applied answers-by-key form fields hakukohderyhmat virkailija?]
  (let [hakukohteet (-> answers-by-key :hakukohteet :value set)]
    (loop [fields  fields
           results {}]
      (if-let [field (first fields)]
        (let [id      (keyword (:id field))
              answers (wrap-coll (:value (get answers-by-key id)))]
          (cond (or (get-in field [:params :hidden] false)
                    (and (field-belongs-to-hakukohde-or-hakukohderyhma? field)
                         (not (belongs-to-existing-hakukohde-or-hakukohderyma? field hakukohteet hakukohderyhmat))))
                (recur (rest fields)
                       (->> (util/flatten-form-fields [field])
                            (keep (fn [field]
                                    (let [id (keyword (:id field))]
                                      (when-let [answer (get answers-by-key id)]
                                        [id answer]))))
                            (into results)))

                (or (:exclude-from-answers field)
                    (= "infoElement" (:fieldClass field)))
                (recur (rest fields)
                       (if-let [answer (get answers-by-key id)]
                         (assoc results id answer)
                         results))

                (some? (:child-validator field))
                (recur (rest fields)
                       (if (->> (build-results koodisto-cache has-applied answers-by-key form (:children field) hakukohderyhmat virkailija?)
                                ((validator-keyword->fn (:child-validator field)) answers-by-key (:children field)))
                         results
                         (->>(:children field)
                             (map (fn [child]
                                    (let [id (keyword (:id field))]
                                      [id (get answers-by-key id)])))
                             (into results))))

                (or (= "wrapperElement" (:fieldClass field))
                    (and (= "questionGroup" (:fieldClass field))
                         (= "fieldset" (:fieldType field))))
                (recur (concat (:children field) (rest fields))
                       results)

                (and (= "dropdown" (:fieldType field))
                     (= "singleChoice" (:fieldType field))
                     (= "multipleChoice" (:fieldType field)))
                (let [options           (:options field)
                      validators        (:validators field)
                      koodisto-source   (:koodisto-source field)
                      allowed-values    (get-allowed-values koodisto-cache koodisto-source options)
                      non-empty-answers (get-non-empty-answers field answers)
                      followups         (get-followup-questions options non-empty-answers)]
                  (recur (concat followups (rest fields))
                         (if (if (is-question-group-answer? non-empty-answers)
                               (and (every? true? (map #(all-answers-allowed? (set %) allowed-values) non-empty-answers))
                                    (every? true? (map #(passes-all? has-applied form validators (set %) answers-by-key field virkailija?) non-empty-answers)))
                               (and (all-answers-allowed? non-empty-answers allowed-values)
                                    (passes-all? has-applied form validators non-empty-answers answers-by-key field virkailija?)))
                           results
                           (assoc results id (get answers-by-key id)))))

                :else
                (recur (rest fields)
                       (if (passes-all? has-applied form (:validators field) answers answers-by-key field virkailija?)
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

(defn valid-application?
  "Verifies that given application is valid by validating each answer
   against their associated validators."
  [koodisto-cache has-applied application form applied-hakukohderyhmat virkailija?]
  {:pre [(not-empty form)]}
  (let [answers-by-key     (util/answers-by-key (:answers application))
        extra-answers      (extra-answers-not-in-original-form
                             (map (comp keyword :id) (util/flatten-form-fields (:content form)))
                             (keys answers-by-key))
        failed-results     (build-results koodisto-cache has-applied answers-by-key form (:content form) applied-hakukohderyhmat virkailija?)
        failed-meta-fields (validate-meta-fields application)]
    (when (not (empty? extra-answers))
      (log/warn "Extra answers in application" (apply str extra-answers)))
    (when (not (empty? failed-results))
      (log/warn "Validation failed in application fields" failed-results))
    (when (not (empty? failed-meta-fields))
      (log/warn "Validation failed in application meta fields " (str failed-meta-fields)))
    {:passed?
     (and
       (empty? extra-answers)
       (empty? failed-results)
       (empty? failed-meta-fields))
     :failures
     (merge
       (when (not-empty extra-answers) {:extra-answers extra-answers})
       failed-results
       failed-meta-fields)
     :code :application-validation-failed-error}))
