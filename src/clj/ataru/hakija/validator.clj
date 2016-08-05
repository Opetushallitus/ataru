(ns ataru.hakija.validator
  (:require [ataru.forms.form-store :as form-store]
            [ataru.hakija.application-validators :as validator]
            [ataru.util :as util]
            [clojure.set :refer [difference]]
            [yesql.core :as sql]
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

(defn extra-answers-not-in-original-form [form-keys answer-keys]
  (apply disj (set answer-keys) form-keys))

(defn validation-failures [form-validators answers-by-key]
  (into {}
    (for [[form-key {:keys [validators allowed-values]}] form-validators
          :let  [answer           (first (get answers-by-key form-key))
                 valid-answer?    (fn [validator]
                                    (validator/validate validator (:value answer)))]
          :when (not
                  (and
                    (every? valid-answer? validators)
                    (or
                      (nil? (:allowed-values validators))
                      (some? ((:allowed-values validators) (:value answer))))))]
      ; this form-key has failed validation against the given answer
      [form-key answer])))

(defn valid-application?
  "Verifies that given application is valid by validating each answer
   against their associated validators."
  ([application]
   (valid-application? application (form-store/fetch-form (:form application))))
  ([application form]
   {:pre [(not-empty form)]}
   (let [form-validators (form-validators (:content form))
         answers-by-key  (group-by (comp keyword :key) (:answers application))]
     (and
       (empty? (extra-answers-not-in-original-form (keys form-validators) (keys answers-by-key)))
       (empty? (validation-failures form-validators answers-by-key))))))
