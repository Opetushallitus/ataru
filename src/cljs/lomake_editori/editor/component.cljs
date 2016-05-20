(ns lomake-editori.editor.component
  (:require [lomake-editori.soresu.component :as component]
            [reagent.ratom :refer-macros [reaction]]
            [re-frame.core :refer [subscribe dispatch]]))

(defn language [lang]
  (fn [lang]
    [:div.language
     [:div (clojure.string/upper-case (name lang))]]))

(defn text-field [path]
  (let [languages (subscribe [:editor/languages])
        value     (subscribe [:editor/get-component-value path])]
    (fn [path]
      (-> [:div.editor-form__component-wrapper
           [:header.form__form-element--header "Tekstikenttä"]]
          (into
            [[:div.form__entity-container
              [:header.form__entity-header "Otsikko"]
              [:div.form__element-container
              (doall
                (for [lang @languages]
                  ^{:key lang}
                  [:input.form__input {:value     (get-in @value [:label lang])
                                       :on-change #(dispatch [:editor/set-component-value (-> % .-target .-value) path :label lang])}]))]]])
          (into
            [[:div.form__entity-container
              [:header.form__entity-header "Koko"]
              [:div.form__element-container
               [:span.form__size-button "S"]
               [:span.form__size-button "M"]
               [:span.form__size-button "L"]]]])
          (into
            (let [id (str (gensym))]
              [[:div.form__checkbox-group
                [:div.form__checkbox-container
                 [:input.form__checkbox {:type "checkbox"
                                         :id (str id "_mandatory_choice")}]
                 [:label.form__checkbox-label {:for (str id "_mandatory_choice")} "Pakollinen tieto"]]
                [:div.form__checkbox-container
                 [:input.form__checkbox {:type "checkbox"
                                         :id (str id "_multiple_choices")}]
                 [:label.form__checkbox-label {:for (str id "_mandatory_choice")} "Vastaaja voi lisätä useita vastauksia"]]]]))))))

(defn link-info [{:keys [params] :as content} path]
  (let [languages (subscribe [:editor/languages])
        value     (subscribe [:editor/get-component-value path])]
    (fn [{:keys [params] :as content} path]
      (into
        [:div.link-info
         [:p "Linkki"]]
        (for [lang @languages]
          [:div
           [:p "Osoite"]
           [:input {:value       (get-in @value [:params :href lang])
                    :type        "url"
                    :on-change   #(dispatch [:editor/set-component-value (-> % .-target .-value) path :params :href lang])
                    :placeholder "http://"}]
           [language lang]
           [:input {:on-change   #(dispatch [:editor/set-component-value (-> % .-target .-value) path :text lang])
                    :value       (get-in @value [:text lang])
                    :placeholder "Otsikko"}]
           [language lang]])))))

(defn info [{:keys [params] :as content} path]
  (let [languages (subscribe [:editor/languages])
        value     (subscribe [:editor/get-component-value path :text])]
    (fn [{:keys [params] :as content} path]
      (into
        [:div.info
         [:p "Ohjeteksti"]]
        (for [lang @languages]
          [:div
           [:input
            {:value       (get @value lang)
             :on-change   #(dispatch [:editor/set-component-value (-> % .-target .-value) path :text lang])
             :placeholder "Ohjetekstin sisältö"}]
           [language lang]
           ])))))

(defn ^:private delayed-trigger [timeout-ms on-trigger]
  (let [latch (atom nil)]
    [(fn [] (reset! latch nil))
     (fn [event]
       (let [local-latch (atom nil)
             handle      (js/setTimeout (fn []
                                          (when (= @latch @local-latch)
                                            (on-trigger event)))
                           timeout-ms)]
         (do
           (reset! local-latch handle)
           (reset! latch handle))))]))

(def ^:private toolbar-elements
  (let [dummy [:div "ei vielä toteutettu.."]]
    {"Lomakeosio"                component/form-section
     "Tekstikenttä"              component/text-field}))
;"Tekstialue"                dummy
;"Lista, monta valittavissa" dummy
;"Lista, yksi valittavissa"  dummy
;"Pudotusvalikko"            dummy
;"Vierekkäiset kentät"       dummy
;"Liitetiedosto"             dummy
;"Ohjeteksti"                info
;"Linkki"                    link-info
;"Väliotsikko"               dummy

(defn ^:private component-toolbar [path]
  (fn [path]
    (into [:ul]
      (for [[component-name generate-fn] toolbar-elements]
        [:li {:on-click #(dispatch [:generate-component generate-fn path])}
         component-name]))))

(defn add-component [path]
  (let [show-bar? (reaction nil)
        [toolbar-abort-trigger
         toolbar-delayed-trigger] (delayed-trigger 1000 #(reset! show-bar? false))
        [plus-abort-trigger plus-delayed-trigger] (delayed-trigger 333 #(reset! show-bar? true))]
    (fn [path]
      (if @show-bar?
        [:div.component-toolbar
         {:on-mouse-leave toolbar-delayed-trigger
          :on-mouse-enter toolbar-abort-trigger}
         [component-toolbar path]]
        [:div.form__add-component-toolbar
         {:on-mouse-enter plus-delayed-trigger
          :on-mouse-leave plus-abort-trigger}
         [:div.plus-component
          [:span "+"]]]))))
