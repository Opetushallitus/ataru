(ns ataru.hakija.application
  "Pure functions handling application data"
  (:require [ataru.util :as util]
            [medley.core :refer [remove-vals filter-vals remove-keys]]
            [taoensso.timbre :refer-macros [spy debug]]
            [ataru.application-common.application-field-common :refer [required-validators]]
            [clojure.core.match :refer [match]]
            [cljs-time.core :as time]
            [cljs-time.coerce :refer [from-long]]))

(defn- initial-valid-status [flattened-form-fields preselected-hakukohde]
  (into
    {}
    (map-indexed
      (fn [idx field]
        (match [field]
               [{:id      "hakukohteet"
                 :label   label
                 :options options}]
               (let [values (cond (= 1 (count options))
                                  [{:value (:value (first options))
                                    :valid true}]
                                  (some? preselected-hakukohde)
                                  [{:value preselected-hakukohde
                                    :valid true}]
                                  :else
                                  [])]
                 [:hakukohteet {:valid     (not (empty? values))
                                :order-idx idx
                                :label     label
                                :values    values}])
               [{:id    "pohjakoulutusristiriita"
                 :label label}]
               [:pohjakoulutusristiriita {:valid     true
                                          :order-idx idx
                                          :label     label}]
               [{:id         id
                 :fieldClass "formField"
                 :fieldType  "dropdown"
                 :label      label
                 :params     {:question-group-id _}
                 :options    options}]
               (let [value     (some #(when (:default-value %) (:value %)) options)
                     required? (some #(contains? required-validators %)
                                     (:validators field))]
                 [(keyword id) (cond-> {:valid     (or (some? value) (not required?))
                                        :order-idx idx
                                        :label     label}
                                       (some? value)
                                       (assoc :values [[{:value value :valid true}]] :value [[value]]))])
               [{:id         id
                 :fieldClass "formField"
                 :fieldType  "dropdown"
                 :label      label
                 :options    options}]
               (let [value     (some #(when (:default-value %) (:value %)) options)
                     required? (some #(contains? required-validators %)
                                     (:validators field))]
                 [(keyword id) (cond-> {:valid     (or (some? value) (not required?))
                                        :order-idx idx
                                        :label     label}
                                       (some? value)
                                       (assoc :value value))])
               [{:id    id
                 :label label}]
               [(keyword id) {:valid     (not (some #(contains? required-validators %)
                                                    (:validators field)))
                              :label     label
                              :order-idx idx}]))
      flattened-form-fields)))

(defn create-initial-answers
  "Create initial answer structure based on form structure.
  Validity, dropdown default value and default hakukohde for now."
  [form preselected-hakukohde]
  (initial-valid-status (util/flatten-form-fields (:content form)) preselected-hakukohde))

(defn not-extra-answer? [answer-key question-ids]
  "Check that the answer (key) has a corresponding quesiton in the form.
   This in necessary to allow older forms that might not have all newest questions
   to be used with latest rules. (e.g. birthplace component)"
  (or (empty? question-ids)
      (some #{answer-key} question-ids)))

(defn answers->valid-status [all-answers ui flat-form-content]
  (let [answer-validity (for [[_ answers] all-answers] (:valid answers))
        question-ids    (map #(-> % :id keyword) flat-form-content)
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

(defn- value-from-values [field-map value]
  (let [t (if (= (:fieldType field-map) "attachment")
            #(-> % :value :key)
            #(or (:value %) ""))]
    (if (vector? value)
      (map t value)
      (t value))))

(defn- create-answers-to-submit [answers form ui]
  (let [flat-form-map (form->flat-form-map form)]
    (for [[ans-key {:keys [value values]}] answers
          :let
          [field-descriptor (get flat-form-map (name ans-key))
           field-type       (:fieldType field-descriptor)]
          :when
          (and (or (= :birth-date ans-key)
                   (= :gender ans-key)
                   (get-in ui [ans-key :visible?] true))
               (not (:exclude-from-answers field-descriptor))
               (or (not-empty value)
                   values
                   (:cannot-edit field-descriptor)
                   (:cannot-view field-descriptor)
                   ;; permit empty value, because server side validation expects to match form fields to answers
                   (and (empty? value)
                        (or (= "dropdown" field-type)
                            (= "singleChoice" field-type)))))]
      {:key       (name ans-key)
       :value     (cond (or (= "dropdown" field-type)
                            (= "singleChoice" field-type))
                        value
                        (some? value)
                        value
                        :else
                        (map (partial value-from-values field-descriptor) values))
       :fieldType field-type
       :label     (:label field-descriptor)})))

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
  (->> (:content form)
       (filter #(and (= (:fieldClass %) "wrapperElement")
                     (not= (:fieldType %) "adjacentfieldset")))
       (map #(select-keys % [:id :label :children]))))

