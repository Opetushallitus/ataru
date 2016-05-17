(ns lomake-editori.editor.core
  (:require [lomake-editori.dev.lomake :as l]
            [lomake-editori.editor.component :as ec]
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
              (flatten (concat [:editor :forms (-> db :editor :selected-form :id) :content]
                               path))
              value)))

(register-sub
  :editor/languages
  (fn [db]
    (reaction [:fi :sv])))

(def toolbar-elements
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

(defn component-toolbar [path]
  (fn [path]
    (into [:ul]
          (for [[component-name generate-fn] toolbar-elements]
            [:li {:on-click #(dispatch [:generate-component generate-fn path])}
             component-name]))))

(defn delayed-trigger [timeout-ms on-trigger]
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
        [:div.add-component
         {:on-mouse-enter plus-delayed-trigger
          :on-mouse-leave plus-abort-trigger}
         [:div.plus-component
          [:span "+"]]]))))

(defn soresu->reagent [{:keys [children] :as content} path]
  (fn [{:keys [children] :as content} path]
    [:section.component
     (match [content]
       [{:fieldClass "wrapperElement"
         :children   children}]
       (let [wrapper-element (->> (for [[index child] (zipmap (range) children)]
                                    [soresu->reagent child (conj path :children index)])
                                  (into [:section.wrapper (when-let [n (-> content :label)]
                                                            [:h1 n])]))]
         (conj wrapper-element [add-component (conj path :children (count (:children content)))]))

       [{:fieldClass "formField"}]
       [ec/text-field path]

       [{:fieldClass "infoElement"
         :fieldType  "link"}]
       [ec/link-info content path]

       [{:fieldClass "infoElement"}]
       [ec/info content path]

       :else (do
               (error content)
               (throw "error" content)))]))

(defn editor []
  (let [form    (subscribe [:editor/selected-form])
        content (reaction (:content @form))]
    (fn []
      [:section.form
       (conj
         (into [:form]
           (for [[index json-blob] (zipmap (range) @content)
                 :when             (not-empty @content)]
             [soresu->reagent json-blob [index]]))
         [add-component (count @content)])])))
