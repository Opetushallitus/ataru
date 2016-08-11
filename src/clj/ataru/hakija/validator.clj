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

(defn validator-keyword->fn [validator-keyword]
  (case (keyword validator-keyword)
    :one-of ; one of the answers of a group of fields must validate to true
    (fn [answers]
      (some true? answers))))

(defn extra-answers-not-in-original-form [form-keys answer-keys]
  (apply disj (set answer-keys) form-keys))

(defn passed? [answer validators]
  (every? (fn [validator]
            (validator/validate validator answer))
          validators))

(defn build-results
  [answers-by-key results [{:keys [id] :as field} & forms]]
  (let [id     (keyword id)
        answer (:value (get answers-by-key id))]
    (into {}
      (match field
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
         :fieldType  "dropdown"
         :validators validators
         :options    options}
        (let [allowed-values (allowed-values options)]
          (build-results
            answers-by-key
            (concat results
                    {id {:passed? (and (or (nil? allowed-values)
                                           (some? (allowed-values answer)))
                                       (passed? answer validators))}})
            forms))

        {:fieldClass "formField"
         :validators validators}
        (build-results
          answers-by-key
          (concat results
                  {id {:passed? (passed? answer validators)}})
          forms)

        :else results))))

(defn valid-application?
  "Verifies that given application is valid by validating each answer
   against their associated validators."
  ([application]
   (valid-application? application (form-store/fetch-form (:form application))))
  ([application form]
   {:pre [(not-empty form)]}
   (let [answers-by-key (util/group-by-first (comp keyword :key) (:answers application))]
     (and
       (empty? (extra-answers-not-in-original-form
                 (map (comp keyword :id) (util/flatten-form-fields form))
                 (keys answers-by-key)))
       (empty? (build-results answers-by-key [] (:content form)))))))

