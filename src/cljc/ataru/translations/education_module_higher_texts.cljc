(ns ataru.translations.education-module-higher-texts)

(def texts
  {
   :estimated-graduation-date                              {:en "Estimated graduation date (dd.mm.yyyy)"
                                                            :fi "Arvioitu valmistumispäivämäärä (pp.kk.vvvv)"
                                                            :sv "Beräknat examensdatum (dd.mm.åååå)"}
   :preliminary-certificate-vocational                     {:en "Preliminary certificate from the educational institution"
                                                            :fi "Ennakkoarvio ammattitutkinnosta"
                                                            :sv "Läroanstaltens preliminär intyg"}
   :preliminary-certificate-vocational-basic               {:en "Preliminary certificate from the educational institution"
                                                            :fi "Ennakkoarvio ammatillisesta perustutkinnosta"
                                                            :sv "Läroanstaltens preliminär intyg om yrkesinriktad grundexamen"}
   :have-competence-based-qualification                    {:en "Have you completed your qualification as a competence based qualification in its entirety?"
                                                            :fi "Oletko suorittanut ammatillisen perustutkinnon näyttötutkintona?"
                                                            :sv "Har du avlagt examen som fristående yrkesexamen?"}
   :notification-competence-based-qualification            {:en "Please note that if you have completed your vocational qualification as a competence-based qualification, you do not take part in the certificate-based admission."
                                                            :fi "Huomaathan, ettet ole mukana todistusvalinnassa, jos olet suorittanut tutkinnon näyttötutkintona. "
                                                            :sv "Obs! En examen som är avlagd som fristående examen beaktas inte i betygsbaserad antagning."}
   :add-more-qualifications                                {:en "Click add if you want to add further qualifications."
                                                            :fi "Paina lisää, jos haluat lisätä useampia tutkintoja."
                                                            :sv "Tryck på lägg till om du vill lägga till flera examina."}
   :add-more-wholes                                        {:en "Click add if you want to add further qualifications."
                                                            :fi "Paina lisää, jos haluat lisätä useampia kokonaisuuksia."
                                                            :sv "Tryck på lägg till om du vill lägga till flera helheter."}
   :add-more-studies                                       {:en "Click add if you want to add further qualifications."
                                                            :fi "Paina lisää, jos haluat lisätä useampia opintokokonaisuuksia."
                                                            :sv "Tryck på lägg till om du vill lägga till flera studiehelheter."}
   :have-you-graduated                                     {:en "Have you graduated"
                                                            :fi "Oletko valmistunut?"
                                                            :sv "Har du tagit examen"}
   :have-you-completed                                     {:en "Have you completed general upper secondary education or a vocational qualification?"
                                                            :fi "Oletko suorittanut lukion/ylioppilastutkinnon tai ammatillisen tutkinnon?"
                                                            :sv "Har du avlagt gymnasiet/studentexamen eller yrkesinriktad examen?"}
   :read-who-can-apply                                     {:en "[Read more about who can apply for bachelor's and master's programmes](https://opintopolku.fi/konfo/en/sivu/how-to-apply-for-bachelors-and-masters)"
                                                            :fi "Lue lisää siitä millä koulutuksella voit hakea
                                                                 - [yliopistokoulutuksiin](https://opintopolku.fi/konfo/fi/sivu/valmistaudu-korkeakoulujen-yhteishakuun#hakukelpoisuus-yliopistoon)
                                                                 - [ammattikorkeakoulutuksiin](https://opintopolku.fi/konfo/fi/sivu/valmistaudu-korkeakoulujen-yhteishakuun#hakukelpoisuus-ammattikorkeakouluun)"
                                                            :sv "Mer information om vem som kan söka till
                                                                 - [universitetsutbildning](https://opintopolku.fi/konfo/sv/sivu/forbered-dig-for-gemensam-ansokan-till-hogskolor#anskningsbehrighet-till-universitet)
                                                                 - [yrkeshögskoleutbildning](https://opintopolku.fi/konfo/sv/sivu/forbered-dig-for-gemensam-ansokan-till-hogskolor#anskningsbehrighet-till-yrkeshgskolor)
                                                                 "}
   :transcript-of-records-upper-secondary                  {:en "Latest transcript of study records (upper secondary education diploma)"
                                                            :fi "Ennakkoarvio tai viimeisin todistus suoritetuista opinnoista muualla kuin Suomessa suoritettavasta toisen asteen tutkinnosta"
                                                            :sv "Förhandsexamensbetyg eller betyg över slutförda studier om examen som avlagts annanstans än i Finland och som i landet ifråga ger ansökningsbehörighet för högskola"}
   :transcript-of-records-secondary-finland                {:en "Latest transcript of study records from Finnish upper secondary school"
                                                            :fi "Ennakkoarvio tai viimeisin todistus suoritetuista opinnoista lukiossa"
                                                            :sv "Förhandsexamensbetyg eller betyg över slutförda studier om gymnasiestudier"}
   :transcript-of-records-higher                           {:en "Transcript of records of higher education degree completed outside Finland"
                                                            :fi "Opintosuoritusote muualla kuin Suomessa suoritettuun korkeakoulututkintoon sisältyvistä opinnoista"
                                                            :sv "Studieprestationsutdrag om högskoleexamen som avlagts annanstans än i Finland"}
   :transcript-of-records-in-progress                      {:en "Transcript of records of higher education degree completed outside Finland"
                                                            :fi "Opintosuoritusote muualla kuin Suomessa suoritettavaan korkeakoulututkintoon sisältyvistä opinnoista"
                                                            :sv "Studieprestationsutdrag om högskoleexamen som avlagts annanstans än i Finland"}
   :transcript-of-records-higher-finland                   {:en "Transcript of records of higher education degree completed in Finland"
                                                            :fi "Opintosuoritusote Suomessa suoritettuun korkeakoulututkintoon sisältyvistä opinnoista"
                                                            :sv "Studieprestationsutdrag om högskoleexamen som avlagts i Finland"}
   :transcript-of-records-higher-finland-in-progress       {:en "Transcript of records of higher education degree completed in Finland",
                                                            :fi "Opintosuoritusote Suomessa suoritettavaan korkeakoulututkintoon sisältyvistä opinnoista",
                                                            :sv "Studieprestationsutdrag om högskoleexamen som avlagts i Finland"}
   :predicted-grades-ib                                    {:en "Predicted grades from IB completed outside Finland"
                                                            :fi "Oppilaitoksen ennakkoarvio muualla kuin Suomessa suoritettavan tutkinnon arvosanoista (Candidate Predicted Grades)"
                                                            :sv "Predicted grades från IB-studentexamen som avlagts annanstans än i Finland "}
   :predicted-grades-ib-finland                            {:en "Predicted grades from IB completed in Finland"
                                                            :fi "Oppilaitoksen ennakkoarvio Suomessa suoritettavan tutkinnon arvosanoista (Candidate Predicted Grades)"
                                                            :sv "Predicted grades från IB-studentexamen som avlagts i Finland "}
   :predicted-grades-eb                                    {:en "Predicted grades from EB completed outside Finland"
                                                            :fi "Oppilaitoksen ennakkoarvio muualla kuin Suomessa suoritettavan EB-tutkinnon arvosanoista"
                                                            :sv "Läroanstaltens preliminära vitsord från EB-studentexamen som avlagts annanstans än i Finland"}
   :predicted-grades-eb-finland                            {:en "Predicted grades from EB completed in Finland"
                                                            :fi "Oppilaitoksen ennakkoarvio Suomessa suoritettavan EB-tutkinnon arvosanoista"
                                                            :sv "Läroanstaltens preliminära vitsord från EB-studentexamen som avlagts i Finland"}
   :diploma-programme-ib                                   {:en "Diploma Programme (DP) Results from IB completed outside Finland"
                                                            :fi "Diploma Programme (DP) Results -asiakirja muualla kuin Suomessa suoritettavasta tutkinnosta"
                                                            :sv "Diploma Programme (DP) Results från IB-studentexamen som avlagts annanstans än i Finland"}
   :diploma-programme-ib-finland                           {:en "Diploma Programme (DP) Results from IB completed in Finland"
                                                            :fi "Diploma Programme (DP) Results -asiakirja Suomessa suoritettavasta tutkinnosta"
                                                            :sv "Diploma Programme (DP) Results från IB-studentexamen som avlagts i Finland"}
   :diploma-programme-eb-finland                           {:en "European Baccalaureate diploma completed in Finland"
                                                            :fi "European Baccalaureate Diploma -tutkintotodistus Suomessa suoritettavasta tutkinnosta"
                                                            :sv "European Baccalaureate Diploma från EB-studentexamen som avlagts i Finland"}
   :higher-education-degree-certificate-alien              {:en "Higher education degree certificate"
                                                            :fi "Muualla kuin Suomessa suoritetun korkeakoulututkinnon tutkintotodistus"
                                                            :sv "Högskoleexamensbetyg som avlagts annanstans än i Finland"}
   :higher-education-degree-certificate-alien-in-progress  {:en "Higher education degree certificate"
                                                            :fi "Muualla kuin Suomessa suoritettavan korkeakoulututkinnon tutkintotodistus"
                                                            :sv "Högskoleexamensbetyg som avlagts annanstans än i Finland"}
   :higher-education-degree-certificate                    {:en "Higher education degree certificate"
                                                            :fi "Suomessa suoritetun korkeakoulututkinnon tutkintotodistus"
                                                            :sv "Högskoleexamensbetyg"}
   :higher-education-degree-certificate-in-progress        {:en "Higher education degree certificate"
                                                            :fi "Suomessa suoritettavan korkeakoulututkinnon tutkintotodistus"
                                                            :sv "Högskoleexamensbetyg"}
   :original-upper-secondary-diploma                       {:en "Original upper secondary education diploma"
                                                            :fi "Tutkintotodistus muualla kuin Suomessa suoritetusta tutkinnosta, joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun"
                                                            :sv "Examensbetyg som avlagts annanstans än i Finland och som i landet ifråga ger ansökningsbehörighet för högskola"}
   :vocational-diploma                                     {:en "Vocational qualification diploma"
                                                            :fi "Ammatillisen perustutkinnon tutkintotodistus"
                                                            :sv "Yrkesinriktad grundexamens betyg"}
   :vocational-or-special-diploma                          {:en "Vocational or specialist vocational qualification diploma"
                                                            :fi "Tutkintotodistus ammatti- tai erikoisammattitutkinnosta"
                                                            :sv "Betyg av yrkesexamen eller en specialyrkesexamen"}
   :koski-vocational-info                                  {:en "Your final vocational qualification details are received automatically from the national registry for study rights and completed studies. You can check your study rights and completed studies in Finland from [My Studyinfo's](https://studyinfo.fi/oma-opintopolku/) section: My completed studies (Only available in Finnish/Swedish). If your information is incorrect or information is missing, please contact the educational institution to correct any errors."
                                                            :fi "Saamme lopulliset ammatillisen perustutkinnon suoritustietosi valtakunnallisesta [Koski-tietovarannosta](https://www.oph.fi/fi/palvelut/koski-tietovaranto). Voit tarkistaa opintosuorituksesi [Oma Opintopolku -palvelun](https://opintopolku.fi/oma-opintopolku/) Omat opintosuoritukseni -osiosta. Jos tiedoissasi on puutteita, ole yhteydessä oppilaitokseesi tietojen korjaamiseksi."
                                                            :sv "Vi får slutliga uppgifterna om din yrkesinriktade grundexamen ur den nationella [informationsresursen Koski](https://www.oph.fi/sv/tjanster/informationsresurser-koski). Du kan kolla dina studieprestationer i [Min Studieinfos](https://studieinfo.fi/oma-opintopolku/) del Mina studier. Om dina uppgifter är felaktiga, ska du kontakta din läroanstalt som kan korrigera felen."}
   :koski-vocational-special-info                          {:en "Your final vocational qualification details are received automatically from the national registry for study rights and completed studies. You can check your study rights and completed studies in Finland from [My Studyinfo's](https://studyinfo.fi/oma-opintopolku/) section: My completed studies (Only available in Finnish/Swedish). If your information is incorrect or information is missing, please contact the educational institution to correct any errors."
                                                            :fi "Saamme lopulliset ammattitutkinnon suoritustietosi valtakunnallisesta [Koski-tietovarannosta](https://www.oph.fi/fi/palvelut/koski-tietovaranto). Voit tarkistaa opintosuorituksesi [Oma Opintopolku -palvelun](https://opintopolku.fi/oma-opintopolku/) Omat opintosuoritukseni -osiosta. Jos tiedoissasi on puutteita, ole yhteydessä oppilaitokseesi tietojen korjaamiseksi."
                                                            :sv "Vi får de slutliga uppgifterna om din yrkesexamen ur den nationella [informationsresursen Koski](https://www.oph.fi/sv/tjanster/informationsresurser-koski). Du kan kontrollera dina studieprestationer i [Min Studieinfos](https://studieinfo.fi/oma-opintopolku/) del Mina studier. Om dina uppgifter är felaktiga, ska du kontakta din läroanstalt som kan korrigera felen."}
   :check-if-really-vocational                             {:en "Please make sure that your degree is truly a Finnish vocational upper secondary qualification (ammatillinen perustutkinto). As a rule, these degrees were not available before 1994. It is not possible to enter the year of completion earlier than 1994 on the form."
                                                            :fi "Tarkistathan, että kyseessä on varmasti ammatillinen perustutkinto. Näitä tutkintoja on voinut suorittaa pääsääntöisesti vuodesta 1994 alkaen. Vuotta 1994 aiempia suoritusvuosia ammatilliselle perustutkinnolle ei lomakkeella pysty ilmoittamaan."
                                                            :sv "Kontrollera att det verkligen är en yrkesinriktad grundexamen. Dessa examina har i regel kunnat avläggas från och med 1994. Det är inte möjligt att ange tidigare än år 1994 avlagda examina på blanketten."}
   :check-if-really-opistoaste                             {:en "Please make sure that your degree is truly a Finnish school level (kouluaste), post-secondary level degree (opistoaste) or a higher vocational level degree (ammatillinen korkea-aste). As a rule, these degrees are no longer available in the 2000s. It is not possible to enter the year of completion later than 2005 on the form."
                                                            :fi "Tarkistathan, että kyseessä on varmasti kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto. Näitä tutkintoja ei pääsääntöisesti ole ollut enää 2000-luvulla. Vuotta 2005 myöhempiä kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkintoja ei lomakkeella pysty ilmoittamaan."
                                                            :sv "Kontrollera att det verkligen är en examen på skolnivå, institutsnivå eller inom yrkesutbildning på högre nivå. Dessa examina fanns i regel inte kvar på 2000-talet. Det är inte möjligt att ange senare än år 2005 avlagda examina på blanketten."}
   :check-if-really-post-secondary                         {:en "Please make sure that your degree is truly a Finnish post-secondary level degree (opistoasteen tutkinto). As a rule, these degrees are no longer available in the 2000s. For example, the degrees of a practical nurse, a commercial school graduate (merkantti) and a mechanic are not post-secondary level degrees."
                                                            :fi "Tarkistathan, että kyseessä on varmasti opistoasteen tutkinto. Näitä tutkintoja ei pääsääntöisesti ole ollut enää 2000-luvulla. Esimerkiksi lähihoitajan, merkantin ja mekaanikon tutkinnot eivät ole opistoasteen tutkintoja."
                                                            :sv "Kontrollera att det verkligen är en examen på institutnivå. Dessa examina fanns i regel inte kvar på 2000-talet. Exempelvis närvårdar-, merkant- och mekanikerexamina är inte examina på institutnivå."}
   :check-if-really-kouluaste                              {:en "Please make sure that your degree is truly a Finnish school level degree (kouluasteen tutkinto). As a rule, these degrees are no longer available in the 2000s. For example, the degrees of agronom, a commercial school graduate (merkonomi) and a technician are not school level degrees."
                                                            :fi "Tarkistathan, että kyseessä on varmasti kouluasteen tutkinto. Näitä tutkintoja ei pääsääntöisesti ole ollut enää 2000-luvulla. Esimerkiksi agrologin, teknikon tai merkonomin tutkinnot eivät ole kouluasteen tutkintoja."
                                                            :sv "Kontrollera att det verkligen är en examen på skolnivå. Dessa examina fanns i regel inte kvar på 2000-talet. Exempelvis agrolog-, tekniker- och merkonomexamina är inte examina på skolnivå."}
   :check-if-really-higher-vocational                       {:en "Please make sure that your degree is truly a Finnish higher vocational level degree. As a rule, these degrees are no longer available in the 2000s. For example, the degrees of a practical nurse, a vocational qualification in business and administration (merkonomi) and a datanome are not higher vocational level degrees."
                                                            :fi "Tarkistathan, että kyseessä on varmasti ammatillisen korkea-asteen tutkinto. Näitä tutkintoja ei pääsääntöisesti ole ollut enää 2000-luvulla. Esimerkiksi lähihoitajan, merkonomin ja datanomin tutkinnot eivät ole ammatillisen korkea-asteen tutkintoja."
                                                            :sv "Kontrollera att det verkligen är en examen inom yrkesutbildning på högre nivå. Dessa examina fanns i regel inte kvar på 2000-talet. Exempelvis närvårdar-, merkonom- och datanomexamina är inte examina inom yrkesutbildning på högre nivå."}
   :select-unknown                                         {:en "Select \"Unknown\", if the educational institution where you have completed your degree cannot be found on the list."
                                                            :fi "Valitse \"Tuntematon\", jos et löydä listasta oppilaitosta, jossa olet suorittanut tutkintosi."
                                                            :sv "Välj \"Okänd\" om du inte hittar den läroanstalt, där du har avlagt din examen."}
   :type-of-vocational                                     {:en "Type of vocational qualification"
                                                            :fi "Ammatillisen tutkinnon tyyppi"
                                                            :sv "Yrkesinriktad examens typ"}
   :kouluaste-of-vocational-qualification                  {:en "Vocational qualification (kouluaste)"
                                                            :fi "Kouluasteen tutkinto"
                                                            :sv "Yrkesinriktad examen på skolnivå"}
   :scope-unit                                             {:en "The scope unit"
                                                            :fi "Laajuuden yksikkö"
                                                            :sv "Omfattningens enhet"}
   :educational-background                                 {:en "Your educational background"
                                                            :fi "Pohjakoulutuksesi"
                                                            :sv "Din utbildningsbakgrund"}
   :completed-education                                    {:en "Fill in the education that you have completed or will complete during the admission process."
                                                            :fi "Ilmoita suorittamasi koulutukset. Ilmoita myös ne, jotka suoritat hakukautena."
                                                            :sv "Ange de utbildningar som du har avlagt. Ange också dem som du avlägger under ansökningsperioden."}
   :matriculation-exam-in-finland                          {:en "Matriculation examination completed in Finland"
                                                            :fi "Suomessa suoritettu ylioppilastutkinto"
                                                            :sv "Studentexamen som avlagts i Finland"}
   :matriculation-exam                                     {:en "Matriculation examination"
                                                            :fi "Ylioppilastutkinto"
                                                            :sv "Studentexamen"}
   :matriculation-exam-certificate                         {:en "Finnish matriculation examination certificate"
                                                            :fi "Ylioppilastutkintotodistus"
                                                            :sv "Studentexamensbetyg"}
   :completed-marticaulation-before-1990?                  {:en "Have you completed your Matriculation examination in Finland in 1990 or after?"
                                                            :fi "Oletko suorittanut ylioppilastutkinnon vuonna 1990 tai sen jälkeen?"
                                                            :sv "Har du avlagt studentexamen år 1990 eller senare?"}
   :marticaulation-before-1990                             {:en "Matriculation examination (completed before 1990)"
                                                            :fi "Ylioppilastutkinto (ennen vuotta 1990)"
                                                            :sv "Studentexamen (före år 1990)"}
   :year-of-completion                                     {:en "Year of completion"
                                                            :fi "Suoritusvuosi"
                                                            :sv "Avlagd år"}
   :automatic-matriculation-info                           {:en "Your matriculation examination details are received automatically from the national registry for study rights and completed studies. You can check your study rights and completed studies in Finland from [My Studyinfo's](https://studyinfo.fi/oma-opintopolku/) section: My completed studies (Only available in Finnish/Swedish). If your information is incorrect or information is missing, please contact the Matriculation Examination Board to correct any errors. "
                                                            :fi "Saamme ylioppilastutkinnon suoritustietosi ylioppilastutkintorekisteristä. Voit tarkistaa opintosuorituksesi [Oma Opintopolku -palvelun](https://opintopolku.fi/oma-opintopolku/) Omat opintosuoritukseni -osiosta. Jos tiedossasi on puutteita, ole yhteydessä ylioppilastutkintolautakuntaan tietojen korjaamiseksi."
                                                            :sv "Vi får uppgifterna om din studentexamen ur studentexamensregistret. Du kan kolla dina studieprestationer i [Min Studieinfos](https://studieinfo.fi/oma-opintopolku/) del Mina studier. Om dina uppgifter är felaktiga, ska du kontakta Studentexamensnämnden som kan korrigera felen."}
   :automatic-qualification-info                           {:en "Your qualification details will be received automatically from Koski-register."
                                                            :fi "Saamme tutkintosi rekisteristämme."
                                                            :sv "Vi får uppgifterna om din examen ur vårt register."}
   :automatic-higher-qualification-info                    {:en "We will receive your degree details automatically."
                                                            :fi "Saamme korkeakoulututkintosi tiedot rekisteristämme."
                                                            :sv "Vi får uppgifterna om din högskoleexamen ur vårt register."}
   :submit-your-attachments                                {:en "Submit your attachments in pdf/jpg/png -format. If you cannot submit your attachments online please contact the higher education institution in question directly. The attachments have to be submitted or returned by 26 Sept at 3 pm Finnish time at the latest."
                                                            :fi "Tallenna liitteesi PDF/JPG/PNG-muodossa. Jos et voi tallentaa liitettä sähköisessä muodossa niin ota yhteyttä hakemaasi korkeakouluun joka pyytää liitettä. Liite tulee olla tallennettuna tai palautettuna viimeistään 26.9.2018 klo 15.00."
                                                            :sv "Spara dina bilagor i PDF/JPG/PNG form. Om du inte kan spara bilagorna i elektronisk forma, ska du kontakta den högskola som du har sökt till, vilken begär bilagor. Bilagorna ska vara sparade eller returnerade senast 26.9.2018 kl. 15.00."}
   :general-upper-secondary-school                         {:en "General upper secondary school syllabus completed in Finland (without matriculation examination)"
                                                            :fi "Suomessa suoritettu lukion oppimäärä ilman ylioppilastutkintoa"
                                                            :sv "Gymnasiets lärokurs som avlagts i Finland utan studentexamen"}
   :secondary-school-year-of-completion                    {:en "Year of the completion of general upper secondary school syllabus"
                                                            :fi "Lukion oppimäärän suoritusvuosi"
                                                            :sv "Gymnasiet lärokurs avlagd år"}
   :fill-year-of-completion                                {:en "Please fill in/choose the year of completion"
                                                            :fi "Merkitse vuosi jolloin sait lukion päättötodistuksen."
                                                            :sv "Ange det år då du fick gymnasiets avgångsbetyg. "}
   :educational-institution                                {:en "Educational institution" :fi "Oppilaitos" :sv "Läroanstalt"}
   :upper-secondary-school-attachment                      {:en "General upper secondary education certificate"
                                                            :fi "Lukion päättötodistus"
                                                            :sv "Gymnasiets avgångsbetyg"}
   :international-marticulation-exam                       {:en "International matriculation examination completed in Finland"
                                                            :fi "Suomessa suoritettu kansainvälinen ylioppilastutkinto"
                                                            :sv "Internationell studentexamen som avlagts i Finland"}
   :international-baccalaureate                            {:en "International Baccalaureate -diploma"
                                                            :fi "International Baccalaureate -tutkinto"
                                                            :sv "International Baccalaureate -examen"}
   :ib-diploma                                             {:en "IB Diploma completed outside Finland"
                                                            :fi "IB Diploma -tutkintotodistus muualla kuin Suomessa suoritetusta tutkinnosta"
                                                            :sv "IB Diploma från IB-studentexamen som avlagts annanstans än i Finland"}
   :ib-diploma-finland                                     {:en "IB Diploma completed in Finland",
                                                            :fi "IB Diploma -tutkintotodistus Suomessa suoritetusta tutkinnosta"
                                                            :sv "IB Diploma från IB-studentexamen som avlagts i Finland"}
   :european-baccalaureate                                 {:en "European Baccalaureate -diploma"
                                                            :fi "Eurooppalainen ylioppilastutkinto"
                                                            :sv "European Baccalaureate -examen"}
   :european-baccalaureate-diploma                         {:en "European Baccalaureate diploma completed outside Finland"
                                                            :fi "European Baccalaureate Diploma -tutkintotodistus muualla kuin Suomessa suoritetusta tutkinnosta"
                                                            :sv "European Baccalaureate Diploma från EB-studentexamen som avlagts annanstans än i Finland"}
   :european-baccalaureate-diploma-finland                 {:en "European Baccalaureate diploma completed in Finland"
                                                            :fi "European Baccalaureate Diploma -tutkintotodistus Suomessa suoritetusta tutkinnosta"
                                                            :sv "European Baccalaureate Diploma från EB-studentexamen som avlagts i Finland"}
   :reifeprufung                                           {:en "Deutsche Internationale Abiturprüfung/Reifeprüfung -diploma"
                                                            :fi "Deutsche Internationale Abiturprüfung/Reifeprüfung -tutkinto"
                                                            :sv "Deutsche Internationale Abiturprüfung/Reifeprüfung -examen"}
   :reifeprufung-diploma                                   {:en "Reifeprüfung/DIA diploma from RP/DIA completed outside Finland"
                                                            :fi "Reifeprüfung/DIA-tutkintotodistus muualla kuin Suomessa suoritetusta tutkinnosta"
                                                            :sv "Reifeprüfung/DIA -examensbetyg from RP/DIA-studentexamen som avlagts annanstans än i Finland"}
   :reifeprufung-diploma-finland                           {:en "Reifeprüfung/DIA diploma completed in Finland"
                                                            :fi "Reifeprüfung/DIA-tutkintotodistus Suomessa suoritetusta tutkinnosta"
                                                            :sv "Reifeprüfung/DIA -examensbetyg from RP/DIA-studentexamen som avlagts i Finland"}
   :dia-diploma                                            {:en "DIA -diploma from DIA completed outside Finland"
                                                            :fi "DIA-tutkintotodistus muualla kuin Suomessa suoritettavasta tutkinnosta"
                                                            :sv "DIA -examensbetyg från DIA-studentexamen som avlagts annanstans än i Finland"}
   :dia-diploma-finland                                    {:en "DIA diploma completed in Finland"
                                                            :fi "DIA-tutkintotodistus Suomessa suoritetusta tutkinnosta"
                                                            :sv "DIA -examensbetyg from DIA-studentexamen som avlagts i Finland"}
   :grade-page-dia                                         {:en "Grade page of a Deutsches Internationales Abitur (DIA) diploma completed outside Finland"
                                                            :fi "DIA-tutkintotodistuksen arvosanasivu muualla kuin Suomessa suoritettavasta tutkinnosta"
                                                            :sv "Examensbetygets vitsordssida av Deutsches Internationales Abitur (DIA) -examen avlagd annanstans än i Finland"}
   :grade-page-dia-finland                                 {:en "Grade page of a Deutsches Internationales Abitur (DIA) diploma completed in Finland"
                                                            :fi "DIA-tutkintotodistuksen arvosanasivu Suomessa suoritettavasta tutkinnosta"
                                                            :sv "Examensbetygets vitsordssida av Deutsches Internationales Abitur (DIA) -examen avlagd i Finland"}
   :vocational-qualification-diploma                       {:en "Vocational qualification diploma (kouluaste, opistoaste, ammatillinen korkea-aste"
                                                            :fi "Kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkintotodistus"
                                                            :sv "Betyg från yrkesinriktad examen på skolnivå, examen på institutsnivå eller yrkesinriktad examen på högre nivå"}
   :upper-secondary-education-diploma                      {:en "Upper secondary education diploma"
                                                            :fi "Tutkintotodistus muualla kuin Suomessa suoritetusta tutkinnosta, joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun"
                                                            :sv "Examensbetyg som avlagts annanstans än i Finland och som i landet ifråga ger ansökningsbehörighet för högskola"}
   :diploma-in-fi-sv-en                                    {:en "Is your original diploma in Finnish, Swedish or English?"
                                                            :fi "Onko todistuksesi suomen-, ruotsin- tai englanninkielinen?"
                                                            :sv "Är ditt betyg finsk-, svensk-, eller engelskspråkigt?"}
   :translation-of-study-records                           {:en "Official translation of the latest transcript of study records to Finnish, Swedish or English"
                                                            :fi "Virallinen käännös ennakkoarviosta tai viimeisimmästä todistuksestasi suomeksi, ruotsiksi tai englanniksi"
                                                            :sv "Officiell översättning av förhandsexamensbetyget eller betyget över slutförda studier till finska, svenska eller engelska"}
   :translation-of-diploma                                 {:en "Official translation of the diploma to Finnish, Swedish or English"
                                                            :fi "Virallinen käännös tutkintotodistuksesta suomeksi, ruotsiksi tai englanniksi"
                                                            :sv "Officiell översättning av examensbetyget till finska, svenska eller engelska"}
   :translation-of-certificate                             {:en "Official translation of the certificate to Finnish, Swedish or English"
                                                            :fi "Virallinen käännös suomeksi, ruotsiksi tai englanniksi"
                                                            :sv "Officiell översättning av intyget till finska, svenska eller engelska"}
   :translation-of-transcript-of-records                   {:en "Official translation of the transcript of records to Finnish, Swedish or English"
                                                            :fi "Virallinen käännös opintosuoritusotteesta suomeksi, ruotsiksi tai englanniksi"
                                                            :sv "Officiell översättning av studieprestationsutdraget till finska, svenska eller engelska"}
   :translation-of-degree-higher                           {:en "Official translation of the higher education degree certificate to Finnish, Swedish or English"
                                                            :fi "Virallinen käännös tutkintotodistuksesta suomeksi, ruotsiksi tai englanniksi"
                                                            :sv "Officiell översättning av högskoleexamensbetyget till finska, svenska eller engelska"}
   :vocational-opistoaste-qualification                    {:en "Vocational qualification (opistoaste)"
                                                            :fi "Opistoasteen tutkinto"
                                                            :sv "Yrkesinriktad examen på institutsnivå"}
   :vocational-korkea-aste-qualification                   {:en "Vocational qualification (ammatillinen korkea-aste)"
                                                            :fi "Ammatillisen korkea-asteen tutkinto"
                                                            :sv "Yrkesinriktad examen på högre nivå"}
   :equivalency-certificate-second                         {:en "An equivalency certificate on upper secondary education based on Reifeprüfung or DIA provisions"
                                                            :fi "Vastaavuustodistus lukio-opinnoista, jotka perustuvat RP- tai DIA-tutkinnon säännöksiin"
                                                            :sv "Motsvarighetsintyget av gymnasiestudier, som är baserad på RP- eller DIA-bestämmelser"}
   :equivalency-certificate-second-dia                     {:en "An equivalency certificate on upper secondary education based on DIA provisions"
                                                            :fi "Vastaavuustodistus lukio-opinnoista, jotka perustuvat DIA-tutkinnon säännöksiin"
                                                            :sv "Motsvarighetsintyget av gymnasiestudier, som är baserad på DIA-bestämmelser"}
   :request-attachment-international-exam                  {:en "Request for attachment on international examination"
                                                            :fi "Kansainvälisen ylioppilastutkinnon liitepyyntö"
                                                            :sv "Begäran om bilagor för internationell studentexamen"}
   :double-degree                                          {:en "Double degree (secondary level)"
                                                            :fi "Ammatillinen perustutkinto ja ylioppilastutkinto (kaksoistutkinto)"
                                                            :sv "Yrkesinriktad grundexamen och studentexamen (dubbelexamen"}
   :double-degree-vocational-attachment                    {:en "Request for attachment on vocational qualification"
                                                            :fi "Kaksoistutkinnon liitepyyntö (ammatillinen tutkinto)"
                                                            :sv "Begäran om bilagor för dubbelexamen (yrkesinriktad examen)"}
   :double-degree-marticulation-attachment                 {:en "Request for attachment on Double degree/ matriculation examination before 1990."
                                                            :fi "Kaksoistutkinnon liitepyyntö (ylioppilastutkinto ennen vuotta 1990)"
                                                            :sv "Begäran om bilagor för dubbelexamen (studentexamen före år 1990)"}
   :marticulation-completion-year                          {:en "The year of completion of Matriculation examination"
                                                            :fi "Ylioppilastutkinnon suoritusvuosi"
                                                            :sv "Studentexamen avlagd år"}
   :vocational-completion-year                             {:en "The year of completion of vocational qualification"
                                                            :fi "Ammatillisen tutkinnon suoritusvuosi"
                                                            :sv "Yrkesinriktad examen avlagd år"}
   :vocational-qualification                               {:en "Vocational qualification"
                                                            :fi "Ammatillinen tutkinto"
                                                            :sv "Yrkesinriktad examen"}
   :scope-of-studies                                       {:en "Scope of studies" :fi "Laajuus" :sv "Omfattning"}
   :scope-of-qualification                                 {:en "Scope of qualification"
                                                            :fi "Tutkinnon laajuus"
                                                            :sv "Examens omfattning"}
   :scope-of-vocational-qualification                      {:en "Scope of vocational qualification"
                                                            :fi "Ammatillisen tutkinnon laajuus"
                                                            :sv "Omfattning av yrkesinriktad examen"}
   :scope-of-basic-vocational-qualification                {:en "Scope of vocational qualification"
                                                            :fi "Ammatillisen perustutkinnon laajuus"
                                                            :sv "Omfattning av yrkesinriktad grundexamen"}
   :scope                                                  {:en "Scope of qualification"
                                                            :fi "Laajuus"
                                                            :sv "Omfattning"}
   :courses                                                {:en "Courses" :fi "Kurssia" :sv "Kurser"}
   :ects-credits                                           {:en "ECTS credits" :fi "Opintopistettä" :sv "Studiepoäng"}
   :study-weeks                                            {:en "Study weeks" :fi "Opintoviikkoa" :sv "Studieveckor"}
   :competence-points                                      {:en "Competence points" :fi "Osaamispistettä" :sv "Kompetenspoäng"}
   :hours                                                  {:en "Hours" :fi "Tuntia" :sv "Timmar"}
   :weekly-lessons                                         {:en "Weekly lessons per year"
                                                            :fi "Vuosiviikkotuntia"
                                                            :sv "Årsveckotimmar"}
   :years                                                  {:en "Years" :fi "Vuotta" :sv "År"}
   :certificate-open-studies                               {:en "Open university / university of applied sciences studies"
                                                            :fi "Todistus avoimen korkeakoulun opinnoista"
                                                            :sv "Studier inom den öppna högskolan"}
   :finnish-vocational                                     {:en "Vocational upper secondary qualification, school-level qualification, post-secondary level qualification or higher vocational level qualification completed in Finland"
                                                            :fi "Suomessa suoritettu ammatillinen perustutkinto, kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto"
                                                            :sv "Yrkesinriktad grundexamen, examen på skolnivå, examen på institutsnivå eller yrkesinriktad examen på högre nivå som avlagts i Finland"}
   :finnish-vocational-2017-or-after                       {:en "Have you completed the qualification in 2017 or after?"
                                                            :fi "Oletko suorittanut tutkinnon vuonna 2017 tai sen jälkeen?"
                                                            :sv "Har du avlagt examen år 2017 eller senare?"}
   :qualification                                          {:en "Qualification"
                                                            :fi "Tutkinto"
                                                            :sv "Examen"}
   :finnish-vocational-completed                           {:en "Have you completed your qualification as a competence based qualification in its entiretity?"
                                                            :fi "Oletko suorittanut tutkinnon kokonaan näyttötutkintona?"
                                                            :sv "Har du avlagt examen som fristående yrkesexamen?"}
   :finnish-vocational-attachment                          {:en "Request for attachment on vocational qualification."
                                                            :fi "Ammatillinen perustutkinto kouluasteen opistoasteen tai ammatillisen korkea-asteen tutkinnon liitepyyntö"
                                                            :sv "Begäran om bilagor för yrkesinriktad grundexamen, examen på skolnivå, examen på institutnivå eller examen på yrkesinriktad högre nivå"}
   :click-to-add-more                                      {:en "Click ADD if you want add further qualifications."
                                                            :fi "Paina LISÄÄ jos haluat lisätä useampia tutkintoja."
                                                            :sv "Tryck på LÄGG TILL om du vill lägga till flera examina."}
   :click-to-add-more-qualifications                       {:en "Click ADD if you want add further qualifications."
                                                            :fi "Paina LISÄÄ jos haluat lisätä useampia opintokokonaisuuksia."
                                                            :sv "Tryck på LÄGG TILL om du vill lägga till flera studiehelheter."}
   :finnish-vocational-or-special                          {:en "Further or specialist vocational qualification completed in Finland (ammatti- tai erikoisammattitutkinto)"
                                                            :fi "Suomessa suoritettu ammatti- tai erikoisammattitutkinto"
                                                            :sv "Yrkesexamen eller specialyrkesexamen som avlagts i Finland"}
   :finnish-special-before-2018                            {:en "Have you completed you Further vocational or Specialist vocational qualification in 2018?"
                                                            :fi "Oletko suorittanut ammatti- tai erikoisammattitutkinnon vuonna 2018?"
                                                            :sv "Har du avlagt en yrkesexamen eller en specialyrkesexamen år 2018?"}
   :finnish-special-attachment                             {:en "Vocational or specialist vocational qualification"
                                                            :fi "Ammatti- tai erikoisammattitutkinto"
                                                            :sv "En yrkesexamen eller en specialyrkesexamen"}
   :finnish-higher-education                               {:en "Higher education qualification completed in Finland"
                                                            :fi "Suomessa suoritettu korkeakoulututkinto"
                                                            :sv "Högskoleexamen som avlagts i Finland"}
   :finnish-higher-education-1995-or-after                 {:en "Have you compeleted your university or university of applied sciences degree in 1995 or after?"
                                                            :fi "Oletko suorittanut korkeakoulututkintosi vuonna 1995 tai sen jälkeen? "
                                                            :sv "Har du avlagt din högskoleexamen år 1995 eller senare? *"}
   :finnish-higher-education-degree-level                  {:en "Degree level"
                                                            :fi "Tutkintotaso"
                                                            :sv "Examensnivå"}
   :degree                                                 {:en "Degree"
                                                            :fi "Tutkinto"
                                                            :sv "Examen"}
   :vocational-institution                                 {:en "Vocational institution"
                                                            :fi "Ammatillinen oppilaitos "
                                                            :sv "Yrkesinriktad läroanstalt "}
   :higher-education-institution                           {:en "Higher education institution"
                                                            :fi "Korkeakoulu"
                                                            :sv "Högskola"}
   :higher-education-degree                                {:en "Higher education degree"
                                                            :fi "Korkeakoulututkinto"
                                                            :sv "Högskoleexamen"}
   :international-marticulation-outside-finland            {:en "International matriculation examination completed outside Finland"
                                                            :fi "Muualla kuin Suomessa suoritettu kansainvälinen ylioppilastutkinto"
                                                            :sv "Internationell studentexamen som avlagts annanstans än i Finland"}
   :international-matriculation-outside-finland-name       {:en "Matriculation examination"
                                                            :fi "Ylioppilastutkinto"
                                                            :sv "Studentexamen"}
   :international-marticulation-outside-finland-attachment {:en "International matriculation examination completed outside Finland"
                                                            :fi "Muualla kuin Suomessa suoritettu kansainvälinen ylioppilastutkinto"
                                                            :sv "Internationell studentexamen som avlagts annanstans än i Finland"}
   :country-of-completion                                  {:en "Country of completion"
                                                            :fi "Suoritusmaa"
                                                            :sv "Land där examen är avlagd"}
   :higher-education-outside-finland                       {:en "Higher education qualification completed outside Finland"
                                                            :fi "Muualla kuin Suomessa suoritettu korkeakoulututkinto"
                                                            :sv "Högskoleexamen som avlagts annanstans än i Finland"}
   :level-of-degree                                        {:en "Level of degree" :fi "Tutkintotaso" :sv "Examensnivå"}
   :year-and-date-of-completion                            {:en "Year and date of completion (DD.MM.YYYY)"
                                                            :fi "Suorituspäivämäärä - ja vuosi (pp.kk.vvvv)"
                                                            :sv "År och datum då examen avlagts (dd.mm.åååå)"}
   :other-qualification-foreign                            {:en "Other qualification completed outside Finland that provides eligibility to apply for higher education in the country in question"
                                                            :fi "Muualla kuin Suomessa suoritettu muu tutkinto, joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun"
                                                            :sv "Övrig examen som avlagts annanstans än i Finland, som ger behörighet för högskolestudier i ifrågavarande land"}
   :other-qualification-foreign-attachment                 {:en "Request for attachment on education that provides eligibility for higher education in the awarding country."
                                                            :fi "Liitepyyntö muualla kuin Suomessa suoritetusta muusta tutkinnosta joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun"
                                                            :sv "Begäran om bilagor för examen som avlagts annanstans än i Finland och som i landet ifråga ger ansökningsbehörighet för högskola. "}
   :base-education-open                                    {:en "Studies required by the higher education institution completed at open university or open polytechnic/UAS"
                                                            :fi "Korkeakoulun edellyttämät avoimen korkeakoulun opinnot"
                                                            :sv "Studier som högskolan kräver vid en öppen högskola"}
   :base-education-open-studies                            {:en "Open university/university of applied sciences studies"
                                                            :fi "Avoimen korkeakoulun opinnot"
                                                            :sv "Studier inom den öppna högskolan"}
   :base-education-open-attachment                         {:en "Request for attachment on open university/university of applied sciences studies"
                                                            :fi "Avoimen korkeakouluopintojen liitepyyntö"
                                                            :sv "Begäran om bilagor för studier inom den öppna högskolan"}
   :field                                                  {:en "Study field" :fi "Ala" :sv "Bransch"}
   :module                                                 {:en "Study module"
                                                            :fi "Opintokokonaisuus"
                                                            :sv "Studiehelhet"}
   :base-education-other                                   {:en "Other eligibility for higher education"
                                                            :fi "Muu korkeakoulukelpoisuus"
                                                            :sv "Övrig högskolebehörighet"}
   :other-eligibility-attachment                           {:en "Other eligibility for higher education"
                                                            :fi "Todistus muusta korkeakoulukelpoisuudesta"
                                                            :sv "Övrig högskolebehörighet"}
   :base-education-other-description                       {:en "Description of your other eligibility"
                                                            :fi "Kelpoisuuden kuvaus"
                                                            :sv "Beskrivning av behörigheten"}
   :secondary-completed-base-education                     {:en "Have you completed general upper secondary education or vocational qualification?"
                                                            :fi "Oletko suorittanut lukion/ylioppilastutkinnon tai ammatillisen tutkinnon? "
                                                            :sv "Har du avlagt gymnasiet/studentexamen eller yrkesinriktad examen?"}
   :secondary-completed-country                            {:en "Choose the country where you have completed your most recent qualification. If you have not yet completed a general upper secondary school syllabus/matriculation examination or vocational qualification but are in the process of doing so please choose the country where you will complete the qualification. NB: a vocational qualification can be a vocational upper secondary qualification school-level qualification post-secondary level qualification higher vocational level qualification further vocational qualification or specialist vocational qualification. Do not fill in the country where you have completed a higher education qualification."
                                                            :fi "Merkitse viimeisimmän tutkintosi suoritusmaa. Jos sinulla ei ole vielä lukion päättötodistusta/ylioppilastutkintoa tai ammatillista tutkintoa mutta olet suorittamassa sellaista valitse se maa jossa parhaillaan suoritat kyseistä tutkintoa. Huom: ammatillinen tutkinto voi olla ammatillinen perustutkinto kouluasteen opistoasteen tai ammatillisen korkea-asteen tutkinto ammatti-tai erikoisammattitutkinto. Älä merkitse tähän korkeakoulututkinnon suoritusmaata."
                                                            :sv "Ange land där din senaste examen avlagts. Om du ännu inte har avlagt gymnasiet/studentexamen eller yrkesinriktad examen men håller på att göra det välj då det land där du som bäst avlägger examen i fråga. Obs: yrkesinriktad examen kan vara yrkesinriktad grundexamen examen på skolnivå examen på institutsnivå yrkesinriktad examen på högre nivå yrkesexamen eller specialyrkesexamen. Ange inte här landet där du har avlagt högskoleexamen."}
   :choose-country                                         {:en "Choose country"
                                                            :fi "Valitse suoritusmaa"
                                                            :sv " Välj land där du avlagt examen"}
   :finnish-vocational-before-1995                         {:en "Have you completed a university or university of applied sciences ( prev.  polytechnic) degree in Finland before 1995?"
                                                            :fi "Oletko suorittanut suomalaisen ammattikorkeakoulu- tai yliopistotutkinnon ennen vuotta 1995?"
                                                            :sv "Har du avlagt en finländsk yrkeshögskole- eller universitetsexamen före år 1995?"}
   :finnish-vocational-before-1995-degree                  {:en "Write your university or university of applied sciences degree only if you have completed it before 1995. After that date the information of completed degrees will be received automatically from the higher education institutions. If you have completed a university/university of applied sciences degree or have received a study place in higher education in Finland after autumn 2014 your admission can be affected. More information on the quota for first -time applicants is available on https://studyinfo.fi/wp2/en/higher-education/applying/quota-for-first-time-applicants/"
                                                            :fi "Merkitse tähän suorittamasi korkeakoulututkinto vain siinä tapauksessa jos olet suorittanut sen ennen vuotta 1995. Sen jälkeen suoritetut tutkinnot saadaan automaattisesti korkeakouluilta. Suomessa suoritettu korkeakoulututkinto tai syksyllä 2014 tai sen jälkeen alkaneesta koulutuksesta vastaanotettu korkeakoulututkintoon johtava opiskelupaikka voivat vaikuttaa valintaan. Lue lisää [ensikertalaiskiintiöstä](https://opintopolku.fi/wp/valintojen-tuki/yhteishaku/korkeakoulujen-yhteishaku/ensikertalaiskiintio/)."
                                                            :sv "Ange här den högskoleexamen som du avlagt före år 1995. Examina som avlagts efter detta fås automatiskt av högskolorna. En högskoleexamen som avlagts i Finland eller en studieplats inom utbildning som leder till högskoleexamen som mottagits år 2014 eller senare kan inverka på antagningen. Läs mera om kvoten för förstagångssökande (https://studieinfo.fi/wp/stod-for-studievalet/gemensam-ansokan/gemensam-ansokan-till-hogskolor/kvot-for-forstagangssokande/)"}
   :name-of-degree                                         {:en "Name of the degree" :fi "Tutkinnon nimi" :sv "Examens namn"}
   :base-education-other-attachment                        {:en "Request for attachment on other eligibility for higher education "
                                                            :fi "Muun korkeakoulukelpoisuuden liitepyyntö"
                                                            :sv "Begäran om bilagor för övrig högskolebehörighet"}
   :have-you-completed-before-2003                         {:en "Have you completed a university or university of applied sciences ( prev. polytechnic) degree in Finland before 2003?"
                                                            :fi "Oletko suorittanut suomalaisen ammattikorkeakoulu- tai yliopistotutkinnon ennen vuotta 2003?"
                                                            :sv "Har du avlagt en finländsk yrkeshögskole- eller universitetsexamen före år 2003?"}
   :write-completed-before-2003                            {:en "Write your university or university of applied sciences degree only if you have completed it before 2003. After that date the information of completed degrees will be received automatically from the higher education institutions. If you have completed a university/university of applied sciences degree or have received a study place in higher education in Finland after autumn 2014 your admission can be affected. More information on [the quota for first-time applicants](https://opintopolku.fi/konfo/en/sivu/provisions-and-restrictions-regarding-student-admissions-to-higher-education#quota-for-first-time-applicants)."
                                                            :fi "Merkitse tähän suorittamasi korkeakoulututkinto vain siinä tapauksessa, jos olet suorittanut sen ennen vuotta 2003. Sen jälkeen suoritetut tutkinnot saadaan automaattisesti korkeakouluilta. Suomessa suoritettu korkeakoulututkinto tai syksyllä 2014 tai sen jälkeen alkaneesta koulutuksesta vastaanotettu korkeakoulututkintoon johtava opiskelupaikka voivat vaikuttaa valintaan. Lue lisää [ensikertalaiskiintiöstä](https://opintopolku.fi/konfo/fi/sivu/valmistaudu-korkeakoulujen-yhteishakuun#ensikertalaiskiinti)."
                                                            :sv "Ange här den högskoleexamen som du avlagt före år 2003. Examina som avlagts efter detta fås automatiskt av högskolorna. En högskoleexamen som avlagts i Finland eller en studieplats inom utbildning som leder till högskoleexamen som mottagits år 2014 eller senare kan inverka på antagningen. Läs mera om [kvoten för förstagångssökande](https://opintopolku.fi/konfo/sv/sivu/forbered-dig-for-gemensam-ansokan-till-hogskolor#kvot-fr-frstagangsskande)."}
   :required-for-statistics                                {:en "This is required for statistical reasons"
                                                            :fi "Tämä tieto kysytään tilastointia varten."
                                                            :sv "Denna uppgift frågas för statistik."}
   :choose-country-of-latest-qualification                 {:en "Choose the country where you have completed your most recent qualification. If you have not yet completed a general upper secondary school syllabus/matriculation examination or vocational qualification but are in the process of doing so please choose the country where you will complete the qualification. NB: a vocational qualification can be a vocational upper secondary qualification, school-level qualification, post-secondary level qualification, higher vocational level qualification, further vocational qualification or specialist vocational qualification. Do not fill in the country where you have completed a higher education qualification."
                                                            :fi "Merkitse viimeisimmän tutkintosi suoritusmaa. Jos sinulla ei ole vielä lukion päättötodistusta/ylioppilastutkintoa tai ammatillista tutkintoa, mutta olet suorittamassa sellaista, valitse se maa, jossa parhaillaan suoritat kyseistä tutkintoa. Huom: ammatillinen tutkinto voi olla ammatillinen perustutkinto, kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto, ammatti- tai erikoisammattitutkinto. Älä merkitse tähän korkeakoulututkinnon suoritusmaata."
                                                            :sv "Ange land där din senaste examen avlagts. Om du ännu inte har avlagt gymnasiet/studentexamen eller yrkesinriktad examen men håller på att göra det välj då det land där du som bäst avlägger examen i fråga. Obs: yrkesinriktad examen kan vara yrkesinriktad grundexamen, examen på skolnivå, examen på institutsnivå, yrkesinriktad examen på högre nivå, yrkesexamen eller specialyrkesexamen. Ange inte här landet där du har avlagt högskoleexamen."}
   :share-link-to-my-studyinfo                             {:en "Share a link to your study records from My Studyinfo"
                                                            :fi "Jaa linkki opintosuoritustietoihisi Oma Opintopolku -palvelussa"
                                                            :sv "Dela dina prestationsuppgifter direkt från Min Studieinfo"}
   :how-to-share-link-to-my-studyinfo                      {:en "This question applies only study programmes listed above, under \"Show study programmes\".

                                                                                                                                   You can share the information about your completed studies using a link via My Studyinfo service. In this case you do not need to submit transcript of records and degree certificate separately as an attachment to your application.

                                                                                                                                   To create a link to your completed study records:

                                                                                                                                   1. Log in to [My Studyinfo service](https://studyinfo.fi/oma-opintopolku/) (requires Finnish e-identification and respective means of identification)
                                                                                                                                   2. Choose \"Proceed to studies\".
                                                                                                                                   3. Choose \"Jaa suoritustietoja\" (share study records).
                                                                                                                                   4. Choose the study records you wish to share.
                                                                                                                                   5. Choose \"Jaa valitsemasi opinnot\" (share studyrecords you have chosen).
                                                                                                                                   6. Choose \"Kopioi linkki\" (copy link).
                                                                                                                                   7. Paste the copied link to the text field below.
                                                                                                                                   "
                                                            :fi "Tämä kysymys koskee vain yllä mainittuja hakukohteita, jotka näet painamalla \"näytä hakukohteet\".

                                                                                                                                   Halutessasi voit jakaa opintosuoritustietosi sekä läsnä- ja poissaolokausitietosi Oma Opintopolku -palvelusta saatavan linkin avulla. Tällöin sinun ei tarvitse toimittaa erillistä opintosuoritusotetta ja tutkintotodistusta hakemuksesi liitteeksi.

                                                                                                                                   Näin luot linkin omiin suoritustietoihisi:

                                                                                                                                   1. Kirjaudu sisään [Oma Opintopolku -palveluun](https://opintopolku.fi/oma-opintopolku/).
                                                                                                                                   2. Valitse ”Siirry opintosuorituksiin”.
                                                                                                                                   3. Valitse näytöltä ”Jaa suoritustietoja”.
                                                                                                                                   4. Valitse suoritustiedot, jotka haluat jakaa. Valitse ainakin koulutus, jonka perusteella haet.
                                                                                                                                   5. Valitse ”Jaa valitsemasi opinnot”.
                                                                                                                                   6. Valitse ”Kopioi linkki”.
                                                                                                                                   7. Liitä linkki alla olevaan tekstikenttään."
                                                            :sv "Denna fråga gäller de ovannämnda ansökningsmålen som du får fram genom att klicka på ”visa ansökningsmål”.

                                                                                                                                   Du kan meddela uppgifterna om dina studieprestationer och dina närvaro- och frånvaroperioder med hjälp av en länk. Då behöver du inte lämna in ett separat studieutdrag och betyg som bilaga till din ansökan.

                                                                                                                                   Så här skapar du en länk till dina prestationsuppgifter:

                                                                                                                                   1. Logga in i [tjänsten Min Studieinfo](https://studieinfo.fi/oma-opintopolku/).
                                                                                                                                   2. Välj ”Fortsätt till studierna”.
                                                                                                                                   3. Välj ”Dela dina prestationsuppgifter”.
                                                                                                                                   4. Välj de prestationsuppgifter du vill dela.
                                                                                                                                   5. Välj ”Dela valda studier”.
                                                                                                                                   6. Välj ”Kopiera länk”.
                                                                                                                                   7. Klistra in länken i fältet nedan på ansökningsblanketten."}
   :are-attachments-in-fi-en-sv                            {:en "Are your attachments in Finnish, Swedish or English?"
                                                            :fi "Ovatko liitteesi suomen-, ruotsin- tai englanninkielisiä?"
                                                            :sv "Är dina bilagor finsk-, svensk-, eller engelskspråkiga?"}
   :deadline-next-to-attachment                            {:en "The exact deadline is available next to the attachment request.

                                                               Name the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.

                                                               Scan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright.

                                                               Recommended file formats are: PDF, JPG, PNG and DOCX."
                                                            :fi "Määräaika ilmoitetaan liitepyynnön vieressä.

                                                               Nimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus

                                                               Skannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.

                                                               Suositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX."
                                                            :sv "Den angivna tidpunkten syns invid begäran om bilagor.

                                                               Namnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg

                                                               Skanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.
                                                               Samla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.
                                                               Kontrollera att dokumenten i filen är rättvända.

                                                               Rekommenderade filformat är PDF, JPG, PNG och DOCX. "}
   :submit-attachment-7-days                               {:en "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request.

                                                                 Name the attachment file(s) in the following way: Lastname\\_First name\\_description/name of document. For example, Smith\\_Mary\\_highschooldiploma.

                                                                 Scan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, an educational certificate should be in one file that can include several pages. Check that the documents are all positioned in the same way upright.

                                                                 Recommended file formats are: PDF, JPG, PNG and DOCX."
                                                            :fi "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä.

                                                                 Nimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_tutkintotodistus

                                                                 Skannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi tutkintotodistuksen tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin.

                                                                 Suositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX.
                                                                                                                                                           "
                                                             :sv "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor.

                                                                 Namnge bilagorna i formen ”Efternamn\\_Förnamn\\_dokument”, t.ex. Svensson\\_Sven\\_examensbetyg

                                                                 Skanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga.
                                                                 Samla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska examensbetyget ingå i en fil, som dock kan innehålla flera sidor.
                                                                 Kontrollera att dokumenten i filen är rättvända.

                                                                 Rekommenderade filformat är PDF, JPG, PNG och DOCX.
                                                                                                                                                           "}})