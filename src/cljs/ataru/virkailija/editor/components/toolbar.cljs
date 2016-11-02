(ns ataru.virkailija.editor.components.toolbar
  (:require
   [ataru.virkailija.component-data.component :as component]
   [re-frame.core :as c :refer [dispatch]]
   [reagent.core :as r]))

(def ^:private toolbar-elements
  {"Lomakeosio"                component/form-section
   "Tekstikenttä"              component/text-field
   "Tekstialue"                component/text-area
   "Pudotusvalikko"            component/dropdown
   "Lista, monta valittavissa" component/multiple-choice})

(def ^:private followup-toolbar-elements
  (select-keys toolbar-elements
    ["Tekstikenttä" "Tekstialue" "Pudotusvalikko" "Lista, monta valittavissa"]))

(defn ^:private component-toolbar [path toolbar generator]
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

(defn followup-add-component [path generator]
  [:div.editor-form__add-component-toolbar
   [component-toolbar path followup-toolbar-elements generator]
   [:div.plus-component
    [:span "+"]]])
