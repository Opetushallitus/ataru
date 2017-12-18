(ns ataru.translations.texts)

(def translation-mapping
  {:application-period                {:fi "Hakuaika"
                                       :sv "Ansökningstid"
                                       :en "Application period"}
   :not-within-application-period     {:fi "hakuaika ei ole käynnissä"
                                       :sv "inte inom ansökningstiden"
                                       :en "application period currently not ongoing"}
   :application-processed-cant-modify {:fi "Tämä hakemus on käsitelty eikä ole enää muokattavissa"
                                       :sv "Denna ansökan har behandlats och kan inte längre bearbetas"
                                       :en "This application has been processed and can no longer be modified"}
   :continuous-period                 {:fi "Jatkuva haku"
                                       :sv "kontinuerlig ansökningstid"
                                       :en "Continuous application period"}
   :add-row                           {:fi "Lisää rivi"
                                       :sv "Lägg till rad"
                                       :en "Add row"}
   :remove-row                        {:fi "Poista rivi"
                                       :sv "Ta bort rad"
                                       :en "Remove row"}
   :remove                            {:fi "Poista"
                                       :sv "Ta bort"
                                       :en "Remove"}
   :add-more                          {:fi "Lisää..."
                                       :sv "Lägg till..."
                                       :en "Add more..."}
   :add-more-button                   {:fi "Lisää"
                                       :sv "Lägg till"
                                       :en "Add"}
   :add                               {:fi "Lisää"
                                       :sv "Lägg till"
                                       :en "Add more"}
   :add-attachment                    {:fi "Lisää liite..."
                                       :en "Upload attachment..."
                                       :sv "Ladda upp bilagan..."}
   :feedback-header                   {:fi "Hei, kerro vielä mitä pidit hakulomakkeesta!"
                                       :en "Hi! Care to take a moment to rate our application form?"
                                       :sv "Hej, berätta ännu vad du tyckte om ansökningsblanketten?"}
   :feedback-disclaimer               {:fi "Yhteystietojasi ei käytetä tai yhdistetä palautteen tietoihin."
                                       :en "Your personal information is not sent or associated with the feedback given."
                                       :sv "Dina kontaktuppgifter används inte och kopplas inte heller ihop med responsuppgifterna."}
   :feedback-ratings                  {:fi {1 "Huono"
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
   :feedback-text-placeholder         {:fi "Anna halutessasi kommentteja hakulomakkeesta."
                                       :en "Feel free to also share your comments regarding the application form."
                                       :sv "Om du vill kan du ge kommentarer om ansökningsblanketten."}
   :feedback-send                     {:fi "Lähetä palaute"
                                       :en "Send feedback"
                                       :sv "Skicka respons"}
   :feedback-thanks                   {:fi "Kiitos palautteestasi!"
                                       :en "Thank you for your feedback!"
                                       :sv "Tack för din respons!"}
   :page-title                        {:fi "Opintopolku – hakulomake"
                                       :en "Studyinfo – application form"
                                       :sv "Studieinfo – ansökningsblankett"}
   :application-sending               {:fi "Hakemusta lähetetään"
                                       :sv "Ansökan skickas"
                                       :en "The application is being sent"}
   :application-confirmation          {:fi "Saat vahvistuksen sähköpostiisi"
                                       :sv "Du får en bekräftelse till din e-post"
                                       :en "Confirmation email will be sent to the email address you've provided"}
   :application-sent                  {:fi "Hakemus lähetetty"
                                       :sv "Ansökan har skickats"
                                       :en "The application has been sent"}
   :modifications-saved               {:fi "Muutokset tallennettu"
                                       :sv "Ändringarna har sparats"
                                       :en "The modifications have been saved"}
   :application-hakija-edit-text      {:fi "LÄHETÄ MUUTOKSET"
                                       :sv "SCICKA FÖRÄNDRINGAR"
                                       :en "SEND MODIFICATIONS"}
   :application-virkailija-edit-text  {:fi "TALLENNA MUUTOKSET"
                                       :sv "SPARA FÖRÄNDRINGAR"
                                       :en "SAVE MODIFICATIONS"}
   :hakija-new-text                   {:fi "LÄHETÄ HAKEMUS"
                                       :sv "SKICKA ANSÖKAN"
                                       :en "SEND APPLICATION"}
   :check-answers                     {:fi ["Tarkista " " tietoa"]
                                       :sv ["Kontrollera " " uppgifter"]
                                       :en ["Check " " answers"]}
   :file-size-info                    {:fi "Tiedoston maksimikoko on 10 MB"
                                       :en "Maximum file size is 10 MB"
                                       :sv "Den maximala filstorleken är 10 MB"}
   :application-received-subject      {:fi "Opintopolku - Hakemuksesi on vastaanotettu"
                                       :sv "Opintopolku - Din ansökan har tagits emot"
                                       :en "Opintopolku - Your application has been received"}
   :application-edited-subject        {:fi "Opintopolku - Hakemuksesi on päivitetty"
                                       :sv "Opintopolku - Din ansökan har updaterats"
                                       :en "Opintopolku - Your application has been received"}
   :application-received-text         {:fi "Hakemuksesi on vastaanotettu."
                                       :en "Your application has been received."
                                       :sv "Din ansökan har tagits emot."}
   :application-edited-text           {:fi "Hakemuksesi on päivitetty."
                                       :en "Your application has been updated."
                                       :sv "Din ansökan har uppdaterats."}
   :best-regards                      {:fi "terveisin"
                                       :sv "Med vänliga hälsningar"
                                       :en "Best Regards"}
   :application-can-be-found-here     {:fi "Hakemuksesi löytyy täältä"
                                       :sv "Din ansökan kan hittas här"
                                       :en "You can find your application here"}
   :hello-text                        {:fi "Hei"
                                       :sv "Hej"
                                       :en "Hi"}
   :modify-link-text                  {:fi "Ylläolevan linkin kautta voit katsella ja muokata hakemustasi."
                                       :en "You can view and modify your application using the link above."
                                       :sv "Du kan se och redigera din ansökan via länken ovan."}
   :do-not-share-warning-text         {:fi "Älä jaa linkkiä ulkopuolisille. Jos käytät yhteiskäyttöistä tietokonetta, muista kirjautua ulos sähköpostiohjelmasta."
                                       :en "Do not share the link with others. If you are using a public or shared computer, remember to log out of the email application."
                                       :sv "Dela inte länken vidare till utomstående. Om du använder en offentlig dator, kom ihåg att logga ut från e-postprogrammet."}
   :search-application-options        {:fi "Etsi tämän haun koulutuksia"
                                       :sv "Sök ansökningsmål i denna ansökan"
                                       :en "Search for application options"}
   :add-application-option            {:fi "Lisää hakukohde"
                                       :sv "Lägg till ansökningsmål"
                                       :en "Add application option"}
   :file-upload-failed                {:fi "Tiedostoa ei ladattu, yritä uudelleen"
                                       :en "File failed to upload, try again"
                                       :sv "Fil inte laddat, försök igen"}
   :file-type-forbidden               {:fi "Tiedostoa ei ladattu, yritä uudelleen"
                                       :en "File failed to upload, try again"
                                       :sv "Fil inte laddat, försök igen"}})

(def general-texts
  {:yes      {:en "Yes"
              :fi "Kyllä"
              :sv "Ja"}
   :no       {:en "No"
              :fi "Ei"
              :sv "Nej"}
   :have-not {:en "No"
              :fi "En"
              :sv "Nej"}})

(def base-education-module-texts
  {:title                            {:fi "Koulutustausta"
                                      :sv "Utbildningsbakgrund"
                                      :en "Eligibility"}
   :completed-education              {:en "Fill in the education that you have completed  or will complete during the application term."
                                      :fi "Merkitse suorittamasi pohjakoulutukset, myös ne jotka suoritat hakukautena. "
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
   :higher-education-institution     {:en "Higher education institution"
                                      :fi "Korkeakoulu"
                                      :sv "Högskola"}
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
   :year-of-completion               {:en "Year of completion"
                                      :fi "Suoritusvuosi"
                                      :sv "Avlagd år"}
   :describe-eligibility             {:en "Describe eligibility"
                                      :fi "Kelpoisuuden kuvaus"
                                      :sv "Beskrivning av behörigheten"}
   :have-you-completed               {:en "Have you completed general upper secondary education or vocational qualification?",
                                      :fi "Oletko suorittanut lukion/ylioppilastutkinnon tai ammatillisen tutkinnon?",
                                      :sv "Har du avlagt gymnasiet/studentexamen eller yrkesinriktad examen?"}
   :choose-country                   {:en "Choose the country where you have completed your most recent qualification. If you have not yet completed a general upper secondary school syllabus/matriculation examination or vocational qualification, but are in the process of doing so, please choose the country where you will complete the qualification. NB: a vocational qualification can be a vocational upper secondary qualification, school-level qualification, post-secondary level qualification, higher vocational level qualification, further vocational qualification or specialist vocational qualification. Do not fill in the country where you have completed a higher education qualification.",
                                      :fi "Merkitse viimeisimmän tutkintosi suoritusmaa. Jos sinulla ei ole vielä lukion päättötodistusta/ylioppilastutkintoa tai ammatillista tutkintoa mutta olet suorittamassa sellaista, valitse se maa, jossa parhaillaan suoritat kyseistä tutkintoa. Huom: ammatillinen tutkinto voi olla ammatillinen perustutkinto, kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto, ammatti-tai erikoisammattitutkinto. Älä merkitse tähän korkeakoulututkinnon suoritusmaata.",
                                      :sv "Ange land där din senaste examen avlagts. Om du ännu inte har avlagt gymnasiet/studentexamen eller yrkesinriktad examen men håller på att göra det, välj då det land där du som bäst avlägger examen i fråga. Obs: yrkesinriktad examen kan vara yrkesinriktad grundexamen, examen på skolnivå, examen på institutsnivå, yrkesinriktad examen på högre nivå, yrkesexamen eller specialyrkesexamen. Ange inte land där du avlagt högskoleexamen."}})

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
