(ns ataru.virkailija.dev.lomake)

(def translations {:translations #js {}})

(def controller {:controller
                 (clj->js {:getCustomComponentTypeMapping (fn [] #js [])
                           :componentDidMount             (fn [_ _])
                           :createCustomComponent         (fn [_])})})

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

(def placeholder-content
  {:content
   [{:fieldClass "wrapperElement"
     :id         "applicant-fieldset"
     :children
     [{:fieldClass "formField"
       :helpText
                   {:fi "Yhteyshenkilöllä tarkoitetaan hankkeen vastuuhenkilöä."
                    :sv "Med kontaktperson avses den projektansvariga i sökandeorganisationen."}
       :label      {:fi "Sukunimi", :sv "Efternamn"}
       :id         "applicant-firstname"
       :validators ["required"]
       :fieldType  "textField"}
      {:fieldClass "formField"
       :label      {:fi "Etunimi", :sv "Förnamn"}
       :id         "applicant-surname"
       :validators ["required"]
       :fieldType  "textField"}]}]})
