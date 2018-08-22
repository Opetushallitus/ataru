(ns ataru.translations.texts
  #?(:cljs (:require [goog.string :refer [format]])))

(def translation-mapping
  {:add                                         {:fi "Lisää"
                                                 :sv "Lägg till"
                                                 :en "Add more"}
   :add-application-option                      {:fi "Lisää hakukohde"
                                                 :sv "Lägg till ansökningsmål"
                                                 :en "Add application option"}
   :add-attachment                              {:fi "Lisää liite..."
                                                 :en "Upload attachment..."
                                                 :sv "Ladda upp bilagan..."}
   :add-more                                    {:fi "Lisää..."
                                                 :sv "Lägg till..."
                                                 :en "Add more..."}
   :add-more-button                             {:fi "Lisää"
                                                 :sv "Lägg till"
                                                 :en "Add"}
   :add-row                                     {:fi "Lisää rivi"
                                                 :sv "Lägg till rad"
                                                 :en "Add row"}
   :allow-publishing-of-results-online          {:fi "Opiskelijavalinnan tulokseni saa julkaista internetissä?"
                                                 :sv "Mitt antagningsresultat får publiceras på internet?"
                                                 :en "Opiskelijavalinnan tulokseni saa julkaista internetissä?"}
   :allow-use-of-contact-information            {:fi "Yhteystietojani saa käyttää koulutusmarkkinoinnissa?"
                                                 :sv "Min kontaktinformation får användas för utbildningsmarknadsföring?"
                                                 :en "Yhteystietojani saa käyttää koulutusmarkkinoinnissa?"}
   :application-can-be-found-here               {:fi "Hakemuksesi löytyy täältä"
                                                 :sv "Din ansökan kan hittas här"
                                                 :en "You can find your application here"}
   :application-confirmation                    {:fi "Saat vahvistuksen sähköpostiisi"
                                                 :sv "Du får en bekräftelse till din e-post"
                                                 :en "Confirmation email will be sent to the email address you've provided"}
   :application-edited-subject                  {:fi "Opintopolku - Hakemuksesi on päivitetty"
                                                 :sv "Studieinfo - Din ansökan har updaterats"
                                                 :en "Opintopolku - Your application has been received"}
   :application-edited-text                     {:fi "Hakemuksesi on päivitetty."
                                                 :en "Your application has been updated."
                                                 :sv "Din ansökan har uppdaterats."}
   :application-hakija-edit-text                {:fi "LÄHETÄ MUUTOKSET"
                                                 :sv "SKICKA FÖRÄNDRINGAR"
                                                 :en "SEND MODIFICATIONS"}
   :application-period                          {:fi "Hakuaika"
                                                 :sv "Ansökningstid"
                                                 :en "Application period"}
   :application-processed-cant-modify           {:fi "Tämä hakemus on käsitelty eikä ole enää muokattavissa"
                                                 :sv "Denna ansökan har behandlats och kan inte längre bearbetas"
                                                 :en "This application has been processed and can no longer be modified"}
   :application-received-subject                {:fi "Opintopolku - Hakemuksesi on vastaanotettu"
                                                 :sv "Studieinfo - Din ansökan har tagits emot"
                                                 :en "Opintopolku - Your application has been received"}
   :application-received-text                   {:fi "Hakemuksesi on vastaanotettu."
                                                 :en "Your application has been received."
                                                 :sv "Din ansökan har tagits emot."}
   :application-sending                         {:fi "Hakemusta lähetetään"
                                                 :sv "Ansökan skickas"
                                                 :en "The application is being sent"}
   :application-sent                            {:fi "Hakemus lähetetty"
                                                 :sv "Ansökan har skickats"
                                                 :en "The application has been sent"}
   :application-virkailija-edit-text            {:fi "TALLENNA MUUTOKSET"
                                                 :sv "SPARA FÖRÄNDRINGARNA"
                                                 :en "SAVE MODIFICATIONS"}
   :applications_at_most                        {:fi "Tässä haussa voit hakea %s hakukohteeseen"
                                                 :sv "I denna ansökan kan du söka till %s ansökningsmål"
                                                 :en "Tässä haussa voit hakea %s hakukohteeseen"}
   :best-regards                                {:fi "terveisin"
                                                 :sv "Med vänliga hälsningar"
                                                 :en "Best Regards"}
   :check-answers                               {:fi ["Tarkista " " tietoa"], :sv ["Kontrollera " " uppgifter"], :en ["Check " " answers"]}
   :contact-language                            {:fi "Asiointikieli"
                                                 :sv "Ärendespråk"
                                                 :en "Asiointikieli"}
   :contact-language-info                       {:fi "Valitse kieli, jolla haluat vastaanottaa opiskelijavalintaan liittyviä tietoja. Toiveesi otetaan huomioon mahdollisuuksien mukaan."
                                                 :sv "Välj det språk på vilket du vill få information om studerandeantagningen. Ditt önskemål tas i beaktande om möjligt."
                                                 :en "Choose the language in which you wish to receive information regarding the student selection. Your choice will be taken into consideration if possible."}
   :continuous-period                           {:fi "Jatkuva haku"
                                                 :sv "kontinuerlig ansökningstid"
                                                 :en "Continuous application period"}
   :do-not-share-warning-text                   {:fi "Älä jaa linkkiä ulkopuolisille. Jos käytät yhteiskäyttöistä tietokonetta, muista kirjautua ulos sähköpostiohjelmasta."
                                                 :en "Do not share the link with others. If you are using a public or shared computer, remember to log out of the email application."
                                                 :sv "Dela inte länken vidare till utomstående. Kom ihåg att logga ut från e-postprogrammet, om du använder en offentlig dator."}
   :english                                     {:fi "Englanti"
                                                 :sv "Engelska"
                                                 :en "English"}
   :expired-secret-button                       {:fi "Tilaa uusi hakemuslinkki sähköpostiisi"
                                                 :en "Send a new application link to your email"
                                                 :sv "Beställ en ny ansökningslänk till din e-post"}
   :expired-secret-contact                      {:fi "Ongelmatilanteessa ota yhteys hakemaasi oppilaitokseen."
                                                 :en "If problems arise, please contact the educational organization to which you have applied."
                                                 :sv "Vid eventuella problemsituationer bör du kontakta den läroanstalt du söker till."}
   :expired-secret-heading                      {:fi "Tämä hakemuslinkki on vanhentunut"
                                                 :en "This application link has expired"
                                                 :sv "Denna ansökningslänk är föråldrad"}
   :expired-secret-paragraph                    {:fi "Turvallisuussyistä hakemuslinkki on voimassa yhden muokkauskerran tai enintään 30 päivää."
                                                 :en "For security reasons the link is valid for one application update or a maximum of 30 days."
                                                 :sv "Av säkerhetsskäl är ansökningslänken i kraft under en session eller i högst 30 dagar."}
   :expired-secret-sent                         {:fi "Uusi linkki lähetetty!"
                                                 :en "The new link has been sent!"
                                                 :sv "Den nya länken har skickats!"}
   :feedback-disclaimer                         {:fi "Yhteystietojasi ei käytetä tai yhdistetä palautteen tietoihin."
                                                 :en "Your personal information is not sent or associated with the feedback given."
                                                 :sv "Dina kontaktuppgifter används inte och kopplas inte heller ihop med responsuppgifterna."}
   :feedback-header                             {:fi "Hei, kerro vielä mitä pidit hakulomakkeesta!"
                                                 :en "Hi! Care to take a moment to rate our application form?"
                                                 :sv "Hej, berätta ännu vad du tyckte om ansökningsblanketten?"}
   :feedback-ratings                            {:fi {1 "Huono"
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
   :feedback-send                               {:fi "Lähetä palaute"
                                                 :en "Send feedback"
                                                 :sv "Skicka respons"}
   :feedback-text-placeholder                   {:fi "Anna halutessasi kommentteja hakulomakkeesta."
                                                 :en "Feel free to also share your comments regarding the application form."
                                                 :sv "Om du vill kan du ge kommentarer om ansökningsblanketten."}
   :feedback-thanks                             {:fi "Kiitos palautteestasi!"
                                                 :en "Thank you for your feedback!"
                                                 :sv "Tack för din respons!"}
   :file-size-info                              {:fi "Tiedoston maksimikoko on 10 MB"
                                                 :en "Maximum file size is 10 MB"
                                                 :sv "Den maximala filstorleken är 10 MB"}
   :file-type-forbidden                         {:fi "Tiedostoa ei ladattu, yritä uudelleen"
                                                 :en "File failed to upload, try again"
                                                 :sv "Filen kunde inte laddas, försök igen"}
   :file-upload-failed                          {:fi "Tiedostoa ei ladattu, yritä uudelleen"
                                                 :en "File failed to upload, try again"
                                                 :sv "Filen kunde inte laddas, försök igen"}
   :finnish                                     {:fi "Suomi"
                                                 :sv "Finska"
                                                 :en "Finnish"}
   :hakija-new-text                             {:fi "LÄHETÄ HAKEMUS"
                                                 :sv "SKICKA ANSÖKAN"
                                                 :en "SEND APPLICATION"}
   :hello-text                                  {:fi "Hei"
                                                 :sv "Hej"
                                                 :en "Hi"}
   :hour                                        {:fi "tunti"
                                                 :en "hour"
                                                 :sv "timme"}
   :hours                                       {:fi "tuntia"
                                                 :en "hours"
                                                 :sv "timmar"}
   :insufficient-base-education                 {:fi "Ilmoitus riittämättömästä pohjakoulutuksesta"
                                                 :sv "Meddelande om otillräcklig grundutbildning"
                                                 :en "Ilmoitus riittämättömästä pohjakoulutuksesta"}
   :liitepyynto-for-hakukohde                   {:fi "Liitepyyntö kuuluu hakukohteisiin"
                                                 :en "Request for attachment is for application options"
                                                 :sv "Begäran om bilagor berör ansökningsmål"}
   :lupatiedot-info                             {:fi "Tarkista hakulomakkeelle täyttämäsi tiedot huolellisesti ennen hakulomakkeen lähettämistä. Opiskelijavalinta voidaan purkaa, jos olet antanut vääriä tietoja. Hakemuksesi kaikki tiedot tallennetaan opiskelijavalintarekisteriin (L 884/2017). [Opintopolun tietosuojaseloste](https://opintopolku.fi/wp/tietosuojaseloste/).

                        Saat vahvistusviestin vastaanotetusta hakulomakkeesta sähköpostiisi."
                                                 :sv "Kontrollera noggrant de uppgifter som du har angett i ansökningsblanketten innan du skickar ansökningsblanketten. Antagningen av studerande kan hävas om du har gett felaktiga uppgifter. Alla uppgifter i din ansökan sparas i antagningsregistret (L 884/2017). [Studieinfors dataskyddsbeskrivning] (https://studieinfo.fi/wp/dataskyddsbeskrivning).

                        Du får i din e-post ett bekräftelsemeddelande över att ansökningsblanketten har kommit fram."
                                                 :en "Please check all the information you have given in the application before you submit the application. If you have given false information, your admission can be withdrawn. The information you have given in this application will be stored in the student admissions register (Act 884/2017). [Studyinfo's register description](https://studyinfo.fi/wp2/en/register/).

                        You will receive a confirmation of your application to your email."}
   :minute                                      {:fi "minuutti"
                                                 :en "minute"
                                                 :sv "minut"}
   :minutes                                     {:fi "minuuttia"
                                                 :en "minutes"
                                                 :sv "minuter"}
   :modifications-saved                         {:fi "Muutokset tallennettu"
                                                 :sv "Ändringarna har sparats"
                                                 :en "The modifications have been saved"}
   :modify-link-text                            {:fi "Ylläolevan linkin kautta voit katsella ja muokata hakemustasi."
                                                 :en "You can view and modify your application using the link above."
                                                 :sv "Du kan se och redigera din ansökan via länken ovan."}
   :muokkaa-hakukohteita                        {:fi "Muokkaa hakukohteita"
                                                 :sv "Bearbeta ansökningsmål"
                                                 :en "Muokkaa hakukohteita"}
   :no-hakukohde-search-hits                    {:fi "Ei hakutuloksia"
                                                 :en "No search results found"
                                                 :sv "Inga sökresultat"}
   :not-applicable-for-hakukohteet              {:fi "Ilmoittamasi pohjakoulutuksen perusteella et voi tulla valituksi seuraaviin hakukohteisiin"
                                                 :sv "På basis av den grundutbildning u har angett, kan du inte antas till följande ansökningsmål"
                                                 :en "Ilmoittamasi pohjakoulutuksen perusteella et voi tulla valituksi seuraaviin hakukohteisiin"}
   :not-editable-application-period-ended       {:fi "Tämä hakutoive ei ole muokattavissa koska sen hakuaika on päättynyt."
                                                 :sv "Ansökningsmålet kan inte bearbetas eftersom ansökningstiden har utgått."
                                                 :en "Tämä hakutoive ei ole muokattavissa koska sen hakuaika on päättynyt."}
   :not-selectable-application-period-ended     {:fi "Hakuaika ei ole käynnissä"
                                                 :sv "Ingen pågående ansökningstid"
                                                 :en "Application period not ongoing"}
   :not-within-application-period               {:fi "hakuaika ei ole käynnissä"
                                                 :sv "ingen pågående ansökningstid"
                                                 :en "application period currently not ongoing"}
   :page-title                                  {:fi "Opintopolku – hakulomake"
                                                 :en "Studyinfo – application form"
                                                 :sv "Studieinfo – ansökningsblankett"}
   :permission-for-electronic-transactions      {:fi "Opiskelijavalinnan tulokset saa lähettää minulle sähköisesti."
                                                 :sv "Mitt antagningsresultat får skickas elektroniskt till mig."
                                                 :en "Opiskelijavalinnan tulokset saa lähettää minulle sähköisesti."}
   :permission-for-electronic-transactions-info {:fi "Täyttämällä sähköisen hakulomakkeen annat samalla luvan siihen, että opiskelijavalintaan liittyvä viestintä hoidetaan pelkästään sähköisesti. Jos et suostu näihin ehtoihin, ota yhteyttä korkeakoulun hakijapalveluihin."
                                                 :sv "Genom att fylla i denna elektroniska ansökningsblankett ger du samtidigt ditt medgivande till att kommunikationen gällande studerandeantagningen kan skötas enbart elektroniskt. Om du inte går med på dessa villkor, kontakta ansökningsservicen vid högskolan."
                                                 :en "By filling in this electronic application form you also give your consent that communication regarding student admissions can be carried out only by email. If you do not agree to these terms, please contact the admissions services of the higher education institution that you are applying to."}
   :permissions                                 {:fi "Lupatiedot"
                                                 :sv "Tilläggsuppgifter"
                                                 :en "Permissions"}
   :pohjakoulutusvaatimus                       {:fi "Pohjakoulutusvaatimus"
                                                 :sv "Grundutbildningskrav"
                                                 :en "Pohjakoulutusvaatimus"}
   :preview                                     {:fi "Esikatselu"
                                                 :en "Preview"
                                                 :sv "Förhandsvisa"}
   :question-for-hakukohde                      {:fi "Kysymys kuuluu hakukohteisiin"
                                                 :en "This question is for application options"
                                                 :sv "Frågan berör ansökningsmål"}
   :read-less                                   {:fi "Sulje ohje"
                                                 :sv "Dölj anvisning"
                                                 :en "Hide instructions"}
   :read-more                                   {:fi "Lue lisää"
                                                 :sv "Läs mer"
                                                 :en "Read more"}
   :remove                                      {:fi "Poista"
                                                 :sv "Ta bort"
                                                 :en "Remove"}
   :remove-row                                  {:fi "Poista rivi"
                                                 :sv "Ta bort rad"
                                                 :en "Remove row"}
   :search-application-options                  {:fi "Etsi tämän haun koulutuksia"
                                                 :sv "Sök ansökningsmål i denna ansökan"
                                                 :en "Search for application options"}
   :second                                      {:fi "sekunti"
                                                 :en "second"
                                                 :sv "sekund"}
   :seconds                                     {:fi "sekuntia"
                                                 :en "seconds"
                                                 :sv "sekunder"}
   :show-more                                   {:fi "Näytä lisää.."
                                                 :en "Show more.."
                                                 :sv "Visa mer.."}
   :swedish                                     {:fi "Ruotsi"
                                                 :sv "Svenska"
                                                 :en "Swedish"}
   :window-close-warning                        {:fi "Varmistathan että hakemus on lähetetty ennen sivun sulkemista."
                                                 :en "Please ensure you have submitted the form before closing the page."
                                                 :sv "Försäkra dig om att du har skickat din ansökan innan du stänger sidan"}})

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
   :have-you-completed               {:en "Have you completed general upper secondary education or vocational qualification?"
                                      :fi "Oletko suorittanut lukion/ylioppilastutkinnon tai ammatillisen tutkinnon?"
                                      :sv "Har du avlagt gymnasiet/studentexamen eller yrkesinriktad examen?"}
   :choose-country                   {:en "Choose the country where you have completed your most recent qualification. If you have not yet completed a general upper secondary school syllabus/matriculation examination or vocational qualification, but are in the process of doing so, please choose the country where you will complete the qualification. NB: a vocational qualification can be a vocational upper secondary qualification, school-level qualification, post-secondary level qualification, higher vocational level qualification, further vocational qualification or specialist vocational qualification. Do not fill in the country where you have completed a higher education qualification."
                                      :fi "Merkitse viimeisimmän tutkintosi suoritusmaa. Jos sinulla ei ole vielä lukion päättötodistusta/ylioppilastutkintoa tai ammatillista tutkintoa mutta olet suorittamassa sellaista, valitse se maa, jossa parhaillaan suoritat kyseistä tutkintoa. Huom: ammatillinen tutkinto voi olla ammatillinen perustutkinto, kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto, ammatti-tai erikoisammattitutkinto. Älä merkitse tähän korkeakoulututkinnon suoritusmaata."
                                      :sv "Ange land där din senaste examen avlagts. Om du ännu inte har avlagt gymnasiet/studentexamen eller yrkesinriktad examen men håller på att göra det, välj då det land där du som bäst avlägger examen i fråga. Obs: yrkesinriktad examen kan vara yrkesinriktad grundexamen, examen på skolnivå, examen på institutsnivå, yrkesinriktad examen på högre nivå, yrkesexamen eller specialyrkesexamen. Ange inte det land där du avlagt högskoleexamen."}})

(def higher-base-education-module-texts
  {:educational-background                                 {:en "Your educational background" :fi "Koodistopohjainen pohjakoulutusosio" :sv "Utbildningsbakgrund"}
   :completed-education                                    {:en "Fill in the education that you have completed  or will complete during the admission process (autumn 2018)"
                                                            :fi "Ilmoita kaikki suorittamasi koulutukset. Myös ne jotka suoritat hakukautena (syksy 2018)."
                                                            :sv "Ange alla utbildningar som du har avlagt. Ange också dem som du avlägger under ansökningsperioden (hösten 2018)."}
   :marticulation-exam-in-finland                          {:en "Matriculation examination"
                                                            :fi "Suomessa suoritettu ylioppilastutkinto"
                                                            :sv "Studentexamen som avlagts i Finland"}
   :marticulation-exam                                     {:en "Matriculation examination"
                                                            :fi "Ylioppilastutkinto"
                                                            :sv "Studentexamen"}
   :completed-marticaulation-before-1990?                  {:en "Have you completed your Matriculation examination in Finland in 1990 or after?"
                                                            :fi "Oletko suorittanut ylioppilastutkinnon vuonna 1990 tai sen jälkeen?"
                                                            :sv "Har du avlagt studentexamen år 1990 eller senare?"}
   :marticaulation-before-1990                             {:en "Matriculation examination (completed before 1990)"
                                                            :fi "Ylioppilastutkinto (ennen vuotta 1990)"
                                                            :sv "Studentexamen (före år 1990)"}
   :year-of-completion                                     {:en "Year of completion"
                                                            :fi "Suoritusvuosi"
                                                            :sv "Avlagd år"}
   :automatic-marticulation-info                           {:en "Your matriculation examination details are received automatically from the Matriculation Examination Board."
                                                            :fi "Saamme ylioppilastutkintosi tiedot rekisteristämme."
                                                            :sv "Vi får uppgifterna om din studentexamen ur vårt register."}
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
   :upper-secondary-school-attachment                      {:en "Upper secondary school certificate"
                                                            :fi "Lukion päättötodistus"
                                                            :sv "Gymnasiets avgångsbetyg"}
   :international-marticulation-exam                       {:en "International matriculation examination completed in Finland"
                                                            :fi "Suomessa suoritettu kansainvälinen ylioppilastutkinto"
                                                            :sv "Internationell studentexamen som avlagts i Finland"}
   :international-baccalaureate                            {:en "International Baccalaureate -diploma"
                                                            :fi "International Baccalaureate"
                                                            :sv "International Baccalaureate -examen"}
   :european-baccalaureate                                 {:en "European Baccalaureate -diploma"
                                                            :fi " Eurooppalainen ylioppilastutkinto"
                                                            :sv "European Baccalaureate -examen"}
   :reifeprufung                                           {:en "Reifeprüfung - diploma/ Deutsche Internationale Abiturprüfung"
                                                            :fi "Reifeprüfung"
                                                            :sv "Reifeprüfung - examen"}
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
   :scope-of-qualification                                 {:en "Scope of qualification"
                                                            :fi "Tutkinnon laajuus"
                                                            :sv "Examens omfattning"}
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
   :finnish-vocational-or-special                          {:en "Further vocational qualification or specialist vocational qualification completed in Finland"
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
   :higher-education-institution                           {:en "Higher education institution"
                                                            :fi "Korkeakoulu"
                                                            :sv "Högskola"}
   :higher-education-degree                                {:en "Higher education degree"
                                                            :fi "Korkeakoulututkinto"
                                                            :sv "Högskoleexamen"}
   :international-marticulation-outside-finland            {:en "International matriculation examination completed outside Finland"
                                                            :fi "Muualla kuin Suomessa suoritettu kansainvälinen ylioppilastutkinto"
                                                            :sv "Internationell studentexamen som avlagts annanstans än i Finland"}
   :international-marticulation-outside-finland-name       {:en "Name of examination/diploma"
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
   :required-for-statistics                                {:fi "Tämä tieto kysytään tilastointia varten."
                                                            :sv "Uppgiften insamlas för statistik."
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
                          :sv "Näradress"
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
                          :sv "Denna del införs automatiskt i blanketten"
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
                                 :sv "Om du vill ändra din ansökan, kan du göra ändringar via länken ovan under ansökningstiden. Dela inte länken vidare till utomstående. Kom ihåg att logga ut från e-postprogrammet om du använder en offentlig dator.\n\nOm du har nätbankskoder, mobilcertifikat eller ett elektroniskt ID-kort, kan du alternativt logga in i [Studieinfo.fi](https://www.studieinfo.fi) och under ansökningstiden göra ändringarna i tjänsten Min Studieinfo. I tjänsten kan du också se ditt antagningsresultat och ta emot studieplatsen.\n\nSvara inte på detta meddelande, det har skickats automatiskt.\n\nMed vänliga hälsningar, <br/>\nStudieinfo\n"
                                 :en "If you wish to edit your application, you can use the link above and make the changes within the application period. Do not share the link with others. If you are using a public or shared computer, remember to log out of the email application.\n\nIf you have Finnish online banking credentials, an electronic ID-card or mobile certificate, you can also log in at [Studyinfo.fi](https://www.studyinfo.fi) and make the changes in the My Studyinfo -service within the application period. In addition to making changes to your application, if you have access to the My Studyinfo -service you can also view the admission results and confirm the study place.\n\nThis is an automatically generated email, please do not reply.\n\nBest regards, <br/>\nStudyinfo\n"}
    :without-application-period {:fi "Voit katsella ja muokata hakemustasi yllä olevan linkin kautta. Älä jaa linkkiä ulkopuolisille. Jos käytät yhteiskäyttöistä tietokonetta, muista kirjautua ulos sähköpostiohjelmasta.\n\nJos sinulla on verkkopankkitunnukset, mobiilivarmenne tai sähköinen henkilökortti, voit vaihtoehtoisesti kirjautua sisään [Opintopolku.fi](https://www.opintopolku.fi):ssä, ja tehdä muutoksia hakemukseesi Oma Opintopolku -palvelussa hakuaikana. Oma Opintopolku -palvelussa voit lisäksi nähdä valintojen tulokset ja ottaa opiskelupaikan vastaan.\n\nÄlä vastaa tähän viestiin - viesti on lähetetty automaattisesti.\n\nYstävällisin terveisin <br/>\nOpintopolku\n"
                                 :sv "Om du vill ändra din ansökan, kan du göra ändringar via länken ovan. Dela inte länken vidare till utomstående. Kom ihåg att logga ut från e-postprogrammet om du använder en offentlig dator.\n\nOm du har nätbankskoder, mobilcertifikat eller ett elektroniskt ID-kort, kan du alternativt logga in i [Studieinfo.fi](https://www.studieinfo.fi) och under ansökningstiden göra ändringarna i tjänsten Min Studieinfo. I tjänsten kan du också, se antagningsresultaten och ta emot studieplatsen.\n\nSvara inte på detta meddelande, det har skickats automatiskt.\n\nMed vänliga hälsningar, <br/>\nStudieinfo\n"
                                 :en "If you wish to edit your application, you can use the link above and make the changes within the application period. Do not share the link with others. If you are using a public or shared computer, remember to log out of the email application.\n\nIf you have Finnish online banking credentials, an electronic\nID-card or mobile certificate, you can also log in\nat [Studyinfo.fi](https://www.studyinfo.fi) and make the\nchanges in the My Studyinfo -service within the application period. In addition to making changes to your application, if you have access to the My Studyinfo -service you can also view the admission results and confirm the study place.\n\nThis is an automatically generated email, please do not reply.\n\nBest regards, <br/>\nStudyinfo\n"}}})

(def virkailija-texts
  {:active                                      {:fi "Aktiivinen"
                                                 :sv "SV: Aktiivinen"
                                                 :en "EN: Aktiivinen"}
   :add                                         {:fi "Lisää"
                                                 :sv "SV: Lägg till"
                                                 :en "EN: Add more"}
   :adjacent-fieldset                           {:fi "Vierekkäiset tekstikentät"
                                                 :sv "SV: Vierekkäiset tekstikentät"
                                                 :en "EN: Vierekkäiset tekstikentät"}
   :all                                         {:fi "Kaikki"
                                                 :sv "SV: Kaikki"
                                                 :en "EN: Kaikki"}
   :all-hakukohteet                             {:fi "Kaikki hakukohteet"
                                                 :sv "SV: Kaikki hakukohteet"
                                                 :en "EN: Kaikki hakukohteet"}
   :alphabetically                              {:fi "Aakkosjärjestyksessä"
                                                 :sv "SV: Aakkosjärjestyksessä"
                                                 :en "EN: Aakkosjärjestyksessä"}
   :answers                                     {:fi "vastausta:"
                                                 :sv "SV: vastausta:"
                                                 :en "EN: vastausta:"}
   :applicant                                   {:fi "Hakija"
                                                 :sv "SV: Hakija"
                                                 :en "EN: Hakija"}
   :applicant-will-receive-following-email      {:fi "Hakija saa allaolevan viestin sähköpostilla hakemuksen lähettämisen jälkeen lähettäjältä"
                                                 :sv "SV: Hakija saa allaolevan viestin sähköpostilla hakemuksen lähettämisen jälkeen lähettäjältä"
                                                 :en "EN: Hakija saa allaolevan viestin sähköpostilla hakemuksen lähettämisen jälkeen lähettäjältä"}
   :application                                 {:fi "hakemus"
                                                 :sv "SV: hakemus"
                                                 :en "EN: hakemus"}
   :application-oid-here                        {:fi "Tähän tulee hakemusnumero, hakutoiveet, puuttuvat liitepyynnöt ja muokkauslinkki"
                                                 :sv "SV: Tähän tulee hakemusnumero, hakutoiveet, puuttuvat liitepyynnöt ja muokkauslinkki"
                                                 :en "EN: Tähän tulee hakemusnumero, hakutoiveet, puuttuvat liitepyynnöt ja muokkauslinkki"}
   :application-options                         {:fi "hakukohdetta"
                                                 :sv "SV: hakukohdetta"
                                                 :en "EN: hakukohdetta"}
   :application-received                        {:fi "Hakemus vastaanotettu"
                                                 :sv "SV: Hakemus vastaanotettu"
                                                 :en "EN: Hakemus vastaanotettu"}
   :application-state                           {:fi "Hakemuksen tila"
                                                 :sv "SV: Hakemuksen tila"
                                                 :en "EN: Hakemuksen tila"}
   :applications                                {:fi "hakemusta"
                                                 :sv "SV: hakemusta"
                                                 :en "EN: hakemusta"}
   :applications-panel                          {:fi "Hakemukset"
                                                 :sv "SV: Hakemukset"
                                                 :en "EN: Hakemukset"}
   :asiointikieli                               {:fi "Asiointikieli"
                                                 :sv "SV: Asiointikieli"
                                                 :en "EN: Asiointikieli"}
   :attachment                                  {:fi "Liitepyyntö"
                                                 :sv "SV: Liitepyyntö"
                                                 :en "EN: Liitepyyntö"}
   :attachment-info-text                        {:fi "Liitepyyntö sisältää ohjetekstin"
                                                 :sv "SV: Liitepyyntö sisältää ohjetekstin"
                                                 :en "EN: Liitepyyntö sisältää ohjetekstin"}
   :attachment-name                             {:fi "Liitteen nimi"
                                                 :sv "SV: Liitteen nimi"
                                                 :en "EN: Liitteen nimi"}
   :attachments                                 {:fi "Liitepyynnöt"
                                                 :sv "SV: Liitepyynnöt"
                                                 :en "EN: Liitepyynnöt"}
   :base-education                              {:fi "Pohjakoulutus"
                                                 :sv "SV: Pohjakoulutus"
                                                 :en "EN: Pohjakoulutus"}
   :base-education-module                       {:fi "Pohjakoulutusmoduuli"
                                                 :sv "SV: Pohjakoulutusmoduuli"
                                                 :en "EN: Pohjakoulutusmoduuli"}
   :change                                      {:fi "Muuta"
                                                 :sv "SV: Muuta"
                                                 :en "EN: Muuta"}
   :change-organization                         {:fi "Vaihda organisaatio"
                                                 :sv "SV: Vaihda organisaatio"
                                                 :en "EN: Vaihda organisaatio"}
   :changed                                     {:fi "muutti"
                                                 :sv "SV: muutti"
                                                 :en "EN: muutti"}
   :changes                                     {:fi "muutosta"
                                                 :sv "SV: muutosta"
                                                 :en "EN: muutosta"}
   :checking                                    {:fi "Tarkastetaan"
                                                 :sv "SV: Tarkastetaan"
                                                 :en "EN: Tarkastetaan"}
   :choose-user-rights                          {:fi "Valitse käyttäjän oikeudet"
                                                 :sv "SV: Valitse käyttäjän oikeudet"
                                                 :en "EN: Valitse käyttäjän oikeudet"}
   :close                                       {:fi "sulje"
                                                 :sv "stäng"
                                                 :en "EN: sulje"}
   :collapse-info-text                          {:fi "Pienennä pitkä ohjeteksti"
                                                 :sv "SV: Pienennä pitkä ohjeteksti"
                                                 :en "EN: Pienennä pitkä ohjeteksti"}
   :compare                                     {:fi "Vertaile"
                                                 :sv "SV: Vertaile"
                                                 :en "EN: Vertaile"}
   :confirm-change                              {:fi "Vahvista muutos"
                                                 :sv "SV: Vahvista muutos"
                                                 :en "EN: Vahvista muutos"}
   :confirm-delete                              {:fi "Vahvista poisto"
                                                 :sv "Bekräfta raderingen"
                                                 :en "EN: Vahvista poisto"}
   :confirmation-sent                           {:fi "Vahvistussähköposti lähetetty hakijalle"
                                                 :sv "SV: Vahvistussähköposti lähetetty hakijalle"
                                                 :en "EN: Vahvistussähköposti lähetetty hakijalle"}
   :contains-fields                             {:fi "Sisältää kentät:"
                                                 :sv "SV: Sisältää kentät:"
                                                 :en "EN: Sisältää kentät:"}
   :copy-form                                   {:fi "Kopioi lomake"
                                                 :sv "Kopiera blanketten"
                                                 :en "EN: Kopioi lomake"}
   :copy-answer-id                              {:fi "Kopioi vastauksen tunniste leikepöydälle"
                                                 :sv "SV: Kopioi vastauksen tunniste leikepöydälle"
                                                 :en "EN: Kopioi vastauksen tunniste leikepöydälle"}
   :copy-question-id                            {:fi "Kopioi kysymyksen tunniste leikepöydälle"
                                                 :sv "SV: Kopioi kysymyksen tunniste leikepöydälle"
                                                 :en "EN: Kopioi kysymyksen tunniste leikepöydälle"}
   :created-by                                  {:fi "Luonut"
                                                 :sv "SV: Luonut"
                                                 :en "EN: Luonut"}
   :custom-choice-label                         {:fi "Omat vastausvaihtoehdot"
                                                 :sv "SV: Omat vastausvaihtoehdot"
                                                 :en "EN: Omat vastausvaihtoehdot"}
   :decimals                                    {:fi "desimaalia"
                                                 :sv "SV: decimaler"
                                                 :en "EN: decimals"}
   :delete-form                                 {:fi "Poista lomake"
                                                 :sv "SV: Ta bort blanketten"
                                                 :en "EN: Poista lomake"}
   :did                                         {:fi "teki"
                                                 :sv "SV: teki"
                                                 :en "EN: teki"}
   :diff-from-changes                           {:fi "Vertailu muutoksesta"
                                                 :sv "SV: Vertailu muutoksesta"
                                                 :en "EN: Vertailu muutoksesta"}
   :dropdown                                    {:fi "Pudotusvalikko"
                                                 :sv "SV: Pudotusvalikko"
                                                 :en "EN: Pudotusvalikko"}
   :edit-application                            {:fi "Muokkaa hakemusta"
                                                 :sv "SV: Muokkaa hakemusta"
                                                 :en "EN: Muokkaa hakemusta"}
   :edit-applications-rights-panel              {:fi "Hakemusten arviointi"
                                                 :sv "SV: Hakemusten arviointi"
                                                 :en "EN: Hakemusten arviointi"}
   :edit-email-templates                        {:fi "Muokkaa sähköpostipohjia"
                                                 :sv "Bearbeta epostmallar"
                                                 :en "EN: Muokkaa sähköpostipohjia"}
   :edit-link-sent-automatically                {:fi "Muokkauslinkki lähtee viestin mukana automaattisesti"
                                                 :sv "SV: Muokkauslinkki lähtee viestin mukana automaattisesti"
                                                 :en "EN: Muokkauslinkki lähtee viestin mukana automaattisesti"}
   :editable-content-beginning                  {:fi "Muokattava osuus (viestin alku)"
                                                 :sv "SV: Muokattava osuus (viestin alku)"
                                                 :en "EN: Muokattava osuus (viestin alku)"}
   :editable-content-ending                     {:fi "Muokattava osuus (viestin loppu)"
                                                 :sv "SV: Muokattava osuus (viestin loppu)"
                                                 :en "EN: Muokattava osuus (viestin loppu)"}
   :editable-content-title                      {:fi "Muokattava osuus (otsikko)"
                                                 :sv "SV: Muokattava osuus (otsikko)"
                                                 :en "EN: Muokattava osuus (otsikko)"}
   :eligibility                                 {:fi "Hakukelpoisuus:"
                                                 :sv "SV: Hakukelpoisuus:"
                                                 :en "EN: Hakukelpoisuus:"}
   :eligibility-explanation                     {:fi "Kelpoisuusmerkinnän selite"
                                                 :sv "SV: Kelpoisuusmerkinnän selite"
                                                 :en "EN: Kelpoisuusmerkinnän selite"}
   :eligibility-set-automatically               {:fi "Hakukelpoisuus asetettu automaattisesti"
                                                 :sv "SV: Hakukelpoisuus asetettu automaattisesti"
                                                 :en "EN: Hakukelpoisuus asetettu automaattisesti"}
   :email-content                               {:fi "Sähköpostiviestin sisältö"
                                                 :sv "SV: Sähköpostiviestin sisältö"
                                                 :en "EN: Sähköpostiviestin sisältö"}
   :english                                     {:fi "Englanti"
                                                 :sv "SV: Engelska"
                                                 :en "EN: English"}
   :ensisijaisesti                              {:fi "Hakenut ensisijaisesti"
                                                 :sv "SV: Hakenut ensisijaisesti"
                                                 :en "EN: Hakenut ensisijaisesti"}
   :error                                       {:fi "Virhe"
                                                 :sv "SV: Virhe"
                                                 :en "EN: Virhe"}
   :events                                      {:fi "Tapahtumat"
                                                 :sv "SV: Tapahtumat"
                                                 :en "EN: Tapahtumat"}
   :filter-applications                         {:fi "Rajaa hakemuksia"
                                                 :sv "SV: Rajaa hakemuksia"
                                                 :en "EN: Rajaa hakemuksia"}
   :finnish                                     {:fi "Suomi"
                                                 :sv "SV: Finska"
                                                 :en "EN: Finnish"}
   :followups                                   {:fi "Lisäkysymykset"
                                                 :sv "Lisäkysymykset"
                                                 :en "Lisäkysymykset"}
   :for-hakukohde                               {:fi "hakukohteelle"
                                                 :sv "SV: hakukohteelle"
                                                 :en "EN: hakukohteelle"}
   :form                                        {:fi "Lomake"
                                                 :sv "Blankett"
                                                 :en "EN: Lomake"}
   :form-edit-rights-panel                      {:fi "Lomakkeiden muokkaus"
                                                 :sv "SV: Lomakkeiden muokkaus"
                                                 :en "EN: Lomakkeiden muokkaus"}
   :form-locked                                 {:fi "Lomakkeen muokkaus on estetty"
                                                 :sv "Du kan inte bearbeta blanketten"
                                                 :en "EN: Lomakkeen muokkaus on estetty"}
   :form-name                                   {:fi "Lomakkeen nimi"
                                                 :sv "Blankettens namn"
                                                 :en "EN: Lomakkeen nimi"}
   :form-section                                {:fi "Lomakeosio"
                                                 :sv "SV: Lomakeosio"
                                                 :en "EN: Lomakeosio"}
   :forms                                       {:fi "Lomakkeet"
                                                 :sv "Blanketter"
                                                 :en "EN: Lomakkeet"}
   :forms-panel                                 {:fi "Lomakkeet"
                                                 :sv "SV: Lomakkeet"
                                                 :en "EN: Lomakkeet"}
   :from-applicant                              {:fi "Hakijalta"
                                                 :sv "SV: Hakijalta"
                                                 :en "EN: Hakijalta"}
   :from-state                                  {:fi "Tilasta"
                                                 :sv "SV: Tilasta"
                                                 :en "EN: Tilasta"}
   :group                                       {:fi "ryhmä"
                                                 :sv "grupp"
                                                 :en "EN: ryhmä"}
   :group-header                                {:fi "Kysymysryhmän otsikko"
                                                 :sv "SV: Kysymysryhmän otsikko"
                                                 :en "EN: Kysymysryhmän otsikko"}
   :hakukohde-info                              {:fi "Tässä hakija voi valita hakukohteet. Hakukohteiden määrä ja priorisointi määritetään haun asetuksissa."
                                                 :sv "Sökande kan här välja ansökningsmål. Antalet ansökningsmål och prioriseringen definieras i inställningarna för ansökan."
                                                 :en "EN: Tässä hakija voi valita hakukohteet. Hakukohteiden määrä ja priorisointi määritetään haun asetuksissa."}
   :hakukohteet                                 {:fi "Hakukohteet"
                                                 :sv "SV: Hakukohteet"
                                                 :en "EN: Hakukohteet"}
   :handling-notes                              {:fi "Käsittelymerkinnät"
                                                 :sv "SV: Käsittelymerkinnät"
                                                 :en "EN: Käsittelymerkinnät"}
   :hide-options                                {:fi "Sulje vastausvaihtoehdot"
                                                 :sv "SV: Sulje vastausvaihtoehdot"
                                                 :en "EN: Sulje vastausvaihtoehdot"}
   :identified                                  {:fi "Yksilöidyt"
                                                 :sv "SV: Yksilöidyt"
                                                 :en "EN: Yksilöidyt"}
   :identifying                                 {:fi "Yksilöinti"
                                                 :sv "SV: Yksilöinti"
                                                 :en "EN: Yksilöinti"}
   :incomplete                                  {:fi "Kesken"
                                                 :sv "SV: Kesken"
                                                 :en "EN: Kesken"}
   :info-addon                                  {:fi "Kysymys sisältää ohjetekstin"
                                                 :sv "SV: Frågan sisältää ohjetekstin"
                                                 :en "EN: Kysymys sisältää ohjetekstin"}
   :info-element                                {:fi "Infoteksti"
                                                 :sv "SV: Infoteksti"
                                                 :en "EN: Infoteksti"}
   :information-request-sent                    {:fi "Täydennyspyyntö lähetetty"
                                                 :sv "SV: Täydennyspyyntö lähetetty"
                                                 :en "EN: Täydennyspyyntö lähetetty"}
   :integer                                     {:fi "kokonaisluku"
                                                 :sv "SV: heltal"
                                                 :en "EN: integer"}
   :kk-base-education-module                    {:fi "Pohjakoulutusmoduuli (kk-yhteishaku)"
                                                 :sv "SV: Pohjakoulutusmoduuli (kk-yhteishaku)"
                                                 :en "EN: Pohjakoulutusmoduuli (kk-yhteishaku)"}
   :koodisto                                    {:fi "Koodisto"
                                                 :sv "SV: Koodisto"
                                                 :en "EN: Koodisto"}
   :koulutusmarkkinointilupa                    {:fi "Koulutusmarkkinointilupa"
                                                 :sv "SV: Koulutusmarkkinointilupa"
                                                 :en "EN: Koulutusmarkkinointilupa"}
   :last-modified                               {:fi "Viimeksi muokattu"
                                                 :sv "SV: Viimeksi muokattu"
                                                 :en "EN: Viimeksi muokattu"}
   :last-modified-by                            {:fi "viimeksi muokannut"
                                                 :sv "SV: viimeksi muokannut"
                                                 :en "EN: viimeksi muokannut"}
   :link-to-form                                {:fi "Linkki lomakkeeseen"
                                                 :sv "Länk till blanketten"
                                                 :en "EN: Linkki lomakkeeseen"}
   :load-excel                                  {:fi "Lataa Excel"
                                                 :sv "SV: Lataa Excel"
                                                 :en "EN: Lataa Excel"}
   :lock-form                                   {:fi "Lukitse lomake"
                                                 :sv "Lås blanketten"
                                                 :en "EN: Lukitse lomake"}
   :logout                                      {:fi "Kirjaudu ulos"
                                                 :sv "SV: Kirjaudu ulos"
                                                 :en "EN: Kirjaudu ulos"}
   :lupa-sahkoiseen-asiointiin                  {:fi "Sähköisen asioinnin lupa"
                                                 :sv "SV: Sähköisen asioinnin lupa"
                                                 :en "EN: Sähköisen asioinnin lupa"}
   :lupatiedot                                  {:fi "Lupatiedot"
                                                 :sv "Tilläggsuppgifter"
                                                 :en "Permissions"}
   :mass-edit                                   {:fi "Massamuutos"
                                                 :sv "SV: Massamuutos"
                                                 :en "EN: Massamuutos"}
   :mass-information-request                    {:fi "Massaviesti"
                                                 :sv "SV-Massaviesti"
                                                 :en "EN-massaviesti"}
   :mass-information-request-confirm-n-messages (fn [n] {:fi (format "Vahvista %d viestin lähetys" n)
                                                         :sv (format "SV-Vahvista %d viestin lähetys" n)
                                                         :en (format "EN-Vahvista %d viestin lähetys" n)})
   :mass-information-request-email-n-recipients (fn [n] {:fi (format "Lähetä sähköposti %d hakijalle:" n)
                                                         :sv (format "SV-Lähetä sähköposti %d hakijalle:" n)
                                                         :en (format "EN-Lähetä sähköposti %d hakijalle:" n)})
   :mass-information-request-messages-sent      {:fi "Viestit lähetetty!"
                                                 :sv "SV-Viestit lähetetty!"
                                                 :en "SV-Viestit lähetetty!"}
   :mass-information-request-send               {:fi "Lähetä"
                                                 :sv "SV-Lähetä:"
                                                 :en "EN-Lähetä:"}
   :mass-information-request-sending-messages   {:fi "Lähetetään viestejä..."
                                                 :sv "SV-Lähetetään viestejä..."
                                                 :en "EN-Lähetetään viestejä..."}
   :mass-information-request-subject            {:fi "Aihe:"
                                                 :sv "SV-Aihe:"
                                                 :en "EN-Aihe:"}
   :max-characters                              {:fi "Max. merkkimäärä"
                                                 :sv "SV: Max. merkkimäärä"
                                                 :en "EN: Max. merkkimäärä"}
   :md-help-bold                                {:fi "**lihavoitava sisältö**"
                                                 :sv "SV: **fetstil innehåll**"
                                                 :en "EN: **bold content**"}
   :md-help-cursive                             {:fi "*kursivoitava sisältö*"
                                                 :sv "SV: *i kursiv stil*"
                                                 :en "EN: *cursive content*"}
   :md-help-link                                {:fi "[linkin teksti](http://linkin osoite)"
                                                 :sv "SV: [länkens text](http://länkens adress)"
                                                 :en "EN: [link text](http://link address)"}
   :md-help-more                                {:fi "Lisää muotoiluohjeita"
                                                 :sv "SV: Lisää muotoiluohjeita"
                                                 :en "EN: Lisää muotoiluohjeita"}
   :md-help-title                               {:fi "# otsikko (# ylin - ###### alin)"
                                                 :sv "SV: # otsikko (# högsta - ###### lägst)"
                                                 :en "EN: # title (# highest - ###### lowest)"}
   :message-preview                             {:fi "Viestin esikatselu"
                                                 :sv "SV: Viestin esikatselu"
                                                 :en "EN: Viestin esikatselu"}
   :more-results-refine-search                  {:fi "Lisää tuloksia, tarkenna hakua"
                                                 :sv "SV: Lisää tuloksia, tarkenna hakua"
                                                 :en "EN: Lisää tuloksia, tarkenna hakua"}
   :multiple-answers                            {:fi "Vastaaja voi lisätä useita vastauksia"
                                                 :sv "Du kan ge flera svar"
                                                 :en "EN: Vastaaja voi lisätä useita vastauksia"}
   :multiple-choice                             {:fi "Lista, monta valittavissa"
                                                 :sv "SV: Lista, monta valittavissa"
                                                 :en "EN: Lista, monta valittavissa"}
   :multiple-organizations                      {:fi "Useita organisaatioita"
                                                 :sv "SV: Useita organisaatioita"
                                                 :en "EN: Useita organisaatioita"}
   :new-form                                    {:fi "Uusi lomake"
                                                 :sv "Ny blankett"
                                                 :en "EN: Uusi lomake"}
   :no-organization                             {:fi "Ei organisaatiota"
                                                 :sv "SV: Ei organisaatiota"
                                                 :en "EN: Ei organisaatiota"}
   :notes                                       {:fi "Muistiinpanot"
                                                 :sv "SV: Muistiinpanot"
                                                 :en "EN: Muistiinpanot"}
   :of-form                                     {:fi "Lomakkeen"
                                                 :sv "SV: Lomakkeen"
                                                 :en "EN: Lomakkeen"}
   :of-hakukohde                                {:fi "Hakukohteen"
                                                 :sv "SV: Hakukohteen"
                                                 :en "EN: Hakukohteen"}
   :only-numeric                                {:fi "Kenttään voi täyttää vain numeroita"
                                                 :sv "SV: Kenttään voi täyttää vain numeroita"
                                                 :en "EN: Kenttään voi täyttää vain numeroita"}
   :open                                        {:fi "avaa"
                                                 :sv "öppna"
                                                 :en "EN: avaa"}
   :options                                     {:fi "Vastausvaihtoehdot"
                                                 :sv "SV: Vastausvaihtoehdot"
                                                 :en "EN: Vastausvaihtoehdot"}
   :passive                                     {:fi "Passiivinen"
                                                 :sv "SV: Passiivinen"
                                                 :en "EN: Passiivinen"}
   :person-completed-education                  {:fi "Henkilön suoritukset"
                                                 :sv "SV: Henkilön suoritukset"
                                                 :en "EN: Henkilön suoritukset"}
   :person-not-individualized                   {:fi "Hakijaa ei ole yksilöity."
                                                 :sv "SV: Hakijaa ei ole yksilöity."
                                                 :en "EN: Hakijaa ei ole yksilöity."}
   :pohjakoulutus_am                            {:fi "Suomessa suoritettu ammatillinen perustutkinto, kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto"
                                                 :sv "SV: Suomessa suoritettu ammatillinen perustutkinto, kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto"
                                                 :en "EN: Suomessa suoritettu ammatillinen perustutkinto, kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto"}
   :pohjakoulutus_amt                           {:fi "Suomessa suoritettu ammatti- tai erikoisammattitutkinto"
                                                 :sv "SV: Suomessa suoritettu ammatti- tai erikoisammattitutkinto"
                                                 :en "EN: Suomessa suoritettu ammatti- tai erikoisammattitutkinto"}
   :pohjakoulutus_avoin                         {:fi "Korkeakoulun edellyttämät avoimen korkeakoulun opinnot"
                                                 :sv "SV: Korkeakoulun edellyttämät avoimen korkeakoulun opinnot"
                                                 :en "EN: Korkeakoulun edellyttämät avoimen korkeakoulun opinnot"}
   :pohjakoulutus_kk                            {:fi "Suomessa suoritettu korkeakoulututkinto"
                                                 :sv "SV: Suomessa suoritettu korkeakoulututkinto"
                                                 :en "EN: Suomessa suoritettu korkeakoulututkinto"}
   :pohjakoulutus_kk_ulk                        {:fi "Muualla kuin Suomessa suoritettu korkeakoulututkinto"
                                                 :sv "SV: Muualla kuin Suomessa suoritettu korkeakoulututkinto"
                                                 :en "EN: Muualla kuin Suomessa suoritettu korkeakoulututkinto"}
   :pohjakoulutus_lk                            {:fi "Suomessa suoritettu lukion oppimäärä ilman ylioppilastutkintoa"
                                                 :sv "SV: Suomessa suoritettu lukion oppimäärä ilman ylioppilastutkintoa"
                                                 :en "EN: Suomessa suoritettu lukion oppimäärä ilman ylioppilastutkintoa"}
   :pohjakoulutus_muu                           {:fi "Muu korkeakoulukelpoisuus"
                                                 :sv "SV: Muu korkeakoulukelpoisuus"
                                                 :en "EN: Muu korkeakoulukelpoisuus"}
   :pohjakoulutus_ulk                           {:fi "Muualla kuin Suomessa suoritettu muu tutkinto, joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun"
                                                 :sv "SV: Muualla kuin Suomessa suoritettu muu tutkinto, joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun"
                                                 :en "EN: Muualla kuin Suomessa suoritettu muu tutkinto, joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun"}
   :pohjakoulutus_yo                            {:fi "Suomessa suoritettu ylioppilastutkinto"
                                                 :sv "SV: Suomessa suoritettu ylioppilastutkinto"
                                                 :en "EN: Suomessa suoritettu ylioppilastutkinto"}
   :pohjakoulutus_yo_ammatillinen               {:fi "Ammatillinen perustutkinto ja ylioppilastutkinto (kaksoistutkinto)"
                                                 :sv "SV: Ammatillinen perustutkinto ja ylioppilastutkinto (kaksoistutkinto)"
                                                 :en "EN: Ammatillinen perustutkinto ja ylioppilastutkinto (kaksoistutkinto)"}
   :pohjakoulutus_yo_kansainvalinen_suomessa    {:fi "Suomessa suoritettu kansainvälinen ylioppilastutkinto"
                                                 :sv "SV: Suomessa suoritettu kansainvälinen ylioppilastutkinto"
                                                 :en "EN: Suomessa suoritettu kansainvälinen ylioppilastutkinto"}
   :pohjakoulutus_yo_ulkomainen                 {:fi "Muualla kuin Suomessa suoritettu kansainvälinen ylioppilastutkinto"
                                                 :sv "SV: Muualla kuin Suomessa suoritettu kansainvälinen ylioppilastutkinto"
                                                 :en "EN: Muualla kuin Suomessa suoritettu kansainvälinen ylioppilastutkinto"}
   :pohjakoulutusristiriita                     {:fi "Pohjakoulutusristiriita"
                                                 :sv "SV: Pohjakoulutusristiriita"
                                                 :en "EN: Pohjakoulutusristiriita"}
   :points                                      {:fi "Pisteet"
                                                 :sv "SV: Pisteet"
                                                 :en "EN: Pisteet"}
   :processed-haut                              {:fi "Käsitellyt haut"
                                                 :sv "SV: Käsitellyt haut"
                                                 :en "EN: Käsitellyt haut"}
   :processing-state                            {:fi "Käsittelyvaihe"
                                                 :sv "SV: Käsittelyvaihe"
                                                 :en "EN: Käsittelyvaihe"}
   :question                                    {:fi "Kysymys"
                                                 :sv "SV: Kysymys"
                                                 :en "EN: Kysymys"}
   :question-group                              {:fi "Kysymysryhmä"
                                                 :sv "SV: Kysymysryhmä"
                                                 :en "EN: Kysymysryhmä"}
   :receiver                                    {:fi "Vastaanottaja:"
                                                 :sv "SV: Vastaanottaja:"
                                                 :en "EN: Vastaanottaja:"}
   :rejection-reason                            {:fi "Hylkäyksen syy.."
                                                 :sv "SV: Hylkäyksen syy.."
                                                 :en "EN: Hylkäyksen syy.."}
   :remove                                      {:fi "Poista"
                                                 :sv "SV: Poista"
                                                 :en "EN: Poista"}
   :remove-filters                              {:fi "Poista rajaimet"
                                                 :sv "SV: Poista rajaimet"
                                                 :en "EN: Poista rajaimet"}
   :remove-lock                                 {:fi "Poista lukitus"
                                                 :sv "Öppna låset"
                                                 :en "EN: Poista lukitus"}
   :required                                    {:fi "Pakollinen tieto"
                                                 :sv "Obligatorisk uppgift"
                                                 :en "EN: Pakollinen tieto"}
   :reset-organization                          {:fi "Palauta oletusorganisaatio"
                                                 :sv "SV: Palauta oletusorganisaatio"
                                                 :en "EN: Palauta oletusorganisaatio"}
   :save                                        {:fi "Tallenna"
                                                 :sv "SV: Tallenna"
                                                 :en "EN: Tallenna"}
   :save-changes                                {:fi "Tallenna muutokset"
                                                 :sv "SV: Tallenna muutokset"
                                                 :en "EN: Tallenna muutokset"}
   :search-by-applicant-info                    {:fi "Etsi hakijan henkilötiedoilla"
                                                 :sv "SV: Etsi hakijan henkilötiedoilla"
                                                 :en "EN: Etsi hakijan henkilötiedoilla"}
   :search-sub-organizations                    {:fi "Etsi aliorganisaatioita"
                                                 :sv "SV: Etsi aliorganisaatioita"
                                                 :en "EN: Etsi aliorganisaatioita"}
   :search-terms-list                           {:fi "Nimi, henkilötunnus, syntymäaika tai sähköpostiosoite"
                                                 :sv "SV: Nimi, henkilötunnus, syntymäaika tai sähköpostiosoite"
                                                 :en "EN: Nimi, henkilötunnus, syntymäaika tai sähköpostiosoite"}
   :sections                                    {:fi "osiot"
                                                 :sv "delar"
                                                 :en "EN: osiot"}
   :selection                                   {:fi "Valinta"
                                                 :sv "SV: Valinta"
                                                 :en "EN: Valinta"}
   :send-confirmation-email-to-applicant        {:fi "Lähetä vahvistussähköposti hakijalle"
                                                 :sv "SV: Lähetä vahvistussähköposti hakijalle"
                                                 :en "EN: Lähetä vahvistussähköposti hakijalle"}
   :send-edit-link-to-applicant                 {:fi "Muokkauslinkki lähetetty hakijalle sähköpostilla"
                                                 :sv "SV: Muokkauslinkki lähetetty hakijalle sähköpostilla"
                                                 :en "EN: Muokkauslinkki lähetetty hakijalle sähköpostilla"}
   :send-information-request                    {:fi "Lähetä täydennyspyyntö"
                                                 :sv "SV: Lähetä täydennyspyyntö"
                                                 :en "EN: Lähetä täydennyspyyntö"}
   :send-information-request-to-applicant       {:fi "Lähetä täydennyspyyntö hakijalle"
                                                 :sv "SV: Lähetä täydennyspyyntö hakijalle"
                                                 :en "EN: Lähetä täydennyspyyntö hakijalle"}
   :sending-information-request                 {:fi "Täydennyspyyntöä lähetetään"
                                                 :sv "SV: Täydennyspyyntöä lähetetään"
                                                 :en "EN: Täydennyspyyntöä lähetetään"}
   :set-haku-to-form                            {:fi "Aseta ensin lomake haun käyttöön niin voit tehdä hakukohteen mukaan näkyviä sisältöjä."
                                                 :sv "SV: Aseta ensin lomake haun käyttöön niin voit tehdä hakukohteen mukaan näkyviä sisältöjä."
                                                 :en "EN: Aseta ensin lomake haun käyttöön niin voit tehdä hakukohteen mukaan näkyviä sisältöjä."}
   :settings                                    {:fi "Asetukset"
                                                 :sv "SV: Asetukset"
                                                 :en "EN: Asetukset"}
   :shape                                       {:fi "Muoto:"
                                                 :sv "SV: Muoto:"
                                                 :en "EN: Muoto:"}
   :show-more                                   {:fi "Näytä lisää.."
                                                 :sv "SV: Näytä lisää.."
                                                 :en "EN: Näytä lisää.."}
   :show-options                                {:fi "Näytä vastausvaihtoehdot"
                                                 :sv "SV: Näytä vastausvaihtoehdot"
                                                 :en "EN: Näytä vastausvaihtoehdot"}
   :single-choice-button                        {:fi "Painikkeet, yksi valittavissa"
                                                 :sv "SV: Painikkeet, yksi valittavissa"
                                                 :en "EN: Painikkeet, yksi valittavissa"}
   :student                                     {:fi "Oppija"
                                                 :sv "SV: Oppija"
                                                 :en "EN: Oppija"}
   :submitted-application                       {:fi "syötti hakemuksen"
                                                 :sv "SV: syötti hakemuksen"
                                                 :en "EN: syötti hakemuksen"}
   :submitted-at                                {:fi "Hakemus jätetty"
                                                 :sv "SV: Hakemus jätetty"
                                                 :en "EN: Hakemus jätetty"}
   :swedish                                     {:fi "Ruotsi"
                                                 :sv "SV: Svenska"
                                                 :en "EN: Finska"}
   :test-application                            {:fi "Testihakemus / Virkailijatäyttö"
                                                 :sv "Testansökan / Administratören fyller i"
                                                 :en "EN: Testihakemus / Virkailijatäyttö"}
   :text                                        {:fi "Teksti"
                                                 :sv "SV: Teksti"
                                                 :en "EN: Teksti"}
   :text-area                                   {:fi "Tekstialue"
                                                 :sv "SV: Tekstialue"
                                                 :en "EN: Tekstialue"}
   :text-area-size                              {:fi "Tekstialueen koko"
                                                 :sv "SV: Tekstialueen koko"
                                                 :en "EN: Tekstialueen koko"}
   :text-field                                  {:fi "Tekstikenttä"
                                                 :sv "SV: Tekstikenttä"
                                                 :en "EN: Tekstikenttä"}
   :text-field-size                             {:fi "Tekstikentän koko"
                                                 :sv "SV: Tekstikentän koko"
                                                 :en "EN: Tekstikentän koko"}
   :title                                       {:fi "Otsikko"
                                                 :sv "SV: Otsikko"
                                                 :en "EN: Otsikko"}
   :to-state                                    {:fi "Muutetaan tilaan"
                                                 :sv "SV: Muutetaan tilaan"
                                                 :en "EN: Muutetaan tilaan"}
   :unidentified                                {:fi "Yksilöimättömät"
                                                 :sv "SV: Yksilöimättömät"
                                                 :en "EN: Yksilöimättömät"}
   :unknown                                     {:fi "Tuntematon"
                                                 :sv "SV: Tuntematon"
                                                 :en "EN: Tuntematon"}
   :unknown-option                              {:fi "Tuntematon vastausvaihtoehto"
                                                 :sv "SV: Tuntematon vastausvaihtoehto"
                                                 :en "EN: Tuntematon vastausvaihtoehto"}
   :unprocessed                                 {:fi "Käsittelemättä"
                                                 :sv "SV: Käsittelemättä"
                                                 :en "EN: Käsittelemättä"}
   :unprocessed-haut                            {:fi "Käsittelemättä olevat haut"
                                                 :sv "SV: Käsittelemättä olevat haut"
                                                 :en "EN: Käsittelemättä olevat haut"}
   :used-by-haku                                {:fi "Tämä lomake on haun käytössä"
                                                 :sv "Denna blankett används i ansökan"
                                                 :en "EN: Tämä lomake on haun käytössä"}
   :used-by-haut                                {:fi "Tämä lomake on seuraavien hakujen käytössä"
                                                 :sv "Denna blankett används i följande ansökningar"
                                                 :en "EN: Tämä lomake on seuraavien hakujen käytössä"}
   :valintatuloksen-julkaisulupa                {:fi "Valintatuloksen julkaisulupa"
                                                 :sv "SV: Valintatuloksen julkaisulupa"
                                                 :en "EN : Valintatuloksen julkaisulupa"
                                                 :e  "E : Valintatuloksen julkaisulupa"}
   :view-applications-rights-panel              {:fi "Hakemusten katselu"
                                                 :sv "SV: Hakemusten katselu"
                                                 :en "EN: Hakemusten katselu"}
   :virus-found                                 {:fi "Virus löytyi"
                                                 :sv "SV: Virus löytyi"
                                                 :en "EN: Virus löytyi"}
   :visibility-on-form                          {:fi "Näkyvyys lomakkeella:"
                                                 :sv "SV: Näkyvyys lomakkeella:"
                                                 :en "EN: Näkyvyys lomakkeella:"}
   :visible-to-all                              {:fi "näkyy kaikille"
                                                 :sv "SV: näkyy kaikille"
                                                 :en "EN: näkyy kaikille"}
   :visible-to-hakukohteet                      {:fi "vain valituille hakukohteille",
                                                 :sv "SV: vain valituille hakukohteille",
                                                 :en "EN: vain valituille hakukohteille"}
   :wrapper-element                             {:fi "Lomakeosio"
                                                 :sv "SV: Lomakeosio"
                                                 :en "EN: Lomakeosio"}
   :wrapper-header                              {:fi "Osion nimi"
                                                 :sv "SV: Osion nimi"
                                                 :en "EN: Osion nimi"}})

(def state-translations
  {:active               {:fi "Aktiivinen"
                          :sv "SV: Aktiivinen"
                          :en "EN: Aktiivinen"}
   :passive              {:fi "Passiivinen"
                          :sv "SV: Passiivinen"
                          :en "EN: Passiivinen"}
   :unprocessed          {:fi "Käsittelemättä"
                          :sv "SV: Käsittelemättä"
                          :en "EN: Käsittelemättä"}
   :processing           {:fi "Käsittelyssä"
                          :sv "SV: Käsittelyssä"
                          :en "EN: Käsittelyssä"}
   :invited-to-interview {:fi "Kutsuttu haast."
                          :sv "SV: Kutsuttu haast."
                          :en "EN: Kutsuttu haast."}
   :invited-to-exam      {:fi "Kutsuttu valintak."
                          :sv "SV: Kutsuttu valintak."
                          :en "EN: Kutsuttu valintak."}
   :evaluating           {:fi "Arvioinnissa"
                          :sv "SV: Arvioinnissa"
                          :en "EN: Arvioinnissa"}
   :processed            {:fi "Käsitelty"
                          :sv "SV: Käsitelty"
                          :en "EN: Käsitelty"}
   :information-request  {:fi "Täydennyspyyntö"
                          :sv "SV: Täydennyspyyntö"
                          :en "EN: Täydennyspyyntö"}
   :incomplete           {:fi "Kesken"
                          :sv "SV: Kesken"
                          :en "EN: Kesken"}
   :selection            {:fi "Valintaesitys"
                          :sv "SV: Valintaesitys"
                          :en "EN: Valintaesitys"}
   :reserve              {:fi "Varalla"
                          :sv "SV: Varalla"
                          :en "EN: Varalla"}
   :selected             {:fi "Hyväksytty"
                          :sv "SV: Hyväksytty"
                          :en "EN: Hyväksytty"}
   :rejected             {:fi "Hylätty"
                          :sv "SV: Hylätty"
                          :en "EN: Hylätty"}
   :unreviewed           {:fi "Tarkastamatta"
                          :sv "SV: Tarkastamatta"
                          :en "EN: Tarkastamatta"}
   :fulfilled            {:fi "Täyttyy"
                          :sv "SV: Täyttyy"
                          :en "EN: Täyttyy"}
   :unfulfilled          {:fi "Ei täyty"
                          :sv "SV: Ei täyty"
                          :en "EN: Ei täyty"}
   :eligible             {:fi "Hakukelpoinen"
                          :sv "SV: Hakukelpoinen"
                          :en "EN: Hakukelpoinen"}
   :uneligible           {:fi "Ei hakukelpoinen"
                          :sv "SV: Ei hakukelpoinen"
                          :en "EN: Ei hakukelpoinen"}
   :obligated            {:fi "Velvollinen"
                          :sv "SV: Velvollinen"
                          :en "EN: Velvollinen"}
   :not-obligated        {:fi "Ei velvollinen"
                          :sv "SV: Ei velvollinen"
                          :en "EN: Ei velvollinen"}
   :processing-state     {:fi "Käsittelyvaihe"
                          :sv "SV: Käsittelyvaihe"
                          :en "EN: Käsittelyvaihe"}
   :language-requirement {:fi "Kielitaitovaatimus"
                          :sv "SV: Kielitaitovaatimus"
                          :en "EN: Kielitaitovaatimus"}
   :degree-requirement   {:fi "Tutkinnon kelpoisuus"
                          :sv "SV: Tutkinnon kelpoisuus"
                          :en "EN: Tutkinnon kelpoisuus"}
   :eligibility-state    {:fi "Hakukelpoisuus"
                          :sv "SV: Hakukelpoisuus"
                          :en "EN: Hakukelpoisuus"}
   :payment-obligation   {:fi "Maksuvelvollisuus"
                          :sv "SV: Maksuvelvollisuus"
                          :en "EN: Maksuvelvollisuus"}
   :selection-state      {:fi "Valinta"
                          :sv "SV: Valinta"
                          :en "EN: Valinta"}
   :not-checked          {:fi "Tarkastamatta"
                          :sv "SV: Tarkastamatta"
                          :en "EN: Tarkastamatta"}
   :checked              {:fi "Tarkistettu"
                          :sv "SV: Tarkistettu"
                          :en "EN: Tarkistettu"}
   :incomplete-answer    {:fi "Puutteellinen"
                          :sv "SV: Puutteellinen"
                          :en "EN: Puutteellinen"}
   :attachmentless       {:fi "Liitteettömät"
                          :sv "SV: Liitteettömät"
                          :en "EN: Liitteettömät"}})

(def excel-texts
  {:name                     {:fi "Nimi"
                              :sv "SV: Nimi"
                              :en "EN: Nimi"}
   :id                       {:fi "Id"
                              :sv "SV: Id"
                              :en "EN: Id"}
   :key                      {:fi "Tunniste"
                              :sv "SV: Tunniste"
                              :en "EN: Tunniste"}
   :created-time             {:fi "Viimeksi muokattu"
                              :sv "SV: Viimeksi muokattu"
                              :en "EN: Viimeksi muokattu"}
   :created-by               {:fi "Viimeinen muokkaaja"
                              :sv "SV: Viimeinen muokkaaja"
                              :en "EN: Viimeinen muokkaaja"}
   :sent-at                  {:fi "Lähetysaika"
                              :sv "SV: Lähetysaika"
                              :en "EN: Lähetysaika"}
   :application-state        {:fi "Hakemuksen tila"
                              :sv "SV: Hakemuksen tila"
                              :en "EN: Hakemuksen tila"}
   :hakukohde-handling-state {:fi "Hakukohteen käsittelyn tila"
                              :sv "SV: Hakukohteen käsittelyn tila"
                              :en "EN: Hakukohteen käsittelyn tila"}
   :kielitaitovaatimus       {:fi "Kielitaitovaatimus"
                              :sv "SV: Kielitaitovaatimus"
                              :en "EN: Kielitaitovaatimus"}
   :tutkinnon-kelpoisuus     {:fi "Tutkinnon kelpoisuus"
                              :sv "SV: Tutkinnon kelpoisuus"
                              :en "EN: Tutkinnon kelpoisuus"}
   :hakukelpoisuus           {:fi "Hakukelpoisuus"
                              :sv "SV: Hakukelpoisuus"
                              :en "EN: Hakukelpoisuus"}
   :maksuvelvollisuus        {:fi "Maksuvelvollisuus"
                              :sv "SV: Maksuvelvollisuus"
                              :en "EN: Maksuvelvollisuus"}
   :valinnan-tila            {:fi "Valinnan tila"
                              :sv "SV: Valinnan tila"
                              :en "EN: Valinnan tila"}
   :pisteet                  {:fi "Pisteet"
                              :sv "SV: Pisteet"
                              :en "EN: Pisteet"}
   :applicant-oid            {:fi "Hakijan henkilö-OID"
                              :sv "SV: Hakijan henkilö-OID"
                              :en "EN: Hakijan henkilö-OID"}
   :turvakielto              {:fi "Turvakielto"
                              :sv "SV: Turvakielto"
                              :en "EN: Turvakielto"}
   :notes                    {:fi "Muistiinpanot"
                              :sv "SV: Muistiinpanot"
                              :en "EN: Muistiinpanot"}})

(defn email-check-correct-notification
  [email]
  {:fi [:div
        [:p "Varmista, että antamasi sähköpostiosoite " [:strong email] " on
         kirjoitettu oikein ja se on henkilökohtainen. Lähetämme sinulle
         tärkeitä viestejä tähän sähköpostiosoitteeseen."]]
   :sv [:div
        [:p "Försäkra dig om att du skrivit din e-postadress " [:strong email]
         " rätt och att det är din personliga e-post. Vi skickar viktiga
         meddelanden till denna e-postadress."]]
   :en [:div
        [:p "Please ensure that the given email address " [:strong email] " is
         typed correctly and is your personal address. Important messages will
         be sent to this email address."]]})

(defn email-applied-error
  [email preferred-name]
  {:fi [:div
        [:p (if (not (clojure.string/blank? preferred-name))
              (str "Hei " preferred-name "!")
              "Hei!")]
        [:p "Huomasimme, että "
         [:strong "olet jo lähettänyt hakemuksen"]
         " tähän hakuun ja siksi et voi lähettää toista hakemusta."]
        [:p "Jos haluat "
         [:strong "muuttaa hakemustasi"]
         " niin löydät muokkauslinkin sähköpostiviestistä jonka sait
         jättäessäsi edellisen hakemuksen."]
        [:p "Tarkista myös, että syöttämäsi sähköpostiosoite "
         [:strong email]
         " on varmasti oikein."]
        [:p "Ongelmatilanteissa ole yhteydessä hakemaasi oppilaitokseen."]]
   :sv [:div
        [:p (if (not (clojure.string/blank? preferred-name))
              (str "Hej " preferred-name "!")
              "Hej!")]
        [:p "Vi märkte att "
         [:strong "du redan har skickat en ansökning"]
         " i denna ansökan och därför kan du inte skicka en annan
          ansökning."]
        [:p "Om du vill "
         [:strong "ändra din ansökning"]
         " hittar du bearbetningslänken i e-postmeddelandet som du fick när
          du skickade din tidigare ansökning."]
        [:p "Kontrollera även att e-postadressen du har angett "
         [:strong email]
         " säkert är korrekt."]
        [:p "Vid eventuella problemsituationer kontakta den läroanstalt du
         söker till."]]
   :en [:div
        [:p (if (not (clojure.string/blank? preferred-name))
              (str "Dear " preferred-name ",")
              "Dear applicant,")]
        [:p "we noticed that "
         [:strong "you have already submitted an application"]
         " to this admission. Therefore, you cannot submit another
          application to the same admission."]
        [:p "If you want to "
         [:strong "make changes"]
         " to your previous application, you can do so by clicking the link
          in the confirmation email you have received with your earlier
          application."]
        [:p "Please also check that the email address "
         [:strong email]
         " you have given is correct."]
        [:p "If you have any problems, please contact the educational
         institution."]]})

(defn email-applied-error-when-modifying
  [email preferred-name]
  {:fi [:div
        [:p (if (not (clojure.string/blank? preferred-name))
              (str "Hei " preferred-name "!")
              "Hei!")]
        [:p "Antamallasi sähköpostiosoitteella "
         [:strong email]
         " on jo jätetty hakemus. Tarkista, että syöttämäsi sähköpostiosoite
          on varmasti oikein."]]
   :sv [:div
        [:p (if (not (clojure.string/blank? preferred-name))
              (str "Hej " preferred-name "!")
              "Hej!")]
        [:p "En ansökning med den e-postadress du angett "
         [:strong email]
         " har redan gjorts. Kontrollera att e-postadressen du har angett
          säkert är korrekt."]]
   :en [:div
        [:p (if (not (clojure.string/blank? preferred-name))
              (str "Dear " preferred-name ",")
              "Dear applicant,")]
        [:p "the email address "
         [:strong email]
         " you have given in your application has already been used by
          another applicant. Please check that the email address you have
          given is correct."]]})

(defn ssn-applied-error
  [preferred-name]
  {:fi [:div
        [:p (if (not (clojure.string/blank? preferred-name))
              (str "Hei " preferred-name "!")
              "Hei!")]
        [:p "Huomasimme, että "
         [:strong "olet jo lähettänyt hakemuksen"]
         " tähän hakuun ja siksi et voi lähettää toista hakemusta."]
        [:p "Jos haluat "
         [:strong "muuttaa hakemustasi"]
         " niin löydät muokkauslinkin sähköpostiviestistä jonka sait
           jättäessäsi edellisen hakemuksen."]
        [:p "Ongelmatilanteissa ole yhteydessä hakemaasi oppilaitokseen."]]
   :sv [:div
        [:p (if (not (clojure.string/blank? preferred-name))
              (str "Hej " preferred-name "!")
              "Hej!")]
        [:p "Vi märkte att "
         [:strong "du redan har skickat en ansökning"]
         " i denna ansökan och därför kan du inte skicka en annan
           ansökning."]
        [:p "Om du vill "
         [:strong "ändra din ansökning"]
         " hittar du bearbetningslänken i e-postmeddelandet som du fick när
           du skickade din tidigare ansökning."]
        [:p "Vid eventuella problemsituationer kontakta den läroanstalt du
         söker till."]]
   :en [:div
        [:p (if (not (clojure.string/blank? preferred-name))
              (str "Dear " preferred-name ",")
              "Dear applicant,")]
        [:p "we noticed that "
         [:strong "you have already submitted an application"]
         " to this admission. Therefore, you cannot submit another
          application to the same admission."]
        [:p "If you want to "
         [:strong "make changes"]
         " to your previous application, you can do so, by clicking the link
          in the confirmation email you have received with your earlier
          application."]
        [:p "If you have any problems, please contact the educational
         institution."]]})
