(ns ataru.fixtures.form)

(def form-with-content
  {:name        {:fi "Test fixture!"}
   :created-by "DEVELOPER"
   :content
                [{:fieldClass "formField"
                  :label      {:fi "tekstiä" :sv ""}
                  :id         "G__19"
                  :fieldType  "textField"}
                 {:fieldClass "wrapperElement"
                  :fieldType  "fieldset"
                  :id         "G__31"
                  :label      {:fi "Osion nimi" :sv "Avsnitt namn"}
                  :children
                              [{:fieldClass "formField"
                                :label      {:fi "" :sv ""}
                                :id         "G__32"
                                :fieldType  "textField"}]}]})

(def person-info-form {:id 2147483647, ;; shouldn't clash with serial sequence id. Tests also create forms which use serial id, and the previous id 15 caused serious issues.
                       :name {:fi "Uusi lomake"},
                       :created-by "DEVELOPER",
                       :organization-oid "1.2.246.562.10.2.45",
                       :created-time "2016-07-28T09:58:34.217+03:00",
                       :content [{:fieldClass "wrapperElement",
                                  :id "a41f4c1a-4eb6-4b05-8c95-c5a2ef59a9a3",
                                  :fieldType "fieldset",
                                  :children [{:fieldClass "wrapperElement",
                                              :id "ac1eb1e9-bdf2-4c32-9ed4-089771f56486",
                                              :fieldType "rowcontainer",
                                              :children [{:label {:fi "Etunimet", :sv "Förnamn"},
                                                          :validators ["required"],
                                                          :fieldClass "formField",
                                                          :id "first-name",
                                                          :params {:size "M"},
                                                          :fieldType "textField"}
                                                         {:label {:fi "Kutsumanimi", :sv "Smeknamn"},
                                                          :validators ["required"],
                                                          :fieldClass "formField",
                                                          :id "preferred-name",
                                                          :params {:size "S"},
                                                          :fieldType "textField"}],
                                              :params {}}
                                             {:label {:fi "Sukunimi", :sv "Efternamn"},
                                              :validators ["required"],
                                              :fieldClass "formField",
                                              :id "last-name",
                                              :params {:size "M"},
                                              :fieldType "textField"}
                                             {:fieldClass "wrapperElement",
                                              :id "f21a741e-286e-4b7e-b477-79ee5bfc8373",
                                              :fieldType "rowcontainer",
                                              :children [{:label {:fi "Kansalaisuus", :sv "Nationalitet"},
                                                          :validators ["required"],
                                                          :fieldClass "formField",
                                                          :id "nationality",
                                                          :params {},
                                                          :options [{:value "", :label {:fi "", :sv ""}}
                                                                    {:value "fi", :label {:fi "Suomi", :sv "Finland"}}
                                                                    {:value "sv", :label {:fi "Ruotsi", :sv "Sverige"}}],
                                                          :fieldType "dropdown"}
                                                         {:label {:fi "Henkilötunnus", :sv "Personnummer"},
                                                          :validators ["ssn" "required"],
                                                          :fieldClass "formField",
                                                          :id "ssn",
                                                          :params {:size "S"},
                                                          :fieldType "textField"}],
                                              :params {}}
                                             {:label {:fi "Sukupuoli", :sv "Kön"},
                                              :validators ["required"],
                                              :fieldClass "formField",
                                              :id "gender",
                                              :params {},
                                              :options [{:value "", :label {:fi "", :sv ""}}
                                                        {:value "male", :label {:fi "Mies", :sv "Människa"}}
                                                        {:value "female", :label {:fi "Nainen", :sv "Kvinna"}}],
                                              :fieldType "dropdown"}
                                             {:label {:fi "Sähköpostiosoite", :sv "E-postadress"},
                                              :validators ["email" "required"],
                                              :fieldClass "formField",
                                              :id "email",
                                              :params {:size "M"},
                                              :fieldType "textField"}
                                             {:label {:fi "Matkapuhelin", :sv "Mobiltelefonnummer"},
                                              :validators ["phone" "required"],
                                              :fieldClass "formField",
                                              :id "phone",
                                              :params {:size "M"},
                                              :fieldType "textField"}
                                             {:label {:fi "Asuinmaa", :sv "Boningsland"},
                                              :validators ["required"],
                                              :fieldClass "formField",
                                              :id "country-of-residence",
                                              :params {:size "M"},
                                              :fieldType "textField"}
                                             {:label {:fi "Katuosoite", :sv "Adress"},
                                              :validators ["required"],
                                              :fieldClass "formField",
                                              :id "address",
                                              :params {:size "L"},
                                              :fieldType "textField"}
                                             {:fieldClass "wrapperElement",
                                              :id "bfe2c8d2-e2cc-4948-9e1c-cd669074c4b1",
                                              :fieldType "rowcontainer",
                                              :children [{:label {:fi "Kotikunta", :sv "Bostadsort"},
                                                          :validators ["required"],
                                                          :fieldClass "formField",
                                                          :id "home-town",
                                                          :params {:size "M"},
                                                          :fieldType "textField"}
                                                         {:label {:fi "Postinumero", :sv "Postnummer"},
                                                          :validators ["postal-code" "required"],
                                                          :fieldClass "formField",
                                                          :id "postal-code",
                                                          :params {:size "S"},
                                                          :fieldType "textField"}],
                                              :params {}}
                                             {:label {:fi "Äidinkieli", :sv "Modersmål"},
                                              :validators ["required"],
                                              :fieldClass "formField",
                                              :id "language",
                                              :params {},
                                              :options [{:value "", :label {:fi "", :sv ""}}
                                                        {:value "fi", :label {:fi "suomi", :sv "finska"}}
                                                        {:value "sv", :label {:fi "ruotsi", :sv "svenska"}}],
                                              :fieldType "dropdown"}],
                                  :params {},
                                  :label {:fi "Henkilötiedot", :sv "Personlig information"},
                                  :module "person-info"}
                                 {:fieldClass "wrapperElement",
                                  :fieldType "fieldset",
                                  :id "6a3bd67e-a4ec-436e-9d70-c107df28932b",
                                  :label {:fi "Toinen osio", :sv "Avsnitt namn"},
                                  :children [{:fieldClass "formField",
                                              :fieldType "textField",
                                              :label {:fi "Tekstikysymys", :sv ""},
                                              :id "b0839467-a6e8-4294-b5cc-830756bbda8a",
                                              :params {},
                                              :validators ["required"]}],
                                  :params {}}
                                 {:label {:fi "Eka liite"
                                          :sv ""}
                                  :fieldClass "formField"
                                  :id "164954b5-7b23-4774-bd44-dee14071316b"
                                  :params {}
                                  :options []
                                  :fieldType "attachment"}]})

