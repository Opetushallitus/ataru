(ns ataru.hakija.application
  "Pure functions handling application data"
  (:require [ataru.util :as util]))

(defn- initial-valid-status [flattened-form-fields]
  (into {}
        (map-indexed
          (fn [idx field]
            [(keyword (:id field)) {:valid (not (some #(= % "required") (:validators field)))
                                    :label (:label field)
                                    :order-idx idx}]) flattened-form-fields)))

(defn create-initial-answers
  "Create initial answer structure based on form structure. Mainly validity for now."
  [form]
  (initial-valid-status (util/flatten-form-fields (:content form))))

(defn answers->valid-status [all-answers ui]
  (let [answer-validity (for [[_ answers] all-answers] (:valid answers))
        invalid-fields (for [[key answers]
                             (sort-by (fn [[_ answers]] (:order-idx answers)) all-answers)
                             :when (and (not (:valid answers)) (get-in ui [key :visible?] true))]
                         (assoc (select-keys answers [:label]) :key key))]
    {:invalid-fields invalid-fields
     :valid          (if (empty? answer-validity)
                       false
                       (= 0 (count invalid-fields)))}))

(defn form->flat-form-map [form]
  (into {}
        (map
         (fn [field] [(:id field) field])
         (util/flatten-form-fields (:content form)))))

(defn- create-answers-to-submit [answers form]
  (for [[ans-key {:keys [value] :as answer}] answers
        :let [flat-form-map (form->flat-form-map form)
              field-map (get flat-form-map (name ans-key))
              field-type (:fieldType field-map)
              label (:label field-map)]
        :when (and (not-empty value) (not (:exclude-from-answers field-map)))]
    {:key (name ans-key) :value value :fieldType field-type :label label}))

(defn create-application-to-submit [application form lang]
  {:form (:id form)
   :lang lang
   :answers (create-answers-to-submit (:answers application) form)})

(defn extract-wrapper-sections [form]
  (map #(select-keys % [:id :label :children])
       (filter #(= (:fieldClass %) "wrapperElement") (:content form))))

(defn- bools-all-true [bools] (and (not (empty? bools)) (every? true? bools)))

(defn wrapper-section-ids-validity [wrapper-sections answers]
  (let [grouped (util/group-answers-by-wrapperelement wrapper-sections answers)]
    (into {}
      (for [[section-id answers] grouped]
        (do
          [section-id (bools-all-true
                        (eduction
                          (comp
                            (map first)
                            (map second)
                            (filter some?)
                            (map :valid))
                          answers))])))))

(defn wrapper-sections-with-validity [wrapper-sections answers]
  (let [wrapper-section-id->valid (wrapper-section-ids-validity wrapper-sections answers)]
    (map
      (fn [wrapper-section]
        (assoc wrapper-section :valid (get wrapper-section-id->valid (:id wrapper-section))))
      wrapper-sections)))
