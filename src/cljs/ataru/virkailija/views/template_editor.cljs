(ns ataru.virkailija.views.template-editor
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]))

(def language-names
  {:fi "Suomi"
   :sv "Ruotsi"
   :en "Englanti"})

(defn email-template-editor
  []
  (let [visible? @(subscribe [:state-query [:editor :ui :template-editor-visible?]])
        lang     @(subscribe [:state-query [:editor :email-template-lang]])
        lang-kw  (keyword lang)
        contents @(subscribe [:state-query [:editor :email-template]])]
    (when visible?
      [:div.virkailija-modal__container
       [:div.virkailija-modal__content.virkailija-email-preview__modal
        [:a.virkailija-modal__close-link
         {:on-click #(dispatch [:editor/toggle-email-template-editor])}
         "Sulje"]
        [:div.virkailija-email-preview
         [:h3.virkailija-email-preview__heading "Sähköpostiviestin sisältö"]
         [:div.virkailija-email-preview__info-text
          (str "Hakija saa allaolevan viestin sähköpostilla hakemuksen lähettämisen jälkeen lähettäjältä '" (get-in contents [lang-kw :from]) "'")]
         [:div.virkailija-email-preview__tabs
          [:div.virkailija-email-preview__tab-panel
           (map
             (fn [button-lang]
               (list
                 [:input.virkailija-email-preview__tab
                  {:type      "radio"
                   :name      "lang"
                   :value     button-lang
                   :id        (str "email-template-language-selection-" button-lang)
                   :checked   (= button-lang lang)
                   :on-change #(dispatch [:editor/set-email-template-language button-lang])}]
                 [:label.virkailija-email-preview__tab-label
                  {:for   (str "email-template-language-selection-" button-lang)
                   :class (when (= button-lang lang) "virkailija-email-preview__tab-label--selected")}
                  (get language-names (keyword button-lang))]))
             ["fi" "sv" "en"])]
          [:div.virkailija-email-preview__tab-border]
          [:div.virkailija-email-preview__tab-content
           [:h4.virkailija-email-preview__sub-heading "Muokattava osuus"]
           [:textarea.virkailija-email-preview__text-input
            {:value     (get-in contents [lang-kw :content])
             :on-change #(dispatch [:editor/update-email-preview lang-kw (.-value (.-target %))])}]
           [:div.virkailija-email-preview__preview-container
            [:h4.virkailija-email-preview__sub-heading "Esikatselu"]
            [:div.virkailija-email-preview__preview-header
             "Otsikko:"
             [:span.virkailija-email-preview__preview-header-value
              (get-in contents [lang-kw :subject])]]
            [:iframe.virkailija-email-preview__preview-iframe
             {:srcDoc               (get-in contents [lang-kw :body])
              :component-did-update (fn [e]
                                      (let [node   (reagent/dom-node e)
                                            height (-> node .-contentWindow .-document .-body .-scrollHeight)]
                                        (set! (-> node .-style .-height) (str height "px"))))}]
            [:div.virkailija-email-preview__buttons
             [:button.virkailija-email-preview__buttons-save.editor-form__control-button.editor-form__control-button--disabled
              {:on-click #(dispatch [:editor/save-email-template])}
              "Tallenna muutokset"]]]]]]]])))