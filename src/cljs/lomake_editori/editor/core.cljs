(ns lomake-editori.editor.core
  (:require [lomake-editori.dev.lomake :as l]
            [re-frame.core :refer [subscribe dispatch dispatch-sync register-handler register-sub]]
            [reagent.ratom :refer-macros [reaction]]
            [reagent.core :as r]
            [cljs.core.match :refer-macros [match]]
            [cljs-uuid-utils.core :as uuid]
            [lomake-editori.soresu.component :as component]
            [taoensso.timbre :refer-macros [spy debug error]]))

(register-sub
  :editor/get-component-value
  (fn [db [_ & path]]
    (reaction (get-in @db
                      (flatten (concat
                                 [:editor :forms (-> @db :editor :selected-form :id) :content]
                                 path))))))

(register-handler
  :editor/set-component-value
  (fn [db [_ value & path]]
    (assoc-in db
              (spy (flatten (concat [:editor :forms (-> db :editor :selected-form :id) :content]
                                    path)))
              value)))

(register-sub
  :editor/languages
  (fn [db]
    (reaction [:fi :sv])))

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
           [:textarea {:on-change   #(dispatch [:editor/set-component-value (-> % .-target .-value) path :text lang])
                       :value       (get-in @value [:text lang])
                       :placeholder "Otsikko"}]])))))

(defn info [{:keys [params] :as content} path]
  (let [languages (subscribe [:editor/languages])
        value     (subscribe [:editor/get-component-value path :text])]
    (fn [{:keys [params] :as content} path]
      (into
        [:div.info
         [:p "Ohjeteksti"]]
        (for [lang @languages]
          [:div
           [:textarea
            {:value       (get @value lang)
             :on-change   #(dispatch [:editor/set-component-value (-> % .-target .-value) path :text lang])
             :placeholder "Ohjetekstin sisältö"}]])))))

(defn add-component []
  (fn []
    [:div
     [:hr]
     [:span "(+) add component"]]))

(defn soresu->reagent [{:keys [children] :as content} path]
  (fn [{:keys [children] :as content} path]
    [:section.component
     (match [content]
            [{:fieldClass "wrapperElement"
              :params     {:name n}
              :children   children}]
            (into [:section.wrapper
                   [:h1 n]]
                  (for [[index child] (zipmap (range) children)]
                    [soresu->reagent child (conj path :children index)]))

            [{:fieldClass "infoElement"
              :fieldType  "link"}]
            [link-info content path]

            [{:fieldClass "infoElement"}]
            [info content path]

            :else (do
                    (error content)
                    (throw "error" content)))
     [add-component]]))

(defn editor []
  (let [form    (subscribe [:editor/selected-form])
        content (reaction (take 2 (:content @form)))]
    (fn []
      [:section.form
       (into [:form]
             (for [[index json-blob] (zipmap (range) @content)
                   :when             (not-empty @content)]
               [soresu->reagent json-blob [index]]))])))

