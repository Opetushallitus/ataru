(ns ataru.virkailija.views.template-editor
  (:require [ataru.cljs-util :refer [wrap-scroll-to get-virkailija-translation]]
            [ataru.translations.texts :refer [email-default-texts virkailija-texts]]
            [ataru.virkailija.views.modal :as modal]
            [goog.string :as s]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [reagent.core :as reagent]))

(def ^:private language-names
  {:fi (:finnish virkailija-texts)
   :sv (:swedish virkailija-texts)
   :en (:english virkailija-texts)})

(defn- get-body-class-list
  []
  (-> js/document
      (.-body)
      (.-classList)))

(defn- render-template-editor []
  (let [tab-lang (r/atom :fi)]
    (fn []
      (let [content          @(subscribe [:editor/email-template])
            contents-changed @(subscribe [:editor/email-templates-altered])
            any-changed?     (some true? (vals contents-changed))
            any-errors?      (some true? (map #(clojure.string/blank? (:subject %)) (vals content)))
            virkailija-lang  (subscribe [:editor/virkailija-lang])
            lang-content     (get-in content [(name @tab-lang)])]
        [modal/modal
         #(dispatch [:editor/toggle-email-template-editor])
         [:div.virkailija-email-preview
          [:h3.virkailija-email-preview__heading (get-virkailija-translation :email-content)]
          [:div.virkailija-email-preview__info-text
           (s/format "%s '%s'"
                     (get-virkailija-translation :applicant-will-receive-following-email)
                     (get lang-content :from))]
          (when (not (nil? content))
            [:div.virkailija-email-preview__tabs
             [:div.virkailija-email-preview__tab-panel
              (doall
               (map
                (fn [button-lang]
                  (list
                   [:input.virkailija-email-preview__tab
                    {:key       (str "email-preview-lang-radio-" (name @tab-lang))
                     :type      "radio"
                     :name      "lang"
                     :value     (name button-lang)
                     :id        (str "email-template-language-selection-" (name button-lang))
                     :checked   (= button-lang @tab-lang)
                     :on-change (fn [c]
                                  (reset! tab-lang button-lang))}]
                   [:label.virkailija-email-preview__tab-label
                    {:key   (str "email-preview-lang-radio-label-" (name @tab-lang))
                     :for   (str "email-template-language-selection-" (name button-lang))
                     :class (when (= button-lang @tab-lang) "virkailija-email-preview__tab-label--selected")}
                    (get-in language-names [button-lang @virkailija-lang])
                    (when (get contents-changed (name button-lang))
                      [:span.virkailija-email-preview__tab-edited "*"])]))
                [:fi :sv :en]))]
             [:div.virkailija-email-preview__tab-border]
             [:div.virkailija-email-preview__tab-content
              [:h4.virkailija-email-preview__sub-heading (get-virkailija-translation :editable-content-title)]
              [:input.virkailija-email-preview__input
               {:value     (:subject lang-content)
                :class     (when (clojure.string/blank? (:subject lang-content)) "virkailija-email-preview__input-field-error")
                :on-change #(dispatch [:editor/update-email-preview (name @tab-lang) :subject (.-value (.-target %))])}]
              [:h4.virkailija-email-preview__sub-heading (get-virkailija-translation :editable-content-beginning)]
              [:textarea.virkailija-email-preview__text-input
               {:value     (:content lang-content)
                :on-change #(dispatch [:editor/update-email-preview (name @tab-lang) :content (.-value (.-target %))])}]
              [:h4.virkailija-email-preview__sub-heading (get-virkailija-translation  :application-oid-here)]
              [:h4.virkailija-email-preview__sub-heading (get-virkailija-translation :editable-content-ending)]
              [:textarea.virkailija-email-preview__text-input
               {:value     (:content-ending lang-content)
                :on-change #(dispatch [:editor/update-email-preview (name @tab-lang) :content-ending (.-value (.-target %))])}]
              [:div.virkailija-email-preview__preview-container
               [:h4.virkailija-email-preview__sub-heading (get-virkailija-translation :message-preview)]
               [:iframe.virkailija-email-preview__preview-iframe
                {:srcDoc (:body lang-content)}]
               [:div.virkailija-email-preview__buttons
                [:button.virkailija-email-preview__buttons-save.editor-form__control-button
                 {:class    (if (and any-changed? (not any-errors?))
                              "editor-form__control-button--enabled"
                              "editor-form__control-button--disabled")
                  :on-click #(when (and any-changed? (not any-errors?)) (dispatch [:editor/save-email-template]))}
                 (str (get-virkailija-translation :save-changes)
                      (when any-changed?
                        (str
                         " ("
                         (->> contents-changed
                              (filter second)
                              (map first)
                              (map name)
                              (clojure.string/join ", ")
                              (clojure.string/upper-case))
                         ")")))]]]]])]]))))

(defn email-template-editor []
  (reagent/create-class
    {:component-did-mount    #(.add (get-body-class-list) "virkailija-modal-enabled")
     :component-will-unmount #(.remove (get-body-class-list) "virkailija-modal-enabled")
     :reagent-render         render-template-editor}))
