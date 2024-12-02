(ns ataru.virkailija.editor.components.form-properties.multiple-checkbox-component
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.ratom :refer-macros [reaction]]
            [reagent.core :as r]
            [ataru.component-data.component :as component]
            [ataru.virkailija.editor.components.text-header-component :as text-header-component]
            [ataru.virkailija.editor.components.component-content :as component-content]
            [ataru.virkailija.editor.components.followup-question :as followup-question]))

(defn- followup-count [option]
  (reduce +
          (map (fn [fup]
                 (let [children (:children fup)]
                   (if (seq children)
                     (count (filter #(not (get-in % [:params :transparent])) children))
                     (if (not (get-in fup [:params :transparent])) 1 0))))
               (:followups option))))

(defn multiple-checkbox-component [content _ path]
  (let [category (keyword (:category content))
        options (:options content)
        show-followups (r/atom nil)
        virkailija-lang (subscribe [:editor/virkailija-lang])
        component-locked (subscribe [:editor/component-locked? path])
        default-selected-list (mapv :id (filter #(= true (:default-value %)) options))
        option-check-statuses (reaction (let [currently-checked
                                              @(subscribe [:editor/get-selected-property-options
                                                           category
                                                           default-selected-list])]
                                          (into {}
                                                (map
                                                  (fn [option] {(keyword (:id option))
                                                                (not (nil? (some #(= (:id option) %)
                                                                                 currently-checked)))})
                                                  options))))]
    (fn [content followups path]
      (let [option-count (count options)
            list-of-selected (mapv :id (filter #((keyword (:id %)) @option-check-statuses) options))
            update-option-state (fn [option-id checked?]
                                  (let [option-currently-checked? ((keyword option-id) @option-check-statuses)
                                        new-list-of-selected (if (and checked? (not option-currently-checked?))
                                                               (conj list-of-selected option-id)
                                                               (vec (remove #(= option-id %) list-of-selected)))]
                                    (dispatch [:editor/update-selected-property-options
                                               category new-list-of-selected])))]
        (when (or (nil? @show-followups)
                  (not (= (count @show-followups) option-count)))
          (reset! show-followups (vec (repeat option-count false))))
        [:div.editor-form__component-wrapper
         {:data-test-id "editor-form__metadata-multiple-checkbox-component-wrapper"}
         [text-header-component/text-header (:id content) (get-in content [:label @virkailija-lang]) path nil
          :data-test-id "editor-form__metadata-multiple-checkbox-component-main"]
         [component-content/component-content
          path
          [:div.editor-form__multi-question-wrapper
           [:div.editor-form__text-field-checkbox-wrapper
            (when (some? (:description content))
              [:div.editor-form__component-item-description
               [:span (get-in content [:description @virkailija-lang])]]
              )
            (doall (map-indexed (fn [idx item]
                                  ^{:key (str "options-" idx)}
                                  [:div
                                   [:div.editor-form__checkbox-container
                                    [:input.editor-form__checkbox
                                     {:id        (:id item)
                                      :type      "checkbox"
                                      :disabled  (or (= true (:forced item)) @component-locked)
                                      :checked   ((keyword (:id item)) @option-check-statuses)
                                      :on-change (fn [event]
                                                   (.preventDefault event)
                                                   (update-option-state (:id item) (-> event .-target .-checked)))}]
                                    [:label.editor-form__checkbox-label
                                     {:for (:id item)}
                                     (get-in item [:label @virkailija-lang])]]
                                   (when ((keyword (:id item)) @option-check-statuses)
                                     (let [followup-label (get-in item [:followup-label @virkailija-lang]
                                                                  @(subscribe [:editor/virkailija-translation :followups]))
                                           followups-of-item (nth followups idx)]
                                      [:div
                                        [:div.editor-form__followup-custom-query-container
                                         [followup-question/followup-question
                                          idx followups-of-item show-followups followup-label (followup-count item)]]
                                      (if (get item :allow-user-followups true)
                                        [followup-question/followup-question-overlay idx followups-of-item path show-followups]
                                        [followup-question/followup-question-overlay-readonly idx followups-of-item show-followups]
                                        )]))])
                                options))]]]]))))