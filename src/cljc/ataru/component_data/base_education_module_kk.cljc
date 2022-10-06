(ns ataru.component-data.base-education-module-kk
  (:require [ataru.translations.texts :refer [higher-base-education-module-texts general-texts virkailija-texts]]))

(defn- seven-day-attachment-followup
  [label]
  {:params {:hidden false,
            :deadline nil,
            :info-text {:value {:en "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.

                                     Name the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.

                                     Scan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright.

                                     Recommended file formats are: PDF, JPG, PNG and DOCX.",
                                :fi "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.

                                     Nimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus

                                     Skannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.

                                     Suositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.
                                                                                                                                                           ",
                                :sv "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.

                                     Namnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg

                                     Skanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.
                                     Samla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.
                                     Kontrollera att dokumenten i filen är rättvända.

                                     Rekommenderade filformat är PDF, JPG, PNG och DOCX.
                                                                                                                                                           "},
                        :enabled? true}},
   :fieldClass "formField",
   :label label
   :fieldType "attachment"})

(def my-large-json
  {:label (:educational-background higher-base-education-module-texts),
   :children [{:params {:hidden false,
                        :info-text {:label {:en "[Read more about who can apply for bachelor's and master's programmes](https://opintopolku.fi/konfo/en/sivu/how-to-apply-for-bachelors-and-masters)",
                                            :fi "Lue lisää siitä millä koulutuksella voit hakea
                                               - [yliopistokoulutuksiin](https://opintopolku.fi/konfo/fi/sivu/valmistaudu-korkeakoulujen-yhteishakuun#hakukelpoisuus-yliopistoon)
                                               - [ammattikorkeakoulutuksiin](https://opintopolku.fi/konfo/fi/sivu/valmistaudu-korkeakoulujen-yhteishakuun#hakukelpoisuus-ammattikorkeakouluun)",
                                            :sv "Mer information om vem som kan söka till
                                               - [universitetsutbildning](https://opintopolku.fi/konfo/sv/sivu/forbered-dig-for-gemensam-ansokan-till-hogskolor#anskningsbehrighet-till-universitet)
                                               - [yrkeshögskoleutbildning](https://opintopolku.fi/konfo/sv/sivu/forbered-dig-for-gemensam-ansokan-till-hogskolor#anskningsbehrighet-till-yrkeshgskolor)
                                               "}}},
               :koodisto-source {:uri "pohjakoulutuskklomake",
                                 :title "Kk-pohjakoulutusvaihtoehdot",
                                 :version 2,
                                 :allow-invalid? false},
               :koodisto-ordered-by-user true,
               :validators ["required"],
               :fieldClass "formField",
               :label (:completed-education higher-base-education-module-texts),
               :options [{:label (:matriculation-exam-in-finland higher-base-education-module-texts),
                          :value "pohjakoulutus_yo",
                          :followups [{:label (:year-of-completion higher-base-education-module-texts),
                                       :params {:size "S",
                                                :hidden false,
                                                :numeric true,
                                                :max-value "2022",
                                                :min-value "1900"},
                                       :options [{:label {:fi "", :sv ""},
                                                  :value "0",
                                                  :condition {:answer-compared-to 1990, :comparison-operator "<"},
                                                  :followups [(seven-day-attachment-followup  {:en "Finnish matriculation examination certificate",
                                                                                               :fi "Ylioppilastutkintotodistus",
                                                                                               :sv "Studentexamensbetyg"})]}
                                                 {:label {:fi "", :sv ""},
                                                  :value "1",
                                                  :condition {:answer-compared-to 1989, :comparison-operator ">"},
                                                  :followups [{:text {:en "Your matriculation examination details are received automatically from the national registry for study rights and completed studies. You can check your study rights and completed studies in Finland from [My Studyinfo's](https://studyinfo.fi/oma-opintopolku/) section: My completed studies (Only available in Finnish/Swedish). If your information is incorrect or information is missing, please contact the Matriculation Examination Board to correct any errors. ",
                                                                      :fi "Saamme ylioppilastutkinnon suoritustietosi ylioppilastutkintorekisteristä. Voit tarkistaa opintosuorituksesi [Oma Opintopolku -palvelun](https://opintopolku.fi/oma-opintopolku/) Omat opintosuoritukseni -osiosta. Jos tiedossasi on puutteita, ole yhteydessä ylioppilastutkintolautakuntaan tietojen korjaamiseksi.
                                                                         ",
                                                                      :sv "Vi får uppgifterna om din studentexamen ur studentexamensregistret. Du kan kolla dina studieprestationer i [Min Studieinfos](https://studieinfo.fi/oma-opintopolku/) del Mina studier. Om dina uppgifter är felaktiga, ska du kontakta Studentexamensnämnden som kan korrigera felen.
                                                                         "},
                                                               :label {:fi ""},
                                                               :fieldType "p",
                                                               :fieldClass "infoElement"}]}],
                                       :fieldType "textField",
                                       :fieldClass "formField",
                                       :validators ["numeric" "required"]}]}
                         {:label (:pohjakoulutus_amp virkailija-texts),
                          :value "pohjakoulutus_amp",
                          :followups [{:text {:en "Please make sure that your degree is truly a Finnish vocational upper secondary qualification (ammatillinen perustutkinto). As a rule, these degrees were not available before 1994. It is not possible to enter the year of completion earlier than 1994 on the form.",
                                              :fi "Tarkistathan, että kyseessä on varmasti ammatillinen perustutkinto. Näitä tutkintoja on voinut suorittaa pääsääntöisesti vuodesta 1994 alkaen. Vuotta 1994 aiempia suoritusvuosia ammatilliselle perustutkinnolle ei lomakkeella pysty ilmoittamaan.",
                                              :sv "Kontrollera att det verkligen är en yrkesinriktad grundexamen. Dessa examina har i regel kunnat avläggas från och med 1994. Det är inte möjligt att ange tidigare än år 1994 avlagda examina på blanketten."},
                                       :label {:fi ""},
                                       :fieldType "p",
                                       :fieldClass "infoElement"}
                                      {:label {:fi "", :sv ""},
                                       :children [{:label {:en "Year of completion", :fi "Suoritusvuosi", :sv "Avlagd år"},
                                                   :params {:size "S",
                                                            :numeric true,
                                                            :max-value "2022",
                                                            :min-value "1994"},
                                                   :options [{:label {:fi "", :sv ""},
                                                              :value "1",
                                                              :condition {:answer-compared-to 2022,
                                                                          :comparison-operator "="},
                                                              :followups [{:label {:en "Have you graduated",
                                                                                   :fi "Oletko valmistunut?",
                                                                                   :sv "Har du tagit examen"},
                                                                           :options [{:label (:yes general-texts),
                                                                                      :value "0",
                                                                                      :followups []}
                                                                                     {:label (:have-not general-texts),
                                                                                      :value "1",
                                                                                      :followups [{:label {:en "Estimated graduation date (dd.mm.yyyy)",
                                                                                                           :fi "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)",
                                                                                                           :sv "Beräknat examensdatum (dd.mm.åååå)"},
                                                                                                   :params {:size "S",
                                                                                                            :numeric false,
                                                                                                            :decimals nil},
                                                                                                   :fieldType "textField",
                                                                                                   :fieldClass "formField",
                                                                                                   :validators ["required"]}
                                                                                                  (seven-day-attachment-followup {:en "Preliminary certificate from the educational institution",
                                                                                                                                  :fi "Ennakkoarvio ammatillisesta perustutkinnosta",
                                                                                                                                  :sv "Läroanstaltens preliminär intyg om yrkesinriktad grundexamen"})]}],
                                                                           :fieldType "singleChoice",
                                                                           :fieldClass "formField",
                                                                           :validators ["required"]}]}
                                                             {:label {:fi "", :sv ""},
                                                              :value "2",
                                                              :condition {:answer-compared-to 2017,
                                                                          :comparison-operator ">"},
                                                              :followups [{:text {:en "Your final vocational qualification details are received automatically from the national registry for study rights and completed studies. You can check your study rights and completed studies in Finland from [My Studyinfo's](https://studyinfo.fi/oma-opintopolku/) section: My completed studies (Only available in Finnish/Swedish). If your information is incorrect or information is missing, please contact the educational institution to correct any errors.
                                                                                     ",
                                                                                  :fi "Saamme lopulliset ammatillisen perustutkinnon suoritustietosi valtakunnallisesta [Koski-tietovarannosta](https://www.oph.fi/fi/palvelut/koski-tietovaranto). Voit tarkistaa opintosuorituksesi [Oma Opintopolku -palvelun](https://opintopolku.fi/oma-opintopolku/) Omat opintosuoritukseni -osiosta. Jos tiedoissasi on puutteita, ole yhteydessä oppilaitokseesi tietojen korjaamiseksi.
                                                                                     ",
                                                                                  :sv "Vi får slutliga uppgifterna om din yrkesinriktade grundexamen ur den nationella [informationsresursen Koski](https://www.oph.fi/sv/tjanster/informationsresurser-koski). Du kan kolla dina studieprestationer i [Min Studieinfos](https://studieinfo.fi/oma-opintopolku/) del Mina studier. Om dina uppgifter är felaktiga, ska du kontakta din läroanstalt som kan korrigera felen.
                                                                                     "},
                                                                           :label {:fi ""},
                                                                           :fieldType "p",
                                                                           :fieldClass "infoElement"}]}
                                                             {:label {:fi "", :sv ""},
                                                              :value "3",
                                                              :condition {:answer-compared-to 2017,
                                                                          :comparison-operator "="},
                                                              :followups [{:label {:en "Have you completed your qualification as a competence based qualification in its entirety?",
                                                                                   :fi "Oletko suorittanut ammatillisen perustutkinnon näyttötutkintona?",
                                                                                   :sv "Har du avlagt examen som fristående yrkesexamen?"},
                                                                           :params {:info-text {:label nil}},
                                                                           :options [{:label (:yes general-texts),
                                                                                      :value "0",
                                                                                      :followups [{:text {:fi "Huomaathan, ettet ole mukana todistusvalinnassa, jos olet suorittanut tutkinnon näyttötutkintona. ",
                                                                                                          :sv "Obs! En examen som är avlagd som fristående examen beaktas inte i betygsbaserad antagning."},
                                                                                                   :label {:fi ""},
                                                                                                   :fieldType "p",
                                                                                                   :fieldClass "infoElement"}
                                                                                                  (seven-day-attachment-followup {:en "Vocational qualification diploma",
                                                                                                                                  :fi "Ammatillisen perustutkinnon tutkintotodistus",
                                                                                                                                  :sv "Yrkesinriktad grundexamens betyg"})]}
                                                                                     {:label (:have-not general-texts),
                                                                                      :value "1",
                                                                                      :followups [{:text {:en "Your final vocational qualification details are received automatically from the national registry for study rights and completed studies. You can check your study rights and completed studies in Finland from [My Studyinfo's](https://studyinfo.fi/oma-opintopolku/) section: My completed studies (Only available in Finnish/Swedish). If your information is incorrect or information is missing, please contact the educational institution to correct any errors.
                                                                                                             ",
                                                                                                          :fi "Saamme lopulliset ammatillisen perustutkinnon suoritustietosi valtakunnallisesta [Koski-tietovarannosta](https://www.oph.fi/fi/palvelut/koski-tietovaranto). Voit tarkistaa opintosuorituksesi [Oma Opintopolku -palvelun](https://opintopolku.fi/oma-opintopolku/) Omat opintosuoritukseni -osiosta. Jos tiedoissasi on puutteita, ole yhteydessä oppilaitokseesi tietojen korjaamiseksi.
                                                                                                             ",
                                                                                                          :sv "Vi får slutliga uppgifterna om din yrkesinriktade grundexamen ur den nationella [informationsresursen Koski](https://www.oph.fi/sv/tjanster/informationsresurser-koski). Du kan kolla dina studieprestationer i [Min Studieinfos](https://studieinfo.fi/oma-opintopolku/) del Mina studier. Om dina uppgifter är felaktiga, ska du kontakta din läroanstalt som kan korrigera felen.
                                                                                                             "},
                                                                                                   :label {:fi ""},
                                                                                                   :fieldType "p",
                                                                                                   :fieldClass "infoElement"}]}],
                                                                           :fieldType "singleChoice",
                                                                           :fieldClass "formField",
                                                                           :validators ["required"]}]}],
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["numeric" "required"]}
                                                  {:label (:vocational-qualification higher-base-education-module-texts),
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["required"]}
                                                  {:label {:en "Scope of vocational qualification",
                                                           :fi "Ammatillisen tutkinnon laajuus",
                                                           :sv "Omfattning av yrkesinriktad examen"},
                                                   :params {:size "S", :numeric true, :decimals 1},
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["numeric" "required"]}
                                                  {:label {:en "The scope unit",
                                                           :fi "Laajuuden yksikkö",
                                                           :sv "Omfattningens enhet"},
                                                   :options [{:label {:en "Courses", :fi "Kurssia", :sv "Kurser"},
                                                              :value "0"}
                                                             {:label (:ects-credits higher-base-education-module-texts),
                                                              :value "1"}
                                                             {:label (:study-weeks higher-base-education-module-texts),
                                                              :value "2"}
                                                             {:label (:competence-points higher-base-education-module-texts),
                                                              :value "3"}
                                                             {:label (:hours higher-base-education-module-texts),
                                                              :value "4"}
                                                             {:label (:weekly-lessons higher-base-education-module-texts),
                                                              :value "5"}
                                                             {:label (:years higher-base-education-module-texts),
                                                              :value "6"}],
                                                   :fieldType "dropdown",
                                                   :fieldClass "formField",
                                                   :validators ["required"]}
                                                  {:params {:info-text {:label {:en "Select \"Unknown\", if the educational institution where you have completed your degree cannot be found on the list.",
                                                                                :fi "Valitse \"Tuntematon\", jos et löydä listasta oppilaitosta, jossa olet suorittanut tutkintosi.",
                                                                                :sv "Välj \"Okänd\" om du inte hittar den läroanstalt, där du har avlagt din examen."}}},
                                                   :koodisto-source {:uri "oppilaitostyyppi",
                                                                     :title "Ammatilliset oppilaitokset",
                                                                     :version 1,
                                                                     :allow-invalid? true},
                                                   :validators ["required"],
                                                   :fieldClass "formField",
                                                   :label (:educational-institution higher-base-education-module-texts),
                                                   :options [],
                                                   :fieldType "dropdown"}
                                                  {:text {:fi ""},
                                                   :label {:en "Click add if you want to add further qualifications.",
                                                           :fi "Paina lisää, jos haluat lisätä useampia tutkintoja.",
                                                           :sv "Tryck på lägg till om du vill lägga till flera examina."},
                                                   :fieldType "p",
                                                   :fieldClass "infoElement"}],
                                       :fieldType "fieldset",
                                       :fieldClass "questionGroup"}]}
                         {:label (:finnish-vocational-or-special higher-base-education-module-texts),
                          :value "pohjakoulutus_amt",
                          :followups [{:label {:fi "", :sv ""},
                                       :children [{:label (:year-of-completion higher-base-education-module-texts),
                                                   :params {:size "S",
                                                            :numeric true,
                                                            :max-value "2022",
                                                            :min-value "1900"},
                                                   :options [{:label {:fi "", :sv ""},
                                                              :value "0",
                                                              :condition {:answer-compared-to 2022,
                                                                          :comparison-operator "="},
                                                              :followups [{:label {:en "Have you graduated?",
                                                                                   :fi "Oletko valmistunut?",
                                                                                   :sv "Har du tagit examen?"},
                                                                           :options [{:label {:en "Yes",
                                                                                              :fi "Kyllä",
                                                                                              :sv "Ja"},
                                                                                      :value "0",
                                                                                      :followups []}
                                                                                     {:label {:en "No",
                                                                                              :fi "En",
                                                                                              :sv "Nej"},
                                                                                      :value "1",
                                                                                      :followups [{:label {:en "Estimated graduation date (dd.mm.yyyy)",
                                                                                                           :fi "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)",
                                                                                                           :sv "Beräknat examensdatum (dd.mm.åååå)"},
                                                                                                   :params {:size "S",
                                                                                                            :numeric false,
                                                                                                            :decimals nil},
                                                                                                   :fieldType "textField",
                                                                                                   :fieldClass "formField",
                                                                                                   :validators ["required"]}
                                                                                                  (seven-day-attachment-followup {:en "Preliminary certificate from the educational institution",
                                                                                                                                  :fi "Ennakkoarvio ammattitutkinnosta",
                                                                                                                                  :sv "Läroanstaltens preliminär intyg"})]}],
                                                                           :fieldType "singleChoice",
                                                                           :fieldClass "formField",
                                                                           :validators ["required"]}]}
                                                             {:label {:fi "", :sv ""},
                                                              :value "1",
                                                              :condition {:answer-compared-to 2018,
                                                                          :comparison-operator "<"},
                                                              :followups [(seven-day-attachment-followup {:en "Vocational or specialist vocational qualification diploma",
                                                                                                          :fi "Tutkintotodistus ammatti- tai erikoisammattitutkinnosta",
                                                                                                          :sv "Betyg av yrkesexamen eller en specialyrkesexamen"})]}
                                                             {:label {:fi "", :sv ""},
                                                              :value "2",
                                                              :condition {:answer-compared-to 2017,
                                                                          :comparison-operator ">"},
                                                              :followups [{:text {:en "Your final vocational qualification details are received automatically from the national registry for study rights and completed studies. You can check your study rights and completed studies in Finland from [My Studyinfo's](https://studyinfo.fi/oma-opintopolku/) section: My completed studies (Only available in Finnish/Swedish). If your information is incorrect or information is missing, please contact the educational institution to correct any errors. ",
                                                                                  :fi "Saamme lopulliset ammattitutkinnon suoritustietosi valtakunnallisesta [Koski-tietovarannosta](https://www.oph.fi/fi/palvelut/koski-tietovaranto). Voit tarkistaa opintosuorituksesi [Oma Opintopolku -palvelun](https://opintopolku.fi/oma-opintopolku/) Omat opintosuoritukseni -osiosta. Jos tiedoissasi on puutteita, ole yhteydessä oppilaitokseesi tietojen korjaamiseksi. ",
                                                                                  :sv "Vi får de slutliga uppgifterna om din yrkesexamen ur den nationella [informationsresursen Koski](https://www.oph.fi/sv/tjanster/informationsresurser-koski). Du kan kontrollera dina studieprestationer i [Min Studieinfos](https://studieinfo.fi/oma-opintopolku/) del Mina studier. Om dina uppgifter är felaktiga, ska du kontakta din läroanstalt som kan korrigera felen."},
                                                                           :label {:fi ""},
                                                                           :fieldType "p",
                                                                           :fieldClass "infoElement"}]}],
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["numeric" "required"]}
                                                  {:label {:en "Qualification", :fi "Tutkinto", :sv "Examen"},
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["required"]}
                                                  {:label {:en "Scope of qualification",
                                                           :fi "Laajuus",
                                                           :sv "Examens omfattning"},
                                                   :params {:size "S", :numeric true, :decimals 1},
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["numeric"]}
                                                  {:label {:en "The scope unit",
                                                           :fi "Laajuuden yksikkö",
                                                           :sv "Omfattningens enhet"},
                                                   :options [{:label {:en "Courses", :fi "Kurssia", :sv "Kurser"},
                                                              :value "0"}
                                                             {:label {:en "ECTS credits",
                                                                      :fi "Opintopistettä",
                                                                      :sv "Studiepoäng"},
                                                              :value "1"}
                                                             {:label {:en "Study weeks",
                                                                      :fi "Opintoviikkoa",
                                                                      :sv "Studieveckor"},
                                                              :value "2"}
                                                             {:label {:en "Competence points",
                                                                      :fi "Osaamispistettä",
                                                                      :sv "Kompetenspoäng"},
                                                              :value "3"}
                                                             {:label {:en "Hours", :fi "Tuntia", :sv "Timmar"},
                                                              :value "4"}
                                                             {:label {:en "Weekly lessons per year",
                                                                      :fi "Vuosiviikkotuntia",
                                                                      :sv "Årsveckotimmar"},
                                                              :value "5"}
                                                             {:label {:en "Years", :fi "Vuotta", :sv "År"}, :value "6"}],
                                                   :fieldType "dropdown",
                                                   :fieldClass "formField",
                                                   :validators []}
                                                  {:params {:info-text {:label {:en "Select \"Unknown\", if the educational institution where you have completed your degree cannot be found on the list.",
                                                                                :fi "Valitse \"Tuntematon\", jos et löydä listasta oppilaitosta, jossa olet suorittanut tutkintosi.",
                                                                                :sv "Välj \"Okänd\" om du inte hittar den läroanstalt, där du har avlagt din examen."}}},
                                                   :koodisto-source {:uri "oppilaitostyyppi",
                                                                     :title "Ammatilliset oppilaitokset",
                                                                     :version 1,
                                                                     :allow-invalid? true},
                                                   :validators ["required"],
                                                   :fieldClass "formField",
                                                   :label {:en "Educational institution",
                                                           :fi "Oppilaitos ",
                                                           :sv "Läroanstalt "},
                                                   :options [],
                                                   :fieldType "dropdown"}
                                                  {:text {:fi ""},
                                                   :label {:en "Click add if you want to add further qualifications.",
                                                           :fi "Paina lisää, jos haluat lisätä useampia tutkintoja.",
                                                           :sv "Tryck på lägg till om du vill lägga till flera examina."},
                                                   :fieldType "p",
                                                   :fieldClass "infoElement"}],
                                       :fieldType "fieldset",
                                       :fieldClass "questionGroup"}]}
                         {:label {:en "Bachelor’s/Master’s/Doctoral degree completed in Finland",
                                  :fi "Suomessa suoritettu korkeakoulututkinto",
                                  :sv "Högskoleexamen som avlagts i Finland"},
                          :value "pohjakoulutus_kk",
                          :followups [{:label {:fi "", :sv ""},
                                       :children [{:label {:en "Year of completion", :fi "Suoritusvuosi", :sv "Avlagd år"},
                                                   :params {:size "S",
                                                            :numeric true,
                                                            :max-value "2022",
                                                            :min-value "1900"},
                                                   :options [{:label {:fi "", :sv ""},
                                                              :value "0",
                                                              :condition {:answer-compared-to 2022,
                                                                          :comparison-operator "="},
                                                              :followups [{:label {:en "Have you graduated",
                                                                                   :fi "Oletko valmistunut?",
                                                                                   :sv "Har du tagit examen"},
                                                                           :options [{:label {:en "Yes",
                                                                                              :fi "Kyllä",
                                                                                              :sv "Ja"},
                                                                                      :value "0",
                                                                                      :followups [(seven-day-attachment-followup {:en "Transcript of records of higher education degree completed in Finland",
                                                                                                                                  :fi "Opintosuoritusote Suomessa suoritettuun korkeakoulututkintoon sisältyvistä opinnoista",
                                                                                                                                  :sv "Studieprestationsutdrag om högskoleexamen som avlagts i Finland"})
                                                                                                  (seven-day-attachment-followup {:en "Higher education degree certificate",
                                                                                                                                  :fi "Suomessa suoritetun korkeakoulututkinnon tutkintotodistus",
                                                                                                                                  :sv "Högskoleexamensbetyg"})
                                                                                                  {:label {:en "Share a link to your study records from My Studyinfo",
                                                                                                           :fi "Jaa linkki opintosuoritustietoihisi Oma Opintopolku -palvelussa",
                                                                                                           :sv "Dela dina prestationsuppgifter direkt från Min Studieinfo"},
                                                                                                   :params {:size "L",
                                                                                                            :hidden false,
                                                                                                            :info-text {:label {:en "This question applies only study programmes listed above, under \"Show study programmes\".

                                                                                                                                   You can share the information about your completed studies using a link via My Studyinfo service. In this case you do not need to submit transcript of records and degree certificate separately as an attachment to your application.

                                                                                                                                   To create a link to your completed study records:

                                                                                                                                   1. Log in to [My Studyinfo service](https://studyinfo.fi/oma-opintopolku/) (requires Finnish e-identification and respective means of identification)
                                                                                                                                   2. Choose \"Proceed to studies\".
                                                                                                                                   3. Choose \"Jaa suoritustietoja\" (share study records).
                                                                                                                                   4. Choose the study records you wish to share.
                                                                                                                                   5. Choose \"Jaa valitsemasi opinnot\" (share studyrecords you have chosen).
                                                                                                                                   6. Choose \"Kopioi linkki\" (copy link).
                                                                                                                                   7. Paste the copied link to the text field below.
                                                                                                                                   ",
                                                                                                                                :fi "Tämä kysymys koskee vain yllä mainittuja hakukohteita, jotka näet painamalla \"näytä hakukohteet\".

                                                                                                                                   Halutessasi voit jakaa opintosuoritustietosi sekä läsnä- ja poissaolokausitietosi Oma Opintopolku -palvelusta saatavan linkin avulla. Tällöin sinun ei tarvitse toimittaa erillistä opintosuoritusotetta ja tutkintotodistusta hakemuksesi liitteeksi.

                                                                                                                                   Näin luot linkin omiin suoritustietoihisi:

                                                                                                                                   1. Kirjaudu sisään [Oma Opintopolku -palveluun](https://opintopolku.fi/oma-opintopolku/).
                                                                                                                                   2. Valitse ”Siirry opintosuorituksiin”.
                                                                                                                                   3. Valitse näytöltä ”Jaa suoritustietoja”.
                                                                                                                                   4. Valitse suoritustiedot, jotka haluat jakaa. Valitse ainakin koulutus, jonka perusteella haet.
                                                                                                                                   5. Valitse ”Jaa valitsemasi opinnot”.
                                                                                                                                   6. Valitse ”Kopioi linkki”.
                                                                                                                                   7. Liitä linkki alla olevaan tekstikenttään.",
                                                                                                                                :sv "Denna fråga gäller de ovannämnda ansökningsmålen som du får fram genom att klicka på ”visa ansökningsmål”.

                                                                                                                                   Du kan meddela uppgifterna om dina studieprestationer och dina närvaro- och frånvaroperioder med hjälp av en länk. Då behöver du inte lämna in ett separat studieutdrag och betyg som bilaga till din ansökan.

                                                                                                                                   Så här skapar du en länk till dina prestationsuppgifter:

                                                                                                                                   1. Logga in i [tjänsten Min Studieinfo](https://studieinfo.fi/oma-opintopolku/).
                                                                                                                                   2. Välj ”Fortsätt till studierna”.
                                                                                                                                   3. Välj ”Dela dina prestationsuppgifter”.
                                                                                                                                   4. Välj de prestationsuppgifter du vill dela.
                                                                                                                                   5. Välj ”Dela valda studier”.
                                                                                                                                   6. Välj ”Kopiera länk”.
                                                                                                                                   7. Klistra in länken i fältet nedan på ansökningsblanketten."}}},
                                                                                                   :fieldType "textField",
                                                                                                   :fieldClass "formField"}]}
                                                                                     {:label {:en "No",
                                                                                              :fi "En",
                                                                                              :sv "Nej"},
                                                                                      :value "1",
                                                                                      :followups [{:label {:en "Estimated graduation date (dd.mm.yyyy)",
                                                                                                           :fi "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)",
                                                                                                           :sv "Beräknat examensdatum (dd.mm.åååå)"},
                                                                                                   :params {:size "S",
                                                                                                            :numeric false,
                                                                                                            :decimals nil},
                                                                                                   :fieldType "textField",
                                                                                                   :fieldClass "formField",
                                                                                                   :validators ["required"]}
                                                                                                  (seven-day-attachment-followup {:en "Transcript of records of higher education degree completed in Finland",
                                                                                                                                  :fi "Opintosuoritusote Suomessa suoritettavaan korkeakoulututkintoon sisältyvistä opinnoista",
                                                                                                                                  :sv "Studieprestationsutdrag om högskoleexamen som avlagts i Finland"})
                                                                                                  {:params {:hidden false,
                                                                                                            :deadline nil,
                                                                                                            :info-text {:value {:en "The exact deadline is available next to the attachment request.

                                                                                                                                   Name the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.

                                                                                                                                   Scan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright.

                                                                                                                                   Recommended file formats are: PDF, JPG, PNG and DOCX.",
                                                                                                                                :fi "Määräaika ilmoitetaan liitepyynnön vieressä.

                                                                                                                                   Nimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus

                                                                                                                                   Skannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.

                                                                                                                                   Suositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                                                                                                                                :sv "Den angivna tidpunkten syns invid begäran om bilagor.

                                                                                                                                   Namnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg

                                                                                                                                   Skanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.
                                                                                                                                   Samla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.
                                                                                                                                   Kontrollera att dokumenten i filen är rättvända.

                                                                                                                                   Rekommenderade filformat är PDF, JPG, PNG och DOCX. "},
                                                                                                                        :enabled? true}},
                                                                                                   :fieldClass "formField",
                                                                                                   :label {:en "Higher education degree certificate",
                                                                                                           :fi "Suomessa suoritettavan korkeakoulututkinnon tutkintotodistus",
                                                                                                           :sv "Högskoleexamensbetyg"},
                                                                                                   :options [],
                                                                                                   :fieldType "attachment"}
                                                                                                  {:label {:en "Share a link to your study records from My Studyinfo",
                                                                                                           :fi "Jaa linkki opintosuoritustietoihisi Oma Opintopolku -palvelussa",
                                                                                                           :sv "Dela dina prestationsuppgifter direkt från Min Studieinfo"},
                                                                                                   :params {:size "L",
                                                                                                            :hidden false,
                                                                                                            :info-text {:label {:en "This question applies only study programmes listed above, under \"Show study programmes\".

                                                                                                                                   You can share the information about your completed studies using a link via My Studyinfo service. In this case you do not need to submit transcript of records and degree certificate separately as an attachment to your application.

                                                                                                                                   To create a link to your completed study records:

                                                                                                                                   1. Log in to [My Studyinfo service](https://studyinfo.fi/oma-opintopolku/) (requires Finnish e-identification and respective means of identification)
                                                                                                                                   2. Choose \"Proceed to studies\".
                                                                                                                                   3. Choose \"Jaa suoritustietoja\" (share study records).
                                                                                                                                   4. Choose the study records you wish to share.
                                                                                                                                   5. Choose \"Jaa valitsemasi opinnot\" (share studyrecords you have chosen).
                                                                                                                                   6. Choose \"Kopioi linkki\" (copy link).
                                                                                                                                   7. Paste the copied link to the text field below.
                                                                                                                                   ",
                                                                                                                                :fi "Tämä kysymys koskee vain yllä mainittuja hakukohteita, jotka näet painamalla \"näytä hakukohteet\".

                                                                                                                                   Halutessasi voit jakaa opintosuoritustietosi sekä läsnä- ja poissaolokausitietosi Oma Opintopolku -palvelusta saatavan linkin avulla. Tällöin sinun ei tarvitse toimittaa erillistä opintosuoritusotetta ja tutkintotodistusta hakemuksesi liitteeksi.

                                                                                                                                   Näin luot linkin omiin suoritustietoihisi:

                                                                                                                                   1. Kirjaudu sisään [Oma Opintopolku -palveluun](https://opintopolku.fi/oma-opintopolku/).
                                                                                                                                   2. Valitse ”Siirry opintosuorituksiin”.
                                                                                                                                   3. Valitse näytöltä ”Jaa suoritustietoja”.
                                                                                                                                   4. Valitse suoritustiedot, jotka haluat jakaa. Valitse ainakin koulutus, jonka perusteella haet.
                                                                                                                                   5. Valitse ”Jaa valitsemasi opinnot”.
                                                                                                                                   6. Valitse ”Kopioi linkki”.
                                                                                                                                   7. Liitä linkki alla olevaan tekstikenttään.",
                                                                                                                                :sv "Denna fråga gäller de ovannämnda ansökningsmålen som du får fram genom att klicka på ”visa ansökningsmål”.

                                                                                                                                   Du kan meddela uppgifterna om dina studieprestationer och dina närvaro- och frånvaroperioder med hjälp av en länk. Då behöver du inte lämna in ett separat studieutdrag och betyg som bilaga till din ansökan.

                                                                                                                                   Så här skapar du en länk till dina prestationsuppgifter:

                                                                                                                                   1. Logga in i [tjänsten Min Studieinfo](https://studieinfo.fi/oma-opintopolku/).
                                                                                                                                   2. Välj ”Fortsätt till studierna”.
                                                                                                                                   3. Välj ”Dela dina prestationsuppgifter”.
                                                                                                                                   4. Välj de prestationsuppgifter du vill dela.
                                                                                                                                   5. Välj ”Dela valda studier”.
                                                                                                                                   6. Välj ”Kopiera länk”.
                                                                                                                                   7. Klistra in länken i fältet nedan på ansökningsblanketten."}}},
                                                                                                   :fieldType "textField",
                                                                                                   :fieldClass "formField"}]}],
                                                                           :fieldType "singleChoice",
                                                                           :fieldClass "formField",
                                                                           :validators ["required"]}]}
                                                             {:label {:fi "", :sv ""},
                                                              :value "1",
                                                              :condition {:answer-compared-to 2022,
                                                                          :comparison-operator "<"},
                                                              :followups [(seven-day-attachment-followup {:en "Transcript of records of higher education degree completed in Finland",
                                                                                                          :fi "Opintosuoritusote Suomessa suoritettuun korkeakoulututkintoon sisältyvistä opinnoista",
                                                                                                          :sv "Studieprestationsutdrag om högskoleexamen som avlagts i Finland"})
                                                                          (seven-day-attachment-followup {:en "Higher education degree certificate",
                                                                                                          :fi "Suomessa suoritetun korkeakoulututkinnon tutkintotodistus",
                                                                                                          :sv "Högskoleexamensbetyg"})
                                                                          {:label {:en "Share a link to your study records from My Studyinfo",
                                                                                   :fi "Jaa linkki opintosuoritustietoihisi Oma Opintopolku -palvelussa",
                                                                                   :sv "Dela dina prestationsuppgifter direkt från Min Studieinfo"},
                                                                           :params {:size "L",
                                                                                    :hidden false,
                                                                                    :info-text {:label {:en "This question applies only study programmes listed above, under \"Show study programmes\".

                                                                                                           You can share the information about your completed studies using a link via My Studyinfo service. In this case you do not need to submit transcript of records and degree certificate separately as an attachment to your application.

                                                                                                           To create a link to your completed study records:

                                                                                                           1. Log in to [My Studyinfo service](https://studyinfo.fi/oma-opintopolku/) (requires Finnish e-identification and respective means of identification)
                                                                                                           2. Choose \"Proceed to studies\".
                                                                                                           3. Choose \"Jaa suoritustietoja\" (share study records).
                                                                                                           4. Choose the study records you wish to share.
                                                                                                           5. Choose \"Jaa valitsemasi opinnot\" (share studyrecords you have chosen).
                                                                                                           6. Choose \"Kopioi linkki\" (copy link).
                                                                                                           7. Paste the copied link to the text field below.
                                                                                                           ",
                                                                                                        :fi "Tämä kysymys koskee vain yllä mainittuja hakukohteita, jotka näet painamalla \"näytä hakukohteet\".

                                                                                                           Halutessasi voit jakaa opintosuoritustietosi sekä läsnä- ja poissaolokausitietosi Oma Opintopolku -palvelusta saatavan linkin avulla. Tällöin sinun ei tarvitse toimittaa erillistä opintosuoritusotetta ja tutkintotodistusta hakemuksesi liitteeksi.

                                                                                                           Näin luot linkin omiin suoritustietoihisi:

                                                                                                           1. Kirjaudu sisään [Oma Opintopolku -palveluun](https://opintopolku.fi/oma-opintopolku/).
                                                                                                           2. Valitse ”Siirry opintosuorituksiin”.
                                                                                                           3. Valitse näytöltä ”Jaa suoritustietoja”.
                                                                                                           4. Valitse suoritustiedot, jotka haluat jakaa. Valitse ainakin koulutus, jonka perusteella haet.
                                                                                                           5. Valitse ”Jaa valitsemasi opinnot”.
                                                                                                           6. Valitse ”Kopioi linkki”.
                                                                                                           7. Liitä linkki alla olevaan tekstikenttään.",
                                                                                                        :sv "Denna fråga gäller de ovannämnda ansökningsmålen som du får fram genom att klicka på ”visa ansökningsmål”.

                                                                                                           Du kan meddela uppgifterna om dina studieprestationer och dina närvaro- och frånvaroperioder med hjälp av en länk. Då behöver du inte lämna in ett separat studieutdrag och betyg som bilaga till din ansökan.

                                                                                                           Så här skapar du en länk till dina prestationsuppgifter:

                                                                                                           1. Logga in i [tjänsten Min Studieinfo](https://studieinfo.fi/oma-opintopolku/).
                                                                                                           2. Välj ”Fortsätt till studierna”.
                                                                                                           3. Välj ”Dela dina prestationsuppgifter”.
                                                                                                           4. Välj de prestationsuppgifter du vill dela.
                                                                                                           5. Välj ”Dela valda studier”.
                                                                                                           6. Välj ”Kopiera länk”.
                                                                                                           7. Klistra in länken i fältet nedan på ansökningsblanketten."}}},
                                                                           :fieldType "textField",
                                                                           :fieldClass "formField"}]}],
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["numeric" "required"]}
                                                  {:koodisto-source {:uri "kktutkinnot",
                                                                     :title "Kk-tutkinnot",
                                                                     :version 1,
                                                                     :allow-invalid? false},
                                                   :koodisto-ordered-by-user true,
                                                   :validators ["required"],
                                                   :fieldClass "formField",
                                                   :label {:en "Degree level", :fi "Tutkintotaso", :sv "Examensnivå"},
                                                   :options [],
                                                   :fieldType "dropdown"}
                                                  {:koodisto-source {:uri "tutkinto",
                                                                     :title "Tutkinto",
                                                                     :version 2,
                                                                     :allow-invalid? false},
                                                   :validators ["required"],
                                                   :fieldClass "formField",
                                                   :label {:en "Degree", :fi "Tutkinto", :sv "Examen"},
                                                   :options [],
                                                   :fieldType "dropdown"}
                                                  {:label {:en "Higher education institution",
                                                           :fi "Korkeakoulu",
                                                           :sv "Högskola"},
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["required"]}
                                                  {:text {:fi ""},
                                                   :label {:en "Click add if you want to add further qualifications.",
                                                           :fi "Paina lisää, jos haluat lisätä useampia tutkintoja.",
                                                           :sv "Tryck på lägg till om du vill lägga till flera examina."},
                                                   :fieldType "p",
                                                   :fieldClass "infoElement"}],
                                       :fieldType "fieldset",
                                       :fieldClass "questionGroup"}]}
                         {:label {:en "Upper secondary double degree completed in Finland (kaksoistutkinto)",
                                  :fi "Suomessa suoritettu kaksoistutkinto (ammatillinen perustutkinto ja ylioppilastutkinto)",
                                  :sv "Dubbelexamen som avlagts i Finland"},
                          :value "pohjakoulutus_yo_ammatillinen",
                          :followups [{:label {:fi "", :sv ""},
                                       :children [{:text {:en "Your matriculation examination details are received automatically from the national registry for study rights and completed studies. You can check your study rights and completed studies in Finland from [My Studyinfo's](https://studyinfo.fi/oma-opintopolku/) section: My completed studies (Only available in Finnish/Swedish). If your information is incorrect or information is missing, please contact the Matriculation Examination Board to correct any errors.",
                                                          :fi "Saamme ylioppilastutkinnon suoritustietosi ylioppilastutkintorekisteristä. Voit tarkistaa opintosuorituksesi [Oma Opintopolku -palvelun](https://opintopolku.fi/oma-opintopolku/) Omat opintosuoritukseni -osiosta. Jos tiedossasi on puutteita, ole yhteydessä ylioppilastutkintolautakuntaan tietojen korjaamiseksi.
                                                             ",
                                                          :sv "Vi får uppgifterna om din studentexamen ur studentexamensregistret. Du kan kontrollera dina studieprestationer i [Min Studieinfos](https://studieinfo.fi/oma-opintopolku/) del Mina studier. Om dina uppgifter är felaktiga, ska du kontakta Studentexamensnämnden som kan korrigera felen."},
                                                   :label {:fi ""},
                                                   :fieldType "p",
                                                   :fieldClass "infoElement"}
                                                  {:label {:en "Year of completion",
                                                           :fi "Suoritusvuosi ",
                                                           :sv "Avlagd år"},
                                                   :params {:size "S",
                                                            :hidden false,
                                                            :numeric true,
                                                            :max-value "2022",
                                                            :min-value "1900"},
                                                   :options [{:label {:fi "", :sv ""},
                                                              :value "0",
                                                              :condition {:answer-compared-to 2022,
                                                                          :comparison-operator "="},
                                                              :followups [{:label {:en "Have you graduated",
                                                                                   :fi "Oletko valmistunut?",
                                                                                   :sv "Har du tagit examen"},
                                                                           :options [{:label {:en "Yes",
                                                                                              :fi "Kyllä",
                                                                                              :sv "Ja"},
                                                                                      :value "0",
                                                                                      :followups []}
                                                                                     {:label {:en "No",
                                                                                              :fi "En",
                                                                                              :sv "Nej"},
                                                                                      :value "1",
                                                                                      :followups [{:label {:en "Estimated graduation date (dd.mm.yyyy)",
                                                                                                           :fi "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)",
                                                                                                           :sv "Beräknat examensdatum (dd.mm.åååå)"},
                                                                                                   :params {:size "S",
                                                                                                            :numeric false,
                                                                                                            :decimals nil},
                                                                                                   :fieldType "textField",
                                                                                                   :fieldClass "formField",
                                                                                                   :validators ["required"]}
                                                                                                  (seven-day-attachment-followup {:en "Preliminary certificate from the educational institution",
                                                                                                                                  :fi "Ennakkoarvio ammatillisesta perustutkinnosta",
                                                                                                                                  :sv "Läroanstaltens preliminär intyg om yrkesinriktad grundexamen"})]}],
                                                                           :fieldType "singleChoice",
                                                                           :fieldClass "formField",
                                                                           :validators ["required"]}]}
                                                             {:label {:fi "", :sv ""},
                                                              :value "2",
                                                              :condition {:answer-compared-to 2017,
                                                                          :comparison-operator "="},
                                                              :followups [{:label {:en "Have you completed your qualification as a competence based qualification in its entirety?",
                                                                                   :fi "Oletko suorittanut ammatillisen perustutkinnon näyttötutkintona?",
                                                                                   :sv "Har du avlagt examen som fristående yrkesexamen?"},
                                                                           :params {:info-text {:label nil}},
                                                                           :options [{:label {:en "Yes",
                                                                                              :fi "Kyllä",
                                                                                              :sv "Ja"},
                                                                                      :value "0",
                                                                                      :followups [(seven-day-attachment-followup {:en "Vocational qualification diploma",
                                                                                                                                  :fi "Ammatillisen perustutkinnon tutkintotodistus",
                                                                                                                                  :sv "Yrkesinriktad grundexamens betyg"})]}
                                                                                     {:label {:en "No",
                                                                                              :fi "En",
                                                                                              :sv "Nej"},
                                                                                      :value "1",
                                                                                      :followups [{:text {:en "Your final vocational qualification details are received automatically from the national registry for study rights and completed studies. You can check your study rights and completed studies in Finland from [My Studyinfo's](https://studyinfo.fi/oma-opintopolku/) section: My completed studies (Only available in Finnish/Swedish). If your information is incorrect or information is missing, please contact the educational institution to correct any errors.
                                                                                                             ",
                                                                                                          :fi "Saamme lopulliset ammatillisen perustutkinnon suoritustietosi valtakunnallisesta [Koski-tietovarannosta](https://www.oph.fi/fi/palvelut/koski-tietovaranto). Voit tarkistaa opintosuorituksesi [Oma Opintopolku -palvelun](https://opintopolku.fi/oma-opintopolku/) Omat opintosuoritukseni -osiosta. Jos tiedoissasi on puutteita, ole yhteydessä oppilaitokseesi tietojen korjaamiseksi.
                                                                                                             ",
                                                                                                          :sv "Vi får slutliga uppgifterna om din yrkesinriktade grundexamen ur den nationella [informationsresursen Koski](https://www.oph.fi/sv/tjanster/informationsresurser-koski). Du kan kolla dina studieprestationer i [Min Studieinfos](https://studieinfo.fi/oma-opintopolku/) del Mina studier. Om dina uppgifter är felaktiga, ska du kontakta din läroanstalt som kan korrigera felen.
                                                                                                             "},
                                                                                                   :label {:fi ""},
                                                                                                   :fieldType "p",
                                                                                                   :fieldClass "infoElement"}]}],
                                                                           :fieldType "singleChoice",
                                                                           :fieldClass "formField"}]}
                                                             {:label {:fi "", :sv ""},
                                                              :value "3",
                                                              :condition {:answer-compared-to 2017,
                                                                          :comparison-operator ">"},
                                                              :followups [{:text {:en "Your final vocational qualification details are received automatically from the national registry for study rights and completed studies. You can check your study rights and completed studies in Finland from [My Studyinfo's](https://studyinfo.fi/oma-opintopolku/) section: My completed studies (Only available in Finnish/Swedish). If your information is incorrect or information is missing, please contact the educational institution to correct any errors.
                                                                                     ",
                                                                                  :fi "Saamme lopulliset ammatillisen perustutkinnon suoritustietosi valtakunnallisesta [Koski-tietovarannosta](https://www.oph.fi/fi/palvelut/koski-tietovaranto). Voit tarkistaa opintosuorituksesi [Oma Opintopolku -palvelun](https://opintopolku.fi/oma-opintopolku/) Omat opintosuoritukseni -osiosta. Jos tiedoissasi on puutteita, ole yhteydessä oppilaitokseesi tietojen korjaamiseksi.
                                                                                     ",
                                                                                  :sv "Vi får slutliga uppgifterna om din yrkesinriktade grundexamen ur den nationella [informationsresursen Koski](https://www.oph.fi/sv/tjanster/informationsresurser-koski). Du kan kolla dina studieprestationer i [Min Studieinfos](https://studieinfo.fi/oma-opintopolku/) del Mina studier. Om dina uppgifter är felaktiga, ska du kontakta din läroanstalt som kan korrigera felen.
                                                                                     "},
                                                                           :label {:fi ""},
                                                                           :fieldType "p",
                                                                           :fieldClass "infoElement"}]}],
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["numeric" "required"]}
                                                  {:label {:en "Vocational qualification",
                                                           :fi "Ammatillinen tutkinto",
                                                           :sv "Yrkesinriktad examen"},
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["required"]}
                                                  {:label {:en "Scope of vocational qualification",
                                                           :fi "Ammatillisen perustutkinnon laajuus",
                                                           :sv "Omfattning av yrkesinriktad grundexamen"},
                                                   :params {:size "S", :numeric true, :decimals 1},
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["numeric" "required"]}
                                                  {:label {:en "The scope unit",
                                                           :fi "Laajuuden yksikkö",
                                                           :sv "Omfattningens enhet"},
                                                   :options [{:label {:en "Courses", :fi "Kurssia", :sv "Kurser"},
                                                              :value "0"}
                                                             {:label {:en "ECTS credits",
                                                                      :fi "Opintopistettä",
                                                                      :sv "Studiepoäng"},
                                                              :value "1"}
                                                             {:label {:en "Study weeks",
                                                                      :fi "Opintoviikkoa",
                                                                      :sv "Studieveckor"},
                                                              :value "2"}
                                                             {:label {:en "Competence points",
                                                                      :fi "Osaamispistettä",
                                                                      :sv "Kompetenspoäng"},
                                                              :value "3"}
                                                             {:label {:en "Hours", :fi "Tuntia", :sv "Timmar"},
                                                              :value "4"}
                                                             {:label {:en "Weekly lessons per year",
                                                                      :fi "Vuosiviikkotuntia",
                                                                      :sv "Årsveckotimmar"},
                                                              :value "5"}
                                                             {:label {:en "Years", :fi "Vuotta", :sv "År"}, :value "6"}],
                                                   :fieldType "dropdown",
                                                   :fieldClass "formField",
                                                   :validators ["required"]}
                                                  {:params {:info-text {:label {:en "Select \"Unknown\", if the educational institution where you have completed your degree cannot be found on the list.",
                                                                                :fi "Valitse \"Tuntematon\", jos et löydä listasta oppilaitosta, jossa olet suorittanut tutkintosi.",
                                                                                :sv "Välj \"Okänd\" om du inte hittar den läroanstalt, där du har avlagt din examen."}}},
                                                   :koodisto-source {:uri "oppilaitostyyppi",
                                                                     :title "Ammatilliset oppilaitokset",
                                                                     :version 1,
                                                                     :allow-invalid? true},
                                                   :validators ["required"],
                                                   :fieldClass "formField",
                                                   :label {:en "Vocational institution",
                                                           :fi "Ammatillinen oppilaitos ",
                                                           :sv "Yrkesinriktad läroanstalt "},
                                                   :options [],
                                                   :fieldType "dropdown"}
                                                  {:text {:fi ""},
                                                   :label {:en "Click add if you want to add further qualifications.",
                                                           :fi "Paina lisää, jos haluat lisätä useampia tutkintoja.",
                                                           :sv "Tryck på lägg till om du vill lägga till flera examina."},
                                                   :fieldType "p",
                                                   :fieldClass "infoElement"}],
                                       :fieldType "fieldset",
                                       :fieldClass "questionGroup"}]}
                         {:label {:en "General upper secondary school syllabus completed in Finland (lukion oppimäärä ilman ylioppilastutkintoa)",
                                  :fi "Suomessa suoritettu lukion oppimäärä ilman ylioppilastutkintoa",
                                  :sv "Gymnasiets lärokurs som avlagts i Finland utan studentexamen"},
                          :value "pohjakoulutus_lk",
                          :followups [{:label {:en "Year of completion", :fi "Suoritusvuosi", :sv "Avlagd år"},
                                       :params {:size "S", :numeric true, :max-value "2022", :min-value "1900"},
                                       :options [{:label {:fi "", :sv ""},
                                                  :value "0",
                                                  :condition {:answer-compared-to 2022, :comparison-operator "="},
                                                  :followups [{:label {:en "Have you graduated?",
                                                                       :fi "Oletko valmistunut",
                                                                       :sv "Har du tagit examen?"},
                                                               :options [{:label {:en "Yes", :fi "Kyllä", :sv "Ja"},
                                                                          :value "0",
                                                                          :followups [(seven-day-attachment-followup {:en "General upper secondary education certificate",
                                                                                                                      :fi "Lukion päättötodistus",
                                                                                                                      :sv "Gymnasiets avgångsbetyg"})]}
                                                                         {:label {:en "No", :fi "En", :sv "Nej"},
                                                                          :value "1",
                                                                          :followups [{:label {:en "Estimated graduation date (dd.mm.yyyy)",
                                                                                               :fi "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)",
                                                                                               :sv "Beräknat examensdatum (dd.mm.åååå)"},
                                                                                       :params {:size "S",
                                                                                                :numeric false,
                                                                                                :decimals nil},
                                                                                       :fieldType "textField",
                                                                                       :fieldClass "formField",
                                                                                       :validators ["required"]}
                                                                                      (seven-day-attachment-followup {:en "Latest transcript of study records from Finnish upper secondary school",
                                                                                                                      :fi "Ennakkoarvio tai viimeisin todistus suoritetuista opinnoista lukiossa",
                                                                                                                      :sv "Förhandsexamensbetyg eller betyg över slutförda studier om gymnasiestudier"})]}],
                                                               :fieldType "singleChoice",
                                                               :fieldClass "formField",
                                                               :validators ["required"]}]}
                                                 {:label {:fi "", :sv ""},
                                                  :value "1",
                                                  :condition {:answer-compared-to 2022, :comparison-operator "<"},
                                                  :followups [(seven-day-attachment-followup {:en "General upper secondary education certificate",
                                                                                              :fi "Lukion päättötodistus",
                                                                                              :sv "Gymnasiets avgångsbetyg"},)]}],
                                       :fieldType "textField",
                                       :fieldClass "formField",
                                       :validators ["numeric" "required"]}]}
                         {:label {:en "International matriculation examination completed in Finland (IB, EB and RP/DIA)",
                                  :fi "Suomessa suoritettu kansainvälinen ylioppilastutkinto (IB, EB ja RP/DIA)",
                                  :sv "Internationell studentexamen som avlagts i Finland (IB, EB och RP/DIA)"},
                          :value "pohjakoulutus_yo_kansainvalinen_suomessa",
                          :followups [{:label {:fi "", :sv ""},
                                       :children [{:label {:en "Year of completion", :fi "Suoritusvuosi", :sv "Avlagd år"},
                                                   :params {:size "S",
                                                            :numeric true,
                                                            :max-value "2022",
                                                            :min-value "1900"},
                                                   :options [{:label {:fi "", :sv ""},
                                                              :value "0",
                                                              :condition {:answer-compared-to 2022,
                                                                          :comparison-operator "<"},
                                                              :followups [{:label {:en "Matriculation examination",
                                                                                   :fi "Ylioppilastutkinto",
                                                                                   :sv "Studentexamen"},
                                                                           :options [{:label {:en "International Baccalaureate -diploma",
                                                                                              :fi "International Baccalaureate -tutkinto",
                                                                                              :sv "International Baccalaureate -examen"},
                                                                                      :value "0",
                                                                                      :followups [(seven-day-attachment-followup {:en "IB Diploma completed in Finland",
                                                                                                                                  :fi "IB Diploma -tutkintotodistus Suomessa suoritetusta tutkinnosta",
                                                                                                                                  :sv "IB Diploma från IB-studentexamen som avlagts i Finland"})]}
                                                                                     {:label {:en "European Baccalaureate -diploma",
                                                                                              :fi "Eurooppalainen ylioppilastutkinto",
                                                                                              :sv "European Baccalaureate -examen"},
                                                                                      :value "1",
                                                                                      :followups [(seven-day-attachment-followup {:en "IB Diploma completed in Finland",
                                                                                                                                  :fi "IB Diploma -tutkintotodistus Suomessa suoritetusta tutkinnosta",
                                                                                                                                  :sv "IB Diploma från IB-studentexamen som avlagts i Finland"})]}
                                                                                     {:label {:en "Deutsche Internationale Abiturprüfung/Reifeprüfung -diploma",
                                                                                              :fi "Deutsche Internationale Abiturprüfung/Reifeprüfung -tutkinto",
                                                                                              :sv "Deutsche Internationale Abiturprüfung/Reifeprüfung -examen"},
                                                                                      :value "2",
                                                                                      :followups [(seven-day-attachment-followup {:en "Reifeprüfung/DIA diploma completed in Finland",
                                                                                                                                  :fi "Reifeprüfung/DIA-tutkintotodistus Suomessa suoritetusta tutkinnosta",
                                                                                                                                  :sv "Reifeprüfung/DIA -examensbetyg from RP/DIA-studentexamen som avlagts i Finland"})
                                                                                                  (seven-day-attachment-followup {:en "An equivalency certificate on upper secondary education based on Reifeprüfung or DIA provisions",
                                                                                                                                  :fi "Vastaavuustodistus lukio-opinnoista, jotka perustuvat RP- tai DIA-tutkinnon säännöksiin",
                                                                                                                                  :sv "Motsvarighetsintyget av gymnasiestudier, som är baserad på RP- eller DIA-bestämmelser"})]}],
                                                                           :fieldType "singleChoice",
                                                                           :fieldClass "formField",
                                                                           :validators ["required"]}]}
                                                             {:label {:fi "", :sv ""},
                                                              :value "1",
                                                              :condition {:answer-compared-to 2022,
                                                                          :comparison-operator "="},
                                                              :followups [{:label {:en "Matriculation examination",
                                                                                   :fi "Ylioppilastutkinto",
                                                                                   :sv "Studentexamen"},
                                                                           :options [{:label {:en "International Baccalaureate -diploma",
                                                                                              :fi "International Baccalaureate -tutkinto",
                                                                                              :sv "International Baccalaureate -examen"},
                                                                                      :value "0",
                                                                                      :followups [{:label {:en "Have you graduated?",
                                                                                                           :fi "Oletko valmistunut?",
                                                                                                           :sv "Har du tagit examen?"},
                                                                                                   :options [{:label {:en "Yes",
                                                                                                                      :fi "Kyllä",
                                                                                                                      :sv "Ja"},
                                                                                                              :value "0",
                                                                                                              :followups [(seven-day-attachment-followup {:en "IB Diploma completed in Finland",
                                                                                                                                                          :fi "IB Diploma -tutkintotodistus Suomessa suoritetusta tutkinnosta",
                                                                                                                                                          :sv "IB Diploma från IB-studentexamen som avlagts i Finland"})]}
                                                                                                             {:label {:en "No",
                                                                                                                      :fi "En",
                                                                                                                      :sv "Nej"},
                                                                                                              :value "1",
                                                                                                              :followups [{:label {:en "Estimated graduation date (dd.mm.yyyy)",
                                                                                                                                   :fi "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)",
                                                                                                                                   :sv "Beräknat examensdatum (dd.mm.åååå)"},
                                                                                                                           :params {:size "S",
                                                                                                                                    :numeric false,
                                                                                                                                    :decimals nil},
                                                                                                                           :fieldType "textField",
                                                                                                                           :fieldClass "formField",
                                                                                                                           :validators ["required"]}
                                                                                                                          (seven-day-attachment-followup {:en "Predicted grades from IB completed in Finland",
                                                                                                                                                          :fi "Oppilaitoksen ennakkoarvio Suomessa suoritettavan tutkinnon arvosanoista (Candidate Predicted Grades)",
                                                                                                                                                          :sv "Predicted grades från IB-studentexamen som avlagts i Finland "})
                                                                                                                          {:params {:hidden false,
                                                                                                                                    :deadline nil,
                                                                                                                                    :info-text {:value {:en "The attachment has to be submitted  by the deadline informed next to the attachment request.

                                                                                                                                                           Name the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.

                                                                                                                                                           Scan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright.

                                                                                                                                                           Recommended file formats are: PDF, JPG, PNG and DOCX.
                                                                                                                                                           ",
                                                                                                                                                        :fi "Määräaika ilmoitetaan liitepyynnön vieressä.

                                                                                                                                                           Nimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus

                                                                                                                                                           Skannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.

                                                                                                                                                           Suositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.
                                                                                                                                                           ",
                                                                                                                                                        :sv "Den angivna tidpunkten syns invid begäran om bilagor.

                                                                                                                                                           Namnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg

                                                                                                                                                           Skanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.
                                                                                                                                                           Samla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.
                                                                                                                                                           Kontrollera att dokumenten i filen är rättvända.

                                                                                                                                                           Rekommenderade filformat är PDF, JPG, PNG och DOCX."},
                                                                                                                                                :enabled? true}},
                                                                                                                           :fieldClass "formField",
                                                                                                                           :label {:en "Diploma Programme (DP) Results from IB completed in Finland",
                                                                                                                                   :fi "Diploma Programme (DP) Results -asiakirja Suomessa suoritettavasta tutkinnosta",
                                                                                                                                   :sv "Diploma Programme (DP) Results från IB-studentexamen som avlagts i Finland"},
                                                                                                                           :options [],
                                                                                                                           :fieldType "attachment"}]}],
                                                                                                   :fieldType "singleChoice",
                                                                                                   :fieldClass "formField",
                                                                                                   :validators ["required"]}]}
                                                                                     {:label {:en "European Baccalaureate -diploma",
                                                                                              :fi "Eurooppalainen ylioppilastutkinto",
                                                                                              :sv "European Baccalaureate -examen"},
                                                                                      :value "1",
                                                                                      :followups [{:label {:en "Have you graduated?",
                                                                                                           :fi "Oletko valmistunut?",
                                                                                                           :sv "Har du tagit examen?"},
                                                                                                   :options [{:label {:en "Yes",
                                                                                                                      :fi "Kyllä",
                                                                                                                      :sv "Ja"},
                                                                                                              :value "0",
                                                                                                              :followups [(seven-day-attachment-followup {:en "European Baccalaureate diploma completed in Finland",
                                                                                                                                                          :fi "European Baccalaureate Diploma -tutkintotodistus Suomessa suoritetusta tutkinnosta",
                                                                                                                                                          :sv "European Baccalaureate Diploma från EB-studentexamen som avlagts i Finland"})]}
                                                                                                             {:label {:en "No",
                                                                                                                      :fi "En",
                                                                                                                      :sv "Nej"},
                                                                                                              :value "1",
                                                                                                              :followups [{:label {:en "Estimated graduation date (dd.mm.yyyy)",
                                                                                                                                   :fi "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)",
                                                                                                                                   :sv "Beräknat examensdatum (dd.mm.åååå)"},
                                                                                                                           :params {:size "S",
                                                                                                                                    :numeric false,
                                                                                                                                    :decimals nil},
                                                                                                                           :fieldType "textField",
                                                                                                                           :fieldClass "formField",
                                                                                                                           :validators ["required"]}
                                                                                                                          (seven-day-attachment-followup {:en "Predicted grades from EB completed in Finland",
                                                                                                                                                          :fi "Oppilaitoksen ennakkoarvio Suomessa suoritettavan EB-tutkinnon arvosanoista",
                                                                                                                                                          :sv "Läroanstaltens preliminära vitsord från EB-studentexamen som avlagts i Finland"})
                                                                                                                          {:params {:hidden false,
                                                                                                                                    :deadline nil,
                                                                                                                                    :info-text {:value {:en "The attachment has to be submitted  by the deadline informed next to the attachment request.

                                                                                                                                                           Name the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.

                                                                                                                                                           Scan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright.

                                                                                                                                                           Recommended file formats are: PDF, JPG, PNG and DOCX.
                                                                                                                                                           ",
                                                                                                                                                        :fi "Määräaika ilmoitetaan liitepyynnön vieressä.

                                                                                                                                                           Nimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus

                                                                                                                                                           Skannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.

                                                                                                                                                           Suositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.
                                                                                                                                                           ",
                                                                                                                                                        :sv "Den angivna tidpunkten syns invid begäran om bilagor.

                                                                                                                                                           Namnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg

                                                                                                                                                           Skanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.
                                                                                                                                                           Samla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.
                                                                                                                                                           Kontrollera att dokumenten i filen är rättvända.

                                                                                                                                                           Rekommenderade filformat är PDF, JPG, PNG och DOCX."},
                                                                                                                                                :enabled? true}},
                                                                                                                           :fieldClass "formField",
                                                                                                                           :label {:en "European Baccalaureate diploma completed in Finland",
                                                                                                                                   :fi "European Baccalaureate Diploma -tutkintotodistus Suomessa suoritettavasta tutkinnosta",
                                                                                                                                   :sv "European Baccalaureate Diploma från EB-studentexamen som avlagts i Finland"},
                                                                                                                           :options [],
                                                                                                                           :fieldType "attachment"}]}],
                                                                                                   :fieldType "singleChoice",
                                                                                                   :fieldClass "formField",
                                                                                                   :validators ["required"]}]}
                                                                                     {:label {:en "Deutsche Internationale Abiturprüfung/Reifeprüfung -diploma",
                                                                                              :fi "Deutsche Internationale Abiturprüfung/Reifeprüfung -tutkinto",
                                                                                              :sv "Deutsche Internationale Abiturprüfung/Reifeprüfung -examen"},
                                                                                      :value "2",
                                                                                      :followups [{:label {:en "Have you graduated",
                                                                                                           :fi "Oletko valmistunut?",
                                                                                                           :sv "Har du tagit examen"},
                                                                                                   :options [{:label {:en "Yes",
                                                                                                                      :fi "Kyllä",
                                                                                                                      :sv "Ja"},
                                                                                                              :value "0",
                                                                                                              :followups [(seven-day-attachment-followup {:en "DIA diploma completed in Finland",
                                                                                                                                                          :fi "DIA-tutkintotodistus Suomessa suoritetusta tutkinnosta",
                                                                                                                                                          :sv "DIA -examensbetyg from DIA-studentexamen som avlagts i Finland"})
                                                                                                                          (seven-day-attachment-followup {:en "An equivalency certificate on upper secondary education based on DIA provisions",
                                                                                                                                                          :fi "Vastaavuustodistus lukio-opinnoista, jotka perustuvat DIA-tutkinnon säännöksiin",
                                                                                                                                                          :sv "Motsvarighetsintyget av gymnasiestudier, som är baserad på DIA-bestämmelser"})]}
                                                                                                             {:label {:en "No",
                                                                                                                      :fi "En",
                                                                                                                      :sv "Nej"},
                                                                                                              :value "1",
                                                                                                              :followups [{:label {:en "Estimated graduation date (dd.mm.yyyy)",
                                                                                                                                   :fi "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)",
                                                                                                                                   :sv "Beräknat examensdatum (dd.mm.åååå)"},
                                                                                                                           :params {:size "S",
                                                                                                                                    :numeric false,
                                                                                                                                    :decimals nil},
                                                                                                                           :fieldType "textField",
                                                                                                                           :fieldClass "formField",
                                                                                                                           :validators ["required"]}
                                                                                                                          (seven-day-attachment-followup {:en "Grade page of a Deutsches Internationales Abitur (DIA) diploma completed in Finland",
                                                                                                                                                          :fi "DIA-tutkintotodistuksen arvosanasivu Suomessa suoritettavasta tutkinnosta",
                                                                                                                                                          :sv "Examensbetygets vitsordssida av Deutsches Internationales Abitur (DIA) -examen avlagd i Finland"})
                                                                                                                          {:params {:hidden false,
                                                                                                                                    :deadline nil,
                                                                                                                                    :info-text {:value {:en "The attachment has to be submitted  by the deadline informed next to the attachment request.

                                                                                                                                                           Name the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.

                                                                                                                                                           Scan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright.

                                                                                                                                                           Recommended file formats are: PDF, JPG, PNG and DOCX.
                                                                                                                                                           ",
                                                                                                                                                        :fi "Määräaika ilmoitetaan liitepyynnön vieressä.

                                                                                                                                                           Nimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus

                                                                                                                                                           Skannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.

                                                                                                                                                           Suositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.
                                                                                                                                                           ",
                                                                                                                                                        :sv "Den angivna tidpunkten syns invid begäran om bilagor.

                                                                                                                                                           Namnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg

                                                                                                                                                           Skanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.
                                                                                                                                                           Samla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.
                                                                                                                                                           Kontrollera att dokumenten i filen är rättvända.

                                                                                                                                                           Rekommenderade filformat är PDF, JPG, PNG och DOCX."},
                                                                                                                                                :enabled? true}},
                                                                                                                           :fieldClass "formField",
                                                                                                                           :label {:en "DIA diploma completed in Finland",
                                                                                                                                   :fi "DIA-tutkintotodistus Suomessa suoritettavasta tutkinnosta",
                                                                                                                                   :sv "DIA -examensbetyg från DIA-studentexamen som avlagts i Finland"},
                                                                                                                           :options [],
                                                                                                                           :fieldType "attachment"}
                                                                                                                          (seven-day-attachment-followup {:en "An equivalency certificate on upper secondary education based on DIA provisions",
                                                                                                                                                          :fi "Vastaavuustodistus lukio-opinnoista, jotka perustuvat DIA-tutkinnon säännöksiin",
                                                                                                                                                          :sv "Motsvarighetsintyget av gymnasiestudier, som är baserad på DIA-bestämmelser"})]}],
                                                                                                   :fieldType "singleChoice",
                                                                                                   :fieldClass "formField",
                                                                                                   :validators ["required"]}]}],
                                                                           :fieldType "singleChoice",
                                                                           :fieldClass "formField",
                                                                           :validators ["required"]}]}],
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["numeric" "required"]}
                                                  {:label {:en "Educational institution",
                                                           :fi "Oppilaitos",
                                                           :sv "Läroanstalt"},
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["required"]}
                                                  {:text {:fi ""},
                                                   :label {:en "Click add if you want to add further qualifications.",
                                                           :fi "Paina lisää, jos haluat lisätä useampia tutkintoja.",
                                                           :sv "Tryck på lägg till om du vill lägga till flera examina."},
                                                   :fieldType "p",
                                                   :fieldClass "infoElement"}],
                                       :fieldType "fieldset",
                                       :fieldClass "questionGroup"}]}
                         {:label {:en "Vocational upper secondary qualification completed in Finland (kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto)",
                                  :fi "Suomessa suoritettu kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto",
                                  :sv "Yrkesinriktad examen på skolnivå, examen på institutsnivå eller yrkesinriktad examen på högre nivå som avlagts i Finland"},
                          :value "pohjakoulutus_amv",
                          :followups [{:text {:en "Please make sure that your degree is truly a Finnish school level (kouluaste), post-secondary level degree (opistoaste) or a higher vocational level degree (ammatillinen korkea-aste). As a rule, these degrees are no longer available in the 2000s. It is not possible to enter the year of completion later than 2005 on the form.",
                                              :fi "Tarkistathan, että kyseessä on varmasti kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto. Näitä tutkintoja ei pääsääntöisesti ole ollut enää 2000-luvulla. Vuotta 2005 myöhempiä kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkintoja ei lomakkeella pysty ilmoittamaan.",
                                              :sv "Kontrollera att det verkligen är en examen på skolnivå, institutsnivå eller inom yrkesutbildning på högre nivå. Dessa examina fanns i regel inte kvar på 2000-talet. Det är inte möjligt att ange senare än år 2005 avlagda examina på blanketten."},
                                       :label {:fi ""},
                                       :fieldType "p",
                                       :fieldClass "infoElement"}
                                      {:label {:fi "", :sv ""},
                                       :children [{:label {:en "Year of completion", :fi "Suoritusvuosi", :sv "Avlagd år"},
                                                   :params {:size "S",
                                                            :hidden false,
                                                            :numeric true,
                                                            :max-value "2005",
                                                            :min-value "1900"},
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["numeric" "required"]}
                                                  {:label {:en "Type of vocational qualification",
                                                           :fi "Ammatillisen tutkinnon tyyppi",
                                                           :sv "Yrkesinriktad examens typ"},
                                                   :params {:hidden false},
                                                   :options [{:label {:en "Vocational qualification (kouluaste)",
                                                                      :fi "Kouluasteen tutkinto",
                                                                      :sv "Yrkesinriktad examen på skolnivå"},
                                                              :value "0",
                                                              :followups [{:text {:en "Please make sure that your degree is truly a Finnish school level degree (kouluasteen tutkinto). As a rule, these degrees are no longer available in the 2000s. For example, the degrees of agronom, a commercial school graduate (merkonomi) and a technician are not school level degrees.",
                                                                                  :fi "Tarkistathan, että kyseessä on varmasti kouluasteen tutkinto. Näitä tutkintoja ei pääsääntöisesti ole ollut enää 2000-luvulla. Esimerkiksi agrologin, teknikon tai merkonomin tutkinnot eivät ole kouluasteen tutkintoja.",
                                                                                  :sv "Kontrollera att det verkligen är en examen på skolnivå. Dessa examina fanns i regel inte kvar på 2000-talet. Exempelvis agrolog-, tekniker- och merkonomexamina är inte examina på skolnivå."},
                                                                           :label {:fi ""},
                                                                           :fieldType "p",
                                                                           :fieldClass "infoElement"}]}
                                                             {:label {:en "Vocational qualification (opistoaste)",
                                                                      :fi "Opistoasteen tutkinto",
                                                                      :sv "Yrkesinriktad examen på institutsnivå"},
                                                              :value "1",
                                                              :followups [{:text {:en "Please make sure that your degree is truly a Finnish post-secondary level degree (opistoasteen tutkinto). As a rule, these degrees are no longer available in the 2000s. For example, the degrees of a practical nurse, a commercial school graduate (merkantti) and a mechanic are not post-secondary level degrees.",
                                                                                  :fi "Tarkistathan, että kyseessä on varmasti opistoasteen tutkinto. Näitä tutkintoja ei pääsääntöisesti ole ollut enää 2000-luvulla. Esimerkiksi lähihoitajan, merkantin ja mekaanikon tutkinnot eivät ole opistoasteen tutkintoja.",
                                                                                  :sv "Kontrollera att det verkligen är en examen på institutnivå. Dessa examina fanns i regel inte kvar på 2000-talet. Exempelvis närvårdar-, merkant- och mekanikerexamina är inte examina på institutnivå."},
                                                                           :label {:fi ""},
                                                                           :fieldType "p",
                                                                           :fieldClass "infoElement"}]}
                                                             {:label {:en "Vocational qualification (ammatillinen korkea-aste)",
                                                                      :fi "Ammatillisen korkea-asteen tutkinto",
                                                                      :sv "Yrkesinriktad examen på högre nivå"},
                                                              :value "2",
                                                              :followups [{:text {:en "Please make sure that your degree is truly a Finnish higher vocational level degree. As a rule, these degrees are no longer available in the 2000s. For example, the degrees of a practical nurse, a vocational qualification in business and administration (merkonomi) and a datanome are not higher vocational level degrees.",
                                                                                  :fi "Tarkistathan, että kyseessä on varmasti ammatillisen korkea-asteen tutkinto. Näitä tutkintoja ei pääsääntöisesti ole ollut enää 2000-luvulla. Esimerkiksi lähihoitajan, merkonomin ja datanomin tutkinnot eivät ole ammatillisen korkea-asteen tutkintoja.",
                                                                                  :sv "Kontrollera att det verkligen är en examen inom yrkesutbildning på högre nivå. Dessa examina fanns i regel inte kvar på 2000-talet. Exempelvis närvårdar-, merkonom- och datanomexamina är inte examina inom yrkesutbildning på högre nivå."},
                                                                           :label {:fi ""},
                                                                           :fieldType "p",
                                                                           :fieldClass "infoElement"}]}],
                                                   :fieldType "dropdown",
                                                   :fieldClass "formField",
                                                   :validators ["required"]}
                                                  {:label {:en "Vocational qualification",
                                                           :fi "Ammatillinen tutkinto",
                                                           :sv "Yrkesinriktad examen"},
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["required"]}
                                                  {:label {:en "Scope of vocational qualification",
                                                           :fi "Ammatillisen tutkinnon laajuus",
                                                           :sv "Omfattning av yrkesinriktad examen"},
                                                   :params {:size "S", :numeric true, :decimals 1},
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["numeric" "required"]}
                                                  {:label {:en "The scope unit",
                                                           :fi "Laajuuden yksikkö",
                                                           :sv "Omfattningens enhet"},
                                                   :options [{:label {:en "Courses", :fi "Kurssia", :sv "Kurser"},
                                                              :value "0"}
                                                             {:label {:en "ECTS credits",
                                                                      :fi "Opintopistettä",
                                                                      :sv "Studiepoäng"},
                                                              :value "1"}
                                                             {:label {:en "Study weeks",
                                                                      :fi "Opintoviikkoa",
                                                                      :sv "Studieveckor"},
                                                              :value "2"}
                                                             {:label {:en "Competence points",
                                                                      :fi "Osaamispistettä",
                                                                      :sv "Kompetenspoäng"},
                                                              :value "3"}
                                                             {:label {:en "Hours", :fi "Tuntia", :sv "Timmar"},
                                                              :value "4"}
                                                             {:label {:en "Weekly lessons per year",
                                                                      :fi "Vuosiviikkotuntia",
                                                                      :sv "Årsveckotimmar"},
                                                              :value "5"}
                                                             {:label {:en "Years", :fi "Vuotta", :sv "År"}, :value "6"}],
                                                   :fieldType "dropdown",
                                                   :fieldClass "formField",
                                                   :validators ["required"]}
                                                  {:params {:info-text {:label {:en "Select \"Unknown\", if the educational institution where you have completed your degree cannot be found on the list.",
                                                                                :fi "Valitse \"Tuntematon\", jos et löydä listasta oppilaitosta, jossa olet suorittanut tutkintosi.",
                                                                                :sv "Välj \"Okänd\" om du inte hittar den läroanstalt, där du har avlagt din examen."}}},
                                                   :koodisto-source {:uri "oppilaitostyyppi",
                                                                     :title "Ammatilliset oppilaitokset",
                                                                     :version 1,
                                                                     :allow-invalid? true},
                                                   :validators ["required"],
                                                   :fieldClass "formField",
                                                   :label {:en "Educational institution",
                                                           :fi "Oppilaitos ",
                                                           :sv "Läroanstalt "},
                                                   :options [],
                                                   :fieldType "dropdown"}
                                                  (seven-day-attachment-followup {:en "Vocational qualification diploma (kouluaste, opistoaste, ammatillinen korkea-aste",
                                                                                  :fi "Kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkintotodistus",
                                                                                  :sv "Betyg från yrkesinriktad examen på skolnivå, examen på institutsnivå eller yrkesinriktad examen på högre nivå"})
                                                  {:text {:fi ""},
                                                   :label {:en "Click add if you want to add further qualifications.",
                                                           :fi "Paina lisää, jos haluat lisätä useampia tutkintoja.",
                                                           :sv "Tryck på lägg till om du vill lägga till flera examina."},
                                                   :fieldType "p",
                                                   :fieldClass "infoElement"}],
                                       :fieldType "fieldset",
                                       :fieldClass "questionGroup"}]}
                         {:label {:en "Upper secondary education completed outside Finland (general or vocational)",
                                  :fi "Muualla kuin Suomessa suoritettu muu tutkinto, joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun",
                                  :sv "Övrig examen som avlagts annanstans än i Finland, och ger behörighet för högskolestudier i ifrågavarande land"},
                          :value "pohjakoulutus_ulk",
                          :followups [{:label {:fi "", :sv ""},
                                       :children [{:label {:en "Year of completion", :fi "Suoritusvuosi", :sv "Avlagd år"},
                                                   :params {:size "S",
                                                            :numeric true,
                                                            :max-value "2022",
                                                            :min-value "1900"},
                                                   :options [{:label {:fi "", :sv ""},
                                                              :value "0",
                                                              :condition {:answer-compared-to 2022,
                                                                          :comparison-operator "="},
                                                              :followups [{:label {:en "Have you graduated",
                                                                                   :fi "Oletko valmistunut?",
                                                                                   :sv "Har du tagit examen"},
                                                                           :options [{:label {:en "Yes",
                                                                                              :fi "Kyllä",
                                                                                              :sv "Ja"},
                                                                                      :value "0",
                                                                                      :followups [(seven-day-attachment-followup {:en "Upper secondary education diploma",
                                                                                                                                  :fi "Tutkintotodistus muualla kuin Suomessa suoritetusta tutkinnosta, joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun",
                                                                                                                                  :sv "Examensbetyg som avlagts annanstans än i Finland och som i landet ifråga ger ansökningsbehörighet för högskola"})
                                                                                                  {:label {:en "Is your original diploma in Finnish, Swedish or English?",
                                                                                                           :fi "Onko todistuksesi suomen-, ruotsin- tai englanninkielinen?",
                                                                                                           :sv "Är ditt betyg finsk-, svensk-, eller engelskspråkigt?"},
                                                                                                   :params {:hidden false},
                                                                                                   :options [{:label {:en "Yes",
                                                                                                                      :fi "Kyllä",
                                                                                                                      :sv "Ja"},
                                                                                                              :value "0"}
                                                                                                             {:label {:en "No",
                                                                                                                      :fi "Ei",
                                                                                                                      :sv "Nej"},
                                                                                                              :value "1",
                                                                                                              :followups [(seven-day-attachment-followup {:en "Official translation of the diploma to Finnish, Swedish or English",
                                                                                                                                                          :fi "Virallinen käännös suomeksi, ruotsiksi tai englanniksi",
                                                                                                                                                          :sv "Officiell översättning av intyget till finska, svenska eller engelska"})]}],
                                                                                                   :fieldType "singleChoice",
                                                                                                   :fieldClass "formField",
                                                                                                   :validators ["required"]}]}
                                                                                     {:label {:en "No",
                                                                                              :fi "En",
                                                                                              :sv "Nej"},
                                                                                      :value "1",
                                                                                      :followups [{:label {:en "Estimated graduation date (dd.mm.yyyy)",
                                                                                                           :fi "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)",
                                                                                                           :sv "Beräknat examensdatum (dd.mm.åååå)"},
                                                                                                   :params {:size "S",
                                                                                                            :numeric false,
                                                                                                            :decimals nil},
                                                                                                   :fieldType "textField",
                                                                                                   :fieldClass "formField",
                                                                                                   :validators ["required"]}
                                                                                                  (seven-day-attachment-followup {:en "Latest transcript of study records (upper secondary education diploma)",
                                                                                                                                  :fi "Ennakkoarvio tai viimeisin todistus suoritetuista opinnoista muualla kuin Suomessa suoritettavasta toisen asteen tutkinnosta",
                                                                                                                                  :sv "Förhandsexamensbetyg eller betyg över slutförda studier om examen som avlagts annanstans än i Finland och som i landet ifråga ger ansökningsbehörighet för högskola"})
                                                                                                  {:params {:hidden false,
                                                                                                            :deadline nil,
                                                                                                            :info-text {:value {:en "The exact deadline is available next to the attachment request.

                                                                                                                                   Name the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.

                                                                                                                                   Scan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright.

                                                                                                                                   Recommended file formats are: PDF, JPG, PNG and DOCX.",
                                                                                                                                :fi "Määräaika ilmoitetaan liitepyynnön vieressä.

                                                                                                                                   Nimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus

                                                                                                                                   Skannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.

                                                                                                                                   Suositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                                                                                                                                :sv "Den angivna tidpunkten syns invid begäran om bilagor.

                                                                                                                                   Namnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg

                                                                                                                                   Skanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.
                                                                                                                                   Samla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.
                                                                                                                                   Kontrollera att dokumenten i filen är rättvända.

                                                                                                                                   Rekommenderade filformat är PDF, JPG, PNG och DOCX.
                                                                                                                                   "},
                                                                                                                        :enabled? true}},
                                                                                                   :fieldClass "formField",
                                                                                                   :label {:en "Original upper secondary education diploma",
                                                                                                           :fi "Tutkintotodistus muualla kuin Suomessa suoritetusta tutkinnosta, joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun",
                                                                                                           :sv "Examensbetyg som avlagts annanstans än i Finland och som i landet ifråga ger ansökningsbehörighet för högskola"},
                                                                                                   :options [],
                                                                                                   :fieldType "attachment"}
                                                                                                  {:label {:en "Are your attachments in Finnish, Swedish or English?",
                                                                                                           :fi "Ovatko liitteesi suomen-, ruotsin- tai englanninkielisiä?",
                                                                                                           :sv "Är dina bilagor finsk-, svensk-, eller engelskspråkiga?"},
                                                                                                   :params {:hidden false},
                                                                                                   :options [{:label {:en "Yes",
                                                                                                                      :fi "Kyllä",
                                                                                                                      :sv "Ja"},
                                                                                                              :value "0"}
                                                                                                             {:label {:en "No",
                                                                                                                      :fi "Ei",
                                                                                                                      :sv "Nej"},
                                                                                                              :value "1",
                                                                                                              :followups [(seven-day-attachment-followup {:en "Official translation of the latest transcript of study records to Finnish, Swedish or English",
                                                                                                                                                          :fi "Virallinen käännös ennakkoarviosta tai viimeisimmästä todistuksestasi suomeksi, ruotsiksi tai englanniksi",
                                                                                                                                                          :sv "Officiell översättning av förhandsexamensbetyget eller betyget över slutförda studier till finska, svenska eller engelska"})
                                                                                                                          {:params {:hidden false,
                                                                                                                                    :deadline nil,
                                                                                                                                    :info-text {:value {:en "The exact deadline is available next to the attachment request.

                                                                                                                                                           Name the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.

                                                                                                                                                           Scan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright.

                                                                                                                                                           Recommended file formats are: PDF, JPG, PNG and DOCX.",
                                                                                                                                                        :fi "Määräaika ilmoitetaan liitepyynnön vieressä.

                                                                                                                                                           Nimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus

                                                                                                                                                           Skannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.

                                                                                                                                                           Suositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                                                                                                                                                        :sv "Den angivna tidpunkten syns invid begäran om bilagor.

                                                                                                                                                           Namnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg

                                                                                                                                                           Skanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.
                                                                                                                                                           Samla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.
                                                                                                                                                           Kontrollera att dokumenten i filen är rättvända.

                                                                                                                                                           Rekommenderade filformat är PDF, JPG, PNG och DOCX.
                                                                                                                                                           "},
                                                                                                                                                :enabled? true}},
                                                                                                                           :fieldClass "formField",
                                                                                                                           :label {:en "Official translation of the diploma to Finnish, Swedish or English",
                                                                                                                                   :fi "Virallinen käännös tutkintotodistuksesta suomeksi, ruotsiksi tai englanniksi",
                                                                                                                                   :sv "Officiell översättning av examensbetyget till finska, svenska eller engelska"},
                                                                                                                           :options [],
                                                                                                                           :fieldType "attachment"}]}],
                                                                                                   :fieldType "singleChoice",
                                                                                                   :fieldClass "formField",
                                                                                                   :validators ["required"]}]}],
                                                                           :fieldType "singleChoice",
                                                                           :fieldClass "formField",
                                                                           :validators ["required"]}]}
                                                             {:label {:fi "", :sv ""},
                                                              :value "1",
                                                              :condition {:answer-compared-to 2022,
                                                                          :comparison-operator "<"},
                                                              :followups [(seven-day-attachment-followup {:en "Upper secondary education diploma",
                                                                                                          :fi "Tutkintotodistus muualla kuin Suomessa suoritetusta tutkinnosta, joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun",
                                                                                                          :sv "Examensbetyg som avlagts annanstans än i Finland och som i landet ifråga ger ansökningsbehörighet för högskola"})
                                                                          {:label {:en "Are your attachments in Finnish, Swedish or English?",
                                                                                   :fi "Ovatko liitteesi suomen-, ruotsin- tai englanninkielisiä?",
                                                                                   :sv "Är dina bilagor finsk-, svensk-, eller engelskspråkiga?"},
                                                                           :params {:hidden false},
                                                                           :options [{:label {:en "Yes",
                                                                                              :fi "Kyllä",
                                                                                              :sv "Ja"},
                                                                                      :value "0"}
                                                                                     {:label {:en "No",
                                                                                              :fi "Ei",
                                                                                              :sv "Nej"},
                                                                                      :value "1",
                                                                                      :followups [(seven-day-attachment-followup {:en "Official translation of the diploma to Finnish, Swedish or English",
                                                                                                                                  :fi "Virallinen käännös suomeksi, ruotsiksi tai englanniksi",
                                                                                                                                  :sv "Officiell översättning av intyget till finska, svenska eller engelska"})]}],
                                                                           :fieldType "singleChoice",
                                                                           :fieldClass "formField",
                                                                           :validators ["required"]}]}],
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["numeric" "required"]}
                                                  {:label {:en "Degree", :fi "Tutkinto", :sv "Examen"},
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["required"]}
                                                  {:label {:en "Educational institution",
                                                           :fi "Oppilaitos",
                                                           :sv "Läroanstalt"},
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["required"]}
                                                  {:koodisto-source {:uri "maatjavaltiot2",
                                                                     :title "Maat ja valtiot",
                                                                     :version 2,
                                                                     :allow-invalid? true},
                                                   :validators ["required"],
                                                   :fieldClass "formField",
                                                   :label {:en "Country of completion",
                                                           :fi "Suoritusmaa",
                                                           :sv "Land där examen är avlagd"},
                                                   :options [],
                                                   :fieldType "dropdown"}
                                                  {:text {:fi ""},
                                                   :label {:en "Click add if you want to add further qualifications.",
                                                           :fi "Paina lisää, jos haluat lisätä useampia tutkintoja.",
                                                           :sv "Tryck på lägg till om du vill lägga till flera examina."},
                                                   :fieldType "p",
                                                   :fieldClass "infoElement"}],
                                       :fieldType "fieldset",
                                       :fieldClass "questionGroup"}]}
                         {:label {:en "International matriculation examination completed outside Finland (IB, EB and RP/DIA)",
                                  :fi "Muualla kuin Suomessa suoritettu kansainvälinen ylioppilastutkinto (IB, EB ja RP/DIA)",
                                  :sv "Internationell studentexamen som avlagts annanstans än i Finland (IB, EB och RP/DIA)"},
                          :value "pohjakoulutus_yo_ulkomainen",
                          :followups [{:label {:fi "", :sv ""},
                                       :children [{:label {:en "Year of completion", :fi "Suoritusvuosi", :sv "Avlagd år"},
                                                   :params {:size "S",
                                                            :numeric true,
                                                            :max-value "2022",
                                                            :min-value "1900"},
                                                   :options [{:label {:fi "", :sv ""},
                                                              :value "0",
                                                              :condition {:answer-compared-to 2022,
                                                                          :comparison-operator "<"},
                                                              :followups [{:label {:en "Matriculation examination",
                                                                                   :fi "Ylioppilastutkinto",
                                                                                   :sv "Studentexamen"},
                                                                           :options [{:label {:en "International Baccalaureate -diploma",
                                                                                              :fi "International Baccalaureate -tutkinto",
                                                                                              :sv "International Baccalaureate -examen"},
                                                                                      :value "0",
                                                                                      :followups [(seven-day-attachment-followup {:en "IB Diploma completed outside Finland",
                                                                                                                                  :fi "IB Diploma -tutkintotodistus muualla kuin Suomessa suoritetusta tutkinnosta",
                                                                                                                                  :sv "IB Diploma från IB-studentexamen som avlagts annanstans än i Finland"})]}
                                                                                     {:label {:en "European Baccalaureate -diploma",
                                                                                              :fi "European Baccalaureate -tutkinto",
                                                                                              :sv "European Baccalaureate -examen"},
                                                                                      :value "1",
                                                                                      :followups [(seven-day-attachment-followup {:en "European Baccalaureate diploma completed outside Finland",
                                                                                                                                  :fi "European Baccalaureate Diploma -tutkintotodistus muualla kuin Suomessa suoritetusta tutkinnosta",
                                                                                                                                  :sv "European Baccalaureate Diploma från EB-studentexamen som avlagts annanstans än i Finland"})]}
                                                                                     {:label {:en "Deutsche Internationale Abiturprüfung/Reifeprüfung -diploma",
                                                                                              :fi "Deutsche Internationale Abiturprüfung/Reifeprüfung -tutkinto",
                                                                                              :sv "Deutsche Internationale Abiturprüfung/Reifeprüfung -examen"},
                                                                                      :value "2",
                                                                                      :followups [(seven-day-attachment-followup {:en "Reifeprüfung/DIA diploma completed outside Finland",
                                                                                                                                  :fi "Reifeprüfung/DIA-tutkintotodistus muualla kuin Suomessa suoritetusta tutkinnosta",
                                                                                                                                  :sv "Reifeprüfung/DIA -examensbetyg from RP/DIA-studentexamen som avlagts annanstans än i Finland"})]}],
                                                                           :fieldType "singleChoice",
                                                                           :fieldClass "formField",
                                                                           :validators ["required"]}]}
                                                             {:label {:fi "", :sv ""},
                                                              :value "1",
                                                              :condition {:answer-compared-to 2022,
                                                                          :comparison-operator "="},
                                                              :followups [{:label {:en "Matriculation examination",
                                                                                   :fi "Ylioppilastutkinto",
                                                                                   :sv "Studentexamen"},
                                                                           :options [{:label {:en "International Baccalaureate -diploma",
                                                                                              :fi "International Baccalaureate -tutkinto",
                                                                                              :sv "International Baccalaureate -examen"},
                                                                                      :value "0",
                                                                                      :followups [{:label {:en "Have you graduated",
                                                                                                           :fi "Oletko valmistunut?",
                                                                                                           :sv "Har du tagit examen"},
                                                                                                   :options [{:label {:en "Yes",
                                                                                                                      :fi "Kyllä",
                                                                                                                      :sv "Ja"},
                                                                                                              :value "0",
                                                                                                              :followups [(seven-day-attachment-followup {:en "IB Diploma completed outside Finland",
                                                                                                                                                          :fi "IB Diploma -tutkintotodistus muualla kuin Suomessa suoritetusta tutkinnosta",
                                                                                                                                                          :sv "IB Diploma från IB-studentexamen som avlagts i annanstans än Finland"})]}
                                                                                                             {:label {:en "No",
                                                                                                                      :fi "En",
                                                                                                                      :sv "Nej"},
                                                                                                              :value "1",
                                                                                                              :followups [{:label {:en "Estimated graduation date (dd.mm.yyyy)",
                                                                                                                                   :fi "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)",
                                                                                                                                   :sv "Beräknat examensdatum (dd.mm.åååå)"},
                                                                                                                           :params {:size "S",
                                                                                                                                    :numeric false,
                                                                                                                                    :decimals nil},
                                                                                                                           :fieldType "textField",
                                                                                                                           :fieldClass "formField",
                                                                                                                           :validators ["required"]}
                                                                                                                          {:params {:hidden false,
                                                                                                                                    :deadline nil,
                                                                                                                                    :info-text {:value {:en "The attachment has to be submitted  by the deadline informed next to the attachment request.

                                                                                                                                                           Name the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.

                                                                                                                                                           Scan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright.

                                                                                                                                                           Recommended file formats are: PDF, JPG, PNG and DOCX.
                                                                                                                                                           ",
                                                                                                                                                        :fi "Määräaika ilmoitetaan liitepyynnön vieressä.

                                                                                                                                                           Nimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus

                                                                                                                                                           Skannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.

                                                                                                                                                           Suositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.
                                                                                                                                                           ",
                                                                                                                                                        :sv "Den angivna tidpunkten syns invid begäran om bilagor.

                                                                                                                                                           Namnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg

                                                                                                                                                           Skanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.
                                                                                                                                                           Samla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.
                                                                                                                                                           Kontrollera att dokumenten i filen är rättvända.

                                                                                                                                                           Rekommenderade filformat är PDF, JPG, PNG och DOCX."},
                                                                                                                                                :enabled? true}},
                                                                                                                           :fieldClass "formField",
                                                                                                                           :label {:en "Predicted grades from IB completed outside Finland",
                                                                                                                                   :fi "Oppilaitoksen ennakkoarvio muualla kuin Suomessa suoritettavan tutkinnon arvosanoista (Candidate Predicted Grades)",
                                                                                                                                   :sv "Predicted grades från IB-studentexamen som avlagts annanstans än i Finland "},
                                                                                                                           :options [],
                                                                                                                           :fieldType "attachment"}
                                                                                                                          {:params {:hidden false,
                                                                                                                                    :deadline nil,
                                                                                                                                    :info-text {:value {:en "The attachment has to be submitted  by the deadline informed next to the attachment request.

                                                                                                                                                           Name the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.

                                                                                                                                                           Scan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright.

                                                                                                                                                           Recommended file formats are: PDF, JPG, PNG and DOCX.
                                                                                                                                                           ",
                                                                                                                                                        :fi "Määräaika ilmoitetaan liitepyynnön vieressä.

                                                                                                                                                           Nimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus

                                                                                                                                                           Skannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.

                                                                                                                                                           Suositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.
                                                                                                                                                           ",
                                                                                                                                                        :sv "Den angivna tidpunkten syns invid begäran om bilagor.

                                                                                                                                                           Namnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg

                                                                                                                                                           Skanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.
                                                                                                                                                           Samla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.
                                                                                                                                                           Kontrollera att dokumenten i filen är rättvända.

                                                                                                                                                           Rekommenderade filformat är PDF, JPG, PNG och DOCX."},
                                                                                                                                                :enabled? true}},
                                                                                                                           :fieldClass "formField",
                                                                                                                           :label {:en "Diploma Programme (DP) Results from IB completed outside Finland",
                                                                                                                                   :fi "Diploma Programme (DP) Results -asiakirja muualla kuin Suomessa suoritettavasta tutkinnosta",
                                                                                                                                   :sv "Diploma Programme (DP) Results från IB-studentexamen som avlagts annanstans än i Finland"},
                                                                                                                           :options [],
                                                                                                                           :fieldType "attachment"}]}],
                                                                                                   :fieldType "singleChoice",
                                                                                                   :fieldClass "formField",
                                                                                                   :validators ["required"]}]}
                                                                                     {:label {:en "European Baccalaureate -diploma",
                                                                                              :fi "Eurooppalainen ylioppilastutkinto",
                                                                                              :sv "European Baccalaureate -examen"},
                                                                                      :value "1",
                                                                                      :followups [{:label {:en "Have you graduated",
                                                                                                           :fi "Oletko valmistunut?",
                                                                                                           :sv "Har du tagit examen"},
                                                                                                   :options [{:label {:en "Yes",
                                                                                                                      :fi "Kyllä",
                                                                                                                      :sv "Ja"},
                                                                                                              :value "0",
                                                                                                              :followups [(seven-day-attachment-followup {:en "European Baccalaureate diploma completed outside Finland",
                                                                                                                                                          :fi "European Baccalaureate Diploma -tutkintotodistus muualla kuin Suomessa suoritetusta tutkinnosta",
                                                                                                                                                          :sv "European Baccalaureate Diploma från EB-studentexamen som avlagts annanstans än i Finland"})]}
                                                                                                             {:label {:en "No",
                                                                                                                      :fi "En",
                                                                                                                      :sv "Nej"},
                                                                                                              :value "1",
                                                                                                              :followups [{:label {:en "Estimated graduation date (dd.mm.yyyy)",
                                                                                                                                   :fi "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)",
                                                                                                                                   :sv "Beräknat examensdatum (dd.mm.åååå)"},
                                                                                                                           :params {:size "S",
                                                                                                                                    :numeric false,
                                                                                                                                    :decimals nil},
                                                                                                                           :fieldType "textField",
                                                                                                                           :fieldClass "formField",
                                                                                                                           :validators ["required"]}
                                                                                                                          {:params {:hidden false,
                                                                                                                                    :deadline nil,
                                                                                                                                    :info-text {:value {:en "The attachment has to be submitted  by the deadline informed next to the attachment request.

                                                                                                                                                           Name the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.

                                                                                                                                                           Scan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright.

                                                                                                                                                           Recommended file formats are: PDF, JPG, PNG and DOCX.
                                                                                                                                                           ",
                                                                                                                                                        :fi "Määräaika ilmoitetaan liitepyynnön vieressä.

                                                                                                                                                           Nimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus

                                                                                                                                                           Skannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.

                                                                                                                                                           Suositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.
                                                                                                                                                           ",
                                                                                                                                                        :sv "Den angivna tidpunkten syns invid begäran om bilagor.

                                                                                                                                                           Namnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg

                                                                                                                                                           Skanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.
                                                                                                                                                           Samla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.
                                                                                                                                                           Kontrollera att dokumenten i filen är rättvända.

                                                                                                                                                           Rekommenderade filformat är PDF, JPG, PNG och DOCX."},
                                                                                                                                                :enabled? true}},
                                                                                                                           :fieldClass "formField",
                                                                                                                           :label {:en "Predicted grades from EB completed outside Finland",
                                                                                                                                   :fi "Oppilaitoksen ennakkoarvio muualla kuin Suomessa suoritettavan EB-tutkinnon arvosanoista",
                                                                                                                                   :sv "Läroanstaltens preliminära vitsord från EB-studentexamen som avlagts annanstans än i Finland"},
                                                                                                                           :options [],
                                                                                                                           :fieldType "attachment"}
                                                                                                                          {:params {:hidden false,
                                                                                                                                    :deadline nil,
                                                                                                                                    :info-text {:value {:en "The attachment has to be submitted  by the deadline informed next to the attachment request.

                                                                                                                                                           Name the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.

                                                                                                                                                           Scan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright.

                                                                                                                                                           Recommended file formats are: PDF, JPG, PNG and DOCX.
                                                                                                                                                           ",
                                                                                                                                                        :fi "Määräaika ilmoitetaan liitepyynnön vieressä.

                                                                                                                                                           Nimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus

                                                                                                                                                           Skannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.

                                                                                                                                                           Suositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.
                                                                                                                                                           ",
                                                                                                                                                        :sv "Den angivna tidpunkten syns invid begäran om bilagor.

                                                                                                                                                           Namnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg

                                                                                                                                                           Skanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.
                                                                                                                                                           Samla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.
                                                                                                                                                           Kontrollera att dokumenten i filen är rättvända.

                                                                                                                                                           Rekommenderade filformat är PDF, JPG, PNG och DOCX."},
                                                                                                                                                :enabled? true}},
                                                                                                                           :fieldClass "formField",
                                                                                                                           :label {:en "European Baccalaureate diploma completed outside Finland",
                                                                                                                                   :fi "European Baccalaureate Diploma -tutkintotodistus muualla kuin Suomessa suoritettavasta tutkinnosta",
                                                                                                                                   :sv "European Baccalaureate Diploma från EB-studentexamen som avlagts annanstans än i Finland"},
                                                                                                                           :options [],
                                                                                                                           :fieldType "attachment"}]}],
                                                                                                   :fieldType "singleChoice",
                                                                                                   :fieldClass "formField",
                                                                                                   :validators ["required"]}]}
                                                                                     {:label {:en "Deutsche Internationale Abiturprüfung/Reifeprüfung -diploma",
                                                                                              :fi "Deutsche Internationale Abiturprüfung/Reifeprüfung -tutkinto",
                                                                                              :sv "Deutsche Internationale Abiturprüfung/Reifeprüfung -examen"},
                                                                                      :value "2",
                                                                                      :followups [{:label {:en "Have you graduated",
                                                                                                           :fi "Oletko valmistunut?",
                                                                                                           :sv "Har du tagit examen"},
                                                                                                   :options [{:label {:en "Yes",
                                                                                                                      :fi "Kyllä",
                                                                                                                      :sv "Ja"},
                                                                                                              :value "0",
                                                                                                              :followups [(seven-day-attachment-followup {:en "Reifeprüfung/DIA diploma from RP/DIA completed outside Finland",
                                                                                                                                                          :fi "Reifeprüfung/DIA-tutkintotodistus muualla kuin Suomessa suoritetusta tutkinnosta",
                                                                                                                                                          :sv "Reifeprüfung/DIA -examensbetyg from RP/DIA-studentexamen som avlagts annanstans än i Finland"})]}
                                                                                                             {:label {:en "No",
                                                                                                                      :fi "En",
                                                                                                                      :sv "Nej"},
                                                                                                              :value "1",
                                                                                                              :followups [{:label {:en "Estimated graduation date (dd.mm.yyyy)",
                                                                                                                                   :fi "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)",
                                                                                                                                   :sv "Beräknat examensdatum (dd.mm.åååå)"},
                                                                                                                           :params {:size "S",
                                                                                                                                    :numeric false,
                                                                                                                                    :decimals nil},
                                                                                                                           :fieldType "textField",
                                                                                                                           :fieldClass "formField",
                                                                                                                           :validators ["required"]}
                                                                                                                          {:params {:hidden false,
                                                                                                                                    :deadline nil,
                                                                                                                                    :info-text {:value {:en "The attachment has to be submitted  by the deadline informed next to the attachment request.

                                                                                                                                                           Name the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.

                                                                                                                                                           Scan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright.

                                                                                                                                                           Recommended file formats are: PDF, JPG, PNG and DOCX.
                                                                                                                                                           ",
                                                                                                                                                        :fi "Määräaika ilmoitetaan liitepyynnön vieressä.

                                                                                                                                                           Nimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus

                                                                                                                                                           Skannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.

                                                                                                                                                           Suositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.
                                                                                                                                                           ",
                                                                                                                                                        :sv "Den angivna tidpunkten syns invid begäran om bilagor.

                                                                                                                                                           Namnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg

                                                                                                                                                           Skanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.
                                                                                                                                                           Samla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.
                                                                                                                                                           Kontrollera att dokumenten i filen är rättvända.

                                                                                                                                                           Rekommenderade filformat är PDF, JPG, PNG och DOCX."},
                                                                                                                                                :enabled? true}},
                                                                                                                           :fieldClass "formField",
                                                                                                                           :label {:en "Grade page of a Deutsches Internationales Abitur (DIA) diploma completed outside Finland",
                                                                                                                                   :fi "DIA-tutkintotodistuksen arvosanasivu muualla kuin Suomessa suoritettavasta tutkinnosta",
                                                                                                                                   :sv "Examensbetygets vitsordssida av Deutsches Internationales Abitur (DIA) -examen avlagd annanstans än i Finland"},
                                                                                                                           :options [],
                                                                                                                           :fieldType "attachment"}
                                                                                                                          {:params {:hidden false,
                                                                                                                                    :deadline nil,
                                                                                                                                    :info-text {:value {:en "The attachment has to be submitted  by the deadline informed next to the attachment request.

                                                                                                                                                           Name the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.

                                                                                                                                                           Scan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright.

                                                                                                                                                           Recommended file formats are: PDF, JPG, PNG and DOCX.
                                                                                                                                                           ",
                                                                                                                                                        :fi "Määräaika ilmoitetaan liitepyynnön vieressä.

                                                                                                                                                           Nimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus

                                                                                                                                                           Skannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.

                                                                                                                                                           Suositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.
                                                                                                                                                           ",
                                                                                                                                                        :sv "Den angivna tidpunkten syns invid begäran om bilagor.

                                                                                                                                                           Namnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg

                                                                                                                                                           Skanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.
                                                                                                                                                           Samla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.
                                                                                                                                                           Kontrollera att dokumenten i filen är rättvända.

                                                                                                                                                           Rekommenderade filformat är PDF, JPG, PNG och DOCX."},
                                                                                                                                                :enabled? true}},
                                                                                                                           :fieldClass "formField",
                                                                                                                           :label {:en "DIA -diploma from DIA completed outside Finland",
                                                                                                                                   :fi "DIA-tutkintotodistus muualla kuin Suomessa suoritettavasta tutkinnosta",
                                                                                                                                   :sv "DIA -examensbetyg från DIA-studentexamen som avlagts annanstans än i Finland"},
                                                                                                                           :options [],
                                                                                                                           :fieldType "attachment"}]}],
                                                                                                   :fieldType "singleChoice",
                                                                                                   :fieldClass "formField",
                                                                                                   :validators ["required"]}]}],
                                                                           :fieldType "singleChoice",
                                                                           :fieldClass "formField",
                                                                           :validators ["required"]}]}],
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["numeric" "required"]}
                                                  {:label {:en "Educational institution",
                                                           :fi "Oppilaitos",
                                                           :sv "Läroanstalt"},
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["required"]}
                                                  {:koodisto-source {:uri "maatjavaltiot2",
                                                                     :title "Maat ja valtiot",
                                                                     :version 2,
                                                                     :allow-invalid? true},
                                                   :validators ["required"],
                                                   :fieldClass "formField",
                                                   :label {:en "Country of completion",
                                                           :fi "Suoritusmaa",
                                                           :sv "Land där examen är avlagd"},
                                                   :options [],
                                                   :fieldType "dropdown"}
                                                  {:text {:fi ""},
                                                   :label {:en "Click add if you want to add further qualifications.",
                                                           :fi "Paina lisää, jos haluat lisätä useampia tutkintoja.",
                                                           :sv "Tryck på lägg till om du vill lägga till flera examina."},
                                                   :fieldType "p",
                                                   :fieldClass "infoElement"}],
                                       :fieldType "fieldset",
                                       :fieldClass "questionGroup"}]}
                         {:label {:en "Bachelor’s/Master’s/Doctoral degree completed outside Finland",
                                  :fi "Muualla kuin Suomessa suoritettu korkeakoulututkinto",
                                  :sv "Högskoleexamen som avlagts annanstans än i Finland"},
                          :value "pohjakoulutus_kk_ulk",
                          :followups [{:label {:fi "", :sv ""},
                                       :children [{:label {:en "Year of completion", :fi "Suoritusvuosi", :sv "Avlagd år"},
                                                   :params {:size "S",
                                                            :numeric true,
                                                            :max-value "2022",
                                                            :min-value "1900"},
                                                   :options [{:label {:fi "", :sv ""},
                                                              :value "0",
                                                              :condition {:answer-compared-to 2022,
                                                                          :comparison-operator "="},
                                                              :followups [{:label {:en "Have you graduated?",
                                                                                   :fi "Oletko valmistunut?",
                                                                                   :sv "Har du tagit examen?"},
                                                                           :options [{:label {:en "Yes",
                                                                                              :fi "Kyllä",
                                                                                              :sv "Ja"},
                                                                                      :value "0",
                                                                                      :followups [(seven-day-attachment-followup {:en "Transcript of records of higher education degree completed outside Finland",
                                                                                                                                  :fi "Opintosuoritusote muualla kuin Suomessa suoritettuun korkeakoulututkintoon sisältyvistä opinnoista",
                                                                                                                                  :sv "Studieprestationsutdrag om högskoleexamen som avlagts annanstans än i Finland"})
                                                                                                  (seven-day-attachment-followup {:en "Higher education degree certificate",
                                                                                                                                  :fi "Muualla kuin Suomessa suoritetun korkeakoulututkinnon tutkintotodistus",
                                                                                                                                  :sv "Högskoleexamensbetyg som avlagts annanstans än i Finland"})
                                                                                                  {:label {:en "Are your attachments in Finnish, Swedish or English?",
                                                                                                           :fi "Ovatko liitteesi suomen-, ruotsin- tai englanninkielisiä?",
                                                                                                           :sv "Är dina bilagor finsk-, svensk-, eller engelskspråkiga?"},
                                                                                                   :params {:hidden false},
                                                                                                   :options [{:label {:en "Yes",
                                                                                                                      :fi "Kyllä",
                                                                                                                      :sv "Ja"},
                                                                                                              :value "0"}
                                                                                                             {:label {:en "No",
                                                                                                                      :fi "Ei",
                                                                                                                      :sv "Nej"},
                                                                                                              :value "1",
                                                                                                              :followups [(seven-day-attachment-followup {:en "Official translation of the certificate to Finnish, Swedish or English",
                                                                                                                                                          :fi "Virallinen käännös suomeksi, ruotsiksi tai englanniksi",
                                                                                                                                                          :sv "Officiell översättning av intyget till finska, svenska eller engelska"})]}],
                                                                                                   :fieldType "singleChoice",
                                                                                                   :fieldClass "formField",
                                                                                                   :validators ["required"]}]}
                                                                                     {:label {:en "No",
                                                                                              :fi "En",
                                                                                              :sv "Nej"},
                                                                                      :value "1",
                                                                                      :followups [{:label {:en "Estimated graduation date (dd.mm.yyyy)",
                                                                                                           :fi "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)",
                                                                                                           :sv "Beräknat examensdatum (dd.mm.åååå)"},
                                                                                                   :params {:size "S",
                                                                                                            :numeric false,
                                                                                                            :decimals nil},
                                                                                                   :fieldType "textField",
                                                                                                   :fieldClass "formField",
                                                                                                   :validators ["required"]}
                                                                                                  (seven-day-attachment-followup {:en "Transcript of records of higher education degree completed outside Finland",
                                                                                                                                  :fi "Opintosuoritusote muualla kuin Suomessa suoritettavaan korkeakoulututkintoon sisältyvistä opinnoista",
                                                                                                                                  :sv "Studieprestationsutdrag om högskoleexamen som avlagts annanstans än i Finland"})
                                                                                                  {:params {:hidden false,
                                                                                                            :deadline nil,
                                                                                                            :info-text {:value {:en "The exact deadline is available next to the attachment request.

                                                                                                                                   Name the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.

                                                                                                                                   Scan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright.

                                                                                                                                   Recommended file formats are: PDF, JPG, PNG and DOCX.",
                                                                                                                                :fi "Määräaika ilmoitetaan liitepyynnön vieressä.

                                                                                                                                   Nimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus

                                                                                                                                   Skannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.

                                                                                                                                   Suositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                                                                                                                                :sv "Den angivna tidpunkten syns invid begäran om bilagor.

                                                                                                                                   Namnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg

                                                                                                                                   Skanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.
                                                                                                                                   Samla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.
                                                                                                                                   Kontrollera att dokumenten i filen är rättvända.

                                                                                                                                   Rekommenderade filformat är PDF, JPG, PNG och DOCX. "},
                                                                                                                        :enabled? true}},
                                                                                                   :fieldClass "formField",
                                                                                                   :label {:en "Higher education degree certificate",
                                                                                                           :fi "Muualla kuin Suomessa suoritettavan korkeakoulututkinnon tutkintotodistus",
                                                                                                           :sv "Högskoleexamensbetyg som avlagts annanstans än i Finland"},
                                                                                                   :options [],
                                                                                                   :fieldType "attachment"}
                                                                                                  {:label {:en "Are your attachments in Finnish, Swedish or English?",
                                                                                                           :fi "Ovatko liitteesi suomen-, ruotsin- tai englanninkielisiä?",
                                                                                                           :sv "Är dina bilagor finsk-, svensk-, eller engelskspråkiga?"},
                                                                                                   :params {:hidden false},
                                                                                                   :options [{:label {:en "Yes",
                                                                                                                      :fi "Kyllä",
                                                                                                                      :sv "Ja"},
                                                                                                              :value "0"}
                                                                                                             {:label {:en "No",
                                                                                                                      :fi "Ei",
                                                                                                                      :sv "Nej"},
                                                                                                              :value "1",
                                                                                                              :followups [(seven-day-attachment-followup {:en "Official translation of the transcript of records to Finnish, Swedish or English",
                                                                                                                                                          :fi "Virallinen käännös opintosuoritusotteesta suomeksi, ruotsiksi tai englanniksi",
                                                                                                                                                          :sv "Officiell översättning av studieprestationsutdraget till finska, svenska eller engelska"})
                                                                                                                          {:params {:hidden false,
                                                                                                                                    :deadline nil,
                                                                                                                                    :info-text {:value {:en "The exact deadline is available next to the attachment request.

                                                                                                                                                           Name the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.

                                                                                                                                                           Scan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright.

                                                                                                                                                           Recommended file formats are: PDF, JPG, PNG and DOCX.",
                                                                                                                                                        :fi "Määräaika ilmoitetaan liitepyynnön vieressä.

                                                                                                                                                           Nimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus

                                                                                                                                                           Skannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.

                                                                                                                                                           Suositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                                                                                                                                                        :sv "Den angivna tidpunkten syns invid begäran om bilagor.

                                                                                                                                                           Namnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg

                                                                                                                                                           Skanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.
                                                                                                                                                           Samla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.
                                                                                                                                                           Kontrollera att dokumenten i filen är rättvända.

                                                                                                                                                           Rekommenderade filformat är PDF, JPG, PNG och DOCX. "},
                                                                                                                                                :enabled? true}},
                                                                                                                           :fieldClass "formField",
                                                                                                                           :label {:en "Official translation of the higher education degree certificate to Finnish, Swedish or English",
                                                                                                                                   :fi "Virallinen käännös tutkintotodistuksesta suomeksi, ruotsiksi tai englanniksi",
                                                                                                                                   :sv "Officiell översättning av högskoleexamensbetyget till finska, svenska eller engelska"},
                                                                                                                           :options [],
                                                                                                                           :fieldType "attachment"}]}],
                                                                                                   :fieldType "singleChoice",
                                                                                                   :fieldClass "formField",
                                                                                                   :validators ["required"]}]}],
                                                                           :fieldType "singleChoice",
                                                                           :fieldClass "formField",
                                                                           :validators ["required"]}]}
                                                             {:label {:fi "", :sv ""},
                                                              :value "1",
                                                              :condition {:answer-compared-to 2022,
                                                                          :comparison-operator "<"},
                                                              :followups [(seven-day-attachment-followup {:en "Transcript of records of higher education degree completed outside Finland",
                                                                                                          :fi "Opintosuoritusote muualla kuin Suomessa suoritettuun korkeakoulututkintoon sisältyvistä opinnoista",
                                                                                                          :sv "Studieprestationsutdrag om högskoleexamen som avlagts annanstans än i Finland"})
                                                                          (seven-day-attachment-followup {:en "Higher education degree certificate",
                                                                                                          :fi "Muualla kuin Suomessa suoritetun korkeakoulututkinnon tutkintotodistus",
                                                                                                          :sv "Högskoleexamensbetyg som avlagts annanstans än i Finland"})
                                                                          {:sensitive-answer true,
                                                                           :params {:hidden false,
                                                                                    :info-text {:label {:en "",
                                                                                                        :fi "** HUOM! Alla olevat vastausvaihtoehdot voivat näkyä harmaana. Tässä tilanteessa poista pohjakoulutusvaihtoehto \"Muualla kuin Suomessa suoritettu korkeakoulututkinto\" ja ole yhteydessä hakemasi korkeakoulun hakijapalveluihin. Tällöin voit lähettää muut muutoksesi ja pohjakoulutustietosi voidaan täydentää virkailijoiden toimesta myöhemmin.**",
                                                                                                        :sv "**OBS! Svarsalternativen nedan kan vara gråa, vilket betyder att du inte kan svara på frågan. I sådant fall ta bort ditt grundutbildningsalternativ ”Högskoleexamen som avlagts annanstans än i Finland” och kontakta den högskola som du söker till. Då kan du fylla i dina övriga ändringar och högskolan kan komplettera uppgifterna om din utbildningsbakgrund senare.**"}}},
                                                                           :validators ["required"],
                                                                           :fieldClass "formField",
                                                                           :label {:en "Are your attachments in Finnish, Swedish or English?",
                                                                                   :fi "Ovatko liitteesi suomen-, ruotsin- tai englanninkielisiä?",
                                                                                   :sv "Är dina bilagor finsk-, svensk-, eller engelskspråkiga?"},
                                                                           :options [{:label {:en "Yes",
                                                                                              :fi "Kyllä",
                                                                                              :sv "Ja"},
                                                                                      :value "0"}
                                                                                     {:label {:en "No",
                                                                                              :fi "Ei",
                                                                                              :sv "Nej"},
                                                                                      :value "1",
                                                                                      :followups [(seven-day-attachment-followup {:en "Official translation of the certificate to Finnish, Swedish or English",
                                                                                                                                  :fi "Virallinen käännös suomeksi, ruotsiksi tai englanniksi",
                                                                                                                                  :sv "Officiell översättning av intyget till finska, svenska eller engelska"})]}],
                                                                           :fieldType "singleChoice"}]}],
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["numeric" "required"]}
                                                  {:label {:en "Degree", :fi "Tutkinto", :sv "Examen"},
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["required"]}
                                                  {:koodisto-source {:uri "kktutkinnot",
                                                                     :title "Kk-tutkinnot",
                                                                     :version 1,
                                                                     :allow-invalid? false},
                                                   :koodisto-ordered-by-user true,
                                                   :validators ["required"],
                                                   :fieldClass "formField",
                                                   :label {:en "Degree level", :fi "Tutkintotaso", :sv "Examensnivå"},
                                                   :options [],
                                                   :fieldType "dropdown"}
                                                  {:label {:en "Higher education institution",
                                                           :fi "Korkeakoulu",
                                                           :sv "Högskola"},
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["required"]}
                                                  {:koodisto-source {:uri "maatjavaltiot2",
                                                                     :title "Maat ja valtiot",
                                                                     :version 2,
                                                                     :allow-invalid? true},
                                                   :validators ["required"],
                                                   :fieldClass "formField",
                                                   :label {:en "Country of completion",
                                                           :fi "Suoritusmaa",
                                                           :sv "Land där examen är avlagd"},
                                                   :options [],
                                                   :fieldType "dropdown"}
                                                  {:text {:fi ""},
                                                   :label {:en "Click add if you want to add further qualifications.",
                                                           :fi "Paina lisää, jos haluat lisätä useampia tutkintoja.",
                                                           :sv "Tryck på lägg till om du vill lägga till flera examina."},
                                                   :fieldType "p",
                                                   :fieldClass "infoElement"}],
                                       :fieldType "fieldset",
                                       :fieldClass "questionGroup"}]}
                         {:label {:en "Open university/UAS studies required by the higher education institution",
                                  :fi "Korkeakoulun edellyttämät avoimen korkeakoulun opinnot",
                                  :sv "Studier som högskolan kräver vid en öppen högskola"},
                          :value "pohjakoulutus_avoin",
                          :followups [{:label {:fi "", :sv ""},
                                       :children [{:label {:en "Year of completion", :fi "Suoritusvuosi", :sv "Avlagd år"},
                                                   :params {:size "S",
                                                            :numeric true,
                                                            :max-value "2022",
                                                            :min-value "1900"},
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["required" "numeric"]}
                                                  {:label {:en "Study field", :fi "Ala", :sv "Bransch"},
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["required"]}
                                                  {:label {:en "Higher education institution",
                                                           :fi "Korkeakoulu",
                                                           :sv "Högskola"},
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["required"]}
                                                  {:label {:en "Study module",
                                                           :fi "Opintokokonaisuus",
                                                           :sv "Studiehelhet"},
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["required"]}
                                                  {:label {:en "Scope of studies", :fi "Laajuus", :sv "Omfattning"},
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["required"]}
                                                  {:params {:hidden false,
                                                            :deadline nil,
                                                            :info-text {:value {:en "The attachment has to be submitted within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.

                                                                                   Name the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.

                                                                                   Scan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright.

                                                                                   Recommended file formats are: PDF, JPG, PNG and DOCX.
                                                                                   ",
                                                                                :fi "Liite täytyy tallentaa viimeistään 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.

                                                                                   Nimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus

                                                                                   Skannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.

                                                                                   Suositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.
                                                                                   ",
                                                                                :sv "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått.Den angivna tidpunkten syns invid begäran om bilagor.

                                                                                   Namnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg

                                                                                   Skanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.
                                                                                   Samla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.
                                                                                   Kontrollera att dokumenten i filen är rättvända.\t

                                                                                   Rekommenderade filformat är PDF, JPG, PNG och DOCX.
                                                                                   "},
                                                                        :enabled? true}},
                                                   :fieldClass "formField",
                                                   :label {:en "Open university / university of applied sciences studies",
                                                           :fi "Todistus avoimen korkeakoulun opinnoista",
                                                           :sv "Studier inom den öppna högskolan"},
                                                   :options [],
                                                   :fieldType "attachment"}
                                                  {:text {:fi ""},
                                                   :label {:en "Click add if you want to add further qualifications.",
                                                           :fi "Paina lisää, jos haluat lisätä useampia opintokokonaisuuksia.",
                                                           :sv "Tryck på lägg till om du vill lägga till flera studiehelheter."},
                                                   :fieldType "p",
                                                   :fieldClass "infoElement"}],
                                       :fieldType "fieldset",
                                       :fieldClass "questionGroup"}]}
                         {:label {:en "Other eligibility for higher education",
                                  :fi "Muu korkeakoulukelpoisuus",
                                  :sv "Övrig högskolebehörighet"},
                          :value "pohjakoulutus_muu",
                          :followups [{:label {:fi "", :sv ""},
                                       :children [{:label {:en "Year of completion", :fi "Suoritusvuosi", :sv "Avlagd år"},
                                                   :params {:numeric true, :max-value "2022", :min-value "1900"},
                                                   :fieldType "textField",
                                                   :fieldClass "formField",
                                                   :validators ["numeric" "required"]}
                                                  {:label {:en "Description of your other eligibility",
                                                           :fi "Kelpoisuuden kuvaus",
                                                           :sv "Beskrivning av behörigheten"},
                                                   :params {:max-length "500"},
                                                   :fieldType "textArea",
                                                   :fieldClass "formField",
                                                   :validators ["required"]}
                                                  (seven-day-attachment-followup {:en "Other eligibility for higher education",
                                                                                  :fi "Todistus muusta korkeakoulukelpoisuudesta",
                                                                                  :sv "Övrig högskolebehörighet"})
                                                  {:text {:fi ""},
                                                   :label {:en "Click add if you want to add further qualifications.",
                                                           :fi "Paina lisää, jos haluat lisätä useampia kokonaisuuksia.",
                                                           :sv "Tryck på lägg till om du vill lägga till flera helheter."},
                                                   :fieldType "p",
                                                   :fieldClass "infoElement"}],
                                       :fieldType "fieldset",
                                       :fieldClass "questionGroup"}]}],
               :fieldType "multipleChoice"}
              {:label {:en "Have you completed general upper secondary education or a vocational qualification?",
                       :fi "Oletko suorittanut lukion/ylioppilastutkinnon tai ammatillisen tutkinnon?",
                       :sv "Har du avlagt gymnasiet/studentexamen eller yrkesinriktad examen?"},
               :params {:info-text {:label {:en "This is required for statistical reasons",
                                            :fi "Tämä tieto kysytään tilastointia varten.",
                                            :sv "Denna uppgift frågas för statistik."}}},
               :options [{:label {:en "Yes", :fi "Kyllä", :sv "Ja"},
                          :value "0",
                          :followups [{:params {:info-text {:label {:en "Choose the country where you have completed your most recent qualification. If you have not yet completed a general upper secondary school syllabus/matriculation examination or vocational qualification but are in the process of doing so please choose the country where you will complete the qualification. NB: a vocational qualification can be a vocational upper secondary qualification, school-level qualification, post-secondary level qualification, higher vocational level qualification, further vocational qualification or specialist vocational qualification. Do not fill in the country where you have completed a higher education qualification.",
                                                                    :fi "Merkitse viimeisimmän tutkintosi suoritusmaa. Jos sinulla ei ole vielä lukion päättötodistusta/ylioppilastutkintoa tai ammatillista tutkintoa, mutta olet suorittamassa sellaista, valitse se maa, jossa parhaillaan suoritat kyseistä tutkintoa. Huom: ammatillinen tutkinto voi olla ammatillinen perustutkinto, kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto, ammatti- tai erikoisammattitutkinto. Älä merkitse tähän korkeakoulututkinnon suoritusmaata.",
                                                                    :sv "Ange land där din senaste examen avlagts. Om du ännu inte har avlagt gymnasiet/studentexamen eller yrkesinriktad examen men håller på att göra det välj då det land där du som bäst avlägger examen i fråga. Obs: yrkesinriktad examen kan vara yrkesinriktad grundexamen, examen på skolnivå, examen på institutsnivå, yrkesinriktad examen på högre nivå, yrkesexamen eller specialyrkesexamen. Ange inte här landet där du har avlagt högskoleexamen."}}},
                                       :koodisto-source {:uri "maatjavaltiot2",
                                                         :title "Maat ja valtiot",
                                                         :version 2,
                                                         :allow-invalid? true},
                                       :validators ["required"],
                                       :fieldClass "formField",
                                       :label {:en "Country of completion",
                                               :fi "Suoritusmaa",
                                               :sv "Land där du avlagt examen"},
                                       :options [],
                                       :fieldType "dropdown"}]}
                         {:label {:en "No", :fi "En", :sv "Nej"}, :value "1"}],
               :fieldType "singleChoice",
               :fieldClass "formField",
               :validators ["required"]}
              {:label {:en "Have you completed a university or university of applied sciences ( prev. polytechnic) degree in Finland before 2003?",
                       :fi "Oletko suorittanut suomalaisen ammattikorkeakoulu- tai yliopistotutkinnon ennen vuotta 2003?",
                       :sv "Har du avlagt en finländsk yrkeshögskole- eller universitetsexamen före år 2003?"},
               :params {:info-text {:label {:en "Write your university or university of applied sciences degree only if you have completed it before 2003. After that date the information of completed degrees will be received automatically from the higher education institutions. If you have completed a university/university of applied sciences degree or have received a study place in higher education in Finland after autumn 2014 your admission can be affected. More information on [the quota for first-time applicants](https://opintopolku.fi/konfo/en/sivu/provisions-and-restrictions-regarding-student-admissions-to-higher-education#quota-for-first-time-applicants).",
                                            :fi "Merkitse tähän suorittamasi korkeakoulututkinto vain siinä tapauksessa, jos olet suorittanut sen ennen vuotta 2003. Sen jälkeen suoritetut tutkinnot saadaan automaattisesti korkeakouluilta. Suomessa suoritettu korkeakoulututkinto tai syksyllä 2014 tai sen jälkeen alkaneesta koulutuksesta vastaanotettu korkeakoulututkintoon johtava opiskelupaikka voivat vaikuttaa valintaan. Lue lisää [ensikertalaiskiintiöstä](https://opintopolku.fi/konfo/fi/sivu/valmistaudu-korkeakoulujen-yhteishakuun#ensikertalaiskiinti).",
                                            :sv "Ange här den högskoleexamen som du avlagt före år 2003. Examina som avlagts efter detta fås automatiskt av högskolorna. En högskoleexamen som avlagts i Finland eller en studieplats inom utbildning som leder till högskoleexamen som mottagits år 2014 eller senare kan inverka på antagningen. Läs mera om [kvoten för förstagångssökande](https://opintopolku.fi/konfo/sv/sivu/forbered-dig-for-gemensam-ansokan-till-hogskolor#kvot-fr-frstagangsskande)."}}},
               :options [{:label {:en "Yes", :fi "Kyllä", :sv "Ja"},
                          :value "0",
                          :followups [{:label {:en "Year of completion", :fi "Suoritusvuosi", :sv "Avlagd år"},
                                       :params {:size "S",
                                                :numeric true,
                                                :decimals nil,
                                                :max-value "2002",
                                                :min-value "1900"},
                                       :fieldType "textField",
                                       :fieldClass "formField",
                                       :validators ["numeric" "required"]}
                                      {:koodisto-source {:uri "tutkinto",
                                                         :title "Tutkinto",
                                                         :version 2,
                                                         :allow-invalid? true},
                                       :validators ["required"],
                                       :fieldClass "formField",
                                       :label {:en "Name of the degree", :fi "Tutkinnon nimi", :sv "Examens namn"},
                                       :options [],
                                       :fieldType "dropdown"}
                                      {:label {:en "Higher education institution", :fi "Korkeakoulu", :sv "Högskola"},
                                       :fieldType "textField",
                                       :fieldClass "formField",
                                       :validators ["required"]}]}
                         {:label {:en "No", :fi "En", :sv "Nej"}, :value "1"}],
               :fieldType "singleChoice",
               :fieldClass "formField",
               :validators ["required"]}],
   :fieldType "fieldset",
   :fieldClass "wrapperElement"}
  )
