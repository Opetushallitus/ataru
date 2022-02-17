(ns ataru.applications.harkinnanvaraisuus-filter
  (:require [ataru.util :refer [flatten-form-fields]]
            [ataru.application.option-visibility :as option-visibility]))

(def harkinnanvaraisuus-key "harkinnanvaraisuus")
(def harkinnanvaraisuus-lomakeosio-key "harkinnanvaraisuus-wrapper")
(def harkinnanvaraisuus-yes-answer-value "1")

(defn- answered-yes-to-harkinnanvaraisuus
  [application]
  (->> (-> application :content :answers)
       (filter #(= harkinnanvaraisuus-key (:original-question %)))
       (some #(= harkinnanvaraisuus-yes-answer-value (:value %)))
       (boolean)))

(defn- field-has-section-visibility-conditions-targeting-harkinnanvaraisuus
  [field]
  (when-let [conditions (seq (:section-visibility-conditions field))]
    (some #(= harkinnanvaraisuus-lomakeosio-key (:section-name %)) conditions)))

(defn- map-visibility-conditions-with-field
  [field]
  (->> (:section-visibility-conditions field)
       seq
       (filter #(= harkinnanvaraisuus-lomakeosio-key (:section-name %)))
       (map (fn [condition] {:id (:id field) :condition condition}))))

(defn- harkinnanvaraisuus-is-set-by-form-logic
  [form-id-with-field-ids-with-conditions application]
  (let [field-ids-with-conditions-targeting-harkinnanvaraisuus (->> form-id-with-field-ids-with-conditions
                                                                    (filter #(= (:form application) (:id %)))
                                                                    (first)
                                                                    :fields-with-visibility-conditions)
        answers (-> application :content :answers)
        get-answer-value (fn [id] (:value (first (filter #(= id (:key %)) answers))))]
    (some #(option-visibility/answer-satisfies-condition? (get-answer-value (:id %)) (:condition %)) field-ids-with-conditions-targeting-harkinnanvaraisuus)))

(defn- form-id-with-field-ids-with-conditions
   [fetch-form-fn form-id]
  (let [fields (-> (fetch-form-fn form-id)
                   :content
                   (flatten-form-fields))
        field-ids-with-conditions-targeting-harkinnanvaraisuus (->> fields
                                                                    (filter field-has-section-visibility-conditions-targeting-harkinnanvaraisuus)
                                                                    (mapcat map-visibility-conditions-with-field))]
    {:id                                form-id
      :fields-with-visibility-conditions field-ids-with-conditions-targeting-harkinnanvaraisuus}))

(defn- filter-harkinnanvaraiset-applications
  [fetch-application-content-fn fetch-form-fn applications]
  (let [applications-contents-and-forms (fetch-application-content-fn (map :id applications))
        distinct-form-ids (->> applications-contents-and-forms
                            (map :form)
                            (distinct))
        form-id-with-field-ids-with-conditions (map (partial form-id-with-field-ids-with-conditions fetch-form-fn) distinct-form-ids)
        harkinnanvaraiset-ids (->> applications-contents-and-forms
                                   (filter (some-fn answered-yes-to-harkinnanvaraisuus
                                                    (partial harkinnanvaraisuus-is-set-by-form-logic form-id-with-field-ids-with-conditions)))
                                   (map :id)
                                   set)]
    (filter (comp harkinnanvaraiset-ids :id) applications)))

(defn filter-applications-by-harkinnanvaraisuus
  [fetch-applications-content-fn fetch-form-fn applications filters]
  (let [only-harkinnanvaraiset? (-> filters :harkinnanvaraisuus :only-harkinnanvaraiset)]
    (if only-harkinnanvaraiset?
      (filter-harkinnanvaraiset-applications fetch-applications-content-fn fetch-form-fn applications)
      applications)))
