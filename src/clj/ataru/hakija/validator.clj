(ns ataru.hakija.validator
  (:require [ataru.forms.form-store :as form-store]
            [ataru.hakija.application-validators :as validator]
            [ataru.util :as util]
            [clojure.set :refer [difference]]
            [yesql.core :as sql]
            [clojure.core.match :refer [match]]
            [taoensso.timbre :refer [spy debug]]))

(defn allowed-values [options]
  (set
    (reduce
      (fn [values option]
        (when-not (clojure.string/blank? (:value option))
          (concat values (vals (:label option)))))
      []
      options)))

(defn form-validators [form-content]
  (reduce (fn [m field]
            (assoc m
                   (keyword (:id field))
                   (cond-> (select-keys field [:validators])
                     (= (:fieldType field) "dropdown")
                     (assoc :allowed-values (allowed-values (:options field))))))
          {}
          (util/flatten-form-fields form-content)))

(defn validator-keyword->fn [validator-keyword]
  (case validator-keyword
    :or
    (fn [& answers]
      (some true? answers))))

(defn evaluate-child-validators [form-content validation-results]
  (for [field form-content
        :let  [child-validators (:child-validators field)
               children         (:children field)]]
    (concat
      (for [[validator-keyword {:keys [fields]}] child-validators
            :let [validation-fn (validator-keyword->fn validator-keyword)]]
        (validation-fn (map (comp :answer validation-results keyword) fields)))
      (child-validators children flattened-validators))))

(defn extra-answers-not-in-original-form [form-keys answer-keys]
  (apply disj (set answer-keys) form-keys))

(defn validation-results [form-validators answers-by-key]
  (into {}
    (for [[form-key {:keys [validators allowed-values]}] form-validators
          :let  [answer           (first (get answers-by-key form-key))
                 valid-answer?    (fn [validator]
                                    (validator/validate validator
                                                        (:value answer)))]]
      {form-key
       {:answer         answer
        :allowed-values allowed-values
        :validators     validators
        :passed?        (and
                          (every? valid-answer? validators)
                          (or
                            (nil? allowed-values)
                            (some? (allowed-values (:value answer)))))}})))

(defn valid-application?
  "Verifies that given application is valid by validating each answer
   against their associated validators."
  ([application]
   (valid-application? application (form-store/fetch-form (:form application))))
  ([application form]
   {:pre [(not-empty form)]}
   (let [form-validators          (form-validators (:content form))
         answers-by-key           (group-by (comp keyword :key) (:answers application))
         validation-results       (validation-results form-validators answers-by-key)
         child-validation-results (evaluate-child-validators (:content form) validation-results)]
     (and
       (empty? (extra-answers-not-in-original-form (keys form-validators) (keys answers-by-key)))
       (empty? (validation-failures form-validators answers-by-key))))))
