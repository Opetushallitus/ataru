(ns ataru.hakija.application
  "Pure functions handling application data"
  (:require [cljs.core.match :refer-macros [match]]))

(defn- flatten-form-fields [fields]
  (flatten
    (for [field fields]
      (match
        [field] [{:fieldClass "wrapperElement"
                  :children   children}] (map #(assoc % :wrapper-id (:id field)) children)
        :else field))))

(defn- initial-valid-status [flattened-form-fields]
  (into {}
        (map
          (fn [field]
            [(keyword (:id field)) {:valid
                                    (not (:required field))
                                    :wrapper-id (:wrapper-id field)}]) flattened-form-fields)))

(defn create-initial-answers
  "Create initial answer structure based on form structure. Mainly validity for now."
  [form]
  (initial-valid-status (flatten-form-fields (:content form))))

(defn answers->valid-status [answers]
  (let [answer-validity (for [[_ answers] answers] (:valid answers))]
    {:valid (if (empty? answer-validity) false (every? true? answer-validity))}))

(defn form->flat-form-map [form]
  (into {}
        (map
         (fn [field] [(:id field) field])
         (flatten-form-fields (:content form)))))

(defn- create-answers-to-submit [answers form]
  (for [[ans-key ans-map] answers
        :let [ans-value (:value ans-map)
              flat-form-map (form->flat-form-map form)
              field-map (get flat-form-map (name ans-key))
              field-type (:fieldType field-map)
              label (:label field-map)]
        :when (not-empty ans-value)]
     {:key (name ans-key) :value ans-value :fieldType field-type :label label}))

(defn create-application-to-submit [application form lang]
  {:form (:id form)
   :lang lang
   :answers (create-answers-to-submit (:answers application) form)})

(defn extract-wrapper-sections [form]
  (map #(select-keys % [:id :label])
       (filter #(= (:fieldClass %) "wrapperElement") (:content form))))

(defn- bools-all-true [bools] (and (not (empty? bools)) (every? true? bools)))

(defn wrapper-section-ids-validity [answers]
  (let [grouped (group-by :wrapper-id (vals answers))]
    (into {} (for [[id answers] grouped] [id (bools-all-true (map :valid answers))]))))

(defn wrapper-sections-with-validity [wrapper-sections answers]
  (let [wrapper-section-id->valid (wrapper-section-ids-validity answers)]
    (map
      (fn [wrapper-section]
        (assoc wrapper-section :valid (get wrapper-section-id->valid (:id wrapper-section))))
      wrapper-sections)))
