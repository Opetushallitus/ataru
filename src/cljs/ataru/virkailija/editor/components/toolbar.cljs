(ns ataru.virkailija.editor.components.toolbar
  (:require
   [ataru.virkailija.component-data.component :as component]
   [re-frame.core :as c :refer [dispatch]]
   [reagent.core :as r]
   [taoensso.timbre :refer-macros [spy debug]]))

(def ^:private toolbar-elements
  {"Lomakeosio"                    component/form-section
   "Tekstikenttä"                  component/text-field
   "Tekstialue"                    component/text-area
   "Pudotusvalikko"                component/dropdown
   "Painikkeet, yksi valittavissa" component/single-choice-button
   "Lista, monta valittavissa"     component/multiple-choice
   "Infokenttä"                    component/info-element
   "Vierekkäiset tekstikentät"     component/adjacent-fieldset})

(def ^:private followup-toolbar-elements
  (select-keys toolbar-elements
    ["Tekstikenttä" "Tekstialue" "Pudotusvalikko" "Lista, monta valittavissa" "Infokenttä" "Painikkeet, yksi valittavissa"]))

(def ^:private adjacent-fieldset-toolbar-elements
  {"Tekstikenttä" (comp (fn [text-field] (assoc text-field :params {:adjacent true}))
                    component/text-field)})

(defn- component-toolbar [path toolbar generator]
  (into [:ul.form__add-component-toolbar--list]
    (for [[component-name generate-fn] toolbar
          :when                        (not (and
                                              (vector? path)
                                              (= :children (second path))
                                              (= "Lomakeosio" component-name)))]
      [:li.form__add-component-toolbar--list-item
       [:a {:on-click (fn [evt]
                        (.preventDefault evt)
                        (generator generate-fn))}
        component-name]])))

(defn add-component [path]
  [:div.editor-form__add-component-toolbar
   [component-toolbar
    path
    toolbar-elements
    (fn [generate-fn]
      (dispatch [:generate-component generate-fn path]))]
   [:div.plus-component
    [:span "+"]]])

(defn custom-add-component [toolbar path generator]
  [:div.editor-form__add-component-toolbar
   [component-toolbar path toolbar generator]
   [:div.plus-component
    [:span "+"]]])

(defn followup-toolbar [option-path generator]
  [custom-add-component followup-toolbar-elements option-path generator])

(defn adjacent-fieldset-toolbar [path generator]
  [custom-add-component adjacent-fieldset-toolbar-elements path generator])
