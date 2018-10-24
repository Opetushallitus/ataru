(ns ataru.hakija.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]
            [ataru.util :as util]
            [ataru.hakuaika :as hakuaika]
            [ataru.application-common.application-field-common :as afc]
            [ataru.hakija.application :refer [answers->valid-status]]
            [ataru.hakija.person-info-fields :as person-info-fields]))

(defonce attachment-modify-grace-period-days
  (get (js->clj js/config) "attachment-modify-grace-period-days" 14))

(re-frame/reg-sub
  :application/form
  (fn [db _]
    (:form db)))

(re-frame/reg-sub
  :application/flat-form-content
  (fn [db _]
    (:flat-form-content db)))

(re-frame/reg-sub
  :application/form-field
  (fn [_ _]
    (re-frame/subscribe [:application/flat-form-content]))
  (fn [flat-form-content [_ id]]
    (first (filter #(= (keyword id) (keyword (:id %))) flat-form-content))))

(re-frame/reg-sub
  :application/application
  (fn [db _]
    (:application db)))

(re-frame/reg-sub
  :application/editing?
  (fn [_ _]
    (re-frame/subscribe [:application/application]))
  (fn [application _]
    (:editing? application)))

(re-frame/reg-sub
  :application/answers
  (fn [_ _]
    (re-frame/subscribe [:application/application]))
  (fn [application _]
    (:answers application)))

(re-frame/reg-sub
  :application/answer
  (fn [_ _]
    (re-frame/subscribe [:application/answers]))
  (fn [answers [_ id question-group-idx repeatable-idx]]
    (let [id (keyword id)]
      (cond (some? question-group-idx)
            (get-in answers [id :values question-group-idx (or repeatable-idx 0)])
            (some? repeatable-idx)
            (get-in answers [id :values repeatable-idx])
            :else
            (get answers id)))))

(re-frame/reg-sub
  :application/person
  (fn [_ _]
    (re-frame/subscribe [:application/application]))
  (fn [application _]
    (:person application)))

(re-frame/reg-sub
  :application/attachment-deadline
  (fn [db _]
    [(re-frame/subscribe [:state-query [:form :tarjonta :hakukohteet]])
     (re-frame/subscribe [:application/selected-language])])
  (fn [[hakukohteet selected-language] [_ field]]
    (if-let [deadline (or (-> field :params :deadline-label selected-language)
                          (when hakukohteet
                            (some-> (hakuaika/select-hakuaika-for-field field hakukohteet) :label :attachment-period-end selected-language)))]
      deadline)))

(re-frame/reg-sub
  :application/haku-aika
  (fn [db _]
    (-> db :form :tarjonta :hakuaika)))

(re-frame/reg-sub
  :application/answer-value
  (fn [[_ id question-group-idx repeatable-idx] _]
    [(re-frame/subscribe [:application/answer
                          id
                          question-group-idx
                          repeatable-idx])
     (re-frame/subscribe [:application/person])
     (re-frame/subscribe [:application/editing?])])
  (fn [[answer person editing?] [_ id _ _]]
    (let [id (keyword id)]
      (if (and editing?
               (contains? person-info-fields/editing-forbidden-person-info-field-ids id))
        (get person id)
        (:value answer)))))

(re-frame/reg-sub
  :application/answer-valid?
  (fn [[_ id question-group-idx repeatable-idx] _]
    [(re-frame/subscribe [:application/answer
                          id
                          question-group-idx
                          repeatable-idx])
     (re-frame/subscribe [:application/editing?])])
  (fn [[answer editing?] [_ id _ _]]
    (if (and editing?
             (contains? person-info-fields/editing-forbidden-person-info-field-ids (keyword id)))
      true
      (:valid answer))))

(re-frame/reg-sub
  :application/answer-errors
  (fn [[_ id question-group-idx repeatable-idx] _]
    (re-frame/subscribe [:application/answer
                         id
                         question-group-idx
                         repeatable-idx]))
  (fn [answer [_ _ _ _]]
    (:errors answer)))

(re-frame/reg-sub
  :application/repeatable-answer-count
  (fn [[_ id _] _]
    (re-frame/subscribe [:application/answer id nil nil]))
  (fn [answer [_ _ question-group-idx]]
    (max 1 (count
            (if (some? question-group-idx)
              (get-in answer [:values question-group-idx])
              (:values answer))))))

(re-frame/reg-sub
  :application/submitted?
  (fn [_ _]
    (re-frame/subscribe [:application/application]))
  (fn [application _]
    (= :submitted (:submit-status application))))

(re-frame/reg-sub
  :application/cannot-edit-because-in-processing?
  (fn [_ _]
    (re-frame/subscribe [:application/application]))
  (fn [application _]
    (:cannot-edit-because-in-processing application)))

(re-frame/reg-sub
  :application/virkailija?
  (fn [_ _]
    (re-frame/subscribe [:application/application]))
  (fn [application _]
    (some? (:virkailija-secret application))))

(re-frame/reg-sub
  :application/ui
  (fn [_ _]
    (re-frame/subscribe [:application/application]))
  (fn [application _]
    (:ui application)))

(re-frame/reg-sub
  :application/ui-of
  (fn [_ _]
    (re-frame/subscribe [:application/ui]))
  (fn [ui [_ id]]
    (get ui (keyword id))))

(re-frame/reg-sub
  :state-query
  (fn [db [_ path]]
    (get-in db (remove nil? path))))

(re-frame/reg-sub
  :application/valid-status
  (fn [db]
    (-> db :application :answers-validity)))

(re-frame/reg-sub
 :application/can-apply?
 (fn [db]
   (if-let [hakukohteet (get-in db [:form :tarjonta :hakukohteet])]
     (or (some? (get-in db [:application :virkailija-secret]))
         (some #(get-in % [:hakuaika :on]) hakukohteet))
     true)))

(re-frame/reg-sub
  :application/hakukohde-count
  (fn [db]
    (count (-> db :tarjonta :hakukohteet))))

(re-frame/reg-sub
  :application/selected-language
  (fn [_ _]
    (re-frame/subscribe [:application/form]))
  (fn [form _]
    (:selected-language form)))

(re-frame/reg-sub
  :application/languages
  (fn [_ _]
    (re-frame/subscribe [:application/form]))
  (fn [form _]
    (:languages form)))

(re-frame/reg-sub
  :application/form-language
  (fn [_ _]
    (re-frame/subscribe [:application/selected-language]))
  (fn [selected-language _]
    ;; When user lands on the page, there isn't any language set until the
    ;; form is loaded
    (or selected-language :fi)))

(defn- selected-hakukohteet [db]
  (map :value (get-in db [:application :answers :hakukohteet :values] [])))

(defn- selected-hakukohteet-from-tarjonta [db]
  (let [selected-hakukohteet     (set (selected-hakukohteet db))]
    (->> (get-in db [:form :tarjonta :hakukohteet])
         (filter #(contains? selected-hakukohteet (:oid %))))))

(re-frame/reg-sub
  :application/selected-hakukohteet-for-field
  (fn [db [_ field]]
    (when-let [ids (seq (concat (get field :belongs-to-hakukohderyhma [])
                                (get field :belongs-to-hakukohteet [])))]
      (filter #(not (empty? (clojure.set/intersection
                             (set ids)
                             (set (cons (:oid %) (:hakukohderyhmat %))))))
              (selected-hakukohteet-from-tarjonta db)))))

(re-frame/reg-sub
  :application/cannot-view?
  (fn [[_ id] _]
    [(re-frame/subscribe [:application/form-field id])
     (re-frame/subscribe [:application/editing?])])
  (fn [[field editing?] _]
    (and editing? (:cannot-view field))))

(re-frame/reg-sub
  :application/cannot-edit?
  (fn [[_ id] _]
    [(re-frame/subscribe [:application/form-field id])
     (re-frame/subscribe [:application/editing?])])
  (fn [[field editing?] _]
    (and editing? (:cannot-edit field))))

(re-frame/reg-sub
  :application/disabled?
  (fn [[_ id] _]
    (re-frame/subscribe [:application/ui-of id]))
  (fn [ui _]
    (get ui :disabled? false)))

(re-frame/reg-sub
  :application/get-i18n-text
  (fn [db [_ translations]]
    (get translations @(re-frame/subscribe [:application/form-language]))))

(re-frame/reg-sub
  :application/adjacent-field-row-amount
  (fn [db [_ field-descriptor question-group-idx]]
    (let [child-id   (-> (:children field-descriptor) first :id keyword)
          value-path (cond-> [:application :answers child-id :values]
                       question-group-idx (conj question-group-idx))
          row-amount (-> (get-in db value-path [])
                         count)]
      (if (= row-amount 0)
        1
        row-amount))))

(re-frame/reg-sub
  :application/multiple-choice-option-checked?
  (fn [db [_ parent-id option-value question-group-idx]]
    (let [option-path (cond-> [:application :answers parent-id :options]
                        question-group-idx (conj question-group-idx))
          options     (get-in db option-path)]
      (true? (get options option-value)))))

(re-frame/reg-sub
  :application/single-choice-option-checked?
  (fn [db [_ parent-id option-value question-group-idx]]
    (let [value (get-in db (if question-group-idx
                             [:application :answers parent-id :values question-group-idx 0 :value]
                             [:application :answers parent-id :value]))]
      (= option-value value))))

(defn- hakukohteet-field [db]
  (->> (:flat-form-content db)
       (filter #(= "hakukohteet" (:id %)))
       first))

(re-frame/reg-sub
  :application/hakukohde-options
  (fn [db _]
    (:options (hakukohteet-field db))))

(re-frame/reg-sub
  :application/hakukohde-options-by-oid
  (fn [db _]
    (into {} (map (juxt :value identity)
                  @(re-frame/subscribe [:application/hakukohde-options])))))

(re-frame/reg-sub
  :application/selected-hakukohteet
  (fn [db _]
    (selected-hakukohteet db)))

(re-frame/reg-sub
  :application/hakukohteet-editable?
  (fn [db _]
    (and (< 1 (count @(re-frame/subscribe [:application/hakukohde-options])))
         (not @(re-frame/subscribe [:application/cannot-edit? :hakukohteet])))))

(re-frame/reg-sub
  :application/hakukohde-editable?
  (fn [db [_ hakukohde-oid]]
    (or (some? (get-in db [:application :virkailija-secret]))
        (->> (get-in db [:form :tarjonta :hakukohteet])
             (some #(when (= hakukohde-oid (:oid %)) %))
             :hakuaika
             :on))))

(re-frame/reg-sub
  :application/hakukohde-query
  (fn [db _] (get-in db [:application :hakukohde-query])))

(re-frame/reg-sub
  :application/show-more-hakukohdes?
  (fn [db _]
    (let [remaining-hakukohde-results (-> db :application :remaining-hakukohde-search-results count)]
      (> remaining-hakukohde-results 0))))

(re-frame/reg-sub
  :application/hakukohde-hits
  (fn [db _]
    (get-in db [:application :hakukohde-hits])))

(re-frame/reg-sub
  :application/hakukohde-selected?
  (fn [db [_ hakukohde-oid]]
    (some #(= % hakukohde-oid)
          @(re-frame/subscribe [:application/selected-hakukohteet]))))

(re-frame/reg-sub
  :application/hakukohde-deleting?
  (fn [db [_ hakukohde-oid]]
    (some #{hakukohde-oid} (-> db :application :ui :hakukohteet :deleting))))

(re-frame/reg-sub
  :application/max-hakukohteet
  (fn [db _]
    (get-in (hakukohteet-field db)
            [:params :max-hakukohteet]
            nil)))

(re-frame/reg-sub
  :application/hakukohteet-full?
  (fn [_ _]
    (if-let [max-hakukohteet @(re-frame/subscribe [:application/max-hakukohteet])]
      (<= max-hakukohteet
          (count @(re-frame/subscribe [:application/selected-hakukohteet])))
      false)))

(re-frame/reg-sub
  :application/default-languages
  (fn [_ _]
    [(re-frame/subscribe [:application/selected-language])
     (re-frame/subscribe [:application/languages])])
  (fn [[selected-language languages] _]
    (concat [selected-language]
            languages
            [:fi :sv :en])))

(re-frame/reg-sub
  :application/hakukohde-label
  (fn [db [_ hakukohde-oid]]
    (util/non-blank-val
     (get-in @(re-frame/subscribe [:application/hakukohde-options-by-oid])
             [hakukohde-oid :label])
     @(re-frame/subscribe [:application/default-languages]))))

(re-frame/reg-sub
  :application/hakukohde-description
  (fn [db [_ hakukohde-oid]]
    (util/non-blank-val
     (get-in @(re-frame/subscribe [:application/hakukohde-options-by-oid])
             [hakukohde-oid :description])
     @(re-frame/subscribe [:application/default-languages]))))

(re-frame/reg-sub
  :application/hakukohteet-header
  (fn [db _]
    (util/non-blank-val
     (:label (hakukohteet-field db))
     @(re-frame/subscribe [:application/default-languages]))))

(re-frame/reg-sub
  :application/show-hakukohde-search
  (fn [db _]
    (get-in db [:application :show-hakukohde-search])))

(re-frame/reg-sub
  :application/mouse-over-remove-question-group-button
  (fn [db [_ field-descriptor idx]]
    (get-in db [:application :ui (keyword (:id field-descriptor)) :mouse-over-remove-button idx])))

(re-frame/reg-sub
  :application/prioritize-hakukohteet?
  (fn [db _]
    (-> db :form :tarjonta :prioritize-hakukohteet)))

(re-frame/reg-sub
  :application/hakukohde-priority-number
  (fn [db [_ hakukohde-oid]]
    (->> (-> db :application :answers :hakukohteet :values)
         (keep-indexed #(when (= hakukohde-oid (:value %2))
                          (inc %1)))
         first)))

(re-frame/reg-sub
  :application/tarjonta-hakukohteet
  (fn [db _]
    (-> db :form :tarjonta :hakukohteet)))

(re-frame/reg-sub
  :application/visible-validation-error
  (fn [_ _]
    (re-frame/subscribe [:application/application]))
  (fn [application _]
    (:visible-validation-error application)))

(re-frame/reg-sub
  :application/validators-processing
  (fn [_ _]
    (re-frame/subscribe [:application/application]))
  (fn [application _]
    (:validators-processing application)))

(re-frame/reg-sub
  :application/validator-processing?
  (fn [_ _]
    (re-frame/subscribe [:application/validators-processing]))
  (fn [validators-processing [_ id]]
    (contains? validators-processing (keyword id))))

(re-frame/reg-sub
  :application/show-validation-error-class?
  (fn [[_ id question-group-idx repeatable-idx] _]
    [(re-frame/subscribe [:application/form-field id])
     (re-frame/subscribe [:application/answer-value id question-group-idx repeatable-idx])
     (re-frame/subscribe [:application/answer-valid? id question-group-idx repeatable-idx])
     (re-frame/subscribe [:application/validator-processing? id])])
  (fn [[field value valid? validator-processing?] _]
    (and (not valid?)
         (or (afc/is-required-field? field)
             (-> field :params :numeric))
         (if (string? value)
           (not (clojure.string/blank? value))
           (not (empty? value)))
         (not validator-processing?))))

(re-frame/reg-sub
  :application/show-validation-error?
  (fn [_ _]
    (re-frame/subscribe [:application/visible-validation-error]))
  (fn [visible-validation-error [_ id]]
    (or (nil? visible-validation-error) (= id visible-validation-error))))

(re-frame/reg-sub
  :application/verify-email?
  (fn [db [_ id]]
    (and (= :email id)
         (-> db :form :tarjonta :yhteishaku))))
