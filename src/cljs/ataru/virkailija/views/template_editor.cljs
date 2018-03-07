(ns ataru.virkailija.views.template-editor
  (:require [re-frame.core :refer [subscribe dispatch]]))

(defn email-template-editor
  []
  (let [visible? @(subscribe [:state-query [:editor :ui :template-editor-visible?]])
        lang     @(subscribe [:state-query [:editor :email-template-lang]])
        lang-kw  (keyword lang)
        contents @(subscribe [:state-query [:editor :email-template]])]
    (when visible?
      [:div.virkailija-modal__container
       [:div.virkailija-modal__content
        [:a.virkailija-modal__close-link
         {:on-click #(dispatch [:editor/toggle-email-template-editor])}
         "Sulje"]
        [:div.virkailija-modal__panels
         [:div.virkailija-modal__editor-panel
          [:h3 "Sisältö"]
          (into
            [:div.virkailija-modal__language-selection]
            (map
              (fn [button-lang]
                [:div
                 [:input {:type      "radio"
                          :name      "lang"
                          :value     button-lang
                          :id        (str "email-template-language-selection-" button-lang)
                          :checked   (= button-lang lang)
                          :on-change #(dispatch [:editor/set-email-template-language button-lang])}]
                 [:label {:for (str "email-template-language-selection-" button-lang)}
                  (clojure.string/upper-case button-lang)]])
              ["fi" "sv" "en"]))
          [:textarea.virkailija-modal__editor
           {:value     (get-in contents [lang-kw :content])
            :on-change #(dispatch [:editor/update-email-preview lang-kw (.-value (.-target %))])}]]
         [:div.virkailija-modal__preview-panel
          [:h3 "Esikatselu"]
          [:div "Lähettäjä:" [:span (get-in contents [lang-kw :from])]]
          [:div "Otsikko:" [:span (get-in contents [lang-kw :subject])]]
          [:iframe.virkailija-modal__preview
           {:srcDoc (get-in contents [lang-kw :body])}]
          [:div.virkailija-modal__buttons-container
           [:button.virkailija-modal__button
            {:on-click #(dispatch [:editor/load-email-template])}
            "Peruuta"]
           [:button.virkailija-modal__button
            {:on-click #(dispatch [:editor/save-email-template])}
            "Talleta"]]]]]])))