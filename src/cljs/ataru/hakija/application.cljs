(ns ataru.hakija.application
  "Pure functions handling application data"
  (:require [ataru.util :as util]
            [medley.core :refer [remove-vals filter-vals remove-keys]]
            [ataru.application-common.application-field-common :refer [required-validators]]
            [clojure.core.match :refer [match]]
            [cljs-time.core :as time]
            [cljs-time.coerce :refer [from-long]]))

(defn- initial-valid-status [flattened-form-fields preselected-hakukohteet]
  (->> flattened-form-fields
       (filter util/answerable?)
       (map-indexed
        (fn [idx field]
          (match [field]
            [{:id      "hakukohteet"
              :label   label
              :options options}]
            (let [values (cond (= 1 (count options))
                               [{:value (:value (first options))
                                 :valid true}]
                               (some? preselected-hakukohteet)
                               (map (fn [oid] {:value oid :valid true}) preselected-hakukohteet)
                               :else
                               [])]
              [:hakukohteet {:valid  (not (empty? values))
                             :label  label
                             :values values}])
            [{:id    "pohjakoulutusristiriita"
              :label label}]
            [:pohjakoulutusristiriita {:valid true
                                       :label label}]
            [{:id         id
              :fieldClass "formField"
              :fieldType  "dropdown"
              :label      label
              :params     {:question-group-id _}
              :options    options}]
            (let [value     (some #(when (:default-value %) (:value %)) options)
                  required? (some #(contains? required-validators %)
                                  (:validators field))]
              [(keyword id) (cond-> {:valid (or (some? value) (not required?))
                                     :label label}
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
              [(keyword id) {:valid (or (some? value) (not required?))
                             :label label
                             :value (or value "")}])
            [{:id         id
              :fieldClass "formField"
              :fieldType  "multipleChoice"
              :label      label}]
            [(keyword id) {:valid (not (some #(contains? required-validators %)
                                             (:validators field)))
                           :value []
                           :label label}]
            [{:id    id
              :label label}]
            [(keyword id) {:valid (not (some #(contains? required-validators %)
                                             (:validators field)))
                           :label label}])))
       (into {})))

(defn create-initial-answers
  "Create initial answer structure based on form structure.
  Validity, dropdown default value and default hakukohde for now."
  [flat-form-content preselected-hakukohde-oids]
  (initial-valid-status flat-form-content preselected-hakukohde-oids))

(defn contains-id? [content id]
  (contains? (set (map :id content)) id))

(defn find-parent-field-descriptor [parent content id]
  (if (contains-id? content id)
    parent
    (let [wrappers (->> content
                        (filter :children))]
      (->> wrappers
           (map (fn [wrapper] (find-parent-field-descriptor wrapper (:children wrapper) id)))
           (filter some?)
           first))))

(defn answers->valid-status [all-answers ui flat-form-content]
  {:invalid-fields (for [field flat-form-content
                         :let  [key (keyword (:id field))
                                answer (get all-answers key)]
                         :when (and (some? answer)
                                    (not (:valid answer))
                                    (get-in ui [key :visible?] true))]
                     {:key   key
                      :label (:label answer)})})

(defn db->valid-status [db]
  (answers->valid-status (-> db :application :answers)
                         (-> db :application :ui)
                         (-> db :flat-form-content)))

(defn- value-from-values [field-map value]
  (let [t (if (= (:fieldType field-map) "attachment")
            #(:value %)
            #(or (:value %) ""))]
    (if (vector? value)
      (map t value)
      (t value))))

(defn- create-answers-to-submit [answers form ui]
  (let [flat-form-map (util/form-fields-by-id form)]
    (for [[ans-key {:keys [value values]}] answers
          :let
          [field-descriptor (get flat-form-map ans-key)
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

            (some? (get application :selection-id)) (assoc :selection-id (get application :selection-id))

            (some? secret) (assoc :secret secret)

            (some? virkailija-secret) (assoc :virkailija-secret virkailija-secret))))

(defn extract-wrapper-sections [form]
  (->> (:content form)
       (filter #(and (= (:fieldClass %) "wrapperElement")
                     (not= (:fieldType %) "adjacentfieldset")))
       (map #(select-keys % [:id :label :children]))))

