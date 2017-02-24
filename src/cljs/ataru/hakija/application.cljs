(ns ataru.hakija.application
  "Pure functions handling application data"
  (:require [ataru.util :as util]
            [ataru.cljs-util :refer [console-log]]
            [medley.core :refer [remove-vals filter-vals remove-keys]]
            [taoensso.timbre :refer-macros [spy debug]]))

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
        invalid-fields  (for [[key answers]
                              (sort-by (fn [[_ answers]] (:order-idx answers)) all-answers)
                              :when (and key (not (:valid answers)) (get-in ui [key :visible?] true))]
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

(defn- remove-invisible-followup-values
  [answers flat-form ui]
  (let [followup-field-ids  (->> flat-form
                                 (filter-vals #(:followup? %))
                                 (keys)
                                 (map keyword)
                                 (set))
        hidden-field-ids    (->> ui
                                 (filter-vals #(not (:visible? %)))
                                 (keys)
                                 (set))
        hidden-followup-ids (clojure.set/intersection followup-field-ids hidden-field-ids)]
    (remove-keys #(contains? hidden-followup-ids %) answers)))

(defn- create-answers-to-submit [answers form ui]
  (let [flat-form-map (form->flat-form-map form)]
    (for [[ans-key {:keys [value values]}] (-> answers
                                               (remove-invisible-followup-values flat-form-map ui))
          :let [field-map    (get flat-form-map (name ans-key))
                field-type   (:fieldType field-map)
                cannot-edit? (boolean (:cannot-edit field-map))
                label        (:label field-map)]
          :when (or
                  values
                  cannot-edit?
                  ; permit empty dropdown values, because server side validation expects to match form fields to answers
                  (and (empty? value) (= "dropdown" field-type))
                  (and (not-empty value) (not (:exclude-from-answers field-map))))]
      {:key         (name ans-key)
       :cannot-edit cannot-edit?
                    :value (or
                             value
                             (map (fn [v] (or (:value v) "")) values))
                    :fieldType field-type
                    :label label})))

(defn create-application-to-submit [application form lang]
  (let [secret (:secret application)]
    (cond-> {:form           (:id form)
             :lang           lang
             :hakukohde      (-> form :tarjonta :hakukohde-oid)
             :haku           (-> form :tarjonta :haku-oid)
             :answers        (create-answers-to-submit (:answers application) form (:ui application))}
      (some? secret)
      (assoc :secret secret))))

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

(defn applying-possible? [form]
  (if (-> form :tarjonta)
   (-> form :tarjonta :hakuaika-dates :on)
   true))
