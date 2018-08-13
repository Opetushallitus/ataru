(ns ataru.virkailija.editor.components.toolbar
  (:require [ataru.cljs-util :as util :refer [get-virkailija-translation]]
            [ataru.component-data.component :as component]
            [ataru.component-data.base-education-module :as base-education-module]
            [ataru.component-data.higher-education-base-education-module :as kk-base-education-module]
            [ataru.feature-config :as fc]
            [re-frame.core :refer [dispatch subscribe]]
            [taoensso.timbre :refer-macros [spy debug]]))

(def ^:private toolbar-elements
  [[:form-section component/form-section]
   [:dropdown component/dropdown]
   [:single-choice-button component/single-choice-button]
   [:multiple-choice component/multiple-choice]
   [:text-field component/text-field]
   [:text-area component/text-area]
   [:adjacent-fieldset component/adjacent-fieldset]
   [:attachment component/attachment]
   [:question-group component/question-group]
   [:info-element component/info-element]
   [:base-education-module base-education-module/module]
   [:kk-base-education-module kk-base-education-module/module]
   [:pohjakoulutusristiriita component/pohjakoulutusristiriita]
   [:lupa-sahkoiseen-asiointiin component/lupa-sahkoiseen-asiointiin]
   [:lupatiedot component/lupatiedot]])

(def followup-toolbar-element-names
  #{:text-field
    :text-area
    :dropdown
    :single-choice-button
    :multiple-choice
    :info-element
    :attachment
    :adjacent-fieldset
    :question-group})

(def question-group-toolbar-element-names
  #{:text-field
    :text-area
    :dropdown
    :single-choice-button
    :multiple-choice
    :info-element
    :attachment
    :adjacent-fieldset})

(def ^:private followup-toolbar-elements
  (filter
    (fn [[el-name _]] (contains? followup-toolbar-element-names el-name))
    toolbar-elements))

(def ^:private question-group-toolbar-elements
  (filter
    (fn [[el-name _]] (contains? question-group-toolbar-element-names el-name))
    toolbar-elements))

(def ^:private adjacent-fieldset-toolbar-elements
  {:text-field (comp (fn [text-field] (assoc text-field :params {:adjacent true}))
                    component/text-field)})

(defn- component-toolbar [path elements generator]
  (fn [path elements generator]
    (let [base-education-module-exists?   (subscribe [:editor/base-education-module-exists?])
          pohjakoulutusristiriita-exists? (subscribe [:editor/pohjakoulutusristiriita-exists?])]
      (into [:ul.form__add-component-toolbar--list]
            (for [[component-name generate-fn] elements
                  :when (and (not (and (vector? path)
                                       (= :children (second path))
                                       (= :form-section component-name)))
                             (not (and @base-education-module-exists?
                                       (contains? #{:base-education-module :kk-base-education-module} component-name)))
                             (not (and @pohjakoulutusristiriita-exists?
                                       (= :pohjakoulutusristiriita component-name))))]
              [:li.form__add-component-toolbar--list-item
               [:a {:on-click (fn [evt]
                                (.preventDefault evt)
                                (generator generate-fn))}
                (get-virkailija-translation component-name)]])))))


(defn add-component [path]
  (let [form-locked (subscribe [:editor/current-form-locked])]
    [:div.editor-form__add-component-toolbar
     {:class (when @form-locked "disabled")}
     (when-not @form-locked
       [component-toolbar path toolbar-elements
        (fn [generate-fn]
          (dispatch [:generate-component generate-fn path]))])
     [:div.plus-component
      {:class (when @form-locked "disabled")}
      [:span "+"]]]))

(defn custom-add-component [toolbar path generator]
  (let [form-locked (subscribe [:editor/current-form-locked])]
    [:div.editor-form__add-component-toolbar
     {:class (when @form-locked "disabled")}
     (when-not @form-locked
       [component-toolbar path toolbar generator])
     [:div.plus-component
      {:class (when @form-locked "disabled")}
      [:span "+"]]]))

(defn followup-toolbar [option-path generator]
  [custom-add-component followup-toolbar-elements option-path generator])

(defn question-group-toolbar [option-path generator]
  [custom-add-component question-group-toolbar-elements option-path generator])

(defn adjacent-fieldset-toolbar [path generator]
  [custom-add-component adjacent-fieldset-toolbar-elements path generator])
