(ns ataru.translations.texts)

(def translation-mapping
  {:application-period                      {:fi "Hakuaika"
                                             :sv "Ansökningstid"
                                             :en "Application period"}
   :not-within-application-period           {:fi "hakuaika ei ole käynnissä"
                                             :sv "inte inom ansökningstiden"
                                             :en "application period currently not ongoing"}
   :not-selectable-application-period-ended {:fi "Hakuaika ei ole käynnissä"
                                             :sv "Inte inom ansökningstiden"
                                             :en "Application period not ongoing"}
   :not-editable-application-period-ended   {:fi "Tämä hakutoive ei ole muokattavissa koska sen hakuaika on päättynyt."
                                             :sv "Tämä hakutoive ei ole muokattavissa koska sen hakuaika on päättynyt."
                                             :en "Tämä hakutoive ei ole muokattavissa koska sen hakuaika on päättynyt."}
   :application-processed-cant-modify       {:fi "Tämä hakemus on käsitelty eikä ole enää muokattavissa"
                                             :sv "Denna ansökan har behandlats och kan inte längre bearbetas"
                                             :en "This application has been processed and can no longer be modified"}
   :continuous-period                       {:fi "Jatkuva haku"
                                             :sv "kontinuerlig ansökningstid"
                                             :en "Continuous application period"}
   :add-row                                 {:fi "Lisää rivi"
                                             :sv "Lägg till rad"
                                             :en "Add row"}
   :remove-row                              {:fi "Poista rivi"
                                             :sv "Ta bort rad"
                                             :en "Remove row"}
   :remove                                  {:fi "Poista"
                                             :sv "Ta bort"
                                             :en "Remove"}
   :add-more                                {:fi "Lisää..."
                                             :sv "Lägg till..."
                                             :en "Add more..."}
   :add-more-button                         {:fi "Lisää"
                                             :sv "Lägg till"
                                             :en "Add"}
   :add                                     {:fi "Lisää"
                                             :sv "Lägg till"
                                             :en "Add more"}
   :add-attachment                          {:fi "Lisää liite..."
                                             :en "Upload attachment..."
                                             :sv "Ladda upp bilagan..."}
   :feedback-header                         {:fi "Hei, kerro vielä mitä pidit hakulomakkeesta!"
                                             :en "Hi! Care to take a moment to rate our application form?"
                                             :sv "Hej, berätta ännu vad du tyckte om ansökningsblanketten?"}
   :feedback-disclaimer                     {:fi "Yhteystietojasi ei käytetä tai yhdistetä palautteen tietoihin."
                                             :en "Your personal information is not sent or associated with the feedback given."
                                             :sv "Dina kontaktuppgifter används inte och kopplas inte heller ihop med responsuppgifterna."}
   :feedback-ratings                        {:fi {1 "Huono"
                                                  2 "Välttävä"
                                                  3 "Tyydyttävä"
                                                  4 "Hyvä"
                                                  5 "Kiitettävä"}
                                             :en {1 "Poor"
                                                  2 "Passable"
                                                  3 "OK"
                                                  4 "Good"
                                                  5 "Excellent"}
                                             :sv {1 "Dålig"
                                                  2 "Försvarlig"
                                                  3 "Nöjaktig"
                                                  4 "Bra"
                                                  5 "Berömlig"}}
   :feedback-text-placeholder               {:fi "Anna halutessasi kommentteja hakulomakkeesta."
                                             :en "Feel free to also share your comments regarding the application form."
                                             :sv "Om du vill kan du ge kommentarer om ansökningsblanketten."}
   :feedback-send                           {:fi "Lähetä palaute"
                                             :en "Send feedback"
                                             :sv "Skicka respons"}
   :feedback-thanks                         {:fi "Kiitos palautteestasi!"
                                             :en "Thank you for your feedback!"
                                             :sv "Tack för din respons!"}
   :page-title                              {:fi "Opintopolku – hakulomake"
                                             :en "Studyinfo – application form"
                                             :sv "Studieinfo – ansökningsblankett"}
   :application-sending                     {:fi "Hakemusta lähetetään"
                                             :sv "Ansökan skickas"
                                             :en "The application is being sent"}
   :application-confirmation                {:fi "Saat vahvistuksen sähköpostiisi"
                                             :sv "Du får en bekräftelse till din e-post"
                                             :en "Confirmation email will be sent to the email address you've provided"}
   :application-sent                        {:fi "Hakemus lähetetty"
                                             :sv "Ansökan har skickats"
                                             :en "The application has been sent"}
   :modifications-saved                     {:fi "Muutokset tallennettu"
                                             :sv "Ändringarna har sparats"
                                             :en "The modifications have been saved"}
   :application-hakija-edit-text            {:fi "LÄHETÄ MUUTOKSET"
                                             :sv "SCICKA FÖRÄNDRINGAR"
                                             :en "SEND MODIFICATIONS"}
   :application-virkailija-edit-text        {:fi "TALLENNA MUUTOKSET"
                                             :sv "SPARA FÖRÄNDRINGAR"
                                             :en "SAVE MODIFICATIONS"}
   :hakija-new-text                         {:fi "LÄHETÄ HAKEMUS"
                                             :sv "SKICKA ANSÖKAN"
                                             :en "SEND APPLICATION"}
   :check-answers                           {:fi ["Tarkista " " tietoa"]
                                             :sv ["Kontrollera " " uppgifter"]
                                             :en ["Check " " answers"]}
   :file-size-info                          {:fi "Tiedoston maksimikoko on 10 MB"
                                             :en "Maximum file size is 10 MB"
                                             :sv "Den maximala filstorleken är 10 MB"}
   :application-received-subject            {:fi "Opintopolku - Hakemuksesi on vastaanotettu"
                                             :sv "Opintopolku - Din ansökan har tagits emot"
                                             :en "Opintopolku - Your application has been received"}
   :application-edited-subject              {:fi "Opintopolku - Hakemuksesi on päivitetty"
                                             :sv "Opintopolku - Din ansökan har updaterats"
                                             :en "Opintopolku - Your application has been received"}
   :application-received-text               {:fi "Hakemuksesi on vastaanotettu."
                                             :en "Your application has been received."
                                             :sv "Din ansökan har tagits emot."}
   :application-edited-text                 {:fi "Hakemuksesi on päivitetty."
                                             :en "Your application has been updated."
                                             :sv "Din ansökan har uppdaterats."}
   :best-regards                            {:fi "terveisin"
                                             :sv "Med vänliga hälsningar"
                                             :en "Best Regards"}
   :application-can-be-found-here           {:fi "Hakemuksesi löytyy täältä"
                                             :sv "Din ansökan kan hittas här"
                                             :en "You can find your application here"}
   :hello-text                              {:fi "Hei"
                                             :sv "Hej"
                                             :en "Hi"}
   :modify-link-text                        {:fi "Ylläolevan linkin kautta voit katsella ja muokata hakemustasi."
                                             :en "You can view and modify your application using the link above."
                                             :sv "Du kan se och redigera din ansökan via länken ovan."}
   :do-not-share-warning-text               {:fi "Älä jaa linkkiä ulkopuolisille. Jos käytät yhteiskäyttöistä tietokonetta, muista kirjautua ulos sähköpostiohjelmasta."
                                             :en "Do not share the link with others. If you are using a public or shared computer, remember to log out of the email application."
                                             :sv "Dela inte länken vidare till utomstående. Om du använder en offentlig dator, kom ihåg att logga ut från e-postprogrammet."}
   :search-application-options              {:fi "Etsi tämän haun koulutuksia"
                                             :sv "Sök ansökningsmål i denna ansökan"
                                             :en "Search for application options"}
   :add-application-option                  {:fi "Lisää hakukohde"
                                             :sv "Lägg till ansökningsmål"
                                             :en "Add application option"}
   :applications_at_most                    {:fi "Tässä haussa voit hakea %s hakukohteeseen"
                                             :sv "Tässä haussa voit hakea %s hakukohteeseen"
                                             :en "Tässä haussa voit hakea %s hakukohteeseen"}
   :file-upload-failed                      {:fi "Tiedostoa ei ladattu, yritä uudelleen"
                                             :en "File failed to upload, try again"
                                             :sv "Fil inte laddat, försök igen"}
   :file-type-forbidden                     {:fi "Tiedostoa ei ladattu, yritä uudelleen"
                                             :en "File failed to upload, try again"
                                             :sv "Fil inte laddat, försök igen"}
   :question-for-hakukohde                  {:fi "Kysymys koskee hakukohteita"
                                             :en "This question is for application options"
                                             :sv "Frågan är för ansökningsmålar"}
   :show-more                               {:fi "Näytä lisää.."
                                             :en "Show more.."
                                             :sv "Visa mer.."}
   :expired-secret-heading                  {:fi "Tämä hakemuslinkki on vanhentunut"
                                             :en "This application link has expired"
                                             :sv "Denna ansökningslänk har föråldrats"}
   :expired-secret-paragraph                {:fi "Turvallisuussyistä hakemuslinkki on voimassa yhden muokkauskerran tai enintään 30 päivää."
                                             :en "For security reasons the link is valid for one application update or a maximum of 30 days."
                                             :sv "Av säkerhetsskäl är ansökningslänken i kraft under en session eller i högst 30 dagar."}
   :expired-secret-button                   {:fi "Tilaa uusi hakemuslinkki sähköpostiisi"
                                             :en "Send a new application link to your email"
                                             :sv "Beställ en ny ansökningslänk till din e-post"}
   :expired-secret-sent                     {:fi "Uusi linkki lähetetty!"
                                             :en "The new link has been sent!"
                                             :sv "Den nya länken har skickats!"}
   :expired-secret-contact                  {:fi "Ongelmatilanteessa ota yhteys hakemaasi oppilaitokseen."
                                             :en "If problems arise, please contact the educational organization to which you have applied."
                                             :sv "Vid eventuella problemsituationer kontakta den läroanstalt du söker till."}
   :no-hakukohde-search-hits                {:fi "Ei hakutuloksia"
                                             :en "No search results found"
                                             :sv "Inga sökresultat"}
   :preview                                 {:fi "Esikatselu"
                                             :en "Preview"
                                             :sv "Förhandsvisa"}
   :window-close-warning                    {:fi "Varmistathan että hakemus on lähetetty ennen sivun sulkemista."
                                             :en "Please ensure you have submitted the form before closing the page."
                                             :sv ""}
   :hours                                   {:fi "tuntia" :en "hours" :sv "timmar"}
   :minutes                                 {:fi "minuuttia" :en "minutes" :sv "minuter"}
   :seconds                                 {:fi "sekuntia" :en "seconds" :sv "sekunder"}
   :hour                                    {:fi "tunti" :en "hour" :sv "timme"}
   :minute                                  {:fi "minuutti" :en "minute" :sv "minut"}
   :second                                  {:fi "sekunti" :en "second" :sv "sekund"}})

