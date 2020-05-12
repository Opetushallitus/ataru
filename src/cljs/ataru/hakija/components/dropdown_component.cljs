(ns ataru.hakija.components.dropdown-component
  (:require [ataru.util :as util]
            [ataru.hakija.components.label-component :as label-component]
            [ataru.hakija.components.question-hakukohde-names-component :as hakukohde-names-component]
            [ataru.hakija.components.info-text-component :as info-text-component]
            [ataru.application-common.application-field-common :as application-field]
            [re-frame.core :as re-frame]))

(defn- dropdown-followups [field-descriptor value render-field]
  (when-let [followups (seq (util/resolve-followups
                              (:options field-descriptor)
                              value))]
    [:div.application__form-dropdown-followups.animated.fadeIn
     (for [followup followups]
       ^{:key (:id followup)}
       [render-field followup nil])]))

(defn dropdown [field-descriptor idx render-field]
  (let [languages (re-frame/subscribe [:application/default-languages])
        id        (application-field/answer-key field-descriptor)
        disabled? @(re-frame/subscribe [:application/cannot-edit? id])
        answer    @(re-frame/subscribe [:application/answer id idx nil])
        on-change (fn [e]
                    (re-frame/dispatch [:application/dropdown-change
                                        field-descriptor
                                        (.-value (.-target e))
                                        idx]))
        options   @(re-frame/subscribe [:application/visible-options field-descriptor])]
    [:div.application__form-field
     [label-component/label field-descriptor]
     (when (application-field/belongs-to-hakukohde-or-ryhma? field-descriptor)
       [hakukohde-names-component/question-hakukohde-names field-descriptor])
     [:div.application__form-text-input-info-text
      [info-text-component/info-text field-descriptor]]
     [:div.application__form-select-wrapper
      (if disabled?
        [:span.application__form-select-arrow.application__form-select-arrow__disabled
         [:i.zmdi.zmdi-chevron-down]]
        [:span.application__form-select-arrow
         [:i.zmdi.zmdi-chevron-down]])
      [(keyword (str "select.application__form-select" (when (not disabled?) ".application__form-select--enabled")))
       {:id           (:id field-descriptor)
        :value        (or (:value answer) "")
        :on-change    on-change
        :disabled     disabled?
        :required     (application-field/is-required-field? field-descriptor)
        :aria-invalid (not (:valid answer))}
       (doall
         (concat
           (when
             (and
               (nil? (:koodisto-source field-descriptor))
               (not (:no-blank-option field-descriptor))
               (not= "" (:value (first options))))
             [^{:key (str "blank-" (:id field-descriptor))} [:option {:value ""} ""]])
           (map
             (fn [option]
               [:option {:value (:value option)
                         :key   (:value option)}
                (util/non-blank-option-label option @languages)])
             (cond->> options
                      (and (some? (:koodisto-source field-descriptor))
                           (not (:koodisto-ordered-by-user field-descriptor)))
                      (sort-by #(util/non-blank-option-label % @languages))))))]]
     (when-not idx
       (dropdown-followups field-descriptor (:value answer) render-field))]))
