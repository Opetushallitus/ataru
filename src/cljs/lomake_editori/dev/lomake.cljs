(ns lomake-editori.dev.lomake)

(def translations {:translations #js {}})

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

(def placeholder-content
  {:content
   [{:fieldClass "infoElement"
     :id         (str (gensym))
     :fieldType  "p"
     :params     {:preview false}
     :text       {:fi "Hakemusta on mahdollista muokata hakuajan loppuun asti. Hakemukset käsitellään hakuajan jälkeen."
                  :sv "Ansökan kan bearbetas ända till ansökningstidens slut. Ansökningarna behandlas efter ansökningstiden."}}

    {:fieldClass "infoElement"
     :id         (str (gensym))
     :fieldType  "link"
     :params     {:href {:fi "http://oph.fi/download/173879_ohje_othk_2016_hakijoille.pdf"
                         :sv "http://oph.fi/download/173895_anvisingar_for_ansokan_personalfortbildning_2016.pdf"}}
     :text       {:fi "Lataa ohje hakijoille."
                  :sv "Ladda ner anvisningarna för ansökan "}}

    {:fieldClass "wrapperElement",
     :id         "applicant-fieldset",
     :fieldType  "fieldset",
     :children
     [{:params     {:size "large", :maxlength 80},
       :fieldClass "formField",
       :helpText
       {:fi "Yhteyshenkilöllä tarkoitetaan hankkeen vastuuhenkilöä.",
        :sv "Med kontaktperson avses den projektansvariga i sökandeorganisationen."},
       :label      {:fi "Yhteyshenkilö", :sv "Kontaktperson"},
       :id         "applicant-name",
       :required   true,
       :fieldType  "textField"}
      {:fieldClass "wrapperElement",
       :id         "postal-stuff-2",
       :fieldType  "fieldset",
       :children
       [{:params     {:size "large", :maxlength 100},
         :fieldClass "formField",
         :helpText   {:fi "", :sv ""},
         :label      {:fi "Postiosoite", :sv "Postadress"},
         :id         "textField-2",
         :required   true,
         :fieldType  "textField"}]}
      {:fieldClass "wrapperElement",
       :id         "postal-stuff",
       :fieldType  "fieldset",
       :children
       [{:params     {:size "extra-small", :maxlength 8},
         :fieldClass "formField",
         :helpText   {:fi "", :sv ""},
         :label      {:fi "Postinumero", :sv "Postnummer"},
         :id         "textField-3",
         :required   true,
         :fieldType  "textField"}
        {:params     {:size "small", :maxlength 100},
         :fieldClass "formField",
         :helpText   {:fi "", :sv ""},
         :label      {:fi "Postitoimipaikka", :sv "Postanstalt"},
         :id         "textField-4",
         :required   true,
         :fieldType  "textField"}]}
      {:params     {},
       :fieldClass "formField",
       :helpText   {:fi "", :sv ""},
       :label      {:fi "Omistajatyyppi", :sv "Ägartyp"},
       :id         "radioButton-0",
       :options
       [{:value "kunta-kuntayhtymae", :label {:fi "Kunta/Kuntayhtymä", :sv "Kommun/Samkommun"}}
        {:value "rekisteroeity-yhteisoe-tai-saeaetioe",
         :label {:fi "rekisteröity yhteisö tai säätiö", :sv "registrerad sammanslutning eller stiftelse"}}
        {:value "yliopisto", :label {:fi "Yliopisto", :sv "Universitet"}}],
       :required   true,
       :fieldType  "radioButton"}
      {:params     {:size "small", :maxlength 100},
       :fieldClass "formField",
       :helpText   {:fi "", :sv ""},
       :label      {:fi "Yritysmuoto", :sv "Företagsform"},
       :id         "textField-0",
       :required   true,
       :fieldType  "textField"}]}]})
