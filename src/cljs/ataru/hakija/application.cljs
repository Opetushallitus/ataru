(ns ataru.hakija.application
  "Pure functions handling application data"
  (:require [ataru.util :as util]
            [ataru.cljs-util :refer [console-log]]
            [medley.core :refer [remove-vals filter-vals remove-keys]]
            [taoensso.timbre :refer-macros [spy debug]]
            [ataru.application.review-states :refer [complete-states]]
            [ataru.application-common.application-field-common :refer [required-validators]]
            [clojure.core.match :refer [match]]))

(defn- initial-valid-status [flattened-form-fields preselected-hakukohde]
  (into {}
        (map-indexed
          (fn [idx {:keys [belongs-to-hakukohteet] :as field}]
            (let [id      (keyword (:id field))
                  options (:options field)]
              (match [id (count options) preselected-hakukohde]
                     [:hakukohteet 1 _]
                     [:hakukohteet {:valid true
                                    :order-idx idx
                                    :label (:label field)
                                    :values [{:value (:value (first options))
                                              :valid true}]}]

                     [:hakukohteet _ (default-hakukohde :guard some?)]
                     [:hakukohteet {:valid true
                                    :order-idx idx
                                    :label (:label field)
                                    :values [{:value default-hakukohde
                                              :valid true}]}]

                     [_ _ _]
                     [id {:valid     (not (some #(contains? required-validators %) (:validators field)))
                          :label     (:label field)
                          :order-idx idx}])))
          flattened-form-fields)))

(defn create-initial-answers
  "Create initial answer structure based on form structure. Only validity + default hakukohde for now."
  [form preselected-hakukohde]
  (initial-valid-status (util/flatten-form-fields (:content form)) preselected-hakukohde))

(defn not-extra-answer? [answer-key question-ids]
  "Check that the answer (key) has a corresponding quesiton in the form.
   This in necessary to allow older forms that might not have all newest questions
   to be used with latest rules. (e.g. birthplace component)"
  (or (empty? question-ids)
      (some #{answer-key} question-ids)))

(defn answers->valid-status [all-answers ui form-content]
  (let [answer-validity (for [[_ answers] all-answers] (:valid answers))
        question-ids    (map #(-> % :id keyword) (util/flatten-form-fields form-content))
        invalid-fields  (for [[key answers]
                              (sort-by (fn [[_ answers]] (:order-idx answers)) all-answers)
                              :when (and key
                                         (not (:valid answers))
                                         (get-in ui [key :visible?] true)
                                         (not-extra-answer? key question-ids))]
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

(def ^:private form-fields-to-hide (comp not-empty
                                         (partial clojure.set/intersection #{:exclude-from-answers-if-hidden :belongs-to-hakukohteet})
                                         set
                                         keys))

(defn- remove-invisible-answers
  [answers flat-form ui]
  (let [fields-to-remove-if-hidden (->> flat-form
                                        (filter-vals form-fields-to-hide)
                                        (keys)
                                        (map keyword)
                                        (set))
        hidden-field-ids           (->> ui
                                        (filter-vals #(false? (:visible? %)))
                                        (keys)
                                        (set))
        fields-to-remove           (clojure.set/intersection fields-to-remove-if-hidden hidden-field-ids)]
    (remove-keys #(contains? fields-to-remove %) answers)))

(defn- value->str [field-map value]
  (cond (= (:fieldType field-map) "attachment")
        (get-in value [:value :key])

        :else (or (:value value) "")))

(defn- create-answers-to-submit [answers form ui]
  (let [flat-form-map (form->flat-form-map form)]
    (for [[ans-key {:keys [value values cannot-edit cannot-view]}] (-> answers
                                                                       (remove-invisible-followup-values flat-form-map ui)
                                                                       (remove-invisible-answers flat-form-map ui))
          :let [field-map    (get flat-form-map (name ans-key))
                field-type   (:fieldType field-map)
                label        (:label field-map)]
          :when (or
                  values
                  cannot-edit
                  cannot-view
                  ; permit empty dropdown values, because server side validation expects to match form fields to answers
                  (and (empty? value) (= "dropdown" field-type))
                  (and (not-empty value) (not (:exclude-from-answers field-map))))]
      (cond-> {:key       (name ans-key)
               :value     (or
                            value
                            (map (partial value->str field-map) values))
               :fieldType field-type
               :label     label}
              cannot-edit (assoc :cannot-edit true)
              cannot-view (assoc :cannot-view true)))))

(defn create-application-to-submit [application form lang]
  (let [{secret :secret virkailija-secret :virkailija-secret} application]
    (cond-> {:form      (:id form)
             :lang      lang
             :haku      (-> form :tarjonta :haku-oid)
             :hakukohde (map :value (get-in application [:answers :hakukohteet :values] []))
             :answers   (create-answers-to-submit (:answers application) form (:ui application))}

            (some? secret) (assoc :secret secret)

            (some? virkailija-secret) (assoc :virkailija-secret virkailija-secret))))

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

(defn application-in-complete-state? [application]
  (boolean (some #{(:state application)} complete-states)))

(defn application-processing-jatkuva-haku? [application haku]
  (and (= (:state application) "processing")
       (:is-jatkuva-haku? haku)))

(defn applying-possible? [form application]
  (cond
    (:virkailija-secret application)
    true

    (or (application-in-complete-state? application)
        (application-processing-jatkuva-haku? application (:tarjonta form)))
    false

    ;; When applying to hakukohde, hakuaika must be on
    (-> form :tarjonta)
    (-> form :tarjonta :hakuaika-dates :on)

    ;; Applying to direct form haku
    :else
    true))
