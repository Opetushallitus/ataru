(ns ataru.virkailija.views.template-editor
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]))

(def language-names
  {:fi "Suomi"
   :sv "Ruotsi"
   :en "Englanti"})

(defn- get-body-class-list
  []
  (-> js/document
      (.-body)
      (.-classList)))

(defn- render-template-editor
  []
  (let [lang             @(subscribe [:state-query [:editor :email-template-lang]])
        contents         @(subscribe [:state-query [:editor :email-template]])
        contents-changed @(subscribe [:editor/email-templates-altered])
        any-changed?     (some true? (vals contents-changed))
        lang-kw          (keyword lang)
        lang-content     (-> contents lang-kw)]
    [:div.virkailija-modal__container
     [:div.virkailija-modal__content.virkailija-email-preview__modal
      [:a.virkailija-modal__close-link
       {:on-click #(dispatch [:editor/toggle-email-template-editor])}
       "Sulje"]
      [:div.virkailija-email-preview
       [:h3.virkailija-email-preview__heading "Sähköpostiviestin sisältö"]
       [:div.virkailija-email-preview__info-text
        (str
          "Hakija saa allaolevan viestin sähköpostilla hakemuksen lähettämisen jälkeen lähettäjältä '"
          (get-in contents [lang-kw :from])
          "'")]
       [:div.virkailija-email-preview__tabs
        [:div.virkailija-email-preview__tab-panel
         (map
           (fn [button-lang]
             (list
               [:input.virkailija-email-preview__tab
                {:key       (str "email-preview-lang-radio-" lang)
                 :type      "radio"
                 :name      "lang"
                 :value     button-lang
                 :id        (str "email-template-language-selection-" button-lang)
                 :checked   (= button-lang lang)
                 :on-change #(dispatch [:editor/set-email-template-language button-lang])}]
               [:label.virkailija-email-preview__tab-label
                {:key   (str "email-preview-lang-radio-label-" lang)
                 :for   (str "email-template-language-selection-" button-lang)
                 :class (when (= button-lang lang) "virkailija-email-preview__tab-label--selected")}
                (get language-names (keyword button-lang))
                (when ((keyword button-lang) contents-changed)
                  [:span.virkailija-email-preview__tab-edited "*"])]))
           ["fi" "sv" "en"])]
        [:div.virkailija-email-preview__tab-border]
        [:div.virkailija-email-preview__tab-content
         [:h4.virkailija-email-preview__sub-heading "Muokattava osuus"]
         [:textarea.virkailija-email-preview__text-input
          {:value     (:content lang-content)
           :on-change #(dispatch [:editor/update-email-preview lang-kw (.-value (.-target %))])}]
         [:div.virkailija-email-preview__preview-container
          [:h4.virkailija-email-preview__sub-heading "Viestin esikatselu"]
          [:div.virkailija-email-preview__preview-header
           "Otsikko:"
           [:span.virkailija-email-preview__preview-header-value
            (:subject lang-content)]]
          [:iframe.virkailija-email-preview__preview-iframe
           {:srcDoc (:body lang-content)}]
          [:div.virkailija-email-preview__buttons
           [:button.virkailija-email-preview__buttons-save.editor-form__control-button
            {:class    (if any-changed?
                         "editor-form__control-button--enabled"
                         "editor-form__control-button--disabled")
             :on-click #(when any-changed? (dispatch [:editor/save-email-template]))}
            "Tallenna muutokset"]]]]]]]]))

(defn email-template-editor
  []
  (reagent/create-class
    {:component-did-mount    #(.add (get-body-class-list) "virkailija-modal-enabled")
     :component-will-unmount #(.remove (get-body-class-list) "virkailija-modal-enabled")
     :reagent-render         render-template-editor}))