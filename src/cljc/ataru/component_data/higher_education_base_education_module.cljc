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

(def yo-arvosana-options
  [{:label {:en "L" :fi "L" :sv "L"} :value "0"}
   {:label {:en "E" :fi "E" :sv "E"} :value "1"}
   {:label {:en "M" :fi "M" :sv "M"} :value "2"}
   {:label {:en "C" :fi "C" :sv "C"} :value "3"}
   {:label {:en "B" :fi "B" :sv "B"} :value "4"}
   {:label {:en "A" :fi "A" :sv "A"} :value "5"}
   {:label {:en "No grade"
            :fi "Ei arvosanaa"
            :sv "Inget vitsord"}
    :value "6"}])

(defn- arvosana-aidinkieli [id]
  {:fieldClass "formField"
   :fieldType  "dropdown"
   :id         id
   :label      {:en "Mother tongue"
                :fi "Äidinkieli"
                :sv "Modersmålet"}
   :options    yo-arvosana-options
   :params     {}
   :validators ["required"]})

(defn- arvosana-matematiikka-pitka [id]
  {:fieldClass "formField"
   :fieldType  "dropdown"
   :id         id
   :label      {:en "Mathematics, advanced syllabus"
                :fi "Matematiikka, pitkä"
                :sv "Matematik, lång"}
   :options    yo-arvosana-options
   :params     {}
   :validators ["required"]})

(defn- arvosana-matematiikka-lyhyt [id]
  {:fieldClass "formField"
   :fieldType  "dropdown"
   :id         id
   :label      {:en "Mathematics, basic syllabus"
                :fi "Matematiikka, lyhyt"
                :sv "Matematik, kort "}
   :options    yo-arvosana-options
   :params     {}
   :validators ["required"]})

(defn- arvosana-kieli-pitka [id]
  {:fieldClass "formField"
   :fieldType  "dropdown"
   :id         id
   :label      {:en "Best advanced language syllabus "
                :fi "Paras kieli, pitkä"
                :sv "Bästa språk, långt "}
   :options    yo-arvosana-options
   :params     {}
   :validators ["required"]})

(defn- arvosana-kieli-lyhyt [id]
  {:fieldClass "formField"
   :fieldType  "dropdown"
   :id         id
   :label      {:en "Best basic/intermediate language syllabus"
                :fi "Paras kieli, lyhyt/keskipitkä"
                :sv "Bästa språk, kort/mellanlångt"}
   :options    yo-arvosana-options
   :params     {}
   :validators ["required"]})

(defn- arvosana-reaali [id]
  {:fieldClass "formField"
   :fieldType  "dropdown"
   :id         id
   :label      {:en "Best of general studies battery tests "
                :fi "Paras reaaliaineiden kokeista"
                :sv "Bästa realämnesprov"}
   :options    yo-arvosana-options
   :params     {}
   :validators ["required"]})

(defn- arvosana-fysiikka-kemia-biologia [id]
  {:fieldClass "formField"
   :fieldType  "dropdown"
   :id         id
   :label      {:en "General studies battery test in physics, chemistry or biology"
                :fi "Fysiikan, kemian tai biologian reaalikoe"
                :sv "Realprovet i fysik, kemi eller biologi"}
   :options    yo-arvosana-options
   :params     {}
   :validators ["required"]})

(defn- pohjakoulutus_yo_kansainvalinen_suomessa-arvosana-info [id]
  {:fieldClass "infoElement"
   :fieldType  "p"
   :id         id
   :label      {:en "Fill in the grades of your international matriculation examination "
                :fi "Täytä kansainvälisen ylioppilastutkintosi arvosanat"
                :sv "Fyll i vitsorden från din internationella studentexamen"}
   :params     {}
   :text       {:en "In the drop-down menu are the grades of the Finnish matriculation examination. First you need to convert your grades with the [conversion chart](https://studyinfo.fi/wp2/en/higher-education/applying/conversion-chart-for-eb-ib-and-reifeprufung-examinations/). If you do not have a grade in the subject, choose 'No grade'."
                :fi "Alasvetovalikoissa on suomalaisen ylioppilastutkinnon arvosanat. Muunna ensin todistuksesi arvosanat [muuntotaulukon](https://opintopolku.fi/wp/ammattikorkeakoulu/miten-opiskelijat-valitaan/ammattikorkeakoulujen-valintaperustesuositukset-2019/#EB-,%20IB-%20ja%20Reifeprufung-tutkintojen%20muuntokaava) avulla. Jos sinulla ei ole arvosanaa kyseisestä aineesta, valitse vaihtoehto 'Ei arvosanaa'."
                :sv "I rullgardinsmenyn finns vitsorden för den finländska studentexamen. Omvandla först vitsorden från ditt betyg med hjälp av [omvandlingsschemat](https://studieinfo.fi/wp/yrkeshogskola/hur-antas-studerande/rekommendation-om-antagningsgrunder-for-yrkeshogskolor-ar-2019/#Omvandlingsschema) . Om du inte har ett vitsord för något ämne, välj alternativet 'Inget vitsord'."}})

