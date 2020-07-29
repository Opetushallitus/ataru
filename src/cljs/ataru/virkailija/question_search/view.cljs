(ns ataru.virkailija.question-search.view
  (:require ataru.virkailija.question-search.subs
            ataru.virkailija.question-search.handlers
            [ataru.util :as util]
            [clojure.string :as string]
            [re-frame.core :as re-frame]))

(defn search-input
  [form-key id placeholder disabled? filter-predicate]
  (let [search-input @(re-frame/subscribe [:question-search/search-input form-key id])]
    [(if disabled?
       :div.question-search-search-input.question-search-search-input--disabled
       :div.question-search-search-input)
     [:input.question-search-search-input__input
      {:type        "text"
       :value       search-input
       :placeholder placeholder
       :disabled    disabled?
       :on-change   #(re-frame/dispatch [:question-search/set-search-input
                                         form-key
                                         id
                                         filter-predicate
                                         (.. % -target -value)])}]
     [:i.zmdi.zmdi-search.question-search-search-input__icon]]))

(defn- hakukohteet
  [field]
  (when-let [hakukohteet (some->> (seq (:belongs-to-hakukohteet field))
                                  (map (fn [oid] @(re-frame/subscribe [:application/hakukohde-and-tarjoaja-name oid])))
                                  sort
                                  (string/join ", "))]
    [:div.question-search-results__result-hakukohteet
     [:span.question-search-results__result-hakukohteet-description
      (str @(re-frame/subscribe [:editor/virkailija-translation :hakukohteet]) ": ")]
     hakukohteet]))

(defn- hakukohderyhmat
  [field]
  (when-let [hakukohderyhmat (some->> (seq (:belongs-to-hakukohderyhma field))
                                      (map (fn [oid] @(re-frame/subscribe [:application/hakukohderyhma-name oid])))
                                      sort
                                      (string/join ", "))]
    [:div.question-search-results__result-hakukohderyhmat
     [:span.question-search-results__result-hakukohderyhmat-description
      (str @(re-frame/subscribe [:editor/virkailija-translation :hakukohderyhmat]) ": ")]
     hakukohderyhmat]))

(defn- ancestor-fields [fields-by-id field]
  (if-let [parent-id (or (:children-of field)
                         (:followup-of field))]
    (let [parent (get fields-by-id (keyword parent-id))]
      (conj (ancestor-fields fields-by-id parent) field))
    [field]))

(defn- ancestors-label
  [lang ancestors]
  (string/join
   "\u00a0/ "
   (map (fn [field followup]
          (let [label        (util/non-blank-val (:label field) [lang :fi :sv :en])
                option-label (some #(when (= (:option-value followup) (:value %))
                                      (util/non-blank-val (:label %) [lang :fi :sv :en]))
                                   (:options field))]
            (string/join
              ": "
              (remove string/blank? [label option-label]))))
        ancestors
        (concat (rest ancestors) [{}]))))

(defn- place-in-form
  [fields-by-id lang field]
  (when-let [ancestors (seq (ancestor-fields fields-by-id field))]
    (let [key (if (= (:fieldType field) "attachment")
                :attachment
                :question)]
      [:div.question-search-results__result-ancestors
       [:span.question-search-results__result-ancestors-description
        (str @(re-frame/subscribe [:editor/virkailija-translation key]) ": ")]
       (ancestors-label lang ancestors)])))

(defn- result
  [form-key id on-click]
  (let [lang         @(re-frame/subscribe [:editor/virkailija-lang])
        fields-by-id @(re-frame/subscribe [:application/form-fields-by-id form-key])
        field        (get fields-by-id id)]
    [:li.question-search-results__result
     {:on-click    #(on-click (:id field))
      :on-key-down (fn [e]
                     (when (or (= " " (.-key e))
                               (= "Enter" (.-key e)))
                       (.preventDefault e)
                       (on-click (:id field))))
      :tab-index   0
      :role        "button"}
     [:div.question-search-results__result-label
      (util/non-blank-val (:label field) [lang :fi :sv :en])]
     (hakukohteet field)
     (hakukohderyhmat field)
     (place-in-form fields-by-id lang field)]))

(defn search-results
  [form-key id on-click]
  (let [searching?     @(re-frame/subscribe [:question-search/searching? form-key id])
        search-results @(re-frame/subscribe [:question-search/search-result form-key id])]
    (when (or searching?
              (seq search-results))
      (cond-> (into [:ul.question-search-results
                     {:tab-index -1}]
                    (keep (fn [id]
                            ^{:key id}
                            [result form-key id on-click])
                          search-results))
              searching?
              (conj [:li.question-search-results__result.question-search-results__result--searching
                     [:i.zmdi.zmdi-spinner.spin]])))))
