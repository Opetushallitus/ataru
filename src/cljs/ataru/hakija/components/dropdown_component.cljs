(ns ataru.hakija.components.dropdown-component
  (:require [ataru.hakija.application-view-icons :as icons]
            [ataru.util :as util]
            [ataru.hakija.components.form-field-label-component :as form-field-label-component]
            [ataru.hakija.components.info-text-component :as info-text-component]
            [ataru.hakija.components.question-hakukohde-names-component :as hakukohde-names-component]
            [ataru.application-common.application-field-common :as application-field]
            [ataru.application-common.components.dropdown-component :as dropdown-component]
            [re-frame.core :as re-frame]
            [schema.core :as s]
            [schema-tools.core :as st]
            [ataru.hakija.schema.render-field-schema :as render-field-schema]))

(defn- prepend-option [prepend-option options]
  (cons (assoc prepend-option :key (str "prepended-" (:value prepend-option)))
        options))

(defn dropdown [field-descriptor idx render-field]
  (let [languages     (re-frame/subscribe [:application/default-languages])
        id            (application-field/answer-key field-descriptor)
        cannot-edit?  @(re-frame/subscribe [:application/cannot-edit? id])
        answer        @(re-frame/subscribe [:application/answer id idx nil])
        locked?       (:locked answer)
        disabled?     (or cannot-edit? locked?)
        on-change     (fn [e]
                        (re-frame/dispatch [:application/set-repeatable-application-field
                                            field-descriptor
                                            idx
                                            nil
                                            (.-value (.-target e))]))
        options       @(re-frame/subscribe [:application/visible-options field-descriptor])
        followups     (->> options
                           (filter #(= (:value answer) (:value %)))
                           first
                           :followups
                           (filter #(deref (re-frame/subscribe [:application/visible? (keyword (:id %))]))))
        form-field-id (application-field/form-field-id field-descriptor idx)
        data-test-id  (when (some #{id} [:home-town
                                         :language])
                        (-> id
                            name
                            (str "-input")))]
    [:div.application__form-field
     [form-field-label-component/form-field-label field-descriptor form-field-id]
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
      [:div.application__input-container
       (when locked?
         [:div.application__lock-icon-container
          [icons/icon-lock-closed]])
       [(keyword (str "select.application__form-select"
                      (when (not disabled?) ".application__form-select--enabled")
                      (when locked? ".application__form-select--locked")))
        {:id           form-field-id
         :value        (or (:value answer) "")
         :on-change    on-change
         :disabled     disabled?
         :required     (application-field/is-required-field? field-descriptor)
         :aria-invalid (not (:valid answer))
         :data-test-id data-test-id}
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
                          :key   (or (:key option) (:value option))}
                 (util/non-blank-option-label option @languages)])
              (cond->> options
                       (and (some? (:koodisto-source field-descriptor))
                            (not (:koodisto-ordered-by-user field-descriptor)))
                       (sort-by #(util/non-blank-option-label % @languages))
                       (some? (:prepend-option field-descriptor))
                       (prepend-option (:prepend-option field-descriptor))))))]]]
     (when (seq followups)
       (into [:div.application__form-dropdown-followups.animated.fadeIn]
             (for [followup followups]
               ^{:key (:id followup)}
               [render-field followup idx])))]))

(s/defn hakija-dropdown
  [{:keys [field-descriptor
           idx
           on-change]} :- (st/assoc
                            render-field-schema/RenderFieldArgs
                            (s/optional-key :on-change) s/Any)]
  (let [lang             @(re-frame/subscribe [:application/form-language])
        answer           @(re-frame/subscribe [:application/answer
                                               (:id field-descriptor)
                                               idx])
        unselected-label (-> field-descriptor :unselected-label lang)
        options          (cond->> (map (fn [option]
                                         {:label (-> option :label lang)
                                          :value (:value option)})
                                       (:options field-descriptor))
                                  (:sort-by-label field-descriptor)
                                  (sort-by :label))
        data-test-id     (:data-test-id field-descriptor)
        unselected-label-icon           (:unselected-label-icon field-descriptor)]
    [dropdown-component/dropdown
     (cond-> {:options               options
              :unselected-label      unselected-label
              :selected-value        (:value answer)
              :on-change             (fn [value]
                                       (re-frame/dispatch [:application/set-repeatable-application-field
                                                           field-descriptor
                                                           idx
                                                           nil
                                                           value])
                                       (when on-change
                                         (on-change)))}

             data-test-id
             (assoc :data-test-id data-test-id)

             unselected-label-icon
             (assoc :unselected-label-icon unselected-label-icon))]))