(defn- liite-hakuaika-deadline [id label]
  {:fieldClass "formField"
   :fieldType  "attachment"
   :id         id
   :label      label
   :options    []
   :params     {:deadline nil
                :info-text
                {:enabled? true
                 :value
                 {:en "The attachment has to be submitted within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n\n"
                  :fi "Liite täytyy tallentaa viimeistään 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n"
                  :sv "Bilagan ska sparas senast 7 dygn innan ansökningstiden går ut. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}}})

(defn- liite-ilmoitettu-deadline [id label]
  {:fieldClass "formField"
   :fieldType  "attachment"
   :id         id
   :label      label
   :options    []
   :params     {:deadline nil
                :info-text
                {:enabled? true
                 :value
                 {:en "The attachment has to be submitted by the deadline informed next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n"
                  :fi "Liitteen määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX."
                  :sv "Den angivna tidpunkten för bilagan syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}}})

(defn- liite-tutkintotodistus [id label]
  {:fieldClass "formField"
   :fieldType  "attachment"
   :id         id
   :label      label
   :options    []
   :params     {:deadline nil
                :info-text
                {:enabled? true
                 :value
                 {:en "The attachment has to be submitted within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n"
                  :fi "Liite täytyy tallentaa viimeistään 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\nTallenna hakemuksen liitteeksi päättötodistus tai tutkintotodistus. Huomaathan, että pelkkä opintosuoritusote ei riitä.\n"
                  :sv "Bilagan ska sparas senast 7 dygn innan ansökningstiden går ut. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}}})

(defn- liite-kk-tutkintotodistus [id label]
  {:fieldClass "formField"
   :fieldType  "attachment"
   :id         id
   :label      label
   :options    []
   :params     {:deadline nil
                :info-text
                {:enabled? true
                 :value
                 {:en "The attachment has to be submitted within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Surname\\_Given name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n"
                  :fi "Liite täytyy tallentaa viimeistään 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\nTallenna hakemuksen liitteeksi kopio tutkintotodistuksesta mahdollisine liitteineen.\n"
                  :sv "Bilagan ska sparas senast 7 dygn innan ansökningstiden går ut. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"}}}})



(defn- kansainvalinen-yo
  [type
   this-year-predicted-label
   this-year-diploma-label
   previous-year-diploma-label]
  [(pohjakoulutus_yo_kansainvalinen_suomessa-arvosana-info
    (str type "--arvosana-info"))
   (arvosana-aidinkieli
    (str type "--arvosana-aidinkieli"))
   (arvosana-matematiikka-pitka
    (str type "--arvosana-matematiikka-pitka"))
   (arvosana-matematiikka-lyhyt
    (str type "--arvosana-matematiikka-lyhyt"))
   (arvosana-kieli-pitka
    (str type "--arvosana-kieli-pitka"))
   (arvosana-kieli-lyhyt
    (str type "--arvosana-kieli-lyhyt"))
   (arvosana-reaali
    (str type "--arvosana-reaali"))
   (arvosana-fysiikka-kemia-biologia
    (str type "--arvosana-fysiikka-kemia-biologia"))
   {:fieldClass "formField"
    :fieldType  "singleChoice"
    :id         (str type "--year-of-completion-this-year")
    :label      {:en "Year of completion"
                 :fi "Suoritusvuosi"
                 :sv "Avlagd år"}
    :options    [{:followups [(liite-hakuaika-deadline
                               (str type "--this-year-predicted")
                               this-year-predicted-label)
                              (liite-ilmoitettu-deadline
                               (str type "--this-year-diploma")
                               this-year-diploma-label)]
                  :label     {:en "2019" :fi "2019" :sv "2019"}
                  :value     "0"}
                 {:followups [{:belongs-to-hakukohteet []
                               :fieldClass             "formField"
                               :fieldType              "textField"
                               :id                     (str type "--year-of-completion")
                               :label                  {:en "Year of completion"
                                                        :fi "Suoritusvuosi"
                                                        :sv "Avlagd år"}
                               :params                 {:numeric true :size "S"}
                               :validators             ["required" "numeric"]}
                              (liite-hakuaika-deadline
                               (str type "--previous-year-diploma")
                               previous-year-diploma-label)]
                  :label     {:en "Before 2019"
                              :fi "Ennen vuotta 2019"
                              :sv "Före 2019"}
                  :value     "1"}]
    :params     {}
    :validators ["required"]}])

(def pohjakoulutus_yo_kansainvalinen_suomessa
  {:followups [{:belongs-to-hakukohderyhma []
                :belongs-to-hakukohteet    []
                :fieldClass                "formField"
                :fieldType                 "dropdown"
                :id                        "pohjakoulutus_yo_kansainvalinen_suomessa--exam-type"
                :label                     {:en "Matriculation examination"
                                            :fi "Ylioppilastutkinto"
                                            :sv "Studentexamen"}
                :options                   [{:followups (kansainvalinen-yo
                                                         "pohjakoulutus_yo_kansainvalinen_suomessa--ib"
                                                         {:en "Predicted grades"
                                                          :fi "Oppilaitoksen myöntämä ennakkoarvio arvosanoista (Candidate Predicted Grades)"
                                                          :sv "Predicted grades"}
                                                         {:en "Diploma Programme (DP) Results"
                                                          :fi "Diploma Programme (DP) Results -dokumentti"
                                                          :sv "Diploma Programme (DP) Results"}
                                                         {:en "Diploma"
                                                          :fi "IB Diploma -tutkintotodistus"
                                                          :sv "Diploma"})
                                             :label     {:en "International Baccalaureate -diploma"
                                                         :fi "International Baccalaureate"
                                                         :sv "International Baccalaureate -examen"}
                                             :value     "0"}
                                            {:followups (kansainvalinen-yo
                                                         "pohjakoulutus_yo_kansainvalinen_suomessa--eb"
                                                         {:en "Predicted grades"
                                                          :fi "Oppilaitoksen myöntämä ennakkoarvio arvosanoista"
                                                          :sv "Läroanstaltens preliminära vitsord"}
                                                         {:en "European Baccalaureate certificate"
                                                          :fi "European Baccalaureate Certificate -tutkintotodistus"
                                                          :sv "European Baccalaureate certificate"}
                                                         {:en "European Baccalaureate certificate"
                                                          :fi "European Baccalaureate Certificate -tutkintotodistus"
                                                          :sv "European Baccalaureate certificate"})
                                             :label     {:en "European Baccalaureate -diploma"
                                                         :fi "Eurooppalainen ylioppilastutkinto"
                                                         :sv "European Baccalaureate -examen"}
                                             :value     "1"}
                                            {:followups (kansainvalinen-yo
                                                         "pohjakoulutus_yo_kansainvalinen_suomessa--rb"
                                                         {:en "Certificate of grades"
                                                          :fi "Oppilaitoksen myöntämä todistus arvosanoista"
                                                          :sv "Läroanstaltens betyg över vitsord"}
                                                         {:en "Reifeprüfung/DIA -diploma"
                                                          :fi "Reifeprüfung/DIA-tutkintotodistus"
                                                          :sv "Reifeprüfung/DIA -examensbetyg"}
                                                         {:en "Reifeprüfung/DIA -diploma"
                                                          :fi "Reifeprüfung/DIA-tutkintotodistus"
                                                          :sv "Reifeprüfung/DIA -examensbetyg"})
                                             :label     {:en "Reifeprüfung - diploma/ Deutsche Internationale Abiturprüfung"
                                                         :fi "Reifeprüfung/Deutsche Internationale Abiturprüfung"
                                                         :sv "Reifeprüfung - examen/Deutsche Internationale Abiturprüfung"}
                                             :value     "2"}]
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
   :label     {:en "International matriculation examination completed in Finland (IB, EB and RP/DIA)"
               :fi "Suomessa suoritettu kansainvälinen ylioppilastutkinto (IB, EB ja RP/DIA)"
               :sv "Internationell studentexamen som avlagts i Finland (IB, EB och RP/DIA)"}
   :value     "pohjakoulutus_yo_kansainvalinen_suomessa"})

(def pohjakoulutus_yo_ulkomainen
  {:followups [{:fieldClass "formField"
                :fieldType  "dropdown"
                :id         "pohjakoulutus_yo_ulkomainen--exam-type"
                :label      {:en "Name of examination/diploma"
                             :fi "Ylioppilastutkinto"
                             :sv "Studentexamen"}
                :options    [{:followups (kansainvalinen-yo
                                          "pohjakoulutus_yo_ulkomainen--ib"
                                          {:en "Predicted grades"
                                           :fi "Oppilaitoksen myöntämä ennakkoarvio arvosanoista (Candidate Predicted Grades)"
                                           :sv "Predicted grades"}
                                          {:en "Diploma Programme (DP) Results"
                                           :fi "Diploma Programme (DP) Results -dokumentti"
                                           :sv "Diploma Programme (DP) Results"}
                                          {:en "Diploma"
                                           :fi "IB Diploma -tutkintotodistus"
                                           :sv "Diploma"})
                              :label     {:en "International Baccalaureate -diploma"
                                          :fi "International Baccalaureate"
                                          :sv "International Baccalaureate -examen"}
                              :value     "0"}
                             {:followups (kansainvalinen-yo
                                          "pohjakoulutus_yo_ulkomainen--eb"
                                          {:en "Predicted grades"
                                           :fi "Oppilaitoksen myöntämä ennakkoarvio arvosanoista"
                                           :sv "Läroanstaltens preliminära vitsord"}
                                          {:en "European Baccalaureate certificate"
                                           :fi "European Baccalaureate Certificate -tutkintotodistus"
                                           :sv "European Baccalaureate certificate"}
                                          {:en "European Baccalaureate certificate"
                                           :fi "European Baccalaureate Certificate -tutkintotodistus"
                                           :sv "European Baccalaureate certificate"})
                              :label     {:en "European Baccalaureate -diploma"
                                          :fi "Eurooppalainen ylioppilastutkinto"
                                          :sv "European Baccalaureate -examen"}
                              :value     "1"}
                             {:followups (kansainvalinen-yo
                                          "pohjakoulutus_yo_ulkomainen--rb"
                                          {:en "Certificate of grades"
                                           :fi "Oppilaitoksen myöntämä todistus arvosanoista"
                                           :sv "Läroanstaltens betyg över vitsord"}
                                          {:en "Reifeprüfung/DIA -diploma"
                                           :fi "Reifeprüfung/DIA-tutkintotodistus"
                                           :sv "Reifeprüfung/DIA -examensbetyg"}
                                          {:en "Reifeprüfung/DIA -diploma"
                                           :fi "Reifeprüfung/DIA-tutkintotodistus"
                                           :sv "Reifeprüfung/DIA -examensbetyg"})
                              :label     {:en "Reifeprüfung - diploma/ Deutsche Internationale Abiturprüfung"
                                          :fi "Reifeprüfung/Deutsche Internationale Abiturprüfung"
                                          :sv "Reifeprüfung - examen/Deutsche Internationale Abiturprüfung"}
                              :value     "2"}]
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
               {:fieldClass      "formField"
                :fieldType       "dropdown"
                :id              "pohjakoulutus_yo_ulkomainen--country-of-completion"
                :koodisto-source {:title "Maat ja valtiot" :uri "maatjavaltiot2" :version 1}
                :label           {:en "Country of completion"
                                  :fi "Suoritusmaa"
                                  :sv "Land där examen är avlagd"}
                :options         []
                :params          {}
                :validators      ["required"]}]
   :label     {:en "International matriculation examination completed outside Finland (IB, EB and RP/DIA)"
               :fi "Muualla kuin Suomessa suoritettu kansainvälinen ylioppilastutkinto (IB, EB ja RP/DIA)"
               :sv "Internationell studentexamen som avlagts annanstans än i Finland (IB, EB och RP/DIA)"}
   :value     "pohjakoulutus_yo_ulkomainen"})

(def higher-education-module-options
  [{:followups [{:belongs-to-hakukohteet []
                 :fieldClass             "formField"
                 :fieldType              "singleChoice"
                 :id                     "pohjakoulutus_yo"
                 :label                  {:en "Have you completed your Matriculation examination in Finland in 1990 or after?"
                                          :fi "Oletko suorittanut ylioppilastutkinnon vuonna 1990 tai sen jälkeen?"
                                          :sv "Har du avlagt studentexamen år 1990 eller senare?"}
                 :options                [{:followups [{:belongs-to-hakukohteet []
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
                                                        :text                   {:en "Your matriculation examination details are received automatically from the Matriculation Examination Board."
                                                                                 :fi "Saamme ylioppilastutkintosi tiedot rekisteristämme."
                                                                                 :sv "Vi får uppgifterna om din studentexamen ur vårt register."}}]
                                           :label     {:en "Yes" :fi "Kyllä" :sv "Ja"}
                                           :value     "0"}
                                          {:followups [{:belongs-to-hakukohteet []
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
                                                       (liite-hakuaika-deadline
                                                        "pohjakoulutus-yo--attachment"
                                                        {:en "Matriculation examination (completed before 1990)"
                                                         :fi "Ylioppilastutkintotodistus (ennen vuotta 1990)"
                                                         :sv "Studentexamen (före år 1990)"})]
                                           :label     {:en "No" :fi "Ei" :sv "Nej"}
                                           :value     "1"}]
                 :params                 {}
                 :validators             ["required"]}]
    :label     {:en "Matriculation examination completed in Finland"
                :fi "Suomessa suoritettu ylioppilastutkinto"
                :sv "Studentexamen som avlagts i Finland"}
    :value     "pohjakoulutus_yo"}
   {:followups [{:belongs-to-hakukohteet []
                 :fieldClass             "formField"
                 :fieldType              "textField"
                 :id                     "pohjakoulutus_lk--year-of-completion"
                 :label                  {:en "Year of completion" :fi "Suoritusvuosi" :sv "Avlagd år"}
                 :params                 {:numeric true :size "S"}
                 :validators             ["required" "numeric"]}
                (liite-hakuaika-deadline
                 "pohjakoulutus_lk--attachment"
                 {:en "High school diploma"
                  :fi "Lukion päättötodistus"
                  :sv ""})]
    :label     {:en "General upper secondary school syllabus completed in Finland (without matriculation examination)"
                :fi "Suomessa suoritettu lukion oppimäärä ilman ylioppilastutkintoa"
                :sv "Gymnasiets lärokurs som avlagts i Finland utan studentexamen"}
    :value     "pohjakoulutus_lk"}
   pohjakoulutus_yo_kansainvalinen_suomessa
   {:followups [{:fieldClass "formField"
                 :fieldType  "textField"
                 :id         "pohjakoulutus_yo_ammatillinen--marticulation-year-of-completion"
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
                 :options    [{:label {:en "Courses" :fi "Kurssia" :sv "Kurser"}
                               :value "0"}
                              {:label {:en "ECTS credits" :fi "Opintopistettä" :sv "Studiepoäng"}
                               :value "1"}
                              {:label {:en "Study weeks" :fi "Opintoviikkoa" :sv "Studieveckor"}
                               :value "2"}
                              {:label {:en "Competence points"
                                       :fi "Osaamispistettä"
                                       :sv "Kompetenspoäng"}
                               :value "3"}
                              {:label {:en "Hours" :fi "Tuntia" :sv "Timmar"}
                               :value "4"}
                              {:label {:en "Weekly lessons per year"
                                       :fi "Vuosiviikkotuntia"
                                       :sv "Årsveckotimmar"}
                               :value "5"}
                              {:label {:en "Years" :fi "Vuotta" :sv "År"}
                               :value "6"}]
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
                (liite-hakuaika-deadline
                 "pohjakoulutus_yo_ammatillinen--attachment"
                 {:en "Vocational qualification"
                  :fi "Ammatillisen perustutkinnon tutkintotodistus"
                  :sv "Yrkesinriktad grundexamen examen på skolnivå examen på institutnivå eller examen på yrkesinriktad högre nivå"})]
    :label     {:en "Double degree (secondary level) completed in Finland"
                :fi "Suomessa suoritettu ammatillinen perustutkinto ja ylioppilastutkinto (kaksoistutkinto)"
                :sv "Dubbelexamen som avlagts i Finland"}
    :value     "pohjakoulutus_yo_ammatillinen"}
   {:followups [{:children   [{:fieldClass "formField"
                               :fieldType  "textField"
                               :id         "pohjakoulutus_am--year-of-completion"
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
                               :options    [{:label {:en "Courses" :fi "Kurssia" :sv "Kurser"}
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
                                            {:label {:en "Hours" :fi "Tuntia" :sv "Timmar"}
                                             :value "4"}
                                            {:label {:en "Weekly lessons per year"
                                                     :fi "Vuosiviikkotuntia"
                                                     :sv "Årsveckotimmar"}
                                             :value "5"}
                                            {:label {:en "Years" :fi "Vuotta" :sv "År"}
                                             :value "6"}]
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
                               :label      {:en "Have you completed your qualification as a competence based qualification in its entiretity?"
                                            :fi "Oletko suorittanut tutkinnon kokonaan näyttötutkintona?"
                                            :sv "Har du avlagt examen som fristående yrkesexamen?"}
                               :options    [{:label {:en "Yes" :fi "Kyllä" :sv "Ja"} :value "0"}
                                            {:label {:en "No" :fi "Ei" :sv "Nej"} :value "1"}]
                               :params     {}
                               :validators ["required"]}
                              (liite-tutkintotodistus
                               "pohjakoulutus_am--attachment"
                               {:en "Vocational qualification"
                                :fi "Ammatillisen perustutkinnon, kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkintotodistus"
                                :sv "Yrkesinriktad grundexamen examen på skolnivå examen på institutnivå eller examen på yrkesinriktad högre nivå"})
                              {:fieldClass "infoElement"
                               :fieldType  "p"
                               :id         "hbem--d252db76-8383-47f8-b723-e0fbe43354f7"
                               :label      {:en "Click add if you want add further qualifications."
                                            :fi "Paina lisää jos haluat lisätä useampia tutkintoja."
                                            :sv "Tryck på lägg till om du vill lägga till flera examina."}
                               :params     {}
                               :text       {:fi ""}}]
                 :fieldClass "questionGroup"
                 :fieldType  "fieldset"
                 :id         "hbem--c8bd423a-b468-4e52-b2fb-3e23164f56ff"
                 :label      {:fi "" :sv ""}
                 :params     {}}]
    :label     {:en "Vocational upper secondary qualification, school-level qualification, post-secondary level qualification or higher vocational level qualification completed in Finland"
                :fi "Suomessa suoritettu ammatillinen perustutkinto, kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto"
                :sv "Yrkesinriktad grundexamen, examen på skolnivå, examen på institutsnivå eller yrkesinriktad examen på högre nivå som avlagts i Finland"}
    :value     "pohjakoulutus_am"}
   {:followups [{:children   [{:fieldClass "formField"
                               :fieldType  "textField"
                               :id         "pohjakoulutus_amt--year-of-completion"
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
                               :options    [{:label {:en "Courses" :fi "Kurssia" :sv "Kurser"}
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
                                            {:label {:en "Hours" :fi "Tuntia" :sv "Timmar"}
                                             :value "4"}
                                            {:label {:en "Weekly lessons per year"
                                                     :fi "Vuosiviikkotuntia"
                                                     :sv "Årsveckotimmar"}
                                             :value "5"}
                                            {:label {:en "Years" :fi "Vuotta" :sv "År"}
                                             :value "6"}]
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
                              (liite-hakuaika-deadline
                               "pohjakoulutus_amt--attachment"
                               {:en "Vocational or specialist vocational qualification"
                                :fi "Ammatti- tai erikoisammattitutkintotodistus"
                                :sv "En yrkesexamen eller en specialyrkesexamen"})
                              {:fieldClass "infoElement"
                               :fieldType  "p"
                               :id         "hbem--6d905522-f5c6-4cb6-a8cc-86199f41b520"
                               :label      {:en "Click add if you want add further qualifications."
                                            :fi "Paina lisää, jos haluat lisätä useampia tutkintoja."
                                            :sv "Tryck på lägg till om du vill lägga till flera examina."}
                               :params     {}
                               :text       {:fi ""}}]
                 :fieldClass "questionGroup"
                 :fieldType  "fieldset"
                 :id         "hbem--c0e5bedd-a5dc-44df-a760-efa382e12545"
                 :label      {:fi "" :sv ""}
                 :params     {}}]
    :label     {:en "Further vocational qualification or specialist vocational qualification completed in Finland"
                :fi "Suomessa suoritettu ammatti- tai erikoisammattitutkinto"
                :sv "Yrkesexamen eller specialyrkesexamen som avlagts i Finland"}
    :value     "pohjakoulutus_amt"}
   {:followups [{:children   [{:belongs-to-hakukohteet []
                               :fieldClass             "formField"
                               :fieldType              "textField"
                               :id                     "pohjakoulutus_kk--completion-date"
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
                               :koodisto-source        {:title "Kk-tutkinnot" :uri "kktutkinnot" :version 1}
                               :label                  {:en "Degree level" :fi "Tutkintotaso " :sv "Examensnivå"}
                               :options                []
                               :params                 {}}
                              {:fieldClass "formField"
                               :fieldType  "textField"
                               :id         "hbem--8c5055b4-b0fb-4ead-bfff-14bbfef01bf0"
                               :label      {:en "Higher education institution"
                                            :fi "Korkeakoulu"
                                            :sv "Högskola"}
                               :params     {}
                               :validators ["required"]}
                              (liite-kk-tutkintotodistus
                               "pohjakoulutus_kk--attachment"
                               {:en "Higher education degree"
                                :fi "Korkeakoulututkinnon tutkintotodistus"
                                :sv "Högskoleexamen"})
                              {:fieldClass "infoElement"
                               :fieldType  "p"
                               :id         "hbem--02d9dbe9-c6a0-420a-8f90-e686d571ef40"
                               :label      {:en "Click add if you want add further qualifications."
                                            :fi "Paina lisää jos haluat lisätä useampia tutkintoja."
                                            :sv "Tryck på lägg till om du vill lägga till flera examina."}
                               :params     {}
                               :text       {:fi ""}}]
                 :fieldClass "questionGroup"
                 :fieldType  "fieldset"
                 :id         "hbem--3c89eb8d-46be-4a0e-b7d8-f5d8dab0fdff"
                 :label      {:fi "" :sv ""}
                 :params     {}}]
    :label     {:en "Higher education qualification completed in Finland"
                :fi "Suomessa suoritettu korkeakoulututkinto"
                :sv "Högskoleexamen som avlagts i Finland"}
    :value     "pohjakoulutus_kk"}
   pohjakoulutus_yo_ulkomainen
   {:followups [{:children   [{:belongs-to-hakukohderyhma []
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
                               :koodisto-source        {:title "Maat ja valtiot" :uri "maatjavaltiot2" :version 1}
                               :label                  {:en "Country of completion"
                                                        :fi "Suoritusmaa"
                                                        :sv "Land där examen är avlagd"}
                               :options                []
                               :params                 {}
                               :validators             ["required"]}
                              (liite-hakuaika-deadline
                               "pohjakoulutus_ulk--attachment"
                               {:en "Education that provides eligibility for higher education in the awarding country"
                                :fi "Tutkintotodistus muualla kuin Suomessa suoritetusta tutkinnosta, joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun"
                                :sv "Examen som avlagts annanstans än i Finland och som i landet ifråga ger ansökningsbehörighet för högskola. "})]
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
                 :text       {:en "Click add if you want add further qualifications."
                              :fi "Paina lisää jos haluat lisätä useampia tutkintoja."
                              :sv "Tryck på lägg till om du vill lägga till flera examina."}}]
    :label     {:en "Other qualification completed outside Finland that provides eligibility to apply for higher education in the country in question"
                :fi "Muualla kuin Suomessa suoritettu muu tutkinto, joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun"
                :sv "Övrig examen som avlagts annanstans än i Finland, och ger behörighet för högskolestudier i ifrågavarande land"}
    :value     "pohjakoulutus_ulk"}
   {:followups [{:children   [{:belongs-to-hakukohteet []
                               :fieldClass             "formField"
                               :fieldType              "dropdown"
                               :id                     "pohjakoulutus_kk_ulk--level-of-degree"
                               :koodisto-source        {:title "Kk-tutkinnot" :uri "kktutkinnot" :version 1}
                               :label                  {:en "Level of degree" :fi "Tutkintotaso" :sv "Examensnivå"}
                               :options                []
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
                               :koodisto-source        {:title "Maat ja valtiot" :uri "maatjavaltiot2" :version 1}
                               :label                  {:en "Country of completion"
                                                        :fi "Suoritusmaa"
                                                        :sv "Land där examen är avlagd"}
                               :options                []
                               :params                 {}
                               :validators             ["required"]}
                              (liite-kk-tutkintotodistus
                               "pohjakoulutus_kk_ulk--attachement"
                               {:en "Higher education qualification completed outside Finland"
                                :fi "Muualla kuin Suomessa suoritetun korkeakoulututkinnon tutkintotodistus"
                                :sv "Högskoleexamen som avlagts annanstans än i Finland"})]
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
                 :text       {:en "Click add if you want add further qualifications."
                              :fi "Paina lisää jos haluat lisätä useampia tutkintoja."
                              :sv "Tryck på lägg till om du vill lägga till flera examina."}}]
    :label     {:en "Higher education qualification completed outside Finland"
                :fi "Muualla kuin Suomessa suoritettu korkeakoulututkinto"
                :sv "Högskoleexamen som avlagts annanstans än i Finland"}
    :value     "pohjakoulutus_kk_ulk"}
   {:followups [{:children   [{:fieldClass "formField"
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
                               :label      {:en "Scope of qualification" :fi "Laajuus" :sv "Omfattning"}
                               :params     {:numeric true :size "S"}
                               :validators ["required" "numeric"]}
                              (liite-hakuaika-deadline
                               "pohjakoulutus_avoin--attachment"
                               {:en "Open university / university of applied sciences studies"
                                :fi "Todistus avoimen korkeakoulun opinnoista"
                                :sv "Studier inom den öppna högskolan"})
                              {:fieldClass "infoElement"
                               :fieldType  "p"
                               :id         "hbem--162f8731-589c-4920-b1e5-b827d8df06a1"
                               :label      {:fi ""}
                               :params     {}
                               :text       {:en "Click add if you want add further qualifications."
                                            :fi "Paina lisää, jos haluat lisätä useampia opintokokonaisuuksia."
                                            :sv "Tryck på lägg till om du vill lägga till flera studiehelhet."}}]
                 :fieldClass "questionGroup"
                 :fieldType  "fieldset"
                 :id         "pohjakoulutus_avoin"
                 :label      {:en "" :fi "" :sv ""}
                 :params     {}}]
    :label     {:en "Studies required by the higher education institution completed at open university or open polytechnic/UAS"
                :fi "Korkeakoulun edellyttämät avoimen korkeakoulun opinnot"
                :sv "Studier som högskolan kräver vid en öppen högskola"}
    :value     "pohjakoulutus_avoin"}
   {:followups [{:fieldClass "formField"
                 :fieldType  "textField"
                 :id         "pohjakoulutus_muu--year-of-completion"
                 :label      {:en "Year of completion" :fi "Suoritusvuosi" :sv "Avlagd år"}
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
                (liite-hakuaika-deadline
                 "pohjakoulutus_muu--attachment"
                 {:en "Other eligibility for higher education"
                  :fi "Todistus muusta korkeakoulukelpoisuudesta"
                  :sv "Övrig högskolebehörighet"})]
    :label     {:en "Other eligibility for higher education"
                :fi "Muu korkeakoulukelpoisuus"
                :sv "Övrig högskolebehörighet"}
    :value     "pohjakoulutus_muu"}])

(defn module [metadata]
  (merge (form-section metadata)
         (clojure.walk/postwalk
          (fn [form]
            (if (or (:options form) (:id form))
              (assoc form :metadata metadata)
              form))
          {:children   [{:belongs-to-hakukohteet   []
                         :fieldClass               "formField"
                         :fieldType                "multipleChoice"
                         :id                       "higher-completed-base-education"
                         :koodisto-ordered-by-user true
                         :koodisto-source          {:title   "Kk-pohjakoulutusvaihtoehdot"
                                                    :uri     "pohjakoulutuskklomake"
                                                    :version 1}
                         :label                    {:en "Fill in the education that you have completed  or will complete during the admission process (autumn 2019)"
                                                    :fi "Ilmoita suorittamasi koulutukset. Ilmoita myös ne, jotka suoritat hakukautena (syksy 2019)."
                                                    :sv "Ange utbildningar som du har avlagt. Ange också dem som du avlägger under ansökningsperioden (hösten 2019)."}
                         :options                  higher-education-module-options
                         :params                   {}
                         :rules                    {:pohjakoulutusristiriita nil}
                         :validators               ["required"]}
                        {:fieldClass "formField"
                         :fieldType  "singleChoice"
                         :id         "secondary-completed-base-education"
                         :label      {:en "Have you completed general upper secondary education or vocational qualification?"
                                      :fi "Oletko suorittanut lukion/ylioppilastutkinnon tai ammatillisen tutkinnon? "
                                      :sv "Har du avlagt gymnasiet/studentexamen eller yrkesinriktad examen?"}
                         :options    [{:followups [{:fieldClass      "formField"
                                                    :fieldType       "dropdown"
                                                    :id              "secondary-completed-base-education--country"
                                                    :koodisto-source {:title "Maat ja valtiot" :uri "maatjavaltiot2" :version 1}
                                                    :label           {:en "Choose country"
                                                                      :fi "Valitse suoritusmaa"
                                                                      :sv " Välj land där du avlagt examen"}
                                                    :options         []
                                                    :params          {:info-text
                                                                      {:label
                                                                       {:en "Choose the country where you have completed your most recent qualification. If you have not yet completed a general upper secondary school syllabus/matriculation examination or vocational qualification but are in the process of doing so please choose the country where you will complete the qualification. NB: a vocational qualification can be a vocational upper secondary qualification school-level qualification post-secondary level qualification higher vocational level qualification further vocational qualification or specialist vocational qualification. Do not fill in the country where you have completed a higher education qualification."
                                                                        :fi "Merkitse viimeisimmän tutkintosi suoritusmaa. Jos sinulla ei ole vielä lukion päättötodistusta/ylioppilastutkintoa tai ammatillista tutkintoa mutta olet suorittamassa sellaista valitse se maa jossa parhaillaan suoritat kyseistä tutkintoa. Huom: ammatillinen tutkinto voi olla ammatillinen perustutkinto kouluasteen opistoasteen tai ammatillisen korkea-asteen tutkinto ammatti-tai erikoisammattitutkinto. Älä merkitse tähän korkeakoulututkinnon suoritusmaata."
                                                                        :sv "Ange land där din senaste examen avlagts. Om du ännu inte har avlagt gymnasiet/studentexamen eller yrkesinriktad examen men håller på att göra det välj då det land där du som bäst avlägger examen i fråga. Obs: yrkesinriktad examen kan vara yrkesinriktad grundexamen examen på skolnivå examen på institutsnivå yrkesinriktad examen på högre nivå yrkesexamen eller specialyrkesexamen. Ange inte här landet där du har avlagt högskoleexamen."}}}}]
                                       :label     {:en "Yes" :fi "Kyllä" :sv "Ja"}
                                       :value     "0"}
                                      {:label {:en "No" :fi "En" :sv "Nej"}
                                       :value "1"}]
                         :params     {:info-text
                                      {:label
                                       {:en "This is required for statistical reasons"
                                        :fi "Tämä tieto kysytään tilastointia varten."
                                        :sv "Denna uppgift frågas för statistik."}}}
                         :validators ["required"]}
                        {:cannot-edit false
                         :cannot-view false
                         :fieldClass  "formField"
                         :fieldType   "singleChoice"
                         :id          "finnish-vocational-before-1995"
                         :label       {:en "Have you completed a university or university of applied sciences ( prev. polytechnic) degree in Finland before 2003?"
                                       :fi "Oletko suorittanut suomalaisen ammattikorkeakoulu- tai yliopistotutkinnon ennen vuotta 2003?"
                                       :sv "Har du avlagt en finländsk yrkeshögskole- eller universitetsexamen före år 2003?"}
                         :options     [{:followups [{:cannot-edit false
                                                     :cannot-view false
                                                     :fieldClass  "formField"
                                                     :fieldType   "textField"
                                                     :id          "finnish-vocational-before-1995--year-of-completion"
                                                     :label       {:en "Year of completion" :fi "Suoritusvuosi" :sv "Avlagd år"}
                                                     :params      {:max-value "2002" :min-value "1900" :numeric true :size "S"}
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
                                                     :options         []
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
                                        :label     {:en "Yes" :fi "Kyllä" :sv "Ja"}
                                        :value     "0"}
                                       {:label {:en "No" :fi "Ei" :sv "Nej"}
                                        :value "1"}]
                         :params      {:info-text
                                       {:label
                                        {:en "Write your university or university of applied sciences degree only if you have completed it before 2003. After that date the information of completed degrees will be received automatically from the higher education institutions. If you have completed a university/university of applied sciences degree or have received a study place in higher education in Finland after autumn 2014 your admission can be affected. More information on: \n<a href=\"https://studyinfo.fi/wp2/en/higher-education/applying/quota-for-first-time-applicants/\" target=\"_blank\">the quota for first -time applicant</a>"
                                         :fi "Merkitse tähän suorittamasi korkeakoulututkinto vain siinä tapauksessa, jos olet suorittanut sen ennen vuotta 2003. Sen jälkeen suoritetut tutkinnot saadaan automaattisesti korkeakouluilta. Suomessa suoritettu korkeakoulututkinto tai syksyllä 2014 tai sen jälkeen alkaneesta koulutuksesta vastaanotettu korkeakoulututkintoon johtava opiskelupaikka voivat vaikuttaa valintaan. Lue lisää : <a href=\"https://opintopolku.fi/wp/valintojen-tuki/yhteishaku/korkeakoulujen-yhteishaku/ensikertalaiskiintio/\" target=\"_blank\">ensikertalaisuuskiintiöstä</a>"
                                         :sv "Ange här den högskoleexamen som du avlagt före år 2003. Examina som avlagts efter detta fås automatiskt av högskolorna. En högskoleexamen som avlagts i Finland eller en studieplats inom utbildning som leder till högskoleexamen som mottagits år 2014 eller senare kan inverka på antagningen. Läs mera om: <a href=\"https://studieinfo.fi/wp/stod-for-studievalet/gemensam-ansokan/gemensam-ansokan-till-hogskolor/kvot-for-forstagangssokande/\" target=\"_blank\">kvoten för förstagångssökande</a>"}}}
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
