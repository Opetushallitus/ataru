(ns ataru.hakija.validator
  (:require [ataru.forms.form-store :as form-store]
            [ataru.hakija.application-validators :as validator]
            [ataru.util :as util]
            [clojure.set :refer [difference]]
            [clojure.core.match :refer [match]]
            [taoensso.timbre :refer [spy debug warn]]
            [ataru.koodisto.koodisto :as koodisto]))

(defn allowed-values [options]
  (set
    (->> (reduce
           (fn [values option]
             (concat values (vals (:label option))))
           []
           options)
         (filter not-empty))))

(defn validator-keyword->fn [validator-keyword]
  (case (keyword validator-keyword)
    :one-of ; one of the answers of a group of fields must validate to true
    (fn [answers]
      (boolean (some true? answers)))))

(defn extra-answers-not-in-original-form [form-keys answer-keys]
  (apply disj (set answer-keys) form-keys))

(defn passed? [answer validators]
  (every? (fn [validator]
            (validator/validate validator answer))
          validators))

(defn- wrap-coll [xs]
  (if (coll? xs)
    xs
    [xs]))

(defn- passes-all? [validators answers]
  (every? true? (map
                  #(passed? % validators)
                  (or
                    (when (empty? answers) [nil])
                    answers))))

(defn build-results
  [answers-by-key results [{:keys [id] :as field} & forms]]
  (let [id      (keyword id)
        answers (wrap-coll (:value (get answers-by-key id)))]
    (into {}
      (match (merge {:validators []
                     :params     []}
               field)

        {:exclude-from-answers true}
        (build-results
          answers-by-key
          results
          forms)

        {:fieldClass      "wrapperElement"
         :children        children
         :child-validator validation-keyword}
        (build-results
          answers-by-key
          (concat results
            {id {:passed?
                 ((validator-keyword->fn validation-keyword)
                  (mapv (comp :passed? second)
                    (build-results answers-by-key [] children)))}})
          forms)

        {:fieldClass "wrapperElement"
         :children   children}
        (build-results
          answers-by-key
          (concat results (build-results answers-by-key [] children))
          forms)

        {:fieldClass "formField"
         :fieldType  (:or "dropdown" "multipleChoice")
         :validators validators
         :options    options}
        (let [koodisto-source (:koodisto-source field)
              allowed-values  (if koodisto-source
                                (koodisto/all-koodisto-values (:uri koodisto-source) (:version koodisto-source))
                                (allowed-values options))
              answers         (if (= "multipleChoice" (:fieldType field))
                                (mapcat
                                  #(clojure.string/split % #", ")
                                  (filter not-empty answers))
                                answers)]
          (build-results
            answers-by-key
            (concat results
              {id {:passed? (and
                              (or
                                (nil? allowed-values)
                                (clojure.set/subset? (set answers) allowed-values))
                              (passes-all? validators answers))}})
            forms))

        {:fieldClass "formField"
         :validators validators}
        (build-results
          answers-by-key
          (concat results
            {id {:passed? (passes-all? validators answers)}})
          forms)

        :else results))))

(defn build-failed-results [answers-by-key failed-results]
  (merge-with merge
    (select-keys answers-by-key (keys failed-results))
    failed-results))

(defn valid-application?
  "Verifies that given application is valid by validating each answer
   against their associated validators."
  ([application]
   (valid-application? application (form-store/fetch-by-id (:form application))))
  ([application form]
   {:pre [(not-empty form)]}
   (let [answers-by-key (util/answers-by-key (:answers application))
         extra-answers (extra-answers-not-in-original-form
                         (map (comp keyword :id) (util/flatten-form-fields (:content form)))
                         (keys answers-by-key))
         results (build-results answers-by-key [] (:content form))
         failed-results (into {} (filter #(not (:passed? (second %))) results))]
     (when (not (empty? extra-answers))
       (warn "Extra answers in application" (apply str extra-answers)))
     (when (not (empty? failed-results))
       (warn "Validation failed in application fields" (build-failed-results answers-by-key failed-results)))
     (and
       (empty? extra-answers)
       (empty? failed-results)))))
