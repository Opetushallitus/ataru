(ns ataru.virkailija.editor.components.toolbar
  (:require
   [ataru.virkailija.component-data.component :as component]
   [ataru.feature-config :as fc]
   [re-frame.core :refer [dispatch]]
   [taoensso.timbre :refer-macros [spy debug]]))

(def ^:private toolbar-elements
  (cond->
    [["Lomakeosio" component/form-section]
     ["Pudotusvalikko" component/dropdown]
     ["Painikkeet, yksi valittavissa" component/single-choice-button]
     ["Lista, monta valittavissa" component/multiple-choice]
     ["Tekstikenttä" component/text-field]
     ["Tekstialue" component/text-area]
     ["Vierekkäiset tekstikentät" component/adjacent-fieldset]
     ["Kysymysryhmä" component/question-group]]
    (fc/feature-enabled? :attachment) (conj ["Liitepyyntö" component/attachment])
    true (conj ["Infoteksti" component/info-element])))

(def followup-toolbar-element-names #{"Tekstikenttä"
                                      "Tekstialue"
                                      "Pudotusvalikko"
                                      "Painikkeet, yksi valittavissa"
                                      "Lista, monta valittavissa"
                                      "Infoteksti"
                                      "Liitepyyntö"
                                      "Vierekkäiset tekstikentät"})

(def ^:private followup-toolbar-elements
  (filter
    (fn [[el-name _]] (contains? followup-toolbar-element-names el-name))
    toolbar-elements))

(def ^:private adjacent-fieldset-toolbar-elements
  {"Tekstikenttä" (comp (fn [text-field] (assoc text-field :params {:adjacent true}))
                    component/text-field)})

(defn- component-toolbar [path elements generator]
  (into [:ul.form__add-component-toolbar--list]
    (for [[component-name generate-fn] elements
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
