(ns ataru.component-data.higher-education-base-education-module
  (:require [ataru.component-data.component :refer [form-section
                                                    multiple-choice
                                                    single-choice-button
                                                    text-field
                                                    text-area
                                                    info-element
                                                    attachment
                                                    dropdown
                                                    question-group]]
            [ataru.util :as util]
            [ataru.translations.texts :refer [higher-base-education-module-texts general-texts]]))

(def higher-education-module-options [{:followups
                      [{:belongs-to-hakukohteet []
                        :fieldClass             "formField"
                        :fieldType              "singleChoice"
                        :id                     "pohjakoulutus_yo"
                        :label
                                                {:en
                                                     "Have you completed your Matriculation examination in Finland in 1990 or after?"
                                                 :fi
                                                     "Oletko suorittanut ylioppilastutkinnon vuonna 1990 tai sen jälkeen?"
                                                 :sv "Har du avlagt studentexamen år 1990 eller senare?"}
                        :options
                                                [{:followups
                                                         [{:belongs-to-hakukohteet []
                                                           :fieldClass             "formField"
                                                           :fieldType              "textField"
                                                           :id                     "pohjakoulutus_yo--yes-year-of-completion"
                                                           :label                  {:en "Year of completion"
                                                                                    :fi "Suoritusvuosi"
                                                                                    :sv "Avlagd år"}
                                                           :params                 {:decimals  nil
                                                                                    :max-value "2019"
                                                                                    :min-value "1990"
                                                                                    :numeric   true
                                                                                    :size      "S"}
                                                           :validators             ["required" "numeric"]}
                                                          {:belongs-to-hakukohteet []
                                                           :fieldClass             "infoElement"
                                                           :fieldType              "p"
                                                           :id                     "hbem--f03a6d74-3e92-4905-84db-eca821d42ed0"
                                                           :label                  {:fi ""}
                                                           :params                 {}
                                                           :text
                                                                                   {:en
                                                                                    "Your matriculation examination details are received automatically from the Matriculation Examination Board."
                                                                                    :fi
                                                                                    "Saamme ylioppilastutkintosi tiedot rekisteristämme."
                                                                                    :sv
                                                                                    "Vi får uppgifterna om din studentexamen ur vårt register."}}]
                                                  :label {:en "Yes" :fi "Kyllä" :sv "Ja"}
                                                  :value "Yes"}
                                                 {:followups
                                                         [{:belongs-to-hakukohteet []
                                                           :fieldClass             "formField"
                                                           :fieldType              "textField"
                                                           :id                     "pohjakoulutus_yo--no-year-of-completion"
                                                           :label                  {:en "Year of completion"
                                                                                    :fi "Suoritusvuosi"
                                                                                    :sv "Avlagd år"}
                                                           :params                 {:max-value "1989"
                                                                                    :min-value "1900"
                                                                                    :numeric   true
                                                                                    :size      "S"}
                                                           :validators             ["numeric" "required"]}
                                                          {:belongs-to-hakukohderyhma ["1.2.246.562.28.24712637358"]
                                                           :belongs-to-hakukohteet    ["1.2.246.562.20.70407522443"
                                                                                       "1.2.246.562.20.43105453732"]
                                                           :fieldClass                "formField"
                                                           :fieldType                 "attachment"
                                                           :id                        "pohjakoulutus-yo--attachment"
                                                           :label
                                                                                      {:en "Matriculation examination (completed before 1990)"
                                                                                       :fi "Ylioppilastutkintotodistus (ennen vuotta 1990)"
                                                                                       :sv "Studentexamen (före år 1990)"}
                                                           :options                   []
                                                           :params
                                                                                      {:info-text
                                                                                       {:enabled? true
                                                                                        :value
                                                                                                  {:en
                                                                                                   "The attachment has to be submitted within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n"
                                                                                                   :fi
                                                                                                   "Liite täytyy tallentaa 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunim\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX."
                                                                                                   :sv
                                                                                                   "Bilagan ska sparas senast 7 dygn innan ansökningstiden går ut. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"}}}}]
                                                  :label {:en "No" :fi "Ei" :sv "Nej"}
                                                  :value "No"}]
                        :params                 {}
                        :validators             ["required"]}]
               :label {:en "Matriculation examination completed in Finland"
                       :fi "Suomessa suoritettu ylioppilastutkinto"
                       :sv "Studentexamen som avlagts i Finland"}
               :value "pohjakoulutus_yo"}
              {:followups
                      [{:belongs-to-hakukohteet []
                        :fieldClass             "formField"
                        :fieldType              "textField"
                        :id                     "hbem--c157cbde-3904-46b7-95e1-641fb8314a11"
                        :label
                                                {:en "Year of completion" :fi "Suoritusvuosi" :sv "Avlagd år"}
                        :params                 {:numeric true :size "S"}
                        :validators             ["required" "numeric"]}
                       {:belongs-to-hakukohderyhma []
                        :belongs-to-hakukohteet    []
                        :fieldClass                "formField"
                        :fieldType                 "attachment"
                        :id                        "lukion-paattotodistus"
                        :label
                                                   {:en "High school diploma" :fi "Lukion päättötodistus" :sv ""}
                        :options                   []
                        :params
                                                   {:hidden true
                                                    :info-text
                                                            {:enabled? true
                                                             :value
                                                                       {:en
                                                                        "The attachment has to be submitted within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n"
                                                                        :fi
                                                                        "Liite täytyy tallentaa 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunim\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX."
                                                                        :sv
                                                                        "Bilagan ska sparas senast 7 dygn innan ansökningstiden går ut. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"}}}}]
               :label
                      {:en
                           "General upper secondary school syllabus completed in Finland (without matriculation examination)"
                       :fi "Suomessa suoritettu lukion oppimäärä ilman ylioppilastutkintoa"
                       :sv "Gymnasiets lärokurs som avlagts i Finland utan studentexamen"}
               :value "pohjakoulutus_lk"}
              {:followups
                      [{:belongs-to-hakukohderyhma []
                        :belongs-to-hakukohteet    []
                        :fieldClass                "formField"
                        :fieldType                 "dropdown"
                        :id                        "pohjakoulutus_yo_kansainvalinen_suomessa--exam-type"
                        :label                     {:en "Matriculation examination"
                                                    :fi "Ylioppilastutkinto"
                                                    :sv "Studentexamen"}
                        :options
                                                   [{:followups
                                                            [{:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                                              :belongs-to-hakukohteet    []
                                                              :fieldClass                "infoElement"
                                                              :fieldType                 "p"
                                                              :id                        "hbem--6febd426-40ba-4481-a729-28ba4981462e"
                                                              :label
                                                                                         {:en
                                                                                          "Fill in the grades of your international matriculation examination "
                                                                                          :fi
                                                                                          "Täytä kansainvälisen ylioppilastutkintosi arvosanat"
                                                                                          :sv
                                                                                          "Fyll i vitsorden från din internationella studentexamen"}
                                                              :params                    {}
                                                              :text
                                                                                         {:en
                                                                                          "In the drop-down menu are the grades of the Finnish matriculation examination. First you need to convert your grades with the [conversion chart](https://studyinfo.fi/wp2/en/higher-education/applying/conversion-chart-for-eb-ib-and-reifeprufung-examinations/). If you do not have a grade in the subject, choose 'No grade'."
                                                                                          :fi
                                                                                          "Alasvetovalikoissa on suomalaisen ylioppilastutkinnon arvosanat. Muunna ensin todistuksesi arvosanat [muuntotaulukon](https://opintopolku.fi/wp/ammattikorkeakoulu/miten-opiskelijat-valitaan/ammattikorkeakoulujen-valintaperustesuositukset-2019/#EB-,%20IB-%20ja%20Reifeprufung-tutkintojen%20muuntokaava) avulla. Jos sinulla ei ole arvosanaa kyseisestä aineesta, valitse vaihtoehto 'Ei arvosanaa'."
                                                                                          :sv
                                                                                          "I rullgardinsmenyn finns vitsorden för den finländska studentexamen. Omvandla först vitsorden från ditt betyg med hjälp av [omvandlingsschemat](https://studieinfo.fi/wp/yrkeshogskola/hur-antas-studerande/rekommendation-om-antagningsgrunder-for-yrkeshogskolor-ar-2019/#Omvandlingsschema) . Om du inte har ett vitsord för något ämne, välj alternativet 'Inget vitsord'."}}
                                                             {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                                              :belongs-to-hakukohteet    []
                                                              :fieldClass                "formField"
                                                              :fieldType                 "dropdown"
                                                              :id                        "hbem--d2fc8f6f-c26a-433b-a735-af2c930d5838"
                                                              :label                     {:en "Mother tongue "
                                                                                          :fi "Äidinkieli"
                                                                                          :sv "Modersmålet "}
                                                              :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                                          {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                                          {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                                          {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                                          {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                                          {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                                          {:label {:en "No grade"
                                                                                                   :fi "Ei arvosanaa"
                                                                                                   :sv "Inget vitsord"}
                                                                                           :value "6"}]
                                                              :params                    {}
                                                              :validators                ["required"]}
                                                             {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                                              :belongs-to-hakukohteet    []
                                                              :fieldClass                "formField"
                                                              :fieldType                 "dropdown"
                                                              :id                        "hbem--79e360dc-b884-42e4-94b5-b01ea8394f0a"
                                                              :label                     {:en "Mathematics, advanced syllabus"
                                                                                          :fi "Matematiikka, pitkä"
                                                                                          :sv "Matematik, lång"}
                                                              :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                                          {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                                          {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                                          {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                                          {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                                          {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                                          {:label {:en "No grade"
                                                                                                   :fi "Ei arvosanaa"
                                                                                                   :sv "Inget vitsord"}
                                                                                           :value "6"}]
                                                              :params                    {}
                                                              :validators                ["required"]}
                                                             {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                                              :belongs-to-hakukohteet    []
                                                              :fieldClass                "formField"
                                                              :fieldType                 "dropdown"
                                                              :id                        "hbem--caf234a0-f6ca-4ff1-ad1e-c9504ef57a98"
                                                              :label                     {:en "Mathematics, basic syllabus"
                                                                                          :fi "Matematiikka, lyhyt"
                                                                                          :sv "Matematik, kort "}
                                                              :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                                          {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                                          {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                                          {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                                          {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                                          {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                                          {:label {:en "No grade"
                                                                                                   :fi "Ei arvosanaa"
                                                                                                   :sv "Inget vitsord"}
                                                                                           :value "6"}]
                                                              :params                    {}
                                                              :validators                ["required"]}
                                                             {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                                              :belongs-to-hakukohteet    []
                                                              :fieldClass                "formField"
                                                              :fieldType                 "dropdown"
                                                              :id                        "hbem--7cd907b0-87f7-42bf-ab38-d7dbb820f854"
                                                              :label                     {:en "Best advanced language syllabus "
                                                                                          :fi "Paras kieli, pitkä"
                                                                                          :sv "Bästa språk, långt "}
                                                              :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                                          {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                                          {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                                          {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                                          {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                                          {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                                          {:label {:en "No grade"
                                                                                                   :fi "Ei arvosanaa"
                                                                                                   :sv "Inget vitsord"}
                                                                                           :value "6"}]
                                                              :params                    {}
                                                              :validators                ["required"]}
                                                             {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                                              :belongs-to-hakukohteet    []
                                                              :fieldClass                "formField"
                                                              :fieldType                 "dropdown"
                                                              :id                        "hbem--bf9532f0-fdd4-4c57-9cf2-274962a3c033"
                                                              :label                     {:en "Best basic/intermediate language syllabus"
                                                                                          :fi "Paras kieli, lyhyt/keskipitkä"
                                                                                          :sv "Bästa språk, kort/mellanlångt"}
                                                              :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                                          {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                                          {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                                          {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                                          {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                                          {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                                          {:label {:en "No grade"
                                                                                                   :fi "Ei arvosanaa"
                                                                                                   :sv "Inget vitsord"}
                                                                                           :value "6"}]
                                                              :params                    {}
                                                              :validators                ["required"]}
                                                             {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                                              :belongs-to-hakukohteet    []
                                                              :fieldClass                "formField"
                                                              :fieldType                 "dropdown"
                                                              :id                        "hbem--47b4d35a-c785-43f7-9ea9-9737a287ba8d"
                                                              :label                     {:en "Best of general studies battery tests "
                                                                                          :fi "Paras reaaliaineiden kokeista"
                                                                                          :sv "Bästa realämnesprov"}
                                                              :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                                          {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                                          {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                                          {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                                          {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                                          {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                                          {:label {:en "No grade"
                                                                                                   :fi "Ei arvosanaa"
                                                                                                   :sv "Inget vitsord"}
                                                                                           :value "6"}]
                                                              :params                    {}
                                                              :validators                ["required"]}
                                                             {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                                              :belongs-to-hakukohteet    []
                                                              :fieldClass                "formField"
                                                              :fieldType                 "dropdown"
                                                              :id                        "hbem--6b0589e3-b690-4aca-9d1a-98aeff2b55ce"
                                                              :label
                                                                                         {:en
                                                                                              "General studies battery test in physics, chemistry or biology"
                                                                                          :fi "Fysiikan, kemian tai biologian reaalikoe"
                                                                                          :sv "Realprovet i fysik, kemi eller biologi"}
                                                              :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                                          {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                                          {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                                          {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                                          {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                                          {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                                          {:label {:en "No grade"
                                                                                                   :fi "Ei arvosanaa"
                                                                                                   :sv "Inget vitsord"}
                                                                                           :value "6"}]
                                                              :params                    {}
                                                              :validators                ["required"]}
                                                             {:belongs-to-hakukohderyhma []
                                                              :belongs-to-hakukohteet    []
                                                              :fieldClass                "formField"
                                                              :fieldType                 "singleChoice"
                                                              :id                        "hbem--32b5f6a9-1ccb-4227-8c68-3c0a82fb0a73"
                                                              :label                     {:en "Year of completion"
                                                                                          :fi "Suoritusvuosi"
                                                                                          :sv "Avlagd år"}
                                                              :options
                                                                                         [{:followups
                                                                                                  [{:belongs-to-hakukohderyhma
                                                                                                                ["1.2.246.562.28.24712637358"]
                                                                                                    :belongs-to-hakukohteet
                                                                                                                ["1.2.246.562.20.43105453732"
                                                                                                                 "1.2.246.562.20.70407522443"]
                                                                                                    :fieldClass "formField"
                                                                                                    :fieldType  "attachment"
                                                                                                    :id         "candidate-predicted-grades"
                                                                                                    :label
                                                                                                                {:en "Predicted grades"
                                                                                                                 :fi
                                                                                                                     "Oppilaitoksen myöntämä ennakkoarvio arvosanoista (Candidate Predicted Grades)"
                                                                                                                 :sv "Predicted grades"}
                                                                                                    :options    []
                                                                                                    :params
                                                                                                                {:deadline nil
                                                                                                                 :info-text
                                                                                                                           {:enabled? true
                                                                                                                            :value
                                                                                                                                      {:en
                                                                                                                                       "The attachment has to be submitted within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n\n"
                                                                                                                                       :fi
                                                                                                                                       "Liite täytyy tallentaa viimeistään 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n"
                                                                                                                                       :sv
                                                                                                                                       "Bilagan ska sparas senast 7 dygn innan ansökningstiden går ut. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}}}
                                                                                                   {:belongs-to-hakukohderyhma
                                                                                                                ["1.2.246.562.28.24712637358"]
                                                                                                    :belongs-to-hakukohteet
                                                                                                                ["1.2.246.562.20.70407522443"
                                                                                                                 "1.2.246.562.20.43105453732"]
                                                                                                    :fieldClass "formField"
                                                                                                    :fieldType  "attachment"
                                                                                                    :id         "diploma-programme"
                                                                                                    :label
                                                                                                                {:en "Diploma Programme (DP) Results"
                                                                                                                 :fi "Diploma Programme (DP) Results -dokumentti"
                                                                                                                 :sv "Diploma Programme (DP) Results"}
                                                                                                    :options    []
                                                                                                    :params
                                                                                                                {:deadline "6.1.2020 15:00"
                                                                                                                 :info-text
                                                                                                                           {:enabled? true
                                                                                                                            :value
                                                                                                                                      {:en
                                                                                                                                       "The attachment has to be submitted by the deadline informed next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n"
                                                                                                                                       :fi
                                                                                                                                       "Liitteen määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX."
                                                                                                                                       :sv
                                                                                                                                       "Den angivna tidpunkten för bilagan syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}}}]
                                                                                           :label {:en "2019" :fi "2019" :sv "2019"}
                                                                                           :value "0"}
                                                                                          {:followups
                                                                                                  [{:belongs-to-hakukohteet []
                                                                                                    :fieldClass             "formField"
                                                                                                    :fieldType              "textField"
                                                                                                    :id                     "hbem--a2bdac0a-e994-4fda-aa59-4ab4af2384a2"
                                                                                                    :label                  {:en "Year of completion"
                                                                                                                             :fi "Suoritusvuosi"
                                                                                                                             :sv "Avlagd år"}
                                                                                                    :params                 {:numeric true :size "S"}
                                                                                                    :validators             ["required" "numeric"]}
                                                                                                   {:belongs-to-hakukohderyhma
                                                                                                                ["1.2.246.562.28.24712637358"]
                                                                                                    :belongs-to-hakukohteet
                                                                                                                ["1.2.246.562.20.70407522443"
                                                                                                                 "1.2.246.562.20.43105453732"]
                                                                                                    :fieldClass "formField"
                                                                                                    :fieldType  "attachment"
                                                                                                    :id         "ib-diploma"
                                                                                                    :label      {:en "Diploma"
                                                                                                                 :fi "IB Diploma -tutkintotodistus"
                                                                                                                 :sv "Diploma"}
                                                                                                    :options    []
                                                                                                    :params
                                                                                                                {:info-text
                                                                                                                 {:enabled? true
                                                                                                                  :value
                                                                                                                            {:en
                                                                                                                             "The attachment has to be submitted within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n"
                                                                                                                             :fi
                                                                                                                             "Liite täytyy tallentaa viimeistään 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n"
                                                                                                                             :sv
                                                                                                                             "Bilagan ska sparas senast 7 dygn innan ansökningstiden går ut. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}}}]
                                                                                           :label {:en "Before 2019"
                                                                                                   :fi "Ennen vuotta 2019"
                                                                                                   :sv "Före 2019"}
                                                                                           :value "1"}]
                                                              :params                    {}
                                                              :validators                ["required"]}]
                                                     :label {:en "International Baccalaureate -diploma"
                                                             :fi "International Baccalaureate"
                                                             :sv "International Baccalaureate -examen"}
                                                     :value "International Baccalaureate -diploma"}
                                                    {:followups
                                                            [{:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                                              :fieldClass                "infoElement"
                                                              :fieldType                 "p"
                                                              :id                        "hbem--3566211e-758c-49a4-ac8d-ad2ed8a5c762"
                                                              :label
                                                                                         {:en
                                                                                          "Fill in the grades of your international matriculation examination "
                                                                                          :fi
                                                                                          "Täytä kansainvälisen ylioppilastutkintosi arvosanat"
                                                                                          :sv
                                                                                          "Fyll i vitsorden från din internationella studentexamen"}
                                                              :params                    {}
                                                              :text
                                                                                         {:en
                                                                                          "In the drop-down menu are the grades of the Finnish matriculation examination. First you need to convert your grades with the [conversion chart](https://studyinfo.fi/wp2/en/higher-education/applying/conversion-chart-for-eb-ib-and-reifeprufung-examinations/). If you do not have a grade in the subject, choose 'No grade'."
                                                                                          :fi
                                                                                          "Alasvetovalikoissa on suomalaisen ylioppilastutkinnon arvosanat. Muunna ensin todistuksesi arvosanat [muuntotaulukon](https://opintopolku.fi/wp/ammattikorkeakoulu/miten-opiskelijat-valitaan/ammattikorkeakoulujen-valintaperustesuositukset-2019/#EB-,%20IB-%20ja%20Reifeprufung-tutkintojen%20muuntokaava) avulla. Jos sinulla ei ole arvosanaa kyseisestä aineesta, valitse vaihtoehto 'Ei arvosanaa'."
                                                                                          :sv
                                                                                          "I rullgardinsmenyn finns vitsorden för den finländska studentexamen. Omvandla först vitsorden från ditt betyg med hjälp av [omvandlingsschemat](https://studieinfo.fi/wp/yrkeshogskola/hur-antas-studerande/rekommendation-om-antagningsgrunder-for-yrkeshogskolor-ar-2019/#Omvandlingsschema) . Om du inte har ett vitsord för något ämne, välj alternativet 'Inget vitsord'."}}
                                                             {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                                              :fieldClass                "formField"
                                                              :fieldType                 "dropdown"
                                                              :id                        "hbem--5290f4bd-e9b6-44c8-ba2e-07bfde0d18ae"
                                                              :label                     {:en "Mother tongue "
                                                                                          :fi "Äidinkieli"
                                                                                          :sv "Modersmålet "}
                                                              :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                                          {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                                          {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                                          {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                                          {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                                          {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                                          {:label {:en "No grade"
                                                                                                   :fi "Ei arvosanaa"
                                                                                                   :sv "Inget vitsord"}
                                                                                           :value "6"}]
                                                              :params                    {}
                                                              :validators                ["required"]}
                                                             {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                                              :fieldClass                "formField"
                                                              :fieldType                 "dropdown"
                                                              :id                        "hbem--e730f2f8-0951-4710-a5b7-099f900f5d48"
                                                              :label                     {:en "Mathematics, advanced syllabus "
                                                                                          :fi "Matematiikka, pitkä"
                                                                                          :sv "Matematik, lång "}
                                                              :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                                          {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                                          {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                                          {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                                          {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                                          {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                                          {:label {:en "No grade"
                                                                                                   :fi "Ei arvosanaa"
                                                                                                   :sv "Inget vitsord"}
                                                                                           :value "6"}]
                                                              :params                    {}
                                                              :validators                ["required"]}
                                                             {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                                              :fieldClass                "formField"
                                                              :fieldType                 "dropdown"
                                                              :id                        "hbem--27307828-0c06-45cf-a1f3-8bff256c8995"
                                                              :label                     {:en "Mathematics, basic syllabus "
                                                                                          :fi "Matematiikka, lyhyt"
                                                                                          :sv "Matematik, kort "}
                                                              :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                                          {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                                          {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                                          {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                                          {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                                          {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                                          {:label {:en "No grade"
                                                                                                   :fi "Ei arvosanaa"
                                                                                                   :sv "Inget vitsord"}
                                                                                           :value "6"}]
                                                              :params                    {}
                                                              :validators                ["required"]}
                                                             {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                                              :fieldClass                "formField"
                                                              :fieldType                 "dropdown"
                                                              :id                        "hbem--65262d84-a0c1-4278-9597-493cc8f33949"
                                                              :label                     {:en "Best advanced language syllabus "
                                                                                          :fi "Paras kieli, pitkä"
                                                                                          :sv "Bästa språk, långt "}
                                                              :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                                          {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                                          {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                                          {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                                          {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                                          {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                                          {:label {:en "No grade"
                                                                                                   :fi "Ei arvosanaa"
                                                                                                   :sv "Inget vitsord"}
                                                                                           :value "6"}]
                                                              :params                    {}
                                                              :validators                ["required"]}
                                                             {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                                              :fieldClass                "formField"
                                                              :fieldType                 "dropdown"
                                                              :id                        "hbem--43c294c8-b53d-4cfa-9369-f7712a044a4e"
                                                              :label                     {:en "Best basic/intermediate language syllabus"
                                                                                          :fi "Paras kieli, lyhyt/keskipitkä"
                                                                                          :sv "Bästa språk, kort/mellanlångt"}
                                                              :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                                          {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                                          {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                                          {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                                          {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                                          {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                                          {:label {:en "No grade"
                                                                                                   :fi "Ei arvosanaa"
                                                                                                   :sv "Inget vitsord"}
                                                                                           :value "6"}]
                                                              :params                    {}
                                                              :validators                ["required"]}
                                                             {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                                              :fieldClass                "formField"
                                                              :fieldType                 "dropdown"
                                                              :id                        "hbem--48370908-00bc-4a03-bddb-1bb6ffe29ede"
                                                              :label                     {:en "Best of general studies battery tests "
                                                                                          :fi "Paras reaaliaineiden kokeista"
                                                                                          :sv "Bästa realämnesprov"}
                                                              :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                                          {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                                          {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                                          {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                                          {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                                          {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                                          {:label {:en "No grade"
                                                                                                   :fi "Ei arvosanaa"
                                                                                                   :sv "Inget vitsord"}
                                                                                           :value "6"}]
                                                              :params                    {}
                                                              :validators                ["required"]}
                                                             {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                                              :fieldClass                "formField"
                                                              :fieldType                 "dropdown"
                                                              :id                        "hbem--6f9a932e-d659-4766-8d42-664980a2b1db"
                                                              :label
                                                                                         {:en
                                                                                              "General studies battery test in physics, chemistry or biology"
                                                                                          :fi "Fysiikan, kemian tai biologian reaalikoe"
                                                                                          :sv "Realprovet i fysik, kemi eller biologi"}
                                                              :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                                          {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                                          {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                                          {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                                          {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                                          {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                                          {:label {:en "No grade"
                                                                                                   :fi "Ei arvosanaa"
                                                                                                   :sv "Inget vitsord"}
                                                                                           :value "6"}]
                                                              :params                    {}
                                                              :validators                ["required"]}
                                                             {:fieldClass "formField"
                                                              :fieldType  "singleChoice"
                                                              :id         "hbem--64d561e2-20f7-4143-9ad8-b6fa9a8f6fed"
                                                              :label      {:en "Year of completion"
                                                                           :fi "Suoritusvuosi"
                                                                           :sv "Avlagd år"}
                                                              :options
                                                                          [{:followups
                                                                                   [{:belongs-to-hakukohderyhma
                                                                                                 ["1.2.246.562.28.24712637358"]
                                                                                     :belongs-to-hakukohteet
                                                                                                 ["1.2.246.562.20.43105453732"
                                                                                                  "1.2.246.562.20.70407522443"]
                                                                                     :fieldClass "formField"
                                                                                     :fieldType  "attachment"
                                                                                     :id         "oppilaitoksen-myontama-ennakkoarvio-arvosanoista"
                                                                                     :label
                                                                                                 {:en "Predicted grades"
                                                                                                  :fi
                                                                                                      "Oppilaitoksen myöntämä ennakkoarvio arvosanoista"
                                                                                                  :sv "Läroanstaltens preliminära vitsord"}
                                                                                     :options    []
                                                                                     :params
                                                                                                 {:deadline nil
                                                                                                  :info-text
                                                                                                            {:enabled? true
                                                                                                             :value
                                                                                                                       {:en
                                                                                                                        "The attachment has to be submitted within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n"
                                                                                                                        :fi
                                                                                                                        "Liite täytyy tallentaa viimeistään 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n"
                                                                                                                        :sv
                                                                                                                        "Bilagan ska sparas senast 7 dygn innan ansökningstiden går ut. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}}}
                                                                                    {:belongs-to-hakukohderyhma
                                                                                                 ["1.2.246.562.28.24712637358"]
                                                                                     :belongs-to-hakukohteet
                                                                                                 ["1.2.246.562.20.70407522443"
                                                                                                  "1.2.246.562.20.43105453732"]
                                                                                     :fieldClass "formField"
                                                                                     :fieldType  "attachment"
                                                                                     :id         "european-baccalaureate-certificate"
                                                                                     :label
                                                                                                 {:en "European Baccalaureate certificate"
                                                                                                  :fi
                                                                                                      "European Baccalaureate Certificate -tutkintotodistus"
                                                                                                  :sv "European Baccalaureate certificate"}
                                                                                     :options    []
                                                                                     :params
                                                                                                 {:deadline "30.12.2019 15:00"
                                                                                                  :info-text
                                                                                                            {:enabled? true
                                                                                                             :value
                                                                                                                       {:en
                                                                                                                        "The attachment has to be submitted by the deadline informed next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX."
                                                                                                                        :fi
                                                                                                                        "Liitteen määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX."
                                                                                                                        :sv
                                                                                                                        "Den angivna tidpunkten för bilagan syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}}}]
                                                                            :label {:en "2019" :fi "2019" :sv "2019"}
                                                                            :value "0"}
                                                                           {:followups
                                                                                   [{:fieldClass "formField"
                                                                                     :fieldType  "textField"
                                                                                     :id         "hbem--6e2ad9bf-5f3a-41de-aada-a939aeda3e87"
                                                                                     :label      {:en "Year of completion"
                                                                                                  :fi "Suoritusvuosi"
                                                                                                  :sv "Avlagd år"}
                                                                                     :params     {:numeric true :size "S"}
                                                                                     :validators ["numeric" "required"]}
                                                                                    {:belongs-to-hakukohderyhma
                                                                                                 ["1.2.246.562.28.24712637358"]
                                                                                     :belongs-to-hakukohteet
                                                                                                 ["1.2.246.562.20.43105453732"
                                                                                                  "1.2.246.562.20.70407522443"]
                                                                                     :fieldClass "formField"
                                                                                     :fieldType  "attachment"
                                                                                     :id         "european-baccalaureate-certificate-2"
                                                                                     :label
                                                                                                 {:en "European Baccalaureate certificate"
                                                                                                  :fi
                                                                                                      "European Baccalaureate Certificate -tutkintotodistus"
                                                                                                  :sv "European Baccalaureate certificate"}
                                                                                     :options    []
                                                                                     :params
                                                                                                 {:info-text
                                                                                                  {:enabled? true
                                                                                                   :value
                                                                                                             {:en
                                                                                                              "The attachment has to be submitted within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n"
                                                                                                              :fi
                                                                                                              "Liite täytyy tallentaa viimeistään 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n"
                                                                                                              :sv
                                                                                                              "Bilagan ska sparas senast 7 dygn innan ansökningstiden går ut. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}}}]
                                                                            :label {:en "Before 2019"
                                                                                    :fi "Ennen vuotta 2019"
                                                                                    :sv "Före 2019"}
                                                                            :value "1"}]
                                                              :params     {}
                                                              :validators ["required"]}]
                                                     :label {:en "European Baccalaureate -diploma"
                                                             :fi "Eurooppalainen ylioppilastutkinto"
                                                             :sv "European Baccalaureate -examen"}
                                                     :value " Eurooppalainen ylioppilastutkinto"}
                                                    {:followups
                                                     [{:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                                       :fieldClass                "infoElement"
                                                       :fieldType                 "p"
                                                       :id                        "hbem--a44bb2f4-d955-41c5-82e1-edfef35e8419"
                                                       :label
                                                                                  {:en
                                                                                   "Fill in the grades of your international matriculation examination "
                                                                                   :fi
                                                                                   "Täytä kansainvälisen ylioppilastutkintosi arvosanat"
                                                                                   :sv
                                                                                   "Fyll i vitsorden från din internationella studentexamen"}
                                                       :params                    {}
                                                       :text
                                                                                  {:en
                                                                                   "In the drop-down menu are the grades of the Finnish matriculation examination. First you need to convert your grades with the [conversion chart](https://studyinfo.fi/wp2/en/higher-education/applying/conversion-chart-for-eb-ib-and-reifeprufung-examinations/). If you do not have a grade in the subject, choose 'No grade'."
                                                                                   :fi
                                                                                   "Alasvetovalikoissa on suomalaisen ylioppilastutkinnon arvosanat. Muunna ensin todistuksesi arvosanat [muuntotaulukon](https://opintopolku.fi/wp/ammattikorkeakoulu/miten-opiskelijat-valitaan/ammattikorkeakoulujen-valintaperustesuositukset-2019/#EB-,%20IB-%20ja%20Reifeprufung-tutkintojen%20muuntokaava) avulla. Jos sinulla ei ole arvosanaa kyseisestä aineesta, valitse vaihtoehto 'Ei arvosanaa'."
                                                                                   :sv
                                                                                   "I rullgardinsmenyn finns vitsorden för den finländska studentexamen. Omvandla först vitsorden från ditt betyg med hjälp av [omvandlingsschemat](https://studieinfo.fi/wp/yrkeshogskola/hur-antas-studerande/rekommendation-om-antagningsgrunder-for-yrkeshogskolor-ar-2019/#Omvandlingsschema) . Om du inte har ett vitsord för något ämne, välj alternativet 'Inget vitsord'."}}
                                                      {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                                       :fieldClass                "formField"
                                                       :fieldType                 "dropdown"
                                                       :id                        "hbem--57198259-7a84-4b36-9e54-121e9b45821b"
                                                       :label                     {:en "Mother tongue "
                                                                                   :fi "Äidinkieli"
                                                                                   :sv "Modersmålet "}
                                                       :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                                   {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                                   {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                                   {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                                   {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                                   {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                                   {:label {:en "No grade"
                                                                                            :fi "Ei arvosanaa"
                                                                                            :sv "Inget vitsord"}
                                                                                    :value "6"}]
                                                       :params                    {}
                                                       :validators                ["required"]}
                                                      {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                                       :fieldClass                "formField"
                                                       :fieldType                 "dropdown"
                                                       :id                        "hbem--0dead2aa-afba-4727-8554-9bee61f4e585"
                                                       :label                     {:en "Mathematics, advanced syllabus"
                                                                                   :fi "Matematiikka, pitkä"
                                                                                   :sv "Matematik, lång "}
                                                       :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                                   {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                                   {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                                   {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                                   {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                                   {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                                   {:label {:en "No grade"
                                                                                            :fi "Ei arvosanaa"
                                                                                            :sv "Inget vitsord"}
                                                                                    :value "6"}]
                                                       :params                    {}
                                                       :validators                ["required"]}
                                                      {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                                       :fieldClass                "formField"
                                                       :fieldType                 "dropdown"
                                                       :id                        "hbem--c676f07a-0294-4be1-b166-5819f8f9586c"
                                                       :label                     {:en "Mathematics, basic syllabus "
                                                                                   :fi "Matematiikka, lyhyt"
                                                                                   :sv "Matematik, kort "}
                                                       :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                                   {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                                   {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                                   {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                                   {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                                   {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                                   {:label {:en "No grade"
                                                                                            :fi "Ei arvosanaa"
                                                                                            :sv "Inget vitsord"}
                                                                                    :value "6"}]
                                                       :params                    {}
                                                       :validators                ["required"]}
                                                      {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                                       :fieldClass                "formField"
                                                       :fieldType                 "dropdown"
                                                       :id                        "hbem--cf8e2907-1ea3-4d77-87c7-0ae7a318d799"
                                                       :label                     {:en "Best advanced language syllabus"
                                                                                   :fi "Paras kieli, pitkä"
                                                                                   :sv "Bästa språk, långt "}
                                                       :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                                   {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                                   {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                                   {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                                   {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                                   {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                                   {:label {:en "No grade"
                                                                                            :fi "Ei arvosanaa"
                                                                                            :sv "Inget vitsord"}
                                                                                    :value "6"}]
                                                       :params                    {}
                                                       :validators                ["required"]}
                                                      {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                                       :fieldClass                "formField"
                                                       :fieldType                 "dropdown"
                                                       :id                        "hbem--78eed37b-04a0-4a68-bdce-c010f9ce8303"
                                                       :label                     {:en "Best basic/intermediate language syllabus"
                                                                                   :fi "Paras kieli, lyhyt/keskipitkä"
                                                                                   :sv "Bästa språk, kort/mellanlångt"}
                                                       :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                                   {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                                   {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                                   {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                                   {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                                   {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                                   {:label {:en "No grade"
                                                                                            :fi "Ei arvosanaa"
                                                                                            :sv "Inget vitsord"}
                                                                                    :value "6"}]
                                                       :params                    {}
                                                       :validators                ["required"]}
                                                      {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                                       :fieldClass                "formField"
                                                       :fieldType                 "dropdown"
                                                       :id                        "hbem--d8510941-7d57-4dfe-8098-269bfa9cb273"
                                                       :label                     {:en "Best of general studies battery tests "
                                                                                   :fi "Paras reaaliaineiden kokeista"
                                                                                   :sv "Bästa realämnesprov"}
                                                       :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                                   {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                                   {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                                   {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                                   {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                                   {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                                   {:label {:en "No grade"
                                                                                            :fi "Ei arvosanaa"
                                                                                            :sv "Inget vitsord"}
                                                                                    :value "6"}]
                                                       :params                    {}
                                                       :validators                ["required"]}
                                                      {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                                       :fieldClass                "formField"
                                                       :fieldType                 "dropdown"
                                                       :id                        "hbem--71e9c362-0fec-4060-bec8-e6de77c14ca8"
                                                       :label
                                                                                  {:en
                                                                                       "General studies battery test in physics, chemistry or biology"
                                                                                   :fi "Fysiikan, kemian tai biologian reaalikoe"
                                                                                   :sv "Realprovet i fysik, kemi eller biologi"}
                                                       :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                                   {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                                   {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                                   {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                                   {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                                   {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                                   {:label {:en "No grade"
                                                                                            :fi "Ei arvosanaa"
                                                                                            :sv "Inget vitsord"}
                                                                                    :value "6"}]
                                                       :params                    {}
                                                       :validators                ["required"]}
                                                      {:fieldClass "formField"
                                                       :fieldType  "singleChoice"
                                                       :id         "hbem--6b7119c9-42ec-467d-909c-6d1cc555b823"
                                                       :label      {:en "Year of completion"
                                                                    :fi "Suoritusvuosi"
                                                                    :sv "Avlagd år"}
                                                       :options
                                                                   [{:followups
                                                                            [{:belongs-to-hakukohderyhma
                                                                                          ["1.2.246.562.28.24712637358"]
                                                                              :belongs-to-hakukohteet
                                                                                          ["1.2.246.562.20.43105453732"
                                                                                           "1.2.246.562.20.70407522443"]
                                                                              :fieldClass "formField"
                                                                              :fieldType  "attachment"
                                                                              :id         "oppilaitoksen-myontama-todistus-arvosanoista"
                                                                              :label
                                                                                          {:en "Certificate of grades"
                                                                                           :fi
                                                                                               "Oppilaitoksen myöntämä todistus arvosanoista"
                                                                                           :sv "Läroanstaltens betyg över vitsord"}
                                                                              :options    []
                                                                              :params
                                                                                          {:deadline nil
                                                                                           :info-text
                                                                                                     {:enabled? true
                                                                                                      :value
                                                                                                                {:en
                                                                                                                 "The attachment has to be submitted within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n"
                                                                                                                 :fi
                                                                                                                 "Liite täytyy tallentaa viimeistään 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n"
                                                                                                                 :sv
                                                                                                                 "Bilagan ska sparas senast 7 dygn innan ansökningstiden går ut. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}}}
                                                                             {:belongs-to-hakukohderyhma
                                                                                          ["1.2.246.562.28.24712637358"]
                                                                              :belongs-to-hakukohteet
                                                                                          ["1.2.246.562.20.43105453732"
                                                                                           "1.2.246.562.20.70407522443"]
                                                                              :fieldClass "formField"
                                                                              :fieldType  "attachment"
                                                                              :id         "reifeprufung-dia-tutkintotodistus"
                                                                              :label      {:en "Reifeprüfung/DIA -diploma"
                                                                                           :fi "Reifeprüfung/DIA-tutkintotodistus"
                                                                                           :sv "Reifeprüfung/DIA -examensbetyg"}
                                                                              :options    []
                                                                              :params
                                                                                          {:deadline "30.12.2019 15:00"
                                                                                           :info-text
                                                                                                     {:enabled? true
                                                                                                      :value
                                                                                                                {:en
                                                                                                                 "The attachment has to be submitted by the deadline informed next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX."
                                                                                                                 :fi
                                                                                                                 "Liitteen määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX."
                                                                                                                 :sv
                                                                                                                 "Den angivna tidpunkten för bilagan syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}}}]
                                                                     :label {:en "2019" :fi "2019" :sv "2019"}
                                                                     :value "0"}
                                                                    {:followups
                                                                            [{:fieldClass "formField"
                                                                              :fieldType  "textField"
                                                                              :id         "hbem--c643447c-b667-42ab-9fd6-66b40a722a3c"
                                                                              :label      {:en "Year of completion"
                                                                                           :fi "Suoritusvuosi"
                                                                                           :sv "Avslagd år"}
                                                                              :params     {:numeric true :size "S"}
                                                                              :validators ["numeric" "required"]}
                                                                             {:belongs-to-hakukohderyhma
                                                                                          ["1.2.246.562.28.24712637358"]
                                                                              :belongs-to-hakukohteet
                                                                                          ["1.2.246.562.20.43105453732"
                                                                                           "1.2.246.562.20.70407522443"]
                                                                              :fieldClass "formField"
                                                                              :fieldType  "attachment"
                                                                              :id         "reifeprufung-dia-tutkintotodistus-2"
                                                                              :label      {:en "Reifeprüfung/DIA -diploma"
                                                                                           :fi "Reifeprüfung/DIA-tutkintotodistus"
                                                                                           :sv "Reifeprüfung/DIA -examensbetyg"}
                                                                              :options    []
                                                                              :params
                                                                                          {:info-text
                                                                                           {:enabled? true
                                                                                            :value
                                                                                                      {:en
                                                                                                       "The attachment has to be submitted within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n"
                                                                                                       :fi
                                                                                                       "Liite täytyy tallentaa viimeistään 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n"
                                                                                                       :sv
                                                                                                       "Bilagan ska sparas senast 7 dygn innan ansökningstiden går ut. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}}}]
                                                                     :label
                                                                            {:en "Before 2019" :fi "Ennen 2019" :sv "Före 2019"}
                                                                     :value "1"}]
                                                       :params     {}}]
                                                     :label
                                                     {:en
                                                          "Reifeprüfung - diploma/ Deutsche Internationale Abiturprüfung"
                                                      :fi "Reifeprüfung/Deutsche Internationale Abiturprüfung"
                                                      :sv
                                                          "Reifeprüfung - examen/Deutsche Internationale Abiturprüfung"}
                                                     :value
                                                     "Reifeprüfung - diploma/ Deutsche Internationale Abiturprüfung"}]
                        :params                    {}
                        :validators                ["required"]}
                       {:fieldClass "formField"
                        :fieldType  "textField"
                        :id         "pohjakoulutus_yo_kansainvalinen_suomessa--institution"
                        :label      {:en "Educational institution"
                                     :fi "Oppilaitos"
                                     :sv "Läroanstalt"}
                        :params     {}
                        :validators ["required"]}]
               :label
                      {:en
                       "International matriculation examination completed in Finland (IB, EB and RP/DIA)"
                       :fi
                       "Suomessa suoritettu kansainvälinen ylioppilastutkinto (IB, EB ja RP/DIA)"
                       :sv
                       "Internationell studentexamen som avlagts i Finland (IB, EB och RP/DIA)"}
               :value "pohjakoulutus_yo_kansainvalinen_suomessa"}
              {:followups
                      [{:fieldClass "formField"
                        :fieldType  "textField"
                        :id
                                    "pohjakoulutus_yo_ammatillinen--marticulation-year-of-completion"
                        :label      {:en "The year of completion of Matriculation examination"
                                     :fi "Ylioppilastutkinnon suoritusvuosi"
                                     :sv "Studentexamen avlagd år"}
                        :params     {:numeric true :size "S"}
                        :validators ["numeric" "required"]}
                       {:fieldClass "formField"
                        :fieldType  "textField"
                        :id         "pohjakoulutus_yo_ammatillinen--vocational-completion-year"
                        :label      {:en "The year of completion of vocational qualification"
                                     :fi "Ammatillisen tutkinnon suoritusvuosi"
                                     :sv "Yrkesinriktad examen avlagd år"}
                        :params     {:numeric true :size "S"}
                        :validators ["numeric" "required"]}
                       {:fieldClass "formField"
                        :fieldType  "textField"
                        :id         "pohjakoulutus_yo_ammatillinen--vocational-qualification"
                        :label      {:en "Vocational qualification"
                                     :fi "Ammatillinen tutkinto"
                                     :sv "Yrkesinriktad examen"}
                        :params     {}
                        :validators ["required"]}
                       {:fieldClass "formField"
                        :fieldType  "textField"
                        :id         "pohjakoulutus_yo_ammatillinen--scope-of-qualification"
                        :label      {:en "Scope of qualification"
                                     :fi "Tutkinnon laajuus"
                                     :sv "Examens omfattning"}
                        :params     {:decimals 1 :numeric true :size "S"}
                        :validators ["numeric" "required"]}
                       {:fieldClass "formField"
                        :fieldType  "dropdown"
                        :id         "pohjakoulutus_yo_ammatillinen--scope-of-qualification-units"
                        :label      {:en "The scope unit"
                                     :fi "Laajuuden yksikkö"
                                     :sv "Omfattningen enheten"}
                        :options
                                    [{:label {:en "Courses" :fi "Kurssia" :sv "Kurser"}
                                      :value "Courses"}
                                     {:label
                                             {:en "ECTS credits" :fi "Opintopistettä" :sv "Studiepoäng"}
                                      :value "ECTS credits"}
                                     {:label
                                             {:en "Study weeks" :fi "Opintoviikkoa" :sv "Studieveckor"}
                                      :value "Study weeks"}
                                     {:label {:en "Competence points"
                                              :fi "Osaamispistettä"
                                              :sv "Kompetenspoäng"}
                                      :value "Competence points"}
                                     {:label {:en "Hours" :fi "Tuntia" :sv "Timmar"} :value "Hours"}
                                     {:label {:en "Weekly lessons per year"
                                              :fi "Vuosiviikkotuntia"
                                              :sv "Årsveckotimmar"}
                                      :value "Weekly lessons per year"}
                                     {:label {:en "Years" :fi "Vuotta" :sv "År"} :value "Years"}]
                        :params     {}
                        :validators ["required"]}
                       {:fieldClass "formField"
                        :fieldType  "textField"
                        :id         "pohjakoulutus_yo_ammatillinen--educational-institution"
                        :label      {:en "Educational institution"
                                     :fi "Oppilaitos"
                                     :sv "Läroanstalt"}
                        :params     {}
                        :validators ["required"]}
                       {:belongs-to-hakukohderyhma []
                        :belongs-to-hakukohteet
                                                   ["1.2.246.562.20.35233116835" "1.2.246.562.20.47250143782"
                                                    "1.2.246.562.20.145275138910" "1.2.246.562.20.98329087336"
                                                    "1.2.246.562.20.35576532648"]
                        :fieldClass                "formField"
                        :fieldType                 "attachment"
                        :id                        "hbem--a5234286-1275-41b1-a72b-84c9cf291966"
                        :label
                                                   {:en "Vocational qualification"
                                                    :fi "Ammatillisen perustutkinnon tutkintotodistus"
                                                    :sv
                                                        "Yrkesinriktad grundexamen examen på skolnivå examen på institutnivå eller examen på yrkesinriktad högre nivå"}
                        :options                   []
                        :params
                                                   {:info-text
                                                    {:enabled? true
                                                     :value
                                                               {:en
                                                                "The attachment has to be submitted within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n"
                                                                :fi
                                                                "Liite täytyy tallentaa viimeistään 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\nTallenna hakemuksen liitteeksi päättötodistus tai tutkintotodistus. Huomaathan, että pelkkä opintosuoritusote ei riitä.\n"
                                                                :sv
                                                                "Bilagan ska sparas senast 7 dygn innan ansökningstiden går ut. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}}}]
               :label
                      {:en "Double degree (secondary level) completed in Finland"
                       :fi
                           "Suomessa suoritettu ammatillinen perustutkinto ja ylioppilastutkinto (kaksoistutkinto)"
                       :sv "Dubbelexamen som avlagts i Finland"}
               :value "pohjakoulutus_yo_ammatillinen"}
              {:followups
                      [{:children
                                    [{:fieldClass "formField"
                                      :fieldType  "textField"
                                      :id         "hbem--f3a87aa7-b782-4947-a4a0-0f126147f7b5"
                                      :label      {:en "Year of completion"
                                                   :fi "Suoritusvuosi"
                                                   :sv "Avslagd år"}
                                      :params     {:numeric true :size "S"}
                                      :validators ["numeric" "required"]}
                                     {:fieldClass "formField"
                                      :fieldType  "textField"
                                      :id         "hbem--8aa3f396-a48e-47c7-9034-f25d3004e7df"
                                      :label      {:en "Qualification" :fi "Tutkinto" :sv "Examen"}
                                      :params     {}
                                      :validators ["required"]}
                                     {:fieldClass "formField"
                                      :fieldType  "textField"
                                      :id         "hbem--4f787c2a-375a-4ec2-a91a-40471fc602d2"
                                      :label      {:en "Scope of qualification"
                                                   :fi "Laajuus"
                                                   :sv "Examens omfattning"}
                                      :params     {:decimals 1 :numeric true :repeatable false :size "S"}
                                      :validators ["numeric" "required"]}
                                     {:fieldClass "formField"
                                      :fieldType  "dropdown"
                                      :id         "hbem--d49c470a-21e7-482f-a238-16a7e749bc48"
                                      :label      {:fi "Laajuuden yksikkö" :sv ""}
                                      :options
                                                  [{:label {:en "Courses" :fi "Kurssia" :sv "Kurser"}
                                                    :value "0"}
                                                   {:label {:en "ECTS credits"
                                                            :fi "Opintopistettä"
                                                            :sv "Studiepoäng"}
                                                    :value "1"}
                                                   {:label {:en "Study weeks"
                                                            :fi "Opintoviikkoa"
                                                            :sv "Studieveckor"}
                                                    :value "2"}
                                                   {:label {:en "Competence points"
                                                            :fi "Osaamispistettä"
                                                            :sv "Kompetenspoäng"}
                                                    :value "3"}
                                                   {:label {:en "Hours" :fi "Tuntia" :sv "Timmar"} :value "4"}
                                                   {:label {:en "Weekly lessons per year"
                                                            :fi "Vuosiviikkotuntia"
                                                            :sv "Årsveckotimmar"}
                                                    :value "5"}
                                                   {:label {:en "Years" :fi "Vuotta" :sv "År"} :value "6"}]
                                      :params     {}
                                      :validators ["required"]}
                                     {:fieldClass "formField"
                                      :fieldType  "textField"
                                      :id         "hbem--f3ed5a32-75b0-4f62-8ebb-b0bf9a042bed"
                                      :label      {:en "Educational institution"
                                                   :fi "Oppilaitos"
                                                   :sv "Läroanstalt"}
                                      :params     {}
                                      :validators ["required"]}
                                     {:fieldClass "formField"
                                      :fieldType  "singleChoice"
                                      :id         "hbem--40d0e877-67b8-4be1-8b0c-d7e14a7a52c1"
                                      :label
                                                  {:en
                                                       "Have you completed your qualification as a competence based qualification in its entiretity?"
                                                   :fi
                                                       "Oletko suorittanut tutkinnon kokonaan näyttötutkintona?"
                                                   :sv "Har du avlagt examen som fristående yrkesexamen?"}
                                      :options    [{:label {:en "Yes" :fi "Kyllä" :sv "Ja"} :value "0"}
                                                   {:label {:en "No" :fi "Ei" :sv "Nej"} :value "1"}]
                                      :params     {}
                                      :validators ["required"]}
                                     {:belongs-to-hakukohderyhma []
                                      :belongs-to-hakukohteet
                                                                 ["1.2.246.562.20.35576532648" "1.2.246.562.20.79616290072"
                                                                  "1.2.246.562.20.70407522443" "1.2.246.562.20.145275138910"
                                                                  "1.2.246.562.20.35233116835" "1.2.246.562.20.62484081492"
                                                                  "1.2.246.562.20.43105453732" "1.2.246.562.20.87913828114"
                                                                  "1.2.246.562.20.89197541538" "1.2.246.562.20.47250143782"
                                                                  "1.2.246.562.20.78454166535" "1.2.246.562.20.16618292623"
                                                                  "1.2.246.562.20.48004078043" "1.2.246.562.20.98329087336"]
                                      :fieldClass                "formField"
                                      :fieldType                 "attachment"
                                      :id                        "hbem--a44e4086-b2f9-4110-839f-3266c23b7f3e"
                                      :label
                                                                 {:en "Vocational qualification"
                                                                  :fi
                                                                      "Ammatillisen perustutkinnon, kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkintotodistus"
                                                                  :sv
                                                                      "Yrkesinriktad grundexamen examen på skolnivå examen på institutnivå eller examen på yrkesinriktad högre nivå"}
                                      :options                   []
                                      :params
                                                                 {:info-text
                                                                  {:enabled? true
                                                                   :value
                                                                             {:en
                                                                              "The attachment has to be submitted within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n"
                                                                              :fi
                                                                              "Liite täytyy tallentaa viimeistään 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\nTallenna hakemuksen liitteeksi päättötodistus tai tutkintotodistus. Huomaathan, että pelkkä opintosuoritusote ei riitä.\n"
                                                                              :sv
                                                                              "Bilagan ska sparas senast 7 dygn innan ansökningstiden går ut. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}}}
                                     {:fieldClass "infoElement"
                                      :fieldType  "p"
                                      :id         "hbem--d252db76-8383-47f8-b723-e0fbe43354f7"
                                      :label
                                                  {:en "Click add if you want add further qualifications."
                                                   :fi "Paina lisää jos haluat lisätä useampia tutkintoja."
                                                   :sv
                                                       "Tryck på lägg till om du vill lägga till flera examina."}
                                      :params     {}
                                      :text       {:fi ""}}]
                        :fieldClass "questionGroup"
                        :fieldType  "fieldset"
                        :id         "hbem--c8bd423a-b468-4e52-b2fb-3e23164f56ff"
                        :label      {:fi "" :sv ""}
                        :params     {}}]
               :label
                      {:en
                       "Vocational upper secondary qualification, school-level qualification, post-secondary level qualification or higher vocational level qualification completed in Finland"
                       :fi
                       "Suomessa suoritettu ammatillinen perustutkinto, kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto"
                       :sv
                       "Yrkesinriktad grundexamen, examen på skolnivå, examen på institutsnivå eller yrkesinriktad examen på högre nivå som avlagts i Finland"}
               :value "pohjakoulutus_am"}
              {:followups
                      [{:children
                                    [{:fieldClass "formField"
                                      :fieldType  "textField"
                                      :id         "hbem--c8d351ad-cd95-4f40-a128-530585fa0c0d"
                                      :label      {:en "Year of completion"
                                                   :fi "Suoritusvuosi"
                                                   :sv "Avslagd år"}
                                      :params     {:numeric true :size "S"}
                                      :validators ["numeric" "required"]}
                                     {:fieldClass "formField"
                                      :fieldType  "textField"
                                      :id         "hbem--833f063b-11ef-4fe2-819b-c9d43967cb89"
                                      :label      {:en "Qualification" :fi "Tutkinto" :sv "Examen"}
                                      :params     {}
                                      :validators ["required"]}
                                     {:fieldClass "formField"
                                      :fieldType  "textField"
                                      :id         "hbem--fb70fc16-9f28-4a37-9d29-7a994e3c6dda"
                                      :label      {:en "Scope of qualification"
                                                   :fi "Laajuus"
                                                   :sv "Examens omfattning"}
                                      :params     {:decimals 1 :numeric true :size "S"}
                                      :validators ["numeric" "required"]}
                                     {:fieldClass "formField"
                                      :fieldType  "dropdown"
                                      :id         "hbem--7136073f-7699-4409-b910-5622457aba1b"
                                      :label      {:fi "Laajuuden yksikkö" :sv ""}
                                      :options
                                                  [{:label {:en "Courses" :fi "Kurssia" :sv "Kurser"}
                                                    :value "0"}
                                                   {:label {:en "ECTS credits"
                                                            :fi "Opintopistettä"
                                                            :sv "Studiepoäng"}
                                                    :value "1"}
                                                   {:label {:en "Study weeks"
                                                            :fi "Opintoviikkoa"
                                                            :sv "Studieveckor"}
                                                    :value "2"}
                                                   {:label {:en "Competence points"
                                                            :fi "Osaamispistettä"
                                                            :sv "Kompetenspoäng"}
                                                    :value "3"}
                                                   {:label {:en "Hours" :fi "Tuntia" :sv "Timmar"} :value "4"}
                                                   {:label {:en "Weekly lessons per year"
                                                            :fi "Vuosiviikkotuntia"
                                                            :sv "Årsveckotimmar"}
                                                    :value "5"}
                                                   {:label {:en "Years" :fi "Vuotta" :sv "År"} :value "6"}]
                                      :params     {}
                                      :validators ["required"]}
                                     {:fieldClass "formField"
                                      :fieldType  "textField"
                                      :id         "hbem--9fc050d3-6e63-4a71-854d-f69b1ac35dc9"
                                      :label      {:en "Educational institution"
                                                   :fi "Oppilaitos"
                                                   :sv "Läroanstalt"}
                                      :params     {}
                                      :validators ["required"]}
                                     {:belongs-to-hakukohderyhma []
                                      :belongs-to-hakukohteet    ["1.2.246.562.20.43105453732"
                                                                  "1.2.246.562.20.70407522443"]
                                      :fieldClass                "formField"
                                      :fieldType                 "attachment"
                                      :id                        "hbem--c1894413-0f89-4039-bf11-c56f76a8c832"
                                      :label                     {:en "Vocational or specialist vocational qualification"
                                                                  :fi "Ammatti- tai erikoisammattitutkintotodistus"
                                                                  :sv "En yrkesexamen eller en specialyrkesexamen"}
                                      :options                   []
                                      :params
                                                                 {:info-text
                                                                  {:enabled? true
                                                                   :value
                                                                             {:en
                                                                              "The attachment has to be submitted within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n"
                                                                              :fi
                                                                              "Liite täytyy tallentaa viimeistään 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n"
                                                                              :sv
                                                                              "Bilagan ska sparas senast 7 dygn innan ansökningstiden går ut. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}}}
                                     {:fieldClass "infoElement"
                                      :fieldType  "p"
                                      :id         "hbem--6d905522-f5c6-4cb6-a8cc-86199f41b520"
                                      :label
                                                  {:en "Click add if you want add further qualifications."
                                                   :fi "Paina lisää, jos haluat lisätä useampia tutkintoja."
                                                   :sv
                                                       "Tryck på lägg till om du vill lägga till flera examina."}
                                      :params     {}
                                      :text       {:fi ""}}]
                        :fieldClass "questionGroup"
                        :fieldType  "fieldset"
                        :id         "hbem--c0e5bedd-a5dc-44df-a760-efa382e12545"
                        :label      {:fi "" :sv ""}
                        :params     {}}]
               :label
                      {:en
                           "Further vocational qualification or specialist vocational qualification completed in Finland"
                       :fi "Suomessa suoritettu ammatti- tai erikoisammattitutkinto"
                       :sv "Yrkesexamen eller specialyrkesexamen som avlagts i Finland"}
               :value "pohjakoulutus_amt"}
              {:followups
                      [{:children
                                    [{:belongs-to-hakukohteet []
                                      :fieldClass             "formField"
                                      :fieldType              "textField"
                                      :id                     "hbem--124a0215-e358-47e1-ab02-f1cc7c831e0e"
                                      :label                  {:en "Date and year of completion (DD.MM.YYYY)"
                                                               :fi "Päivämäärä ja suoritusvuosi  (pp.kk.vvvv)"
                                                               :sv "Datum och år då examen avlagts (dd.mm.åååå)"}
                                      :params                 {:size "S"}
                                      :validators             ["required"]}
                                     {:belongs-to-hakukohteet []
                                      :fieldClass             "formField"
                                      :fieldType              "textField"
                                      :id                     "hbem--f02807da-f80c-47d8-b71e-efcb755dbed9"
                                      :label                  {:en "Qualification" :fi "Tutkinto" :sv "Examen"}
                                      :params                 {}
                                      :validators             ["required"]}
                                     {:belongs-to-hakukohteet []
                                      :fieldClass             "formField"
                                      :fieldType              "dropdown"
                                      :id                     "hbem--ffa32c10-dccd-4932-bef4-dde322131722"
                                      :koodisto-source
                                                              {:title "Kk-tutkinnot" :uri "kktutkinnot" :version 1}
                                      :label
                                                              {:en "Degree level" :fi "Tutkintotaso " :sv "Examensnivå"}
                                      :options                [{:label {:en "Lower university degree (Bachelor's)"
                                                                        :fi "Alempi yliopistotutkinto (kandidaatti)"
                                                                        :sv "Lägre universitetsexamen (kandidat)"}
                                                                :value "2"}
                                                               {:label {:en "Polytechnic/UAS Bachelor's degree"
                                                                        :fi "Ammattikorkeakoulututkinto"
                                                                        :sv "Yrkeshögskoleexamen"}
                                                                :value "1"}
                                                               {:label {:en "Licentiate/doctoral"
                                                                        :fi "Lisensiaatti/tohtori"
                                                                        :sv "Licentiat/doktor"}
                                                                :value "5"}
                                                               {:label {:en "Polytechnic/UAS Master's degree"
                                                                        :fi "Ylempi ammattikorkeakoulututkinto"
                                                                        :sv "Högre yrkeshögskoleexamen"}
                                                                :value "3"}
                                                               {:label {:en "Higher university degree (Master's)"
                                                                        :fi "Ylempi yliopistotutkinto (maisteri)"
                                                                        :sv "Högre universitetsexamen (magister)"}
                                                                :value "4"}]
                                      :params                 {}}
                                     {:fieldClass "formField"
                                      :fieldType  "textField"
                                      :id         "hbem--8c5055b4-b0fb-4ead-bfff-14bbfef01bf0"
                                      :label      {:en "Higher education institution"
                                                   :fi "Korkeakoulu"
                                                   :sv "Högskola"}
                                      :params     {}
                                      :validators ["required"]}
                                     {:belongs-to-hakukohderyhma []
                                      :belongs-to-hakukohteet    ["1.2.246.562.20.43105453732"
                                                                  "1.2.246.562.20.70407522443"]
                                      :fieldClass                "formField"
                                      :fieldType                 "attachment"
                                      :id                        "korkeakoulututkinnon-tutkintotodistus"
                                      :label                     {:en "Higher education degree"
                                                                  :fi "Korkeakoulututkinnon tutkintotodistus"
                                                                  :sv "Högskoleexamen"}
                                      :options                   []
                                      :params
                                                                 {:info-text
                                                                  {:enabled? true
                                                                   :value
                                                                             {:en
                                                                              "The attachment has to be submitted within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n"
                                                                              :fi
                                                                              "Liite täytyy tallentaa viimeistään 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\nTallenna hakemuksen liitteeksi kopio tutkintotodistuksesta mahdollisine liitteineen.\n"
                                                                              :sv
                                                                              "Bilagan ska sparas senast 7 dygn innan ansökningstiden går ut. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}}}
                                     {:fieldClass "infoElement"
                                      :fieldType  "p"
                                      :id         "hbem--02d9dbe9-c6a0-420a-8f90-e686d571ef40"
                                      :label
                                                  {:en "Click add if you want add further qualifications."
                                                   :fi "Paina lisää jos haluat lisätä useampia tutkintoja."
                                                   :sv
                                                       "Tryck på lägg till om du vill lägga till flera examina."}
                                      :params     {}
                                      :text       {:fi ""}}]
                        :fieldClass "questionGroup"
                        :fieldType  "fieldset"
                        :id         "hbem--3c89eb8d-46be-4a0e-b7d8-f5d8dab0fdff"
                        :label      {:fi "" :sv ""}
                        :params     {}}]
               :label {:en "Higher education qualification completed in Finland"
                       :fi "Suomessa suoritettu korkeakoulututkinto"
                       :sv "Högskoleexamen som avlagts i Finland"}
               :value "pohjakoulutus_kk"}
              {:followups
                      [{:fieldClass "formField"
                        :fieldType  "dropdown"
                        :id         "pohjakoulutus_yo_ulkomainen--name-of-examination"
                        :label      {:en "Name of examination/diploma"
                                     :fi "Ylioppilastutkinto"
                                     :sv "Studentexamen"}
                        :options
                                    [{:followups
                                             [{:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                               :fieldClass                "infoElement"
                                               :fieldType                 "p"
                                               :id                        "hbem--acbfd09f-152e-452d-a239-183a8178ca3c"
                                               :label
                                                                          {:en
                                                                           "Fill in the grades of your international matriculation examination "
                                                                           :fi
                                                                           "Täytä kansainvälisen ylioppilastutkintosi arvosanat"
                                                                           :sv
                                                                           "Fyll i vitsorden från din internationella studentexamen"}
                                               :params                    {}
                                               :text
                                                                          {:en
                                                                           "In the drop-down menu are the grades of the Finnish matriculation examination. First you need to convert your grades with the [conversion chart](https://studyinfo.fi/wp2/en/higher-education/applying/conversion-chart-for-eb-ib-and-reifeprufung-examinations/). If you do not have a grade in the subject, choose 'No grade'."
                                                                           :fi
                                                                           "Alasvetovalikoissa on suomalaisen ylioppilastutkinnon arvosanat. Muunna ensin todistuksesi arvosanat [muuntotaulukon](https://opintopolku.fi/wp/ammattikorkeakoulu/miten-opiskelijat-valitaan/ammattikorkeakoulujen-valintaperustesuositukset-2019/#EB-,%20IB-%20ja%20Reifeprufung-tutkintojen%20muuntokaava) avulla. Jos sinulla ei ole arvosanaa kyseisestä aineesta, valitse vaihtoehto 'Ei arvosanaa'."
                                                                           :sv
                                                                           "I rullgardinsmenyn finns vitsorden för den finländska studentexamen. Omvandla först vitsorden från ditt betyg med hjälp av [omvandlingsschemat](https://studieinfo.fi/wp/yrkeshogskola/hur-antas-studerande/rekommendation-om-antagningsgrunder-for-yrkeshogskolor-ar-2019/#Omvandlingsschema) . Om du inte har ett vitsord för något ämne, välj alternativet 'Inget vitsord'."}}
                                              {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                               :fieldClass                "formField"
                                               :fieldType                 "dropdown"
                                               :id                        "hbem--3c8d15b9-3d0b-43b0-b928-2f8b0b7ccaed"
                                               :label
                                                                          {:en "Mother tongue " :fi "Äidinkieli" :sv "Modersmålet"}
                                               :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                           {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                           {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                           {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                           {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                           {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                           {:label {:en "No grade"
                                                                                    :fi "Ei arvosanaa"
                                                                                    :sv "Inget vitsord"}
                                                                            :value "6"}]
                                               :params                    {}
                                               :validators                ["required"]}
                                              {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                               :fieldClass                "formField"
                                               :fieldType                 "dropdown"
                                               :id                        "hbem--72da3965-6adb-48e8-97f2-3447c1dfe930"
                                               :label                     {:en "Mathematics, advanced syllabus"
                                                                           :fi "Matematiikka, pitkä"
                                                                           :sv "Matematik, lång"}
                                               :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                           {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                           {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                           {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                           {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                           {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                           {:label {:en "No grade"
                                                                                    :fi "Ei arvosanaa"
                                                                                    :sv "Inget vitsord"}
                                                                            :value "6"}]
                                               :params                    {}
                                               :validators                ["required"]}
                                              {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                               :fieldClass                "formField"
                                               :fieldType                 "dropdown"
                                               :id                        "hbem--bdaa634b-01ae-406c-9b1a-49b86c87d1ad"
                                               :label                     {:en "Mathematics, basic syllabus"
                                                                           :fi "Matematiikka, lyhyt"
                                                                           :sv "Matematik, kort "}
                                               :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                           {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                           {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                           {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                           {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                           {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                           {:label {:en "No grade"
                                                                                    :fi "Ei arvosanaa"
                                                                                    :sv "Inget vitsord"}
                                                                            :value "6"}]
                                               :params                    {}
                                               :validators                ["required"]}
                                              {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                               :fieldClass                "formField"
                                               :fieldType                 "dropdown"
                                               :id                        "hbem--6273c6c6-5265-444d-9374-65e961ff5876"
                                               :label                     {:en "Best advanced language syllabus "
                                                                           :fi "Paras kieli, pitkä"
                                                                           :sv "Bästa språk, långt "}
                                               :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                           {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                           {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                           {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                           {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                           {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                           {:label {:en "No grade"
                                                                                    :fi "Ei arvosanaa"
                                                                                    :sv "Inget vitsord"}
                                                                            :value "6"}]
                                               :params                    {}
                                               :validators                ["required"]}
                                              {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                               :fieldClass                "formField"
                                               :fieldType                 "dropdown"
                                               :id                        "hbem--27f1ce8a-5f29-4ad7-8b4e-412b2344089f"
                                               :label                     {:en "Best basic/intermediate language syllabus"
                                                                           :fi "Paras kieli, lyhyt/keskipitkä"
                                                                           :sv "Bästa språk, kort/mellanlångt"}
                                               :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                           {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                           {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                           {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                           {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                           {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                           {:label {:en "No grade"
                                                                                    :fi "Ei arvosanaa"
                                                                                    :sv "Inget vitsord"}
                                                                            :value "6"}]
                                               :params                    {}
                                               :validators                ["required"]}
                                              {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                               :fieldClass                "formField"
                                               :fieldType                 "dropdown"
                                               :id                        "hbem--3a456c72-5c10-4ee6-9ae5-2ac5ecc60642"
                                               :label                     {:en "Best of general studies battery tests "
                                                                           :fi "Paras reaaliaineiden kokeista"
                                                                           :sv "Bästa realämnesprov"}
                                               :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                           {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                           {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                           {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                           {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                           {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                           {:label {:en "No grade"
                                                                                    :fi "Ei arvosanaa"
                                                                                    :sv "Inget vitsord"}
                                                                            :value "6"}]
                                               :params                    {}
                                               :validators                ["required"]}
                                              {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                               :fieldClass                "formField"
                                               :fieldType                 "dropdown"
                                               :id                        "hbem--33fccf0e-2a0e-4189-a065-9188fe791cb4"
                                               :label
                                                                          {:en
                                                                               "General studies battery test in physics, chemistry or biology"
                                                                           :fi "Fysiikan, kemian tai biologian reaalikoe"
                                                                           :sv "Realprovet i fysik, kemi eller biologi"}
                                               :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                           {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                           {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                           {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                           {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                           {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                           {:label {:en "No grade"
                                                                                    :fi "Ei arvosanaa"
                                                                                    :sv "Inget vitsord"}
                                                                            :value "6"}]
                                               :params                    {}
                                               :validators                ["required"]}
                                              {:belongs-to-hakukohderyhma []
                                               :fieldClass                "formField"
                                               :fieldType                 "singleChoice"
                                               :id                        "hbem--d037fa56-6354-44fc-87d6-8b774b95dcdf"
                                               :label                     {:en "Year of completion"
                                                                           :fi "Suoritusvuosi"
                                                                           :sv "Avlagd år"}
                                               :options
                                                                          [{:followups
                                                                                   [{:belongs-to-hakukohderyhma
                                                                                                 ["1.2.246.562.28.24712637358"]
                                                                                     :belongs-to-hakukohteet
                                                                                                 ["1.2.246.562.20.43105453732"
                                                                                                  "1.2.246.562.20.70407522443"]
                                                                                     :fieldClass "formField"
                                                                                     :fieldType  "attachment"
                                                                                     :id         "candidate-predicted-grades-2"
                                                                                     :label
                                                                                                 {:en "Predicted grades"
                                                                                                  :fi
                                                                                                      "Oppilaitoksen myöntämä ennakkoarvio arvosanoista (Candidate Predicted Grades)"
                                                                                                  :sv "Predicted grades"}
                                                                                     :options    []
                                                                                     :params
                                                                                                 {:deadline nil
                                                                                                  :info-text
                                                                                                            {:enabled? true
                                                                                                             :value
                                                                                                                       {:en
                                                                                                                        "The attachment has to be submitted within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n"
                                                                                                                        :fi
                                                                                                                        "Liite täytyy tallentaa viimeistään 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n"
                                                                                                                        :sv
                                                                                                                        "Bilagan ska sparas senast 7 dygn innan ansökningstiden går ut. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}}}
                                                                                    {:belongs-to-hakukohderyhma
                                                                                                 ["1.2.246.562.28.24712637358"]
                                                                                     :belongs-to-hakukohteet
                                                                                                 ["1.2.246.562.20.43105453732"
                                                                                                  "1.2.246.562.20.70407522443"]
                                                                                     :fieldClass "formField"
                                                                                     :fieldType  "attachment"
                                                                                     :id         "diploma-programme-2"
                                                                                     :label
                                                                                                 {:en "Diploma Programme (DP) Results"
                                                                                                  :fi "Diploma Programme (DP) Results -dokumentti"
                                                                                                  :sv "Diploma Programme (DP) Results"}
                                                                                     :options    []
                                                                                     :params
                                                                                                 {:deadline "6.1.2020 15:00"
                                                                                                  :info-text
                                                                                                            {:enabled? true
                                                                                                             :value
                                                                                                                       {:en
                                                                                                                        "The attachment has to be submitted by the deadline informed next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n"
                                                                                                                        :fi
                                                                                                                        "Liitteen määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX."
                                                                                                                        :sv
                                                                                                                        "Den angivna tidpunkten för bilagan syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}}}]
                                                                            :label {:en "2019" :fi "2019" :sv "2019"}
                                                                            :value "0"}
                                                                           {:followups
                                                                                   [{:fieldClass "formField"
                                                                                     :fieldType  "textField"
                                                                                     :id         "hbem--77ea3ff1-6c04-4b3f-87d2-72bbe7db12e2"
                                                                                     :label      {:en "Year of completion"
                                                                                                  :fi "Suoritusvuosi"
                                                                                                  :sv "Avlagd år"}
                                                                                     :params     {:numeric true :size "S"}
                                                                                     :validators ["required" "numeric"]}
                                                                                    {:belongs-to-hakukohderyhma
                                                                                                 ["1.2.246.562.28.24712637358"]
                                                                                     :belongs-to-hakukohteet
                                                                                                 ["1.2.246.562.20.43105453732"
                                                                                                  "1.2.246.562.20.70407522443"]
                                                                                     :fieldClass "formField"
                                                                                     :fieldType  "attachment"
                                                                                     :id         "ib-diploma-2"
                                                                                     :label      {:en "Diploma "
                                                                                                  :fi "IB Diploma -tutkintotodistus"
                                                                                                  :sv "Diploma "}
                                                                                     :options    []
                                                                                     :params
                                                                                                 {:info-text
                                                                                                  {:enabled? true
                                                                                                   :value
                                                                                                             {:en
                                                                                                              "The attachment has to be submitted within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \nRecommended file formats are: PDF, JPG, PNG and DOCX.\n"
                                                                                                              :fi
                                                                                                              "Liite täytyy tallentaa viimeistään 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n"
                                                                                                              :sv
                                                                                                              "Bilagan ska sparas senast 7 dygn innan ansökningstiden går ut. Den angivna tidpunkten syns invid begäran om bilagor. \nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}}}]
                                                                            :label {:en "Before 2019"
                                                                                    :fi "Ennen vuotta 2019"
                                                                                    :sv "Före 2019"}
                                                                            :value "1"}]
                                               :params                    {}
                                               :validators                ["required"]}]
                                      :label {:en "International Baccalaureate -diploma"
                                              :fi "International Baccalaureate"
                                              :sv "International Baccalaureate -examen"}
                                      :value "International Baccalaureate -diploma"}
                                     {:followups
                                             [{:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                               :fieldClass                "infoElement"
                                               :fieldType                 "p"
                                               :id                        "hbem--9e87da60-6db9-40ec-a959-a0d1b00ba501"
                                               :label
                                                                          {:en
                                                                           "Fill in the grades of your international matriculation examination "
                                                                           :fi
                                                                           "Täytä kansainvälisen ylioppilastutkintosi arvosanat"
                                                                           :sv
                                                                           "Fyll i vitsorden från din internationella studentexamen"}
                                               :params                    {}
                                               :text
                                                                          {:en
                                                                           "In the drop-down menu are the grades of the Finnish matriculation examination. First you need to convert your grades with the [conversion chart](https://studyinfo.fi/wp2/en/higher-education/applying/conversion-chart-for-eb-ib-and-reifeprufung-examinations/). If you do not have a grade in the subject, choose 'No grade'."
                                                                           :fi
                                                                           "Alasvetovalikoissa on suomalaisen ylioppilastutkinnon arvosanat. Muunna ensin todistuksesi arvosanat [muuntotaulukon](https://opintopolku.fi/wp/ammattikorkeakoulu/miten-opiskelijat-valitaan/ammattikorkeakoulujen-valintaperustesuositukset-2019/#EB-,%20IB-%20ja%20Reifeprufung-tutkintojen%20muuntokaava) avulla. Jos sinulla ei ole arvosanaa kyseisestä aineesta, valitse vaihtoehto 'Ei arvosanaa'."
                                                                           :sv
                                                                           "I rullgardinsmenyn finns vitsorden för den finländska studentexamen. Omvandla först vitsorden från ditt betyg med hjälp av [omvandlingsschemat](https://studieinfo.fi/wp/yrkeshogskola/hur-antas-studerande/rekommendation-om-antagningsgrunder-for-yrkeshogskolor-ar-2019/#Omvandlingsschema) . Om du inte har ett vitsord för något ämne, välj alternativet 'Inget vitsord'."}}
                                              {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                               :fieldClass                "formField"
                                               :fieldType                 "dropdown"
                                               :id                        "hbem--56de05a3-2902-4153-a4af-cf529e60a887"
                                               :label
                                                                          {:en "Mother tongue" :fi "Äidinkieli" :sv "Modersmålet"}
                                               :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                           {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                           {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                           {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                           {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                           {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                           {:label {:en "No grade"
                                                                                    :fi "Ei arvosanaa"
                                                                                    :sv "Inget vitsord"}
                                                                            :value "6"}]
                                               :params                    {}
                                               :validators                ["required"]}
                                              {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                               :fieldClass                "formField"
                                               :fieldType                 "dropdown"
                                               :id                        "hbem--df929f7a-9b34-4eb6-92f3-768f6b49ebc4"
                                               :label                     {:en "Mathematics, advanced syllabus "
                                                                           :fi "Matematiikka, pitkä"
                                                                           :sv "Matematik, lång "}
                                               :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                           {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                           {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                           {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                           {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                           {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                           {:label {:en "No grade"
                                                                                    :fi "Ei arvosanaa"
                                                                                    :sv "Inget vitsord"}
                                                                            :value "6"}]
                                               :params                    {}
                                               :validators                ["required"]}
                                              {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                               :fieldClass                "formField"
                                               :fieldType                 "dropdown"
                                               :id                        "hbem--073290b1-e683-435d-9a01-c0c13bf71641"
                                               :label                     {:en "Mathematics, basic syllabus "
                                                                           :fi "Matematiikka, lyhyt"
                                                                           :sv "Matematik, kort "}
                                               :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                           {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                           {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                           {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                           {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                           {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                           {:label {:en "No grade"
                                                                                    :fi "Ei arvosanaa"
                                                                                    :sv "Inget vitsord"}
                                                                            :value "6"}]
                                               :params                    {}
                                               :validators                ["required"]}
                                              {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                               :fieldClass                "formField"
                                               :fieldType                 "dropdown"
                                               :id                        "hbem--69d1b998-e5f4-42ad-a9e5-ba95a48bbf68"
                                               :label                     {:en "Best advanced language syllabus"
                                                                           :fi "Paras kieli, pitkä"
                                                                           :sv "Bästa språk, långt "}
                                               :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                           {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                           {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                           {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                           {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                           {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                           {:label {:en "No grade"
                                                                                    :fi "Ei arvosanaa"
                                                                                    :sv "Inget vitsord"}
                                                                            :value "6"}]
                                               :params                    {}
                                               :validators                ["required"]}
                                              {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                               :fieldClass                "formField"
                                               :fieldType                 "dropdown"
                                               :id                        "hbem--88ccd731-2742-40a7-b4b5-c6d1c889cbd3"
                                               :label                     {:en "Best basic/intermediate language syllabus"
                                                                           :fi "Paras kieli, lyhyt/keskipitkä"
                                                                           :sv "Bästa språk, kort/mellanlångt"}
                                               :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                           {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                           {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                           {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                           {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                           {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                           {:label {:en "No grade"
                                                                                    :fi "Ei arvosanaa"
                                                                                    :sv "Inget vitsord"}
                                                                            :value "6"}]
                                               :params                    {}
                                               :validators                ["required"]}
                                              {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                               :fieldClass                "formField"
                                               :fieldType                 "dropdown"
                                               :id                        "hbem--9ebda4f0-e83d-4915-b492-673fd0a83147"
                                               :label                     {:en "Best of general studies battery tests"
                                                                           :fi "Paras reaaliaineiden kokeista"
                                                                           :sv "Bästa realämnesprov"}
                                               :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                           {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                           {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                           {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                           {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                           {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                           {:label {:en "No grade"
                                                                                    :fi "Ei arvosanaa"
                                                                                    :sv "Inget vitsord"}
                                                                            :value "6"}]
                                               :params                    {}
                                               :validators                ["required"]}
                                              {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                               :fieldClass                "formField"
                                               :fieldType                 "dropdown"
                                               :id                        "hbem--68b95a05-e3eb-4ad2-81b7-f1cc90fe4cad"
                                               :label
                                                                          {:en
                                                                               "General studies battery test in physics, chemistry or biology"
                                                                           :fi "Fysiikan, kemian tai biologian reaalikoe"
                                                                           :sv "Realprovet i fysik, kemi eller biologi"}
                                               :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                           {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                           {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                           {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                           {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                           {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                           {:label {:en "No grade"
                                                                                    :fi "Ei arvosanaa"
                                                                                    :sv "Inget vitsord"}
                                                                            :value "6"}]
                                               :params                    {}
                                               :validators                ["required"]}
                                              {:belongs-to-hakukohderyhma []
                                               :fieldClass                "formField"
                                               :fieldType                 "singleChoice"
                                               :id                        "hbem--6e980e4d-257a-49ba-a5e6-5424220e6f08"
                                               :label                     {:en "Year of completion"
                                                                           :fi "Suoritusvuosi"
                                                                           :sv "Avlagd år"}
                                               :options
                                                                          [{:followups
                                                                                   [{:belongs-to-hakukohderyhma
                                                                                                 ["1.2.246.562.28.24712637358"]
                                                                                     :belongs-to-hakukohteet
                                                                                                 ["1.2.246.562.20.43105453732"
                                                                                                  "1.2.246.562.20.70407522443"]
                                                                                     :fieldClass "formField"
                                                                                     :fieldType  "attachment"
                                                                                     :id         "oppilaitoksen-myontama-ennakkoarvio-arvosanoista-2"
                                                                                     :label
                                                                                                 {:en "Predicted grades"
                                                                                                  :fi
                                                                                                      "Oppilaitoksen myöntämä ennakkoarvio arvosanoista"
                                                                                                  :sv "Läroanstaltens preliminära vitsord"}
                                                                                     :options    []
                                                                                     :params
                                                                                                 {:deadline nil
                                                                                                  :info-text
                                                                                                            {:enabled? true
                                                                                                             :value
                                                                                                                       {:en
                                                                                                                        "The attachment has to be submitted within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \nRecommended file formats are: PDF, JPG, PNG and DOCX.\n"
                                                                                                                        :fi
                                                                                                                        "Liite täytyy tallentaa viimeistään 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n"
                                                                                                                        :sv
                                                                                                                        "Bilagan ska sparas senast 7 dygn innan ansökningstiden går ut. Den angivna tidpunkten syns invid begäran om bilagor. \nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}}}
                                                                                    {:belongs-to-hakukohderyhma
                                                                                                 ["1.2.246.562.28.24712637358"]
                                                                                     :belongs-to-hakukohteet
                                                                                                 ["1.2.246.562.20.43105453732"
                                                                                                  "1.2.246.562.20.70407522443"]
                                                                                     :fieldClass "formField"
                                                                                     :fieldType  "attachment"
                                                                                     :id         "european-baccalaureate-certificate-3"
                                                                                     :label
                                                                                                 {:en "European Baccalaureate certificate"
                                                                                                  :fi
                                                                                                      "European Baccalaureate Certificate -tutkintotodistus"
                                                                                                  :sv "European Baccalaureate certificate"}
                                                                                     :options    []
                                                                                     :params
                                                                                                 {:deadline         "30.12.2019 15:00"
                                                                                                  :info-text
                                                                                                                    {:enabled? true
                                                                                                                     :value
                                                                                                                               {:en
                                                                                                                                "The attachment has to be submitted by the deadline informed next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n"
                                                                                                                                :fi
                                                                                                                                "Liitteen määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX."
                                                                                                                                :sv
                                                                                                                                "Den angivna tidpunkten för bilagan syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."}}
                                                                                                  :mail-attachment? false}
                                                                                     :validators []}]
                                                                            :label {:en "2019" :fi "2019" :sv "2019"}
                                                                            :value "0"}
                                                                           {:followups
                                                                                   [{:fieldClass "formField"
                                                                                     :fieldType  "textField"
                                                                                     :id         "hbem--2c85ef9c-d6c2-448d-ac56-f8da4ca5c1fc"
                                                                                     :label      {:en "Year of completion"
                                                                                                  :fi "Suoritusvuosi"
                                                                                                  :sv "Avslagd år"}
                                                                                     :params     {:numeric true :size "S"}
                                                                                     :validators ["numeric" "required"]}
                                                                                    {:belongs-to-hakukohderyhma
                                                                                                 ["1.2.246.562.28.24712637358"]
                                                                                     :belongs-to-hakukohteet
                                                                                                 ["1.2.246.562.20.43105453732"
                                                                                                  "1.2.246.562.20.70407522443"]
                                                                                     :fieldClass "formField"
                                                                                     :fieldType  "attachment"
                                                                                     :id         "european-baccalaureate-certificate-4"
                                                                                     :label
                                                                                                 {:en "European Baccalaureate certificate"
                                                                                                  :fi
                                                                                                      "European Baccalaureate Certificate -tutkintotodistus"
                                                                                                  :sv "European Baccalaureate certificate"}
                                                                                     :options    []
                                                                                     :params
                                                                                                 {:info-text
                                                                                                  {:enabled? true
                                                                                                   :value
                                                                                                             {:en
                                                                                                              "The attachment has to be submitted within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n"
                                                                                                              :fi
                                                                                                              "Liite täytyy tallentaa viimeistään 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n"
                                                                                                              :sv
                                                                                                              "Bilagan ska sparas senast 7 dygn innan ansökningstiden går ut. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}}}]
                                                                            :label {:en "Before 2019"
                                                                                    :fi "Ennen vuotta 2019"
                                                                                    :sv "Före 2019"}
                                                                            :value "1"}]
                                               :params                    {}
                                               :validators                ["required"]}]
                                      :label {:en "European Baccalaureate -diploma"
                                              :fi " Eurooppalainen ylioppilastutkinto"
                                              :sv "European Baccalaureate -examen"}
                                      :value "European Baccalaureate -diploma"}
                                     {:followups
                                             [{:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                               :fieldClass                "infoElement"
                                               :fieldType                 "p"
                                               :id                        "hbem--8d111edc-c8de-4fa2-a97f-e9e40bcb4d4c"
                                               :label
                                                                          {:en
                                                                           "Fill in the grades of your international matriculation examination "
                                                                           :fi
                                                                           "Täytä kansainvälisen ylioppilastutkintosi arvosanat"
                                                                           :sv
                                                                           "Fyll i vitsorden från din internationella studentexamen"}
                                               :params                    {}
                                               :text
                                                                          {:en
                                                                           "In the drop-down menu are the grades of the Finnish matriculation examination. First you need to convert your grades with the [conversion chart](https://studyinfo.fi/wp2/en/higher-education/applying/conversion-chart-for-eb-ib-and-reifeprufung-examinations/). If you do not have a grade in the subject, choose 'No grade'."
                                                                           :fi
                                                                           "Alasvetovalikoissa on suomalaisen ylioppilastutkinnon arvosanat. Muunna ensin todistuksesi arvosanat [muuntotaulukon](https://opintopolku.fi/wp/ammattikorkeakoulu/miten-opiskelijat-valitaan/ammattikorkeakoulujen-valintaperustesuositukset-2019/#EB-,%20IB-%20ja%20Reifeprufung-tutkintojen%20muuntokaava) avulla. Jos sinulla ei ole arvosanaa kyseisestä aineesta, valitse vaihtoehto 'Ei arvosanaa'."
                                                                           :sv
                                                                           "I rullgardinsmenyn finns vitsorden för den finländska studentexamen. Omvandla först vitsorden från ditt betyg med hjälp av [omvandlingsschemat](https://studieinfo.fi/wp/yrkeshogskola/hur-antas-studerande/rekommendation-om-antagningsgrunder-for-yrkeshogskolor-ar-2019/#Omvandlingsschema) . Om du inte har ett vitsord för något ämne, välj alternativet 'Inget vitsord'."}}
                                              {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                               :fieldClass                "formField"
                                               :fieldType                 "dropdown"
                                               :id                        "hbem--49fd4d23-fc81-451a-bc13-78e509c25e4d"
                                               :label                     {:en "Mother tongue "
                                                                           :fi "Äidinkieli"
                                                                           :sv "Modersmålet "}
                                               :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                           {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                           {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                           {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                           {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                           {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                           {:label {:en "No grade"
                                                                                    :fi "Ei arvosanaa"
                                                                                    :sv "Inget vitsord"}
                                                                            :value "6"}]
                                               :params                    {}
                                               :validators                ["required"]}
                                              {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                               :fieldClass                "formField"
                                               :fieldType                 "dropdown"
                                               :id                        "hbem--6e4cd511-f769-49a5-85b4-46d6e6fe3e9b"
                                               :label                     {:en "Mathematics, advanced syllabus"
                                                                           :fi "Matematiikka, pitkä"
                                                                           :sv "Matematik, lång "}
                                               :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                           {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                           {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                           {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                           {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                           {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                           {:label {:en "No grade"
                                                                                    :fi "Ei arvosanaa"
                                                                                    :sv "Inget vitsord"}
                                                                            :value "6"}]
                                               :params                    {}
                                               :validators                ["required"]}
                                              {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                               :fieldClass                "formField"
                                               :fieldType                 "dropdown"
                                               :id                        "hbem--10a33b1d-bf27-4709-b43e-817aa74e4b84"
                                               :label                     {:en "Mathematics, basic syllabus "
                                                                           :fi "Matematiikka, lyhyt"
                                                                           :sv "Matematik, kort "}
                                               :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                           {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                           {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                           {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                           {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                           {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                           {:label {:en "No grade"
                                                                                    :fi "Ei arvosanaa"
                                                                                    :sv "Inget vitsord"}
                                                                            :value "6"}]
                                               :params                    {}
                                               :validators                ["required"]}
                                              {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                               :fieldClass                "formField"
                                               :fieldType                 "dropdown"
                                               :id                        "hbem--ec6786d0-6c17-47c7-b37f-dec21e2f1c79"
                                               :label                     {:en "Best advanced language syllabus"
                                                                           :fi "Paras kieli, pitkä"
                                                                           :sv "Bästa språk, långt "}
                                               :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                           {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                           {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                           {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                           {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                           {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                           {:label {:en "No grade"
                                                                                    :fi "Ei arvosanaa"
                                                                                    :sv "Inget vitsord"}
                                                                            :value "6"}]
                                               :params                    {}
                                               :validators                ["required"]}
                                              {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                               :fieldClass                "formField"
                                               :fieldType                 "dropdown"
                                               :id                        "hbem--55b113db-baa0-412c-aa6b-82250f9fdb80"
                                               :label                     {:en "Best basic/intermediate language syllabus"
                                                                           :fi "Paras kieli, lyhyt/keskipitkä"
                                                                           :sv "Bästa språk, kort/mellanlångt"}
                                               :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                           {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                           {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                           {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                           {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                           {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                           {:label {:en "No grade"
                                                                                    :fi "Ei arvosanaa"
                                                                                    :sv "Inget vitsord"}
                                                                            :value "6"}]
                                               :params                    {}
                                               :validators                ["required"]}
                                              {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                               :fieldClass                "formField"
                                               :fieldType                 "dropdown"
                                               :id                        "hbem--ba79e436-d76b-4e6c-ae43-11c59ada3186"
                                               :label                     {:en "Best of general studies battery tests"
                                                                           :fi "Paras reaaliaineiden kokeista"
                                                                           :sv "Bästa realämnesprov"}
                                               :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                           {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                           {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                           {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                           {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                           {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                           {:label {:en "No grade"
                                                                                    :fi "Ei arvosanaa"
                                                                                    :sv "Inget vitsord"}
                                                                            :value "6"}]
                                               :params                    {}
                                               :validators                ["required"]}
                                              {:belongs-to-hakukohderyhma ["1.2.246.562.28.51165942270"]
                                               :fieldClass                "formField"
                                               :fieldType                 "dropdown"
                                               :id                        "hbem--7b028110-766a-4646-ad3a-8bee5c905f20"
                                               :label
                                                                          {:en
                                                                               "General studies battery test in physics, chemistry or biology"
                                                                           :fi "Fysiikan, kemian tai biologian reaalikoe"
                                                                           :sv "Realprovet i fysik, kemi eller biologi"}
                                               :options                   [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
                                                                           {:label {:en "E" :fi "E" :sv "E"} :value "1"}
                                                                           {:label {:en "M" :fi "M" :sv "M"} :value "2"}
                                                                           {:label {:en "C" :fi "C" :sv "C"} :value "3"}
                                                                           {:label {:en "B" :fi "B" :sv "B"} :value "4"}
                                                                           {:label {:en "A" :fi "A" :sv "A"} :value "5"}
                                                                           {:label {:en "No grade"
                                                                                    :fi "Ei arvosanaa"
                                                                                    :sv "Inget vitsord"}
                                                                            :value "6"}]
                                               :params                    {}
                                               :validators                ["required"]}
                                              {:fieldClass "formField"
                                               :fieldType  "singleChoice"
                                               :id         "hbem--220c3b47-1ca6-47e7-8af2-2f6ff823e07b"
                                               :label      {:en "Year of completion"
                                                            :fi "Suoritusvuosi"
                                                            :sv "Avslagd år"}
                                               :options
                                                           [{:followups
                                                                    [{:belongs-to-hakukohderyhma
                                                                                  ["1.2.246.562.28.24712637358"]
                                                                      :belongs-to-hakukohteet
                                                                                  ["1.2.246.562.20.43105453732"
                                                                                   "1.2.246.562.20.70407522443"]
                                                                      :fieldClass "formField"
                                                                      :fieldType  "attachment"
                                                                      :id         "oppilaitoksen-myontama-todistus-arvosanoista-2"
                                                                      :label
                                                                                  {:en "Certificate of grades"
                                                                                   :fi
                                                                                       "Oppilaitoksen myöntämä todistus arvosanoista"
                                                                                   :sv "Läroanstaltens betyg över vitsord"}
                                                                      :options    []
                                                                      :params
                                                                                  {:deadline nil
                                                                                   :info-text
                                                                                             {:enabled? true
                                                                                              :value
                                                                                                        {:en
                                                                                                         "The attachment has to be submitted within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n"
                                                                                                         :fi
                                                                                                         "Liite täytyy tallentaa viimeistään 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n"
                                                                                                         :sv
                                                                                                         "Bilagan ska sparas senast 7 dygn innan ansökningstiden går ut. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}}}
                                                                     {:belongs-to-hakukohderyhma
                                                                                  ["1.2.246.562.28.24712637358"]
                                                                      :belongs-to-hakukohteet
                                                                                  ["1.2.246.562.20.43105453732"
                                                                                   "1.2.246.562.20.70407522443"]
                                                                      :fieldClass "formField"
                                                                      :fieldType  "attachment"
                                                                      :id         "reifeprufung-dia-tutkintotodistus-3"
                                                                      :label      {:en "Reifeprüfung/DIA -diploma"
                                                                                   :fi "Reifeprüfung/DIA-tutkintotodistus"
                                                                                   :sv "Reifeprüfung/DIA -examensbetyg"}
                                                                      :options    []
                                                                      :params
                                                                                  {:deadline "30.12.2019 15:00"
                                                                                   :info-text
                                                                                             {:enabled? true
                                                                                              :value
                                                                                                        {:en
                                                                                                         "The attachment has to be submitted by the deadline informed next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX."
                                                                                                         :fi
                                                                                                         "Liitteen määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX."
                                                                                                         :sv
                                                                                                         "Den angivna tidpunkten för bilagan syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}}}]
                                                             :label {:en "2019" :fi "2019" :sv "2019"}
                                                             :value "0"}
                                                            {:followups
                                                                    [{:fieldClass "formField"
                                                                      :fieldType  "textField"
                                                                      :id         "hbem--e70041ff-e6f4-4dc5-a87f-3267543cced4"
                                                                      :label      {:en "Year of completion"
                                                                                   :fi "Suoritusvuosi"
                                                                                   :sv "Avslagd år"}
                                                                      :params     {:numeric true :size "S"}
                                                                      :validators ["numeric" "required"]}
                                                                     {:belongs-to-hakukohderyhma
                                                                                  ["1.2.246.562.28.24712637358"]
                                                                      :belongs-to-hakukohteet
                                                                                  ["1.2.246.562.20.43105453732"
                                                                                   "1.2.246.562.20.70407522443"]
                                                                      :fieldClass "formField"
                                                                      :fieldType  "attachment"
                                                                      :id         "reifeprufung-dia-tutkintotodistus-4"
                                                                      :label      {:en "Reifeprüfung/DIA -diploma"
                                                                                   :fi "Reifeprüfung/DIA-tutkintotodistus"
                                                                                   :sv "Reifeprüfung/DIA -examensbetyg"}
                                                                      :options    []
                                                                      :params
                                                                                  {:info-text
                                                                                   {:enabled? true
                                                                                    :value
                                                                                              {:en
                                                                                               "The attachment has to be submitted within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n"
                                                                                               :fi
                                                                                               "Liite täytyy tallentaa viimeistään 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n"
                                                                                               :sv
                                                                                               "Bilagan ska sparas senast 7 dygn innan ansökningstiden går ut. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}}}]
                                                             :label
                                                                    {:en "Before 2019" :fi "Ennen 2019" :sv "Före 2019"}
                                                             :value "1"}]
                                               :params     {}}]
                                      :label
                                             {:en
                                                  "Reifeprüfung - diploma/ Deutsche Internationale Abiturprüfung"
                                              :fi "Reifeprüfung/Deutsche Internationale Abiturprüfung"
                                              :sv
                                                  "Reifeprüfung - examen/Deutsche Internationale Abiturprüfung"}
                                      :value "Reifeprüfung - diploma"}]
                        :params     {}
                        :validators ["required"]}
                       {:fieldClass "formField"
                        :fieldType  "textField"
                        :id         "pohjakoulutus_yo_ulkomainen--institution"
                        :label      {:en "Educational institution"
                                     :fi "Oppilaitos"
                                     :sv "Läroanstalt"}
                        :params     {}
                        :validators ["required"]}
                       {:fieldClass "formField"
                        :fieldType  "dropdown"
                        :id         "pohjakoulutus_yo_ulkomainen--country-of-completion"
                        :koodisto-source
                                    {:title "Maat ja valtiot" :uri "maatjavaltiot2" :version 1}
                        :label      {:en "Country of completion"
                                     :fi "Suoritusmaa"
                                     :sv "Land där examen är avlagd"}
                        :options
                                    [{:label {:en "Afghanistan" :fi "Afganistan" :sv "Afghanistan"}
                                      :value "004"}
                                     {:label {:en "Åland Islands" :fi "Ahvenanmaa" :sv "Åland"}
                                      :value "248"}
                                     {:label {:en "Netherlands" :fi "Alankomaat" :sv "Nederländerna"}
                                      :value "528"}
                                     {:label {:en "Albania" :fi "Albania" :sv "Albanien"}
                                      :value "008"}
                                     {:label {:en "Algeria" :fi "Algeria" :sv "Algeriet"}
                                      :value "012"}
                                     {:label {:en "American Samoa"
                                              :fi "Amerikan Samoa"
                                              :sv "Amerikanska Samoa"}
                                      :value "016"}
                                     {:label {:en "Andorra" :fi "Andorra" :sv "Andorra"}
                                      :value "020"}
                                     {:label {:en "Angola" :fi "Angola" :sv "Angola"} :value "024"}
                                     {:label {:en "Anguilla" :fi "Anguilla" :sv "Anguilla"}
                                      :value "660"}
                                     {:label {:en "Antarctica" :fi "Antarktis" :sv "Antarktis"}
                                      :value "010"}
                                     {:label {:en "Antigua and Barbuda"
                                              :fi "Antigua ja Barbuda"
                                              :sv "Antigua och Barbuda"}
                                      :value "028"}
                                     {:label {:en "United Arab Emirates"
                                              :fi "Arabiemiirikunnat"
                                              :sv "Förenade Arabemirate"}
                                      :value "784"}
                                     {:label {:en "Argentina" :fi "Argentiina" :sv "Argentina"}
                                      :value "032"}
                                     {:label {:en "Armenia" :fi "Armenia" :sv "Armenien"}
                                      :value "051"}
                                     {:label {:en "Aruba" :fi "Aruba" :sv "Aruba"} :value "533"}
                                     {:label {:en "Australia" :fi "Australia" :sv "Australien"}
                                      :value "036"}
                                     {:label {:en "Azerbaijan" :fi "Azerbaidzan" :sv "Azerbajdzjan"}
                                      :value "031"}
                                     {:label {:en "Bahamas" :fi "Bahama" :sv "Bahamas"} :value "044"}
                                     {:label {:en "Bahrain" :fi "Bahrain" :sv "Bahrain"}
                                      :value "048"}
                                     {:label {:en "Bangladesh" :fi "Bangladesh" :sv "Bangladesh"}
                                      :value "050"}
                                     {:label {:en "Barbados" :fi "Barbados" :sv "Barbados"}
                                      :value "052"}
                                     {:label {:en "Belgium" :fi "Belgia" :sv "Belgien"} :value "056"}
                                     {:label {:en "Belize" :fi "Belize" :sv "Belize"} :value "084"}
                                     {:label {:en "Benin" :fi "Benin" :sv "Benin"} :value "204"}
                                     {:label {:en "Bermuda" :fi "Bermuda" :sv "Bermuda"}
                                      :value "060"}
                                     {:label {:en "Bhutan" :fi "Bhutan" :sv "Bhutan"} :value "064"}
                                     {:label {:en "Bolivia" :fi "Bolivia" :sv "Bolivia"}
                                      :value "068"}
                                     {:label {:en "Bonaire, Sint Eustatius and Saba"
                                              :fi "Bonaire,Sint Eustatius ja Saba"
                                              :sv "Bonaire,Sint Eustatius ja Saba"}
                                      :value "535"}
                                     {:label {:en "Bosnia and Herzegovina"
                                              :fi "Bosnia ja Hertsegovina"
                                              :sv "Bosnien och Hercegovina"}
                                      :value "070"}
                                     {:label {:en "Botswana" :fi "Botswana" :sv "Botswana"}
                                      :value "072"}
                                     {:label {:en "Bouvet Island" :fi "Bouvet'nsaari" :sv "Bouvetön"}
                                      :value "074"}
                                     {:label {:en "Brazil" :fi "Brasilia" :sv "Brasilien"}
                                      :value "076"}
                                     {:label
                                             {:en "United Kingdom" :fi "Britannia" :sv "Storbritannien"}
                                      :value "826"}
                                     {:label {:en "British Indian Ocean Territory"
                                              :fi "Brittiläinen Intian valtameren alue"
                                              :sv "Brittiska territoriet i Indiska Oceanen"}
                                      :value "086"}
                                     {:label {:en "Virgin Island, British"
                                              :fi "Brittiläiset Neitsytsaaret"
                                              :sv "Brittiska Jungfruöarna"}
                                      :value "092"}
                                     {:label {:en "Brunei" :fi "Brunei" :sv "Brunei"} :value "096"}
                                     {:label {:en "Bulgaria" :fi "Bulgaria" :sv "Bulgarien"}
                                      :value "100"}
                                     {:label
                                             {:en "Burkina Faso" :fi "Burkina Faso" :sv "Burkina Faso"}
                                      :value "854"}
                                     {:label {:en "Burundi" :fi "Burundi" :sv "Burundi"}
                                      :value "108"}
                                     {:label
                                             {:en "Cayman Islands" :fi "Caymansaaret" :sv "Caymanöarna"}
                                      :value "136"}
                                     {:label {:en "Chile" :fi "Chile" :sv "Chile"} :value "152"}
                                     {:label {:en "Cook Islands" :fi "Cookinsaaret" :sv "Cooköarna"}
                                      :value "184"}
                                     {:label {:en "Costa Rica" :fi "Costa Rica" :sv "Costa Rica"}
                                      :value "188"}
                                     {:label {:en "Curaçao" :fi "Curacao" :sv "Curacao"}
                                      :value "531"}
                                     {:label {:en "Djibouti" :fi "Djibouti" :sv "Djibouti"}
                                      :value "262"}
                                     {:label {:en "Dominica" :fi "Dominica" :sv "Dominica"}
                                      :value "212"}
                                     {:label {:en "Dominican Republic"
                                              :fi "Dominikaaninen tasavalta"
                                              :sv "Dominikanska republiken"}
                                      :value "214"}
                                     {:label {:en "Ecuador" :fi "Ecuador" :sv "Ecuador"}
                                      :value "218"}
                                     {:label {:en "Egypt" :fi "Egypti" :sv "Egypten"} :value "818"}
                                     {:label {:en "El Salvador" :fi "El Salvador" :sv "El Salvador"}
                                      :value "222"}
                                     {:label {:en "Eritrea" :fi "Eritrea" :sv "Eritrea"}
                                      :value "232"}
                                     {:label {:en "Spain" :fi "Espanja" :sv "Spanien"} :value "724"}
                                     {:label {:en "South Africa" :fi "Etelä-Afrikka" :sv "Sydafrika"}
                                      :value "710"}
                                     {:label {:en "South Georgia and the South Sandwich Islands"
                                              :fi "Etelä-Georgia ja Eteläiset Sandwichsaaret"
                                              :sv "Sydgeorgien och Sydsandwichöarna"}
                                      :value "239"}
                                     {:label {:en "South Sudan" :fi "Etelä-Sudan" :sv "Etelä-Sudan"}
                                      :value "728"}
                                     {:label {:en "Ethiopia" :fi "Etiopia" :sv "Etiopien"}
                                      :value "231"}
                                     {:label {:en "Falkland Islands"
                                              :fi "Falklandinsaaret"
                                              :sv "Falklandsöarna"}
                                      :value "238"}
                                     {:label {:en "Fiji" :fi "Fidzi" :sv "Fiji"} :value "242"}
                                     {:label {:en "Philippines" :fi "Filippiinit" :sv "Filippinerna"}
                                      :value "608"}
                                     {:label {:en "Faroe Islands" :fi "Färsaaret" :sv "Färöarna"}
                                      :value "234"}
                                     {:label {:en "Gabon" :fi "Gabon" :sv "Gabon"} :value "266"}
                                     {:label {:en "Gambia" :fi "Gambia" :sv "Gambia"} :value "270"}
                                     {:label {:en "Georgia" :fi "Georgia" :sv "Georgien"}
                                      :value "268"}
                                     {:label {:en "Ghana" :fi "Ghana" :sv "Ghana"} :value "288"}
                                     {:label {:en "Gibraltar" :fi "Gibraltar" :sv "Gibraltar"}
                                      :value "292"}
                                     {:label {:en "Grenada" :fi "Grenada" :sv "Grenada"}
                                      :value "308"}
                                     {:label {:en "Greenland" :fi "Grönlanti" :sv "Grönland"}
                                      :value "304"}
                                     {:label {:en "Guadeloupe" :fi "Guadeloupe" :sv "Guadeloupe"}
                                      :value "312"}
                                     {:label {:en "Guam" :fi "Guam" :sv "Guam"} :value "316"}
                                     {:label {:en "Guatemala" :fi "Guatemala" :sv "Guatemala"}
                                      :value "320"}
                                     {:label {:en "Guernsey" :fi "Guernsey" :sv "Guernsey"}
                                      :value "831"}
                                     {:label {:en "Guinea" :fi "Guinea" :sv "Guinea"} :value "324"}
                                     {:label
                                             {:en "Guinea-Bissau" :fi "Guinea-Bissau" :sv "Guinea-Bissau"}
                                      :value "624"}
                                     {:label {:en "Guyana" :fi "Guyana" :sv "Guyana"} :value "328"}
                                     {:label {:en "Haiti" :fi "Haiti" :sv "Haiti"} :value "332"}
                                     {:label {:en "Heard Island and McDonald Islands"
                                              :fi "Heard ja McDonaldinsaaret"
                                              :sv "Heard och McDonaldöarna"}
                                      :value "334"}
                                     {:label {:en "Honduras" :fi "Honduras" :sv "Honduras"}
                                      :value "340"}
                                     {:label {:en "Hong Kong" :fi "Hongkong" :sv "Hongkong"}
                                      :value "344"}
                                     {:label {:en "Without Citizenship"
                                              :fi "Ilman kansalaisuutta"
                                              :sv "Utan nationalitet"}
                                      :value "998"}
                                     {:label {:en "Without nationality"
                                              :fi "Ilman kansalaisuutta"
                                              :sv "Utan medborgarskap"}
                                      :value "991"}
                                     {:label {:en "Indonesia" :fi "Indonesia" :sv "Indonesien"}
                                      :value "360"}
                                     {:label {:en "India" :fi "Intia" :sv "Indien"} :value "356"}
                                     {:label {:en "Iraq" :fi "Irak" :sv "Irak"} :value "368"}
                                     {:label {:en "Iran" :fi "Iran" :sv "Iran"} :value "364"}
                                     {:label {:en "Ireland" :fi "Irlanti" :sv "Irland"} :value "372"}
                                     {:label {:en "Iceland" :fi "Islanti" :sv "Island"} :value "352"}
                                     {:label {:en "Israel" :fi "Israel" :sv "Israel"} :value "376"}
                                     {:label {:en "Italy" :fi "Italia" :sv "Italien"} :value "380"}
                                     {:label {:en "Timor-Leste (East Timor)"
                                              :fi "Itä-Timor"
                                              :sv "Östtimor"}
                                      :value "626"}
                                     {:label {:en "Austria" :fi "Itävalta" :sv "Österrike"}
                                      :value "040"}
                                     {:label {:en "Jamaica" :fi "Jamaika" :sv "Jamaica"}
                                      :value "388"}
                                     {:label {:en "Japan" :fi "Japani" :sv "Japan"} :value "392"}
                                     {:label {:en "Yemen" :fi "Jemen" :sv "Jemen"} :value "887"}
                                     {:label {:en "Jersey" :fi "Jersey" :sv "Jersey"} :value "832"}
                                     {:label {:en "Jordan" :fi "Jordania" :sv "Jordanien"}
                                      :value "400"}
                                     {:label {:en "Christmas Island" :fi "Joulusaari" :sv "Julön"}
                                      :value "162"}
                                     {:label {:en "Cambodia" :fi "Kambodza" :sv "Kambodja"}
                                      :value "116"}
                                     {:label {:en "Cameroon" :fi "Kamerun" :sv "Kamerun"}
                                      :value "120"}
                                     {:label {:en "Canada" :fi "Kanada" :sv "Kanada"} :value "124"}
                                     {:label {:en "Capo Verde" :fi "Kap Verde" :sv "Kap Verde"}
                                      :value "132"}
                                     {:label {:en "Kazakhstan" :fi "Kazakstan" :sv "Kazakstan"}
                                      :value "398"}
                                     {:label {:en "Kenya" :fi "Kenia" :sv "Kenya"} :value "404"}
                                     {:label {:en "Central African Republic"
                                              :fi "Keski-Afrikkan tasavalta"
                                              :sv "Centralafrikanska republiken"}
                                      :value "140"}
                                     {:label {:en "China" :fi "Kiina" :sv "Kina"} :value "156"}
                                     {:label {:en "Kyrgyzstan" :fi "Kirgisia" :sv "Kirgizistan"}
                                      :value "417"}
                                     {:label {:en "Kiribati" :fi "Kiribati" :sv "Kiribati"}
                                      :value "296"}
                                     {:label {:en "Colombia" :fi "Kolumbia" :sv "Columbia"}
                                      :value "170"}
                                     {:label {:en "Comoros" :fi "Komorit" :sv "Komorerna"}
                                      :value "174"}
                                     {:label {:en "Congo"
                                              :fi "Kongo (Kongo-Brazzaville)"
                                              :sv "Kongo (Kongo-Brazzaville)"}
                                      :value "178"}
                                     {:label {:en "Congo, The Democratic Republic of the"
                                              :fi "Kongo (Kongo-Kinshasa)"
                                              :sv "Kongo (Kongo-Kinshasa)"}
                                      :value "180"}
                                     {:label {:en "Cocos (Keeling) Islands"
                                              :fi "Kookossaaret"
                                              :sv "Cocosöarna"}
                                      :value "166"}
                                     {:label
                                             {:en "Korea, Democratic People's Republic of (North Korea)"
                                              :fi "Korean demokraattinen kansantasavalta (Pohjois-Korea)"
                                              :sv "Demokratiska folkrepubliken Korea (Nordkorea)"}
                                      :value "408"}
                                     {:label {:en "Korea, Republic of (South Korea)"
                                              :fi "Korean Tasavalta (Etelä-Korea)"
                                              :sv "Republiken Korea (Sydkorea)"}
                                      :value "410"}
                                     {:label {:en "Kosovo" :fi "Kosovo" :sv "Kosovo"} :value "907"}
                                     {:label {:en "Kosovo" :fi "Kosovo" :sv "Kosovo"} :value "997"}
                                     {:label {:en "Greece" :fi "Kreikka" :sv "Grekland"}
                                      :value "300"}
                                     {:label {:en "Croatia" :fi "Kroatia" :sv "Kroatien"}
                                      :value "191"}
                                     {:label {:en "Cuba" :fi "Kuuba" :sv "Kuba"} :value "192"}
                                     {:label {:en "Kuwait" :fi "Kuwait" :sv "Kuwait"} :value "414"}
                                     {:label {:en "Cyprus" :fi "Kypros" :sv "Cypern"} :value "196"}
                                     {:label {:en "Laos" :fi "Laos" :sv "Laos"} :value "418"}
                                     {:label {:en "Latvia" :fi "Latvia" :sv "Lettland"} :value "428"}
                                     {:label {:en "Lesotho" :fi "Lesotho" :sv "Lesotho"}
                                      :value "426"}
                                     {:label {:en "Lebanon" :fi "Libanon" :sv "Libanon"}
                                      :value "422"}
                                     {:label {:en "Liberia" :fi "Liberia" :sv "Liberia"}
                                      :value "430"}
                                     {:label {:en "Libyan Arab Jamahiriya" :fi "Libya" :sv "Libyen"}
                                      :value "434"}
                                     {:label
                                             {:en "Liechtenstein" :fi "Liechtenstein" :sv "Liechtenstein"}
                                      :value "438"}
                                     {:label {:en "Lithuania" :fi "Liettua" :sv "Litauen"}
                                      :value "440"}
                                     {:label {:en "Luxembourg" :fi "Luxemburg" :sv "Luxemburg"}
                                      :value "442"}
                                     {:label
                                             {:en "Western Sahara" :fi "Länsi-Sahara" :sv "Västsahara"}
                                      :value "732"}
                                     {:label {:en "Macao" :fi "Macao" :sv "Macao"} :value "446"}
                                     {:label {:en "Madagascar" :fi "Madagaskar" :sv "Madagaskar"}
                                      :value "450"}
                                     {:label {:en "Macedonia" :fi "Makedonia" :sv "Makedonien"}
                                      :value "807"}
                                     {:label {:en "Malawi" :fi "Malawi" :sv "Malawi"} :value "454"}
                                     {:label {:en "Maldives" :fi "Malediivit" :sv "Maldiverna"}
                                      :value "462"}
                                     {:label {:en "Malaysia" :fi "Malesia" :sv "Malaysia"}
                                      :value "458"}
                                     {:label {:en "Mali" :fi "Mali" :sv "Mali"} :value "466"}
                                     {:label {:en "Malta" :fi "Malta" :sv "Malta"} :value "470"}
                                     {:label {:en "Isle of Man" :fi "Mansaari" :sv "Mansaari"}
                                      :value "833"}
                                     {:label {:en "Morocco" :fi "Marokko" :sv "Marocko"}
                                      :value "504"}
                                     {:label {:en "Marshall Islands"
                                              :fi "Marshallinsaaret"
                                              :sv "Marshallöarna"}
                                      :value "584"}
                                     {:label {:en "Martinique" :fi "Martinique" :sv "Martinique"}
                                      :value "474"}
                                     {:label {:en "Mauritania" :fi "Mauritania" :sv "Mauretanien"}
                                      :value "478"}
                                     {:label {:en "Mauritius" :fi "Mauritius" :sv "Mauritius"}
                                      :value "480"}
                                     {:label {:en "Mayotte" :fi "Mayotte" :sv "Mayotte"}
                                      :value "175"}
                                     {:label {:en "Mexico" :fi "Meksiko" :sv "Mexiko"} :value "484"}
                                     {:label {:en "Micronesia, Federated States of"
                                              :fi "Mikronesia"
                                              :sv "Mikronesien"}
                                      :value "583"}
                                     {:label {:en "Moldova" :fi "Moldova" :sv "Moldavien"}
                                      :value "498"}
                                     {:label {:en "Monaco" :fi "Monaco" :sv "Monaco"} :value "492"}
                                     {:label {:en "Mongolia" :fi "Mongolia" :sv "Mongoliet"}
                                      :value "496"}
                                     {:label {:en "Montenegro" :fi "Montenegro" :sv "Montenegro"}
                                      :value "499"}
                                     {:label {:en "Montserrat" :fi "Montserrat" :sv "Montserrat"}
                                      :value "500"}
                                     {:label {:en "Mozambique" :fi "Mosambik" :sv "Mosambique"}
                                      :value "508"}
                                     {:label {:en "Other dependent country"
                                              :fi "Muu epäitsenäinen alue"
                                              :sv "Annat icke-suveränt område"}
                                      :value "990"}
                                     {:label {:en "Myanmar" :fi "Myanmar" :sv "Myanmar"}
                                      :value "104"}
                                     {:label {:en "Namibia" :fi "Namibia" :sv "Namibia"}
                                      :value "516"}
                                     {:label {:en "Nauru" :fi "Nauru" :sv "Nauru"} :value "520"}
                                     {:label {:en "Nepal" :fi "Nepal" :sv "Nepal"} :value "524"}
                                     {:label {:en "Nicaragua" :fi "Nicaragua" :sv "Nicaragua"}
                                      :value "558"}
                                     {:label {:en "Niger" :fi "Niger" :sv "Niger"} :value "562"}
                                     {:label {:en "Nigeria" :fi "Nigeria" :sv "Nigeria"}
                                      :value "566"}
                                     {:label {:en "Niue" :fi "Niue" :sv "Niue"} :value "570"}
                                     {:label
                                             {:en "Norfolk Island" :fi "Norfolkinsaari" :sv "Norfolkön"}
                                      :value "574"}
                                     {:label {:en "Norway" :fi "Norja" :sv "Norge"} :value "578"}
                                     {:label {:en "Côte d'lvoire"
                                              :fi "Norsunluurannikko"
                                              :sv "Elfenbenskusten"}
                                      :value "384"}
                                     {:label {:en "Oman" :fi "Oman" :sv "Oman"} :value "512"}
                                     {:label {:en "Pakistan" :fi "Pakistan" :sv "Pakistan"}
                                      :value "586"}
                                     {:label {:en "Palau" :fi "Palau" :sv "Palau"} :value "585"}
                                     {:label {:en "Palestine"
                                              :fi "Palestiinan valtio"
                                              :sv "De palestinska områdena"}
                                      :value "275"}
                                     {:label {:en "Panama" :fi "Panama" :sv "Panama"} :value "591"}
                                     {:label {:en "Papua New Guinea"
                                              :fi "Papua-Uusi-Guinea"
                                              :sv "Papua Nya Guinea"}
                                      :value "598"}
                                     {:label {:en "Paraguay" :fi "Paraguay" :sv "Paraguay"}
                                      :value "600"}
                                     {:label {:en "Peru" :fi "Peru" :sv "Peru"} :value "604"}
                                     {:label {:en "Pitcairn" :fi "Pitcairn" :sv "Pitcairn"}
                                      :value "612"}
                                     {:label {:en "Northern Mariana Islands"
                                              :fi "Pohjois-Mariaanit"
                                              :sv "Nordmarianerna"}
                                      :value "580"}
                                     {:label {:en "Portugal" :fi "Portugali" :sv "Portugal"}
                                      :value "620"}
                                     {:label {:en "Puerto Rico" :fi "Puerto Rico" :sv "Puerto Rico"}
                                      :value "630"}
                                     {:label {:en "Poland" :fi "Puola" :sv "Polen"} :value "616"}
                                     {:label {:en "Equatorial Guinea"
                                              :fi "Päiväntasaajan Guinea"
                                              :sv "Ekvatorialguinea"}
                                      :value "226"}
                                     {:label {:en "Qatar" :fi "Qatar" :sv "Qatar"} :value "634"}
                                     {:label {:en "France" :fi "Ranska" :sv "Frankrike"}
                                      :value "250"}
                                     {:label {:en "French Southern Territories"
                                              :fi "Ranskan eteläiset alueet"
                                              :sv "De franska territorierna i södra Indiska Oceanen"}
                                      :value "260"}
                                     {:label {:en "French Guiana"
                                              :fi "Ranskan Guayana"
                                              :sv "Franska Guyana"}
                                      :value "254"}
                                     {:label {:en "French Plynesia"
                                              :fi "Ranskan Polynesia"
                                              :sv "Franska Polynesien"}
                                      :value "258"}
                                     {:label {:en "Romania" :fi "Romania" :sv "Rumänien"}
                                      :value "642"}
                                     {:label {:en "Rwanda" :fi "Ruanda" :sv "Rwanda"} :value "646"}
                                     {:label {:en "Sweden" :fi "Ruotsi" :sv "Sverige"} :value "752"}
                                     {:label {:en "Réunion" :fi "Réunion" :sv "Réunion"}
                                      :value "638"}
                                     {:label {:en "Saint Barthélemy"
                                              :fi "Saint Barthélemy"
                                              :sv "Saint Barthélemy"}
                                      :value "652"}
                                     {:label
                                             {:en "Saint Helena" :fi "Saint Helena" :sv "Saint Helena"}
                                      :value "654"}
                                     {:label {:en "Saint Kitts and Nevis"
                                              :fi "Saint Kitts ja Nevis"
                                              :sv "Saint Kitts och Nevis"}
                                      :value "659"}
                                     {:label {:en "Saint Lucia" :fi "Saint Lucia" :sv "Saint Lucia"}
                                      :value "662"}
                                     {:label {:en "Saint Martin (French Part)"
                                              :fi "Saint Martin (Ranska)"
                                              :sv "Saint Martin (Ranska)"}
                                      :value "663"}
                                     {:label {:en "Saint Vincent and the Grenadines"
                                              :fi "Saint Vincent ja Grenadiinit"
                                              :sv "Saint Vincent och Grenadinerna"}
                                      :value "670"}
                                     {:label {:en "Saint Pierre and Miquelon"
                                              :fi "Saint-Pierre ja Miquelon"
                                              :sv "Saint Pierre och Miquelon"}
                                      :value "666"}
                                     {:label {:en "Germany" :fi "Saksa" :sv "Tyskland"} :value "276"}
                                     {:label {:en "Solomon Islands"
                                              :fi "Salomonsaaret"
                                              :sv "Salomonöarna"}
                                      :value "090"}
                                     {:label {:en "Zambia" :fi "Sambia" :sv "Zambia"} :value "894"}
                                     {:label {:en "Samoa" :fi "Samoa" :sv "Samoa"} :value "882"}
                                     {:label {:en "San Marino" :fi "San Marino" :sv "San Marino"}
                                      :value "674"}
                                     {:label
                                             {:en "Saudi Arabia" :fi "Saudi-Arabia" :sv "Saudiarabien"}
                                      :value "682"}
                                     {:label {:en "Senegal" :fi "Senegal" :sv "Senegal"}
                                      :value "686"}
                                     {:label {:en "Serbia" :fi "Serbia" :sv "Serbien"} :value "688"}
                                     {:label {:en "Seychelles" :fi "Seychellit" :sv "Seychellerna"}
                                      :value "690"}
                                     {:label
                                             {:en "Sierra Leone" :fi "Sierra Leone" :sv "Sierra Leone"}
                                      :value "694"}
                                     {:label {:en "Singapore" :fi "Singapore" :sv "Singapore"}
                                      :value "702"}
                                     {:label {:en "Sint Maarten (Dutch part)"
                                              :fi "Sint Maarten(Alankomaat)"
                                              :sv "Sint Maarten(Alankomaat)"}
                                      :value "534"}
                                     {:label {:en "Slovakia" :fi "Slovakia" :sv "Slovakien"}
                                      :value "703"}
                                     {:label {:en "Slovenia" :fi "Slovenia" :sv "Slovenien"}
                                      :value "705"}
                                     {:label {:en "Somalia" :fi "Somalia" :sv "Somalia"}
                                      :value "706"}
                                     {:label {:en "Sri Lanka" :fi "Sri Lanka" :sv "Sri Lanka"}
                                      :value "144"}
                                     {:label {:en "Sudan" :fi "Sudan" :sv "Sudan"} :value "729"}
                                     {:label {:en "Finland" :fi "Suomi" :sv "Finland"} :value "246"}
                                     {:label {:en "Suriname" :fi "Suriname" :sv "Suninam"}
                                      :value "740"}
                                     {:label {:en "Svalbard and Jan Mayen"
                                              :fi "Svalbard ja Jan Mayen"
                                              :sv "Svalbard och Jan Mayen"}
                                      :value "744"}
                                     {:label {:en "Switzerland" :fi "Sveitsi" :sv "Schweiz"}
                                      :value "756"}
                                     {:label {:en "Swaziland" :fi "Swazimaa" :sv "Swaziland"}
                                      :value "748"}
                                     {:label {:en "Syria" :fi "Syyria" :sv "Syrien"} :value "760"}
                                     {:label {:en "São Tomé and Príncipe"
                                              :fi "São Tomé ja Príncipe"
                                              :sv "São Tomé och Príncipe"}
                                      :value "678"}
                                     {:label {:en "Tajikistan" :fi "Tadzikistan" :sv "Tadzjikistan"}
                                      :value "762"}
                                     {:label
                                             {:en "Taiwan, Province of China" :fi "Taiwan" :sv "Taiwan"}
                                      :value "158"}
                                     {:label {:en "Tanzania" :fi "Tansania" :sv "Tanzania"}
                                      :value "834"}
                                     {:label {:en "Denmark" :fi "Tanska" :sv "Danmark"} :value "208"}
                                     {:label {:en "Thailand" :fi "Thaimaa" :sv "Thailand"}
                                      :value "764"}
                                     {:label {:en "Togo" :fi "Togo" :sv "Togo"} :value "768"}
                                     {:label {:en "Tokelau" :fi "Tokelau" :sv "Tokelau"}
                                      :value "772"}
                                     {:label {:en "Tonga" :fi "Tonga" :sv "Tonga"} :value "776"}
                                     {:label {:en "Trinidad and Tobago"
                                              :fi "Trinidad ja Tobago"
                                              :sv "Trinidad och Tobago"}
                                      :value "780"}
                                     {:label {:en "Tunisia" :fi "Tunisia" :sv "Tunisien"}
                                      :value "788"}
                                     {:label {:en "Unknown" :fi "Tuntematon" :sv "Okänt land"}
                                      :value "999"}
                                     {:label {:en "Turkey" :fi "Turkki" :sv "Turkiet"} :value "792"}
                                     {:label
                                             {:en "Turkmenistan" :fi "Turkmenistan" :sv "Turkmenistan"}
                                      :value "795"}
                                     {:label {:en "Turks and Caicos Islands"
                                              :fi "Turks- ja Caicossaaret"
                                              :sv "Turks- och Caicosöarna"}
                                      :value "796"}
                                     {:label {:en "Tuvalu" :fi "Tuvalu" :sv "Tuvalu"} :value "798"}
                                     {:label {:en "Chad" :fi "Tšad" :sv "Tchad"} :value "148"}
                                     {:label {:en "Czech Republic" :fi "Tšekki" :sv "Tjeckien"}
                                      :value "203"}
                                     {:label {:en "Uganda" :fi "Uganda" :sv "Uganda"} :value "800"}
                                     {:label {:en "Ukraine" :fi "Ukraina" :sv "Ukraina"}
                                      :value "804"}
                                     {:label {:en "Hungary" :fi "Unkari" :sv "Ungern"} :value "348"}
                                     {:label {:en "Uruguay" :fi "Uruguay" :sv "Uruguay"}
                                      :value "858"}
                                     {:label {:en "New Caledonia"
                                              :fi "Uusi-Kaledonia"
                                              :sv "Nya Kaledonien"}
                                      :value "540"}
                                     {:label
                                             {:en "New Zealand" :fi "Uusi-Seelanti" :sv "Nya Zeeland"}
                                      :value "554"}
                                     {:label {:en "Uzbekistan" :fi "Uzbekistan" :sv "Uzbekistan"}
                                      :value "860"}
                                     {:label {:en "Belarus" :fi "Valko-Venäjä" :sv "Vitryssland"}
                                      :value "112"}
                                     {:label {:en "Vanuatu" :fi "Vanuatu" :sv "Vanuatu"}
                                      :value "548"}
                                     {:label {:en "Holy See (Vatican City State)"
                                              :fi "Vatikaani"
                                              :sv "Vatikanstaten"}
                                      :value "336"}
                                     {:label {:en "Venezuela" :fi "Venezuela" :sv "Venezuela"}
                                      :value "862"}
                                     {:label {:en "Russian Federation" :fi "Venäjä" :sv "Ryssland"}
                                      :value "643"}
                                     {:label {:en "Viet Nam" :fi "Vietnam" :sv "Vietnam"}
                                      :value "704"}
                                     {:label {:en "Estonia" :fi "Viro" :sv "Estland"} :value "233"}
                                     {:label {:en "Wallis and Futuna"
                                              :fi "Wallis ja Futuna"
                                              :sv "Wallis och Futuna"}
                                      :value "876"}
                                     {:label {:en "United States, USA"
                                              :fi "Yhdysvallat (USA)"
                                              :sv "Förenta Staterna (USA)"}
                                      :value "840"}
                                     {:label {:en "Virgin Islands, U.S."
                                              :fi "Yhdysvaltain Neitsytsaaret"
                                              :sv "Amerikanska Jungfruöarna"}
                                      :value "850"}
                                     {:label
                                             {:en "United States Minor Outlying Islands"
                                              :fi "Yhdysvaltain pienet erillissaaret"
                                              :sv
                                                  "Förenta Staternas mindre öar i Oceanien och Västindien"}
                                      :value "581"}
                                     {:label {:en "Zimbabwe" :fi "Zimbabwe" :sv "Zimbabwe"}
                                      :value "716"} {:label {:fi "" :sv ""} :value ""}]
                        :params     {}
                        :validators ["required"]}]
               :label
                      {:en
                       "International matriculation examination completed outside Finland (IB, EB and RP/DIA)"
                       :fi
                       "Muualla kuin Suomessa suoritettu kansainvälinen ylioppilastutkinto (IB, EB ja RP/DIA)"
                       :sv
                       "Internationell studentexamen som avlagts annanstans än i Finland (IB, EB och RP/DIA)"}
               :value "pohjakoulutus_yo_ulkomainen"}
              {:followups
                      [{:children
                                    [{:belongs-to-hakukohderyhma []
                                      :belongs-to-hakukohteet    []
                                      :fieldClass                "formField"
                                      :fieldType                 "textField"
                                      :id                        "pohjakoulutus_ulk--year-of-completion"
                                      :label                     {:en "Year of completion"
                                                                  :fi "Suoritusvuosi"
                                                                  :sv "Avlagd år"}
                                      :params                    {:numeric true :size "S"}
                                      :validators                ["required" "numeric"]}
                                     {:belongs-to-hakukohteet []
                                      :fieldClass             "formField"
                                      :fieldType              "textField"
                                      :id                     "pohjakoulutus_ulk--degree"
                                      :label                  {:en "Qualification" :fi "Tutkinto" :sv "Examen"}
                                      :params                 {}
                                      :validators             ["required"]}
                                     {:belongs-to-hakukohteet []
                                      :fieldClass             "formField"
                                      :fieldType              "textField"
                                      :id                     "pohjakoulutus_ulk--institution"
                                      :label                  {:en "Educational institution"
                                                               :fi "Oppilaitos"
                                                               :sv "Läroanstalt"}
                                      :params                 {}
                                      :validators             ["required"]}
                                     {:belongs-to-hakukohteet []
                                      :fieldClass             "formField"
                                      :fieldType              "dropdown"
                                      :id                     "pohjakoulutus_ulk--country-of-completion"
                                      :koodisto-source
                                                              {:title "Maat ja valtiot" :uri "maatjavaltiot2" :version 1}
                                      :label                  {:en "Country of completion"
                                                               :fi "Suoritusmaa"
                                                               :sv "Land där examen är avlagd"}
                                      :options
                                                              [{:label
                                                                       {:en "Afghanistan" :fi "Afganistan" :sv "Afghanistan"}
                                                                :value "004"}
                                                               {:label {:en "Åland Islands" :fi "Ahvenanmaa" :sv "Åland"}
                                                                :value "248"}
                                                               {:label
                                                                       {:en "Netherlands" :fi "Alankomaat" :sv "Nederländerna"}
                                                                :value "528"}
                                                               {:label {:en "Albania" :fi "Albania" :sv "Albanien"}
                                                                :value "008"}
                                                               {:label {:en "Algeria" :fi "Algeria" :sv "Algeriet"}
                                                                :value "012"}
                                                               {:label {:en "American Samoa"
                                                                        :fi "Amerikan Samoa"
                                                                        :sv "Amerikanska Samoa"}
                                                                :value "016"}
                                                               {:label {:en "Andorra" :fi "Andorra" :sv "Andorra"}
                                                                :value "020"}
                                                               {:label {:en "Angola" :fi "Angola" :sv "Angola"}
                                                                :value "024"}
                                                               {:label {:en "Anguilla" :fi "Anguilla" :sv "Anguilla"}
                                                                :value "660"}
                                                               {:label {:en "Antarctica" :fi "Antarktis" :sv "Antarktis"}
                                                                :value "010"}
                                                               {:label {:en "Antigua and Barbuda"
                                                                        :fi "Antigua ja Barbuda"
                                                                        :sv "Antigua och Barbuda"}
                                                                :value "028"}
                                                               {:label {:en "United Arab Emirates"
                                                                        :fi "Arabiemiirikunnat"
                                                                        :sv "Förenade Arabemirate"}
                                                                :value "784"}
                                                               {:label {:en "Argentina" :fi "Argentiina" :sv "Argentina"}
                                                                :value "032"}
                                                               {:label {:en "Armenia" :fi "Armenia" :sv "Armenien"}
                                                                :value "051"}
                                                               {:label {:en "Aruba" :fi "Aruba" :sv "Aruba"} :value "533"}
                                                               {:label {:en "Australia" :fi "Australia" :sv "Australien"}
                                                                :value "036"}
                                                               {:label
                                                                       {:en "Azerbaijan" :fi "Azerbaidzan" :sv "Azerbajdzjan"}
                                                                :value "031"}
                                                               {:label {:en "Bahamas" :fi "Bahama" :sv "Bahamas"}
                                                                :value "044"}
                                                               {:label {:en "Bahrain" :fi "Bahrain" :sv "Bahrain"}
                                                                :value "048"}
                                                               {:label {:en "Bangladesh" :fi "Bangladesh" :sv "Bangladesh"}
                                                                :value "050"}
                                                               {:label {:en "Barbados" :fi "Barbados" :sv "Barbados"}
                                                                :value "052"}
                                                               {:label {:en "Belgium" :fi "Belgia" :sv "Belgien"}
                                                                :value "056"}
                                                               {:label {:en "Belize" :fi "Belize" :sv "Belize"}
                                                                :value "084"}
                                                               {:label {:en "Benin" :fi "Benin" :sv "Benin"} :value "204"}
                                                               {:label {:en "Bermuda" :fi "Bermuda" :sv "Bermuda"}
                                                                :value "060"}
                                                               {:label {:en "Bhutan" :fi "Bhutan" :sv "Bhutan"}
                                                                :value "064"}
                                                               {:label {:en "Bolivia" :fi "Bolivia" :sv "Bolivia"}
                                                                :value "068"}
                                                               {:label {:en "Bonaire, Sint Eustatius and Saba"
                                                                        :fi "Bonaire,Sint Eustatius ja Saba"
                                                                        :sv "Bonaire,Sint Eustatius ja Saba"}
                                                                :value "535"}
                                                               {:label {:en "Bosnia and Herzegovina"
                                                                        :fi "Bosnia ja Hertsegovina"
                                                                        :sv "Bosnien och Hercegovina"}
                                                                :value "070"}
                                                               {:label {:en "Botswana" :fi "Botswana" :sv "Botswana"}
                                                                :value "072"}
                                                               {:label
                                                                       {:en "Bouvet Island" :fi "Bouvet'nsaari" :sv "Bouvetön"}
                                                                :value "074"}
                                                               {:label {:en "Brazil" :fi "Brasilia" :sv "Brasilien"}
                                                                :value "076"}
                                                               {:label {:en "United Kingdom"
                                                                        :fi "Britannia"
                                                                        :sv "Storbritannien"}
                                                                :value "826"}
                                                               {:label {:en "British Indian Ocean Territory"
                                                                        :fi "Brittiläinen Intian valtameren alue"
                                                                        :sv "Brittiska territoriet i Indiska Oceanen"}
                                                                :value "086"}
                                                               {:label {:en "Virgin Island, British"
                                                                        :fi "Brittiläiset Neitsytsaaret"
                                                                        :sv "Brittiska Jungfruöarna"}
                                                                :value "092"}
                                                               {:label {:en "Brunei" :fi "Brunei" :sv "Brunei"}
                                                                :value "096"}
                                                               {:label {:en "Bulgaria" :fi "Bulgaria" :sv "Bulgarien"}
                                                                :value "100"}
                                                               {:label {:en "Burkina Faso"
                                                                        :fi "Burkina Faso"
                                                                        :sv "Burkina Faso"}
                                                                :value "854"}
                                                               {:label {:en "Burundi" :fi "Burundi" :sv "Burundi"}
                                                                :value "108"}
                                                               {:label {:en "Cayman Islands"
                                                                        :fi "Caymansaaret"
                                                                        :sv "Caymanöarna"}
                                                                :value "136"}
                                                               {:label {:en "Chile" :fi "Chile" :sv "Chile"} :value "152"}
                                                               {:label
                                                                       {:en "Cook Islands" :fi "Cookinsaaret" :sv "Cooköarna"}
                                                                :value "184"}
                                                               {:label {:en "Costa Rica" :fi "Costa Rica" :sv "Costa Rica"}
                                                                :value "188"}
                                                               {:label {:en "Curaçao" :fi "Curacao" :sv "Curacao"}
                                                                :value "531"}
                                                               {:label {:en "Djibouti" :fi "Djibouti" :sv "Djibouti"}
                                                                :value "262"}
                                                               {:label {:en "Dominica" :fi "Dominica" :sv "Dominica"}
                                                                :value "212"}
                                                               {:label {:en "Dominican Republic"
                                                                        :fi "Dominikaaninen tasavalta"
                                                                        :sv "Dominikanska republiken"}
                                                                :value "214"}
                                                               {:label {:en "Ecuador" :fi "Ecuador" :sv "Ecuador"}
                                                                :value "218"}
                                                               {:label {:en "Egypt" :fi "Egypti" :sv "Egypten"}
                                                                :value "818"}
                                                               {:label
                                                                       {:en "El Salvador" :fi "El Salvador" :sv "El Salvador"}
                                                                :value "222"}
                                                               {:label {:en "Eritrea" :fi "Eritrea" :sv "Eritrea"}
                                                                :value "232"}
                                                               {:label {:en "Spain" :fi "Espanja" :sv "Spanien"}
                                                                :value "724"}
                                                               {:label
                                                                       {:en "South Africa" :fi "Etelä-Afrikka" :sv "Sydafrika"}
                                                                :value "710"}
                                                               {:label {:en "South Georgia and the South Sandwich Islands"
                                                                        :fi "Etelä-Georgia ja Eteläiset Sandwichsaaret"
                                                                        :sv "Sydgeorgien och Sydsandwichöarna"}
                                                                :value "239"}
                                                               {:label
                                                                       {:en "South Sudan" :fi "Etelä-Sudan" :sv "Etelä-Sudan"}
                                                                :value "728"}
                                                               {:label {:en "Ethiopia" :fi "Etiopia" :sv "Etiopien"}
                                                                :value "231"}
                                                               {:label {:en "Falkland Islands"
                                                                        :fi "Falklandinsaaret"
                                                                        :sv "Falklandsöarna"}
                                                                :value "238"}
                                                               {:label {:en "Fiji" :fi "Fidzi" :sv "Fiji"} :value "242"}
                                                               {:label
                                                                       {:en "Philippines" :fi "Filippiinit" :sv "Filippinerna"}
                                                                :value "608"}
                                                               {:label {:en "Faroe Islands" :fi "Färsaaret" :sv "Färöarna"}
                                                                :value "234"}
                                                               {:label {:en "Gabon" :fi "Gabon" :sv "Gabon"} :value "266"}
                                                               {:label {:en "Gambia" :fi "Gambia" :sv "Gambia"}
                                                                :value "270"}
                                                               {:label {:en "Georgia" :fi "Georgia" :sv "Georgien"}
                                                                :value "268"}
                                                               {:label {:en "Ghana" :fi "Ghana" :sv "Ghana"} :value "288"}
                                                               {:label {:en "Gibraltar" :fi "Gibraltar" :sv "Gibraltar"}
                                                                :value "292"}
                                                               {:label {:en "Grenada" :fi "Grenada" :sv "Grenada"}
                                                                :value "308"}
                                                               {:label {:en "Greenland" :fi "Grönlanti" :sv "Grönland"}
                                                                :value "304"}
                                                               {:label {:en "Guadeloupe" :fi "Guadeloupe" :sv "Guadeloupe"}
                                                                :value "312"}
                                                               {:label {:en "Guam" :fi "Guam" :sv "Guam"} :value "316"}
                                                               {:label {:en "Guatemala" :fi "Guatemala" :sv "Guatemala"}
                                                                :value "320"}
                                                               {:label {:en "Guernsey" :fi "Guernsey" :sv "Guernsey"}
                                                                :value "831"}
                                                               {:label {:en "Guinea" :fi "Guinea" :sv "Guinea"}
                                                                :value "324"}
                                                               {:label {:en "Guinea-Bissau"
                                                                        :fi "Guinea-Bissau"
                                                                        :sv "Guinea-Bissau"}
                                                                :value "624"}
                                                               {:label {:en "Guyana" :fi "Guyana" :sv "Guyana"}
                                                                :value "328"}
                                                               {:label {:en "Haiti" :fi "Haiti" :sv "Haiti"} :value "332"}
                                                               {:label {:en "Heard Island and McDonald Islands"
                                                                        :fi "Heard ja McDonaldinsaaret"
                                                                        :sv "Heard och McDonaldöarna"}
                                                                :value "334"}
                                                               {:label {:en "Honduras" :fi "Honduras" :sv "Honduras"}
                                                                :value "340"}
                                                               {:label {:en "Hong Kong" :fi "Hongkong" :sv "Hongkong"}
                                                                :value "344"}
                                                               {:label {:en "Without Citizenship"
                                                                        :fi "Ilman kansalaisuutta"
                                                                        :sv "Utan nationalitet"}
                                                                :value "998"}
                                                               {:label {:en "Without nationality"
                                                                        :fi "Ilman kansalaisuutta"
                                                                        :sv "Utan medborgarskap"}
                                                                :value "991"}
                                                               {:label {:en "Indonesia" :fi "Indonesia" :sv "Indonesien"}
                                                                :value "360"}
                                                               {:label {:en "India" :fi "Intia" :sv "Indien"} :value "356"}
                                                               {:label {:en "Iraq" :fi "Irak" :sv "Irak"} :value "368"}
                                                               {:label {:en "Iran" :fi "Iran" :sv "Iran"} :value "364"}
                                                               {:label {:en "Ireland" :fi "Irlanti" :sv "Irland"}
                                                                :value "372"}
                                                               {:label {:en "Iceland" :fi "Islanti" :sv "Island"}
                                                                :value "352"}
                                                               {:label {:en "Israel" :fi "Israel" :sv "Israel"}
                                                                :value "376"}
                                                               {:label {:en "Italy" :fi "Italia" :sv "Italien"}
                                                                :value "380"}
                                                               {:label {:en "Timor-Leste (East Timor)"
                                                                        :fi "Itä-Timor"
                                                                        :sv "Östtimor"}
                                                                :value "626"}
                                                               {:label {:en "Austria" :fi "Itävalta" :sv "Österrike"}
                                                                :value "040"}
                                                               {:label {:en "Jamaica" :fi "Jamaika" :sv "Jamaica"}
                                                                :value "388"}
                                                               {:label {:en "Japan" :fi "Japani" :sv "Japan"} :value "392"}
                                                               {:label {:en "Yemen" :fi "Jemen" :sv "Jemen"} :value "887"}
                                                               {:label {:en "Jersey" :fi "Jersey" :sv "Jersey"}
                                                                :value "832"}
                                                               {:label {:en "Jordan" :fi "Jordania" :sv "Jordanien"}
                                                                :value "400"}
                                                               {:label
                                                                       {:en "Christmas Island" :fi "Joulusaari" :sv "Julön"}
                                                                :value "162"}
                                                               {:label {:en "Cambodia" :fi "Kambodza" :sv "Kambodja"}
                                                                :value "116"}
                                                               {:label {:en "Cameroon" :fi "Kamerun" :sv "Kamerun"}
                                                                :value "120"}
                                                               {:label {:en "Canada" :fi "Kanada" :sv "Kanada"}
                                                                :value "124"}
                                                               {:label {:en "Capo Verde" :fi "Kap Verde" :sv "Kap Verde"}
                                                                :value "132"}
                                                               {:label {:en "Kazakhstan" :fi "Kazakstan" :sv "Kazakstan"}
                                                                :value "398"}
                                                               {:label {:en "Kenya" :fi "Kenia" :sv "Kenya"} :value "404"}
                                                               {:label {:en "Central African Republic"
                                                                        :fi "Keski-Afrikkan tasavalta"
                                                                        :sv "Centralafrikanska republiken"}
                                                                :value "140"}
                                                               {:label {:en "China" :fi "Kiina" :sv "Kina"} :value "156"}
                                                               {:label {:en "Kyrgyzstan" :fi "Kirgisia" :sv "Kirgizistan"}
                                                                :value "417"}
                                                               {:label {:en "Kiribati" :fi "Kiribati" :sv "Kiribati"}
                                                                :value "296"}
                                                               {:label {:en "Colombia" :fi "Kolumbia" :sv "Columbia"}
                                                                :value "170"}
                                                               {:label {:en "Comoros" :fi "Komorit" :sv "Komorerna"}
                                                                :value "174"}
                                                               {:label {:en "Congo"
                                                                        :fi "Kongo (Kongo-Brazzaville)"
                                                                        :sv "Kongo (Kongo-Brazzaville)"}
                                                                :value "178"}
                                                               {:label {:en "Congo, The Democratic Republic of the"
                                                                        :fi "Kongo (Kongo-Kinshasa)"
                                                                        :sv "Kongo (Kongo-Kinshasa)"}
                                                                :value "180"}
                                                               {:label {:en "Cocos (Keeling) Islands"
                                                                        :fi "Kookossaaret"
                                                                        :sv "Cocosöarna"}
                                                                :value "166"}
                                                               {:label
                                                                       {:en
                                                                            "Korea, Democratic People's Republic of (North Korea)"
                                                                        :fi
                                                                            "Korean demokraattinen kansantasavalta (Pohjois-Korea)"
                                                                        :sv "Demokratiska folkrepubliken Korea (Nordkorea)"}
                                                                :value "408"}
                                                               {:label {:en "Korea, Republic of (South Korea)"
                                                                        :fi "Korean Tasavalta (Etelä-Korea)"
                                                                        :sv "Republiken Korea (Sydkorea)"}
                                                                :value "410"}
                                                               {:label {:en "Kosovo" :fi "Kosovo" :sv "Kosovo"}
                                                                :value "907"}
                                                               {:label {:en "Kosovo" :fi "Kosovo" :sv "Kosovo"}
                                                                :value "997"}
                                                               {:label {:en "Greece" :fi "Kreikka" :sv "Grekland"}
                                                                :value "300"}
                                                               {:label {:en "Croatia" :fi "Kroatia" :sv "Kroatien"}
                                                                :value "191"}
                                                               {:label {:en "Cuba" :fi "Kuuba" :sv "Kuba"} :value "192"}
                                                               {:label {:en "Kuwait" :fi "Kuwait" :sv "Kuwait"}
                                                                :value "414"}
                                                               {:label {:en "Cyprus" :fi "Kypros" :sv "Cypern"}
                                                                :value "196"}
                                                               {:label {:en "Laos" :fi "Laos" :sv "Laos"} :value "418"}
                                                               {:label {:en "Latvia" :fi "Latvia" :sv "Lettland"}
                                                                :value "428"}
                                                               {:label {:en "Lesotho" :fi "Lesotho" :sv "Lesotho"}
                                                                :value "426"}
                                                               {:label {:en "Lebanon" :fi "Libanon" :sv "Libanon"}
                                                                :value "422"}
                                                               {:label {:en "Liberia" :fi "Liberia" :sv "Liberia"}
                                                                :value "430"}
                                                               {:label
                                                                       {:en "Libyan Arab Jamahiriya" :fi "Libya" :sv "Libyen"}
                                                                :value "434"}
                                                               {:label {:en "Liechtenstein"
                                                                        :fi "Liechtenstein"
                                                                        :sv "Liechtenstein"}
                                                                :value "438"}
                                                               {:label {:en "Lithuania" :fi "Liettua" :sv "Litauen"}
                                                                :value "440"}
                                                               {:label {:en "Luxembourg" :fi "Luxemburg" :sv "Luxemburg"}
                                                                :value "442"}
                                                               {:label {:en "Western Sahara"
                                                                        :fi "Länsi-Sahara"
                                                                        :sv "Västsahara"}
                                                                :value "732"}
                                                               {:label {:en "Macao" :fi "Macao" :sv "Macao"} :value "446"}
                                                               {:label {:en "Madagascar" :fi "Madagaskar" :sv "Madagaskar"}
                                                                :value "450"}
                                                               {:label {:en "Macedonia" :fi "Makedonia" :sv "Makedonien"}
                                                                :value "807"}
                                                               {:label {:en "Malawi" :fi "Malawi" :sv "Malawi"}
                                                                :value "454"}
                                                               {:label {:en "Maldives" :fi "Malediivit" :sv "Maldiverna"}
                                                                :value "462"}
                                                               {:label {:en "Malaysia" :fi "Malesia" :sv "Malaysia"}
                                                                :value "458"}
                                                               {:label {:en "Mali" :fi "Mali" :sv "Mali"} :value "466"}
                                                               {:label {:en "Malta" :fi "Malta" :sv "Malta"} :value "470"}
                                                               {:label {:en "Isle of Man" :fi "Mansaari" :sv "Mansaari"}
                                                                :value "833"}
                                                               {:label {:en "Morocco" :fi "Marokko" :sv "Marocko"}
                                                                :value "504"}
                                                               {:label {:en "Marshall Islands"
                                                                        :fi "Marshallinsaaret"
                                                                        :sv "Marshallöarna"}
                                                                :value "584"}
                                                               {:label {:en "Martinique" :fi "Martinique" :sv "Martinique"}
                                                                :value "474"}
                                                               {:label
                                                                       {:en "Mauritania" :fi "Mauritania" :sv "Mauretanien"}
                                                                :value "478"}
                                                               {:label {:en "Mauritius" :fi "Mauritius" :sv "Mauritius"}
                                                                :value "480"}
                                                               {:label {:en "Mayotte" :fi "Mayotte" :sv "Mayotte"}
                                                                :value "175"}
                                                               {:label {:en "Mexico" :fi "Meksiko" :sv "Mexiko"}
                                                                :value "484"}
                                                               {:label {:en "Micronesia, Federated States of"
                                                                        :fi "Mikronesia"
                                                                        :sv "Mikronesien"}
                                                                :value "583"}
                                                               {:label {:en "Moldova" :fi "Moldova" :sv "Moldavien"}
                                                                :value "498"}
                                                               {:label {:en "Monaco" :fi "Monaco" :sv "Monaco"}
                                                                :value "492"}
                                                               {:label {:en "Mongolia" :fi "Mongolia" :sv "Mongoliet"}
                                                                :value "496"}
                                                               {:label {:en "Montenegro" :fi "Montenegro" :sv "Montenegro"}
                                                                :value "499"}
                                                               {:label {:en "Montserrat" :fi "Montserrat" :sv "Montserrat"}
                                                                :value "500"}
                                                               {:label {:en "Mozambique" :fi "Mosambik" :sv "Mosambique"}
                                                                :value "508"}
                                                               {:label {:en "Other dependent country"
                                                                        :fi "Muu epäitsenäinen alue"
                                                                        :sv "Annat icke-suveränt område"}
                                                                :value "990"}
                                                               {:label {:en "Myanmar" :fi "Myanmar" :sv "Myanmar"}
                                                                :value "104"}
                                                               {:label {:en "Namibia" :fi "Namibia" :sv "Namibia"}
                                                                :value "516"}
                                                               {:label {:en "Nauru" :fi "Nauru" :sv "Nauru"} :value "520"}
                                                               {:label {:en "Nepal" :fi "Nepal" :sv "Nepal"} :value "524"}
                                                               {:label {:en "Nicaragua" :fi "Nicaragua" :sv "Nicaragua"}
                                                                :value "558"}
                                                               {:label {:en "Niger" :fi "Niger" :sv "Niger"} :value "562"}
                                                               {:label {:en "Nigeria" :fi "Nigeria" :sv "Nigeria"}
                                                                :value "566"}
                                                               {:label {:en "Niue" :fi "Niue" :sv "Niue"} :value "570"}
                                                               {:label {:en "Norfolk Island"
                                                                        :fi "Norfolkinsaari"
                                                                        :sv "Norfolkön"}
                                                                :value "574"}
                                                               {:label {:en "Norway" :fi "Norja" :sv "Norge"} :value "578"}
                                                               {:label {:en "Côte d'lvoire"
                                                                        :fi "Norsunluurannikko"
                                                                        :sv "Elfenbenskusten"}
                                                                :value "384"}
                                                               {:label {:en "Oman" :fi "Oman" :sv "Oman"} :value "512"}
                                                               {:label {:en "Pakistan" :fi "Pakistan" :sv "Pakistan"}
                                                                :value "586"}
                                                               {:label {:en "Palau" :fi "Palau" :sv "Palau"} :value "585"}
                                                               {:label {:en "Palestine"
                                                                        :fi "Palestiinan valtio"
                                                                        :sv "De palestinska områdena"}
                                                                :value "275"}
                                                               {:label {:en "Panama" :fi "Panama" :sv "Panama"}
                                                                :value "591"}
                                                               {:label {:en "Papua New Guinea"
                                                                        :fi "Papua-Uusi-Guinea"
                                                                        :sv "Papua Nya Guinea"}
                                                                :value "598"}
                                                               {:label {:en "Paraguay" :fi "Paraguay" :sv "Paraguay"}
                                                                :value "600"}
                                                               {:label {:en "Peru" :fi "Peru" :sv "Peru"} :value "604"}
                                                               {:label {:en "Pitcairn" :fi "Pitcairn" :sv "Pitcairn"}
                                                                :value "612"}
                                                               {:label {:en "Northern Mariana Islands"
                                                                        :fi "Pohjois-Mariaanit"
                                                                        :sv "Nordmarianerna"}
                                                                :value "580"}
                                                               {:label {:en "Portugal" :fi "Portugali" :sv "Portugal"}
                                                                :value "620"}
                                                               {:label
                                                                       {:en "Puerto Rico" :fi "Puerto Rico" :sv "Puerto Rico"}
                                                                :value "630"}
                                                               {:label {:en "Poland" :fi "Puola" :sv "Polen"} :value "616"}
                                                               {:label {:en "Equatorial Guinea"
                                                                        :fi "Päiväntasaajan Guinea"
                                                                        :sv "Ekvatorialguinea"}
                                                                :value "226"}
                                                               {:label {:en "Qatar" :fi "Qatar" :sv "Qatar"} :value "634"}
                                                               {:label {:en "France" :fi "Ranska" :sv "Frankrike"}
                                                                :value "250"}
                                                               {:label
                                                                       {:en "French Southern Territories"
                                                                        :fi "Ranskan eteläiset alueet"
                                                                        :sv "De franska territorierna i södra Indiska Oceanen"}
                                                                :value "260"}
                                                               {:label {:en "French Guiana"
                                                                        :fi "Ranskan Guayana"
                                                                        :sv "Franska Guyana"}
                                                                :value "254"}
                                                               {:label {:en "French Plynesia"
                                                                        :fi "Ranskan Polynesia"
                                                                        :sv "Franska Polynesien"}
                                                                :value "258"}
                                                               {:label {:en "Romania" :fi "Romania" :sv "Rumänien"}
                                                                :value "642"}
                                                               {:label {:en "Rwanda" :fi "Ruanda" :sv "Rwanda"}
                                                                :value "646"}
                                                               {:label {:en "Sweden" :fi "Ruotsi" :sv "Sverige"}
                                                                :value "752"}
                                                               {:label {:en "Réunion" :fi "Réunion" :sv "Réunion"}
                                                                :value "638"}
                                                               {:label {:en "Saint Barthélemy"
                                                                        :fi "Saint Barthélemy"
                                                                        :sv "Saint Barthélemy"}
                                                                :value "652"}
                                                               {:label {:en "Saint Helena"
                                                                        :fi "Saint Helena"
                                                                        :sv "Saint Helena"}
                                                                :value "654"}
                                                               {:label {:en "Saint Kitts and Nevis"
                                                                        :fi "Saint Kitts ja Nevis"
                                                                        :sv "Saint Kitts och Nevis"}
                                                                :value "659"}
                                                               {:label
                                                                       {:en "Saint Lucia" :fi "Saint Lucia" :sv "Saint Lucia"}
                                                                :value "662"}
                                                               {:label {:en "Saint Martin (French Part)"
                                                                        :fi "Saint Martin (Ranska)"
                                                                        :sv "Saint Martin (Ranska)"}
                                                                :value "663"}
                                                               {:label {:en "Saint Vincent and the Grenadines"
                                                                        :fi "Saint Vincent ja Grenadiinit"
                                                                        :sv "Saint Vincent och Grenadinerna"}
                                                                :value "670"}
                                                               {:label {:en "Saint Pierre and Miquelon"
                                                                        :fi "Saint-Pierre ja Miquelon"
                                                                        :sv "Saint Pierre och Miquelon"}
                                                                :value "666"}
                                                               {:label {:en "Germany" :fi "Saksa" :sv "Tyskland"}
                                                                :value "276"}
                                                               {:label {:en "Solomon Islands"
                                                                        :fi "Salomonsaaret"
                                                                        :sv "Salomonöarna"}
                                                                :value "090"}
                                                               {:label {:en "Zambia" :fi "Sambia" :sv "Zambia"}
                                                                :value "894"}
                                                               {:label {:en "Samoa" :fi "Samoa" :sv "Samoa"} :value "882"}
                                                               {:label {:en "San Marino" :fi "San Marino" :sv "San Marino"}
                                                                :value "674"}
                                                               {:label {:en "Saudi Arabia"
                                                                        :fi "Saudi-Arabia"
                                                                        :sv "Saudiarabien"}
                                                                :value "682"}
                                                               {:label {:en "Senegal" :fi "Senegal" :sv "Senegal"}
                                                                :value "686"}
                                                               {:label {:en "Serbia" :fi "Serbia" :sv "Serbien"}
                                                                :value "688"}
                                                               {:label
                                                                       {:en "Seychelles" :fi "Seychellit" :sv "Seychellerna"}
                                                                :value "690"}
                                                               {:label {:en "Sierra Leone"
                                                                        :fi "Sierra Leone"
                                                                        :sv "Sierra Leone"}
                                                                :value "694"}
                                                               {:label {:en "Singapore" :fi "Singapore" :sv "Singapore"}
                                                                :value "702"}
                                                               {:label {:en "Sint Maarten (Dutch part)"
                                                                        :fi "Sint Maarten(Alankomaat)"
                                                                        :sv "Sint Maarten(Alankomaat)"}
                                                                :value "534"}
                                                               {:label {:en "Slovakia" :fi "Slovakia" :sv "Slovakien"}
                                                                :value "703"}
                                                               {:label {:en "Slovenia" :fi "Slovenia" :sv "Slovenien"}
                                                                :value "705"}
                                                               {:label {:en "Somalia" :fi "Somalia" :sv "Somalia"}
                                                                :value "706"}
                                                               {:label {:en "Sri Lanka" :fi "Sri Lanka" :sv "Sri Lanka"}
                                                                :value "144"}
                                                               {:label {:en "Sudan" :fi "Sudan" :sv "Sudan"} :value "729"}
                                                               {:label {:en "Finland" :fi "Suomi" :sv "Finland"}
                                                                :value "246"}
                                                               {:label {:en "Suriname" :fi "Suriname" :sv "Suninam"}
                                                                :value "740"}
                                                               {:label {:en "Svalbard and Jan Mayen"
                                                                        :fi "Svalbard ja Jan Mayen"
                                                                        :sv "Svalbard och Jan Mayen"}
                                                                :value "744"}
                                                               {:label {:en "Switzerland" :fi "Sveitsi" :sv "Schweiz"}
                                                                :value "756"}
                                                               {:label {:en "Swaziland" :fi "Swazimaa" :sv "Swaziland"}
                                                                :value "748"}
                                                               {:label {:en "Syria" :fi "Syyria" :sv "Syrien"}
                                                                :value "760"}
                                                               {:label {:en "São Tomé and Príncipe"
                                                                        :fi "São Tomé ja Príncipe"
                                                                        :sv "São Tomé och Príncipe"}
                                                                :value "678"}
                                                               {:label
                                                                       {:en "Tajikistan" :fi "Tadzikistan" :sv "Tadzjikistan"}
                                                                :value "762"}
                                                               {:label {:en "Taiwan, Province of China"
                                                                        :fi "Taiwan"
                                                                        :sv "Taiwan"}
                                                                :value "158"}
                                                               {:label {:en "Tanzania" :fi "Tansania" :sv "Tanzania"}
                                                                :value "834"}
                                                               {:label {:en "Denmark" :fi "Tanska" :sv "Danmark"}
                                                                :value "208"}
                                                               {:label {:en "Thailand" :fi "Thaimaa" :sv "Thailand"}
                                                                :value "764"}
                                                               {:label {:en "Togo" :fi "Togo" :sv "Togo"} :value "768"}
                                                               {:label {:en "Tokelau" :fi "Tokelau" :sv "Tokelau"}
                                                                :value "772"}
                                                               {:label {:en "Tonga" :fi "Tonga" :sv "Tonga"} :value "776"}
                                                               {:label {:en "Trinidad and Tobago"
                                                                        :fi "Trinidad ja Tobago"
                                                                        :sv "Trinidad och Tobago"}
                                                                :value "780"}
                                                               {:label {:en "Tunisia" :fi "Tunisia" :sv "Tunisien"}
                                                                :value "788"}
                                                               {:label {:en "Unknown" :fi "Tuntematon" :sv "Okänt land"}
                                                                :value "999"}
                                                               {:label {:en "Turkey" :fi "Turkki" :sv "Turkiet"}
                                                                :value "792"}
                                                               {:label {:en "Turkmenistan"
                                                                        :fi "Turkmenistan"
                                                                        :sv "Turkmenistan"}
                                                                :value "795"}
                                                               {:label {:en "Turks and Caicos Islands"
                                                                        :fi "Turks- ja Caicossaaret"
                                                                        :sv "Turks- och Caicosöarna"}
                                                                :value "796"}
                                                               {:label {:en "Tuvalu" :fi "Tuvalu" :sv "Tuvalu"}
                                                                :value "798"}
                                                               {:label {:en "Chad" :fi "Tšad" :sv "Tchad"} :value "148"}
                                                               {:label {:en "Czech Republic" :fi "Tšekki" :sv "Tjeckien"}
                                                                :value "203"}
                                                               {:label {:en "Uganda" :fi "Uganda" :sv "Uganda"}
                                                                :value "800"}
                                                               {:label {:en "Ukraine" :fi "Ukraina" :sv "Ukraina"}
                                                                :value "804"}
                                                               {:label {:en "Hungary" :fi "Unkari" :sv "Ungern"}
                                                                :value "348"}
                                                               {:label {:en "Uruguay" :fi "Uruguay" :sv "Uruguay"}
                                                                :value "858"}
                                                               {:label {:en "New Caledonia"
                                                                        :fi "Uusi-Kaledonia"
                                                                        :sv "Nya Kaledonien"}
                                                                :value "540"}
                                                               {:label
                                                                       {:en "New Zealand" :fi "Uusi-Seelanti" :sv "Nya Zeeland"}
                                                                :value "554"}
                                                               {:label {:en "Uzbekistan" :fi "Uzbekistan" :sv "Uzbekistan"}
                                                                :value "860"}
                                                               {:label {:en "Belarus" :fi "Valko-Venäjä" :sv "Vitryssland"}
                                                                :value "112"}
                                                               {:label {:en "Vanuatu" :fi "Vanuatu" :sv "Vanuatu"}
                                                                :value "548"}
                                                               {:label {:en "Holy See (Vatican City State)"
                                                                        :fi "Vatikaani"
                                                                        :sv "Vatikanstaten"}
                                                                :value "336"}
                                                               {:label {:en "Venezuela" :fi "Venezuela" :sv "Venezuela"}
                                                                :value "862"}
                                                               {:label
                                                                       {:en "Russian Federation" :fi "Venäjä" :sv "Ryssland"}
                                                                :value "643"}
                                                               {:label {:en "Viet Nam" :fi "Vietnam" :sv "Vietnam"}
                                                                :value "704"}
                                                               {:label {:en "Estonia" :fi "Viro" :sv "Estland"}
                                                                :value "233"}
                                                               {:label {:en "Wallis and Futuna"
                                                                        :fi "Wallis ja Futuna"
                                                                        :sv "Wallis och Futuna"}
                                                                :value "876"}
                                                               {:label {:en "United States, USA"
                                                                        :fi "Yhdysvallat (USA)"
                                                                        :sv "Förenta Staterna (USA)"}
                                                                :value "840"}
                                                               {:label {:en "Virgin Islands, U.S."
                                                                        :fi "Yhdysvaltain Neitsytsaaret"
                                                                        :sv "Amerikanska Jungfruöarna"}
                                                                :value "850"}
                                                               {:label
                                                                       {:en "United States Minor Outlying Islands"
                                                                        :fi "Yhdysvaltain pienet erillissaaret"
                                                                        :sv
                                                                            "Förenta Staternas mindre öar i Oceanien och Västindien"}
                                                                :value "581"}
                                                               {:label {:en "Zimbabwe" :fi "Zimbabwe" :sv "Zimbabwe"}
                                                                :value "716"} {:label {:fi "" :sv ""} :value ""}]
                                      :params                 {}
                                      :validators             ["required"]}
                                     {:belongs-to-hakukohderyhma ["1.2.246.562.28.24712637358"]
                                      :belongs-to-hakukohteet    ["1.2.246.562.20.43105453732"
                                                                  "1.2.246.562.20.70407522443"]
                                      :fieldClass                "formField"
                                      :fieldType                 "attachment"
                                      :id                        "pohjakoulutus_ulk--attachment"
                                      :label
                                                                 {:en
                                                                  "Education that provides eligibility for higher education in the awarding country"
                                                                  :fi
                                                                  "Tutkintotodistus muualla kuin Suomessa suoritetusta tutkinnosta, joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun"
                                                                  :sv
                                                                  "Examen som avlagts annanstans än i Finland och som i landet ifråga ger ansökningsbehörighet för högskola. "}
                                      :options                   []
                                      :params
                                                                 {:info-text
                                                                  {:enabled? true
                                                                   :value
                                                                             {:en
                                                                              "Submit your attachments in pdf/jpg/png -format. If you cannot submit your attachments online please contact the higher education institution in question directly. The attachments have to be submitted or returned by application period 1: 30.1.2019 at 3 pm Finnish time and application period 2: 10.4.2019 at 3 pm Finnish time at the latest."
                                                                              :fi
                                                                              "Liite täytyy tallentaa viimeistään 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat \ntiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n"
                                                                              :sv
                                                                              "Bilagan ska sparas senast 7 dygn innan ansökningstiden går ut. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}}}]
                        :fieldClass "questionGroup"
                        :fieldType  "fieldset"
                        :id         "pohjakoulutus_ulk"
                        :label      {:fi "" :sv ""}
                        :params     {}}
                       {:fieldClass "infoElement"
                        :fieldType  "p"
                        :id         "hbem--ba44b601-343c-49e8-a5cf-159d1a5acabc"
                        :label      {:fi ""}
                        :params     {}
                        :text
                                    {:en "Click add if you want add further qualifications."
                                     :fi "Paina lisää jos haluat lisätä useampia tutkintoja."
                                     :sv "Tryck på lägg till om du vill lägga till flera examina."}}]
               :label
                      {:en
                       "Other qualification completed outside Finland that provides eligibility to apply for higher education in the country in question"
                       :fi
                       "Muualla kuin Suomessa suoritettu muu tutkinto, joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun"
                       :sv
                       "Övrig examen som avlagts annanstans än i Finland, och ger behörighet för högskolestudier i ifrågavarande land"}
               :value "pohjakoulutus_ulk"}
              {:followups
                      [{:children
                                    [{:belongs-to-hakukohteet []
                                      :fieldClass             "formField"
                                      :fieldType              "dropdown"
                                      :id                     "pohjakoulutus_kk_ulk--level-of-degree"
                                      :koodisto-source
                                                              {:title "Kk-tutkinnot" :uri "kktutkinnot" :version 1}
                                      :label
                                                              {:en "Level of degree" :fi "Tutkintotaso" :sv "Examensnivå"}
                                      :options                [{:label {:en "Lower university degree (Bachelor's)"
                                                                        :fi "Alempi yliopistotutkinto (kandidaatti)"
                                                                        :sv "Lägre universitetsexamen (kandidat)"}
                                                                :value "2"}
                                                               {:label {:en "Polytechnic/UAS Bachelor's degree"
                                                                        :fi "Ammattikorkeakoulututkinto"
                                                                        :sv "Yrkeshögskoleexamen"}
                                                                :value "1"}
                                                               {:label {:en "Licentiate/doctoral"
                                                                        :fi "Lisensiaatti/tohtori"
                                                                        :sv "Licentiat/doktor"}
                                                                :value "5"}
                                                               {:label {:en "Polytechnic/UAS Master's degree"
                                                                        :fi "Ylempi ammattikorkeakoulututkinto"
                                                                        :sv "Högre yrkeshögskoleexamen"}
                                                                :value "3"}
                                                               {:label {:en "Higher university degree (Master's)"
                                                                        :fi "Ylempi yliopistotutkinto (maisteri)"
                                                                        :sv "Högre universitetsexamen (magister)"}
                                                                :value "4"} {:label {:fi "" :sv ""} :value ""}]
                                      :params                 {}
                                      :validators             ["required"]}
                                     {:belongs-to-hakukohteet []
                                      :fieldClass             "formField"
                                      :fieldType              "textField"
                                      :id                     "pohjakoulutus_kk_ulk--year-of-completion"
                                      :label                  {:en "Year and date of completion (dd.mm.yyyy)"
                                                               :fi "Suorituspäivämäärä - ja vuosi (pp.kk.vvvv)"
                                                               :sv "År och datum då examen avlagts (dd.mm.åååå)"}
                                      :params                 {:decimals nil :numeric false :size "S"}
                                      :validators             ["required"]}
                                     {:belongs-to-hakukohteet []
                                      :fieldClass             "formField"
                                      :fieldType              "textField"
                                      :id                     "pohjakoulutus_kk_ulk--degree"
                                      :label                  {:en "Degree" :fi "Tutkinto" :sv "Examen"}
                                      :params                 {}
                                      :validators             ["required"]}
                                     {:belongs-to-hakukohteet []
                                      :fieldClass             "formField"
                                      :fieldType              "textField"
                                      :id                     "pohjakoulutus_kk_ulk--institution"
                                      :label                  {:en "Higher education institution"
                                                               :fi "Korkeakoulu"
                                                               :sv "Hogskola"}
                                      :params                 {}
                                      :validators             ["required"]}
                                     {:belongs-to-hakukohteet []
                                      :fieldClass             "formField"
                                      :fieldType              "dropdown"
                                      :id                     "pohjakoulutus_kk_ulk--country"
                                      :koodisto-source
                                                              {:title "Maat ja valtiot" :uri "maatjavaltiot2" :version 1}
                                      :label                  {:en "Country of completion"
                                                               :fi "Suoritusmaa"
                                                               :sv "Land där examen är avlagd"}
                                      :options
                                                              [{:label
                                                                       {:en "Afghanistan" :fi "Afganistan" :sv "Afghanistan"}
                                                                :value "004"}
                                                               {:label {:en "Åland Islands" :fi "Ahvenanmaa" :sv "Åland"}
                                                                :value "248"}
                                                               {:label
                                                                       {:en "Netherlands" :fi "Alankomaat" :sv "Nederländerna"}
                                                                :value "528"}
                                                               {:label {:en "Albania" :fi "Albania" :sv "Albanien"}
                                                                :value "008"}
                                                               {:label {:en "Algeria" :fi "Algeria" :sv "Algeriet"}
                                                                :value "012"}
                                                               {:label {:en "American Samoa"
                                                                        :fi "Amerikan Samoa"
                                                                        :sv "Amerikanska Samoa"}
                                                                :value "016"}
                                                               {:label {:en "Andorra" :fi "Andorra" :sv "Andorra"}
                                                                :value "020"}
                                                               {:label {:en "Angola" :fi "Angola" :sv "Angola"}
                                                                :value "024"}
                                                               {:label {:en "Anguilla" :fi "Anguilla" :sv "Anguilla"}
                                                                :value "660"}
                                                               {:label {:en "Antarctica" :fi "Antarktis" :sv "Antarktis"}
                                                                :value "010"}
                                                               {:label {:en "Antigua and Barbuda"
                                                                        :fi "Antigua ja Barbuda"
                                                                        :sv "Antigua och Barbuda"}
                                                                :value "028"}
                                                               {:label {:en "United Arab Emirates"
                                                                        :fi "Arabiemiirikunnat"
                                                                        :sv "Förenade Arabemirate"}
                                                                :value "784"}
                                                               {:label {:en "Argentina" :fi "Argentiina" :sv "Argentina"}
                                                                :value "032"}
                                                               {:label {:en "Armenia" :fi "Armenia" :sv "Armenien"}
                                                                :value "051"}
                                                               {:label {:en "Aruba" :fi "Aruba" :sv "Aruba"} :value "533"}
                                                               {:label {:en "Australia" :fi "Australia" :sv "Australien"}
                                                                :value "036"}
                                                               {:label
                                                                       {:en "Azerbaijan" :fi "Azerbaidzan" :sv "Azerbajdzjan"}
                                                                :value "031"}
                                                               {:label {:en "Bahamas" :fi "Bahama" :sv "Bahamas"}
                                                                :value "044"}
                                                               {:label {:en "Bahrain" :fi "Bahrain" :sv "Bahrain"}
                                                                :value "048"}
                                                               {:label {:en "Bangladesh" :fi "Bangladesh" :sv "Bangladesh"}
                                                                :value "050"}
                                                               {:label {:en "Barbados" :fi "Barbados" :sv "Barbados"}
                                                                :value "052"}
                                                               {:label {:en "Belgium" :fi "Belgia" :sv "Belgien"}
                                                                :value "056"}
                                                               {:label {:en "Belize" :fi "Belize" :sv "Belize"}
                                                                :value "084"}
                                                               {:label {:en "Benin" :fi "Benin" :sv "Benin"} :value "204"}
                                                               {:label {:en "Bermuda" :fi "Bermuda" :sv "Bermuda"}
                                                                :value "060"}
                                                               {:label {:en "Bhutan" :fi "Bhutan" :sv "Bhutan"}
                                                                :value "064"}
                                                               {:label {:en "Bolivia" :fi "Bolivia" :sv "Bolivia"}
                                                                :value "068"}
                                                               {:label {:en "Bonaire, Sint Eustatius and Saba"
                                                                        :fi "Bonaire,Sint Eustatius ja Saba"
                                                                        :sv "Bonaire,Sint Eustatius ja Saba"}
                                                                :value "535"}
                                                               {:label {:en "Bosnia and Herzegovina"
                                                                        :fi "Bosnia ja Hertsegovina"
                                                                        :sv "Bosnien och Hercegovina"}
                                                                :value "070"}
                                                               {:label {:en "Botswana" :fi "Botswana" :sv "Botswana"}
                                                                :value "072"}
                                                               {:label
                                                                       {:en "Bouvet Island" :fi "Bouvet'nsaari" :sv "Bouvetön"}
                                                                :value "074"}
                                                               {:label {:en "Brazil" :fi "Brasilia" :sv "Brasilien"}
                                                                :value "076"}
                                                               {:label {:en "United Kingdom"
                                                                        :fi "Britannia"
                                                                        :sv "Storbritannien"}
                                                                :value "826"}
                                                               {:label {:en "British Indian Ocean Territory"
                                                                        :fi "Brittiläinen Intian valtameren alue"
                                                                        :sv "Brittiska territoriet i Indiska Oceanen"}
                                                                :value "086"}
                                                               {:label {:en "Virgin Island, British"
                                                                        :fi "Brittiläiset Neitsytsaaret"
                                                                        :sv "Brittiska Jungfruöarna"}
                                                                :value "092"}
                                                               {:label {:en "Brunei" :fi "Brunei" :sv "Brunei"}
                                                                :value "096"}
                                                               {:label {:en "Bulgaria" :fi "Bulgaria" :sv "Bulgarien"}
                                                                :value "100"}
                                                               {:label {:en "Burkina Faso"
                                                                        :fi "Burkina Faso"
                                                                        :sv "Burkina Faso"}
                                                                :value "854"}
                                                               {:label {:en "Burundi" :fi "Burundi" :sv "Burundi"}
                                                                :value "108"}
                                                               {:label {:en "Cayman Islands"
                                                                        :fi "Caymansaaret"
                                                                        :sv "Caymanöarna"}
                                                                :value "136"}
                                                               {:label {:en "Chile" :fi "Chile" :sv "Chile"} :value "152"}
                                                               {:label
                                                                       {:en "Cook Islands" :fi "Cookinsaaret" :sv "Cooköarna"}
                                                                :value "184"}
                                                               {:label {:en "Costa Rica" :fi "Costa Rica" :sv "Costa Rica"}
                                                                :value "188"}
                                                               {:label {:en "Curaçao" :fi "Curacao" :sv "Curacao"}
                                                                :value "531"}
                                                               {:label {:en "Djibouti" :fi "Djibouti" :sv "Djibouti"}
                                                                :value "262"}
                                                               {:label {:en "Dominica" :fi "Dominica" :sv "Dominica"}
                                                                :value "212"}
                                                               {:label {:en "Dominican Republic"
                                                                        :fi "Dominikaaninen tasavalta"
                                                                        :sv "Dominikanska republiken"}
                                                                :value "214"}
                                                               {:label {:en "Ecuador" :fi "Ecuador" :sv "Ecuador"}
                                                                :value "218"}
                                                               {:label {:en "Egypt" :fi "Egypti" :sv "Egypten"}
                                                                :value "818"}
                                                               {:label
                                                                       {:en "El Salvador" :fi "El Salvador" :sv "El Salvador"}
                                                                :value "222"}
                                                               {:label {:en "Eritrea" :fi "Eritrea" :sv "Eritrea"}
                                                                :value "232"}
                                                               {:label {:en "Spain" :fi "Espanja" :sv "Spanien"}
                                                                :value "724"}
                                                               {:label
                                                                       {:en "South Africa" :fi "Etelä-Afrikka" :sv "Sydafrika"}
                                                                :value "710"}
                                                               {:label {:en "South Georgia and the South Sandwich Islands"
                                                                        :fi "Etelä-Georgia ja Eteläiset Sandwichsaaret"
                                                                        :sv "Sydgeorgien och Sydsandwichöarna"}
                                                                :value "239"}
                                                               {:label
                                                                       {:en "South Sudan" :fi "Etelä-Sudan" :sv "Etelä-Sudan"}
                                                                :value "728"}
                                                               {:label {:en "Ethiopia" :fi "Etiopia" :sv "Etiopien"}
                                                                :value "231"}
                                                               {:label {:en "Falkland Islands"
                                                                        :fi "Falklandinsaaret"
                                                                        :sv "Falklandsöarna"}
                                                                :value "238"}
                                                               {:label {:en "Fiji" :fi "Fidzi" :sv "Fiji"} :value "242"}
                                                               {:label
                                                                       {:en "Philippines" :fi "Filippiinit" :sv "Filippinerna"}
                                                                :value "608"}
                                                               {:label {:en "Faroe Islands" :fi "Färsaaret" :sv "Färöarna"}
                                                                :value "234"}
                                                               {:label {:en "Gabon" :fi "Gabon" :sv "Gabon"} :value "266"}
                                                               {:label {:en "Gambia" :fi "Gambia" :sv "Gambia"}
                                                                :value "270"}
                                                               {:label {:en "Georgia" :fi "Georgia" :sv "Georgien"}
                                                                :value "268"}
                                                               {:label {:en "Ghana" :fi "Ghana" :sv "Ghana"} :value "288"}
                                                               {:label {:en "Gibraltar" :fi "Gibraltar" :sv "Gibraltar"}
                                                                :value "292"}
                                                               {:label {:en "Grenada" :fi "Grenada" :sv "Grenada"}
                                                                :value "308"}
                                                               {:label {:en "Greenland" :fi "Grönlanti" :sv "Grönland"}
                                                                :value "304"}
                                                               {:label {:en "Guadeloupe" :fi "Guadeloupe" :sv "Guadeloupe"}
                                                                :value "312"}
                                                               {:label {:en "Guam" :fi "Guam" :sv "Guam"} :value "316"}
                                                               {:label {:en "Guatemala" :fi "Guatemala" :sv "Guatemala"}
                                                                :value "320"}
                                                               {:label {:en "Guernsey" :fi "Guernsey" :sv "Guernsey"}
                                                                :value "831"}
                                                               {:label {:en "Guinea" :fi "Guinea" :sv "Guinea"}
                                                                :value "324"}
                                                               {:label {:en "Guinea-Bissau"
                                                                        :fi "Guinea-Bissau"
                                                                        :sv "Guinea-Bissau"}
                                                                :value "624"}
                                                               {:label {:en "Guyana" :fi "Guyana" :sv "Guyana"}
                                                                :value "328"}
                                                               {:label {:en "Haiti" :fi "Haiti" :sv "Haiti"} :value "332"}
                                                               {:label {:en "Heard Island and McDonald Islands"
                                                                        :fi "Heard ja McDonaldinsaaret"
                                                                        :sv "Heard och McDonaldöarna"}
                                                                :value "334"}
                                                               {:label {:en "Honduras" :fi "Honduras" :sv "Honduras"}
                                                                :value "340"}
                                                               {:label {:en "Hong Kong" :fi "Hongkong" :sv "Hongkong"}
                                                                :value "344"}
                                                               {:label {:en "Without Citizenship"
                                                                        :fi "Ilman kansalaisuutta"
                                                                        :sv "Utan nationalitet"}
                                                                :value "998"}
                                                               {:label {:en "Without nationality"
                                                                        :fi "Ilman kansalaisuutta"
                                                                        :sv "Utan medborgarskap"}
                                                                :value "991"}
                                                               {:label {:en "Indonesia" :fi "Indonesia" :sv "Indonesien"}
                                                                :value "360"}
                                                               {:label {:en "India" :fi "Intia" :sv "Indien"} :value "356"}
                                                               {:label {:en "Iraq" :fi "Irak" :sv "Irak"} :value "368"}
                                                               {:label {:en "Iran" :fi "Iran" :sv "Iran"} :value "364"}
                                                               {:label {:en "Ireland" :fi "Irlanti" :sv "Irland"}
                                                                :value "372"}
                                                               {:label {:en "Iceland" :fi "Islanti" :sv "Island"}
                                                                :value "352"}
                                                               {:label {:en "Israel" :fi "Israel" :sv "Israel"}
                                                                :value "376"}
                                                               {:label {:en "Italy" :fi "Italia" :sv "Italien"}
                                                                :value "380"}
                                                               {:label {:en "Timor-Leste (East Timor)"
                                                                        :fi "Itä-Timor"
                                                                        :sv "Östtimor"}
                                                                :value "626"}
                                                               {:label {:en "Austria" :fi "Itävalta" :sv "Österrike"}
                                                                :value "040"}
                                                               {:label {:en "Jamaica" :fi "Jamaika" :sv "Jamaica"}
                                                                :value "388"}
                                                               {:label {:en "Japan" :fi "Japani" :sv "Japan"} :value "392"}
                                                               {:label {:en "Yemen" :fi "Jemen" :sv "Jemen"} :value "887"}
                                                               {:label {:en "Jersey" :fi "Jersey" :sv "Jersey"}
                                                                :value "832"}
                                                               {:label {:en "Jordan" :fi "Jordania" :sv "Jordanien"}
                                                                :value "400"}
                                                               {:label
                                                                       {:en "Christmas Island" :fi "Joulusaari" :sv "Julön"}
                                                                :value "162"}
                                                               {:label {:en "Cambodia" :fi "Kambodza" :sv "Kambodja"}
                                                                :value "116"}
                                                               {:label {:en "Cameroon" :fi "Kamerun" :sv "Kamerun"}
                                                                :value "120"}
                                                               {:label {:en "Canada" :fi "Kanada" :sv "Kanada"}
                                                                :value "124"}
                                                               {:label {:en "Capo Verde" :fi "Kap Verde" :sv "Kap Verde"}
                                                                :value "132"}
                                                               {:label {:en "Kazakhstan" :fi "Kazakstan" :sv "Kazakstan"}
                                                                :value "398"}
                                                               {:label {:en "Kenya" :fi "Kenia" :sv "Kenya"} :value "404"}
                                                               {:label {:en "Central African Republic"
                                                                        :fi "Keski-Afrikkan tasavalta"
                                                                        :sv "Centralafrikanska republiken"}
                                                                :value "140"}
                                                               {:label {:en "China" :fi "Kiina" :sv "Kina"} :value "156"}
                                                               {:label {:en "Kyrgyzstan" :fi "Kirgisia" :sv "Kirgizistan"}
                                                                :value "417"}
                                                               {:label {:en "Kiribati" :fi "Kiribati" :sv "Kiribati"}
                                                                :value "296"}
                                                               {:label {:en "Colombia" :fi "Kolumbia" :sv "Columbia"}
                                                                :value "170"}
                                                               {:label {:en "Comoros" :fi "Komorit" :sv "Komorerna"}
                                                                :value "174"}
                                                               {:label {:en "Congo"
                                                                        :fi "Kongo (Kongo-Brazzaville)"
                                                                        :sv "Kongo (Kongo-Brazzaville)"}
                                                                :value "178"}
                                                               {:label {:en "Congo, The Democratic Republic of the"
                                                                        :fi "Kongo (Kongo-Kinshasa)"
                                                                        :sv "Kongo (Kongo-Kinshasa)"}
                                                                :value "180"}
                                                               {:label {:en "Cocos (Keeling) Islands"
                                                                        :fi "Kookossaaret"
                                                                        :sv "Cocosöarna"}
                                                                :value "166"}
                                                               {:label
                                                                       {:en
                                                                            "Korea, Democratic People's Republic of (North Korea)"
                                                                        :fi
                                                                            "Korean demokraattinen kansantasavalta (Pohjois-Korea)"
                                                                        :sv "Demokratiska folkrepubliken Korea (Nordkorea)"}
                                                                :value "408"}
                                                               {:label {:en "Korea, Republic of (South Korea)"
                                                                        :fi "Korean Tasavalta (Etelä-Korea)"
                                                                        :sv "Republiken Korea (Sydkorea)"}
                                                                :value "410"}
                                                               {:label {:en "Kosovo" :fi "Kosovo" :sv "Kosovo"}
                                                                :value "907"}
                                                               {:label {:en "Kosovo" :fi "Kosovo" :sv "Kosovo"}
                                                                :value "997"}
                                                               {:label {:en "Greece" :fi "Kreikka" :sv "Grekland"}
                                                                :value "300"}
                                                               {:label {:en "Croatia" :fi "Kroatia" :sv "Kroatien"}
                                                                :value "191"}
                                                               {:label {:en "Cuba" :fi "Kuuba" :sv "Kuba"} :value "192"}
                                                               {:label {:en "Kuwait" :fi "Kuwait" :sv "Kuwait"}
                                                                :value "414"}
                                                               {:label {:en "Cyprus" :fi "Kypros" :sv "Cypern"}
                                                                :value "196"}
                                                               {:label {:en "Laos" :fi "Laos" :sv "Laos"} :value "418"}
                                                               {:label {:en "Latvia" :fi "Latvia" :sv "Lettland"}
                                                                :value "428"}
                                                               {:label {:en "Lesotho" :fi "Lesotho" :sv "Lesotho"}
                                                                :value "426"}
                                                               {:label {:en "Lebanon" :fi "Libanon" :sv "Libanon"}
                                                                :value "422"}
                                                               {:label {:en "Liberia" :fi "Liberia" :sv "Liberia"}
                                                                :value "430"}
                                                               {:label
                                                                       {:en "Libyan Arab Jamahiriya" :fi "Libya" :sv "Libyen"}
                                                                :value "434"}
                                                               {:label {:en "Liechtenstein"
                                                                        :fi "Liechtenstein"
                                                                        :sv "Liechtenstein"}
                                                                :value "438"}
                                                               {:label {:en "Lithuania" :fi "Liettua" :sv "Litauen"}
                                                                :value "440"}
                                                               {:label {:en "Luxembourg" :fi "Luxemburg" :sv "Luxemburg"}
                                                                :value "442"}
                                                               {:label {:en "Western Sahara"
                                                                        :fi "Länsi-Sahara"
                                                                        :sv "Västsahara"}
                                                                :value "732"}
                                                               {:label {:en "Macao" :fi "Macao" :sv "Macao"} :value "446"}
                                                               {:label {:en "Madagascar" :fi "Madagaskar" :sv "Madagaskar"}
                                                                :value "450"}
                                                               {:label {:en "Macedonia" :fi "Makedonia" :sv "Makedonien"}
                                                                :value "807"}
                                                               {:label {:en "Malawi" :fi "Malawi" :sv "Malawi"}
                                                                :value "454"}
                                                               {:label {:en "Maldives" :fi "Malediivit" :sv "Maldiverna"}
                                                                :value "462"}
                                                               {:label {:en "Malaysia" :fi "Malesia" :sv "Malaysia"}
                                                                :value "458"}
                                                               {:label {:en "Mali" :fi "Mali" :sv "Mali"} :value "466"}
                                                               {:label {:en "Malta" :fi "Malta" :sv "Malta"} :value "470"}
                                                               {:label {:en "Isle of Man" :fi "Mansaari" :sv "Mansaari"}
                                                                :value "833"}
                                                               {:label {:en "Morocco" :fi "Marokko" :sv "Marocko"}
                                                                :value "504"}
                                                               {:label {:en "Marshall Islands"
                                                                        :fi "Marshallinsaaret"
                                                                        :sv "Marshallöarna"}
                                                                :value "584"}
                                                               {:label {:en "Martinique" :fi "Martinique" :sv "Martinique"}
                                                                :value "474"}
                                                               {:label
                                                                       {:en "Mauritania" :fi "Mauritania" :sv "Mauretanien"}
                                                                :value "478"}
                                                               {:label {:en "Mauritius" :fi "Mauritius" :sv "Mauritius"}
                                                                :value "480"}
                                                               {:label {:en "Mayotte" :fi "Mayotte" :sv "Mayotte"}
                                                                :value "175"}
                                                               {:label {:en "Mexico" :fi "Meksiko" :sv "Mexiko"}
                                                                :value "484"}
                                                               {:label {:en "Micronesia, Federated States of"
                                                                        :fi "Mikronesia"
                                                                        :sv "Mikronesien"}
                                                                :value "583"}
                                                               {:label {:en "Moldova" :fi "Moldova" :sv "Moldavien"}
                                                                :value "498"}
                                                               {:label {:en "Monaco" :fi "Monaco" :sv "Monaco"}
                                                                :value "492"}
                                                               {:label {:en "Mongolia" :fi "Mongolia" :sv "Mongoliet"}
                                                                :value "496"}
                                                               {:label {:en "Montenegro" :fi "Montenegro" :sv "Montenegro"}
                                                                :value "499"}
                                                               {:label {:en "Montserrat" :fi "Montserrat" :sv "Montserrat"}
                                                                :value "500"}
                                                               {:label {:en "Mozambique" :fi "Mosambik" :sv "Mosambique"}
                                                                :value "508"}
                                                               {:label {:en "Other dependent country"
                                                                        :fi "Muu epäitsenäinen alue"
                                                                        :sv "Annat icke-suveränt område"}
                                                                :value "990"}
                                                               {:label {:en "Myanmar" :fi "Myanmar" :sv "Myanmar"}
                                                                :value "104"}
                                                               {:label {:en "Namibia" :fi "Namibia" :sv "Namibia"}
                                                                :value "516"}
                                                               {:label {:en "Nauru" :fi "Nauru" :sv "Nauru"} :value "520"}
                                                               {:label {:en "Nepal" :fi "Nepal" :sv "Nepal"} :value "524"}
                                                               {:label {:en "Nicaragua" :fi "Nicaragua" :sv "Nicaragua"}
                                                                :value "558"}
                                                               {:label {:en "Niger" :fi "Niger" :sv "Niger"} :value "562"}
                                                               {:label {:en "Nigeria" :fi "Nigeria" :sv "Nigeria"}
                                                                :value "566"}
                                                               {:label {:en "Niue" :fi "Niue" :sv "Niue"} :value "570"}
                                                               {:label {:en "Norfolk Island"
                                                                        :fi "Norfolkinsaari"
                                                                        :sv "Norfolkön"}
                                                                :value "574"}
                                                               {:label {:en "Norway" :fi "Norja" :sv "Norge"} :value "578"}
                                                               {:label {:en "Côte d'lvoire"
                                                                        :fi "Norsunluurannikko"
                                                                        :sv "Elfenbenskusten"}
                                                                :value "384"}
                                                               {:label {:en "Oman" :fi "Oman" :sv "Oman"} :value "512"}
                                                               {:label {:en "Pakistan" :fi "Pakistan" :sv "Pakistan"}
                                                                :value "586"}
                                                               {:label {:en "Palau" :fi "Palau" :sv "Palau"} :value "585"}
                                                               {:label {:en "Palestine"
                                                                        :fi "Palestiinan valtio"
                                                                        :sv "De palestinska områdena"}
                                                                :value "275"}
                                                               {:label {:en "Panama" :fi "Panama" :sv "Panama"}
                                                                :value "591"}
                                                               {:label {:en "Papua New Guinea"
                                                                        :fi "Papua-Uusi-Guinea"
                                                                        :sv "Papua Nya Guinea"}
                                                                :value "598"}
                                                               {:label {:en "Paraguay" :fi "Paraguay" :sv "Paraguay"}
                                                                :value "600"}
                                                               {:label {:en "Peru" :fi "Peru" :sv "Peru"} :value "604"}
                                                               {:label {:en "Pitcairn" :fi "Pitcairn" :sv "Pitcairn"}
                                                                :value "612"}
                                                               {:label {:en "Northern Mariana Islands"
                                                                        :fi "Pohjois-Mariaanit"
                                                                        :sv "Nordmarianerna"}
                                                                :value "580"}
                                                               {:label {:en "Portugal" :fi "Portugali" :sv "Portugal"}
                                                                :value "620"}
                                                               {:label
                                                                       {:en "Puerto Rico" :fi "Puerto Rico" :sv "Puerto Rico"}
                                                                :value "630"}
                                                               {:label {:en "Poland" :fi "Puola" :sv "Polen"} :value "616"}
                                                               {:label {:en "Equatorial Guinea"
                                                                        :fi "Päiväntasaajan Guinea"
                                                                        :sv "Ekvatorialguinea"}
                                                                :value "226"}
                                                               {:label {:en "Qatar" :fi "Qatar" :sv "Qatar"} :value "634"}
                                                               {:label {:en "France" :fi "Ranska" :sv "Frankrike"}
                                                                :value "250"}
                                                               {:label
                                                                       {:en "French Southern Territories"
                                                                        :fi "Ranskan eteläiset alueet"
                                                                        :sv "De franska territorierna i södra Indiska Oceanen"}
                                                                :value "260"}
                                                               {:label {:en "French Guiana"
                                                                        :fi "Ranskan Guayana"
                                                                        :sv "Franska Guyana"}
                                                                :value "254"}
                                                               {:label {:en "French Plynesia"
                                                                        :fi "Ranskan Polynesia"
                                                                        :sv "Franska Polynesien"}
                                                                :value "258"}
                                                               {:label {:en "Romania" :fi "Romania" :sv "Rumänien"}
                                                                :value "642"}
                                                               {:label {:en "Rwanda" :fi "Ruanda" :sv "Rwanda"}
                                                                :value "646"}
                                                               {:label {:en "Sweden" :fi "Ruotsi" :sv "Sverige"}
                                                                :value "752"}
                                                               {:label {:en "Réunion" :fi "Réunion" :sv "Réunion"}
                                                                :value "638"}
                                                               {:label {:en "Saint Barthélemy"
                                                                        :fi "Saint Barthélemy"
                                                                        :sv "Saint Barthélemy"}
                                                                :value "652"}
                                                               {:label {:en "Saint Helena"
                                                                        :fi "Saint Helena"
                                                                        :sv "Saint Helena"}
                                                                :value "654"}
                                                               {:label {:en "Saint Kitts and Nevis"
                                                                        :fi "Saint Kitts ja Nevis"
                                                                        :sv "Saint Kitts och Nevis"}
                                                                :value "659"}
                                                               {:label
                                                                       {:en "Saint Lucia" :fi "Saint Lucia" :sv "Saint Lucia"}
                                                                :value "662"}
                                                               {:label {:en "Saint Martin (French Part)"
                                                                        :fi "Saint Martin (Ranska)"
                                                                        :sv "Saint Martin (Ranska)"}
                                                                :value "663"}
                                                               {:label {:en "Saint Vincent and the Grenadines"
                                                                        :fi "Saint Vincent ja Grenadiinit"
                                                                        :sv "Saint Vincent och Grenadinerna"}
                                                                :value "670"}
                                                               {:label {:en "Saint Pierre and Miquelon"
                                                                        :fi "Saint-Pierre ja Miquelon"
                                                                        :sv "Saint Pierre och Miquelon"}
                                                                :value "666"}
                                                               {:label {:en "Germany" :fi "Saksa" :sv "Tyskland"}
                                                                :value "276"}
                                                               {:label {:en "Solomon Islands"
                                                                        :fi "Salomonsaaret"
                                                                        :sv "Salomonöarna"}
                                                                :value "090"}
                                                               {:label {:en "Zambia" :fi "Sambia" :sv "Zambia"}
                                                                :value "894"}
                                                               {:label {:en "Samoa" :fi "Samoa" :sv "Samoa"} :value "882"}
                                                               {:label {:en "San Marino" :fi "San Marino" :sv "San Marino"}
                                                                :value "674"}
                                                               {:label {:en "Saudi Arabia"
                                                                        :fi "Saudi-Arabia"
                                                                        :sv "Saudiarabien"}
                                                                :value "682"}
                                                               {:label {:en "Senegal" :fi "Senegal" :sv "Senegal"}
                                                                :value "686"}
                                                               {:label {:en "Serbia" :fi "Serbia" :sv "Serbien"}
                                                                :value "688"}
                                                               {:label
                                                                       {:en "Seychelles" :fi "Seychellit" :sv "Seychellerna"}
                                                                :value "690"}
                                                               {:label {:en "Sierra Leone"
                                                                        :fi "Sierra Leone"
                                                                        :sv "Sierra Leone"}
                                                                :value "694"}
                                                               {:label {:en "Singapore" :fi "Singapore" :sv "Singapore"}
                                                                :value "702"}
                                                               {:label {:en "Sint Maarten (Dutch part)"
                                                                        :fi "Sint Maarten(Alankomaat)"
                                                                        :sv "Sint Maarten(Alankomaat)"}
                                                                :value "534"}
                                                               {:label {:en "Slovakia" :fi "Slovakia" :sv "Slovakien"}
                                                                :value "703"}
                                                               {:label {:en "Slovenia" :fi "Slovenia" :sv "Slovenien"}
                                                                :value "705"}
                                                               {:label {:en "Somalia" :fi "Somalia" :sv "Somalia"}
                                                                :value "706"}
                                                               {:label {:en "Sri Lanka" :fi "Sri Lanka" :sv "Sri Lanka"}
                                                                :value "144"}
                                                               {:label {:en "Sudan" :fi "Sudan" :sv "Sudan"} :value "729"}
                                                               {:label {:en "Finland" :fi "Suomi" :sv "Finland"}
                                                                :value "246"}
                                                               {:label {:en "Suriname" :fi "Suriname" :sv "Suninam"}
                                                                :value "740"}
                                                               {:label {:en "Svalbard and Jan Mayen"
                                                                        :fi "Svalbard ja Jan Mayen"
                                                                        :sv "Svalbard och Jan Mayen"}
                                                                :value "744"}
                                                               {:label {:en "Switzerland" :fi "Sveitsi" :sv "Schweiz"}
                                                                :value "756"}
                                                               {:label {:en "Swaziland" :fi "Swazimaa" :sv "Swaziland"}
                                                                :value "748"}
                                                               {:label {:en "Syria" :fi "Syyria" :sv "Syrien"}
                                                                :value "760"}
                                                               {:label {:en "São Tomé and Príncipe"
                                                                        :fi "São Tomé ja Príncipe"
                                                                        :sv "São Tomé och Príncipe"}
                                                                :value "678"}
                                                               {:label
                                                                       {:en "Tajikistan" :fi "Tadzikistan" :sv "Tadzjikistan"}
                                                                :value "762"}
                                                               {:label {:en "Taiwan, Province of China"
                                                                        :fi "Taiwan"
                                                                        :sv "Taiwan"}
                                                                :value "158"}
                                                               {:label {:en "Tanzania" :fi "Tansania" :sv "Tanzania"}
                                                                :value "834"}
                                                               {:label {:en "Denmark" :fi "Tanska" :sv "Danmark"}
                                                                :value "208"}
                                                               {:label {:en "Thailand" :fi "Thaimaa" :sv "Thailand"}
                                                                :value "764"}
                                                               {:label {:en "Togo" :fi "Togo" :sv "Togo"} :value "768"}
                                                               {:label {:en "Tokelau" :fi "Tokelau" :sv "Tokelau"}
                                                                :value "772"}
                                                               {:label {:en "Tonga" :fi "Tonga" :sv "Tonga"} :value "776"}
                                                               {:label {:en "Trinidad and Tobago"
                                                                        :fi "Trinidad ja Tobago"
                                                                        :sv "Trinidad och Tobago"}
                                                                :value "780"}
                                                               {:label {:en "Tunisia" :fi "Tunisia" :sv "Tunisien"}
                                                                :value "788"}
                                                               {:label {:en "Unknown" :fi "Tuntematon" :sv "Okänt land"}
                                                                :value "999"}
                                                               {:label {:en "Turkey" :fi "Turkki" :sv "Turkiet"}
                                                                :value "792"}
                                                               {:label {:en "Turkmenistan"
                                                                        :fi "Turkmenistan"
                                                                        :sv "Turkmenistan"}
                                                                :value "795"}
                                                               {:label {:en "Turks and Caicos Islands"
                                                                        :fi "Turks- ja Caicossaaret"
                                                                        :sv "Turks- och Caicosöarna"}
                                                                :value "796"}
                                                               {:label {:en "Tuvalu" :fi "Tuvalu" :sv "Tuvalu"}
                                                                :value "798"}
                                                               {:label {:en "Chad" :fi "Tšad" :sv "Tchad"} :value "148"}
                                                               {:label {:en "Czech Republic" :fi "Tšekki" :sv "Tjeckien"}
                                                                :value "203"}
                                                               {:label {:en "Uganda" :fi "Uganda" :sv "Uganda"}
                                                                :value "800"}
                                                               {:label {:en "Ukraine" :fi "Ukraina" :sv "Ukraina"}
                                                                :value "804"}
                                                               {:label {:en "Hungary" :fi "Unkari" :sv "Ungern"}
                                                                :value "348"}
                                                               {:label {:en "Uruguay" :fi "Uruguay" :sv "Uruguay"}
                                                                :value "858"}
                                                               {:label {:en "New Caledonia"
                                                                        :fi "Uusi-Kaledonia"
                                                                        :sv "Nya Kaledonien"}
                                                                :value "540"}
                                                               {:label
                                                                       {:en "New Zealand" :fi "Uusi-Seelanti" :sv "Nya Zeeland"}
                                                                :value "554"}
                                                               {:label {:en "Uzbekistan" :fi "Uzbekistan" :sv "Uzbekistan"}
                                                                :value "860"}
                                                               {:label {:en "Belarus" :fi "Valko-Venäjä" :sv "Vitryssland"}
                                                                :value "112"}
                                                               {:label {:en "Vanuatu" :fi "Vanuatu" :sv "Vanuatu"}
                                                                :value "548"}
                                                               {:label {:en "Holy See (Vatican City State)"
                                                                        :fi "Vatikaani"
                                                                        :sv "Vatikanstaten"}
                                                                :value "336"}
                                                               {:label {:en "Venezuela" :fi "Venezuela" :sv "Venezuela"}
                                                                :value "862"}
                                                               {:label
                                                                       {:en "Russian Federation" :fi "Venäjä" :sv "Ryssland"}
                                                                :value "643"}
                                                               {:label {:en "Viet Nam" :fi "Vietnam" :sv "Vietnam"}
                                                                :value "704"}
                                                               {:label {:en "Estonia" :fi "Viro" :sv "Estland"}
                                                                :value "233"}
                                                               {:label {:en "Wallis and Futuna"
                                                                        :fi "Wallis ja Futuna"
                                                                        :sv "Wallis och Futuna"}
                                                                :value "876"}
                                                               {:label {:en "United States, USA"
                                                                        :fi "Yhdysvallat (USA)"
                                                                        :sv "Förenta Staterna (USA)"}
                                                                :value "840"}
                                                               {:label {:en "Virgin Islands, U.S."
                                                                        :fi "Yhdysvaltain Neitsytsaaret"
                                                                        :sv "Amerikanska Jungfruöarna"}
                                                                :value "850"}
                                                               {:label
                                                                       {:en "United States Minor Outlying Islands"
                                                                        :fi "Yhdysvaltain pienet erillissaaret"
                                                                        :sv
                                                                            "Förenta Staternas mindre öar i Oceanien och Västindien"}
                                                                :value "581"}
                                                               {:label {:en "Zimbabwe" :fi "Zimbabwe" :sv "Zimbabwe"}
                                                                :value "716"} {:label {:fi "" :sv ""} :value ""}]
                                      :params                 {}
                                      :validators             ["required"]}
                                     {:belongs-to-hakukohderyhma ["1.2.246.562.28.93472377998"]
                                      :belongs-to-hakukohteet    ["1.2.246.562.20.43105453732"
                                                                  "1.2.246.562.20.70407522443"
                                                                  "1.2.246.562.20.51243547893"]
                                      :fieldClass                "formField"
                                      :fieldType                 "attachment"
                                      :id                        "pohjakoulutus_kk_ulk--attachement"
                                      :label
                                                                 {:en
                                                                      "Higher education qualification completed outside Finland"
                                                                  :fi
                                                                      "Muualla kuin Suomessa suoritetun korkeakoulututkinnon tutkintotodistus"
                                                                  :sv "Högskoleexamen som avlagts annanstans än i Finland"}
                                      :options                   []
                                      :params
                                                                 {:info-text
                                                                                      {:enabled? true
                                                                                       :value
                                                                                                 {:en
                                                                                                  "The attachment has to be submitted within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n"
                                                                                                  :fi
                                                                                                  "Liite täytyy tallentaa viimeistään 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\nTallenna hakemuksen liitteeksi kopio tutkintotodistuksesta mahdollisine liitteineen.\n"
                                                                                                  :sv
                                                                                                  "Bilagan ska sparas senast 7 dygn innan ansökningstiden går ut. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}
                                                                  :info-text-collapse false}
                                      :validators                []}]
                        :fieldClass "questionGroup"
                        :fieldType  "fieldset"
                        :id         "pohjakoulutus_kk_ulk"
                        :label      {:fi "" :sv ""}
                        :params     {}}
                       {:fieldClass "infoElement"
                        :fieldType  "p"
                        :id         "hbem--073d1398-b17f-47ce-adeb-13285d5c9e58"
                        :label      {:fi ""}
                        :params     {}
                        :text
                                    {:en "Click add if you want add further qualifications."
                                     :fi "Paina lisää jos haluat lisätä useampia tutkintoja."
                                     :sv "Tryck på lägg till om du vill lägga till flera examina."}}]
               :label {:en "Higher education qualification completed outside Finland"
                       :fi "Muualla kuin Suomessa suoritettu korkeakoulututkinto"
                       :sv "Högskoleexamen som avlagts annanstans än i Finland"}
               :value "pohjakoulutus_kk_ulk"}
              {:followups
                      [{:children
                                    [{:fieldClass "formField"
                                      :fieldType  "textField"
                                      :id         "pohjakoulutus_avoin--field"
                                      :label      {:en "Study field" :fi "Ala" :sv "Bransch"}
                                      :params     {:size "M"}
                                      :validators ["required"]}
                                     {:fieldClass "formField"
                                      :fieldType  "textField"
                                      :id         "pohjakoulutus_avoin--institution"
                                      :label      {:en "Higher education institution"
                                                   :fi "Korkeakoulu"
                                                   :sv "Högskola"}
                                      :params     {:size "M"}
                                      :validators ["required"]}
                                     {:fieldClass "formField"
                                      :fieldType  "textField"
                                      :id         "pohjakoulutus_avoin--module"
                                      :label      {:en "Study module"
                                                   :fi "Opintokokonaisuus"
                                                   :sv "Studiehelhet"}
                                      :params     {:size "M"}
                                      :validators ["required"]}
                                     {:fieldClass "formField"
                                      :fieldType  "textField"
                                      :id         "pohjakoulutus_avoin--scope"
                                      :label
                                                  {:en "Scope of qualification" :fi "Laajuus" :sv "Omfattning"}
                                      :params     {:numeric true :size "S"}
                                      :validators ["required" "numeric"]}
                                     {:belongs-to-hakukohderyhma []
                                      :belongs-to-hakukohteet    []
                                      :fieldClass                "formField"
                                      :fieldType                 "attachment"
                                      :id                        "pohjakoulutus_avoin--attachment"
                                      :label
                                                                 {:en
                                                                      "Open university / university of applied sciences studies"
                                                                  :fi "Todistus avoimen korkeakoulun opinnoista"
                                                                  :sv "Studier inom den öppna högskolan"}
                                      :options                   []
                                      :params
                                                                 {:hidden true
                                                                  :info-text
                                                                          {:enabled? true
                                                                           :value
                                                                                     {:en
                                                                                      "Submit your attachments in pdf/jpg/png -format. If you cannot submit your attachments online please contact the higher education institution in question directly. The attachments have to be submitted or returned by application period 1: 30.1.2019 at 3 pm Finnish time and application period 2: 10.4.2019 at 3 pm Finnish time at the latest."
                                                                                      :fi
                                                                                      "Liite täytyy tallentaa viimeistään 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n"
                                                                                      :sv
                                                                                      "Bilagan ska sparas senast 7 dygn innan ansökningstiden går ut. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"}}}}
                                     {:fieldClass "infoElement"
                                      :fieldType  "p"
                                      :id         "hbem--162f8731-589c-4920-b1e5-b827d8df06a1"
                                      :label      {:fi ""}
                                      :params     {}
                                      :text
                                                  {:en "Click add if you want add further qualifications."
                                                   :fi
                                                       "Paina lisää, jos haluat lisätä useampia opintokokonaisuuksia."
                                                   :sv
                                                       "Tryck på lägg till om du vill lägga till flera studiehelhet."}}]
                        :fieldClass "questionGroup"
                        :fieldType  "fieldset"
                        :id         "pohjakoulutus_avoin"
                        :label      {:en "" :fi "" :sv ""}
                        :params     {}}]
               :label
                      {:en
                           "Studies required by the higher education institution completed at open university or open polytechnic/UAS"
                       :fi "Korkeakoulun edellyttämät avoimen korkeakoulun opinnot"
                       :sv "Studier som högskolan kräver vid en öppen högskola"}
               :value "pohjakoulutus_avoin"}
              {:followups
                      [{:fieldClass "formField"
                        :fieldType  "textField"
                        :id         "pohjakoulutus_muu--year-of-completion"
                        :label
                                    {:en "Year of completion" :fi "Suoritusvuosi" :sv "Avlagd år"}
                        :params     {:numeric true :size "S"}
                        :validators ["numeric" "required"]}
                       {:fieldClass "formField"
                        :fieldType  "textArea"
                        :id         "pohjakoulutus_muu--description"
                        :label      {:en "Description of your other eligibility"
                                     :fi "Kelpoisuuden kuvaus"
                                     :sv "Beskrivning av behörigheten"}
                        :params     {:max-length "500"}
                        :validators ["required"]}
                       {:belongs-to-hakukohderyhma []
                        :belongs-to-hakukohteet    []
                        :fieldClass                "formField"
                        :fieldType                 "attachment"
                        :id                        "todistus-muusta-korkeakoulukelpoisuudesta"
                        :label                     {:en "Other eligibility for higher education"
                                                    :fi "Todistus muusta korkeakoulukelpoisuudesta"
                                                    :sv "Övrig högskolebehörighet"}
                        :options                   []
                        :params
                                                   {:hidden true
                                                    :info-text
                                                            {:enabled? true
                                                             :value
                                                                       {:en
                                                                        "The attachment has to be submitted within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n"
                                                                        :fi
                                                                        "Liite täytyy tallentaa viimeistään 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX."
                                                                        :sv
                                                                        "Bilagan ska sparas senast 7 dygn innan ansökningstiden går ut. Den angivna tidpunkten syns invid begäran om bilagor. \nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"}}}}]
               :label {:en "Other eligibility for higher education"
                       :fi "Muu korkeakoulukelpoisuus"
                       :sv "Övrig högskolebehörighet"}
               :value "pohjakoulutus_muu"}])

(defn module [metadata]
  (prn metadata)
  (merge (form-section metadata)
         (clojure.walk/postwalk
           (fn [form]
             (if (or (:options form) (:id form))
               (assoc form :metadata metadata)
               form))
         {:children
                      [{:belongs-to-hakukohteet   []
                        :fieldClass               "formField"
                          :fieldType                "multipleChoice"
                        :id                       "higher-completed-base-education"
                        :koodisto-ordered-by-user true
                        :koodisto-source          {:title   "Kk-pohjakoulutusvaihtoehdot"
                                                   :uri     "pohjakoulutuskklomake"
                                                   :version 1}
                        :label
                                                  {:en
                                                   "Fill in the education that you have completed  or will complete during the admission process (autumn 2019)"
                                                   :fi
                                                   "Ilmoita suorittamasi koulutukset. Ilmoita myös ne, jotka suoritat hakukautena (syksy 2019)."
                                                   :sv
                                                   "Ange utbildningar som du har avlagt. Ange också dem som du avlägger under ansökningsperioden (hösten 2019)."}
                        :options                  higher-education-module-options
                        :params                   {}
                        :rules                    {:pohjakoulutusristiriita nil}
                        :validators               ["required"]}
                       {:fieldClass "formField"
                        :fieldType  "singleChoice"
                        :id         "hbem--0bc965a3-fc4a-4b86-a1cb-25b55b414258"
                        :label
                                    {:en
                                         "Have you completed general upper secondary education or vocational qualification?"
                                     :fi
                                         "Oletko suorittanut lukion/ylioppilastutkinnon tai ammatillisen tutkinnon? "
                                     :sv "Har du avlagt gymnasiet/studentexamen eller yrkesinriktad examen?"}
                        :options
                                    [{:followups
                                             [{:fieldClass "formField"
                                               :fieldType  "dropdown"
                                               :id         "hbem--893ede6f-998e-4e66-9ca5-b10bc602c944"
                                               :koodisto-source
                                                           {:title "Maat ja valtiot" :uri "maatjavaltiot2" :version 1}
                                               :label      {:en "Choose country"
                                                            :fi "Valitse suoritusmaa"
                                                            :sv " Välj land där du avlagt examen"}
                                               :options    []
                                               :params
                                                           {:info-text
                                                            {:label
                                                             {:en
                                                              "Choose the country where you have completed your most recent qualification. If you have not yet completed a general upper secondary school syllabus/matriculation examination or vocational qualification but are in the process of doing so please choose the country where you will complete the qualification. NB: a vocational qualification can be a vocational upper secondary qualification school-level qualification post-secondary level qualification higher vocational level qualification further vocational qualification or specialist vocational qualification. Do not fill in the country where you have completed a higher education qualification."
                                                              :fi
                                                              "Merkitse viimeisimmän tutkintosi suoritusmaa. Jos sinulla ei ole vielä lukion päättötodistusta/ylioppilastutkintoa tai ammatillista tutkintoa mutta olet suorittamassa sellaista valitse se maa jossa parhaillaan suoritat kyseistä tutkintoa. Huom: ammatillinen tutkinto voi olla ammatillinen perustutkinto kouluasteen opistoasteen tai ammatillisen korkea-asteen tutkinto ammatti-tai erikoisammattitutkinto. Älä merkitse tähän korkeakoulututkinnon suoritusmaata."
                                                              :sv
                                                              "Ange land där din senaste examen avlagts. Om du ännu inte har avlagt gymnasiet/studentexamen eller yrkesinriktad examen men håller på att göra det välj då det land där du som bäst avlägger examen i fråga. Obs: yrkesinriktad examen kan vara yrkesinriktad grundexamen examen på skolnivå examen på institutsnivå yrkesinriktad examen på högre nivå yrkesexamen eller specialyrkesexamen. Ange inte här landet där du har avlagt högskoleexamen."}}}}]
                                      :label {:en "Yes" :fi "Kyllä" :sv "Ja"}
                                      :value "0"} {:label {:en "No" :fi "En" :sv "Nej"} :value "1"}]
                        :params     {:info-text {:label {:en "This is required for statistical reasons"
                                                         :fi "Tämä tieto kysytään tilastointia varten."
                                                         :sv "Denna uppgift frågas för statistik."}}}
                        :validators ["required"]}
                       {:cannot-edit false
                        :cannot-view false
                        :fieldClass  "formField"
                        :fieldType   "singleChoice"
                        :id          "finnish-vocational-before-1995"
                        :label
                                     {:en
                                      "Have you completed a university or university of applied sciences ( prev. polytechnic) degree in Finland before 2003?"
                                      :fi
                                      "Oletko suorittanut suomalaisen ammattikorkeakoulu- tai yliopistotutkinnon ennen vuotta 2003?"
                                      :sv
                                      "Har du avlagt en finländsk yrkeshögskole- eller universitetsexamen före år 2003?"}
                        :options
                                     [{:followups
                                              [{:cannot-edit false
                                                :cannot-view false
                                                :fieldClass  "formField"
                                                :fieldType   "textField"
                                                :id          "finnish-vocational-before-1995--year-of-completion"
                                                :label
                                                             {:en "Year of completion" :fi "Suoritusvuosi" :sv "Avlagd år"}
                                                :params
                                                             {:max-value "2002" :min-value "1900" :numeric true :size "S"}
                                                :validators  ["numeric" "required"]}
                                               {:cannot-edit     false
                                                :cannot-view     false
                                                :fieldClass      "formField"
                                                :fieldType       "dropdown"
                                                :id              "finnish-vocational-before-1995--degree"
                                                :koodisto-source {:title "Tutkinto" :uri "tutkinto" :version 1}
                                                :label           {:en "Name of the degree"
                                                                  :fi "Tutkinnon nimi"
                                                                  :sv "Examens namn"}
                                                :options
                                                                 [{:label {:en "" :fi "" :sv ""} :value ""}
                                                                  {:label {:en "------------------"
                                                                           :fi "------------------"
                                                                           :sv "------------------"}
                                                                   :value "XX"}
                                                                  {:label {:en "Master of Science (Architecture)"
                                                                           :fi "Arkkitehti"
                                                                           :sv "Arkitekt"}
                                                                   :value "139"}
                                                                  {:label {:en "Master of Science (Technology)"
                                                                           :fi "Diplomi-insinööri"
                                                                           :sv "Diplomingenjör"}
                                                                   :value "130"}
                                                                  {:label {:en "Bachelor of Food Sciences"
                                                                           :fi "Elintarviketieteiden kandidaatti"
                                                                           :sv "Kandidat i livsmedelsvetenskaper"}
                                                                   :value "738"}
                                                                  {:label {:en "" :fi "Elintarviketieteiden lisensiaatti" :sv ""}
                                                                   :value "718"}
                                                                  {:label {:en "Master of Food Sciences"
                                                                           :fi "Elintarviketieteiden maisteri"
                                                                           :sv "Magister i livsmedelsvetenskaper"}
                                                                   :value "717"}
                                                                  {:label {:en "Doctor of Food Sciences"
                                                                           :fi "Elintarviketieteiden tohtori"
                                                                           :sv "Doktor i livsmedelsvetenskaper"}
                                                                   :value "719"}
                                                                  {:label {:en "Bachelor of Veterinary Medicine"
                                                                           :fi "Eläinlääketieteen kandidaatti"
                                                                           :sv "Veterinärmedicine kandidat"}
                                                                   :value "032"}
                                                                  {:label {:en "Licentiate of Veterinary Medicine"
                                                                           :fi "Eläinlääketieteen lisensiaatti"
                                                                           :sv "Veterinärmedicine licentiat"}
                                                                   :value "022"}
                                                                  {:label {:en "Doctor of Veterinary Medicine"
                                                                           :fi "Eläinlääketieteen tohtori"
                                                                           :sv "Veterinärmedicine doktor"}
                                                                   :value "012"}
                                                                  {:label {:en "" :fi "Erikoiseläinlääkäri" :sv ""} :value "728"}
                                                                  {:label {:en "" :fi "Erikoishammaslääkäri" :sv ""} :value "726"}
                                                                  {:label
                                                                          {:en "" :fi "Erikoislääkäri (5-6v. uusimuotoinen)" :sv ""}
                                                                   :value "739"}
                                                                  {:label {:en "Bachelor of Science (Pharmacy)"
                                                                           :fi "Farmaseutti"
                                                                           :sv "Farmaceut"}
                                                                   :value "444"}
                                                                  {:label {:en "" :fi "Farmasian lisensiaatti" :sv ""}
                                                                   :value "424"}
                                                                  {:label {:en "Doctor of Science (Pharmacy)"
                                                                           :fi "Farmasian tohtori"
                                                                           :sv "Farmacie doktor"}
                                                                   :value "414"}
                                                                  {:label {:en "" :fi "Filosofian lisensiaatti" :sv ""}
                                                                   :value "703"}
                                                                  {:label {:en "Master of Science"
                                                                           :fi "Filosofian maisteri"
                                                                           :sv "Filosofie magister"}
                                                                   :value "702"}
                                                                  {:label {:en "Master of Arts"
                                                                           :fi "Filosofian maisteri"
                                                                           :sv "Filosofie magister"}
                                                                   :value "705"}
                                                                  {:label {:en "Doctor of Philosophy"
                                                                           :fi "Filosofian tohtori"
                                                                           :sv "Filosofie doktor"}
                                                                   :value "704"}
                                                                  {:label {:en "Bachelor of Administrative Sciences"
                                                                           :fi "Hallintotieteiden kandidaatti"
                                                                           :sv "Kandidat i förvaltnings vetenskaper"}
                                                                   :value "735"}
                                                                  {:label {:en "" :fi "Hallintotieteiden lisensiaatti" :sv ""}
                                                                   :value "323"}
                                                                  {:label {:en "Master of Administrative Sciences"
                                                                           :fi "Hallintotieteiden maisteri"
                                                                           :sv "Magister i förvaltnings vetenskaper"}
                                                                   :value "333"}
                                                                  {:label {:en "Doctor of Administrative Sciences"
                                                                           :fi "Hallintotieteiden tohtori"
                                                                           :sv "Förvaltningsdoktor"}
                                                                   :value "313"}
                                                                  {:label {:en "Bachelor of Odontology"
                                                                           :fi "Hammaslääketieteen kandidaatti"
                                                                           :sv "Odontologie kandidat"}
                                                                   :value "423"}
                                                                  {:label {:en "Licentiate of Dentistry"
                                                                           :fi "Hammaslääketieteen lisensiaatti"
                                                                           :sv "Odontologie licenciat"}
                                                                   :value "422"}
                                                                  {:label {:en "Doctor of Dental Sciences"
                                                                           :fi "Hammaslääketieteen tohtori"
                                                                           :sv "Odontologie doktor"}
                                                                   :value "412"}
                                                                  {:label {:en "Bachelor of Humanities"
                                                                           :fi "Humanistisen alan ammattikorkeakoulututkinto"
                                                                           :sv
                                                                               "Yrkeshögskoleexamen inom det humanistiska området"}
                                                                   :value "753"}
                                                                  {:label
                                                                          {:en "Master of Humanities"
                                                                           :fi "Humanistisen alan ylempi ammattikorkeakoulututkinto"
                                                                           :sv
                                                                               "Högre yrkeshögskoleexamen inom det humanistiska området"}
                                                                   :value "Y01"}
                                                                  {:label {:en "Bachelor of Arts "
                                                                           :fi "Humanististen tieteiden kandidaatti"
                                                                           :sv "Kandidat i humanistiska vetenskaper"}
                                                                   :value "701"}
                                                                  {:label
                                                                          {:en "Master of International and Comparative Law"
                                                                           :fi
                                                                               "Kansainvälisen ja vertailevan oikeustieteen maisterin tutkinto"
                                                                           :sv "Magister i internationell och komparativ rätt"}
                                                                   :value "860"}
                                                                  {:label {:en "Bachelor of Education"
                                                                           :fi "Kasvatustieteen kandidaatti"
                                                                           :sv "Pedagogie kandidat"}
                                                                   :value "850"}
                                                                  {:label {:en "" :fi "Kasvatustieteen lisensiaatti" :sv ""}
                                                                   :value "327"}
                                                                  {:label {:en "Master of Education"
                                                                           :fi "Kasvatustieteen maisteri"
                                                                           :sv "Pedagogie magister"}
                                                                   :value "337"}
                                                                  {:label {:en "Doctor of Philosophy (Education)"
                                                                           :fi "Kasvatustieteen tohtori"
                                                                           :sv "Pedagogie doktor"}
                                                                   :value "317"}
                                                                  {:label {:en "Bachelor of Beauty and Cosmetics"
                                                                           :fi "Kauneudenhoitoalan ammattikorkeakoulututkinto"
                                                                           :sv "Yrkeshögskoleexamen inom skönhetsbranschen"}
                                                                   :value "591"}
                                                                  {:label
                                                                          {:en "Master of Beauty and Cosmetics"
                                                                           :fi "Kauneudenhoitoalan ylempi ammattikorkeakoulututkinto"
                                                                           :sv "Högre yrkeshögskoleexamen inom skönhetsbranschen"}
                                                                   :value "Y91"}
                                                                  {:label
                                                                          {:en
                                                                               "Bachelor of Science (Economics and Business Administration)"
                                                                           :fi "Kauppatieteiden kandidaatti"
                                                                           :sv "Ekonomie kandidat"}
                                                                   :value "232"}
                                                                  {:label {:en "" :fi "Kauppatieteiden lisensiaatti" :sv ""}
                                                                   :value "220"}
                                                                  {:label
                                                                          {:en
                                                                               "Master of Science (Economics and Business Administration)"
                                                                           :fi "Kauppatieteiden maisteri"
                                                                           :sv "Ekonomie magister"}
                                                                   :value "231"}
                                                                  {:label
                                                                          {:en
                                                                               "Doctor of Science (Economics and Business Administration)"
                                                                           :fi "Kauppatieteiden tohtori"
                                                                           :sv "Ekonomie doktor"}
                                                                   :value "210"}
                                                                  {:label {:en "Bachelor of Culture and Arts"
                                                                           :fi "Kulttuurialan ammattikorkeakoulututkinto"
                                                                           :sv "Yrkeshögskoleexamen inom kulturbranschen"}
                                                                   :value "681"}
                                                                  {:label {:en "Master of Culture and Arts"
                                                                           :fi "Kulttuurialan ylempi ammattikorkeakoulututkinto"
                                                                           :sv "Högre yrkeshögskoleexamen inom kulturbranschen"}
                                                                   :value "Y04"}
                                                                  {:label {:en "Bachelor of Fine Arts"
                                                                           :fi "Kuvataiteen kandidaatti"
                                                                           :sv "Bildkonstkandidat"}
                                                                   :value "844"}
                                                                  {:label {:en "Master of Fine Arts"
                                                                           :fi "Kuvataiteen maisteri"
                                                                           :sv "Bildkonstmagister"}
                                                                   :value "845"}
                                                                  {:label {:en "Doctor of Fine Arts"
                                                                           :fi "Kuvataiteen tohtori"
                                                                           :sv "Doktor i bildkonst"}
                                                                   :value "853"}
                                                                  {:label {:en "" :fi "Lastentarhanopettaja" :sv ""} :value "659"}
                                                                  {:label {:en "Bachelor of Business Administration"
                                                                           :fi "Liiketalouden ammattikorkeakoulututkinto"
                                                                           :sv "Yrkeshögskoleexamen i företagsekonomi"}
                                                                   :value "311"}
                                                                  {:label {:en "Master of Business Administration"
                                                                           :fi "Liiketalouden ylempi ammattikorkeakoulututkinto"
                                                                           :sv "Högre yrkeshögskoleexamen i företagsekonomi"}
                                                                   :value "Y31"}
                                                                  {:label {:en "Bachelor of Sports Studies"
                                                                           :fi "Liikunnan ammattikorkeakoulututkinto"
                                                                           :sv "Yrkeshögskoleexamen i idrott"}
                                                                   :value "711"}
                                                                  {:label {:en "Master of Sports Studies"
                                                                           :fi "Liikunnan ylempi ammattikorkeakoulututkinto"
                                                                           :sv "Högre yrkeshögskoleexamen i idrott"}
                                                                   :value "Y41"}
                                                                  {:label {:en "Bachelor of Sport and Health Sciences"
                                                                           :fi "Liikuntatieteiden kandidaatti"
                                                                           :sv "Kandidat i gymnastik - och idrottsvetenskap"}
                                                                   :value "736"}
                                                                  {:label {:en "" :fi "Liikuntatieteiden lisensiaatti" :sv ""}
                                                                   :value "328"}
                                                                  {:label {:en "Master of Sport and Health Sciences"
                                                                           :fi "Liikuntatieteiden maisteri"
                                                                           :sv "Magister i gymnastik - och idrottsvetenskap"}
                                                                   :value "338"}
                                                                  {:label {:en "Doctor of Philosophy (Sport and Health Sciences)"
                                                                           :fi "Liikuntatieteiden tohtori"
                                                                           :sv "Doktor i gymnastik- och idrottsvetenskaper"}
                                                                   :value "318"}
                                                                  {:label {:en "Bachelor of Science"
                                                                           :fi "Luonnontieteiden kandidaatti"
                                                                           :sv "Kandidat i naturvetenskaper"}
                                                                   :value "345"}
                                                                  {:label {:en "Bachelor of Natural Resources"
                                                                           :fi "Luonnonvara-alan ammattikorkeakoulututkinto"
                                                                           :sv "Yrkeshögskoleexamen inom naturbruk"}
                                                                   :value "121"}
                                                                  {:label {:en "Master of Natural Resources"
                                                                           :fi
                                                                               "Luonnonvara-alan ylempi ammattikorkeakoulututkinto"
                                                                           :sv "Högre yrkeshögskoleexamen i naturbruk"}
                                                                   :value "Y15"}
                                                                  {:label {:en "Bachelor of Medicine"
                                                                           :fi "Lääketieteen kandidaatti"
                                                                           :sv "Medicine kandidat"}
                                                                   :value "420"}
                                                                  {:label {:en "Licentiate of Medicine"
                                                                           :fi "Lääketieteen lisensiaatti"
                                                                           :sv "Medicine licenciat"}
                                                                   :value "421"}
                                                                  {:label {:en "Doctor of Medicinal Sciences"
                                                                           :fi "Lääketieteen tohtori"
                                                                           :sv "Medicine doktor"}
                                                                   :value "410"}
                                                                  {:label {:en "Bachelor of Science (Agriculture and Forestry)"
                                                                           :fi "Maatalous- ja metsätieteiden kandidaatti"
                                                                           :sv "Kandidat i lant- och skogbroksvetenskaper"}
                                                                   :value "033"}
                                                                  {:label {:en ""
                                                                           :fi "Maatalous- ja metsätieteiden lisensiaatti"
                                                                           :sv ""}
                                                                   :value "021"}
                                                                  {:label {:en "Master of Science (Agriculture and Forestry)"
                                                                           :fi "Maatalous- ja metsätieteiden maisteri"
                                                                           :sv "Magister i lant- och skogsbroksvetenskaper"}
                                                                   :value "031"}
                                                                  {:label {:en "Doctor of Science (Agriculture and Forestry)"
                                                                           :fi "Maatalous- ja metsätieteiden tohtorin tutkinto"
                                                                           :sv "Agronomie- och forstdoktor"}
                                                                   :value "011"}
                                                                  {:label {:en "Master of Science (Landscape Architecture)"
                                                                           :fi "Maisema-arkkitehti"
                                                                           :sv "Landskapsarkitekt"}
                                                                   :value "140"}
                                                                  {:label
                                                                          {:en "Bachelor of Hospitality Management"
                                                                           :fi "Matkailu- ja ravitsemisalan ammattikorkeakoulututkinto"
                                                                           :sv
                                                                               "Yrkeshögskoleexamen inom turism- och kosthållsbranschen"}
                                                                   :value "411"}
                                                                  {:label
                                                                          {:en "Master of Hospitality Management"
                                                                           :fi
                                                                               "Matkailu- ja ravitsemisalan ylempi ammattikorkeakoulututkinto"
                                                                           :sv
                                                                               "Högre yrkeshögskoleexamen inom turism- och kosthållsbranschen"}
                                                                   :value "Y81"}
                                                                  {:label {:en "Bachelor of Marine Technology"
                                                                           :fi "Merenkulun ammattikorkeakoulututkinto"
                                                                           :sv "Yrkeshögskoleexamen i sjöfart"}
                                                                   :value "218"}
                                                                  {:label {:en "Bachelor of Engineering"
                                                                           :fi "Merenkulun ammattikorkeakoulututkinto"
                                                                           :sv "Yrkehögskoleexamen i sjöfart"}
                                                                   :value "217"}
                                                                  {:label {:en "Master of Engineering"
                                                                           :fi "Merenkulun ylempi ammattikorkeakoulututkinto"
                                                                           :sv "Högre yrkeshögskoleexamen i sjöfart"}
                                                                   :value "Y53"}
                                                                  {:label {:en "Master of Marine Technology"
                                                                           :fi "Merenkulun ylempi ammattikorkeakoulututkinto"
                                                                           :sv "Högre yrkeshögskoleexamen i sjöfart"}
                                                                   :value "Y52"}
                                                                  {:label {:en "Bachelor of Music"
                                                                           :fi "Musiikin kandidaatti"
                                                                           :sv "Musikkandidat"}
                                                                   :value "851"}
                                                                  {:label {:en "" :fi "Musiikin lisensiaatti" :sv ""}
                                                                   :value "837"}
                                                                  {:label {:en "Master of Music"
                                                                           :fi "Musiikin maisteri"
                                                                           :sv "Musikmagister"}
                                                                   :value "836"}
                                                                  {:label {:en "Doctor of Music"
                                                                           :fi "Musiikin tohtori"
                                                                           :sv "Musikdoktor"}
                                                                   :value "838"}
                                                                  {:label {:en "Bachelor of Laws"
                                                                           :fi "Oikeusnotaari"
                                                                           :sv "Rättsnotarie"}
                                                                   :value "351"}
                                                                  {:label {:en "" :fi "Oikeustieteen lisensiaatti" :sv ""}
                                                                   :value "322"}
                                                                  {:label {:en "Master of Laws"
                                                                           :fi "Oikeustieteen maisteri"
                                                                           :sv "Juris magister"}
                                                                   :value "332"}
                                                                  {:label {:en "Doctor of Laws"
                                                                           :fi "Oikeustieteen tohtori"
                                                                           :sv "Juris doktor"}
                                                                   :value "312"}
                                                                  {:label {:en "Master of Science (Pharmacy)"
                                                                           :fi "Proviisori"
                                                                           :sv "Provisor"}
                                                                   :value "433"}
                                                                  {:label {:en "Bachelor of Arts (Psychology)"
                                                                           :fi "Psykologian kandidaatti"
                                                                           :sv "Psykologie kandidat"}
                                                                   :value "631"}
                                                                  {:label {:en "" :fi "Psykologian lisensiaatti" :sv ""}
                                                                   :value "633"}
                                                                  {:label {:en "Master of Arts (Psychology)"
                                                                           :fi "Psykologian maisteri"
                                                                           :sv "Psykologie magister"}
                                                                   :value "632"}
                                                                  {:label {:en "Doctor of Philosophy (Psychology)"
                                                                           :fi "Psykologian tohtori"
                                                                           :sv "Psykologie doktor"}
                                                                   :value "634"}
                                                                  {:label
                                                                          {:en ""
                                                                           :fi
                                                                               "Sisäisen turvallisuuden alan ammattikorkeakoulututkinto"
                                                                           :sv ""}
                                                                   :value "861"}
                                                                  {:label
                                                                          {:en "Bachelor of Health Care"
                                                                           :fi "Sosiaali- ja terveysalan ammattikorkeakoulututkinto"
                                                                           :sv
                                                                               "Yrkeshögskoleexamen inom hälsovård och det sociala området"}
                                                                   :value "531"}
                                                                  {:label
                                                                          {:en "Bachelor of Social Services and Health Care"
                                                                           :fi "Sosiaali- ja terveysalan ammattikorkeakoulututkinto"
                                                                           :sv "Yrkeshögskoleexamen inom social- och hälsoområdet"}
                                                                   :value "532"}
                                                                  {:label
                                                                          {:en "Master of Health Care"
                                                                           :fi
                                                                               "Sosiaali- ja terveysalan ylempi ammattikorkeakoulututkinto"
                                                                           :sv
                                                                               "Högre yrkeshögskoleexamen inom hälsovård och det sociala området"}
                                                                   :value "Y19"}
                                                                  {:label
                                                                          {:en "Master of Social Services"
                                                                           :fi
                                                                               "Sosiaali- ja terveysalan ylempi ammattikorkeakoulututkinto"
                                                                           :sv
                                                                               "Högre yrkeshögskoleexamen inom social- och hälsoområdet"}
                                                                   :value "Y21"}
                                                                  {:label
                                                                          {:en "Master of Social Services and Health Care"
                                                                           :fi
                                                                               "Sosiaali- ja terveysalan ylempi ammattikorkeakoulututkinto"
                                                                           :sv
                                                                               "Högre yrkeshögskoleexamen inom social- och hälsoområdet"}
                                                                   :value "Y22"}
                                                                  {:label
                                                                          {:en "Master of Social Services and Health Care"
                                                                           :fi
                                                                               "Sosiaali- ja terveysalan ylempi ammattikorkeakoulututkinto"
                                                                           :sv
                                                                               "Högre yrkeshögskoleexamen inom social- och hälsoområdet"}
                                                                   :value "Y20"}
                                                                  {:label {:en "Bachelor of Arts (Military Science)"
                                                                           :fi "Sotatieteiden kandidaatti"
                                                                           :sv "Kandidat i militärvetenskaper"}
                                                                   :value "855"}
                                                                  {:label {:en "Master of Arts (Military Science)"
                                                                           :fi "Sotatieteiden maisteri"
                                                                           :sv "Magister i militärvetenskaper"}
                                                                   :value "856"}
                                                                  {:label {:en "Doctor of Arts (Military Science)"
                                                                           :fi "Sotatieteiden tohtori"
                                                                           :sv "Doktor i militärvetenskaper"}
                                                                   :value "857"}
                                                                  {:label {:en "Bachelor of Arts (Art and Design)"
                                                                           :fi "Taiteen kandidaatti"
                                                                           :sv "Konstkandidat"}
                                                                   :value "847"}
                                                                  {:label {:en "Master of Arts (Art and Design)"
                                                                           :fi "Taiteen maisteri"
                                                                           :sv "Konstmagister"}
                                                                   :value "830"}
                                                                  {:label {:en "Doctor of Arts (Art and Design)"
                                                                           :fi "Taiteen tohtori"
                                                                           :sv "Konstdoktor"}
                                                                   :value "832"}
                                                                  {:label {:en "Bachelor of Arts (Dance)"
                                                                           :fi "Tanssitaiteen kandidaatti"
                                                                           :sv "Danskonstkandidat"}
                                                                   :value "848"}
                                                                  {:label {:en "" :fi "Tanssitaiteen lisensiaatti" :sv ""}
                                                                   :value "840"}
                                                                  {:label {:en "Master of Arts (Dance)"
                                                                           :fi "Tanssitaiteen maisteri"
                                                                           :sv "Magister i danskonst"}
                                                                   :value "839"}
                                                                  {:label {:en "Doctor of Arts (Dance)"
                                                                           :fi "Tanssitaiteen tohtori"
                                                                           :sv "Doktor i danskonst"}
                                                                   :value "841"}
                                                                  {:label {:en "Bachelor of Arts (Theatre and Drama)"
                                                                           :fi "Teatteritaiteen kandidaatti"
                                                                           :sv "Kandidat i teaterkonst"}
                                                                   :value "737"}
                                                                  {:label {:en "" :fi "Teatteritaiteen lisensiaatti" :sv ""}
                                                                   :value "721"}
                                                                  {:label {:en "Master of Arts (Theatre and Drama)"
                                                                           :fi "Teatteritaiteen maisteri"
                                                                           :sv "Magister i teaterkonst"}
                                                                   :value "720"}
                                                                  {:label {:en "Doctor of Arts (Theatre and Drama)"
                                                                           :fi "Teatteritaiteen tohtori"
                                                                           :sv "Doktor i teaterkonst"}
                                                                   :value "722"}
                                                                  {:label {:en "Bachelor of Laboratory Services"
                                                                           :fi "Tekniikan ammattikorkeakoulututkinto"
                                                                           :sv "Yrkeshögskoleexamen inom teknik"}
                                                                   :value "212"}
                                                                  {:label {:en "Bachelor of Construction Architecture"
                                                                           :fi "Tekniikan ammattikorkeakoulututkinto"
                                                                           :sv "Yrkeshögskoleexamen inom teknik"}
                                                                   :value "213"}
                                                                  {:label {:en "Bachelor of Construction Management"
                                                                           :fi "Tekniikan ammattikorkeakoulututkinto"
                                                                           :sv "Yrkeshögskoleexamen inom teknik"}
                                                                   :value "214"}
                                                                  {:label {:en "Bachelor of Engineering"
                                                                           :fi "Tekniikan ammattikorkeakoulututkinto"
                                                                           :sv "Yrkeshögskoleexamen i teknik"}
                                                                   :value "211"}
                                                                  {:label {:en "Bachelor of Science (Technology)"
                                                                           :fi "Tekniikan kandidaatti"
                                                                           :sv "Teknologie kandidat"}
                                                                   :value "150"}
                                                                  {:label {:en "Bachelor of Science (Architecture)"
                                                                           :fi "Tekniikan kandidaatti (Arkkitehtuuri)"
                                                                           :sv "Teknologie kandidat (Arkitektur)"}
                                                                   :value "151"}
                                                                  {:label {:en "" :fi "Tekniikan lisensiaatti" :sv ""}
                                                                   :value "120"}
                                                                  {:label {:en "Doctor of Science in Technology"
                                                                           :fi "Tekniikan tohtori"
                                                                           :sv "Teknologie doktor"}
                                                                   :value "110"}
                                                                  {:label {:en "Master of Laboratory Services"
                                                                           :fi "Tekniikan ylempi ammattikorkeakoulututkinto"
                                                                           :sv "Högre yrkeshögskoleexamen i teknik"}
                                                                   :value "Y54"}
                                                                  {:label {:en "Master of Engineering"
                                                                           :fi "Tekniikan ylempi ammattikorkeakoulututkinto"
                                                                           :sv "Högre yrkeshögskoleexamen i teknik"}
                                                                   :value "Y51"}
                                                                  {:label {:en "Bachelor of Theology"
                                                                           :fi "Teologian kandidaatti"
                                                                           :sv "Teologie kandidat"}
                                                                   :value "849"}
                                                                  {:label {:en "" :fi "Teologian lisensiaatti" :sv ""}
                                                                   :value "321"}
                                                                  {:label {:en "Master of Theology"
                                                                           :fi "Teologian maisteri"
                                                                           :sv "Teologian maisteri"}
                                                                   :value "331"}
                                                                  {:label {:en "Doctor of Theology"
                                                                           :fi "Teologian tohtori"
                                                                           :sv "Teologie doktor"}
                                                                   :value "310"}
                                                                  {:label {:en "Bachelor of Science (Health Care)"
                                                                           :fi "Terveystieteiden kandidaatti"
                                                                           :sv "Kandidat i hälsovetenskaper"}
                                                                   :value "854"}
                                                                  {:label {:en "" :fi "Terveystieteiden lisensiaatti" :sv ""}
                                                                   :value "715"}
                                                                  {:label {:en "Master of Science (Health Care)"
                                                                           :fi "Terveystieteiden maisteri"
                                                                           :sv "Magister i hälsovetenskaper"}
                                                                   :value "714"}
                                                                  {:label {:en "Doctor of Health Sciences"
                                                                           :fi "Terveystieteiden tohtori"
                                                                           :sv "Doktor i hälsovetenskaper"}
                                                                   :value "716"}
                                                                  {:label {:en "Bachelor of Social Sciences "
                                                                           :fi "Valtiotieteiden kandidaatti"
                                                                           :sv "Politices kandidat"}
                                                                   :value "733"}
                                                                  {:label {:en "" :fi "Valtiotieteiden lisensiaatti" :sv ""}
                                                                   :value "326"}
                                                                  {:label {:en "Master of Social Sciences"
                                                                           :fi "Valtiotieteiden maisteri"
                                                                           :sv "Politices magister"}
                                                                   :value "336"}
                                                                  {:label {:en "Doctor of Social Sciences"
                                                                           :fi "Valtiotieteiden tohtori"
                                                                           :sv "Politices doktor"}
                                                                   :value "316"}
                                                                  {:label {:en "Bachelor of Social Sciences"
                                                                           :fi "Yhteiskuntatieteiden kandidaatti"
                                                                           :sv "Kandidat i samhällsvetenskaper"}
                                                                   :value "732"}
                                                                  {:label {:en "" :fi "Yhteiskuntatieteiden lisensiaatti" :sv ""}
                                                                   :value "329"}
                                                                  {:label {:en "Master of Social Sciences"
                                                                           :fi "Yhteiskuntatieteiden maisteri"
                                                                           :sv "Magister i samhällsvetenskaper"}
                                                                   :value "339"}
                                                                  {:label {:en "Doctor of Social Sciences"
                                                                           :fi "Yhteiskuntatieteiden tohtori"
                                                                           :sv "Doktor i samhällsvetenskaper"}
                                                                   :value "319"}
                                                                  {:label {:en "" :fi "Yleisesikuntaupseeri" :sv ""}
                                                                   :value "859"}]
                                                :params          {}
                                                :validators      ["required"]}
                                               {:cannot-edit false
                                                :cannot-view false
                                                :fieldClass  "formField"
                                                :fieldType   "textField"
                                                :id          "finnish-vocational-before-1995--other-institution"
                                                :label       {:en "Higher education institution"
                                                              :fi "Korkeakoulu"
                                                              :sv "Högskola"}
                                                :params      {}
                                                :validators  ["required"]}]
                                       :label {:en "Yes" :fi "Kyllä" :sv "Ja"}
                                       :value "0"} {:label {:en "No" :fi "Ei" :sv "Nej"} :value "1"}]
                        :params
                                     {:info-text
                                      {:label
                                       {:en
                                        "Write your university or university of applied sciences degree only if you have completed it before 2003. After that date the information of completed degrees will be received automatically from the higher education institutions. If you have completed a university/university of applied sciences degree or have received a study place in higher education in Finland after autumn 2014 your admission can be affected. More information on: \n<a href=\"https://studyinfo.fi/wp2/en/higher-education/applying/quota-for-first-time-applicants/\" target=\"_blank\">the quota for first -time applicant</a>"
                                        :fi
                                        "Merkitse tähän suorittamasi korkeakoulututkinto vain siinä tapauksessa, jos olet suorittanut sen ennen vuotta 2003. Sen jälkeen suoritetut tutkinnot saadaan automaattisesti korkeakouluilta. Suomessa suoritettu korkeakoulututkinto tai syksyllä 2014 tai sen jälkeen alkaneesta koulutuksesta vastaanotettu korkeakoulututkintoon johtava opiskelupaikka voivat vaikuttaa valintaan. Lue lisää : <a href=\"https://opintopolku.fi/wp/valintojen-tuki/yhteishaku/korkeakoulujen-yhteishaku/ensikertalaiskiintio/\" target=\"_blank\">ensikertalaisuuskiintiöstä</a>"
                                        :sv
                                        "Ange här den högskoleexamen som du avlagt före år 2003. Examina som avlagts efter detta fås automatiskt av högskolorna. En högskoleexamen som avlagts i Finland eller en studieplats inom utbildning som leder till högskoleexamen som mottagits år 2014 eller senare kan inverka på antagningen. Läs mera om: <a href=\"https://studieinfo.fi/wp/stod-for-studievalet/gemensam-ansokan/gemensam-ansokan-till-hogskolor/kvot-for-forstagangssokande/\" target=\"_blank\">kvoten för förstagångssökande</a>"}}}
                        :validators  ["required"]}]
          :fieldClass "wrapperElement"
          :fieldType  "fieldset"
          :id         "higher-base-education-module"
          :label      {:en "Your educational background"
                       :fi "Pohjakoulutuksesi "
                       :sv "Utbildningsbakgrund"}
          :params     {}})))

(def higher-education-base-education-questions
  (->> (module {})
       :children
       util/flatten-form-fields
       (map (comp name :id))
       set))