(def general-texts
  {:yes                {:en "Yes"
                        :fi "Kyllä"
                        :sv "Ja"}
   :no                 {:en "No"
                        :fi "Ei"
                        :sv "Nej"}
   :have-not           {:en "No"
                        :fi "En"
                        :sv "Nej"}
   :year-of-completion {:en "Year of completion"
                        :fi "Suoritusvuosi"
                        :sv "Avlagd år"}})

(def base-education-module-texts
  {:title                            {:fi "Koulutustausta"
                                      :sv "Utbildningsbakgrund"
                                      :en "Eligibility"}
   :completed-education              {:en "Fill in the education that you have completed  or will complete during the application term."
                                      :fi "Merkitse suorittamasi pohjakoulutukset, myös ne jotka suoritat hakukautena."
                                      :sv "Ange avlagda grundutbildningar, samt de som du avlägger under ansökningsperioden"}
   :higher-education-qualification   {:en "Higher education qualification completed in Finland"
                                      :fi "Suomessa suoritettu korkeakoulututkinto "
                                      :sv "Högskoleexamen som avlagts i Finland"}
   :qualification-level              {:en "Qualification level"
                                      :fi "Tutkintotaso"
                                      :sv "Examensnivå"}
   :completion-year-and-date         {:en "Year and date of completion"
                                      :fi "Suoritusvuosi ja päivämäärä"
                                      :sv "År och datum då examen avlagts"}
   :qualification                    {:en "Qualification/degree"
                                      :fi "Tutkinto"
                                      :sv "Examen"}
   :studies-required                 {:en "Studies required by the higher education institution completed at open university or open university of applied sciences (UAS)"
                                      :fi "Korkeakoulun edellyttämät avoimen korkeakoulun opinnot "
                                      :sv "Studier som högskolan kräver vid en öppen högskola"}
   :field                            {:en "Field"
                                      :fi "Ala"
                                      :sv "Bransch"}
   :module                           {:en "Study module"
                                      :fi "Opintokokonaisuus "
                                      :sv "Studiehelhet"}
   :scope                            {:en "Scope"
                                      :fi "Laajuus "
                                      :sv "Omfattning"}
   :institution                      {:en "Higher education institution"
                                      :fi "Korkeakoulu"
                                      :sv "Högskola"}
   :higher-education-outside-finland {:en "Higher education qualification completed outside Finland"
                                      :fi "Muualla kuin Suomessa suoritettu korkeakoulututkinto "
                                      :sv "Högskoleexamen som avlagts annanstans än i Finland"}
   :qualification-country            {:en "Country where the qualification has been awarded"
                                      :fi "Suoritusmaa"
                                      :sv "Land där examen har avlagts"}
   :other-eligibility                {:en "Other eligibility for higher education"
                                      :fi "Muu korkeakoulukelpoisuus"
                                      :sv "Övrig högskolebehörighet"}
   :describe-eligibility             {:en "Describe eligibility"
                                      :fi "Kelpoisuuden kuvaus"
                                      :sv "Beskrivning av behörigheten"}
   :have-you-completed               {:en "Have you completed general upper secondary education or vocational qualification?",
                                      :fi "Oletko suorittanut lukion/ylioppilastutkinnon tai ammatillisen tutkinnon?",
                                      :sv "Har du avlagt gymnasiet/studentexamen eller yrkesinriktad examen?"}
   :choose-country                   {:en "Choose the country where you have completed your most recent qualification. If you have not yet completed a general upper secondary school syllabus/matriculation examination or vocational qualification, but are in the process of doing so, please choose the country where you will complete the qualification. NB: a vocational qualification can be a vocational upper secondary qualification, school-level qualification, post-secondary level qualification, higher vocational level qualification, further vocational qualification or specialist vocational qualification. Do not fill in the country where you have completed a higher education qualification.",
                                      :fi "Merkitse viimeisimmän tutkintosi suoritusmaa. Jos sinulla ei ole vielä lukion päättötodistusta/ylioppilastutkintoa tai ammatillista tutkintoa mutta olet suorittamassa sellaista, valitse se maa, jossa parhaillaan suoritat kyseistä tutkintoa. Huom: ammatillinen tutkinto voi olla ammatillinen perustutkinto, kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto, ammatti-tai erikoisammattitutkinto. Älä merkitse tähän korkeakoulututkinnon suoritusmaata.",
                                      :sv "Ange land där din senaste examen avlagts. Om du ännu inte har avlagt gymnasiet/studentexamen eller yrkesinriktad examen men håller på att göra det, välj då det land där du som bäst avlägger examen i fråga. Obs: yrkesinriktad examen kan vara yrkesinriktad grundexamen, examen på skolnivå, examen på institutsnivå, yrkesinriktad examen på högre nivå, yrkesexamen eller specialyrkesexamen. Ange inte land där du avlagt högskoleexamen."}})

