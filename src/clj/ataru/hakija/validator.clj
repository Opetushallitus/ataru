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
  [answers-by-key child-answers]
  (boolean
    (and (-> child-answers :birth-date :passed?)
         (-> child-answers :gender :passed?)
         (or (-> child-answers :ssn :passed?)
             (and (clojure.string/blank? (-> answers-by-key :ssn :value))
                  (not (nationalities-value-contains-finland? (-> answers-by-key :nationality :value))))))))

(defn- validate-ssn-or-birthdate-component
  [answers-by-key child-answers]
  (boolean
    (and (-> child-answers :birth-date :passed?)
         (or (-> child-answers :ssn :passed?)
             (and (clojure.string/blank? (-> answers-by-key :ssn :value))
                  (not (nationalities-value-contains-finland? (-> answers-by-key :nationality :value))))))))

(defn validator-keyword->fn [validator-keyword]
  (case (keyword validator-keyword)
    :birthdate-and-gender-component
    validate-birthdate-and-gender-component

    :ssn-or-birthdate-component
    validate-ssn-or-birthdate-component

    :one-of ; one of the answers of a group of fields must validate to true - used in old versions of person info module
    (fn [_ child-answers]
      (boolean (some true? (map (comp :passed? second) child-answers))))))

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
  (not-empty (concat (:belongs-to-hakukohderyhma field) (:belongs-to-hakukohteet field))))

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
  (not-empty (clojure.set/intersection (-> field :belongs-to-hakukohderyhma set) (set hakukohderyhmat))))

(defn- belongs-to-existing-hakukohde-or-hakukohderyma? [field hakukohteet hakukohderyhmat]
  (and (or (belongs-to-correct-hakukohde? field hakukohteet)
           (belongs-to-correct-hakukohderyhma? field hakukohderyhmat))
       (not-empty hakukohteet)))

(defn- every-followup-nil? [answers-by-key followups]
  (every? clojure.string/blank? (map #(-> answers-by-key (keyword (:id %)) :value) followups)))

(defn- all-answers-nil? [non-empty-answers answers-by-key followups]
  (and (empty? non-empty-answers)
       (every-followup-nil? answers-by-key followups)))

(defn- answers-nil? [answers-by-key children]
  (let [answer-keys (reduce collect-ids [] children)]
    (every? (fn [id] (nil? (get answers-by-key (keyword id)))) answer-keys)))

(defn build-results
  [koodisto-cache has-applied answers-by-key results form [{:keys [id] :as field} & rest-form-fields] hakukohderyhmat virkailija?]
  (let [id          (keyword id)
        answers     (wrap-coll (:value (get answers-by-key id)))
        hakukohteet (-> answers-by-key :hakukohteet :value set)]
    (into {}
          (if (get-in field [:params :hidden] false)
            (->> (util/flatten-form-fields [field])
                 (map (fn [field]
                        {id {:passed? (not (contains? answers-by-key (keyword (:id field))))}}))
                 (concat results))
            (if-let [ret (match (merge {:validators []
                                        :params     []}
                                       field)
                                {:exclude-from-answers true}
                                results

                                {:fieldClass "infoElement"}
                                results

                                {:fieldClass      "wrapperElement"
                                 :children        children
                                 :child-validator validation-keyword}
                                (concat results
                                        {id {:passed?
                                             ((validator-keyword->fn validation-keyword) answers-by-key
                                              (build-results koodisto-cache has-applied answers-by-key [] form children hakukohderyhmat virkailija?))}})

                                {:fieldClass "wrapperElement"
                                 :children   children}
                                (let [belongs-to (or (not (field-belongs-to-hakukohde-or-hakukohderyhma? field))
                                                     (belongs-to-existing-hakukohde-or-hakukohderyma? field hakukohteet hakukohderyhmat))]
                                  (if belongs-to
                                    (concat results (build-results koodisto-cache has-applied answers-by-key [] form children hakukohderyhmat virkailija?))
                                    (concat results {id {:passed? (answers-nil? answers-by-key children)}})))

                                {:fieldClass "questionGroup"
                                 :fieldType  "fieldset"
                                 :children   children}
                                (concat results (build-results koodisto-cache has-applied answers-by-key [] form children hakukohderyhmat virkailija?))

                                {:fieldClass "formField"
                                 :fieldType  (:or "dropdown" "multipleChoice" "singleChoice")
                                 :validators validators
                                 :options    options}
                                (let [koodisto-source   (:koodisto-source field)
                                      allowed-values    (get-allowed-values koodisto-cache koodisto-source options)
                                      non-empty-answers (get-non-empty-answers field answers)
                                      followups         (get-followup-questions options non-empty-answers)]
                                  (concat results
                                          {id {:passed? (if (or (not (field-belongs-to-hakukohde-or-hakukohderyhma? field))
                                                                (belongs-to-existing-hakukohde-or-hakukohderyma? field hakukohteet hakukohderyhmat))
                                                          (if (is-question-group-answer? non-empty-answers)
                                                            (and (every? true? (map #(all-answers-allowed? (set %) allowed-values) non-empty-answers))
                                                                 (every? true? (map #(passes-all? has-applied form validators (set %) answers-by-key field virkailija?) non-empty-answers)))
                                                            (and (all-answers-allowed? non-empty-answers allowed-values)
                                                                 (passes-all? has-applied form validators non-empty-answers answers-by-key field virkailija?)))
                                                          (all-answers-nil? non-empty-answers answers-by-key followups))}}
                                          (when followups
                                                (build-results koodisto-cache
                                                  has-applied
                                                  answers-by-key
                                                  results
                                                  form
                                                  followups
                                                  hakukohderyhmat
                                                  virkailija?))))

                                {:fieldClass "formField"
                                 :validators validators}
                                (concat results
                                        {id {:passed? (if (or (not (field-belongs-to-hakukohde-or-hakukohderyhma? field))
                                                              (belongs-to-existing-hakukohde-or-hakukohderyma? field hakukohteet hakukohderyhmat))
                                                        (passes-all? has-applied form validators answers answers-by-key field virkailija?)
                                                        (every? nil? answers))}})
                                :else (when (some? field) (log/warn "Invalid field clause, not validated:" field)))]
              (build-results koodisto-cache has-applied answers-by-key ret form rest-form-fields hakukohderyhmat virkailija?)
              results)))))

(defn build-failed-results [answers-by-key failed-results]
  (merge-with merge
    (select-keys answers-by-key (keys failed-results))
    failed-results))

(defn- validate-meta-fields [application]
  (reduce-kv
    (fn [failed-results k v]
      (let [validator-fn (case k
                           :lang (fn [v] (contains? #{"fi" "sv" "en"} v))
                           (fn [_] true))
            valid?       (validator-fn v)]
        (cond-> failed-results
          (not valid?)
          (assoc k v))))
    {}
    (dissoc application :answers)))

(defn valid-application?
  "Verifies that given application is valid by validating each answer
   against their associated validators."
  [koodisto-cache has-applied application form applied-hakukohderyhmat virkailija?]
  {:pre [(not-empty form)]}
  (let [answers-by-key     (util/answers-by-key (:answers application))
        extra-answers      (extra-answers-not-in-original-form
                             (map (comp keyword :id) (util/flatten-form-fields (:content form)))
                             (keys answers-by-key))
        results            (build-results koodisto-cache has-applied answers-by-key [] form (:content form) applied-hakukohderyhmat virkailija?)
        failed-results     (some->>
                             (into {} (filter #(not (:passed? (second %))) results))
                             (build-failed-results answers-by-key))
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
