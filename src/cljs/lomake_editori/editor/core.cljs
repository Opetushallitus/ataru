(ns lomake-editori.editor.core
  (:require [lomake-editori.dev.lomake :as l]
            [re-frame.core :refer [subscribe dispatch dispatch-sync register-handler register-sub]]
            [reagent.ratom :refer-macros [reaction]]
            [reagent.core :as r]
            [cljs.core.match :refer-macros [match]]
            [cljs-uuid-utils.core :as uuid]
            [lomake-editori.soresu.component :as component]
            [taoensso.timbre :refer-macros [spy debug]]))

(defonce soresu-id-uuid-separator "-__-")

(register-sub
  :editor/selected-form-values
  (fn [db [_ id] [form]]
    (reaction (-> form :values (get id)))))

(register-sub
  :editor/languages
  (fn [db]
    (reaction [:fi :sv])))

(defn soresu->reagent [{:keys [id fieldClass fieldType children text label] :as soresu-component}]
  (let [selected-languages     (subscribe [:editor/languages])
        selected-form          (subscribe [:editor/selected-form])
        stored-values          (subscribe [:editor/selected-form-values id] [selected-form])
        soresu-component-class (match [fieldClass]
                                      ["infoElement"] component/info-element
                                      :else component/form-component)]
    (fn [{:keys [id fieldClass fieldType children text label] :as soresu-component}]
      (if (some? children)
        [:section.child
         (soresu->reagent children)]
        (into [:section.component]
              (for [language @selected-languages]
                [soresu-component-class
                 (merge soresu-component
                        {:id           (str id soresu-id-uuid-separator (uuid/uuid-string (uuid/make-random-uuid)))
                         :controller   (clj->js {:getCustomComponentTypeMapping (fn [] #js [])
                                                 :componentDidMount             (fn [field value]
                                                                                  (debug field value))
                                                 :createCustomComponent         (fn [props])})
                         :translations #js {} ; required or soresu does nothing
                         :field        soresu-component
                         :lang         language

                                       ; formField has :value
                         :value        (get @stored-values language)

                                       ; info-element has :values
                         :values       (get @stored-values language)})]))))))

(defn editor []
  (let [form    (subscribe [:editor/selected-form])
        content (reaction (spy (:content @form)))]
    (fn []
      [:section.form
       (into [:form]
             (for [soresu-component @content]
               [soresu->reagent soresu-component]))])))