(def higher-base-education-module-texts
  {:educational-background                          {:en "Your educational background" :fi "Koodistopohjainen pohjakoulutusosio" :sv "Utbildningsbakgrund"}
   :completed-education                             {:en "Fill in the education that you have completed  or will complete during the admission process (autumn 2018)"
                                                     :fi "Ilmoita kaikki suorittamasi koulutukset. Myös ne jotka suoritat hakukautena (syksy 2018)."
                                                     :sv "Ange alla utbildningar som du har avlagt. Ange också dem som du avlägger under ansökningsperioden."}
   :marticulation-exam-in-finland                   {:en "Matriculation examination"
                                                     :fi "Suomessa suoritettu ylioppilastutkinto"
                                                     :sv "Studentexamen som avlagts i Finland"}
   :marticulation-exam                              {:en "Matriculation examination"
                                                     :fi "Ylioppilastutkinto"
                                                     :sv "Studentexamen"}
   :completed-marticaulation-before-1990?           {:en "Have you completed your Matriculation examination in Finland in 1990 or after?"
                                                     :fi "Oletko suorittanut ylioppilastutkinnon vuonna 1990 tai sen jälkeen?"
                                                     :sv "Har du avlagt studentexamen år 1990 eller senare?"}
   :marticaulation-before-1990                      {:en "Matriculation examination (completed before 1990)"
                                                     :fi "Ylioppilastutkinto (ennen vuotta 1990)"
                                                     :sv "Studentexamen (före år 1990)"}
   :year-of-completion                              {:en "Year of completion"
                                                     :fi "Suoritusvuosi"
                                                     :sv "Avlagd år"}
   :automatic-marticulation-info                    {:en "Your matriculation examination details are received automatically from the Matriculation Examination Board."
                                                     :fi "Saamme ylioppilastutkintosi tiedot rekisteristämme."
                                                     :sv "Vi får uppgifterna om din studentexamen ur vårt register."}
   :automatic-qualification-info                    {:en "Your qualification details will be received automatically from Koski-register."
                                                     :fi "Saamme tutkintosi rekisteristämme."
                                                     :sv "Vi får uppgifterna om din examen ur vårt register."}
   :automatic-higher-qualification-info             {:en "We will receive your degree details automatically."
                                                     :fi "Saamme korkeakoulututkintosi tiedot rekisteristämme."
                                                     :sv "Vi får uppgifterna om din högskoleexamen ur vårt register."}
   :submit-your-attachments                         {:en "Submit your attachments in pdf/jpg/png -format. If you cannot submit your attachments online please contact the higher education institution in question directly. The attachments have to be submitted or returned by 26 Sept at 3 pm Finnish time at the latest."
                                                     :fi "Tallenna liitteesi PDF/JPG/PNG-muodossa. Jos et voi tallentaa liitettä sähköisessä muodossa niin ota yhteyttä hakemaasi korkeakouluun joka pyytää liitettä. Liite tulee olla tallennettuna tai palautettuna viimeistään 26.9.2018 klo 15.00."
                                                     :sv "Spara dina bilagor i PDF/JPG/PNG form. Ta kontakt med den högskola som du har sökt till och som begär bilagor om du inte kan spara bilagorna i elektronisk form. Bilagorna ska vara sparade eller returnerade senast 26.9.2018 kl. 15.00."}
   :general-upper-secondary-school                  {:en "General upper secondary school syllabus completed in Finland (without matriculation examination)"
                                                     :fi "Suomessa suoritettu lukion oppimäärä ilman ylioppilastutkintoa"
                                                     :sv "Gymnasiets lärokurs som avlagts i Finland utan studentexamen"}
   :secondary-school-year-of-completion             {:en "Year of the completion of general upper secondary school syllabus"
                                                     :fi "Lukion oppimäärän suoritusvuosi"
                                                     :sv "Gymnasiet lärokurs avlagd år"}
   :fill-year-of-completion                         {:en "Please fill in/choose the year of completion"
                                                     :fi "Merkitse vuosi jolloin sait lukion päättötodistuksen."
                                                     :sv "Ange det år då du fick gymnasiets avgångsbetyg. "}
   :educational-institution                         {:en "Educational institution" :fi "Oppilaitos" :sv "Läroanstalt"}
   :upper-secondary-school-attachment               {:en "Upper secondary school certificate"
                                                     :fi "Lukion päättötodistus"
                                                     :sv "Gymnasiets avgångsbetyg"}
   :international-marticulation-exam                {:en "International matriculation examination completed in Finland"
                                                     :fi "Suomessa suoritettu kansainvälinen ylioppilastutkinto"
                                                     :sv "Internationell studentexamen som avlagts i Finland"}
   :international-baccalaureate                     {:en "International Baccalaureate -diploma"
                                                     :fi "International Baccalaureate"
                                                     :sv "International Baccalaureate -examen"}
   :european-baccalaureate                          {:en "European Baccalaureate -diploma"
                                                     :fi " Eurooppalainen ylioppilastutkinto"
                                                     :sv "European Baccalaureate -examen"}
   :reifeprufung                                    {:en "Reifeprüfung - diploma/ Deutsche Internationale Abiturprüfung"
                                                     :fi "Reifeprüfung"
                                                     :sv "Reifeprüfung - examen"}
   :request-attachment-international-exam           {:en "Request for attachment on international examination"
                                                     :fi "Kansainvälisen ylioppilastutkinnon liitepyyntö"
                                                     :sv "Begäran om bilagor för internationell studentexamen"}
   :double-degree                                   {:en "Double degree (secondary level)"
                                                     :fi "Ammatillinen perustutkinto ja ylioppilastutkinto (kaksoistutkinto)"
                                                     :sv "Dubbelexamen"}
   :double-degree-vocational-attachment             {:en "Request for attachment on vocational qualification"
                                                     :fi "Kaksoistutkinnon liitepyyntö (ammatillinen tutkinto)"
                                                     :sv "Begäran om bilagor för dubbelexamen (yrkesinriktad examen)"}
   :double-degree-marticulation-attachment          {:en "Request for attachment on Double degree/ matriculation examination before 1990."
                                                     :fi "Kaksoistutkinnon liitepyyntö (ylioppilastutkinto ennen vuotta 1990)"
                                                     :sv "Begäran om bilagor för dubbelexamen (studentexamen före år 1990)"}
   :marticulation-completion-year                   {:en "The year of completion of Matriculation examination"
                                                     :fi "Ylioppilastutkinnon suoritusvuosi"
                                                     :sv "Studentexamen avlagd år"}
   :vocational-completion-year                      {:en "The year of completion of vocational qualification"
                                                     :fi "Ammatillisen tutkinnon suoritusvuosi"
                                                     :sv "Yrkesinriktad examen avlagd år"}
   :vocational-qualification                        {:en "Vocational qualification"
                                                     :fi "Ammatillinen tutkinto"
                                                     :sv "Yrkesinriktad examen"}
   :scope-of-qualification                          {:en "Scope of qualification"
                                                     :fi "Tutkinnon laajuus"
                                                     :sv "Examens omfattning"}
   :courses                                         {:en "Courses" :fi "Kurssia" :sv "Kurser"}
   :ects-credits                                    {:en "ECTS credits" :fi "Opintopistettä" :sv "Studiepoäng"}
   :study-weeks                                     {:en "Study weeks" :fi "Opintoviikkoa" :sv "Studieveckor"}
   :competence-points                               {:en "Competence points" :fi "Osaamispistettä" :sv "Kompetenspoäng"}
   :hours                                           {:en "Hours" :fi "Tuntia" :sv "Timmar"}
   :weekly-lessons                                  {:en "Weekly lessons per year"
                                                     :fi "Vuosiviikkotuntia"
                                                     :sv "Årsveckotimmar"}
   :years                                           {:en "Years" :fi "Vuotta" :sv "År"}
   :finnish-vocational                              {:en "Vocational upper secondary qualification, school-level qualification, post-secondary level qualification or higher vocational level qualification completed in Finland"
                                                     :fi "Suomessa suoritettu ammatillinen perustutkinto, kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto"
                                                     :sv "Yrkesinriktad grundexamen, examen på skolnivå, examen på institutsnivå eller yrkesinriktad examen på högre nivå som avlagts i Finland"}
   :finnish-vocational-2017-or-after                {:en "Have you completed the qualification in 2017 or after?"
                                                     :fi "Oletko suorittanut tutkinnon vuonna 2017 tai sen jälkeen?"
                                                     :sv "Har du avlagt examen år 2017 eller senare?"}
   :qualification                                   {:en "Qualification"
                                                     :fi "Tutkinto"
                                                     :sv "Examen"}
   :finnish-vocational-completed                    {:en "Have you completed your qualification as a competence based qualification in its entiretity?"
                                                     :fi "Oletko suorittanut tutkinnon kokonaan näyttötutkintona?"
                                                     :sv "Har du avlagt examen som fristående yrkesexamen?"}
   :finnish-vocational-attachment                   {:en "Request for attachment on vocational qualification."
                                                     :fi "Ammatillinen perustutkinto kouluasteen opistoasteen tai ammatillisen korkea-asteen tutkinnon liitepyyntö"
                                                     :sv "Begäran om bilagor för yrkesinriktad grundexamen examen på skolnivå examen på institutnivå eller examen på yrkesinriktad högre nivå"}
   :click-to-add-more                               {:en "Click ADD if you want add further qualifications."
                                                     :fi "Paina LISÄÄ jos haluat lisätä useampia tutkintoja."
                                                     :sv "Tryck på LÄGG TILL om du vill lägga till flera examina."}
   :click-to-add-more-qualifications                {:en "Click ADD if you want add further qualifications."
                                                     :fi "Paina LISÄÄ jos haluat lisätä useampia opintokokonaisuuksia."
                                                     :sv "Tryck på LÄGG TILL om du vill lägga till flera studiehelhet."}
   :finnish-vocational-or-special                   {:en "Further vocational qualification or specialist vocational qualification completed in Finland"
                                                     :fi "Suomessa suoritettu ammatti- tai erikoisammattitutkinto"
                                                     :sv "Yrkesexamen eller specialyrkesexamen som avlagts i Finland"}
   :finnish-special-before-2018                     {:en "Have you completed you Further vocational or Specialist vocational qualification in 2018?"
                                                     :fi "Oletko suorittanut ammatti- tai erikoisammattitutkinnon vuonna 2018?"
                                                     :sv "Har du avlagt en yrkesexamen eller en specialyrkesexamen år 2018?"}
   :finnish-special-attachment                      {:en "Vocational or specialist vocational qualification"
                                                     :fi "Ammatti- tai erikoisammattitutkinto"
                                                     :sv "En yrkesexamen eller en specialyrkesexamen"}
   :finnish-higher-education                        {:en "Higher education qualification completed in Finland"
                                                     :fi "Suomessa suoritettu korkeakoulututkinto"
                                                     :sv "Högskoleexamen som avlagts i Finland"}
   :finnish-higher-education-1995-or-after          {:en "Have you compeleted your university or university of applied sciences degree in 1995 or after?"
                                                     :fi "Oletko suorittanut korkeakoulututkintosi vuonna 1995 tai sen jälkeen? "
                                                     :sv "Har du avlagt din högskoleexamen år 1995 eller senare? *"}
   :finnish-higher-education-degree-level           {:en "Degree level"
                                                     :fi "Tutkintotaso"
                                                     :sv "Examensnivå"}
   :degree                                          {:en "Degree"
                                                     :fi "Tutkinto"
                                                     :sv "Examen"}
   :higher-education-institution                    {:en "Higher education institution"
                                                     :fi "Korkeakoulu"
                                                     :sv "Högskola"}
   :higher-education-degree                         {:en "Higher education degree"
                                                     :fi "Korkeakoulututkinto"
                                                     :sv "Högskoleexamen"}
   :international-marticulation-outside-finland     {:en "International matriculation examination completed outside Finland"
                                                     :fi "Muualla kuin Suomessa suoritettu kansainvälinen ylioppilastutkinto"
                                                     :sv "Internationell studentexamen som avlagts annanstans än i Finland"}
   :internationa-marticulation-outside-finland-name {:en "Name of examination/diploma"
                                                     :fi "Ylioppilastutkinto"
                                                     :sv "Studentexamen"}
   :country-of-completion                           {:en "Country of completion"
                                                     :fi "Suoritusmaa"
                                                     :sv "Land där examen är avlagd"}
   :higher-education-outside-finland                {:en "Higher education qualification completed outside Finland"
                                                     :fi "Muualla kuin Suomessa suoritettu korkeakoulututkinto"
                                                     :sv "Högskoleexamen som avlagts annanstans än i Finland"}
   :level-of-degree                                 {:en "Level of degree" :fi "Tutkintotaso" :sv "Examensnivå"}
   :year-and-date-of-completion                     {:en "Year and date of completion (DD.MM.YYYY)"
                                                     :fi "Suorituspäivämäärä - ja vuosi (pp.kk.vvvv)"
                                                     :sv "År och datum då examen avlagts (dd.mm.åååå)"}
   :other-qualification-foreign                     {:en "Other qualification completed outside Finland that provides eligibility to apply for higher education in the country in question"
                                                     :fi "Muualla kuin Suomessa suoritettu muu tutkinto, joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun"
                                                     :sv "Övrig examen som avlagts annanstans än i Finland, och ger behörighet för högskolestudier i ifrågavarande land"}
   :other-qualification-foreign-attachment          {:en "Request for attachment on education that provides eligibility for higher education in the awarding country."
                                                     :fi "Liitepyyntö muualla kuin Suomessa suoritetusta muusta tutkinnosta joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun"
                                                     :sv "Begäran om bilagor för examen som avlagts annanstans än i Finland och som i landet ifråga ger ansökningsbehörighet för högskola. "}
   :base-education-open                             {:en "Studies required by the higher education institution completed at open university or open polytechnic/UAS"
                                                     :fi "Korkeakoulun edellyttämät avoimen korkeakoulun opinnot"
                                                     :sv "Studier som högskolan kräver vid en öppen högskola"}
   :base-education-open-studies                     {:en "Open university/university of applied sciences studies"
                                                     :fi "Avoimen korkeakoulun opinnot"
                                                     :sv "Studier inom den öppna högskolan"}
   :base-education-open-attachment                  {:en "Request for attachment on open university/university of applied sciences studies"
                                                     :fi "Avoimen korkeakouluopintojen liitepyyntö"
                                                     :sv "Begäran om bilagor för studier inom den öppna högskolan"}
   :field                                           {:en "Study field" :fi "Ala" :sv "Bransch"}
   :module                                          {:en "Study module"
                                                     :fi "Opintokokonaisuus"
                                                     :sv "Studiehelhet"}
   :base-education-other                            {:en "Other eligibility for higher education"
                                                     :fi "Muu korkeakoulukelpoisuus"
                                                     :sv "Övrig högskolebehörighet"}
   :base-education-other-description                {:en "Description of your other eligibility"
                                                     :fi "Kelpoisuuden kuvaus"
                                                     :sv "Beskrivning av behörigheten"}
   :secondary-completed-base-education              {:en "Have you completed general upper secondary education or vocational qualification?"
                                                     :fi "Oletko suorittanut lukion/ylioppilastutkinnon tai ammatillisen tutkinnon? "
                                                     :sv "Har du avlagt gymnasiet/studentexamen eller yrkesinriktad examen?"}
   :secondary-completed-country                     {:en "Choose the country where you have completed your most recent qualification. If you have not yet completed a general upper secondary school syllabus/matriculation examination or vocational qualification but are in the process of doing so please choose the country where you will complete the qualification. NB: a vocational qualification can be a vocational upper secondary qualification school-level qualification post-secondary level qualification higher vocational level qualification further vocational qualification or specialist vocational qualification. Do not fill in the country where you have completed a higher education qualification."
                                                     :fi "Merkitse viimeisimmän tutkintosi suoritusmaa. Jos sinulla ei ole vielä lukion päättötodistusta/ylioppilastutkintoa tai ammatillista tutkintoa mutta olet suorittamassa sellaista valitse se maa jossa parhaillaan suoritat kyseistä tutkintoa. Huom: ammatillinen tutkinto voi olla ammatillinen perustutkinto kouluasteen opistoasteen tai ammatillisen korkea-asteen tutkinto ammatti-tai erikoisammattitutkinto. Älä merkitse tähän korkeakoulututkinnon suoritusmaata."
                                                     :sv "Ange land där din senaste examen avlagts. Om du ännu inte har avlagt gymnasiet/studentexamen eller yrkesinriktad examen men håller på att göra det välj då det land där du som bäst avlägger examen i fråga. Obs: yrkesinriktad examen kan vara yrkesinriktad grundexamen examen på skolnivå examen på institutsnivå yrkesinriktad examen på högre nivå yrkesexamen eller specialyrkesexamen. Ange inte här landet där du har avlagt högskoleexamen."}
   :choose-country                                  {:en "Choose country"
                                                     :fi "Valitse suoritusmaa"
                                                     :sv " Välj land där du avlagt examen"}
   :finnish-vocational-before-1995                  {:en "Have you completed a university or university of applied sciences ( prev.  polytechnic) degree in Finland before 1995?"
                                                     :fi "Oletko suorittanut suomalaisen ammattikorkeakoulu- tai yliopistotutkinnon ennen vuotta 1995?"
                                                     :sv "Har du avlagt en finländsk yrkeshögskole- eller universitetsexamen före år 1995?"}
   :finnish-vocational-before-1995-degree           {:en "Write your university or university of applied sciences degree only if you have completed it before 1995. After that date the information of completed degrees will be received automatically from the higher education institutions. If you have completed a university/university of applied sciences degree or have received a study place in higher education in Finland after autumn 2014 your admission can be affected. More information on the quota for first -time applicants is available on https://studyinfo.fi/wp2/en/higher-education/applying/quota-for-first-time-applicants/"
                                                     :fi "Merkitse tähän suorittamasi korkeakoulututkinto vain siinä tapauksessa jos olet suorittanut sen ennen vuotta 1995. Sen jälkeen suoritetut tutkinnot saadaan automaattisesti korkeakouluilta. Suomessa suoritettu korkeakoulututkinto tai syksyllä 2014 tai sen jälkeen alkaneesta koulutuksesta vastaanotettu korkeakoulututkintoon johtava opiskelupaikka voivat vaikuttaa valintaan. Lue lisää [ensikertalaiskiintiöstä](https://opintopolku.fi/wp/valintojen-tuki/yhteishaku/korkeakoulujen-yhteishaku/ensikertalaiskiintio/)."
                                                     :sv "Ange här den högskoleexamen som du avlagt före år 1995. Examina som avlagts efter detta fås automatiskt av högskolorna. En högskoleexamen som avlagts i Finland eller en studieplats inom utbildning som leder till högskoleexamen som mottagits år 2014 eller senare kan inverka på antagningen. Läs mera om kvoten för förstagångssökande (https://studieinfo.fi/wp/stod-for-studievalet/gemensam-ansokan/gemensam-ansokan-till-hogskolor/kvot-for-forstagangssokande/)"}
   :name-of-degree                                  {:en "Name of the degree" :fi "Tutkinnon nimi" :sv "Examens namn"}
   :base-education-other-attachment                 {:en "Request for attachment on other eligibility for higher education "
                                                     :fi "Muun korkeakoulukelpoisuuden liitepyyntö"
                                                     :sv "Begäran om bilagor för övrig högskolebehörighet"}
   :required-for-statistics                         {:fi "Tämä tieto kysytään tilastointia varten."
                                                     :sv "Denna uppgift frågas för statistik."
                                                     :en "This is required for statistical reasons"}})

