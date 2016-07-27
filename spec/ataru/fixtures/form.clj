(ns ataru.fixtures.form)

(def form-with-content
  {:name        "Test fixture!"
   :modified-by "DEVELOPER"
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

(def person-info-form {:id 15
                       :name "valid form"
                       :modified_by "DEVELOPER"
                       :content [{:id "a41f4c1a-4eb6-4b05-8c95-c5a2ef59a9a3",
                                  :label {:fi "Henkilötiedot", :sv "Personlig information"},
                                  :module "person-info",
                                  :params {},
                                  :children [{:id "ac1eb1e9-bdf2-4c32-9ed4-089771f56486",
                                              :params {},
                                              :children [{:id "first-name",
                                                          :label {:fi "Etunimet", :sv "Förnamn"},
                                                          :params {:size "M"},
                                                          :fieldType "textField",
                                                          :fieldClass "formField",
                                                          :validators ["required"]}
                                                         {:id "preferred-name",
                                                          :label {:fi "Kutsumanimi", :sv "Smeknamn"},
                                                          :params {:size "S"},
                                                          :fieldType "textField",
                                                          :fieldClass "formField",
                                                          :validators ["required"]}],
                                              :fieldType "rowcontainer",
                                              :fieldClass "wrapperElement"}
                                             {:id "last-name",
                                              :label {:fi "Sukunimi", :sv "Efternamn"},
                                              :params {:size "M"},
                                              :fieldType "textField",
                                              :fieldClass "formField",
                                              :validators ["required"]}
                                             {:id "f21a741e-286e-4b7e-b477-79ee5bfc8373",
                                              :params {},
                                              :children [{:id "nationality",
                                                          :label {:fi "Kansalaisuus", :sv "Nationalitet"},
                                                          :params {},
                                                          :options [{:label {:fi "Suomi", :sv "Finland"}, :value "fi"}
                                                                    {:label {:fi "Ruotsi", :sv "Sverige"}, :value "sv"}],
                                                          :fieldType "dropdown",
                                                          :fieldClass "formField",
                                                          :validators ["required"]}
                                                         {:id "ssn",
                                                          :label {:fi "Henkilötunnus", :sv "Personnummer"},
                                                          :params {:size "S"},
                                                          :fieldType "textField",
                                                          :fieldClass "formField",
                                                          :validators ["ssn" "required"]}],
                                              :fieldType "rowcontainer",
                                              :fieldClass "wrapperElement"}
                                             {:id "gender",
                                              :label {:fi "Sukupuoli", :sv "Kön"},
                                              :params {},
                                              :options [{:label {:fi "Mies", :sv "Människa"}, :value "male"}
                                                        {:label {:fi "Nainen", :sv "Kvinna"}, :value "female"}],
                                              :fieldType "dropdown",
                                              :fieldClass "formField",
                                              :validators ["required"]}
                                             {:id "email",
                                              :label {:fi "Sähköpostiosoite", :sv "E-postadress"},
                                              :params {:size "M"},
                                              :fieldType "textField",
                                              :fieldClass "formField",
                                              :validators ["email" "required"]}
                                             {:id "phone",
                                              :label {:fi "Matkapuhelin", :sv "Mobiltelefonnummer"},
                                              :params {:size "M"},
                                              :fieldType "textField",
                                              :fieldClass "formField",
                                              :validators ["phone" "required"]}
                                             {:id "address",
                                              :label {:fi "Katuosoite", :sv "Adress"},
                                              :params {:size "L"},
                                              :fieldType "textField",
                                              :fieldClass "formField",
                                              :validators ["required"]}
                                             {:id "bfe2c8d2-e2cc-4948-9e1c-cd669074c4b1",
                                              :params {},
                                              :children [{:id "municipality",
                                                          :label {:fi "Kotikunta", :sv "Bostadsort"},
                                                          :params {:size "M"},
                                                          :fieldType "textField",
                                                          :fieldClass "formField",
                                                          :validators ["required"]}
                                                         {:id "postal-code",
                                                          :label {:fi "Postinumero", :sv "Postnummer"},
                                                          :params {:size "S"},
                                                          :fieldType "textField",
                                                          :fieldClass "formField",
                                                          :validators ["postal-code" "required"]}],
                                              :fieldType "rowcontainer",
                                              :fieldClass "wrapperElement"}
                                             {:id "language",
                                              :label {:fi "Äidinkieli", :sv "Modersmål"},
                                              :params {},
                                              :options [{:label {:fi "suomi", :sv "finska"}, :value "fi"}
                                                        {:label {:fi "ruotsi", :sv "svenska"}, :value "sv"}],
                                              :fieldType "dropdown",
                                              :fieldClass "formField",
                                              :validators ["required"]}],
                                  :fieldType "fieldset",
                                  :fieldClass "wrapperElement"}]})
