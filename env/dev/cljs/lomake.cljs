(ns dev.cljs.lomake)

(def translations {:translations  #js {}})

(def controller {:controller
                 (clj->js {:getCustomComponentTypeMapping (fn [] #js [])
                           :componentDidMount             (fn [field value])
                           :createCustomComponent         (fn [props])})})

(def text-field {:id         "test-text-field"
                 :fieldType  "textField"
                 :fieldClass "formField"
                 :label      {:fi "Suomi"
                              :sv "Ruotsi"
                              :en "Englanti"}})

(def lomake-1 {:form {:content [text-field]}})

(defn field [field]
  {:field field
   :fieldType (:fieldType field)})