(def person-info-module-texts
  {:forenames            {:fi "Etunimet"
                          :sv "Förnamn"
                          :en "Forenames"}
   :main-forename        {:fi "Kutsumanimi"
                          :sv "Tilltalsnamn"
                          :en "Main forename"}
   :surname              {:fi "Sukunimi"
                          :sv "Efternamn"
                          :en "Surname"}
   :nationality          {:fi "Kansalaisuus"
                          :sv "Medborgarskap"
                          :en "Nationality"}
   :country-of-residence {:fi "Asuinmaa"
                          :sv "Boningsland"
                          :en "Country of residence"}
   :have-finnish-ssn     {:fi "Onko sinulla suomalainen henkilötunnus?"
                          :sv "Har du en finländsk personbeteckning?"
                          :en "Do you have a Finnish personal identity code?"}
   :ssn                  {:fi "Henkilötunnus"
                          :sv "Personbeteckning"
                          :en "Personal identity code"}
   :gender               {:fi "Sukupuoli"
                          :sv "Kön"
                          :en "Gender"}
   :birth-date           {:fi "Syntymäaika"
                          :sv "Födelsetid"
                          :en "Date of birth"}
   :passport-number      {:fi "Passin numero"
                          :sv "Passnummer"
                          :en "Passport number"}
   :national-id-number   {:fi "Kansallinen ID-tunnus"
                          :sv "Nationellt ID-signum"
                          :en "National ID number"}
   :birthplace           {:fi "Syntymäpaikka ja -maa"
                          :sv "Födelseort och -land"
                          :en "Place and country of birth"}
   :email                {:fi "Sähköpostiosoite"
                          :sv "E-postadress"
                          :en "E-mail address"}
   :phone                {:fi "Matkapuhelin"
                          :sv "Mobiltelefonnummer"
                          :en "Mobile phone number"}
   :address              {:fi "Katuosoite"
                          :sv "Näraddress"
                          :en "Address"}
   :home-town            {:fi "Kotikunta"
                          :sv "Hemkommun"
                          :en "Home town"}
   :city                 {:fi "Kaupunki ja maa"
                          :sv "Stad och land"
                          :en "City and country"}
   :postal-code          {:fi "Postinumero"
                          :sv "Postnummer"
                          :en "Postal code"}
   :postal-office        {:fi "Postitoimipaikka"
                          :sv "Postkontor"
                          :en "Postal office"}
   :language             {:fi "Äidinkieli"
                          :sv "Modersmål"
                          :en "Native language"}
   :label                {:fi "Henkilötiedot"
                          :sv "Personuppgifter"
                          :en "Personal information"}
   :label-amendment      {:fi "(Osio lisätään automaattisesti lomakkeelle)"
                          :sv "Partitionen automatiskt lägga formen"
                          :en "The section will be automatically added to the application"}
   :date-formats         {:fi "pp.kk.vvvv"
                          :sv "dd.mm.åååå"
                          :en "dd.mm.yyyy"}})

