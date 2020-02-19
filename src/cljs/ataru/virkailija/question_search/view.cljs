(ns ataru.virkailija.question-search.view
  (:require ataru.virkailija.question-search.subs
            ataru.virkailija.question-search.handlers
            [ataru.util :as util]
            [re-frame.core :as re-frame]))

(defn- ancestor-fields [field fields-by-id]
  (if-let [parent-id (or (:children-of field)
                         (:followup-of field))]
    (let [parent (get fields-by-id (keyword parent-id))]
      (conj (ancestor-fields parent fields-by-id) parent))
    []))

(defn- ancestor-labels
  [ancestors lang]
  (map (fn [parent field]
         [:li.question-search-results__result-ancestor
          [:div.question-search-results__result-ancestor-label
           (util/non-blank-val (:label field) [lang :fi :sv :en])]
          (let [option-label (some #(when (= (:option-value field) (:value %))
                                      (util/non-blank-val (:label %) [lang :fi :sv :en]))
                                   (:options parent))]
            (when (not (clojure.string/blank? option-label))
              [:div.question-search-results__result-ancestor-option-label
               option-label]))])
       (cons nil ancestors)
       ancestors))

(defn search-input
  [form-key id placeholder disabled? filter-predicate]
  (let [search-input @(re-frame/subscribe [:question-search/search-input form-key id])]
    [:div.question-search-search-input
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

(defn- result
  [field fields-by-id lang on-click]
  [:li.question-search-results__result
   {:on-click #(on-click (:id field))}
   [:div.question-search-results__result-label
    (util/non-blank-val (:label field) [lang :fi :sv :en])]
   (when-let [hakukohteet (seq (:belongs-to-hakukohteet field))]
     [:div.question-search-results__result-hakukohteet
      [:div
       @(re-frame/subscribe [:editor/virkailija-translation :hakukohteet])]
      (into
       [:ul.question-search-results__result-hakukohteet-list]
       (map (fn [oid]
              [:li.question-search-results__result-hakukohde
               @(re-frame/subscribe [:application/hakukohde-and-tarjoaja-name oid])])
            (:belongs-to-hakukohteet field)))])
   (when-let [hakukohderyhmat (seq (:belongs-to-hakukohderyhma field))]
     [:div.question-search-results__result-hakukohderyhmat
      [:div
       @(re-frame/subscribe [:editor/virkailija-translation :hakukohderyhmat])]
      (into
       [:ul.question-search-results__result-hakukohderyhmat-list]
       (map (fn [oid]
              [:li.question-search-results__result-hakukohderyhma
               @(re-frame/subscribe [:application/hakukohderyhma-name oid])])
            (:belongs-to-hakukohderyhma field)))])
   (when-let [ancestors (seq (ancestor-fields field fields-by-id))]
     [:div.question-search-results__result-ancestors
      [:div
       @(re-frame/subscribe [:editor/virkailija-translation :place-in-form])]
      (into
       [:ul.question-search-results__result-ancestors-list]
       (ancestor-labels ancestors lang))])])

(defn search-results
  [form-key id on-click]
  (let [lang           @(re-frame/subscribe [:editor/virkailija-lang])
        fields-by-id   @(re-frame/subscribe [:application/form-fields-by-id form-key])
        search-results @(re-frame/subscribe [:question-search/search-result form-key id])]
    (cond (seq search-results)
          (into [:ul.question-search-results]
                (keep (fn [id]
                        ^{:key id}
                        (result (get fields-by-id id) fields-by-id lang on-click))
                      search-results))
          (some? search-results)
          [:div.question-search-empty-result
           @(re-frame/subscribe [:editor/virkailija-translation :no-search-hits])]
          :else
          nil)))
