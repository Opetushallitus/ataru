(ns ataru.component-data.base-education-module-kk)

(def my-large-json
  {:id "245d59d6-a7a4-4572-91e9-48862a32b061",
 :label
 {:en "Your educational background",
  :fi "Pohjakoulutuksesi",
  :sv "Din utbildningsbakgrund"},
 :params {},
 :children
 [{:params
   {:hidden false,
    :info-text
    {:label
     {:en
      "[Read more about who can apply for bachelor's and master's programmes](https://opintopolku.fi/konfo/en/sivu/how-to-apply-for-bachelors-and-masters)",
      :fi
      "Lue lisää siitä millä koulutuksella voit hakea\n- [yliopistokoulutuksiin](https://opintopolku.fi/konfo/fi/sivu/valmistaudu-korkeakoulujen-yhteishakuun#hakukelpoisuus-yliopistoon)\n- [ammattikorkeakoulutuksiin](https://opintopolku.fi/konfo/fi/sivu/valmistaudu-korkeakoulujen-yhteishakuun#hakukelpoisuus-ammattikorkeakouluun)",
      :sv
      "Mer information om vem som kan söka till\n- [universitetsutbildning](https://opintopolku.fi/konfo/sv/sivu/forbered-dig-for-gemensam-ansokan-till-hogskolor#anskningsbehrighet-till-universitet)\n- [yrkeshögskoleutbildning](https://opintopolku.fi/konfo/sv/sivu/forbered-dig-for-gemensam-ansokan-till-hogskolor#anskningsbehrighet-till-yrkeshgskolor)\n"}}},
   :koodisto-source
   {:uri "pohjakoulutuskklomake",
    :title "Kk-pohjakoulutusvaihtoehdot",
    :version 2,
    :allow-invalid? false},
   :koodisto-ordered-by-user true,
   :validators ["required"],
   :fieldClass "formField",
   :label
   {:en
    "Fill in the education that you have completed or will complete during the admission process.",
    :fi
    "Ilmoita suorittamasi koulutukset. Ilmoita myös ne, jotka suoritat hakukautena.",
    :sv
    "Ange de utbildningar som du har avlagt. Ange också dem som du avlägger under ansökningsperioden."},
   :id "higher-completed-base-education",
   :options
   [{:label
     {:en
      "Matriculation examination completed in Finland (Ylioppilastutkinto)",
      :fi "Suomessa suoritettu ylioppilastutkinto",
      :sv "Studentexamen som avlagts i Finland"},
     :value "pohjakoulutus_yo",
     :followups
     [{:id "pohjakoulutus_yo--yes-year-of-completion",
       :label
       {:en "Year of completion",
        :fi "Suoritusvuosi",
        :sv "Avlagd år"},
       :params
       {:size "S",
        :hidden false,
        :numeric true,
        :max-value "2022",
        :min-value "1900"},
       :options
       [{:label {:fi "", :sv ""},
         :value "0",
         :condition
         {:answer-compared-to 1990, :comparison-operator "<"},
         :followups
         [{:belongs-to-hakukohteet [],
           :params
           {:hidden false,
            :deadline nil,
            :info-text
            {:value
             {:en
              "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
              :fi
              "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
              :sv
              "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. "},
             :enabled? true}},
           :belongs-to-hakukohderyhma [],
           :fieldClass "formField",
           :label
           {:en "Finnish matriculation examination certificate",
            :fi "Ylioppilastutkintotodistus",
            :sv "Studentexamensbetyg"},
           :id "b76856ad-2a7d-4918-aa11-05de869f8fa3",
           :options [],
           :metadata
           {:locked false,
            :created-by
            {:oid "1.2.246.562.24.42485718933",
             :date "2020-06-29T07:30:04Z",
             :name "Anonymisoitu Virkailija"},
            :modified-by
            {:oid "1.2.246.562.24.83554265088",
             :date "2022-09-08T11:58:51Z",
             :name "Minea Wilo-Tanninen"}},
           :fieldType "attachment"}]}
        {:label {:fi "", :sv ""},
         :value "1",
         :condition
         {:answer-compared-to 1989, :comparison-operator ">"},
         :followups
         [{:id "0b149b90-8556-473d-81d9-866c955b0644",
           :text
           {:en
            "Your matriculation examination details are received automatically from the national registry for study rights and completed studies. You can check your study rights and completed studies in Finland from [My Studyinfo's](https://studyinfo.fi/oma-opintopolku/) section: My completed studies (Only available in Finnish/Swedish). If your information is incorrect or information is missing, please contact the Matriculation Examination Board to correct any errors. ",
            :fi
            "Saamme ylioppilastutkinnon suoritustietosi ylioppilastutkintorekisteristä. Voit tarkistaa opintosuorituksesi [Oma Opintopolku -palvelun](https://opintopolku.fi/oma-opintopolku/) Omat opintosuoritukseni -osiosta. Jos tiedossasi on puutteita, ole yhteydessä ylioppilastutkintolautakuntaan tietojen korjaamiseksi. \n",
            :sv
            "Vi får uppgifterna om din studentexamen ur studentexamensregistret. Du kan kolla dina studieprestationer i [Min Studieinfos](https://studieinfo.fi/oma-opintopolku/) del Mina studier. Om dina uppgifter är felaktiga, ska du kontakta Studentexamensnämnden som kan korrigera felen.\n"},
           :label {:fi ""},
           :params {},
           :metadata
           {:locked false,
            :created-by
            {:oid "1.2.246.562.24.42485718933",
             :date "2020-06-29T08:10:42Z",
             :name "Anonymisoitu Virkailija"},
            :modified-by
            {:oid "1.2.246.562.24.42485718933",
             :date "2021-07-27T07:07:20Z",
             :name "Stefan Hanhinen"}},
           :fieldType "p",
           :fieldClass "infoElement"}]}],
       :metadata
       {:locked false,
        :created-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2020-06-29T07:29:49Z",
         :name "Anonymisoitu Virkailija"},
        :modified-by
        {:oid "1.2.246.562.24.83554265088",
         :date "2022-09-08T11:58:31Z",
         :name "Minea Wilo-Tanninen"}},
       :fieldType "textField",
       :fieldClass "formField",
       :validators ["numeric" "required"]}],
     :belongs-to-hakukohderyhma []}
    {:label
     {:en
      "Vocational upper secondary qualification completed in Finland (ammatillinen perustutkinto)",
      :fi "Suomessa suoritettu ammatillinen perustutkinto",
      :sv "Yrkesinriktad grundexamen som avlagts i Finland"},
     :value "pohjakoulutus_amp",
     :followups
     [{:id "15de78a3-29a6-435b-88f2-46f318ca8acb",
       :text
       {:en
        "Please make sure that your degree is truly a Finnish vocational upper secondary qualification (ammatillinen perustutkinto). As a rule, these degrees were not available before 1994. It is not possible to enter the year of completion earlier than 1994 on the form.",
        :fi
        "Tarkistathan, että kyseessä on varmasti ammatillinen perustutkinto. Näitä tutkintoja on voinut suorittaa pääsääntöisesti vuodesta 1994 alkaen. Vuotta 1994 aiempia suoritusvuosia ammatilliselle perustutkinnolle ei lomakkeella pysty ilmoittamaan.",
        :sv
        "Kontrollera att det verkligen är en yrkesinriktad grundexamen. Dessa examina har i regel kunnat avläggas från och med 1994. Det är inte möjligt att ange tidigare än år 1994 avlagda examina på blanketten."},
       :label {:fi ""},
       :params {},
       :metadata
       {:locked false,
        :created-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2021-02-02T09:14:23Z",
         :name "Stefan Hanhinen"},
        :modified-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2021-07-27T07:07:20Z",
         :name "Stefan Hanhinen"}},
       :fieldType "p",
       :fieldClass "infoElement"}
      {:id "b447d3d0-590a-40f5-8a7b-4f88c053266f",
       :label {:fi "", :sv ""},
       :params {},
       :children
       [{:id "pohjakoulutus_amp--year-of-completion",
         :label
         {:en "Year of completion",
          :fi "Suoritusvuosi",
          :sv "Avlagd år"},
         :params
         {:size "S",
          :numeric true,
          :max-value "2022",
          :min-value "1994"},
         :options
         [{:label {:fi "", :sv ""},
           :value "1",
           :condition
           {:answer-compared-to 2022, :comparison-operator "="},
           :followups
           [{:id "e0446747-4daa-43c9-8aa3-44d42a520cbf",
             :label
             {:en "Have you graduated",
              :fi "Oletko valmistunut?",
              :sv "Har du tagit examen"},
             :params {},
             :options
             [{:label {:en "Yes", :fi "Kyllä", :sv "Ja"},
               :value "0",
               :followups []}
              {:label {:en "No", :fi "En", :sv "Nej"},
               :value "1",
               :followups
               [{:id "dc8f4912-074f-470c-99ad-01152482b117",
                 :label
                 {:en "Estimated graduation date (dd.mm.yyyy)",
                  :fi "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)",
                  :sv "Beräknat examensdatum (dd.mm.åååå)"},
                 :params {:size "S", :numeric false, :decimals nil},
                 :fieldType "textField",
                 :fieldClass "formField",
                 :validators ["required"]}
                {:id "9315bb6f-641b-4387-a7d5-7070b71da242",
                 :label
                 {:en
                  "Preliminary certificate from the educational institution",
                  :fi "Ennakkoarvio ammatillisesta perustutkinnosta",
                  :sv
                  "Läroanstaltens preliminär intyg om yrkesinriktad grundexamen"},
                 :params
                 {:hidden false,
                  :deadline nil,
                  :info-text
                  {:value
                   {:en
                    "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                    :fi
                    "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                    :sv
                    "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. "},
                   :enabled? true}},
                 :options [],
                 :fieldType "attachment",
                 :fieldClass "formField",
                 :belongs-to-hakukohteet [],
                 :belongs-to-hakukohderyhma []}]}],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2020-06-29T07:59:53Z",
               :name "Anonymisoitu Virkailija"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "singleChoice",
             :fieldClass "formField",
             :validators ["required"]}]}
          {:label {:fi "", :sv ""},
           :value "2",
           :condition
           {:answer-compared-to 2017, :comparison-operator ">"},
           :followups
           [{:id "ad41b90a-6ec5-4394-9e46-b44a7b4e10be",
             :text
             {:en
              "Your final vocational qualification details are received automatically from the national registry for study rights and completed studies. You can check your study rights and completed studies in Finland from [My Studyinfo's](https://studyinfo.fi/oma-opintopolku/) section: My completed studies (Only available in Finnish/Swedish). If your information is incorrect or information is missing, please contact the educational institution to correct any errors. \n",
              :fi
              "Saamme lopulliset ammatillisen perustutkinnon suoritustietosi valtakunnallisesta [Koski-tietovarannosta](https://www.oph.fi/fi/palvelut/koski-tietovaranto). Voit tarkistaa opintosuorituksesi [Oma Opintopolku -palvelun](https://opintopolku.fi/oma-opintopolku/) Omat opintosuoritukseni -osiosta. Jos tiedoissasi on puutteita, ole yhteydessä oppilaitokseesi tietojen korjaamiseksi. \n",
              :sv
              "Vi får slutliga uppgifterna om din yrkesinriktade grundexamen ur den nationella [informationsresursen Koski](https://www.oph.fi/sv/tjanster/informationsresurser-koski). Du kan kolla dina studieprestationer i [Min Studieinfos](https://studieinfo.fi/oma-opintopolku/) del Mina studier. Om dina uppgifter är felaktiga, ska du kontakta din läroanstalt som kan korrigera felen.\n"},
             :label {:fi ""},
             :params {},
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2020-06-29T08:10:42Z",
               :name "Anonymisoitu Virkailija"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "p",
             :fieldClass "infoElement"}]}
          {:label {:fi "", :sv ""},
           :value "3",
           :condition
           {:answer-compared-to 2017, :comparison-operator "="},
           :followups
           [{:id "f5e22a47-724e-4991-adc7-298a4e90890a",
             :label
             {:en
              "Have you completed your qualification as a competence based qualification in its entirety?",
              :fi
              "Oletko suorittanut ammatillisen perustutkinnon näyttötutkintona?",
              :sv "Har du avlagt examen som fristående yrkesexamen?"},
             :params {:info-text {:label nil}},
             :options
             [{:label {:en "Yes", :fi "Kyllä", :sv "Ja"},
               :value "0",
               :followups
               [{:id "cc683935-76cc-45b8-96b4-c1968458c099",
                 :text
                 {:fi
                  "Huomaathan, ettet ole mukana todistusvalinnassa, jos olet suorittanut tutkinnon näyttötutkintona. ",
                  :sv
                  "Obs! En examen som är avlagd som fristående examen beaktas inte i betygsbaserad antagning."},
                 :label {:fi ""},
                 :params {},
                 :metadata
                 {:created-by
                  {:oid "1.2.246.562.24.27704980991",
                   :date "2022-03-14T10:48:55Z",
                   :name "Topias Kähärä"},
                  :modified-by
                  {:oid "1.2.246.562.24.27704980991",
                   :date "2022-03-14T10:49:46Z",
                   :name "Topias Kähärä"}},
                 :fieldType "p",
                 :fieldClass "infoElement"}
                {:belongs-to-hakukohteet [],
                 :params
                 {:hidden false,
                  :deadline nil,
                  :info-text
                  {:value
                   {:en
                    "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                    :fi
                    "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                    :sv
                    "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
                   :enabled? true}},
                 :belongs-to-hakukohderyhma [],
                 :fieldClass "formField",
                 :label
                 {:en "Vocational qualification diploma",
                  :fi "Ammatillisen perustutkinnon tutkintotodistus",
                  :sv "Yrkesinriktad grundexamens betyg"},
                 :id "79ede6c5-a158-4ef1-98fa-8225da672d7c",
                 :options [],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T07:41:02Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.83554265088",
                   :date "2022-09-08T12:11:23Z",
                   :name "Minea Wilo-Tanninen"}},
                 :fieldType "attachment"}]}
              {:label {:en "No", :fi "En", :sv "Nej"},
               :value "1",
               :followups
               [{:id "527201f9-37aa-42ad-9ace-b33a6fcd0d83",
                 :text
                 {:en
                  "Your final vocational qualification details are received automatically from the national registry for study rights and completed studies. You can check your study rights and completed studies in Finland from [My Studyinfo's](https://studyinfo.fi/oma-opintopolku/) section: My completed studies (Only available in Finnish/Swedish). If your information is incorrect or information is missing, please contact the educational institution to correct any errors. \n",
                  :fi
                  "Saamme lopulliset ammatillisen perustutkinnon suoritustietosi valtakunnallisesta [Koski-tietovarannosta](https://www.oph.fi/fi/palvelut/koski-tietovaranto). Voit tarkistaa opintosuorituksesi [Oma Opintopolku -palvelun](https://opintopolku.fi/oma-opintopolku/) Omat opintosuoritukseni -osiosta. Jos tiedoissasi on puutteita, ole yhteydessä oppilaitokseesi tietojen korjaamiseksi. \n",
                  :sv
                  "Vi får slutliga uppgifterna om din yrkesinriktade grundexamen ur den nationella [informationsresursen Koski](https://www.oph.fi/sv/tjanster/informationsresurser-koski). Du kan kolla dina studieprestationer i [Min Studieinfos](https://studieinfo.fi/oma-opintopolku/) del Mina studier. Om dina uppgifter är felaktiga, ska du kontakta din läroanstalt som kan korrigera felen.\n"},
                 :label {:fi ""},
                 :params {},
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T08:10:42Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2021-07-27T07:07:20Z",
                   :name "Stefan Hanhinen"}},
                 :fieldType "p",
                 :fieldClass "infoElement"}]}],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-06-09T12:00:12Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2022-02-24T12:34:31Z",
               :name "Topias Kähärä"}},
             :fieldType "singleChoice",
             :fieldClass "formField",
             :validators ["required"]}]}],
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T07:38:34Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-10-19T11:22:56Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["numeric" "required"]}
        {:id "a83f5d5e-3b77-4902-8acf-ffd945dc5712",
         :label
         {:en "Vocational qualification",
          :fi "Ammatillinen tutkinto",
          :sv "Yrkesinriktad examen"},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T08:39:05Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["required"]}
        {:id "eba7c1a7-782a-4190-804e-8a5983f494c1",
         :label
         {:en "Scope of vocational qualification",
          :fi "Ammatillisen tutkinnon laajuus",
          :sv "Omfattning av yrkesinriktad examen"},
         :params {:size "S", :numeric true, :decimals 1},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T07:45:15Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["numeric" "required"]}
        {:id "a8ea25c1-ef29-4beb-9ad4-404515f86576",
         :label
         {:en "The scope unit",
          :fi "Laajuuden yksikkö",
          :sv "Omfattningens enhet"},
         :params {},
         :options
         [{:label {:en "Courses", :fi "Kurssia", :sv "Kurser"},
           :value "0"}
          {:label
           {:en "ECTS credits",
            :fi "Opintopistettä",
            :sv "Studiepoäng"},
           :value "1"}
          {:label
           {:en "Study weeks",
            :fi "Opintoviikkoa",
            :sv "Studieveckor"},
           :value "2"}
          {:label
           {:en "Competence points",
            :fi "Osaamispistettä",
            :sv "Kompetenspoäng"},
           :value "3"}
          {:label {:en "Hours", :fi "Tuntia", :sv "Timmar"},
           :value "4"}
          {:label
           {:en "Weekly lessons per year",
            :fi "Vuosiviikkotuntia",
            :sv "Årsveckotimmar"},
           :value "5"}
          {:label {:en "Years", :fi "Vuotta", :sv "År"}, :value "6"}],
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T07:46:27Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "dropdown",
         :fieldClass "formField",
         :validators ["required"]}
        {:params
         {:info-text
          {:label
           {:en
            "Select \"Unknown\", if the educational institution where you have completed your degree cannot be found on the list.",
            :fi
            "Valitse \"Tuntematon\", jos et löydä listasta oppilaitosta, jossa olet suorittanut tutkintosi.",
            :sv
            "Välj \"Okänd\" om du inte hittar den läroanstalt, där du har avlagt din examen."}}},
         :koodisto-source
         {:uri "oppilaitostyyppi",
          :title "Ammatilliset oppilaitokset",
          :version 1,
          :allow-invalid? true},
         :validators ["required"],
         :fieldClass "formField",
         :label
         {:en "Educational institution",
          :fi "Oppilaitos ",
          :sv "Läroanstalt "},
         :id "9073691d-b72d-4412-9a40-0f4939399c26",
         :options
         [{:label
           {:en "Ålands folkhögskola",
            :fi "Ålands folkhögskola",
            :sv "Ålands folkhögskola"},
           :value "01701",
           :followups
           [{:id "4c4fd45e-4805-4269-972a-03f759bc56f0",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:deadline nil,
              :info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2022-09-22T10:23:06Z",
               :name "Riku Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands handelsläroverk",
            :fi "Ålands handelsläroverk",
            :sv "Ålands handelsläroverk"},
           :value "01279",
           :followups
           [{:id "3cfc86c4-4ae2-4c56-a8e8-063a512860d5",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:deadline nil,
              :info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2022-09-22T10:23:12Z",
               :name "Riku Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands hantverksskola",
            :fi "Ålands hantverksskola",
            :sv "Ålands hantverksskola"},
           :value "02596",
           :followups
           [{:id "278e5453-e409-4465-aaec-2f1457d9d156",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:deadline nil,
              :info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2022-09-22T10:23:17Z",
               :name "Riku Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands hotell- och restaurangskola",
            :fi "Ålands hotell- och restaurangskola",
            :sv "Ålands hotell- och restaurangskola"},
           :value "01419",
           :followups
           [{:id "e419b58c-d61e-4065-9fb0-8abe58c1b1f4",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:deadline nil,
              :info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2022-09-22T10:23:23Z",
               :name "Riku Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands husmodersskola",
            :fi "Ålands husmodersskola",
            :sv "Ålands husmodersskola"},
           :value "01388",
           :followups
           [{:id "8d92034d-9ee5-48ee-942b-235ed55cd836",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:deadline nil,
              :info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2022-09-22T10:23:27Z",
               :name "Riku Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands musikinstitut",
            :fi "Ålands musikinstitut",
            :sv "Ålands musikinstitut"},
           :value "10004",
           :followups
           [{:id "a73685e2-be98-4f6b-bf96-b1bdad9831ba",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:deadline nil,
              :info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2022-09-22T10:23:32Z",
               :name "Riku Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands naturbruksskola",
            :fi "Ålands naturbruksskola",
            :sv "Ålands naturbruksskola"},
           :value "01510",
           :followups
           [{:id "26b83bb1-6ec1-4ea0-b524-dbcee0a28f7b",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:deadline nil,
              :info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2022-09-22T10:23:39Z",
               :name "Riku Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands sjöfartsläroverk",
            :fi "Ålands sjöfartsläroverk",
            :sv "Ålands sjöfartsläroverk"},
           :value "01569",
           :followups
           [{:id "6b90681d-9fef-4ac8-a71d-52004e94fd9d",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:deadline nil,
              :info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2022-09-22T10:23:43Z",
               :name "Riku Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands sjömansskola",
            :fi "Ålands sjömansskola",
            :sv "Ålands sjömansskola"},
           :value "01573",
           :followups
           [{:id "4d3f4782-5117-4d1d-aad1-83235963877e",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:deadline nil,
              :info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2022-09-22T10:23:49Z",
               :name "Riku Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands tekniska läroverk",
            :fi "Ålands tekniska läroverk",
            :sv "Ålands tekniska läroverk"},
           :value "01029",
           :followups
           [{:id "9951a464-642b-4f5d-a00e-7214fb1b5485",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:deadline nil,
              :info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2022-09-22T10:23:55Z",
               :name "Riku Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands vårdinstitut",
            :fi "Ålands vårdinstitut",
            :sv "Ålands vårdinstitut"},
           :value "02526",
           :followups
           [{:id "dd3e1ef4-6d6e-4edb-92d3-e3208c435e72",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands yrkesgymnasium",
            :fi "Ålands yrkesgymnasium",
            :sv "Ålands yrkesgymnasium"},
           :value "10102",
           :followups
           [{:id "a6f2790d-5a2e-4be9-b872-bf01023465dd",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands yrkesskola",
            :fi "Ålands yrkesskola",
            :sv "Ålands yrkesskola"},
           :value "01110",
           :followups
           [{:id "1be0b573-d164-4071-bd7d-ee6a5fe3ea56",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}],
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T07:51:01Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "dropdown"}
        {:id "212acba1-ab88-4d65-89d0-1080a60d4ea6",
         :text {:fi ""},
         :label
         {:en "Click add if you want to add further qualifications.",
          :fi "Paina lisää, jos haluat lisätä useampia tutkintoja.",
          :sv
          "Tryck på lägg till om du vill lägga till flera examina."},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T07:49:39Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "p",
         :fieldClass "infoElement"}],
       :metadata
       {:locked false,
        :created-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2020-06-29T07:38:31Z",
         :name "Anonymisoitu Virkailija"},
        :modified-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2021-07-27T07:07:20Z",
         :name "Stefan Hanhinen"}},
       :fieldType "fieldset",
       :fieldClass "questionGroup"}]}
    {:label
     {:en
      "Further or specialist vocational qualification completed in Finland (ammatti- tai erikoisammattitutkinto)",
      :fi "Suomessa suoritettu ammatti- tai erikoisammattitutkinto",
      :sv
      "Yrkesexamen eller specialyrkesexamen som avlagts i Finland"},
     :value "pohjakoulutus_amt",
     :followups
     [{:id "b0597988-c918-4fa6-92de-19b732751bab",
       :label {:fi "", :sv ""},
       :params {},
       :children
       [{:id "pohjakoulutus_amt--year-of-completion",
         :label
         {:en "Year of completion",
          :fi "Suoritusvuosi",
          :sv "Avlagd år"},
         :params
         {:size "S",
          :numeric true,
          :max-value "2022",
          :min-value "1900"},
         :options
         [{:label {:fi "", :sv ""},
           :value "0",
           :condition
           {:answer-compared-to 2022, :comparison-operator "="},
           :followups
           [{:id "ab8dd921-d324-4b7f-bb51-cb55e3958ad2",
             :label
             {:en "Have you graduated?",
              :fi "Oletko valmistunut?",
              :sv "Har du tagit examen?"},
             :params {},
             :options
             [{:label {:en "Yes", :fi "Kyllä", :sv "Ja"},
               :value "0",
               :followups []}
              {:label {:en "No", :fi "En", :sv "Nej"},
               :value "1",
               :followups
               [{:id "b0b77b24-3f98-4991-842d-f938dbb526b9",
                 :label
                 {:en "Estimated graduation date (dd.mm.yyyy)",
                  :fi "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)",
                  :sv "Beräknat examensdatum (dd.mm.åååå)"},
                 :params {:size "S", :numeric false, :decimals nil},
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T08:02:07Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2021-07-27T07:07:20Z",
                   :name "Stefan Hanhinen"}},
                 :fieldType "textField",
                 :fieldClass "formField",
                 :validators ["required"]}
                {:belongs-to-hakukohteet [],
                 :params
                 {:hidden false,
                  :deadline nil,
                  :info-text
                  {:value
                   {:en
                    "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                    :fi
                    "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                    :sv
                    "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. "},
                   :enabled? true}},
                 :belongs-to-hakukohderyhma [],
                 :fieldClass "formField",
                 :label
                 {:en
                  "Preliminary certificate from the educational institution",
                  :fi "Ennakkoarvio ammattitutkinnosta",
                  :sv "Läroanstaltens preliminär intyg"},
                 :id "f151f24a-4d94-46b4-891a-8ca1063d36f3",
                 :options [],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T08:01:17Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.83554265088",
                   :date "2022-09-08T12:12:04Z",
                   :name "Minea Wilo-Tanninen"}},
                 :fieldType "attachment"}]}],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2020-06-29T07:59:53Z",
               :name "Anonymisoitu Virkailija"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "singleChoice",
             :fieldClass "formField",
             :validators ["required"]}]}
          {:label {:fi "", :sv ""},
           :value "1",
           :condition
           {:answer-compared-to 2018, :comparison-operator "<"},
           :followups
           [{:belongs-to-hakukohteet [],
             :params
             {:hidden false,
              :deadline nil,
              :info-text
              {:value
               {:en
                "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                :fi
                "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. "},
               :enabled? true}},
             :belongs-to-hakukohderyhma [],
             :fieldClass "formField",
             :label
             {:en
              "Vocational or specialist vocational qualification diploma",
              :fi
              "Tutkintotodistus ammatti- tai erikoisammattitutkinnosta",
              :sv "Betyg av yrkesexamen eller en specialyrkesexamen"},
             :id "f6ab74b7-b8b6-46a4-8bf2-8f4404454cfd",
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2020-06-29T08:40:45Z",
               :name "Anonymisoitu Virkailija"},
              :modified-by
              {:oid "1.2.246.562.24.83554265088",
               :date "2022-09-08T12:12:18Z",
               :name "Minea Wilo-Tanninen"}},
             :fieldType "attachment"}]}
          {:label {:fi "", :sv ""},
           :value "2",
           :condition
           {:answer-compared-to 2017, :comparison-operator ">"},
           :followups
           [{:id "f62bb43f-fc0f-46a7-8a4a-583e3a0f7a0b",
             :text
             {:en
              "Your final vocational qualification details are received automatically from the national registry for study rights and completed studies. You can check your study rights and completed studies in Finland from [My Studyinfo's](https://studyinfo.fi/oma-opintopolku/) section: My completed studies (Only available in Finnish/Swedish). If your information is incorrect or information is missing, please contact the educational institution to correct any errors. ",
              :fi
              "Saamme lopulliset ammattitutkinnon suoritustietosi valtakunnallisesta [Koski-tietovarannosta](https://www.oph.fi/fi/palvelut/koski-tietovaranto). Voit tarkistaa opintosuorituksesi [Oma Opintopolku -palvelun](https://opintopolku.fi/oma-opintopolku/) Omat opintosuoritukseni -osiosta. Jos tiedoissasi on puutteita, ole yhteydessä oppilaitokseesi tietojen korjaamiseksi. ",
              :sv
              "Vi får de slutliga uppgifterna om din yrkesexamen ur den nationella [informationsresursen Koski](https://www.oph.fi/sv/tjanster/informationsresurser-koski). Du kan kontrollera dina studieprestationer i [Min Studieinfos](https://studieinfo.fi/oma-opintopolku/) del Mina studier. Om dina uppgifter är felaktiga, ska du kontakta din läroanstalt som kan korrigera felen."},
             :label {:fi ""},
             :params {},
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2020-06-29T08:10:42Z",
               :name "Anonymisoitu Virkailija"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "p",
             :fieldClass "infoElement"}]}],
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T08:35:13Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-10-19T11:22:35Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["numeric" "required"]}
        {:id "84000e6a-ca09-468c-8b45-5cc39694f5ab",
         :label {:en "Qualification", :fi "Tutkinto", :sv "Examen"},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T08:39:05Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["required"]}
        {:id "3f5fea06-fa56-450e-94f5-b7e98b34c9e8",
         :label
         {:en "Scope of qualification",
          :fi "Laajuus",
          :sv "Examens omfattning"},
         :params {:size "S", :numeric true, :decimals 1},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T08:39:05Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["numeric"]}
        {:id "3e9337ef-5e9f-4f36-8eb0-d0464ab6c463",
         :label
         {:en "The scope unit",
          :fi "Laajuuden yksikkö",
          :sv "Omfattningens enhet"},
         :params {},
         :options
         [{:label {:en "Courses", :fi "Kurssia", :sv "Kurser"},
           :value "0"}
          {:label
           {:en "ECTS credits",
            :fi "Opintopistettä",
            :sv "Studiepoäng"},
           :value "1"}
          {:label
           {:en "Study weeks",
            :fi "Opintoviikkoa",
            :sv "Studieveckor"},
           :value "2"}
          {:label
           {:en "Competence points",
            :fi "Osaamispistettä",
            :sv "Kompetenspoäng"},
           :value "3"}
          {:label {:en "Hours", :fi "Tuntia", :sv "Timmar"},
           :value "4"}
          {:label
           {:en "Weekly lessons per year",
            :fi "Vuosiviikkotuntia",
            :sv "Årsveckotimmar"},
           :value "5"}
          {:label {:en "Years", :fi "Vuotta", :sv "År"}, :value "6"}],
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T07:46:27Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "dropdown",
         :fieldClass "formField",
         :validators []}
        {:params
         {:info-text
          {:label
           {:en
            "Select \"Unknown\", if the educational institution where you have completed your degree cannot be found on the list.",
            :fi
            "Valitse \"Tuntematon\", jos et löydä listasta oppilaitosta, jossa olet suorittanut tutkintosi.",
            :sv
            "Välj \"Okänd\" om du inte hittar den läroanstalt, där du har avlagt din examen."}}},
         :koodisto-source
         {:uri "oppilaitostyyppi",
          :title "Ammatilliset oppilaitokset",
          :version 1,
          :allow-invalid? true},
         :validators ["required"],
         :fieldClass "formField",
         :label
         {:en "Educational institution",
          :fi "Oppilaitos ",
          :sv "Läroanstalt "},
         :id "ed3e75df-eee5-40e2-8ec3-fce9a5b2aeef",
         :options
         [{:label
           {:en "Ålands folkhögskola",
            :fi "Ålands folkhögskola",
            :sv "Ålands folkhögskola"},
           :value "01701",
           :followups
           [{:id "2f961533-4c17-4d59-b113-028a285c542a",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands handelsläroverk",
            :fi "Ålands handelsläroverk",
            :sv "Ålands handelsläroverk"},
           :value "01279",
           :followups
           [{:id "bb5f9abf-0b3d-4c0e-8e98-5d57fdcd2390",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands hantverksskola",
            :fi "Ålands hantverksskola",
            :sv "Ålands hantverksskola"},
           :value "02596",
           :followups
           [{:id "e5676031-de78-41d8-8027-b15cf6e5a0a8",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands hotell- och restaurangskola",
            :fi "Ålands hotell- och restaurangskola",
            :sv "Ålands hotell- och restaurangskola"},
           :value "01419",
           :followups
           [{:id "944d66d6-c7a5-421b-8662-968fd689470b",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands husmodersskola",
            :fi "Ålands husmodersskola",
            :sv "Ålands husmodersskola"},
           :value "01388",
           :followups
           [{:id "d2bbf479-850b-4f9f-87bb-d5e52648d192",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands musikinstitut",
            :fi "Ålands musikinstitut",
            :sv "Ålands musikinstitut"},
           :value "10004",
           :followups
           [{:id "7a85a09a-43cb-425f-a8e7-0d119571ce2b",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands naturbruksskola",
            :fi "Ålands naturbruksskola",
            :sv "Ålands naturbruksskola"},
           :value "01510",
           :followups
           [{:id "0efb4936-752f-4be0-b190-7f71d103ef9c",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands sjöfartsläroverk",
            :fi "Ålands sjöfartsläroverk",
            :sv "Ålands sjöfartsläroverk"},
           :value "01569",
           :followups
           [{:id "75830b10-89ef-4bd0-8542-b3ef733b3ff8",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands sjömansskola",
            :fi "Ålands sjömansskola",
            :sv "Ålands sjömansskola"},
           :value "01573",
           :followups
           [{:id "7d58e26f-5100-4a07-8b3c-9d026f19ad6c",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands tekniska läroverk",
            :fi "Ålands tekniska läroverk",
            :sv "Ålands tekniska läroverk"},
           :value "01029",
           :followups
           [{:id "0ff46c10-517f-4081-a790-f206dcaa6092",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands vårdinstitut",
            :fi "Ålands vårdinstitut",
            :sv "Ålands vårdinstitut"},
           :value "02526",
           :followups
           [{:id "e16441fe-27b2-493b-8862-b8261f2901fc",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands yrkesgymnasium",
            :fi "Ålands yrkesgymnasium",
            :sv "Ålands yrkesgymnasium"},
           :value "10102",
           :followups
           [{:id "f5f9697b-b567-408c-ad1c-c226d0439913",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands yrkesskola",
            :fi "Ålands yrkesskola",
            :sv "Ålands yrkesskola"},
           :value "01110",
           :followups
           [{:id "43c4ac82-5947-43c5-addb-bb7d8b9c70be",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}],
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T07:51:01Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "dropdown"}
        {:id "73c7d045-9a5b-4b8e-be3a-7bd66e457cd8",
         :text {:fi ""},
         :label
         {:en "Click add if you want to add further qualifications.",
          :fi "Paina lisää, jos haluat lisätä useampia tutkintoja.",
          :sv
          "Tryck på lägg till om du vill lägga till flera examina."},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T07:49:39Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "p",
         :fieldClass "infoElement"}],
       :metadata
       {:locked false,
        :created-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2020-06-29T08:35:04Z",
         :name "Anonymisoitu Virkailija"},
        :modified-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2021-07-27T07:07:20Z",
         :name "Stefan Hanhinen"}},
       :fieldType "fieldset",
       :fieldClass "questionGroup"}]}
    {:label
     {:en "Bachelor’s/Master’s/Doctoral degree completed in Finland",
      :fi "Suomessa suoritettu korkeakoulututkinto",
      :sv "Högskoleexamen som avlagts i Finland"},
     :value "pohjakoulutus_kk",
     :followups
     [{:id "253f1d39-b7ae-4ab6-b3d2-bd4ccf590601",
       :label {:fi "", :sv ""},
       :params {},
       :children
       [{:id "pohjakoulutus_kk--completion-date",
         :label
         {:en "Year of completion",
          :fi "Suoritusvuosi",
          :sv "Avlagd år"},
         :params
         {:size "S",
          :numeric true,
          :max-value "2022",
          :min-value "1900"},
         :options
         [{:label {:fi "", :sv ""},
           :value "0",
           :condition
           {:answer-compared-to 2022, :comparison-operator "="},
           :followups
           [{:id "0ac3a1ee-ee30-4cc4-97a3-d9cf4422fb98",
             :label
             {:en "Have you graduated",
              :fi "Oletko valmistunut?",
              :sv "Har du tagit examen"},
             :params {},
             :options
             [{:label {:en "Yes", :fi "Kyllä", :sv "Ja"},
               :value "0",
               :followups
               [{:belongs-to-hakukohteet [],
                 :params
                 {:hidden false,
                  :deadline nil,
                  :info-text
                  {:value
                   {:en
                    "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                    :fi
                    "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                    :sv
                    "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                   :enabled? true}},
                 :belongs-to-hakukohderyhma [],
                 :fieldClass "formField",
                 :label
                 {:en
                  "Transcript of records of higher education degree completed in Finland",
                  :fi
                  "Opintosuoritusote Suomessa suoritettuun korkeakoulututkintoon sisältyvistä opinnoista",
                  :sv
                  "Studieprestationsutdrag om högskoleexamen som avlagts i Finland"},
                 :id "0525badf-86bb-40eb-83ae-88cfb2eb2e9e",
                 :options [],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T09:13:13Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.83554265088",
                   :date "2022-09-08T12:12:41Z",
                   :name "Minea Wilo-Tanninen"}},
                 :fieldType "attachment"}
                {:belongs-to-hakukohteet [],
                 :params
                 {:hidden false,
                  :deadline nil,
                  :info-text
                  {:value
                   {:en
                    "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                    :fi
                    "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                    :sv
                    "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. "},
                   :enabled? true}},
                 :belongs-to-hakukohderyhma [],
                 :fieldClass "formField",
                 :label
                 {:en "Higher education degree certificate",
                  :fi
                  "Suomessa suoritetun korkeakoulututkinnon tutkintotodistus",
                  :sv "Högskoleexamensbetyg"},
                 :id "2be194a7-35cc-4142-9de1-a6d38851db1c",
                 :options [],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T09:13:41Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.83554265088",
                   :date "2022-09-08T12:12:51Z",
                   :name "Minea Wilo-Tanninen"}},
                 :fieldType "attachment"}
                {:id "7c5a8eec-723e-4249-96a3-836f8ebde337",
                 :label
                 {:en
                  "Share a link to your study records from My Studyinfo",
                  :fi
                  "Jaa linkki opintosuoritustietoihisi Oma Opintopolku -palvelussa",
                  :sv
                  "Dela dina prestationsuppgifter direkt från Min Studieinfo"},
                 :params
                 {:size "L",
                  :hidden false,
                  :info-text
                  {:label
                   {:en
                    "This question applies only study programmes listed above, under \"Show study programmes\".\n\nYou can share the information about your completed studies using a link via My Studyinfo service. In this case you do not need to submit transcript of records and degree certificate separately as an attachment to your application.\n\nTo create a link to your completed study records:\n\n1. Log in to [My Studyinfo service](https://studyinfo.fi/oma-opintopolku/) (requires Finnish e-identification and respective means of identification)\n2. Choose \"Proceed to studies\".\n3. Choose \"Jaa suoritustietoja\" (share study records).\n4. Choose the study records you wish to share.\n5. Choose \"Jaa valitsemasi opinnot\" (share studyrecords you have chosen).\n6. Choose \"Kopioi linkki\" (copy link). \n7. Paste the copied link to the text field below.\n",
                    :fi
                    "Tämä kysymys koskee vain yllä mainittuja hakukohteita, jotka näet painamalla \"näytä hakukohteet\".\n\nHalutessasi voit jakaa opintosuoritustietosi sekä läsnä- ja poissaolokausitietosi Oma Opintopolku -palvelusta saatavan linkin avulla. Tällöin sinun ei tarvitse toimittaa erillistä opintosuoritusotetta ja tutkintotodistusta hakemuksesi liitteeksi.\n\nNäin luot linkin omiin suoritustietoihisi:\n\n1. Kirjaudu sisään [Oma Opintopolku -palveluun](https://opintopolku.fi/oma-opintopolku/).\n2. Valitse ”Siirry opintosuorituksiin”.\n3. Valitse näytöltä ”Jaa suoritustietoja”.\n4. Valitse suoritustiedot, jotka haluat jakaa. Valitse ainakin koulutus, jonka perusteella haet.\n5. Valitse ”Jaa valitsemasi opinnot”.\n6. Valitse ”Kopioi linkki”.\n7. Liitä linkki alla olevaan tekstikenttään.",
                    :sv
                    "Denna fråga gäller de ovannämnda ansökningsmålen som du får fram genom att klicka på ”visa ansökningsmål”. \n\nDu kan meddela uppgifterna om dina studieprestationer och dina närvaro- och frånvaroperioder med hjälp av en länk. Då behöver du inte lämna in ett separat studieutdrag och betyg som bilaga till din ansökan.\n\nSå här skapar du en länk till dina prestationsuppgifter:\n\n1. Logga in i [tjänsten Min Studieinfo](https://studieinfo.fi/oma-opintopolku/).\n2. Välj ”Fortsätt till studierna”.\n3. Välj ”Dela dina prestationsuppgifter”.\n4. Välj de prestationsuppgifter du vill dela.\n5. Välj ”Dela valda studier”.\n6. Välj ”Kopiera länk”.\n7. Klistra in länken i fältet nedan på ansökningsblanketten."}}},
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2021-02-02T09:30:05Z",
                   :name "Stefan Hanhinen"},
                  :modified-by
                  {:oid "1.2.246.562.24.83554265088",
                   :date "2022-09-08T12:13:00Z",
                   :name "Minea Wilo-Tanninen"}},
                 :fieldType "textField",
                 :fieldClass "formField",
                 :belongs-to-hakukohteet [],
                 :belongs-to-hakukohderyhma []}]}
              {:label {:en "No", :fi "En", :sv "Nej"},
               :value "1",
               :followups
               [{:id "9efbd3d0-82d6-4f76-968d-7d69ae7ace51",
                 :label
                 {:en "Estimated graduation date (dd.mm.yyyy)",
                  :fi "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)",
                  :sv "Beräknat examensdatum (dd.mm.åååå)"},
                 :params {:size "S", :numeric false, :decimals nil},
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T08:02:07Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2021-07-27T07:07:20Z",
                   :name "Stefan Hanhinen"}},
                 :fieldType "textField",
                 :fieldClass "formField",
                 :validators ["required"]}
                {:belongs-to-hakukohteet [],
                 :params
                 {:hidden false,
                  :deadline nil,
                  :info-text
                  {:value
                   {:en
                    "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                    :fi
                    "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                    :sv
                    "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                   :enabled? true}},
                 :belongs-to-hakukohderyhma [],
                 :fieldClass "formField",
                 :label
                 {:en
                  "Transcript of records of higher education degree completed in Finland",
                  :fi
                  "Opintosuoritusote Suomessa suoritettavaan korkeakoulututkintoon sisältyvistä opinnoista",
                  :sv
                  "Studieprestationsutdrag om högskoleexamen som avlagts i Finland"},
                 :id "0ccbda54-f182-41f9-bdab-31bb29d08fe6",
                 :options [],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T08:01:17Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.83554265088",
                   :date "2022-09-08T12:13:44Z",
                   :name "Minea Wilo-Tanninen"}},
                 :fieldType "attachment"}
                {:belongs-to-hakukohteet [],
                 :params
                 {:hidden false,
                  :deadline nil,
                  :info-text
                  {:value
                   {:en
                    "The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                    :fi
                    "Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                    :sv
                    "Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. "},
                   :enabled? true}},
                 :belongs-to-hakukohderyhma [],
                 :fieldClass "formField",
                 :label
                 {:en "Higher education degree certificate",
                  :fi
                  "Suomessa suoritettavan korkeakoulututkinnon tutkintotodistus",
                  :sv "Högskoleexamensbetyg"},
                 :id "701bc7e3-e8af-4541-89e0-60000b743e48",
                 :options [],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T09:13:41Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.83554265088",
                   :date "2022-09-08T12:13:52Z",
                   :name "Minea Wilo-Tanninen"}},
                 :fieldType "attachment"}
                {:id "897f6088-4860-4d15-9679-d485686fd7da",
                 :label
                 {:en
                  "Share a link to your study records from My Studyinfo",
                  :fi
                  "Jaa linkki opintosuoritustietoihisi Oma Opintopolku -palvelussa",
                  :sv
                  "Dela dina prestationsuppgifter direkt från Min Studieinfo"},
                 :params
                 {:size "L",
                  :hidden false,
                  :info-text
                  {:label
                   {:en
                    "This question applies only study programmes listed above, under \"Show study programmes\".\n\nYou can share the information about your completed studies using a link via My Studyinfo service. In this case you do not need to submit transcript of records and degree certificate separately as an attachment to your application.\n\nTo create a link to your completed study records:\n\n1. Log in to [My Studyinfo service](https://studyinfo.fi/oma-opintopolku/) (requires Finnish e-identification and respective means of identification)\n2. Choose \"Proceed to studies\".\n3. Choose \"Jaa suoritustietoja\" (share study records).\n4. Choose the study records you wish to share.\n5. Choose \"Jaa valitsemasi opinnot\" (share studyrecords you have chosen).\n6. Choose \"Kopioi linkki\" (copy link). \n7. Paste the copied link to the text field below.\n",
                    :fi
                    "Tämä kysymys koskee vain yllä mainittuja hakukohteita, jotka näet painamalla \"näytä hakukohteet\".\n\nHalutessasi voit jakaa opintosuoritustietosi sekä läsnä- ja poissaolokausitietosi Oma Opintopolku -palvelusta saatavan linkin avulla. Tällöin sinun ei tarvitse toimittaa erillistä opintosuoritusotetta ja tutkintotodistusta hakemuksesi liitteeksi.\n\nNäin luot linkin omiin suoritustietoihisi:\n\n1. Kirjaudu sisään [Oma Opintopolku -palveluun](https://opintopolku.fi/oma-opintopolku/).\n2. Valitse ”Siirry opintosuorituksiin”.\n3. Valitse näytöltä ”Jaa suoritustietoja”.\n4. Valitse suoritustiedot, jotka haluat jakaa. Valitse ainakin koulutus, jonka perusteella haet.\n5. Valitse ”Jaa valitsemasi opinnot”.\n6. Valitse ”Kopioi linkki”.\n7. Liitä linkki alla olevaan tekstikenttään.",
                    :sv
                    "Denna fråga gäller de ovannämnda ansökningsmålen som du får fram genom att klicka på ”visa ansökningsmål”. \n\nDu kan meddela uppgifterna om dina studieprestationer och dina närvaro- och frånvaroperioder med hjälp av en länk. Då behöver du inte lämna in ett separat studieutdrag och betyg som bilaga till din ansökan.\n\nSå här skapar du en länk till dina prestationsuppgifter:\n\n1. Logga in i [tjänsten Min Studieinfo](https://studieinfo.fi/oma-opintopolku/).\n2. Välj ”Fortsätt till studierna”.\n3. Välj ”Dela dina prestationsuppgifter”.\n4. Välj de prestationsuppgifter du vill dela.\n5. Välj ”Dela valda studier”.\n6. Välj ”Kopiera länk”.\n7. Klistra in länken i fältet nedan på ansökningsblanketten."}}},
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2021-02-02T09:30:05Z",
                   :name "Stefan Hanhinen"},
                  :modified-by
                  {:oid "1.2.246.562.24.83554265088",
                   :date "2022-09-08T12:14:00Z",
                   :name "Minea Wilo-Tanninen"}},
                 :fieldType "textField",
                 :fieldClass "formField",
                 :belongs-to-hakukohteet [],
                 :belongs-to-hakukohderyhma []}]}],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2020-06-29T07:59:53Z",
               :name "Anonymisoitu Virkailija"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "singleChoice",
             :fieldClass "formField",
             :validators ["required"]}]}
          {:label {:fi "", :sv ""},
           :value "1",
           :condition
           {:answer-compared-to 2022, :comparison-operator "<"},
           :followups
           [{:belongs-to-hakukohteet [],
             :params
             {:hidden false,
              :deadline nil,
              :info-text
              {:value
               {:en
                "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                :fi
                "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
               :enabled? true}},
             :belongs-to-hakukohderyhma [],
             :fieldClass "formField",
             :label
             {:en
              "Transcript of records of higher education degree completed in Finland",
              :fi
              "Opintosuoritusote Suomessa suoritettuun korkeakoulututkintoon sisältyvistä opinnoista",
              :sv
              "Studieprestationsutdrag om högskoleexamen som avlagts i Finland"},
             :id "35ae1773-c193-49ce-8562-31a3528678d5",
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2020-06-29T09:13:13Z",
               :name "Anonymisoitu Virkailija"},
              :modified-by
              {:oid "1.2.246.562.24.83554265088",
               :date "2022-09-08T12:14:11Z",
               :name "Minea Wilo-Tanninen"}},
             :fieldType "attachment"}
            {:belongs-to-hakukohteet [],
             :params
             {:hidden false,
              :deadline nil,
              :info-text
              {:value
               {:en
                "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                :fi
                "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. "},
               :enabled? true}},
             :belongs-to-hakukohderyhma [],
             :fieldClass "formField",
             :label
             {:en "Higher education degree certificate",
              :fi
              "Suomessa suoritetun korkeakoulututkinnon tutkintotodistus",
              :sv "Högskoleexamensbetyg"},
             :id "0d4b933b-efbd-4944-b15e-81eee4fefa01",
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2020-06-29T09:13:41Z",
               :name "Anonymisoitu Virkailija"},
              :modified-by
              {:oid "1.2.246.562.24.83554265088",
               :date "2022-09-08T12:14:23Z",
               :name "Minea Wilo-Tanninen"}},
             :fieldType "attachment"}
            {:id "1049072b-532c-4cc1-9c8e-ce0b2237beb7",
             :label
             {:en
              "Share a link to your study records from My Studyinfo",
              :fi
              "Jaa linkki opintosuoritustietoihisi Oma Opintopolku -palvelussa",
              :sv
              "Dela dina prestationsuppgifter direkt från Min Studieinfo"},
             :params
             {:size "L",
              :hidden false,
              :info-text
              {:label
               {:en
                "This question applies only study programmes listed above, under \"Show study programmes\".\n\nYou can share the information about your completed studies using a link via My Studyinfo service. In this case you do not need to submit transcript of records and degree certificate separately as an attachment to your application.\n\nTo create a link to your completed study records:\n\n1. Log in to [My Studyinfo service](https://studyinfo.fi/oma-opintopolku/) (requires Finnish e-identification and respective means of identification)\n2. Choose \"Proceed to studies\".\n3. Choose \"Jaa suoritustietoja\" (share study records).\n4. Choose the study records you wish to share.\n5. Choose \"Jaa valitsemasi opinnot\" (share studyrecords you have chosen).\n6. Choose \"Kopioi linkki\" (copy link). \n7. Paste the copied link to the text field below.\n",
                :fi
                "Tämä kysymys koskee vain yllä mainittuja hakukohteita, jotka näet painamalla \"näytä hakukohteet\".\n\nHalutessasi voit jakaa opintosuoritustietosi sekä läsnä- ja poissaolokausitietosi Oma Opintopolku -palvelusta saatavan linkin avulla. Tällöin sinun ei tarvitse toimittaa erillistä opintosuoritusotetta ja tutkintotodistusta hakemuksesi liitteeksi.\n\nNäin luot linkin omiin suoritustietoihisi:\n\n1. Kirjaudu sisään [Oma Opintopolku -palveluun](https://opintopolku.fi/oma-opintopolku/).\n2. Valitse ”Siirry opintosuorituksiin”.\n3. Valitse näytöltä ”Jaa suoritustietoja”.\n4. Valitse suoritustiedot, jotka haluat jakaa. Valitse ainakin koulutus, jonka perusteella haet.\n5. Valitse ”Jaa valitsemasi opinnot”.\n6. Valitse ”Kopioi linkki”.\n7. Liitä linkki alla olevaan tekstikenttään.",
                :sv
                "Denna fråga gäller de ovannämnda ansökningsmålen som du får fram genom att klicka på ”visa ansökningsmål”. \n\nDu kan meddela uppgifterna om dina studieprestationer och dina närvaro- och frånvaroperioder med hjälp av en länk. Då behöver du inte lämna in ett separat studieutdrag och betyg som bilaga till din ansökan.\n\nSå här skapar du en länk till dina prestationsuppgifter:\n\n1. Logga in i [tjänsten Min Studieinfo](https://studieinfo.fi/oma-opintopolku/).\n2. Välj ”Fortsätt till studierna”.\n3. Välj ”Dela dina prestationsuppgifter”.\n4. Välj de prestationsuppgifter du vill dela.\n5. Välj ”Dela valda studier”.\n6. Välj ”Kopiera länk”.\n7. Klistra in länken i fältet nedan på ansökningsblanketten."}}},
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-02-02T09:30:05Z",
               :name "Stefan Hanhinen"},
              :modified-by
              {:oid "1.2.246.562.24.21928143170",
               :date "2022-02-03T07:56:51Z",
               :name "Saana Finni"}},
             :fieldType "textField",
             :fieldClass "formField",
             :belongs-to-hakukohteet [],
             :belongs-to-hakukohderyhma
             ["1.2.246.562.28.82245421241"
              "1.2.246.562.28.62434568325"
              "1.2.246.562.28.94314351951"
              "1.2.246.562.28.98195701192"
              "1.2.246.562.28.99649224161"]}]}],
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T09:10:23Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-10-19T11:22:22Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["numeric" "required"]}
        {:params {},
         :koodisto-source
         {:uri "kktutkinnot",
          :title "Kk-tutkinnot",
          :version 1,
          :allow-invalid? false},
         :koodisto-ordered-by-user true,
         :validators ["required"],
         :fieldClass "formField",
         :label
         {:en "Degree level", :fi "Tutkintotaso", :sv "Examensnivå"},
         :id "0026f937-a221-4612-ab6f-a1dd67c19e7e",
         :options [],
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T09:11:18Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "dropdown"}
        {:params {},
         :koodisto-source
         {:uri "tutkinto",
          :title "Tutkinto",
          :version 2,
          :allow-invalid? false},
         :validators ["required"],
         :fieldClass "formField",
         :label {:en "Degree", :fi "Tutkinto", :sv "Examen"},
         :id "b52fa408-e170-48f2-ac0b-404a86ac1fba",
         :options [],
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-07-29T07:27:16Z",
           :name "Risto Hanhinen"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "dropdown"}
        {:id "19385eff-1c3c-450b-abcd-e78680f70eea",
         :label
         {:en "Higher education institution",
          :fi "Korkeakoulu",
          :sv "Högskola"},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T09:12:01Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["required"]}
        {:id "eaebe392-aa5e-43e8-a98f-d48315face01",
         :text {:fi ""},
         :label
         {:en "Click add if you want to add further qualifications.",
          :fi "Paina lisää, jos haluat lisätä useampia tutkintoja.",
          :sv
          "Tryck på lägg till om du vill lägga till flera examina."},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T07:49:39Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "p",
         :fieldClass "infoElement"}],
       :metadata
       {:locked false,
        :created-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2020-06-29T09:20:09Z",
         :name "Anonymisoitu Virkailija"},
        :modified-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2021-07-27T07:07:20Z",
         :name "Stefan Hanhinen"}},
       :fieldType "fieldset",
       :fieldClass "questionGroup"}]}
    {:label
     {:en
      "Upper secondary double degree completed in Finland (kaksoistutkinto)",
      :fi
      "Suomessa suoritettu kaksoistutkinto (ammatillinen perustutkinto ja ylioppilastutkinto)",
      :sv "Dubbelexamen som avlagts i Finland"},
     :value "pohjakoulutus_yo_ammatillinen",
     :followups
     [{:id "2436c512-b733-463e-8ca2-8c6b0ee9f140",
       :label {:fi "", :sv ""},
       :params {},
       :children
       [{:id "fa8ded62-6e16-4ac2-a785-35bd9e22f0dc",
         :text
         {:en
          "Your matriculation examination details are received automatically from the national registry for study rights and completed studies. You can check your study rights and completed studies in Finland from [My Studyinfo's](https://studyinfo.fi/oma-opintopolku/) section: My completed studies (Only available in Finnish/Swedish). If your information is incorrect or information is missing, please contact the Matriculation Examination Board to correct any errors.",
          :fi
          "Saamme ylioppilastutkinnon suoritustietosi ylioppilastutkintorekisteristä. Voit tarkistaa opintosuorituksesi [Oma Opintopolku -palvelun](https://opintopolku.fi/oma-opintopolku/) Omat opintosuoritukseni -osiosta. Jos tiedossasi on puutteita, ole yhteydessä ylioppilastutkintolautakuntaan tietojen korjaamiseksi. \n",
          :sv
          "Vi får uppgifterna om din studentexamen ur studentexamensregistret. Du kan kontrollera dina studieprestationer i [Min Studieinfos](https://studieinfo.fi/oma-opintopolku/) del Mina studier. Om dina uppgifter är felaktiga, ska du kontakta Studentexamensnämnden som kan korrigera felen."},
         :label {:fi ""},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T08:10:42Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "p",
         :fieldClass "infoElement"}
        {:id
         "pohjakoulutus_yo_ammatillinen--vocational-completion-year",
         :label
         {:en "Year of completion",
          :fi "Suoritusvuosi ",
          :sv "Avlagd år"},
         :params
         {:size "S",
          :hidden false,
          :numeric true,
          :max-value "2022",
          :min-value "1900"},
         :options
         [{:label {:fi "", :sv ""},
           :value "0",
           :condition
           {:answer-compared-to 2022, :comparison-operator "="},
           :followups
           [{:id "fedc8df4-a40e-4a4b-85f1-37fd918c7b62",
             :label
             {:en "Have you graduated",
              :fi "Oletko valmistunut?",
              :sv "Har du tagit examen"},
             :params {},
             :options
             [{:label {:en "Yes", :fi "Kyllä", :sv "Ja"},
               :value "0",
               :followups []}
              {:label {:en "No", :fi "En", :sv "Nej"},
               :value "1",
               :followups
               [{:id "f53383e9-2996-42be-b9b1-9a7537f9ab03",
                 :label
                 {:en "Estimated graduation date (dd.mm.yyyy)",
                  :fi "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)",
                  :sv "Beräknat examensdatum (dd.mm.åååå)"},
                 :params {:size "S", :numeric false, :decimals nil},
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T08:02:07Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2021-07-27T07:07:20Z",
                   :name "Stefan Hanhinen"}},
                 :fieldType "textField",
                 :fieldClass "formField",
                 :validators ["required"]}
                {:belongs-to-hakukohteet [],
                 :params
                 {:hidden false,
                  :deadline nil,
                  :info-text
                  {:value
                   {:en
                    "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                    :fi
                    "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                    :sv
                    "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. "},
                   :enabled? true}},
                 :belongs-to-hakukohderyhma [],
                 :fieldClass "formField",
                 :label
                 {:en
                  "Preliminary certificate from the educational institution",
                  :fi "Ennakkoarvio ammatillisesta perustutkinnosta",
                  :sv
                  "Läroanstaltens preliminär intyg om yrkesinriktad grundexamen"},
                 :id "2df2e11a-988a-4984-bac1-7ff48efee838",
                 :options [],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T08:01:17Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.83554265088",
                   :date "2022-09-08T12:15:27Z",
                   :name "Minea Wilo-Tanninen"}},
                 :fieldType "attachment"}]}],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2020-06-29T07:59:53Z",
               :name "Anonymisoitu Virkailija"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "singleChoice",
             :fieldClass "formField",
             :validators ["required"]}]}
          {:label {:fi "", :sv ""},
           :value "2",
           :condition
           {:answer-compared-to 2017, :comparison-operator "="},
           :followups
           [{:id "e3b63b4c-7b4e-4a44-9f8c-226bab3e38f7",
             :label
             {:en
              "Have you completed your qualification as a competence based qualification in its entirety?",
              :fi
              "Oletko suorittanut ammatillisen perustutkinnon näyttötutkintona?",
              :sv "Har du avlagt examen som fristående yrkesexamen?"},
             :params {:info-text {:label nil}},
             :options
             [{:label {:en "Yes", :fi "Kyllä", :sv "Ja"},
               :value "0",
               :followups
               [{:belongs-to-hakukohteet [],
                 :params
                 {:hidden false,
                  :deadline nil,
                  :info-text
                  {:value
                   {:en
                    "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                    :fi
                    "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                    :sv
                    "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
                   :enabled? true}},
                 :belongs-to-hakukohderyhma [],
                 :fieldClass "formField",
                 :label
                 {:en "Vocational qualification diploma",
                  :fi "Ammatillisen perustutkinnon tutkintotodistus",
                  :sv "Yrkesinriktad grundexamens betyg"},
                 :id "72da1411-0a7c-4993-a3e9-ca46fc6202a0",
                 :options [],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T07:41:02Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.83554265088",
                   :date "2022-09-08T12:15:38Z",
                   :name "Minea Wilo-Tanninen"}},
                 :fieldType "attachment"}]}
              {:label {:en "No", :fi "En", :sv "Nej"},
               :value "1",
               :followups
               [{:id "60a949d0-00f1-49fa-91ad-952210dd6c19",
                 :text
                 {:en
                  "Your final vocational qualification details are received automatically from the national registry for study rights and completed studies. You can check your study rights and completed studies in Finland from [My Studyinfo's](https://studyinfo.fi/oma-opintopolku/) section: My completed studies (Only available in Finnish/Swedish). If your information is incorrect or information is missing, please contact the educational institution to correct any errors. \n",
                  :fi
                  "Saamme lopulliset ammatillisen perustutkinnon suoritustietosi valtakunnallisesta [Koski-tietovarannosta](https://www.oph.fi/fi/palvelut/koski-tietovaranto). Voit tarkistaa opintosuorituksesi [Oma Opintopolku -palvelun](https://opintopolku.fi/oma-opintopolku/) Omat opintosuoritukseni -osiosta. Jos tiedoissasi on puutteita, ole yhteydessä oppilaitokseesi tietojen korjaamiseksi. \n",
                  :sv
                  "Vi får slutliga uppgifterna om din yrkesinriktade grundexamen ur den nationella [informationsresursen Koski](https://www.oph.fi/sv/tjanster/informationsresurser-koski). Du kan kolla dina studieprestationer i [Min Studieinfos](https://studieinfo.fi/oma-opintopolku/) del Mina studier. Om dina uppgifter är felaktiga, ska du kontakta din läroanstalt som kan korrigera felen.\n"},
                 :label {:fi ""},
                 :params {},
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T08:10:42Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2021-07-27T07:07:20Z",
                   :name "Stefan Hanhinen"}},
                 :fieldType "p",
                 :fieldClass "infoElement"}]}],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-06-09T12:00:12Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2022-02-24T12:34:16Z",
               :name "Topias Kähärä"}},
             :fieldType "singleChoice",
             :fieldClass "formField"}]}
          {:label {:fi "", :sv ""},
           :value "3",
           :condition
           {:answer-compared-to 2017, :comparison-operator ">"},
           :followups
           [{:id "8b5187cc-1cc0-4007-a7a3-24ba3718db9a",
             :text
             {:en
              "Your final vocational qualification details are received automatically from the national registry for study rights and completed studies. You can check your study rights and completed studies in Finland from [My Studyinfo's](https://studyinfo.fi/oma-opintopolku/) section: My completed studies (Only available in Finnish/Swedish). If your information is incorrect or information is missing, please contact the educational institution to correct any errors. \n",
              :fi
              "Saamme lopulliset ammatillisen perustutkinnon suoritustietosi valtakunnallisesta [Koski-tietovarannosta](https://www.oph.fi/fi/palvelut/koski-tietovaranto). Voit tarkistaa opintosuorituksesi [Oma Opintopolku -palvelun](https://opintopolku.fi/oma-opintopolku/) Omat opintosuoritukseni -osiosta. Jos tiedoissasi on puutteita, ole yhteydessä oppilaitokseesi tietojen korjaamiseksi. \n",
              :sv
              "Vi får slutliga uppgifterna om din yrkesinriktade grundexamen ur den nationella [informationsresursen Koski](https://www.oph.fi/sv/tjanster/informationsresurser-koski). Du kan kolla dina studieprestationer i [Min Studieinfos](https://studieinfo.fi/oma-opintopolku/) del Mina studier. Om dina uppgifter är felaktiga, ska du kontakta din läroanstalt som kan korrigera felen.\n"},
             :label {:fi ""},
             :params {},
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2020-06-29T08:10:42Z",
               :name "Anonymisoitu Virkailija"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "p",
             :fieldClass "infoElement"}]}],
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.27704980991",
           :date "2021-06-09T13:40:22Z",
           :name "Topias Kähärä"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-10-19T11:22:06Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["numeric" "required"]}
        {:id "9aaa1425-7db0-46f8-8086-3b0de4d63d01",
         :label
         {:en "Vocational qualification",
          :fi "Ammatillinen tutkinto",
          :sv "Yrkesinriktad examen"},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T08:39:05Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["required"]}
        {:id "6c391cd7-21ff-4655-8cd7-5e40375628fa",
         :label
         {:en "Scope of vocational qualification",
          :fi "Ammatillisen perustutkinnon laajuus",
          :sv "Omfattning av yrkesinriktad grundexamen"},
         :params {:size "S", :numeric true, :decimals 1},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T07:45:15Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["numeric" "required"]}
        {:id "db9c52cd-f908-4587-9faa-98e10b087df1",
         :label
         {:en "The scope unit",
          :fi "Laajuuden yksikkö",
          :sv "Omfattningens enhet"},
         :params {},
         :options
         [{:label {:en "Courses", :fi "Kurssia", :sv "Kurser"},
           :value "0"}
          {:label
           {:en "ECTS credits",
            :fi "Opintopistettä",
            :sv "Studiepoäng"},
           :value "1"}
          {:label
           {:en "Study weeks",
            :fi "Opintoviikkoa",
            :sv "Studieveckor"},
           :value "2"}
          {:label
           {:en "Competence points",
            :fi "Osaamispistettä",
            :sv "Kompetenspoäng"},
           :value "3"}
          {:label {:en "Hours", :fi "Tuntia", :sv "Timmar"},
           :value "4"}
          {:label
           {:en "Weekly lessons per year",
            :fi "Vuosiviikkotuntia",
            :sv "Årsveckotimmar"},
           :value "5"}
          {:label {:en "Years", :fi "Vuotta", :sv "År"}, :value "6"}],
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T07:46:27Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "dropdown",
         :fieldClass "formField",
         :validators ["required"]}
        {:params
         {:info-text
          {:label
           {:en
            "Select \"Unknown\", if the educational institution where you have completed your degree cannot be found on the list.",
            :fi
            "Valitse \"Tuntematon\", jos et löydä listasta oppilaitosta, jossa olet suorittanut tutkintosi.",
            :sv
            "Välj \"Okänd\" om du inte hittar den läroanstalt, där du har avlagt din examen."}}},
         :koodisto-source
         {:uri "oppilaitostyyppi",
          :title "Ammatilliset oppilaitokset",
          :version 1,
          :allow-invalid? true},
         :validators ["required"],
         :fieldClass "formField",
         :label
         {:en "Vocational institution",
          :fi "Ammatillinen oppilaitos ",
          :sv "Yrkesinriktad läroanstalt "},
         :id "49813794-2d67-447d-8426-cf2a9b4f715e",
         :options
         [{:label
           {:en "Ålands folkhögskola",
            :fi "Ålands folkhögskola",
            :sv "Ålands folkhögskola"},
           :value "01701",
           :followups
           [{:id "109d33f4-71f1-4887-80ef-7c14848c9ea3",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands handelsläroverk",
            :fi "Ålands handelsläroverk",
            :sv "Ålands handelsläroverk"},
           :value "01279",
           :followups
           [{:id "72fff9f6-df3b-4af7-9414-07d64e5b1e45",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands hantverksskola",
            :fi "Ålands hantverksskola",
            :sv "Ålands hantverksskola"},
           :value "02596",
           :followups
           [{:id "752d5e63-0c6a-4344-bce8-aaaa47cd713a",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands hotell- och restaurangskola",
            :fi "Ålands hotell- och restaurangskola",
            :sv "Ålands hotell- och restaurangskola"},
           :value "01419",
           :followups
           [{:id "f76c8cdd-130a-41fb-880d-f1d184d41c7d",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands husmodersskola",
            :fi "Ålands husmodersskola",
            :sv "Ålands husmodersskola"},
           :value "01388",
           :followups
           [{:id "2cc9d421-7a84-4461-8058-dcda3a632954",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands musikinstitut",
            :fi "Ålands musikinstitut",
            :sv "Ålands musikinstitut"},
           :value "10004",
           :followups
           [{:id "ec380ef0-316f-45b8-aad8-e25d7187a785",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands naturbruksskola",
            :fi "Ålands naturbruksskola",
            :sv "Ålands naturbruksskola"},
           :value "01510",
           :followups
           [{:id "3997cb8a-157d-4b42-bd1d-de888802ac41",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands sjöfartsläroverk",
            :fi "Ålands sjöfartsläroverk",
            :sv "Ålands sjöfartsläroverk"},
           :value "01569",
           :followups
           [{:id "f942a0a7-adf4-492a-8d45-e1fd1f27b919",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands sjömansskola",
            :fi "Ålands sjömansskola",
            :sv "Ålands sjömansskola"},
           :value "01573",
           :followups
           [{:id "56bf28ae-28d6-4528-b27f-72b6a9f1fea9",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands tekniska läroverk",
            :fi "Ålands tekniska läroverk",
            :sv "Ålands tekniska läroverk"},
           :value "01029",
           :followups
           [{:id "7b543235-188e-4f40-aa39-1148e245b6e5",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands vårdinstitut",
            :fi "Ålands vårdinstitut",
            :sv "Ålands vårdinstitut"},
           :value "02526",
           :followups
           [{:id "89b1faa1-72ef-44e7-87ff-1e12ff8e7120",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands yrkesgymnasium",
            :fi "Ålands yrkesgymnasium",
            :sv "Ålands yrkesgymnasium"},
           :value "10102",
           :followups
           [{:id "38dd208e-2a5c-4e1a-8596-36a844fe5364",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}
          {:label
           {:en "Ålands yrkesskola",
            :fi "Ålands yrkesskola",
            :sv "Ålands yrkesskola"},
           :value "01110",
           :followups
           [{:id "ac307713-e7b5-4dff-a590-6977f725fbea",
             :label
             {:fi "Ammatillisen tutkinnon tutkintotodistus",
              :sv "Examensbetyg för yrkesinriktad examen"},
             :params
             {:info-text
              {:value
               {:fi
                "Olet suorittanut/suoritat ammatillisen tutkintosi Ahvenanmaalla, jolloin tutkintotietojasi ei saada suoraan tietovarannosta. Jos olet jo valmistunut, lataa tähän tutkintotodistuksesi. Jos taas valmistut hakuajan jälkeen, toimita liite hakijapalveluihin tai lataa liite hakemuksellesi heti valmistuttuasi.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Du har avlagt/avlägger en yrkesinriktad examen på Åland, vilket innebär att uppgifter om din examen inte fås direkt från datalagret. Ladda upp ditt examensbetyg här om du redan är utexaminerad. Om du utexamineras efter att ansökningstiden har avslutats, lämna in bilagan till antagningsservice eller ladda upp bilagan till din ansökan genast då du har utexaminerats.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.\n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.\nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.\nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
               :enabled? true}},
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2021-03-08T12:30:27Z",
               :name "Topias Kähärä"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "attachment",
             :fieldClass "formField"}]}],
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T07:51:01Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "dropdown"}
        {:id "f0c8c266-2baa-4c6f-bd27-14c96f158bcc",
         :text {:fi ""},
         :label
         {:en "Click add if you want to add further qualifications.",
          :fi "Paina lisää, jos haluat lisätä useampia tutkintoja.",
          :sv
          "Tryck på lägg till om du vill lägga till flera examina."},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T07:49:39Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "p",
         :fieldClass "infoElement"}],
       :metadata
       {:locked false,
        :created-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2020-06-29T07:38:31Z",
         :name "Anonymisoitu Virkailija"},
        :modified-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2021-07-27T07:07:20Z",
         :name "Stefan Hanhinen"}},
       :fieldType "fieldset",
       :fieldClass "questionGroup"}]}
    {:label
     {:en
      "General upper secondary school syllabus completed in Finland (lukion oppimäärä ilman ylioppilastutkintoa)",
      :fi
      "Suomessa suoritettu lukion oppimäärä ilman ylioppilastutkintoa",
      :sv
      "Gymnasiets lärokurs som avlagts i Finland utan studentexamen"},
     :value "pohjakoulutus_lk",
     :followups
     [{:id "pohjakoulutus_lk--year-of-completion",
       :label
       {:en "Year of completion",
        :fi "Suoritusvuosi",
        :sv "Avlagd år"},
       :params
       {:size "S",
        :numeric true,
        :max-value "2022",
        :min-value "1900"},
       :options
       [{:label {:fi "", :sv ""},
         :value "0",
         :condition
         {:answer-compared-to 2022, :comparison-operator "="},
         :followups
         [{:id "56eadc16-8f23-4dfa-b81d-f11513a34f92",
           :label
           {:en "Have you graduated?",
            :fi "Oletko valmistunut",
            :sv "Har du tagit examen?"},
           :params {},
           :options
           [{:label {:en "Yes", :fi "Kyllä", :sv "Ja"},
             :value "0",
             :followups
             [{:belongs-to-hakukohteet [],
               :params
               {:hidden false,
                :deadline nil,
                :info-text
                {:value
                 {:en
                  "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                  :fi
                  "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                  :sv
                  "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. "},
                 :enabled? true}},
               :belongs-to-hakukohderyhma [],
               :fieldClass "formField",
               :label
               {:en "General upper secondary education certificate",
                :fi "Lukion päättötodistus",
                :sv "Gymnasiets avgångsbetyg"},
               :id "bc870a63-6bf4-4088-a8df-20435a0148df",
               :options [],
               :metadata
               {:locked false,
                :created-by
                {:oid "1.2.246.562.24.42485718933",
                 :date "2020-06-29T08:40:45Z",
                 :name "Anonymisoitu Virkailija"},
                :modified-by
                {:oid "1.2.246.562.24.83554265088",
                 :date "2022-09-08T12:16:07Z",
                 :name "Minea Wilo-Tanninen"}},
               :fieldType "attachment"}]}
            {:label {:en "No", :fi "En", :sv "Nej"},
             :value "1",
             :followups
             [{:id "70342187-b00c-4b8a-a7a6-51fc94cd8c40",
               :label
               {:en "Estimated graduation date (dd.mm.yyyy)",
                :fi "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)",
                :sv "Beräknat examensdatum (dd.mm.åååå)"},
               :params {:size "S", :numeric false, :decimals nil},
               :metadata
               {:locked false,
                :created-by
                {:oid "1.2.246.562.24.42485718933",
                 :date "2020-06-29T08:02:07Z",
                 :name "Anonymisoitu Virkailija"},
                :modified-by
                {:oid "1.2.246.562.24.42485718933",
                 :date "2021-07-27T07:07:20Z",
                 :name "Stefan Hanhinen"}},
               :fieldType "textField",
               :fieldClass "formField",
               :validators ["required"]}
              {:id "766a275a-7483-48c9-8817-d2f38e7a2ad1",
               :label
               {:en
                "Latest transcript of study records from Finnish upper secondary school",
                :fi
                "Ennakkoarvio tai viimeisin todistus suoritetuista opinnoista lukiossa",
                :sv
                "Förhandsexamensbetyg eller betyg över slutförda studier om gymnasiestudier"},
               :params
               {:hidden false,
                :deadline nil,
                :info-text
                {:value
                 {:en
                  "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                  :fi
                  "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                  :sv
                  "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. "},
                 :enabled? true}},
               :options [],
               :metadata
               {:locked false,
                :created-by
                {:oid "1.2.246.562.24.42485718933",
                 :date "2021-02-02T10:02:28Z",
                 :name "Stefan Hanhinen"},
                :modified-by
                {:oid "1.2.246.562.24.83554265088",
                 :date "2022-09-08T12:16:15Z",
                 :name "Minea Wilo-Tanninen"}},
               :fieldType "attachment",
               :fieldClass "formField",
               :belongs-to-hakukohderyhma []}]}],
           :metadata
           {:locked false,
            :created-by
            {:oid "1.2.246.562.24.42485718933",
             :date "2021-02-02T10:01:06Z",
             :name "Stefan Hanhinen"},
            :modified-by
            {:oid "1.2.246.562.24.42485718933",
             :date "2021-07-27T07:07:20Z",
             :name "Stefan Hanhinen"}},
           :fieldType "singleChoice",
           :fieldClass "formField",
           :validators ["required"]}]}
        {:label {:fi "", :sv ""},
         :value "1",
         :condition
         {:answer-compared-to 2022, :comparison-operator "<"},
         :followups
         [{:belongs-to-hakukohteet [],
           :params
           {:hidden false,
            :deadline nil,
            :info-text
            {:value
             {:en
              "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
              :fi
              "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
              :sv
              "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. "},
             :enabled? true}},
           :belongs-to-hakukohderyhma [],
           :fieldClass "formField",
           :label
           {:en "General upper secondary education certificate",
            :fi "Lukion päättötodistus",
            :sv "Gymnasiets avgångsbetyg"},
           :id "77b6aaff-ee38-4be4-9d02-1359c2bd7d52",
           :options [],
           :metadata
           {:locked false,
            :created-by
            {:oid "1.2.246.562.24.42485718933",
             :date "2020-06-29T08:40:45Z",
             :name "Anonymisoitu Virkailija"},
            :modified-by
            {:oid "1.2.246.562.24.83554265088",
             :date "2022-09-08T12:16:22Z",
             :name "Minea Wilo-Tanninen"}},
           :fieldType "attachment"}]}],
       :metadata
       {:locked false,
        :created-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2020-06-29T07:33:01Z",
         :name "Anonymisoitu Virkailija"},
        :modified-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2021-10-19T11:21:33Z",
         :name "Stefan Hanhinen"}},
       :fieldType "textField",
       :fieldClass "formField",
       :validators ["numeric" "required"]}]}
    {:label
     {:en
      "International matriculation examination completed in Finland (IB, EB and RP/DIA)",
      :fi
      "Suomessa suoritettu kansainvälinen ylioppilastutkinto (IB, EB ja RP/DIA)",
      :sv
      "Internationell studentexamen som avlagts i Finland (IB, EB och RP/DIA)"},
     :value "pohjakoulutus_yo_kansainvalinen_suomessa",
     :followups
     [{:id "49a62843-a25d-45f9-8a26-229a1ef88acb",
       :label {:fi "", :sv ""},
       :params {},
       :children
       [{:id
         "pohjakoulutus_yo_kansainvalinen_suomessa--year-of-completion",
         :label
         {:en "Year of completion",
          :fi "Suoritusvuosi",
          :sv "Avlagd år"},
         :params
         {:size "S",
          :numeric true,
          :max-value "2022",
          :min-value "1900"},
         :options
         [{:label {:fi "", :sv ""},
           :value "0",
           :condition
           {:answer-compared-to 2022, :comparison-operator "<"},
           :followups
           [{:id "4b4d5370-e46a-48a5-ad7d-a23943e00696",
             :label
             {:en "Matriculation examination",
              :fi "Ylioppilastutkinto",
              :sv "Studentexamen"},
             :params {},
             :options
             [{:label
               {:en "International Baccalaureate -diploma",
                :fi "International Baccalaureate -tutkinto",
                :sv "International Baccalaureate -examen"},
               :value "0",
               :followups
               [{:belongs-to-hakukohteet [],
                 :params
                 {:hidden false,
                  :deadline nil,
                  :info-text
                  {:value
                   {:en
                    "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                    :fi
                    "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                    :sv
                    "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                   :enabled? true}},
                 :belongs-to-hakukohderyhma [],
                 :fieldClass "formField",
                 :label
                 {:en "IB Diploma completed in Finland",
                  :fi
                  "IB Diploma -tutkintotodistus Suomessa suoritetusta tutkinnosta",
                  :sv
                  "IB Diploma från IB-studentexamen som avlagts i Finland"},
                 :id "4f4a8f4c-c955-489a-bc4f-58208dfaa87a",
                 :options [],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T08:47:08Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.83554265088",
                   :date "2022-09-08T12:17:42Z",
                   :name "Minea Wilo-Tanninen"}},
                 :fieldType "attachment"}]}
              {:label
               {:en "European Baccalaureate -diploma",
                :fi "Eurooppalainen ylioppilastutkinto",
                :sv "European Baccalaureate -examen"},
               :value "1",
               :followups
               [{:belongs-to-hakukohteet [],
                 :params
                 {:hidden false,
                  :deadline nil,
                  :info-text
                  {:value
                   {:en
                    "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                    :fi
                    "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                    :sv
                    "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                   :enabled? true}},
                 :belongs-to-hakukohderyhma [],
                 :fieldClass "formField",
                 :label
                 {:en
                  "European Baccalaureate diploma completed in Finland",
                  :fi
                  "European Baccalaureate Diploma -tutkintotodistus Suomessa suoritetusta tutkinnosta",
                  :sv
                  "European Baccalaureate Diploma från EB-studentexamen som avlagts i Finland"},
                 :id "3914888c-9c73-4d57-ba38-f5463ffe07ca",
                 :options [],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T08:47:08Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.83554265088",
                   :date "2022-09-08T12:18:01Z",
                   :name "Minea Wilo-Tanninen"}},
                 :fieldType "attachment"}]}
              {:label
               {:en
                "Deutsche Internationale Abiturprüfung/Reifeprüfung -diploma",
                :fi
                "Deutsche Internationale Abiturprüfung/Reifeprüfung -tutkinto",
                :sv
                "Deutsche Internationale Abiturprüfung/Reifeprüfung -examen"},
               :value "2",
               :followups
               [{:belongs-to-hakukohteet [],
                 :params
                 {:hidden false,
                  :deadline nil,
                  :info-text
                  {:value
                   {:en
                    "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                    :fi
                    "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                    :sv
                    "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                   :enabled? true}},
                 :belongs-to-hakukohderyhma [],
                 :fieldClass "formField",
                 :label
                 {:en "Reifeprüfung/DIA diploma completed in Finland",
                  :fi
                  "Reifeprüfung/DIA-tutkintotodistus Suomessa suoritetusta tutkinnosta",
                  :sv
                  "Reifeprüfung/DIA -examensbetyg from RP/DIA-studentexamen som avlagts i Finland"},
                 :id "67d35b8f-28b4-4433-b219-ea0b63c8c85b",
                 :options [],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T08:47:08Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.83554265088",
                   :date "2022-09-08T12:18:26Z",
                   :name "Minea Wilo-Tanninen"}},
                 :fieldType "attachment"}
                {:belongs-to-hakukohteet [],
                 :params
                 {:hidden false,
                  :deadline nil,
                  :info-text
                  {:value
                   {:en
                    "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                    :fi
                    "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                    :sv
                    "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                   :enabled? true}},
                 :belongs-to-hakukohderyhma [],
                 :fieldClass "formField",
                 :label
                 {:en
                  "An equivalency certificate on upper secondary education based on Reifeprüfung or DIA provisions",
                  :fi
                  "Vastaavuustodistus lukio-opinnoista, jotka perustuvat RP- tai DIA-tutkinnon säännöksiin",
                  :sv
                  "Motsvarighetsintyget av gymnasiestudier, som är baserad på RP- eller DIA-bestämmelser"},
                 :id "e7880326-d3de-4992-8989-919518107847",
                 :options [],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T08:47:08Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.83554265088",
                   :date "2022-09-08T12:18:38Z",
                   :name "Minea Wilo-Tanninen"}},
                 :fieldType "attachment"}]}],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2020-06-29T08:44:41Z",
               :name "Anonymisoitu Virkailija"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "singleChoice",
             :fieldClass "formField",
             :validators ["required"]}]}
          {:label {:fi "", :sv ""},
           :value "1",
           :condition
           {:answer-compared-to 2022, :comparison-operator "="},
           :followups
           [{:id "385c35ba-d1e8-4f56-a7aa-6b2205ead98f",
             :label
             {:en "Matriculation examination",
              :fi "Ylioppilastutkinto",
              :sv "Studentexamen"},
             :params {},
             :options
             [{:label
               {:en "International Baccalaureate -diploma",
                :fi "International Baccalaureate -tutkinto",
                :sv "International Baccalaureate -examen"},
               :value "0",
               :followups
               [{:id "c19a4f45-25c2-42c2-901d-3a9370091651",
                 :label
                 {:en "Have you graduated?",
                  :fi "Oletko valmistunut?",
                  :sv "Har du tagit examen?"},
                 :params {},
                 :options
                 [{:label {:en "Yes", :fi "Kyllä", :sv "Ja"},
                   :value "0",
                   :followups
                   [{:belongs-to-hakukohteet [],
                     :params
                     {:hidden false,
                      :deadline nil,
                      :info-text
                      {:value
                       {:en
                        "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                        :fi
                        "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                        :sv
                        "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                       :enabled? true}},
                     :belongs-to-hakukohderyhma [],
                     :fieldClass "formField",
                     :label
                     {:en "IB Diploma completed in Finland",
                      :fi
                      "IB Diploma -tutkintotodistus Suomessa suoritetusta tutkinnosta",
                      :sv
                      "IB Diploma från IB-studentexamen som avlagts i Finland"},
                     :id "0e603d37-5b21-412d-8979-924c0112c200",
                     :options [],
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2020-06-29T08:47:08Z",
                       :name "Anonymisoitu Virkailija"},
                      :modified-by
                      {:oid "1.2.246.562.24.83554265088",
                       :date "2022-09-08T12:19:24Z",
                       :name "Minea Wilo-Tanninen"}},
                     :fieldType "attachment"}]}
                  {:label {:en "No", :fi "En", :sv "Nej"},
                   :value "1",
                   :followups
                   [{:id "caa6c297-2d57-42c4-a0a0-f3e082c2d59b",
                     :label
                     {:en "Estimated graduation date (dd.mm.yyyy)",
                      :fi
                      "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)",
                      :sv "Beräknat examensdatum (dd.mm.åååå)"},
                     :params
                     {:size "S", :numeric false, :decimals nil},
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2020-06-29T08:02:07Z",
                       :name "Anonymisoitu Virkailija"},
                      :modified-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2021-07-27T07:07:20Z",
                       :name "Stefan Hanhinen"}},
                     :fieldType "textField",
                     :fieldClass "formField",
                     :validators ["required"]}
                    {:belongs-to-hakukohteet [],
                     :params
                     {:hidden false,
                      :deadline nil,
                      :info-text
                      {:value
                       {:en
                        "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                        :fi
                        "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                        :sv
                        "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                       :enabled? true}},
                     :belongs-to-hakukohderyhma [],
                     :fieldClass "formField",
                     :label
                     {:en
                      "Predicted grades from IB completed in Finland",
                      :fi
                      "Oppilaitoksen ennakkoarvio Suomessa suoritettavan tutkinnon arvosanoista (Candidate Predicted Grades)",
                      :sv
                      "Predicted grades från IB-studentexamen som avlagts i Finland "},
                     :id "0aa015ff-7fc0-4737-8363-5d32278ab524",
                     :options [],
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2020-06-29T08:01:17Z",
                       :name "Anonymisoitu Virkailija"},
                      :modified-by
                      {:oid "1.2.246.562.24.83554265088",
                       :date "2022-09-08T12:19:39Z",
                       :name "Minea Wilo-Tanninen"}},
                     :fieldType "attachment"}
                    {:belongs-to-hakukohteet [],
                     :params
                     {:hidden false,
                      :deadline nil,
                      :info-text
                      {:value
                       {:en
                        "The attachment has to be submitted  by the deadline informed next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n",
                        :fi
                        "Määräaika ilmoitetaan liitepyynnön vieressä. \n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                        :sv
                        "Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
                       :enabled? true}},
                     :belongs-to-hakukohderyhma [],
                     :fieldClass "formField",
                     :label
                     {:en
                      "Diploma Programme (DP) Results from IB completed in Finland",
                      :fi
                      "Diploma Programme (DP) Results -asiakirja Suomessa suoritettavasta tutkinnosta",
                      :sv
                      "Diploma Programme (DP) Results från IB-studentexamen som avlagts i Finland"},
                     :id "39ec49ca-9a4b-4c01-8c76-7d1ff5255b66",
                     :options [],
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2020-06-29T08:48:35Z",
                       :name "Anonymisoitu Virkailija"},
                      :modified-by
                      {:oid "1.2.246.562.24.83554265088",
                       :date "2022-09-08T12:19:47Z",
                       :name "Minea Wilo-Tanninen"}},
                     :fieldType "attachment"}]}],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T07:59:53Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.27704980991",
                   :date "2021-11-18T15:56:59Z",
                   :name "Topias Kähärä"}},
                 :fieldType "singleChoice",
                 :fieldClass "formField",
                 :validators ["required"]}]}
              {:label
               {:en "European Baccalaureate -diploma",
                :fi "Eurooppalainen ylioppilastutkinto",
                :sv "European Baccalaureate -examen"},
               :value "1",
               :followups
               [{:id "0ca95204-6f7c-40c9-9642-3c83d6683571",
                 :label
                 {:en "Have you graduated?",
                  :fi "Oletko valmistunut?",
                  :sv "Har du tagit examen?"},
                 :params {},
                 :options
                 [{:label {:en "Yes", :fi "Kyllä", :sv "Ja"},
                   :value "0",
                   :followups
                   [{:belongs-to-hakukohteet [],
                     :params
                     {:hidden false,
                      :deadline nil,
                      :info-text
                      {:value
                       {:en
                        "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                        :fi
                        "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                        :sv
                        "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                       :enabled? true}},
                     :belongs-to-hakukohderyhma [],
                     :fieldClass "formField",
                     :label
                     {:en
                      "European Baccalaureate diploma completed in Finland",
                      :fi
                      "European Baccalaureate Diploma -tutkintotodistus Suomessa suoritetusta tutkinnosta",
                      :sv
                      "European Baccalaureate Diploma från EB-studentexamen som avlagts i Finland"},
                     :id "f56ac7de-3c0f-46e3-8724-8f9fa99c5aec",
                     :options [],
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2020-06-29T08:47:08Z",
                       :name "Anonymisoitu Virkailija"},
                      :modified-by
                      {:oid "1.2.246.562.24.83554265088",
                       :date "2022-09-08T12:20:12Z",
                       :name "Minea Wilo-Tanninen"}},
                     :fieldType "attachment"}]}
                  {:label {:en "No", :fi "En", :sv "Nej"},
                   :value "1",
                   :followups
                   [{:id "94035fa3-bfb7-41cf-86fc-937a48906e40",
                     :label
                     {:en "Estimated graduation date (dd.mm.yyyy)",
                      :fi
                      "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)",
                      :sv "Beräknat examensdatum (dd.mm.åååå)"},
                     :params
                     {:size "S", :numeric false, :decimals nil},
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2020-06-29T08:02:07Z",
                       :name "Anonymisoitu Virkailija"},
                      :modified-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2021-07-27T07:07:20Z",
                       :name "Stefan Hanhinen"}},
                     :fieldType "textField",
                     :fieldClass "formField",
                     :validators ["required"]}
                    {:belongs-to-hakukohteet [],
                     :params
                     {:hidden false,
                      :deadline nil,
                      :info-text
                      {:value
                       {:en
                        "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                        :fi
                        "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                        :sv
                        "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                       :enabled? true}},
                     :belongs-to-hakukohderyhma [],
                     :fieldClass "formField",
                     :label
                     {:en
                      "Predicted grades from EB completed in Finland",
                      :fi
                      "Oppilaitoksen ennakkoarvio Suomessa suoritettavan EB-tutkinnon arvosanoista",
                      :sv
                      "Läroanstaltens preliminära vitsord från EB-studentexamen som avlagts i Finland"},
                     :id "a3de1708-9039-4ed6-8c37-13a2bc23941a",
                     :options [],
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2020-06-29T08:01:17Z",
                       :name "Anonymisoitu Virkailija"},
                      :modified-by
                      {:oid "1.2.246.562.24.83554265088",
                       :date "2022-09-08T12:20:26Z",
                       :name "Minea Wilo-Tanninen"}},
                     :fieldType "attachment"}
                    {:belongs-to-hakukohteet [],
                     :params
                     {:hidden false,
                      :deadline nil,
                      :info-text
                      {:value
                       {:en
                        "The attachment has to be submitted  by the deadline informed next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n",
                        :fi
                        "Määräaika ilmoitetaan liitepyynnön vieressä. \n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                        :sv
                        "Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
                       :enabled? true}},
                     :belongs-to-hakukohderyhma [],
                     :fieldClass "formField",
                     :label
                     {:en
                      "European Baccalaureate diploma completed in Finland",
                      :fi
                      "European Baccalaureate Diploma -tutkintotodistus Suomessa suoritettavasta tutkinnosta",
                      :sv
                      "European Baccalaureate Diploma från EB-studentexamen som avlagts i Finland"},
                     :id "a400e870-7265-4217-b1f9-086cbaffbb98",
                     :options [],
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2020-06-29T08:48:35Z",
                       :name "Anonymisoitu Virkailija"},
                      :modified-by
                      {:oid "1.2.246.562.24.83554265088",
                       :date "2022-09-08T12:20:34Z",
                       :name "Minea Wilo-Tanninen"}},
                     :fieldType "attachment"}]}],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T07:59:53Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.27704980991",
                   :date "2021-11-18T15:56:54Z",
                   :name "Topias Kähärä"}},
                 :fieldType "singleChoice",
                 :fieldClass "formField",
                 :validators ["required"]}]}
              {:label
               {:en
                "Deutsche Internationale Abiturprüfung/Reifeprüfung -diploma",
                :fi
                "Deutsche Internationale Abiturprüfung/Reifeprüfung -tutkinto",
                :sv
                "Deutsche Internationale Abiturprüfung/Reifeprüfung -examen"},
               :value "2",
               :followups
               [{:id "be900146-e4cc-4be1-921e-e09881479494",
                 :label
                 {:en "Have you graduated",
                  :fi "Oletko valmistunut?",
                  :sv "Har du tagit examen"},
                 :params {},
                 :options
                 [{:label {:en "Yes", :fi "Kyllä", :sv "Ja"},
                   :value "0",
                   :followups
                   [{:belongs-to-hakukohteet [],
                     :params
                     {:hidden false,
                      :deadline nil,
                      :info-text
                      {:value
                       {:en
                        "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                        :fi
                        "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                        :sv
                        "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                       :enabled? true}},
                     :belongs-to-hakukohderyhma [],
                     :fieldClass "formField",
                     :label
                     {:en "DIA diploma completed in Finland",
                      :fi
                      "DIA-tutkintotodistus Suomessa suoritetusta tutkinnosta",
                      :sv
                      "DIA -examensbetyg from DIA-studentexamen som avlagts i Finland"},
                     :id "0886b32f-62e8-421d-b8d9-44aff03200ec",
                     :options [],
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2020-06-29T08:47:08Z",
                       :name "Anonymisoitu Virkailija"},
                      :modified-by
                      {:oid "1.2.246.562.24.83554265088",
                       :date "2022-09-08T12:20:49Z",
                       :name "Minea Wilo-Tanninen"}},
                     :fieldType "attachment"}
                    {:belongs-to-hakukohteet [],
                     :params
                     {:hidden false,
                      :deadline nil,
                      :info-text
                      {:value
                       {:en
                        "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                        :fi
                        "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                        :sv
                        "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                       :enabled? true}},
                     :belongs-to-hakukohderyhma [],
                     :fieldClass "formField",
                     :label
                     {:en
                      "An equivalency certificate on upper secondary education based on DIA provisions",
                      :fi
                      "Vastaavuustodistus lukio-opinnoista, jotka perustuvat DIA-tutkinnon säännöksiin",
                      :sv
                      "Motsvarighetsintyget av gymnasiestudier, som är baserad på DIA-bestämmelser"},
                     :id "15b5ab30-30b4-4f91-84be-999473ff7e6d",
                     :options [],
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2020-06-29T08:47:08Z",
                       :name "Anonymisoitu Virkailija"},
                      :modified-by
                      {:oid "1.2.246.562.24.83554265088",
                       :date "2022-09-08T12:21:00Z",
                       :name "Minea Wilo-Tanninen"}},
                     :fieldType "attachment"}]}
                  {:label {:en "No", :fi "En", :sv "Nej"},
                   :value "1",
                   :followups
                   [{:id "a02a78c8-2baa-4c19-94aa-3e2c9e790d91",
                     :label
                     {:en "Estimated graduation date (dd.mm.yyyy)",
                      :fi
                      "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)",
                      :sv "Beräknat examensdatum (dd.mm.åååå)"},
                     :params
                     {:size "S", :numeric false, :decimals nil},
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2020-06-29T08:02:07Z",
                       :name "Anonymisoitu Virkailija"},
                      :modified-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2021-07-27T07:07:20Z",
                       :name "Stefan Hanhinen"}},
                     :fieldType "textField",
                     :fieldClass "formField",
                     :validators ["required"]}
                    {:belongs-to-hakukohteet [],
                     :params
                     {:hidden false,
                      :deadline nil,
                      :info-text
                      {:value
                       {:en
                        "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                        :fi
                        "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                        :sv
                        "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                       :enabled? true}},
                     :belongs-to-hakukohderyhma [],
                     :fieldClass "formField",
                     :label
                     {:en
                      "Grade page of a Deutsches Internationales Abitur (DIA) diploma completed in Finland",
                      :fi
                      "DIA-tutkintotodistuksen arvosanasivu Suomessa suoritettavasta tutkinnosta",
                      :sv
                      "Examensbetygets vitsordssida av Deutsches Internationales Abitur (DIA) -examen avlagd i Finland"},
                     :id "464743a3-a13c-43a4-88ca-9a07febbe7e9",
                     :options [],
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2020-06-29T08:01:17Z",
                       :name "Anonymisoitu Virkailija"},
                      :modified-by
                      {:oid "1.2.246.562.24.83554265088",
                       :date "2022-09-08T12:21:12Z",
                       :name "Minea Wilo-Tanninen"}},
                     :fieldType "attachment"}
                    {:belongs-to-hakukohteet [],
                     :params
                     {:hidden false,
                      :deadline nil,
                      :info-text
                      {:value
                       {:en
                        "The attachment has to be submitted  by the deadline informed next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n",
                        :fi
                        "Määräaika ilmoitetaan liitepyynnön vieressä. \n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                        :sv
                        "Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
                       :enabled? true}},
                     :belongs-to-hakukohderyhma [],
                     :fieldClass "formField",
                     :label
                     {:en "DIA diploma completed in Finland",
                      :fi
                      "DIA-tutkintotodistus Suomessa suoritettavasta tutkinnosta",
                      :sv
                      "DIA -examensbetyg från DIA-studentexamen som avlagts i Finland"},
                     :id "1d196c4d-dfc2-429c-9159-9368dd9ad317",
                     :options [],
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2020-06-29T08:48:35Z",
                       :name "Anonymisoitu Virkailija"},
                      :modified-by
                      {:oid "1.2.246.562.24.83554265088",
                       :date "2022-09-08T12:21:18Z",
                       :name "Minea Wilo-Tanninen"}},
                     :fieldType "attachment"}
                    {:belongs-to-hakukohteet [],
                     :params
                     {:hidden false,
                      :deadline nil,
                      :info-text
                      {:value
                       {:en
                        "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                        :fi
                        "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                        :sv
                        "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                       :enabled? true}},
                     :belongs-to-hakukohderyhma [],
                     :fieldClass "formField",
                     :label
                     {:en
                      "An equivalency certificate on upper secondary education based on DIA provisions",
                      :fi
                      "Vastaavuustodistus lukio-opinnoista, jotka perustuvat DIA-tutkinnon säännöksiin",
                      :sv
                      "Motsvarighetsintyget av gymnasiestudier, som är baserad på DIA-bestämmelser"},
                     :id "316be2f5-cb7f-4af7-a8d1-43c752e389eb",
                     :options [],
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2020-06-29T08:47:08Z",
                       :name "Anonymisoitu Virkailija"},
                      :modified-by
                      {:oid "1.2.246.562.24.83554265088",
                       :date "2022-09-08T12:21:28Z",
                       :name "Minea Wilo-Tanninen"}},
                     :fieldType "attachment"}]}],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T07:59:53Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2021-07-27T07:07:20Z",
                   :name "Stefan Hanhinen"}},
                 :fieldType "singleChoice",
                 :fieldClass "formField",
                 :validators ["required"]}]}],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2020-06-29T08:44:41Z",
               :name "Anonymisoitu Virkailija"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "singleChoice",
             :fieldClass "formField",
             :validators ["required"]}]}],
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T10:07:39Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-10-19T11:21:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["numeric" "required"]}
        {:id "a38d512b-ce05-4c58-99d5-387513aa6b1d",
         :label
         {:en "Educational institution",
          :fi "Oppilaitos",
          :sv "Läroanstalt"},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T08:51:11Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["required"]}
        {:id "8beb8bc1-9317-4f73-af7a-7c926c9d35a5",
         :text {:fi ""},
         :label
         {:en "Click add if you want to add further qualifications.",
          :fi "Paina lisää, jos haluat lisätä useampia tutkintoja.",
          :sv
          "Tryck på lägg till om du vill lägga till flera examina."},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T07:49:39Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "p",
         :fieldClass "infoElement"}],
       :metadata
       {:locked false,
        :created-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2020-06-29T09:33:48Z",
         :name "Anonymisoitu Virkailija"},
        :modified-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2021-07-27T07:07:20Z",
         :name "Stefan Hanhinen"}},
       :fieldType "fieldset",
       :fieldClass "questionGroup"}]}
    {:label
     {:en
      "Vocational upper secondary qualification completed in Finland (kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto)",
      :fi
      "Suomessa suoritettu kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto",
      :sv
      "Yrkesinriktad examen på skolnivå, examen på institutsnivå eller yrkesinriktad examen på högre nivå som avlagts i Finland"},
     :value "pohjakoulutus_amv",
     :followups
     [{:id "d50c812b-76ba-4424-b874-c6acd1f755f8",
       :text
       {:en
        "Please make sure that your degree is truly a Finnish school level (kouluaste), post-secondary level degree (opistoaste) or a higher vocational level degree (ammatillinen korkea-aste). As a rule, these degrees are no longer available in the 2000s. It is not possible to enter the year of completion later than 2005 on the form.",
        :fi
        "Tarkistathan, että kyseessä on varmasti kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto. Näitä tutkintoja ei pääsääntöisesti ole ollut enää 2000-luvulla. Vuotta 2005 myöhempiä kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkintoja ei lomakkeella pysty ilmoittamaan.",
        :sv
        "Kontrollera att det verkligen är en examen på skolnivå, institutsnivå eller inom yrkesutbildning på högre nivå. Dessa examina fanns i regel inte kvar på 2000-talet. Det är inte möjligt att ange senare än år 2005 avlagda examina på blanketten."},
       :label {:fi ""},
       :params {},
       :metadata
       {:locked false,
        :created-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2021-02-02T10:05:07Z",
         :name "Stefan Hanhinen"},
        :modified-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2021-07-27T07:07:20Z",
         :name "Stefan Hanhinen"}},
       :fieldType "p",
       :fieldClass "infoElement"}
      {:id "d6516067-d6a1-40ac-87af-5b3693f7612c",
       :label {:fi "", :sv ""},
       :params {},
       :children
       [{:id "pohjakoulutus_amv--year-of-completion",
         :label
         {:en "Year of completion",
          :fi "Suoritusvuosi",
          :sv "Avlagd år"},
         :params
         {:size "S",
          :hidden false,
          :numeric true,
          :max-value "2005",
          :min-value "1900"},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-10-15T08:19:48Z",
           :name "Risto Hanhinen"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["numeric" "required"]}
        {:id "044260b8-b226-4848-8526-516b2770f3e5",
         :label
         {:en "Type of vocational qualification",
          :fi "Ammatillisen tutkinnon tyyppi",
          :sv "Yrkesinriktad examens typ"},
         :params {:hidden false},
         :options
         [{:label
           {:en "Vocational qualification (kouluaste)",
            :fi "Kouluasteen tutkinto",
            :sv "Yrkesinriktad examen på skolnivå"},
           :value "0",
           :followups
           [{:id "e1918e05-664b-41f5-8dda-6cf9b35cfab4",
             :text
             {:en
              "Please make sure that your degree is truly a Finnish school level degree (kouluasteen tutkinto). As a rule, these degrees are no longer available in the 2000s. For example, the degrees of agronom, a commercial school graduate (merkonomi) and a technician are not school level degrees.",
              :fi
              "Tarkistathan, että kyseessä on varmasti kouluasteen tutkinto. Näitä tutkintoja ei pääsääntöisesti ole ollut enää 2000-luvulla. Esimerkiksi agrologin, teknikon tai merkonomin tutkinnot eivät ole kouluasteen tutkintoja.",
              :sv
              "Kontrollera att det verkligen är en examen på skolnivå. Dessa examina fanns i regel inte kvar på 2000-talet. Exempelvis agrolog-, tekniker- och merkonomexamina är inte examina på skolnivå."},
             :label {:fi ""},
             :params {},
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2020-12-23T07:36:20Z",
               :name "Risto Hanhinen"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "p",
             :fieldClass "infoElement"}]}
          {:label
           {:en "Vocational qualification (opistoaste)",
            :fi "Opistoasteen tutkinto",
            :sv "Yrkesinriktad examen på institutsnivå"},
           :value "1",
           :followups
           [{:id "279c1d48-e7af-407e-8a94-900a28ecd3e0",
             :text
             {:en
              "Please make sure that your degree is truly a Finnish post-secondary level degree (opistoasteen tutkinto). As a rule, these degrees are no longer available in the 2000s. For example, the degrees of a practical nurse, a commercial school graduate (merkantti) and a mechanic are not post-secondary level degrees.",
              :fi
              "Tarkistathan, että kyseessä on varmasti opistoasteen tutkinto. Näitä tutkintoja ei pääsääntöisesti ole ollut enää 2000-luvulla. Esimerkiksi lähihoitajan, merkantin ja mekaanikon tutkinnot eivät ole opistoasteen tutkintoja.",
              :sv
              "Kontrollera att det verkligen är en examen på institutnivå. Dessa examina fanns i regel inte kvar på 2000-talet. Exempelvis närvårdar-, merkant- och mekanikerexamina är inte examina på institutnivå."},
             :label {:fi ""},
             :params {},
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2020-11-18T11:39:34Z",
               :name "Risto Hanhinen"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "p",
             :fieldClass "infoElement"}]}
          {:label
           {:en "Vocational qualification (ammatillinen korkea-aste)",
            :fi "Ammatillisen korkea-asteen tutkinto",
            :sv "Yrkesinriktad examen på högre nivå"},
           :value "2",
           :followups
           [{:id "bcf190f5-d92b-46f4-a6bb-8b3825eef3b1",
             :text
             {:en
              "Please make sure that your degree is truly a Finnish higher vocational level degree. As a rule, these degrees are no longer available in the 2000s. For example, the degrees of a practical nurse, a vocational qualification in business and administration (merkonomi) and a datanome are not higher vocational level degrees.",
              :fi
              "Tarkistathan, että kyseessä on varmasti ammatillisen korkea-asteen tutkinto. Näitä tutkintoja ei pääsääntöisesti ole ollut enää 2000-luvulla. Esimerkiksi lähihoitajan, merkonomin ja datanomin tutkinnot eivät ole ammatillisen korkea-asteen tutkintoja.",
              :sv
              "Kontrollera att det verkligen är en examen inom yrkesutbildning på högre nivå. Dessa examina fanns i regel inte kvar på 2000-talet. Exempelvis närvårdar-, merkonom- och datanomexamina är inte examina inom yrkesutbildning på högre nivå."},
             :label {:fi ""},
             :params {},
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2020-11-18T11:39:34Z",
               :name "Risto Hanhinen"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "p",
             :fieldClass "infoElement"}]}],
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-11-18T10:53:39Z",
           :name "Risto Hanhinen"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "dropdown",
         :fieldClass "formField",
         :validators ["required"]}
        {:id "1bb2c136-df3e-4dc2-b3e0-4b6444cf5c89",
         :label
         {:en "Vocational qualification",
          :fi "Ammatillinen tutkinto",
          :sv "Yrkesinriktad examen"},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T08:39:05Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["required"]}
        {:id "ba47ff03-f197-48f7-9ed5-cee5a32081e3",
         :label
         {:en "Scope of vocational qualification",
          :fi "Ammatillisen tutkinnon laajuus",
          :sv "Omfattning av yrkesinriktad examen"},
         :params {:size "S", :numeric true, :decimals 1},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T07:45:15Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["numeric" "required"]}
        {:id "e02bc848-4549-4133-960c-29300dd0b4f1",
         :label
         {:en "The scope unit",
          :fi "Laajuuden yksikkö",
          :sv "Omfattningens enhet"},
         :params {},
         :options
         [{:label {:en "Courses", :fi "Kurssia", :sv "Kurser"},
           :value "0"}
          {:label
           {:en "ECTS credits",
            :fi "Opintopistettä",
            :sv "Studiepoäng"},
           :value "1"}
          {:label
           {:en "Study weeks",
            :fi "Opintoviikkoa",
            :sv "Studieveckor"},
           :value "2"}
          {:label
           {:en "Competence points",
            :fi "Osaamispistettä",
            :sv "Kompetenspoäng"},
           :value "3"}
          {:label {:en "Hours", :fi "Tuntia", :sv "Timmar"},
           :value "4"}
          {:label
           {:en "Weekly lessons per year",
            :fi "Vuosiviikkotuntia",
            :sv "Årsveckotimmar"},
           :value "5"}
          {:label {:en "Years", :fi "Vuotta", :sv "År"}, :value "6"}],
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T07:46:27Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "dropdown",
         :fieldClass "formField",
         :validators ["required"]}
        {:params
         {:info-text
          {:label
           {:en
            "Select \"Unknown\", if the educational institution where you have completed your degree cannot be found on the list.",
            :fi
            "Valitse \"Tuntematon\", jos et löydä listasta oppilaitosta, jossa olet suorittanut tutkintosi.",
            :sv
            "Välj \"Okänd\" om du inte hittar den läroanstalt, där du har avlagt din examen."}}},
         :koodisto-source
         {:uri "oppilaitostyyppi",
          :title "Ammatilliset oppilaitokset",
          :version 1,
          :allow-invalid? true},
         :validators ["required"],
         :fieldClass "formField",
         :label
         {:en "Educational institution",
          :fi "Oppilaitos ",
          :sv "Läroanstalt "},
         :id "ba9827c0-0cff-4af0-b8b0-2ff19624340c",
         :options [],
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T07:51:01Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "dropdown"}
        {:belongs-to-hakukohteet [],
         :params
         {:hidden false,
          :deadline nil,
          :info-text
          {:value
           {:en
            "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
            :fi
            "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
            :sv
            "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
           :enabled? true}},
         :belongs-to-hakukohderyhma [],
         :fieldClass "formField",
         :label
         {:en
          "Vocational qualification diploma (kouluaste, opistoaste, ammatillinen korkea-aste",
          :fi
          "Kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkintotodistus",
          :sv
          "Betyg från yrkesinriktad examen på skolnivå, examen på institutsnivå eller yrkesinriktad examen på högre nivå"},
         :id "fd8921d9-5c28-40f0-8a40-8f2107a202cd",
         :options [],
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T07:41:02Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.83554265088",
           :date "2022-09-08T12:22:16Z",
           :name "Minea Wilo-Tanninen"}},
         :fieldType "attachment"}
        {:id "43a5ee3b-1a3d-42e0-ac8e-6ea5a8b0a1ee",
         :text {:fi ""},
         :label
         {:en "Click add if you want to add further qualifications.",
          :fi "Paina lisää, jos haluat lisätä useampia tutkintoja.",
          :sv
          "Tryck på lägg till om du vill lägga till flera examina."},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T07:49:39Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "p",
         :fieldClass "infoElement"}],
       :metadata
       {:locked false,
        :created-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2020-10-15T08:18:53Z",
         :name "Risto Hanhinen"},
        :modified-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2021-07-27T07:07:20Z",
         :name "Stefan Hanhinen"}},
       :fieldType "fieldset",
       :fieldClass "questionGroup"}]}
    {:label
     {:en
      "Upper secondary education completed outside Finland (general or vocational)",
      :fi
      "Muualla kuin Suomessa suoritettu muu tutkinto, joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun",
      :sv
      "Övrig examen som avlagts annanstans än i Finland, och ger behörighet för högskolestudier i ifrågavarande land"},
     :value "pohjakoulutus_ulk",
     :followups
     [{:id "f58d1925-6d30-4762-b19b-cda36ea8f805",
       :label {:fi "", :sv ""},
       :params {},
       :children
       [{:id "pohjakoulutus_ulk--year-of-completion",
         :label
         {:en "Year of completion",
          :fi "Suoritusvuosi",
          :sv "Avlagd år"},
         :params
         {:size "S",
          :numeric true,
          :max-value "2022",
          :min-value "1900"},
         :options
         [{:label {:fi "", :sv ""},
           :value "0",
           :condition
           {:answer-compared-to 2022, :comparison-operator "="},
           :followups
           [{:id "5471de64-e457-48b4-8398-de5d677481ce",
             :label
             {:en "Have you graduated",
              :fi "Oletko valmistunut?",
              :sv "Har du tagit examen"},
             :params {},
             :options
             [{:label {:en "Yes", :fi "Kyllä", :sv "Ja"},
               :value "0",
               :followups
               [{:belongs-to-hakukohteet [],
                 :params
                 {:hidden false,
                  :deadline nil,
                  :info-text
                  {:value
                   {:en
                    "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                    :fi
                    "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                    :sv
                    "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                   :enabled? true}},
                 :belongs-to-hakukohderyhma [],
                 :fieldClass "formField",
                 :label
                 {:en "Upper secondary education diploma",
                  :fi
                  "Tutkintotodistus muualla kuin Suomessa suoritetusta tutkinnosta, joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun",
                  :sv
                  "Examensbetyg som avlagts annanstans än i Finland och som i landet ifråga ger ansökningsbehörighet för högskola"},
                 :id "3e558416-8cde-43f5-8945-03a0636d2f89",
                 :options [],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T09:13:13Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.83554265088",
                   :date "2022-09-08T12:22:51Z",
                   :name "Minea Wilo-Tanninen"}},
                 :fieldType "attachment"}
                {:id "33750eaa-2711-46f1-8d97-c215e45e2ac2",
                 :label
                 {:en
                  "Is your original diploma in Finnish, Swedish or English?",
                  :fi
                  "Onko todistuksesi suomen-, ruotsin- tai englanninkielinen?",
                  :sv
                  "Är ditt betyg finsk-, svensk-, eller engelskspråkigt?"},
                 :params {:hidden false},
                 :options
                 [{:label {:en "Yes", :fi "Kyllä", :sv "Ja"},
                   :value "0"}
                  {:label {:en "No", :fi "Ei", :sv "Nej"},
                   :value "1",
                   :followups
                   [{:belongs-to-hakukohteet [],
                     :params
                     {:hidden false,
                      :deadline nil,
                      :info-text
                      {:value
                       {:en
                        "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                        :fi
                        "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                        :sv
                        "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. "},
                       :enabled? true}},
                     :belongs-to-hakukohderyhma [],
                     :fieldClass "formField",
                     :label
                     {:en
                      "Official translation of the diploma to Finnish, Swedish or English",
                      :fi
                      "Virallinen käännös suomeksi, ruotsiksi tai englanniksi",
                      :sv
                      "Officiell översättning av intyget till finska, svenska eller engelska"},
                     :id "e0f75567-e7b7-4b5a-9afa-185848d27cb9",
                     :options [],
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2019-11-24T12:27:19Z",
                       :name "Risto Hanhinen"},
                      :modified-by
                      {:oid "1.2.246.562.24.83554265088",
                       :date "2022-09-08T12:23:05Z",
                       :name "Minea Wilo-Tanninen"}},
                     :fieldType "attachment"}]}],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2019-11-24T12:25:00Z",
                   :name "Risto Hanhinen"},
                  :modified-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2021-07-27T07:07:20Z",
                   :name "Stefan Hanhinen"}},
                 :fieldType "singleChoice",
                 :fieldClass "formField",
                 :validators ["required"]}]}
              {:label {:en "No", :fi "En", :sv "Nej"},
               :value "1",
               :followups
               [{:id "b2d8c2a5-acf9-4df1-9b12-6250351f336b",
                 :label
                 {:en "Estimated graduation date (dd.mm.yyyy)",
                  :fi "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)",
                  :sv "Beräknat examensdatum (dd.mm.åååå)"},
                 :params {:size "S", :numeric false, :decimals nil},
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T08:02:07Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2021-07-27T07:07:20Z",
                   :name "Stefan Hanhinen"}},
                 :fieldType "textField",
                 :fieldClass "formField",
                 :validators ["required"]}
                {:belongs-to-hakukohteet [],
                 :params
                 {:hidden false,
                  :deadline nil,
                  :info-text
                  {:value
                   {:en
                    "Attach a transcript of study records or other certificate provided by the learning institution that states the degree/qualification you are completing. It should also state the scope of the degree and estimated date of graduation. The certificate should be provided with the official stamp of the educational institution and must be signed and clarified by an educational institution representative including a representative's title.\n\nSubmit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                    :fi
                    "Tallenna liitteeksi opintosuoritusote tai muu oppilaitoksen myöntämä todistus, josta käy ilmi, mitä tutkintoa/oppimäärää suoritat. Todistuksesta tulee näkyä tutkinnon laajuus ja arvioitu valmistumisaika ja siinä tulee olla oppilaitoksen virallinen leima sekä oppilaitoksen edustajan allekirjoitus, nimenselvennys sekä virkanimike.\n\nTallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                    :sv
                    "Spara som bilaga till ansökan studieutdraget eller ett annat intyg från läroanstalten som visar vilken examen du avlägger. Av intyget måste framgå examens omfattning och när den beräknas bli färdig. Intyget bör vara försett med läroanstaltens officiella stämpel samt vara undertecknat och namnförtydligat av en läroanstaltsrepresentant inklusive tjänstebeteckning.\n\nSpara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                   :enabled? true}},
                 :belongs-to-hakukohderyhma [],
                 :fieldClass "formField",
                 :label
                 {:en
                  "Latest transcript of study records (upper secondary education diploma)",
                  :fi
                  "Ennakkoarvio tai viimeisin todistus suoritetuista opinnoista muualla kuin Suomessa suoritettavasta toisen asteen tutkinnosta",
                  :sv
                  "Förhandsexamensbetyg eller betyg över slutförda studier om examen som avlagts annanstans än i Finland och som i landet ifråga ger ansökningsbehörighet för högskola"},
                 :id "cc036f3b-7f3d-489a-b14d-698ccb7d90ac",
                 :options [],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T08:01:17Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.83554265088",
                   :date "2022-09-08T12:23:17Z",
                   :name "Minea Wilo-Tanninen"}},
                 :fieldType "attachment"}
                {:belongs-to-hakukohteet [],
                 :params
                 {:hidden false,
                  :deadline nil,
                  :info-text
                  {:value
                   {:en
                    "The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                    :fi
                    "Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                    :sv
                    "Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                   :enabled? true}},
                 :belongs-to-hakukohderyhma [],
                 :fieldClass "formField",
                 :label
                 {:en "Original upper secondary education diploma",
                  :fi
                  "Tutkintotodistus muualla kuin Suomessa suoritetusta tutkinnosta, joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun",
                  :sv
                  "Examensbetyg som avlagts annanstans än i Finland och som i landet ifråga ger ansökningsbehörighet för högskola"},
                 :id "557a62bb-d272-4437-8b2f-74b3e850b007",
                 :options [],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T09:13:13Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.83554265088",
                   :date "2022-09-08T12:23:25Z",
                   :name "Minea Wilo-Tanninen"}},
                 :fieldType "attachment"}
                {:id "29cce110-6360-4c07-b338-2fe3cdf90259",
                 :label
                 {:en
                  "Are your attachments in Finnish, Swedish or English?",
                  :fi
                  "Ovatko liitteesi suomen-, ruotsin- tai englanninkielisiä?",
                  :sv
                  "Är dina bilagor finsk-, svensk-, eller engelskspråkiga?"},
                 :params {:hidden false},
                 :options
                 [{:label {:en "Yes", :fi "Kyllä", :sv "Ja"},
                   :value "0"}
                  {:label {:en "No", :fi "Ei", :sv "Nej"},
                   :value "1",
                   :followups
                   [{:belongs-to-hakukohteet [],
                     :params
                     {:hidden false,
                      :deadline nil,
                      :info-text
                      {:value
                       {:en
                        "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                        :fi
                        "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                        :sv
                        "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                       :enabled? true}},
                     :belongs-to-hakukohderyhma [],
                     :fieldClass "formField",
                     :label
                     {:en
                      "Official translation of the latest transcript of study records to Finnish, Swedish or English",
                      :fi
                      "Virallinen käännös ennakkoarviosta tai viimeisimmästä todistuksestasi suomeksi, ruotsiksi tai englanniksi",
                      :sv
                      "Officiell översättning av förhandsexamensbetyget eller betyget över slutförda studier till finska, svenska eller engelska"},
                     :id "8cc1e9fc-c087-40fd-a618-184a3e78e106",
                     :options [],
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2019-11-24T12:27:19Z",
                       :name "Risto Hanhinen"},
                      :modified-by
                      {:oid "1.2.246.562.24.83554265088",
                       :date "2022-09-08T12:23:39Z",
                       :name "Minea Wilo-Tanninen"}},
                     :fieldType "attachment"}
                    {:belongs-to-hakukohteet [],
                     :params
                     {:hidden false,
                      :deadline nil,
                      :info-text
                      {:value
                       {:en
                        "The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                        :fi
                        "Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                        :sv
                        "Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                       :enabled? true}},
                     :belongs-to-hakukohderyhma [],
                     :fieldClass "formField",
                     :label
                     {:en
                      "Official translation of the diploma to Finnish, Swedish or English",
                      :fi
                      "Virallinen käännös tutkintotodistuksesta suomeksi, ruotsiksi tai englanniksi",
                      :sv
                      "Officiell översättning av examensbetyget till finska, svenska eller engelska"},
                     :id "a901e8ea-1ac0-4d5a-adfa-5bfb68268a6b",
                     :options [],
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2019-11-24T12:27:19Z",
                       :name "Risto Hanhinen"},
                      :modified-by
                      {:oid "1.2.246.562.24.83554265088",
                       :date "2022-09-08T12:23:47Z",
                       :name "Minea Wilo-Tanninen"}},
                     :fieldType "attachment"}]}],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2019-11-24T12:25:00Z",
                   :name "Risto Hanhinen"},
                  :modified-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2021-07-27T07:07:20Z",
                   :name "Stefan Hanhinen"}},
                 :fieldType "singleChoice",
                 :fieldClass "formField",
                 :validators ["required"]}]}],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2020-06-29T07:59:53Z",
               :name "Anonymisoitu Virkailija"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "singleChoice",
             :fieldClass "formField",
             :validators ["required"]}]}
          {:label {:fi "", :sv ""},
           :value "1",
           :condition
           {:answer-compared-to 2022, :comparison-operator "<"},
           :followups
           [{:belongs-to-hakukohteet [],
             :params
             {:hidden false,
              :deadline nil,
              :info-text
              {:value
               {:en
                "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                :fi
                "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
               :enabled? true}},
             :belongs-to-hakukohderyhma [],
             :fieldClass "formField",
             :label
             {:en "Upper secondary education diploma",
              :fi
              "Tutkintotodistus muualla kuin Suomessa suoritetusta tutkinnosta, joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun",
              :sv
              "Examensbetyg som avlagts annanstans än i Finland och som i landet ifråga ger ansökningsbehörighet för högskola"},
             :id "4f15f579-fbf0-4d38-baf6-de706e49acf4",
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2020-06-29T09:13:13Z",
               :name "Anonymisoitu Virkailija"},
              :modified-by
              {:oid "1.2.246.562.24.83554265088",
               :date "2022-09-08T12:24:09Z",
               :name "Minea Wilo-Tanninen"}},
             :fieldType "attachment"}
            {:id "d9ff0887-e4b1-4472-a7dc-3720dab980e4",
             :label
             {:en
              "Are your attachments in Finnish, Swedish or English?",
              :fi
              "Ovatko liitteesi suomen-, ruotsin- tai englanninkielisiä?",
              :sv
              "Är dina bilagor finsk-, svensk-, eller engelskspråkiga?"},
             :params {:hidden false},
             :options
             [{:label {:en "Yes", :fi "Kyllä", :sv "Ja"}, :value "0"}
              {:label {:en "No", :fi "Ei", :sv "Nej"},
               :value "1",
               :followups
               [{:belongs-to-hakukohteet [],
                 :params
                 {:hidden false,
                  :deadline nil,
                  :info-text
                  {:value
                   {:en
                    "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                    :fi
                    "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                    :sv
                    "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. "},
                   :enabled? true}},
                 :belongs-to-hakukohderyhma [],
                 :fieldClass "formField",
                 :label
                 {:en
                  "Official translation of the diploma to Finnish, Swedish or English",
                  :fi
                  "Virallinen käännös suomeksi, ruotsiksi tai englanniksi",
                  :sv
                  "Officiell översättning av intyget till finska, svenska eller engelska"},
                 :id "80d1a1da-2875-44bf-8aa4-e838480b3936",
                 :options [],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2019-11-24T12:27:19Z",
                   :name "Risto Hanhinen"},
                  :modified-by
                  {:oid "1.2.246.562.24.83554265088",
                   :date "2022-09-08T12:24:24Z",
                   :name "Minea Wilo-Tanninen"}},
                 :fieldType "attachment"}]}],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2019-11-24T12:25:00Z",
               :name "Risto Hanhinen"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "singleChoice",
             :fieldClass "formField",
             :validators ["required"]}]}],
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T09:10:23Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-10-19T11:21:06Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["numeric" "required"]}
        {:id "60bc3c1f-b8b6-46e9-9502-0de45b7c94fd",
         :label {:en "Degree", :fi "Tutkinto", :sv "Examen"},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T09:10:52Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["required"]}
        {:id "1836bf0c-376b-4353-b094-9381533847c3",
         :label
         {:en "Educational institution",
          :fi "Oppilaitos",
          :sv "Läroanstalt"},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T09:12:01Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["required"]}
        {:params {},
         :koodisto-source
         {:uri "maatjavaltiot2",
          :title "Maat ja valtiot",
          :version 2,
          :allow-invalid? true},
         :validators ["required"],
         :fieldClass "formField",
         :label
         {:en "Country of completion",
          :fi "Suoritusmaa",
          :sv "Land där examen är avlagd"},
         :id "e81a48cf-09fe-4d0e-a9d9-9e48c270fc19",
         :options [],
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T09:17:51Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "dropdown"}
        {:id "d9e153fe-b295-4662-99ff-348fa6da69a4",
         :text {:fi ""},
         :label
         {:en "Click add if you want to add further qualifications.",
          :fi "Paina lisää, jos haluat lisätä useampia tutkintoja.",
          :sv
          "Tryck på lägg till om du vill lägga till flera examina."},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T07:49:39Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "p",
         :fieldClass "infoElement"}],
       :metadata
       {:locked false,
        :created-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2020-06-29T09:17:14Z",
         :name "Anonymisoitu Virkailija"},
        :modified-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2021-07-27T07:07:20Z",
         :name "Stefan Hanhinen"}},
       :fieldType "fieldset",
       :fieldClass "questionGroup"}]}
    {:label
     {:en
      "International matriculation examination completed outside Finland (IB, EB and RP/DIA)",
      :fi
      "Muualla kuin Suomessa suoritettu kansainvälinen ylioppilastutkinto (IB, EB ja RP/DIA)",
      :sv
      "Internationell studentexamen som avlagts annanstans än i Finland (IB, EB och RP/DIA)"},
     :value "pohjakoulutus_yo_ulkomainen",
     :followups
     [{:id "b2e34efe-43ca-40c4-82bb-616a8335da08",
       :label {:fi "", :sv ""},
       :params {},
       :children
       [{:id "pohjakoulutus_yo_ulkomainen--year-of-completion",
         :label
         {:en "Year of completion",
          :fi "Suoritusvuosi",
          :sv "Avlagd år"},
         :params
         {:size "S",
          :numeric true,
          :max-value "2022",
          :min-value "1900"},
         :options
         [{:label {:fi "", :sv ""},
           :value "0",
           :condition
           {:answer-compared-to 2022, :comparison-operator "<"},
           :followups
           [{:id "057d2228-903c-4faf-8c7f-0fb5bf438573",
             :label
             {:en "Matriculation examination",
              :fi "Ylioppilastutkinto",
              :sv "Studentexamen"},
             :params {},
             :options
             [{:label
               {:en "International Baccalaureate -diploma",
                :fi "International Baccalaureate -tutkinto",
                :sv "International Baccalaureate -examen"},
               :value "0",
               :followups
               [{:belongs-to-hakukohteet [],
                 :params
                 {:hidden false,
                  :deadline nil,
                  :info-text
                  {:value
                   {:en
                    "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                    :fi
                    "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                    :sv
                    "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                   :enabled? true}},
                 :belongs-to-hakukohderyhma [],
                 :fieldClass "formField",
                 :label
                 {:en "IB Diploma completed outside Finland",
                  :fi
                  "IB Diploma -tutkintotodistus muualla kuin Suomessa suoritetusta tutkinnosta",
                  :sv
                  "IB Diploma från IB-studentexamen som avlagts annanstans än i Finland"},
                 :id "85aafe77-c70e-4a81-847b-925dd930765d",
                 :options [],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T08:47:08Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.83554265088",
                   :date "2022-09-08T12:24:52Z",
                   :name "Minea Wilo-Tanninen"}},
                 :fieldType "attachment"}]}
              {:label
               {:en "European Baccalaureate -diploma",
                :fi "European Baccalaureate -tutkinto",
                :sv "European Baccalaureate -examen"},
               :value "1",
               :followups
               [{:belongs-to-hakukohteet [],
                 :params
                 {:hidden false,
                  :deadline nil,
                  :info-text
                  {:value
                   {:en
                    "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                    :fi
                    "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                    :sv
                    "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                   :enabled? true}},
                 :belongs-to-hakukohderyhma [],
                 :fieldClass "formField",
                 :label
                 {:en
                  "European Baccalaureate diploma completed outside Finland",
                  :fi
                  "European Baccalaureate Diploma -tutkintotodistus muualla kuin Suomessa suoritetusta tutkinnosta",
                  :sv
                  "European Baccalaureate Diploma från EB-studentexamen som avlagts annanstans än i Finland"},
                 :id "50398654-16bf-49b2-8858-353d9765364b",
                 :options [],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T08:47:08Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.83554265088",
                   :date "2022-09-08T12:25:09Z",
                   :name "Minea Wilo-Tanninen"}},
                 :fieldType "attachment"}]}
              {:label
               {:en
                "Deutsche Internationale Abiturprüfung/Reifeprüfung -diploma",
                :fi
                "Deutsche Internationale Abiturprüfung/Reifeprüfung -tutkinto",
                :sv
                "Deutsche Internationale Abiturprüfung/Reifeprüfung -examen"},
               :value "2",
               :followups
               [{:belongs-to-hakukohteet [],
                 :params
                 {:hidden false,
                  :deadline nil,
                  :info-text
                  {:value
                   {:en
                    "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                    :fi
                    "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                    :sv
                    "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                   :enabled? true}},
                 :belongs-to-hakukohderyhma [],
                 :fieldClass "formField",
                 :label
                 {:en
                  "Reifeprüfung/DIA diploma completed outside Finland",
                  :fi
                  "Reifeprüfung/DIA-tutkintotodistus muualla kuin Suomessa suoritetusta tutkinnosta",
                  :sv
                  "Reifeprüfung/DIA -examensbetyg from RP/DIA-studentexamen som avlagts annanstans än i Finland"},
                 :id "70fef327-87d2-43e3-8d16-b5f3f7ec0e8b",
                 :options [],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T08:47:08Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.83554265088",
                   :date "2022-09-08T12:25:25Z",
                   :name "Minea Wilo-Tanninen"}},
                 :fieldType "attachment"}]}],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2020-06-29T08:44:41Z",
               :name "Anonymisoitu Virkailija"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "singleChoice",
             :fieldClass "formField",
             :validators ["required"]}]}
          {:label {:fi "", :sv ""},
           :value "1",
           :condition
           {:answer-compared-to 2022, :comparison-operator "="},
           :followups
           [{:id "8e9bd16b-f671-4f5f-933b-e0a906e3cb45",
             :label
             {:en "Matriculation examination",
              :fi "Ylioppilastutkinto",
              :sv "Studentexamen"},
             :params {},
             :options
             [{:label
               {:en "International Baccalaureate -diploma",
                :fi "International Baccalaureate -tutkinto",
                :sv "International Baccalaureate -examen"},
               :value "0",
               :followups
               [{:id "c517d740-41f6-40ce-91e5-27267e24047e",
                 :label
                 {:en "Have you graduated",
                  :fi "Oletko valmistunut?",
                  :sv "Har du tagit examen"},
                 :params {},
                 :options
                 [{:label {:en "Yes", :fi "Kyllä", :sv "Ja"},
                   :value "0",
                   :followups
                   [{:belongs-to-hakukohteet [],
                     :params
                     {:hidden false,
                      :deadline nil,
                      :info-text
                      {:value
                       {:en
                        "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                        :fi
                        "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                        :sv
                        "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                       :enabled? true}},
                     :belongs-to-hakukohderyhma [],
                     :fieldClass "formField",
                     :label
                     {:en "IB Diploma completed outside Finland",
                      :fi
                      "IB Diploma -tutkintotodistus muualla kuin Suomessa suoritetusta tutkinnosta",
                      :sv
                      "IB Diploma från IB-studentexamen som avlagts i annanstans än Finland"},
                     :id "52a4d8ef-1ed7-4b18-b8f8-92dc0c026031",
                     :options [],
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2020-06-29T08:47:08Z",
                       :name "Anonymisoitu Virkailija"},
                      :modified-by
                      {:oid "1.2.246.562.24.83554265088",
                       :date "2022-09-08T12:25:43Z",
                       :name "Minea Wilo-Tanninen"}},
                     :fieldType "attachment"}]}
                  {:label {:en "No", :fi "En", :sv "Nej"},
                   :value "1",
                   :followups
                   [{:id "eee3698b-71b9-4fd4-8d94-5be1c32af960",
                     :label
                     {:en "Estimated graduation date (dd.mm.yyyy)",
                      :fi
                      "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)",
                      :sv "Beräknat examensdatum (dd.mm.åååå)"},
                     :params
                     {:size "S", :numeric false, :decimals nil},
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2020-06-29T08:02:07Z",
                       :name "Anonymisoitu Virkailija"},
                      :modified-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2021-07-27T07:07:20Z",
                       :name "Stefan Hanhinen"}},
                     :fieldType "textField",
                     :fieldClass "formField",
                     :validators ["required"]}
                    {:belongs-to-hakukohteet [],
                     :params
                     {:hidden false,
                      :deadline nil,
                      :info-text
                      {:value
                       {:en
                        "The attachment has to be submitted  by the deadline informed next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n",
                        :fi
                        "Määräaika ilmoitetaan liitepyynnön vieressä. \n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                        :sv
                        "Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
                       :enabled? true}},
                     :belongs-to-hakukohderyhma [],
                     :fieldClass "formField",
                     :label
                     {:en
                      "Predicted grades from IB completed outside Finland",
                      :fi
                      "Oppilaitoksen ennakkoarvio muualla kuin Suomessa suoritettavan tutkinnon arvosanoista (Candidate Predicted Grades)",
                      :sv
                      "Predicted grades från IB-studentexamen som avlagts annanstans än i Finland "},
                     :id "b3b4d5f4-e594-49e1-9abe-be692a88e2c3",
                     :options [],
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2020-06-29T08:01:17Z",
                       :name "Anonymisoitu Virkailija"},
                      :modified-by
                      {:oid "1.2.246.562.24.83554265088",
                       :date "2022-09-08T12:25:58Z",
                       :name "Minea Wilo-Tanninen"}},
                     :fieldType "attachment"}
                    {:belongs-to-hakukohteet [],
                     :params
                     {:hidden false,
                      :deadline nil,
                      :info-text
                      {:value
                       {:en
                        "The attachment has to be submitted  by the deadline informed next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n",
                        :fi
                        "Määräaika ilmoitetaan liitepyynnön vieressä. \n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                        :sv
                        "Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
                       :enabled? true}},
                     :belongs-to-hakukohderyhma [],
                     :fieldClass "formField",
                     :label
                     {:en
                      "Diploma Programme (DP) Results from IB completed outside Finland",
                      :fi
                      "Diploma Programme (DP) Results -asiakirja muualla kuin Suomessa suoritettavasta tutkinnosta",
                      :sv
                      "Diploma Programme (DP) Results från IB-studentexamen som avlagts annanstans än i Finland"},
                     :id "8cededc3-7e73-4f3f-bb58-952202c4e728",
                     :options [],
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2020-06-29T08:48:35Z",
                       :name "Anonymisoitu Virkailija"},
                      :modified-by
                      {:oid "1.2.246.562.24.83554265088",
                       :date "2022-09-08T12:26:06Z",
                       :name "Minea Wilo-Tanninen"}},
                     :fieldType "attachment"}]}],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T07:59:53Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2021-07-27T07:07:20Z",
                   :name "Stefan Hanhinen"}},
                 :fieldType "singleChoice",
                 :fieldClass "formField",
                 :validators ["required"]}]}
              {:label
               {:en "European Baccalaureate -diploma",
                :fi "Eurooppalainen ylioppilastutkinto",
                :sv "European Baccalaureate -examen"},
               :value "1",
               :followups
               [{:id "0934f121-e3c5-4d49-a1ff-78b62b394e0a",
                 :label
                 {:en "Have you graduated",
                  :fi "Oletko valmistunut?",
                  :sv "Har du tagit examen"},
                 :params {},
                 :options
                 [{:label {:en "Yes", :fi "Kyllä", :sv "Ja"},
                   :value "0",
                   :followups
                   [{:belongs-to-hakukohteet [],
                     :params
                     {:hidden false,
                      :deadline nil,
                      :info-text
                      {:value
                       {:en
                        "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                        :fi
                        "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                        :sv
                        "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                       :enabled? true}},
                     :belongs-to-hakukohderyhma [],
                     :fieldClass "formField",
                     :label
                     {:en
                      "European Baccalaureate diploma completed outside Finland",
                      :fi
                      "European Baccalaureate Diploma -tutkintotodistus muualla kuin Suomessa suoritetusta tutkinnosta",
                      :sv
                      "European Baccalaureate Diploma från EB-studentexamen som avlagts annanstans än i Finland"},
                     :id "33a7e336-ed21-4787-897e-f4370f19d462",
                     :options [],
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2020-06-29T08:47:08Z",
                       :name "Anonymisoitu Virkailija"},
                      :modified-by
                      {:oid "1.2.246.562.24.83554265088",
                       :date "2022-09-08T12:26:22Z",
                       :name "Minea Wilo-Tanninen"}},
                     :fieldType "attachment"}]}
                  {:label {:en "No", :fi "En", :sv "Nej"},
                   :value "1",
                   :followups
                   [{:id "9a8e77ee-4194-4464-9068-47f3c3836e7a",
                     :label
                     {:en "Estimated graduation date (dd.mm.yyyy)",
                      :fi
                      "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)",
                      :sv "Beräknat examensdatum (dd.mm.åååå)"},
                     :params
                     {:size "S", :numeric false, :decimals nil},
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2020-06-29T08:02:07Z",
                       :name "Anonymisoitu Virkailija"},
                      :modified-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2021-07-27T07:07:20Z",
                       :name "Stefan Hanhinen"}},
                     :fieldType "textField",
                     :fieldClass "formField",
                     :validators ["required"]}
                    {:belongs-to-hakukohteet [],
                     :params
                     {:hidden false,
                      :deadline nil,
                      :info-text
                      {:value
                       {:en
                        "The attachment has to be submitted  by the deadline informed next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n",
                        :fi
                        "Määräaika ilmoitetaan liitepyynnön vieressä. \n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                        :sv
                        "Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
                       :enabled? true}},
                     :belongs-to-hakukohderyhma [],
                     :fieldClass "formField",
                     :label
                     {:en
                      "Predicted grades from EB completed outside Finland",
                      :fi
                      "Oppilaitoksen ennakkoarvio muualla kuin Suomessa suoritettavan EB-tutkinnon arvosanoista",
                      :sv
                      "Läroanstaltens preliminära vitsord från EB-studentexamen som avlagts annanstans än i Finland"},
                     :id "46133438-bc8c-43c6-b47f-7f650d88498c",
                     :options [],
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2020-06-29T08:01:17Z",
                       :name "Anonymisoitu Virkailija"},
                      :modified-by
                      {:oid "1.2.246.562.24.83554265088",
                       :date "2022-09-08T12:26:40Z",
                       :name "Minea Wilo-Tanninen"}},
                     :fieldType "attachment"}
                    {:belongs-to-hakukohteet [],
                     :params
                     {:hidden false,
                      :deadline nil,
                      :info-text
                      {:value
                       {:en
                        "The attachment has to be submitted  by the deadline informed next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n",
                        :fi
                        "Määräaika ilmoitetaan liitepyynnön vieressä. \n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                        :sv
                        "Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
                       :enabled? true}},
                     :belongs-to-hakukohderyhma [],
                     :fieldClass "formField",
                     :label
                     {:en
                      "European Baccalaureate diploma completed outside Finland",
                      :fi
                      "European Baccalaureate Diploma -tutkintotodistus muualla kuin Suomessa suoritettavasta tutkinnosta",
                      :sv
                      "European Baccalaureate Diploma från EB-studentexamen som avlagts annanstans än i Finland"},
                     :id "961a59e7-ddcc-4ffa-9c69-7df50ae932f3",
                     :options [],
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2020-06-29T08:48:35Z",
                       :name "Anonymisoitu Virkailija"},
                      :modified-by
                      {:oid "1.2.246.562.24.83554265088",
                       :date "2022-09-08T12:26:49Z",
                       :name "Minea Wilo-Tanninen"}},
                     :fieldType "attachment"}]}],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T07:59:53Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2021-07-27T07:07:20Z",
                   :name "Stefan Hanhinen"}},
                 :fieldType "singleChoice",
                 :fieldClass "formField",
                 :validators ["required"]}]}
              {:label
               {:en
                "Deutsche Internationale Abiturprüfung/Reifeprüfung -diploma",
                :fi
                "Deutsche Internationale Abiturprüfung/Reifeprüfung -tutkinto",
                :sv
                "Deutsche Internationale Abiturprüfung/Reifeprüfung -examen"},
               :value "2",
               :followups
               [{:id "f2e95435-79a2-4668-90bf-8d74afd7111a",
                 :label
                 {:en "Have you graduated",
                  :fi "Oletko valmistunut?",
                  :sv "Har du tagit examen"},
                 :params {},
                 :options
                 [{:label {:en "Yes", :fi "Kyllä", :sv "Ja"},
                   :value "0",
                   :followups
                   [{:belongs-to-hakukohteet [],
                     :params
                     {:hidden false,
                      :deadline nil,
                      :info-text
                      {:value
                       {:en
                        "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                        :fi
                        "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                        :sv
                        "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                       :enabled? true}},
                     :belongs-to-hakukohderyhma [],
                     :fieldClass "formField",
                     :label
                     {:en
                      "Reifeprüfung/DIA diploma from RP/DIA completed outside Finland",
                      :fi
                      "Reifeprüfung/DIA-tutkintotodistus muualla kuin Suomessa suoritetusta tutkinnosta",
                      :sv
                      "Reifeprüfung/DIA -examensbetyg from RP/DIA-studentexamen som avlagts annanstans än i Finland"},
                     :id "693b0ce3-8e12-4c18-9d21-91216238cf12",
                     :options [],
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2020-06-29T08:47:08Z",
                       :name "Anonymisoitu Virkailija"},
                      :modified-by
                      {:oid "1.2.246.562.24.83554265088",
                       :date "2022-09-08T12:27:04Z",
                       :name "Minea Wilo-Tanninen"}},
                     :fieldType "attachment"}]}
                  {:label {:en "No", :fi "En", :sv "Nej"},
                   :value "1",
                   :followups
                   [{:id "b4b23cd0-1404-479b-9e65-d58715924bea",
                     :label
                     {:en "Estimated graduation date (dd.mm.yyyy)",
                      :fi
                      "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)",
                      :sv "Beräknat examensdatum (dd.mm.åååå)"},
                     :params
                     {:size "S", :numeric false, :decimals nil},
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2020-06-29T08:02:07Z",
                       :name "Anonymisoitu Virkailija"},
                      :modified-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2021-07-27T07:07:20Z",
                       :name "Stefan Hanhinen"}},
                     :fieldType "textField",
                     :fieldClass "formField",
                     :validators ["required"]}
                    {:belongs-to-hakukohteet [],
                     :params
                     {:hidden false,
                      :deadline nil,
                      :info-text
                      {:value
                       {:en
                        "The attachment has to be submitted  by the deadline informed next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n",
                        :fi
                        "Määräaika ilmoitetaan liitepyynnön vieressä. \n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                        :sv
                        "Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
                       :enabled? true}},
                     :belongs-to-hakukohderyhma [],
                     :fieldClass "formField",
                     :label
                     {:en
                      "Grade page of a Deutsches Internationales Abitur (DIA) diploma completed outside Finland",
                      :fi
                      "DIA-tutkintotodistuksen arvosanasivu muualla kuin Suomessa suoritettavasta tutkinnosta",
                      :sv
                      "Examensbetygets vitsordssida av Deutsches Internationales Abitur (DIA) -examen avlagd annanstans än i Finland"},
                     :id "ba31413b-06b4-4137-b7b1-0b3d90d2f53d",
                     :options [],
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2020-06-29T08:01:17Z",
                       :name "Anonymisoitu Virkailija"},
                      :modified-by
                      {:oid "1.2.246.562.24.83554265088",
                       :date "2022-09-08T12:27:16Z",
                       :name "Minea Wilo-Tanninen"}},
                     :fieldType "attachment"}
                    {:belongs-to-hakukohteet [],
                     :params
                     {:hidden false,
                      :deadline nil,
                      :info-text
                      {:value
                       {:en
                        "The attachment has to be submitted  by the deadline informed next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n",
                        :fi
                        "Määräaika ilmoitetaan liitepyynnön vieressä. \n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
                        :sv
                        "Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
                       :enabled? true}},
                     :belongs-to-hakukohderyhma [],
                     :fieldClass "formField",
                     :label
                     {:en
                      "DIA -diploma from DIA completed outside Finland",
                      :fi
                      "DIA-tutkintotodistus muualla kuin Suomessa suoritettavasta tutkinnosta",
                      :sv
                      "DIA -examensbetyg från DIA-studentexamen som avlagts annanstans än i Finland"},
                     :id "f3b4b91c-755a-4af4-b548-f07c274bab1b",
                     :options [],
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2020-06-29T08:48:35Z",
                       :name "Anonymisoitu Virkailija"},
                      :modified-by
                      {:oid "1.2.246.562.24.83554265088",
                       :date "2022-09-08T12:27:22Z",
                       :name "Minea Wilo-Tanninen"}},
                     :fieldType "attachment"}]}],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T07:59:53Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2021-07-27T07:07:20Z",
                   :name "Stefan Hanhinen"}},
                 :fieldType "singleChoice",
                 :fieldClass "formField",
                 :validators ["required"]}]}],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2020-06-29T08:44:41Z",
               :name "Anonymisoitu Virkailija"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "singleChoice",
             :fieldClass "formField",
             :validators ["required"]}]}],
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T10:07:39Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-10-19T11:47:18Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["numeric" "required"]}
        {:id "be37ebd5-9a99-48e2-9891-d6e2c36fcc03",
         :label
         {:en "Educational institution",
          :fi "Oppilaitos",
          :sv "Läroanstalt"},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T08:51:11Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["required"]}
        {:params {},
         :koodisto-source
         {:uri "maatjavaltiot2",
          :title "Maat ja valtiot",
          :version 2,
          :allow-invalid? true},
         :validators ["required"],
         :fieldClass "formField",
         :label
         {:en "Country of completion",
          :fi "Suoritusmaa",
          :sv "Land där examen är avlagd"},
         :id "bd6353ce-763d-4799-9b28-904d69c52c5e",
         :options [],
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T09:37:13Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "dropdown"}
        {:id "1ff26522-418d-4026-94e8-684afaeaddf1",
         :text {:fi ""},
         :label
         {:en "Click add if you want to add further qualifications.",
          :fi "Paina lisää, jos haluat lisätä useampia tutkintoja.",
          :sv
          "Tryck på lägg till om du vill lägga till flera examina."},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T07:49:39Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "p",
         :fieldClass "infoElement"}],
       :metadata
       {:locked false,
        :created-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2020-06-29T09:33:48Z",
         :name "Anonymisoitu Virkailija"},
        :modified-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2021-07-27T07:07:20Z",
         :name "Stefan Hanhinen"}},
       :fieldType "fieldset",
       :fieldClass "questionGroup"}]}
    {:label
     {:en
      "Bachelor’s/Master’s/Doctoral degree completed outside Finland",
      :fi "Muualla kuin Suomessa suoritettu korkeakoulututkinto",
      :sv "Högskoleexamen som avlagts annanstans än i Finland"},
     :value "pohjakoulutus_kk_ulk",
     :followups
     [{:id "5530be0d-037f-4233-a480-3fe121c30b29",
       :label {:fi "", :sv ""},
       :params {},
       :children
       [{:id "pohjakoulutus_kk_ulk--year-of-completion",
         :label
         {:en "Year of completion",
          :fi "Suoritusvuosi",
          :sv "Avlagd år"},
         :params
         {:size "S",
          :numeric true,
          :max-value "2022",
          :min-value "1900"},
         :options
         [{:label {:fi "", :sv ""},
           :value "0",
           :condition
           {:answer-compared-to 2022, :comparison-operator "="},
           :followups
           [{:id "18094879-ae44-4498-860e-1a23ffcbbebb",
             :label
             {:en "Have you graduated?",
              :fi "Oletko valmistunut?",
              :sv "Har du tagit examen?"},
             :params {},
             :options
             [{:label {:en "Yes", :fi "Kyllä", :sv "Ja"},
               :value "0",
               :followups
               [{:belongs-to-hakukohteet [],
                 :params
                 {:hidden false,
                  :deadline nil,
                  :info-text
                  {:value
                   {:en
                    "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                    :fi
                    "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                    :sv
                    "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                   :enabled? true}},
                 :belongs-to-hakukohderyhma [],
                 :fieldClass "formField",
                 :label
                 {:en
                  "Transcript of records of higher education degree completed outside Finland",
                  :fi
                  "Opintosuoritusote muualla kuin Suomessa suoritettuun korkeakoulututkintoon sisältyvistä opinnoista",
                  :sv
                  "Studieprestationsutdrag om högskoleexamen som avlagts annanstans än i Finland"},
                 :id "71e40e75-60f9-40f7-8f28-842c0d72535f",
                 :options [],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T09:13:13Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.83554265088",
                   :date "2022-09-08T12:27:59Z",
                   :name "Minea Wilo-Tanninen"}},
                 :fieldType "attachment"}
                {:belongs-to-hakukohteet [],
                 :params
                 {:hidden false,
                  :deadline nil,
                  :info-text
                  {:value
                   {:en
                    "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                    :fi
                    "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                    :sv
                    "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. "},
                   :enabled? true}},
                 :belongs-to-hakukohderyhma [],
                 :fieldClass "formField",
                 :label
                 {:en "Higher education degree certificate",
                  :fi
                  "Muualla kuin Suomessa suoritetun korkeakoulututkinnon tutkintotodistus",
                  :sv
                  "Högskoleexamensbetyg som avlagts annanstans än i Finland"},
                 :id "0a3ba565-5aa8-4224-b5e1-365c2efb27f3",
                 :options [],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T09:13:41Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.83554265088",
                   :date "2022-09-08T12:31:20Z",
                   :name "Minea Wilo-Tanninen"}},
                 :fieldType "attachment"}
                {:id "4cbf5af0-837c-4a22-bbca-9e7fc3447bf7",
                 :label
                 {:en
                  "Are your attachments in Finnish, Swedish or English?",
                  :fi
                  "Ovatko liitteesi suomen-, ruotsin- tai englanninkielisiä?",
                  :sv
                  "Är dina bilagor finsk-, svensk-, eller engelskspråkiga?"},
                 :params {:hidden false},
                 :options
                 [{:label {:en "Yes", :fi "Kyllä", :sv "Ja"},
                   :value "0"}
                  {:label {:en "No", :fi "Ei", :sv "Nej"},
                   :value "1",
                   :followups
                   [{:belongs-to-hakukohteet [],
                     :params
                     {:hidden false,
                      :deadline nil,
                      :info-text
                      {:value
                       {:en
                        "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                        :fi
                        "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                        :sv
                        "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. "},
                       :enabled? true}},
                     :belongs-to-hakukohderyhma [],
                     :fieldClass "formField",
                     :label
                     {:en
                      "Official translation of the certificate to Finnish, Swedish or English",
                      :fi
                      "Virallinen käännös suomeksi, ruotsiksi tai englanniksi",
                      :sv
                      "Officiell översättning av intyget till finska, svenska eller engelska"},
                     :id "2d9626e2-4e23-4dd6-b3d0-b6ea0f0e2f10",
                     :options [],
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2019-11-24T12:27:19Z",
                       :name "Risto Hanhinen"},
                      :modified-by
                      {:oid "1.2.246.562.24.83554265088",
                       :date "2022-09-08T12:31:35Z",
                       :name "Minea Wilo-Tanninen"}},
                     :fieldType "attachment"}]}],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2019-11-24T12:25:00Z",
                   :name "Risto Hanhinen"},
                  :modified-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2021-07-27T07:07:20Z",
                   :name "Stefan Hanhinen"}},
                 :fieldType "singleChoice",
                 :fieldClass "formField",
                 :validators ["required"]}]}
              {:label {:en "No", :fi "En", :sv "Nej"},
               :value "1",
               :followups
               [{:id "cf82104c-19c0-4ca1-9108-daa8e59f1ba1",
                 :label
                 {:en "Estimated graduation date (dd.mm.yyyy)",
                  :fi "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)",
                  :sv "Beräknat examensdatum (dd.mm.åååå)"},
                 :params {:size "S", :numeric false, :decimals nil},
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T08:02:07Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2021-07-27T07:07:20Z",
                   :name "Stefan Hanhinen"}},
                 :fieldType "textField",
                 :fieldClass "formField",
                 :validators ["required"]}
                {:belongs-to-hakukohteet [],
                 :params
                 {:hidden false,
                  :deadline nil,
                  :info-text
                  {:value
                   {:en
                    "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                    :fi
                    "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                    :sv
                    "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
                   :enabled? true}},
                 :belongs-to-hakukohderyhma [],
                 :fieldClass "formField",
                 :label
                 {:en
                  "Transcript of records of higher education degree completed outside Finland",
                  :fi
                  "Opintosuoritusote muualla kuin Suomessa suoritettavaan korkeakoulututkintoon sisältyvistä opinnoista",
                  :sv
                  "Studieprestationsutdrag om högskoleexamen som avlagts annanstans än i Finland"},
                 :id "dd65be68-0f97-4399-a02b-7d7d3b86f5be",
                 :options [],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T08:01:17Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.83554265088",
                   :date "2022-09-08T12:32:03Z",
                   :name "Minea Wilo-Tanninen"}},
                 :fieldType "attachment"}
                {:belongs-to-hakukohteet [],
                 :params
                 {:hidden false,
                  :deadline nil,
                  :info-text
                  {:value
                   {:en
                    "The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                    :fi
                    "Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                    :sv
                    "Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. "},
                   :enabled? true}},
                 :belongs-to-hakukohderyhma [],
                 :fieldClass "formField",
                 :label
                 {:en "Higher education degree certificate",
                  :fi
                  "Muualla kuin Suomessa suoritettavan korkeakoulututkinnon tutkintotodistus",
                  :sv
                  "Högskoleexamensbetyg som avlagts annanstans än i Finland"},
                 :id "579f4996-4fec-48e5-bb26-35f54d143dca",
                 :options [],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2020-06-29T09:13:41Z",
                   :name "Anonymisoitu Virkailija"},
                  :modified-by
                  {:oid "1.2.246.562.24.83554265088",
                   :date "2022-09-08T12:32:09Z",
                   :name "Minea Wilo-Tanninen"}},
                 :fieldType "attachment"}
                {:id "a9a0e695-c26b-4acb-90e9-3345f8e05fbb",
                 :label
                 {:en
                  "Are your attachments in Finnish, Swedish or English?",
                  :fi
                  "Ovatko liitteesi suomen-, ruotsin- tai englanninkielisiä?",
                  :sv
                  "Är dina bilagor finsk-, svensk-, eller engelskspråkiga?"},
                 :params {:hidden false},
                 :options
                 [{:label {:en "Yes", :fi "Kyllä", :sv "Ja"},
                   :value "0"}
                  {:label {:en "No", :fi "Ei", :sv "Nej"},
                   :value "1",
                   :followups
                   [{:belongs-to-hakukohteet [],
                     :params
                     {:hidden false,
                      :deadline nil,
                      :info-text
                      {:value
                       {:en
                        "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                        :fi
                        "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                        :sv
                        "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. "},
                       :enabled? true}},
                     :belongs-to-hakukohderyhma [],
                     :fieldClass "formField",
                     :label
                     {:en
                      "Official translation of the transcript of records to Finnish, Swedish or English",
                      :fi
                      "Virallinen käännös opintosuoritusotteesta suomeksi, ruotsiksi tai englanniksi",
                      :sv
                      "Officiell översättning av studieprestationsutdraget till finska, svenska eller engelska"},
                     :id "9630243d-6cc9-429a-a783-428f94d5fdab",
                     :options [],
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2019-11-24T12:27:19Z",
                       :name "Risto Hanhinen"},
                      :modified-by
                      {:oid "1.2.246.562.24.83554265088",
                       :date "2022-09-08T12:32:22Z",
                       :name "Minea Wilo-Tanninen"}},
                     :fieldType "attachment"}
                    {:belongs-to-hakukohteet [],
                     :params
                     {:hidden false,
                      :deadline nil,
                      :info-text
                      {:value
                       {:en
                        "The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                        :fi
                        "Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                        :sv
                        "Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. "},
                       :enabled? true}},
                     :belongs-to-hakukohderyhma [],
                     :fieldClass "formField",
                     :label
                     {:en
                      "Official translation of the higher education degree certificate to Finnish, Swedish or English",
                      :fi
                      "Virallinen käännös tutkintotodistuksesta suomeksi, ruotsiksi tai englanniksi",
                      :sv
                      "Officiell översättning av högskoleexamensbetyget till finska, svenska eller engelska"},
                     :id "72fd0176-cfa8-4750-ab4a-aee21ce7e1b4",
                     :options [],
                     :metadata
                     {:locked false,
                      :created-by
                      {:oid "1.2.246.562.24.42485718933",
                       :date "2019-11-24T12:27:19Z",
                       :name "Risto Hanhinen"},
                      :modified-by
                      {:oid "1.2.246.562.24.83554265088",
                       :date "2022-09-08T12:32:35Z",
                       :name "Minea Wilo-Tanninen"}},
                     :fieldType "attachment"}]}],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2019-11-24T12:25:00Z",
                   :name "Risto Hanhinen"},
                  :modified-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2021-07-27T07:07:20Z",
                   :name "Stefan Hanhinen"}},
                 :fieldType "singleChoice",
                 :fieldClass "formField",
                 :validators ["required"]}]}],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2020-06-29T07:59:53Z",
               :name "Anonymisoitu Virkailija"},
              :modified-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2021-07-27T07:07:20Z",
               :name "Stefan Hanhinen"}},
             :fieldType "singleChoice",
             :fieldClass "formField",
             :validators ["required"]}]}
          {:label {:fi "", :sv ""},
           :value "1",
           :condition
           {:answer-compared-to 2022, :comparison-operator "<"},
           :followups
           [{:belongs-to-hakukohteet [],
             :params
             {:hidden false,
              :deadline nil,
              :info-text
              {:value
               {:en
                "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                :fi
                "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. \n"},
               :enabled? true}},
             :belongs-to-hakukohderyhma [],
             :fieldClass "formField",
             :label
             {:en
              "Transcript of records of higher education degree completed outside Finland",
              :fi
              "Opintosuoritusote muualla kuin Suomessa suoritettuun korkeakoulututkintoon sisältyvistä opinnoista",
              :sv
              "Studieprestationsutdrag om högskoleexamen som avlagts annanstans än i Finland"},
             :id "9a3955ad-7412-4921-961e-73e83cf9a3f6",
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2020-06-29T09:13:13Z",
               :name "Anonymisoitu Virkailija"},
              :modified-by
              {:oid "1.2.246.562.24.83554265088",
               :date "2022-09-08T12:32:47Z",
               :name "Minea Wilo-Tanninen"}},
             :fieldType "attachment"}
            {:belongs-to-hakukohteet [],
             :params
             {:hidden false,
              :deadline nil,
              :info-text
              {:value
               {:en
                "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                :fi
                "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                :sv
                "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. "},
               :enabled? true}},
             :belongs-to-hakukohderyhma [],
             :fieldClass "formField",
             :label
             {:en "Higher education degree certificate",
              :fi
              "Muualla kuin Suomessa suoritetun korkeakoulututkinnon tutkintotodistus",
              :sv
              "Högskoleexamensbetyg som avlagts annanstans än i Finland"},
             :id "0367bd26-5cd6-4427-8248-933515492277",
             :options [],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2020-06-29T09:13:41Z",
               :name "Anonymisoitu Virkailija"},
              :modified-by
              {:oid "1.2.246.562.24.83554265088",
               :date "2022-09-08T12:32:58Z",
               :name "Minea Wilo-Tanninen"}},
             :fieldType "attachment"}
            {:sensitive-answer true,
             :belongs-to-hakukohteet [],
             :params
             {:hidden false,
              :info-text
              {:label
               {:en "",
                :fi
                "** HUOM! Alla olevat vastausvaihtoehdot voivat näkyä harmaana. Tässä tilanteessa poista pohjakoulutusvaihtoehto \"Muualla kuin Suomessa suoritettu korkeakoulututkinto\" ja ole yhteydessä hakemasi korkeakoulun hakijapalveluihin. Tällöin voit lähettää muut muutoksesi ja pohjakoulutustietosi voidaan täydentää virkailijoiden toimesta myöhemmin.**",
                :sv
                "**OBS! Svarsalternativen nedan kan vara gråa, vilket betyder att du inte kan svara på frågan. I sådant fall ta bort ditt grundutbildningsalternativ ”Högskoleexamen som avlagts annanstans än i Finland” och kontakta den högskola som du söker till. Då kan du fylla i dina övriga ändringar och högskolan kan komplettera uppgifterna om din utbildningsbakgrund senare.**"}}},
             :belongs-to-hakukohderyhma [],
             :validators ["required"],
             :fieldClass "formField",
             :label
             {:en
              "Are your attachments in Finnish, Swedish or English?",
              :fi
              "Ovatko liitteesi suomen-, ruotsin- tai englanninkielisiä?",
              :sv
              "Är dina bilagor finsk-, svensk-, eller engelskspråkiga?"},
             :id "069c63cc-3bdf-4cfe-9490-a65ba04fc3f0",
             :options
             [{:label {:en "Yes", :fi "Kyllä", :sv "Ja"}, :value "0"}
              {:label {:en "No", :fi "Ei", :sv "Nej"},
               :value "1",
               :followups
               [{:belongs-to-hakukohteet [],
                 :params
                 {:hidden false,
                  :deadline nil,
                  :info-text
                  {:value
                   {:en
                    "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
                    :fi
                    "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
                    :sv
                    "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX. "},
                   :enabled? true}},
                 :belongs-to-hakukohderyhma [],
                 :fieldClass "formField",
                 :label
                 {:en
                  "Official translation of the certificate to Finnish, Swedish or English",
                  :fi
                  "Virallinen käännös suomeksi, ruotsiksi tai englanniksi",
                  :sv
                  "Officiell översättning av intyget till finska, svenska eller engelska"},
                 :id "e80db0c8-097b-4e15-b7df-195b7ae6d0e8",
                 :options [],
                 :metadata
                 {:locked false,
                  :created-by
                  {:oid "1.2.246.562.24.42485718933",
                   :date "2019-11-24T12:27:19Z",
                   :name "Risto Hanhinen"},
                  :modified-by
                  {:oid "1.2.246.562.24.83554265088",
                   :date "2022-09-08T12:33:13Z",
                   :name "Minea Wilo-Tanninen"}},
                 :fieldType "attachment"}]}],
             :metadata
             {:locked false,
              :created-by
              {:oid "1.2.246.562.24.42485718933",
               :date "2019-11-24T12:25:00Z",
               :name "Risto Hanhinen"},
              :modified-by
              {:oid "1.2.246.562.24.27704980991",
               :date "2022-03-30T08:38:33Z",
               :name "Topias Kähärä"}},
             :fieldType "singleChoice"}]}],
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T09:10:23Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-10-19T11:19:43Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["numeric" "required"]}
        {:id "b9d3bb35-6088-419f-94d5-9b06d6e7218b",
         :label {:en "Degree", :fi "Tutkinto", :sv "Examen"},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T09:10:52Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["required"]}
        {:params {},
         :koodisto-source
         {:uri "kktutkinnot",
          :title "Kk-tutkinnot",
          :version 1,
          :allow-invalid? false},
         :koodisto-ordered-by-user true,
         :validators ["required"],
         :fieldClass "formField",
         :label
         {:en "Degree level", :fi "Tutkintotaso", :sv "Examensnivå"},
         :id "6cc14c49-8720-4480-bba1-802c258e3a38",
         :options [],
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T09:11:18Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "dropdown"}
        {:id "4afc8778-9536-4bb6-b6d4-62a8daa3f7ee",
         :label
         {:en "Higher education institution",
          :fi "Korkeakoulu",
          :sv "Högskola"},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T09:12:01Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["required"]}
        {:params {},
         :koodisto-source
         {:uri "maatjavaltiot2",
          :title "Maat ja valtiot",
          :version 2,
          :allow-invalid? true},
         :validators ["required"],
         :fieldClass "formField",
         :label
         {:en "Country of completion",
          :fi "Suoritusmaa",
          :sv "Land där examen är avlagd"},
         :id "b0eb04ec-fa2c-4ac1-b8e7-535803e19b2c",
         :options [],
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T09:17:51Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "dropdown"}
        {:id "de5f7fa0-9bdc-4228-a94c-fcbdc7476a4a",
         :text {:fi ""},
         :label
         {:en "Click add if you want to add further qualifications.",
          :fi "Paina lisää, jos haluat lisätä useampia tutkintoja.",
          :sv
          "Tryck på lägg till om du vill lägga till flera examina."},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T07:49:39Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "p",
         :fieldClass "infoElement"}],
       :metadata
       {:locked false,
        :created-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2020-06-29T09:17:14Z",
         :name "Anonymisoitu Virkailija"},
        :modified-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2021-07-27T07:07:20Z",
         :name "Stefan Hanhinen"}},
       :fieldType "fieldset",
       :fieldClass "questionGroup"}]}
    {:label
     {:en
      "Open university/UAS studies required by the higher education institution",
      :fi "Korkeakoulun edellyttämät avoimen korkeakoulun opinnot",
      :sv "Studier som högskolan kräver vid en öppen högskola"},
     :value "pohjakoulutus_avoin",
     :followups
     [{:id "df43136e-0a92-45ce-adf6-49d53043a0f7",
       :label {:fi "", :sv ""},
       :params {},
       :children
       [{:id "pohjakoulutus_avoin--year-of-completion",
         :label
         {:en "Year of completion",
          :fi "Suoritusvuosi",
          :sv "Avlagd år"},
         :params
         {:size "S",
          :numeric true,
          :max-value "2022",
          :min-value "1900"},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-07-24T06:28:47Z",
           :name "Risto Hanhinen"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-10-19T11:21:55Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["required" "numeric"]}
        {:id "4ba3e8eb-7b42-44de-9404-63c5c955b0b6",
         :label {:en "Study field", :fi "Ala", :sv "Bransch"},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T09:27:46Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["required"]}
        {:id "f3712dad-d7c1-42dc-855b-0cef8342fb01",
         :label
         {:en "Higher education institution",
          :fi "Korkeakoulu",
          :sv "Högskola"},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T09:27:48Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["required"]}
        {:id "618a49a1-029d-4458-9089-e52e6c5857d3",
         :label
         {:en "Study module",
          :fi "Opintokokonaisuus",
          :sv "Studiehelhet"},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T09:27:50Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["required"]}
        {:id "bb8f716b-615f-4775-878e-e924d504f492",
         :label
         {:en "Scope of studies", :fi "Laajuus", :sv "Omfattning"},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T09:27:51Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["required"]}
        {:belongs-to-hakukohteet [],
         :params
         {:hidden false,
          :deadline nil,
          :info-text
          {:value
           {:en
            "The attachment has to be submitted within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.\n",
            :fi
            "Liite täytyy tallentaa viimeistään 7 vuorokautta hakuajan päättymisen jälkeen. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.\n",
            :sv
            "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått.Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\t\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX.\n"},
           :enabled? true}},
         :belongs-to-hakukohderyhma [],
         :fieldClass "formField",
         :label
         {:en
          "Open university / university of applied sciences studies",
          :fi "Todistus avoimen korkeakoulun opinnoista",
          :sv "Studier inom den öppna högskolan"},
         :id "1d94cae9-e6cb-4850-a658-aeab708c1241",
         :options [],
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T09:29:01Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.83554265088",
           :date "2022-09-08T10:09:24Z",
           :name "Minea Wilo-Tanninen"}},
         :fieldType "attachment"}
        {:id "30c76532-afcd-48d7-b54c-76a46f69fe63",
         :text {:fi ""},
         :label
         {:en "Click add if you want to add further qualifications.",
          :fi
          "Paina lisää, jos haluat lisätä useampia opintokokonaisuuksia.",
          :sv
          "Tryck på lägg till om du vill lägga till flera studiehelheter."},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T09:29:33Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "p",
         :fieldClass "infoElement"}],
       :metadata
       {:locked false,
        :created-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2020-06-29T09:27:37Z",
         :name "Anonymisoitu Virkailija"},
        :modified-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2021-07-27T07:07:20Z",
         :name "Stefan Hanhinen"}},
       :fieldType "fieldset",
       :fieldClass "questionGroup"}]}
    {:label
     {:en "Other eligibility for higher education",
      :fi "Muu korkeakoulukelpoisuus",
      :sv "Övrig högskolebehörighet"},
     :value "pohjakoulutus_muu",
     :followups
     [{:id "aa018c7b-221e-4c40-bb9f-0696907f518f",
       :label {:fi "", :sv ""},
       :params {},
       :children
       [{:id "pohjakoulutus_muu--year-of-completion",
         :label
         {:en "Year of completion",
          :fi "Suoritusvuosi",
          :sv "Avlagd år"},
         :params {:numeric true, :max-value "2022", :min-value "1900"},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T09:30:16Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-10-19T11:19:59Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textField",
         :fieldClass "formField",
         :validators ["numeric" "required"]}
        {:id "f49509e3-7c83-44cf-82ac-f99557f61e31",
         :label
         {:en "Description of your other eligibility",
          :fi "Kelpoisuuden kuvaus",
          :sv "Beskrivning av behörigheten"},
         :params {:max-length "500"},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T09:30:20Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "textArea",
         :fieldClass "formField",
         :validators ["required"]}
        {:belongs-to-hakukohteet [],
         :params
         {:hidden false,
          :deadline nil,
          :info-text
          {:value
           {:en
            "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.\n\nName the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.\n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX.",
            :fi
            "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.\n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus.\n\nSkannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.\n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.",
            :sv
            "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg\n\nSkanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. \nSamla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor. \nKontrollera att dokumenten i filen är rättvända.\n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."},
           :enabled? true}},
         :belongs-to-hakukohderyhma [],
         :fieldClass "formField",
         :label
         {:en "Other eligibility for higher education",
          :fi "Todistus muusta korkeakoulukelpoisuudesta",
          :sv "Övrig högskolebehörighet"},
         :id "7e8bf52d-794c-491e-a6c6-0bfa0985ceb6",
         :options [],
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T09:30:24Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.83554265088",
           :date "2022-09-08T10:03:45Z",
           :name "Minea Wilo-Tanninen"}},
         :fieldType "attachment"}
        {:id "c6c906fe-c9c7-48e3-9b2e-c8a5d1a6a525",
         :text {:fi ""},
         :label
         {:en "Click add if you want to add further qualifications.",
          :fi
          "Paina lisää, jos haluat lisätä useampia kokonaisuuksia.",
          :sv
          "Tryck på lägg till om du vill lägga till flera helheter."},
         :params {},
         :metadata
         {:locked false,
          :created-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2020-06-29T09:29:33Z",
           :name "Anonymisoitu Virkailija"},
          :modified-by
          {:oid "1.2.246.562.24.42485718933",
           :date "2021-07-27T07:07:20Z",
           :name "Stefan Hanhinen"}},
         :fieldType "p",
         :fieldClass "infoElement"}],
       :metadata
       {:locked false,
        :created-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2020-06-29T09:30:11Z",
         :name "Anonymisoitu Virkailija"},
        :modified-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2021-07-27T07:07:20Z",
         :name "Stefan Hanhinen"}},
       :fieldType "fieldset",
       :fieldClass "questionGroup"}]}],
   :metadata
   {:locked false,
    :created-by
    {:oid "1.2.246.562.24.42485718933",
     :date "2020-10-15T07:36:01Z",
     :name "Risto Hanhinen"},
    :modified-by
    {:oid "1.2.246.562.24.83554265088",
     :date "2022-09-14T05:55:35Z",
     :name "Minea Wilo-Tanninen"}},
   :fieldType "multipleChoice"}
  {:id "secondary-completed-base-education",
   :label
   {:en
    "Have you completed general upper secondary education or a vocational qualification?",
    :fi
    "Oletko suorittanut lukion/ylioppilastutkinnon tai ammatillisen tutkinnon?",
    :sv
    "Har du avlagt gymnasiet/studentexamen eller yrkesinriktad examen?"},
   :params
   {:info-text
    {:label
     {:en "This is required for statistical reasons",
      :fi "Tämä tieto kysytään tilastointia varten.",
      :sv "Denna uppgift frågas för statistik."}}},
   :options
   [{:label {:en "Yes", :fi "Kyllä", :sv "Ja"},
     :value "0",
     :followups
     [{:params
       {:info-text
        {:label
         {:en
          "Choose the country where you have completed your most recent qualification. If you have not yet completed a general upper secondary school syllabus/matriculation examination or vocational qualification but are in the process of doing so please choose the country where you will complete the qualification. NB: a vocational qualification can be a vocational upper secondary qualification, school-level qualification, post-secondary level qualification, higher vocational level qualification, further vocational qualification or specialist vocational qualification. Do not fill in the country where you have completed a higher education qualification.",
          :fi
          "Merkitse viimeisimmän tutkintosi suoritusmaa. Jos sinulla ei ole vielä lukion päättötodistusta/ylioppilastutkintoa tai ammatillista tutkintoa, mutta olet suorittamassa sellaista, valitse se maa, jossa parhaillaan suoritat kyseistä tutkintoa. Huom: ammatillinen tutkinto voi olla ammatillinen perustutkinto, kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto, ammatti- tai erikoisammattitutkinto. Älä merkitse tähän korkeakoulututkinnon suoritusmaata.",
          :sv
          "Ange land där din senaste examen avlagts. Om du ännu inte har avlagt gymnasiet/studentexamen eller yrkesinriktad examen men håller på att göra det välj då det land där du som bäst avlägger examen i fråga. Obs: yrkesinriktad examen kan vara yrkesinriktad grundexamen, examen på skolnivå, examen på institutsnivå, yrkesinriktad examen på högre nivå, yrkesexamen eller specialyrkesexamen. Ange inte här landet där du har avlagt högskoleexamen."}}},
       :koodisto-source
       {:uri "maatjavaltiot2",
        :title "Maat ja valtiot",
        :version 2,
        :allow-invalid? true},
       :validators ["required"],
       :fieldClass "formField",
       :label
       {:en "Country of completion",
        :fi "Suoritusmaa",
        :sv "Land där du avlagt examen"},
       :id "secondary-completed-base-education–country",
       :options [],
       :metadata
       {:locked false,
        :created-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2020-06-29T09:42:56Z",
         :name "Anonymisoitu Virkailija"},
        :modified-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2021-07-27T07:07:20Z",
         :name "Stefan Hanhinen"}},
       :fieldType "dropdown"}]}
    {:label {:en "No", :fi "En", :sv "Nej"}, :value "1"}],
   :metadata
   {:locked false,
    :created-by
    {:oid "1.2.246.562.24.42485718933",
     :date "2020-06-29T09:41:24Z",
     :name "Anonymisoitu Virkailija"},
    :modified-by
    {:oid "1.2.246.562.24.28532088747",
     :date "2021-08-18T15:44:30Z",
     :name "Ulrika Sundelin"}},
   :fieldType "singleChoice",
   :fieldClass "formField",
   :validators ["required"]}
  {:id "finnish-vocational-before-1995",
   :label
   {:en
    "Have you completed a university or university of applied sciences ( prev. polytechnic) degree in Finland before 2003?",
    :fi
    "Oletko suorittanut suomalaisen ammattikorkeakoulu- tai yliopistotutkinnon ennen vuotta 2003?",
    :sv
    "Har du avlagt en finländsk yrkeshögskole- eller universitetsexamen före år 2003?"},
   :params
   {:info-text
    {:label
     {:en
      "Write your university or university of applied sciences degree only if you have completed it before 2003. After that date the information of completed degrees will be received automatically from the higher education institutions. If you have completed a university/university of applied sciences degree or have received a study place in higher education in Finland after autumn 2014 your admission can be affected. More information on [the quota for first-time applicants](https://opintopolku.fi/konfo/en/sivu/provisions-and-restrictions-regarding-student-admissions-to-higher-education#quota-for-first-time-applicants).",
      :fi
      "Merkitse tähän suorittamasi korkeakoulututkinto vain siinä tapauksessa, jos olet suorittanut sen ennen vuotta 2003. Sen jälkeen suoritetut tutkinnot saadaan automaattisesti korkeakouluilta. Suomessa suoritettu korkeakoulututkinto tai syksyllä 2014 tai sen jälkeen alkaneesta koulutuksesta vastaanotettu korkeakoulututkintoon johtava opiskelupaikka voivat vaikuttaa valintaan. Lue lisää [ensikertalaiskiintiöstä](https://opintopolku.fi/konfo/fi/sivu/valmistaudu-korkeakoulujen-yhteishakuun#ensikertalaiskiinti).",
      :sv
      "Ange här den högskoleexamen som du avlagt före år 2003. Examina som avlagts efter detta fås automatiskt av högskolorna. En högskoleexamen som avlagts i Finland eller en studieplats inom utbildning som leder till högskoleexamen som mottagits år 2014 eller senare kan inverka på antagningen. Läs mera om [kvoten för förstagångssökande](https://opintopolku.fi/konfo/sv/sivu/forbered-dig-for-gemensam-ansokan-till-hogskolor#kvot-fr-frstagangsskande)."}}},
   :options
   [{:label {:en "Yes", :fi "Kyllä", :sv "Ja"},
     :value "0",
     :followups
     [{:id "finnish-vocational-before-1995--year-of-completion",
       :label
       {:en "Year of completion",
        :fi "Suoritusvuosi",
        :sv "Avlagd år"},
       :params
       {:size "S",
        :numeric true,
        :decimals nil,
        :max-value "2002",
        :min-value "1900"},
       :metadata
       {:locked false,
        :created-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2020-06-29T09:45:06Z",
         :name "Anonymisoitu Virkailija"},
        :modified-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2021-07-27T07:07:20Z",
         :name "Stefan Hanhinen"}},
       :fieldType "textField",
       :fieldClass "formField",
       :validators ["numeric" "required"]}
      {:params {},
       :koodisto-source
       {:uri "tutkinto",
        :title "Tutkinto",
        :version 2,
        :allow-invalid? true},
       :validators ["required"],
       :fieldClass "formField",
       :label
       {:en "Name of the degree",
        :fi "Tutkinnon nimi",
        :sv "Examens namn"},
       :id "a7fd3c4a-1e4f-4819-8862-29e22d626e73",
       :options [],
       :metadata
       {:locked false,
        :created-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2020-06-29T09:45:46Z",
         :name "Anonymisoitu Virkailija"},
        :modified-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2021-07-27T07:07:20Z",
         :name "Stefan Hanhinen"}},
       :fieldType "dropdown"}
      {:id "4dda2c1b-d7ff-4c82-abd7-39236a26f1d6",
       :label
       {:en "Higher education institution",
        :fi "Korkeakoulu",
        :sv "Högskola"},
       :params {},
       :metadata
       {:locked false,
        :created-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2020-06-29T09:46:44Z",
         :name "Anonymisoitu Virkailija"},
        :modified-by
        {:oid "1.2.246.562.24.42485718933",
         :date "2021-07-27T07:07:20Z",
         :name "Stefan Hanhinen"}},
       :fieldType "textField",
       :fieldClass "formField",
       :validators ["required"]}]}
    {:label {:en "No", :fi "En", :sv "Nej"}, :value "1"}],
   :metadata
   {:locked false,
    :created-by
    {:oid "1.2.246.562.24.42485718933",
     :date "2020-06-29T09:44:06Z",
     :name "Anonymisoitu Virkailija"},
    :modified-by
    {:oid "1.2.246.562.24.83554265088",
     :date "2022-09-22T12:24:13Z",
     :name "Minea Wilo-Tanninen"}},
   :fieldType "singleChoice",
   :fieldClass "formField",
   :validators ["required"]}],
 :metadata
 {:locked false,
  :created-by
  {:oid "1.2.246.562.24.42485718933",
   :date "2020-06-29T07:25:26Z",
   :name "Anonymisoitu Virkailija"},
  :modified-by
  {:oid "1.2.246.562.24.42485718933",
   :date "2021-10-20T10:23:37Z",
   :name "Stefan Hanhinen"}},
 :fieldType "fieldset",
 :fieldClass "wrapperElement"})

(defn remove-metadata
  []
  (let [modified (clojure.walk/prewalk (fn [node] (if (map? node)
                                     (apply dissoc node [:metadata])
                                     node))
                        my-large-json)]
  modified))