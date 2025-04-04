(ns ataru.hakija.application
  "Pure functions handling application data"
  (:require [ataru.constants :as constants]
            [ataru.util :as util]
            [ataru.component-data.koski-tutkinnot-module :as ktm]
            [ataru.tutkinto.tutkinto-util :as tutkinto-util]
            [ataru.translations.texts :refer [koski-tutkinnot-texts]]
            [ataru.application-common.application-field-common :refer [required-validators pad sanitize-value]]
            [clojure.core.match :refer [match]]))

(def selected-language-map
  { :fi "FI" :sv "SV" })

(defn- initial-valid-status [flattened-form-fields preselected-hakukohteet selected-language]
  (->> flattened-form-fields
       (filter util/answerable?)
       (map-indexed
        (fn [_ field]
          (match [field]
            [{:id      "hakukohteet"
              :label   label}]
            (let [values (cond (some? preselected-hakukohteet)
                               (mapv (fn [oid] {:value oid :valid true}) preselected-hakukohteet)
                               :else
                               [])]
              [:hakukohteet {:valid  (not (empty? values))
                             :label  label
                             :value  (mapv :value values)
                             :values values}])

            [{:id    "pohjakoulutusristiriita"
              :label label}]
            [:pohjakoulutusristiriita {:valid  true
                                       :label  label
                                       :value  nil
                                       :values {:value nil
                                                :valid true}}]

            ; Override default language based on selected form language
            [{:id    "language"
              :label label}]
            (let [value (or (get selected-language-map selected-language) "")]
              [:language {:valid  (not (empty? value))
                          :label  label
                          :value  value
                          :values {:value value
                                   :valid (not (empty? value))}}])

            [{:id         id
              :fieldClass "formField"
              :fieldType  (:or "textField" "textArea")
              :label      label
              :params     {:question-group-id _}}]
                 (let [required? (some #(contains? required-validators %)
                                       (:validators field))]
                   [(keyword id) {:valid  (not required?)
                                           :label  label
                                           :value  [[""]]
                                           :values [[{:value ""
                                                      :valid (not required?)}]]}])

            [{:id         id
              :fieldClass "formField"
              :fieldType  "textField"
              :label      label
              :params     {:adjacent-field-id _}}]
            (let [required? (some #(contains? required-validators %)
                                  (:validators field))]
              [(keyword id) {:valid  (not required?)
                             :label  label
                             :value  [""]
                             :values [{:value ""
                                       :valid (not required?)}]}])

            [{:id         id
              :fieldClass "formField"
              :fieldType  "textField"
              :label      label
              :params     {:repeatable true}}]
            (let [required? (some #(contains? required-validators %)
                                  (:validators field))]
              [(keyword id) {:valid  (not required?)
                             :label  label
                             :value  [""]
                             :values [{:value ""
                                       :valid (not required?)}]}])

            [{:id         id
              :fieldClass "formField"
              :fieldType  (:or "textField" "textArea")
              :label      label}]
            (let [required? (some #(contains? required-validators %)
                                  (:validators field))]
              [(keyword id) {:valid  (not required?)
                             :label  label
                             :value  ""
                             :values {:value ""
                                      :valid (not required?)}}])

            [{:id         id
              :fieldClass "formField"
              :fieldType  "dropdown"
              :label      label
              :params     {:question-group-id _}}]
            (let [value     (some #(when (:default-value %) (:value %)) (:options field))
                 required?  (some #(contains? required-validators %)
                                 (:validators field))]
             [(keyword id) {:valid  (or (some? value) (not required?))
                                     :label  label
                                     :value  [[(or value "")]]
                                     :values [[{:value (or value "")
                                                :valid (or (some? value) (not required?))}]]}])

            [{:id         id
              :fieldClass "formField"
              :fieldType  "dropdown"
              :label      label
              :options    options}]
            (let [value     (some #(when (:default-value %) (:value %)) options)
                  required? (some #(contains? required-validators %)
                                  (:validators field))]
              [(keyword id) {:valid  (or
                                       (some? value)
                                       (boolean (:per-hakukohde field))
                                       (not required?))
                             :label  label
                             :value  (or value "")
                             :values {:value (or value "")
                                      :valid (or (some? value)
                                                    (or (not required?)
                                                    (boolean (:per-hakukohde field))))}}])

            [{:id         id
              :fieldClass "formField"
              :fieldType  "singleChoice"
              :label      label
              :params     {:question-group-id _}}]
            (let [required? (some #(contains? required-validators %)
                                 (:validators field))]
              [(keyword id) {:valid  (not required?)
                                     :value  [nil]
                                     :values [nil]
                                     :label  label}])

            [{:id         id
              :fieldClass "formField"
              :fieldType  "singleChoice"
              :label      label}]
            (let [required? (some #(contains? required-validators %)
                                  (:validators field))]
              [(keyword id) {:valid  (or (not required?)
                                         (boolean (:per-hakukohde field)))
                             :value  nil
                             :values {:value nil
                                      :valid (or (not required?)
                                                 (boolean (:per-hakukohde field)))}
                             :label  label}])

            [{:id         id
              :fieldClass "formField"
              :fieldType  "multipleChoice"
              :label      label
              :params     {:question-group-id _}}]
            [(keyword id) {:valid  (not (some #(contains? required-validators %)
                                                      (:validators field)))
                                   :value  [[]]
                                   :values [[]]
                                   :label  label}]

            [{:id         id
              :fieldClass "formField"
              :fieldType  "multipleChoice"
              :label      label}]
            [(keyword id) {:valid  (or (not (some #(contains? required-validators %)
                                             (:validators field)))
                                  (boolean (:per-hakukohde field)))
                      :value  []
                      :values []
                      :label  label}]

            [{:id         id
              :fieldClass "formField"
              :fieldType  "attachment"
              :label      label
              :params     {:question-group-id _}}]
            [(keyword id) {:valid  (not (some #(contains? required-validators %)
                                                      (:validators field)))
                                   :value  [[]]
                                   :values [[]]
                                   :label  label}]

            [{:id         id
              :fieldClass "formField"
              :fieldType  "attachment"
              :label      label}]
            [(keyword id) {:valid  (not (some #(contains? required-validators %)
                                              (:validators field)))
                           :value  []
                           :values []
                           :label  label}])))
       (into {})))

(defn create-initial-answers
  "Create initial answer structure based on form structure.
  Validity, dropdown default value and default hakukohde + language for now."
  [flat-form-content preselected-hakukohde-oids selected-language]
  (initial-valid-status flat-form-content preselected-hakukohde-oids selected-language))

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

(defn- field-value-invalid? [key answer ui tutkinnot-required-and-missing?]
  (or (and (some? answer)
           (not (:valid answer))
           (get-in ui [key :visible?] true))
      (and tutkinnot-required-and-missing? (= ktm/koski-module-id (name key)))))

(defn- get-label-for-invalid-field [key answer tutkinnot-required-and-missing?]
  (if (and tutkinnot-required-and-missing? (= ktm/koski-module-id (name key)))
    (:tutkinto-validation-error-msg koski-tutkinnot-texts)
    (:label answer)))

(defn answers->valid-status [all-answers ui flat-form-content]
  (let [tutkinnot-required-and-missing? (tutkinto-util/tutkinnot-required-and-missing flat-form-content ui all-answers)]
    {:invalid-fields (cond-> (for [field flat-form-content
                              :let  [key (keyword (:id field))
                                     answer (get all-answers key)]
                              :when (field-value-invalid? key answer ui tutkinnot-required-and-missing?)]
                               {:key   key
                                :label (get-label-for-invalid-field key answer tutkinnot-required-and-missing?)}))}))

(defn- sanitize-attachment-value-by-state [value values]
  (when (not= :deleting (:status values))
    value))

(defn- sanitize-attachment-values-by-states [value values]
       (if (and (nil? values)
                (nil? value))
           nil
           (filterv identity (map-indexed (fn [idx value]
                                              (sanitize-attachment-value-by-state value (get values idx))) value))))

(defn- sanitize-attachment-value [value values question-group-highest-dimension]
  (if (vector? value)
    (if (or (vector? (first value)) (nil? (first value)))
      (pad (or question-group-highest-dimension 0)
           (map-indexed (fn [idx value]
                          (sanitize-attachment-values-by-states value (get values idx))) value) nil)
      (sanitize-attachment-values-by-states value values))
    (sanitize-attachment-value-by-state value values)))

(defn- question-group-shared-answers [ans-key answers flat-form-map]
  (when-let [question-group-id (-> (get flat-form-map ans-key) :params :question-group-id)]
    (->> flat-form-map
         (filter (fn [[_ v]] (= (-> v :params :question-group-id) question-group-id)))
         (keep (fn [[k _]] (get answers k))))))

(defn- create-answers-to-submit [answers form ui]
  (let [flat-form-map (util/form-fields-by-id form)]
    (for [[ans-key {:keys [value values]}] answers
          :let
          [field-descriptor (get flat-form-map ans-key)
           question-group-highest-dimension (->> (question-group-shared-answers ans-key answers flat-form-map)
                                                 (map #(count (:value %)))
                                                 (distinct)
                                                 (sort (comp - compare))
                                                 (first))
           original-question (:original-question field-descriptor)
           original-followup (:original-followup field-descriptor)
           duplikoitu-kysymys-hakukohde-oid (:duplikoitu-kysymys-hakukohde-oid field-descriptor)
           duplikoitu-followup-hakukohde-oid (:duplikoitu-followup-hakukohde-oid field-descriptor)]
          :when
          (and (or (= :birth-date ans-key)
                   (= :gender ans-key)
                   (get-in ui [ans-key :visible?] true))
               (not (:exclude-from-answers field-descriptor)))]
      (cond->
        {:key       (:id field-descriptor)
         :value     (cond (#{"attachment"} (:fieldType field-descriptor))
                          (sanitize-attachment-value value values question-group-highest-dimension)

                          :else
                          (sanitize-value field-descriptor value question-group-highest-dimension))
         :fieldType (:fieldType field-descriptor)
         :label     (:label field-descriptor)}
        (some? original-question) (assoc :original-question original-question)
        (some? original-followup) (assoc :original-followup original-followup)
        (some? duplikoitu-kysymys-hakukohde-oid) (assoc :duplikoitu-kysymys-hakukohde-oid duplikoitu-kysymys-hakukohde-oid)
        (some? duplikoitu-followup-hakukohde-oid) (assoc :duplikoitu-followup-hakukohde-oid duplikoitu-followup-hakukohde-oid)))))

(defn create-application-to-submit [application form lang strict-warnings-on-unchanged-edits? session-data]
  (let [{secret :secret virkailija-secret :virkailija-secret} application]
    (cond-> {:form      (:id form)
             :strict-warnings-on-unchanged-edits? (if (nil? strict-warnings-on-unchanged-edits?)
                                                    true
                                                    (boolean strict-warnings-on-unchanged-edits?))
             :lang      lang
             :haku      (-> form :tarjonta :haku-oid)
             :hakukohde (map :value (get-in application [:answers :hakukohteet :values] []))

             :answers   (create-answers-to-submit (:answers application) form (:ui application))
             :tunnistautunut (boolean (:logged-in session-data))
             :save-koski-tutkinnot (and (= (:auth-type session-data) constants/auth-type-strong)
                                        (tutkinto-util/koski-tutkinto-levels-in-form form)
                                        (tutkinto-util/save-koski-tutkinnot? form))}

            (some? (get application :selection-id)) (assoc :selection-id (get application :selection-id))

            (some? secret) (assoc :secret secret)

            (some? virkailija-secret) (assoc :virkailija-secret virkailija-secret))))

(defn extract-wrapper-sections [form]
  (->> (:content form)
       (filter #(and (= (:fieldClass %) "wrapperElement")
                     (not= (:fieldType %) "adjacentfieldset")))
       (map #(select-keys % [:id :label :children]))))

