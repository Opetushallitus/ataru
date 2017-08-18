(ns ataru.fixtures.db.test-form)

(def test-form
  "This form is generated by virkailija test. Update if necessary.
   The form is taken here so that the tests can be run individually.
   Separate file since it's quite large."
  {:id               7
   :key              "3e784de0-c7c4-4634-b4ee-024e8b0658b6"
   :name             "Testilomake"
   :created-by       "DEVELOPER"
   :organization-oid "1.2.246.562.10.0439845"
   :languages        ["fi"]
   :content          [{:id                             "hakukohteet"
                       :label                          {:en "" :fi "Hakukohteet" :sv ""}
                       :params                         {}
                       :options                        []
                       :fieldType                      "hakukohteet"
                       :fieldClass                     "formField"
                       :validators                     ["hakukohteet"]
                       :exclude-from-answers-if-hidden true}
                      {:id              "03d05a71-f726-452f-917e-f652c2478130"
                       :label           {:en "Personal information" :fi "Henkilötiedot" :sv "Personuppgifter"}
                       :module          "person-info"
                       :params          {}
                       :children        [{:id         "1e8916d8-b0c4-44dd-b7d3-3e6b0fce2631"
                                          :params     {}
                                          :children   [{:id         "first-name"
                                                        :label      {:en "Forenames" :fi "Etunimet" :sv "Förnamn"}
                                                        :rules      {}
                                                        :params     {:size "M"}
                                                        :fieldType  "textField"
                                                        :blur-rules {:prefill-preferred-first-name "main-first-name"}
                                                        :fieldClass "formField"
                                                        :validators ["required"]}
                                                       {:id         "preferred-name"
                                                        :label      {:en "Main forename" :fi "Kutsumanimi" :sv "Tilltalsnamn"}
                                                        :rules      {}
                                                        :params     {:size "S"}
                                                        :fieldType  "textField"
                                                        :fieldClass "formField"
                                                        :validators ["required" "main-first-name"]}]
                                          :fieldType  "rowcontainer"
                                          :fieldClass "wrapperElement"}
                                         {:id         "last-name"
                                          :label      {:en "Surname" :fi "Sukunimi" :sv "Efternamn"}
                                          :rules      {}
                                          :params     {:size "M"}
                                          :fieldType  "textField"
                                          :fieldClass "formField"
                                          :validators ["required"]}
                                         {:params          {}
                                          :koodisto-source {:uri "maatjavaltiot2" :version 1 :default-option "Suomi"}
                                          :rules           {:swap-ssn-birthdate-based-on-nationality ["ssn" "birth-date"]}
                                          :validators      ["required"]
                                          :fieldClass      "formField"
                                          :label           {:en "Nationality" :fi "Kansalaisuus" :sv "Medborgarskap"}
                                          :id              "nationality"
                                          :options         [{:label {:fi "" :sv ""} :value ""}]
                                          :fieldType       "dropdown"}
                                         {:params               {}
                                          :rules                {:toggle-ssn-based-fields "ssn"}
                                          :fieldClass           "formField"
                                          :label                {:en "Do you have a Finnish social security number?"
                                                                 :fi "Onko sinulla suomalainen henkilötunnus?"
                                                                 :sv "Har du en finländsk personbeteckning?"}
                                          :id                   "have-finnish-ssn"
                                          :exclude-from-answers true
                                          :no-blank-option      true
                                          :options              [{:label {:en "Yes" :fi "Kyllä" :sv "Ja"} :value "true" :default-value true}
                                                                 {:label {:en "No" :fi "Ei" :sv "Nej"} :value "false"}]
                                          :fieldType            "dropdown"}
                                         {:id              "0ebd7e1a-0d2a-450e-977b-6f9650f49907"
                                          :params          {}
                                          :children        [{:id         "ssn"
                                                             :label      {:en "Social security number" :fi "Henkilötunnus" :sv "Personbeteckning"}
                                                             :rules      {:update-gender-and-birth-date-based-on-ssn "gender"}
                                                             :params     {:size "S"}
                                                             :fieldType  "textField"
                                                             :fieldClass "formField"
                                                             :validators ["ssn" "required"]}
                                                            {:id         "73b3ea09-11d9-4b8a-9102-f655a24dd560"
                                                             :params     {}
                                                             :children   [{:id         "birth-date"
                                                                           :label      {:en "Date of birth" :fi "Syntymäaika" :sv "Födelsetid"}
                                                                           :rules      {}
                                                                           :params     {:size "S" :placeholder {:fi "pp.kk.vvvv"}}
                                                                           :fieldType  "textField"
                                                                           :fieldClass "formField"
                                                                           :validators ["past-date" "required"]}
                                                                          {:id              "gender"
                                                                           :label           {:en "Gender" :fi "Sukupuoli" :sv "Kön"}
                                                                           :params          {}
                                                                           :options         [{:label {:fi "" :sv ""} :value ""}]
                                                                           :fieldType       "dropdown"
                                                                           :fieldClass      "formField"
                                                                           :validators      ["required"]
                                                                           :koodisto-source {:uri "sukupuoli" :version 1}}]
                                                             :fieldType  "rowcontainer"
                                                             :fieldClass "wrapperElement"}]
                                          :fieldType       "rowcontainer"
                                          :fieldClass      "wrapperElement"
                                          :child-validator "birthdate-and-gender-component"}
                                         {:id         "birthplace"
                                          :label      {:en "Place and country of birth"
                                                       :fi "Syntymäpaikka ja -maa"
                                                       :sv "Födelseort och -land"}
                                          :rules      {}
                                          :params     {:size "M"}
                                          :fieldType  "textField"
                                          :fieldClass "formField"
                                          :validators ["birthplace"]}
                                         {:id         "passport-number"
                                          :label      {:en "Passport number" :fi "Passin numero" :sv "Passnummer"}
                                          :rules      {}
                                          :params     {:size "M"}
                                          :fieldType  "textField"
                                          :fieldClass "formField"
                                          :validators []}
                                         {:id         "national-id-number"
                                          :label      {:en "National ID number" :fi "Kansallinen ID-tunnus" :sv "Nationellt ID-signum"}
                                          :rules      {}
                                          :params     {:size "M"}
                                          :fieldType  "textField"
                                          :fieldClass "formField"
                                          :validators []}
                                         {:id         "email"
                                          :label      {:en "E-mail address" :fi "Sähköpostiosoite" :sv "E-postadress"}
                                          :rules      {}
                                          :params     {:size "M"}
                                          :fieldType  "textField"
                                          :fieldClass "formField"
                                          :validators ["required" "email"]}
                                         {:id         "phone"
                                          :label      {:en "Mobile phone number" :fi "Matkapuhelin" :sv "Mobiltelefonnummer"}
                                          :rules      {}
                                          :params     {:size "M"}
                                          :fieldType  "textField"
                                          :fieldClass "formField"
                                          :validators ["required" "phone"]}
                                         {:params          {}
                                          :koodisto-source {:uri "maatjavaltiot2" :version 1 :default-option "Suomi"}
                                          :rules           {:change-country-of-residence nil}
                                          :validators      ["required"]
                                          :fieldClass      "formField"
                                          :label           {:en "Country of residence" :fi "Asuinmaa" :sv "Boningsland"}
                                          :id              "country-of-residence"
                                          :options         [{:label {:fi "" :sv ""} :value ""}]
                                          :fieldType       "dropdown"}
                                         {:id         "address"
                                          :label      {:en "Address" :fi "Katuosoite" :sv "Näraddress"}
                                          :rules      {}
                                          :params     {:size "M"}
                                          :fieldType  "textField"
                                          :fieldClass "formField"
                                          :validators ["required"]}
                                         {:id         "d9340641-cbb3-40ae-8be5-152d48e2326e"
                                          :params     {}
                                          :children   [{:id         "postal-code"
                                                        :label      {:en "Postal code" :fi "Postinumero" :sv "Postnummer"}
                                                        :rules      {:select-postal-office-based-on-postal-code "postal-office"}
                                                        :params     {:size "S"}
                                                        :fieldType  "textField"
                                                        :fieldClass "formField"
                                                        :validators ["postal-code"]}
                                                       {:id                             "postal-office"
                                                        :label                          {:en "Postal office" :fi "Postitoimipaikka" :sv "Postkontor"}
                                                        :rules                          {}
                                                        :params                         {:size "M"}
                                                        :fieldType                      "textField"
                                                        :fieldClass                     "formField"
                                                        :validators                     ["postal-office"]
                                                        :exclude-from-answers-if-hidden true}]
                                          :fieldType  "rowcontainer"
                                          :fieldClass "wrapperElement"}
                                         {:id                             "home-town"
                                          :label                          {:en "Home town" :fi "Kotikunta" :sv "Hemkommun"}
                                          :rules                          {}
                                          :params                         {:size "M"}
                                          :fieldType                      "textField"
                                          :fieldClass                     "formField"
                                          :validators                     ["home-town"]
                                          :exclude-from-answers-if-hidden true}
                                         {:id                             "city"
                                          :label                          {:en "City" :fi "Kaupunki" :sv "Stad"}
                                          :rules                          {}
                                          :params                         {:size "M"}
                                          :fieldType                      "textField"
                                          :fieldClass                     "formField"
                                          :validators                     ["city"]
                                          :exclude-from-answers-if-hidden true}
                                         {:id              "language"
                                          :label           {:en "Native language" :fi "Äidinkieli" :sv "Modersmål"}
                                          :params          {}
                                          :options         [{:label {:fi "" :sv ""} :value ""}]
                                          :fieldType       "dropdown"
                                          :fieldClass      "formField"
                                          :validators      ["required"]
                                          :koodisto-source {:uri "kieli" :version 1 :default-option "suomi"}}]
                       :fieldType       "fieldset"
                       :fieldClass      "wrapperElement"
                       :label-amendment {:en "The section will be automatically added to the application"
                                         :fi "(Osio lisätään automaattisesti lomakkeelle)"
                                         :sv "Partitionen automatiskt lägga formen"}}
                      {:id         "b00638c6-8b47-4fb4-b790-c424ffb283b5"
                       :label      {:fi "Ensimmäinen kysymys" :sv ""}
                       :params     {:info-text {:label {:en "" :fi "Ensimmäisen kysymyksen ohjeteksti" :sv ""}}}
                       :fieldType  "textField"
                       :fieldClass "formField"}
                      {:id         "7762041c-51ba-4a61-9891-bb24cb2409ca"
                       :label      {:fi "Ensimmäinen kysymys toistuvilla arvoilla" :sv ""}
                       :params     {:repeatable true}
                       :fieldType  "textField"
                       :fieldClass "formField"}
                      {:id         "216bb100-97f7-47f9-a1f7-51763e99ff5f"
                       :label      {:fi "Toinen kysymys" :sv ""}
                       :params     {:size "L" :info-text {:label {:en "" :fi "Toisen kysymyksen ohjeteksti" :sv ""}}}
                       :fieldType  "textArea"
                       :fieldClass "formField"
                       :validators ["required"]}
                      {:id         "3bf5392b-7f27-4edc-a695-71d80deb8d70"
                       :label      {:fi "Kolmas kysymys" :sv ""}
                       :params     {:info-text {:label {:en "" :fi "Kolmannen kysymyksen ohjeteksti" :sv ""}}}
                       :options    [{:label {:fi "Ensimmäinen vaihtoehto" :sv ""} :value "Ensimmäinen vaihtoehto"}
                                    {:label {:fi "Toinen vaihtoehto" :sv ""} :value "Toinen vaihtoehto"}
                                    {:label     {:fi "Kolmas vaihtoehto" :sv ""}
                                     :value     "Kolmas vaihtoehto"
                                     :followups [{:id         "b9f0a526-38ca-4560-aee4-aee03c6ca4e4"
                                                  :label      {:fi "Jatkokysymys" :sv ""}
                                                  :params     {}
                                                  :fieldType  "textField"
                                                  :fieldClass "formField"}]}
                                    {:label {:fi "" :sv ""} :value ""}]
                       :fieldType  "dropdown"
                       :fieldClass "formField"}
                      {:id              "6bac2c85-7868-4697-8d42-bb8b9c85b8ef"
                       :label           {:fi "Neljäs kysymys" :sv ""}
                       :params          {}
                       :options         [{:label {:fi "" :sv ""} :value ""}]
                       :fieldType       "dropdown"
                       :fieldClass      "formField"
                       :koodisto-source {:uri "pohjakoulutuseditori" :title "Pohjakoulutus" :version 1}}
                      {:id         "5956a500-1a29-4904-877f-f5f049516434"
                       :label      {:fi "Viides kysymys" :sv ""}
                       :params     {}
                       :options    [{:label {:fi "Ensimmäinen vaihtoehto" :sv ""} :value "Ensimmäinen vaihtoehto"}
                                    {:label     {:fi "Toinen vaihtoehto" :sv ""}
                                     :value     "Toinen vaihtoehto"
                                     :followups [{:id         "d3a065f0-daf7-4599-a5b4-d50ab2254e43"
                                                  :label      {:fi "Oletko punavihervärisokea?" :sv ""}
                                                  :params     {}
                                                  :options    [{:label {:fi "Kyllä" :sv ""} :value "Kyllä"}
                                                               {:label {:fi "En" :sv ""} :value "En"}]
                                                  :fieldType  "singleChoice"
                                                  :fieldClass "formField"
                                                  :validators ["required"]}
                                                 {:id         "c06dd3f8-a2a3-4472-ac13-136e12b134d4"
                                                  :label      {:fi "Vierekkäinen tekstikenttä monivalinnan jatkokysymyksenä"}
                                                  :params     {:repeatable true}
                                                  :children   [{:id         "727a5848-751c-4d17-aab4-2d2145621791"
                                                                :label      {:fi "Jatkokysymys A" :sv ""}
                                                                :params     {:adjacent true}
                                                                :fieldType  "textField"
                                                                :fieldClass "formField"
                                                                :validators ["required"]}
                                                               {:id         "7192099b-07c6-4529-8735-7fe126f732f6"
                                                                :label      {:fi "Jatkokysymys B" :sv ""}
                                                                :params     {:adjacent true}
                                                                :fieldType  "textField"
                                                                :fieldClass "formField"}
                                                               {:id         "d758edeb-9391-4bc4-95ee-4f1d83c4cd5b"
                                                                :label      {:fi "Jatkokysymys C" :sv ""}
                                                                :params     {:adjacent true}
                                                                :fieldType  "textField"
                                                                :fieldClass "formField"
                                                                :validators ["required"]}]
                                                  :fieldType  "adjacentfieldset"
                                                  :fieldClass "wrapperElement"}]}
                                    {:label {:fi "Kolmas vaihtoehto" :sv ""} :value "Kolmas vaihtoehto"}
                                    {:label {:fi "" :sv ""} :value ""}]
                       :fieldType  "multipleChoice"
                       :fieldClass "formField"}
                      {:id              "a4d77f64-7324-4fcb-9f34-4e3e8c1e27d8"
                       :label           {:fi "Kuudes kysymys" :sv ""}
                       :params          {}
                       :options         []
                       :fieldType       "multipleChoice"
                       :fieldClass      "formField"
                       :koodisto-source {:uri "tutkinto" :title "Tutkinto" :version 1}}
                      {:id         "10b55e7e-c5af-4ee9-bc60-e91a1f4891ff"
                       :label      {:fi "Testiosio" :sv ""}
                       :params     {}
                       :children   [{:id         "960cfe2a-50d6-442f-82a6-64cdc532e181"
                                     :label      {:fi "Osiokysymys" :sv ""}
                                     :params     {:size "S"}
                                     :fieldType  "textArea"
                                     :fieldClass "formField"
                                     :validators ["required"]}]
                       :fieldType  "fieldset"
                       :fieldClass "wrapperElement"}
                      {:id         "0bd55843-3303-48ab-8168-77d82d90e4db"
                       :label      {:fi "Infoteksti" :sv ""}
                       :params     {:info-text {:label {:en "" :fi "oikeen pitka infoteksti sitten tassa." :sv ""}}}
                       :fieldType  "textField"
                       :fieldClass "formField"}
                      {:id              "b4b29b8d-ed9b-45c3-ac2a-dd88c6ca4212"
                       :label           {:fi "Viimeinen kysymys" :sv ""}
                       :params          {}
                       :options         [{:label {:fi "" :sv ""} :value ""}]
                       :fieldType       "dropdown"
                       :fieldClass      "formField"
                       :koodisto-source {:uri "tutkinto" :title "Tutkinto" :version 1}}
                      {:id         "2c3c05f5-e088-49ca-9853-71e77cbfafa8"
                       :label      {:fi "Lyhyen listan kysymys" :sv ""}
                       :params     {}
                       :options    [{:label     {:fi "Ensimmäinen vaihtoehto" :sv ""}
                                     :value     "Ensimmäinen vaihtoehto"
                                     :followups [{:id         "7a6568a7-3ad5-4ac3-a9cb-7b09404e1063"
                                                  :label      {:fi "Monivalinta jatkokysymyksenä" :sv ""}
                                                  :params     {}
                                                  :options    [{:label {:fi "Jatkokysymys A" :sv ""} :value "Jatkokysymys A"}
                                                               {:label {:fi "Jatkokysymys B" :sv ""} :value "Jatkokysymys B"}]
                                                  :fieldType  "multipleChoice"
                                                  :fieldClass "formField"
                                                  :validators ["required"]}
                                                 {:id         "adb520a7-f65a-4d09-9414-c97d96b363d0"
                                                  :label      {:fi "Vierekkäinen tekstikenttä painikkeiden jatkokysymyksenä"}
                                                  :params     {:repeatable true}
                                                  :children   [{:id         "83614490-0345-44a7-aa3a-18cf72f5aef1"
                                                                :label      {:fi "Jatkokysymys A" :sv ""}
                                                                :params     {:adjacent true}
                                                                :fieldType  "textField"
                                                                :fieldClass "formField"
                                                                :validators ["required"]}
                                                               {:id         "5f85651c-94b8-42da-95c8-b374defbf916"
                                                                :label      {:fi "Jatkokysymys B" :sv ""}
                                                                :params     {:adjacent true}
                                                                :fieldType  "textField"
                                                                :fieldClass "formField"}
                                                               {:id         "ab5b249f-ae4f-4072-b238-6bca577687fc"
                                                                :label      {:fi "Jatkokysymys C" :sv ""}
                                                                :params     {:adjacent true}
                                                                :fieldType  "textField"
                                                                :fieldClass "formField"
                                                                :validators ["required"]}]
                                                  :fieldType  "adjacentfieldset"
                                                  :fieldClass "wrapperElement"}]}
                                    {:label {:fi "Toinen vaihtoehto" :sv ""} :value "Toinen vaihtoehto"}]
                       :fieldType  "singleChoice"
                       :fieldClass "formField"
                       :validators ["required"]}
                      {:id         "3705addb-5723-4f65-a706-192f8625d53a"
                       :label      {:fi "Vierekkäinen tekstikenttä"}
                       :children   [{:id         "d55aa422-4bed-4a05-aa35-b07b5179324b"
                                     :label      {:fi "Tekstikenttä 1" :sv ""}
                                     :params     {:adjacent true}
                                     :fieldType  "textField"
                                     :fieldClass "formField"}
                                    {:id         "def3efb6-46ce-4983-a288-9e39528aff4d"
                                     :label      {:fi "Tekstikenttä 2" :sv ""}
                                     :params     {:adjacent true}
                                     :fieldType  "textField"
                                     :fieldClass "formField"}]
                       :fieldType  "adjacentfieldset"
                       :fieldClass "wrapperElement"}
                      {:id         "dbca8687-e8ff-4d21-b306-3b927018080f"
                       :label      {:fi "Päätason pudotusvalikko" :sv ""}
                       :params     {}
                       :options    [{:label     {:fi "Pudotusvalikon 1. kysymys" :sv ""}
                                     :value     "Pudotusvalikon 1. kysymys"
                                     :followups [{:id         "768eb783-1124-4baf-8f64-f40aeea86ad3"
                                                  :label      {:fi "Vierekkäinen tekstikenttä jatkokysymyksenä"}
                                                  :params     {:repeatable true}
                                                  :children   [{:id         "a4a03d55-ebdd-45ab-b292-1e9a0ed3a209"
                                                                :label      {:fi "Jatkokysymys A" :sv ""}
                                                                :params     {:adjacent true}
                                                                :fieldType  "textField"
                                                                :fieldClass "formField"
                                                                :validators ["required"]}
                                                               {:id         "89642a00-df35-4c72-8181-badd5ea2046d"
                                                                :label      {:fi "Jatkokysymys B" :sv ""}
                                                                :params     {:adjacent true}
                                                                :fieldType  "textField"
                                                                :fieldClass "formField"}
                                                               {:id         "329f186a-92d6-46e6-a60f-874bb7940a74"
                                                                :label      {:fi "Jatkokysymys C" :sv ""}
                                                                :params     {:adjacent true}
                                                                :fieldType  "textField"
                                                                :fieldClass "formField"
                                                                :validators ["required"]}]
                                                  :fieldType  "adjacentfieldset"
                                                  :fieldClass "wrapperElement"}]}
                                    {:label {:fi "Pudotusvalikon 2. kysymys" :sv ""} :value "Pudotusvalikon 2. kysymys"}]
                       :fieldType  "dropdown"
                       :fieldClass "formField"}]})
