(ns ataru.hakija.validator
  (:require [ataru.forms.form-store :as form-store]
            [ataru.hakija.application-validators :as validator]
            [ataru.util :as util]
            [clojure.set :refer [difference]]
            [clojure.core.match :refer [match]]
            [clojure.core.async :as async]
            [taoensso.timbre :refer [spy debug warn]]
            [ataru.koodisto.koodisto :as koodisto]
            [clojure.pprint :as pprint]))

(defn allowed-values [options]
  (set
    (reduce
      (fn [values option]
        (concat values (vals (:label option))))
      []
      options)))

(defn- validate-birthdate-and-gender-component
  [answers-by-key child-answers]
  (boolean
   (and (-> child-answers :birth-date :passed?)
        (-> child-answers :gender :passed?)
        (or (-> child-answers :ssn :passed?)
            (and (clojure.string/blank? (-> answers-by-key :ssn :value))
                 (not= (-> answers-by-key :nationality :value) "246"))))))

(defn validator-keyword->fn [validator-keyword]
  (case (keyword validator-keyword)
    :birthdate-and-gender-component
    validate-birthdate-and-gender-component

    :one-of ; one of the answers of a group of fields must validate to true - used in old versions of person info module
    (fn [_ child-answers]
      (boolean (some true? (map (comp :passed? second) child-answers))))))

(defn extra-answers-not-in-original-form [form-keys answer-keys]
  (apply disj (set answer-keys) form-keys))

(defn- passed? [has-applied answer validators answers-by-key field-descriptor]
  (every? (fn [validator]
            (first (async/<!! (validator/validate has-applied
                                                  validator
                                                  answer
                                                  answers-by-key
                                                  field-descriptor))))
          validators))

(defn- wrap-coll [xs]
  (if (coll? xs)
    xs
    [xs]))

(def answers-to-validate-as-vector
  "Do not validate these answers a collection of individual items, but pass the whole list instead"
  #{:hakukohteet})

(defn- passes-all?
  [has-applied validators answers answers-by-key field-descriptor]
  (every? true? (map
                  #(passed? has-applied % validators answers-by-key field-descriptor)
                  (or
                    (when (empty? answers) [nil])
                    (when (contains? answers-to-validate-as-vector (-> field-descriptor :id (keyword))) [answers])
                    answers))))

(defn- field-belongs-to-hakukohde? [field]
  (not-empty (:belongs-to-hakukohteet field)))

(defn- not-dropdown-or-multiple-choice [field]
  (empty? (some #{(:fieldType field)} '("dropdown" "multipleChoice"))))

(defn- get-followup-questions [options answers]
  (not-empty (eduction (comp
                        (filter (fn [option]
                                  (and (not-empty (:followups option))
                                       (= (seq answers) (wrap-coll (:value option))))))
                        (mapcat :followups))
                       options)))

(defn- get-non-empty-answers [field answers]
  (set
   (->> (if (= "multipleChoice" (:fieldType field))
          (filter not-empty answers)
          answers)
        (filter (comp not clojure.string/blank?)))))

(defn- get-allowed-values [koodisto-source options]
  (if koodisto-source
    (koodisto/all-koodisto-values (:uri koodisto-source) (:version koodisto-source))
    (allowed-values options)))

(defn- all-answers-allowed? [all-answers allowed-values]
  (or (nil? allowed-values)
      (clojure.set/subset? all-answers allowed-values)))

(defn- belongs-to-correct-hakukohde? [field hakukohteet]
  (not-empty (clojure.set/intersection (-> field :belongs-to-hakukohteet set) hakukohteet)))

(defn- belongs-to-existing-hakukohde? [field hakukohteet]
  (and (belongs-to-correct-hakukohde? field hakukohteet)
       (not-empty hakukohteet)))

(defn- every-followup-nil? [answers-by-key followups]
  (every? clojure.string/blank? (map #(-> answers-by-key (keyword (:id %)) :value) followups)))

(defn- all-answers-nil? [non-empty-answers answers-by-key followups]
  (and (empty? non-empty-answers)
       (every-followup-nil? answers-by-key followups)))

(defn build-results
  [has-applied answers-by-key results [{:keys [id] :as field} & rest-form-fields]]
  (let [id          (keyword id)
        answers     (wrap-coll (:value (get answers-by-key id)))
        ; Hakukohdes selected by user
        hakukohteet (-> answers-by-key :hakukohteet :value set)]
    (into {}
          (if-let [ret (match (merge {:validators []
                                      :params     []}
                                     field)

                              {:exclude-from-answers true}
                              results

                              {:fieldClass      "wrapperElement"
                               :children        children
                               :child-validator validation-keyword}
                              (concat results
                                      {id {:passed?
                                           ((validator-keyword->fn validation-keyword) answers-by-key
                                             (build-results has-applied answers-by-key [] children))}})

                              {:fieldClass "wrapperElement"
                               :children   children}
                              (concat results (build-results has-applied answers-by-key [] children))

                              {:fieldClass "formField"
                               :fieldType  (:or "dropdown" "multipleChoice")
                               :validators validators
                               :options    options}
                              (let [koodisto-source   (:koodisto-source field)
                                    allowed-values    (get-allowed-values koodisto-source options)
                                    non-empty-answers (get-non-empty-answers field answers)
                                    followups         (get-followup-questions options non-empty-answers)]
                                (concat results
                                        {id {:passed? (if (or (not (field-belongs-to-hakukohde? field))
                                                              (belongs-to-existing-hakukohde? field hakukohteet))
                                                        (and (all-answers-allowed? non-empty-answers allowed-values)
                                                             (passes-all? has-applied validators non-empty-answers answers-by-key field))
                                                        (all-answers-nil? non-empty-answers answers-by-key followups))}}
                                        (when followups
                                          (build-results
                                            has-applied
                                            answers-by-key
                                            results
                                            followups))))

                              {:fieldClass "formField"
                               :validators validators}
                              (concat results
                                      {id {:passed? (if (or (not (field-belongs-to-hakukohde? field))
                                                            (belongs-to-existing-hakukohde? field hakukohteet))
                                                      (passes-all? has-applied validators answers answers-by-key field)
                                                      (every? nil? answers))}})

                              :else nil)]
            (build-results has-applied answers-by-key ret rest-form-fields)
            results))))

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
  [has-applied application form]
  {:pre [(not-empty form)]}
  (let [answers-by-key     (util/answers-by-key (:answers application))
        extra-answers      (extra-answers-not-in-original-form
                             (map (comp keyword :id) (util/flatten-form-fields (:content form)))
                             (keys answers-by-key))
        results            (build-results has-applied answers-by-key [] (:content form))
        failed-results     (some->>
                             (into {} (filter #(not (:passed? (second %))) results))
                             (build-failed-results answers-by-key))
        failed-meta-fields (validate-meta-fields application)]
    (when (not (empty? extra-answers))
      (warn "Extra answers in application" (apply str extra-answers)))
    (when (not (empty? failed-results))
      (warn "Validation failed in application fields" failed-results))
    (when (not (empty? failed-meta-fields))
      (warn "Validation failed in application meta fields " (str failed-meta-fields)))
    {:passed?
     (and
       (empty? extra-answers)
       (empty? failed-results)
       (empty? failed-meta-fields))
     :failures
     (merge
       (when (not-empty extra-answers) {:extra-answers extra-answers})
       failed-results
       failed-meta-fields)}))
