(ns lomake-editori.editor.component
  (:require [re-frame.core :refer [subscribe dispatch]]))

(defn language [lang]
  (fn [lang]
    [:div.language
     [:div (clojure.string/upper-case (name lang))]]))

(defn form-field [path]
  (let [languages (subscribe [:editor/languages])
        value     (subscribe [:editor/get-component-value path])]
    (fn [path]
      (into [:div.form-field
             [:p "Kentän nimi"]]
        (for [lang @languages]
          [:div
           [:input {:value     (get-in @value [:label lang])
                    :on-change #(dispatch [:editor/set-component-value (-> % .-target .-value) path :label lang])}]
           [language lang]
           #_[:p "Aputeksti"]
           #_[:input {:value     (get-in @value [:helpText lang])
                      :on-change #(dispatch [:editor/set-component-value (-> % .-target .-value) path :helpText lang])}]
           #_[language lang]])))))