(def email-default-texts
  {:email-submit-confirmation-template
   {:submit-email-subjects      {:fi "Opintopolku: hakemuksesi on vastaanotettu"
                                 :sv "Studieinfo: Din ansökan har mottagits"
                                 :en "Studyinfo: Your application has been received"}
    :with-application-period    {:fi "Voit katsella ja muokata hakemustasi hakuaikana yllä olevan linkin kautta. Älä jaa linkkiä ulkopuolisille. Jos käytät yhteiskäyttöistä tietokonetta, muista kirjautua ulos sähköpostiohjelmasta.\n\nJos sinulla on verkkopankkitunnukset, mobiilivarmenne tai sähköinen henkilökortti, voit vaihtoehtoisesti kirjautua sisään [Opintopolku.fi](https://www.opintopolku.fi):ssä, ja tehdä muutoksia hakemukseesi Oma Opintopolku -palvelussa hakuaikana. Oma Opintopolku -palvelussa voit lisäksi nähdä valintojen tulokset ja ottaa opiskelupaikan vastaan.\n\nÄlä vastaa tähän viestiin - viesti on lähetetty automaattisesti.\n\nYstävällisin terveisin <br/>\nOpintopolku\n"
                                 :sv "Om du vill ändra din ansökan, kan du göra ändringar via följande länken ovan under ansökningstiden. Dela inte länken vidare till utomstående. Om du använder en offentlig dator, kom ihåg att logga ut från e-postprogrammet.\n\nOm du har nätbankskoder, mobilcertifikat eller ett elektroniskt ID-kort, kan du alternativt logga in i [Studieinfo.fi](https://www.studieinfo.fi) och under ansökningstiden göra ändringarna i tjänsten Min Studieinfo. I tjänsten kan du också, se antagningsresultaten och ta emot studieplatsen.\n\nSvara inte på detta meddelande, det har skickats automatiskt.\n\nMed vänliga hälsningar, <br/>\nStudieinfo\n"
                                 :en "If you wish to edit your application, you can use the link above and make the changes within the application period. Do not share the link with others. If you are using a public or shared computer, remember to log out of the email application.\n\nIf you have Finnish online banking credentials, an electronic ID-card or mobile certificate, you can also log in at [Studyinfo.fi](https://www.studyinfo.fi) and make the changes in the My Studyinfo -service within the application period. In addition to making changes to your application, if you have access to the My Studyinfo -service you can also view the admission results and confirm the study place.\n\nThis is an automatically generated email, please do not reply.\n\nBest regards, <br/>\nStudyinfo\n"}
    :without-application-period {:fi "Voit katsella ja muokata hakemustasi yllä olevan linkin kautta. Älä jaa linkkiä ulkopuolisille. Jos käytät yhteiskäyttöistä tietokonetta, muista kirjautua ulos sähköpostiohjelmasta.\n\nJos sinulla on verkkopankkitunnukset, mobiilivarmenne tai sähköinen henkilökortti, voit vaihtoehtoisesti kirjautua sisään [Opintopolku.fi](https://www.opintopolku.fi):ssä, ja tehdä muutoksia hakemukseesi Oma Opintopolku -palvelussa hakuaikana. Oma Opintopolku -palvelussa voit lisäksi nähdä valintojen tulokset ja ottaa opiskelupaikan vastaan.\n\nÄlä vastaa tähän viestiin - viesti on lähetetty automaattisesti.\n\nYstävällisin terveisin <br/>\nOpintopolku\n"
                                 :sv "Om du vill ändra din ansökan, kan du göra ändringar via följande länken ovan. Dela inte länken vidare till utomstående. Om du använder en offentlig dator, kom ihåg att logga ut från e-postprogrammet.\n\nOm du har nätbankskoder, mobilcertifikat eller ett elektroniskt ID-kort, kan du alternativt logga in i [Studieinfo.fi](https://www.studieinfo.fi) och under ansökningstiden göra ändringarna i tjänsten Min Studieinfo. I tjänsten kan du också, se antagningsresultaten och ta emot studieplatsen.\n\nSvara inte på detta meddelande, det har skickats automatiskt.\n\nMed vänliga hälsningar, <br/>\nStudieinfo\n"
                                 :en "If you wish to edit your application, you can use the link above and make the changes within the application period. Do not share the link with others. If you are using a public or shared computer, remember to log out of the email application.\n\nIf you have Finnish online banking credentials, an electronic\nID-card or mobile certificate, you can also log in\nat [Studyinfo.fi](https://www.studyinfo.fi) and make the\nchanges in the My Studyinfo -service within the application period. In addition to making changes to your application, if you have access to the My Studyinfo -service you can also view the admission results and confirm the study place.\n\nThis is an automatically generated email, please do not reply.\n\nBest regards, <br/>\nStudyinfo\n"}}})