(def more-questions
  [{:fieldClass "formField"
    :fieldType  "attachment"
    :id         "more-questions-attachment-id"
    :label      {:fi "Eka liite" :sv ""}
    :validators ["required"]}
   {:fieldClass "formField"
    :fieldType  "dropdown"
    :id         "more-answers-dropdown-id"
    :label      {:fi "droparii" :sv ""}
    :options    [{:label     {:fi "eka vaihtoehto" :sv ""}
                  :value     "eka vaihtoehto"
                  :followups [{:fieldClass "formField"
                               :fieldType  "attachment"
                               :id         "dropdown-followup-1"
                               :label      {:fi "Dropdown liite" :sv ""}}
                              {:fieldClass "formField",
                               :fieldType  "singleChoice",
                               :id         "dropdown-followup-2",
                               :label      {:fi "Dropdown painikkeet required" :sv ""}
                               :validators ["required"]
                               :options    [{:label {:fi "eka" :sv ""}
                                             :value "eka"}
                                            {:label {:fi "toka" :sv ""}
                                             :value "toka"}]}]}
                 {:label {:fi "toka vaihtoehto" :sv ""}
                  :value "toka vaihtoehto"}]}
   {:fieldClass "wrapperElement"
    :fieldType  "adjacentfieldset"
    :id         "adjacent-quesiton-id"
    :label      {:fi "vierekkäiset kentät"}
    :children   [{:fieldClass "formField"
                  :fieldType  "textField"
                  :id         "adjacent-answer-1"
                  :label      {:fi "Vierekkäinen Kenttä1" :sv ""}
                  :params     {:adjacent true}
                  :validators ["required"]}
                 {:fieldClass "formField"
                  :fieldType  "textField"
                  :id "adjacent-answer-2"
                  :label      {:fi "Vierekkäinen Kenttä2" :sv ""}
                  :params     {:adjacent true}}]}
   {:fieldClass "formField"
    :fieldType "textField"
    :id "repeatable-required"
    :label {:fi "Toistuva pakollinen" :sv ""}
    :params {:repeatable true}
    :validators ["required"]}])

(def person-info-form-with-more-questions
  (update person-info-form :content concat more-questions))
