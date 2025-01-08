(ns ataru.translations.texts
  (:require [clojure.string :as string]))

(def translation-mapping
  {:lisaa-kieli                                 {:fi "Lisää kieli"
                                                 :sv "Lägg till språk"
                                                 :en "Lisää kieli"}
   :add                                         {:fi "Lisää"
                                                 :sv "Lägg till"
                                                 :en "Add"}
   :add-application-option                      {:fi "Lisää hakukohde"
                                                 :sv "Lägg till ansökningsmål"
                                                 :en "Add study programme"}
   :add-attachment                              {:fi "Lisää liite..."
                                                 :en "Upload attachment..."
                                                 :sv "Ladda upp bilagan..."}
   :add-more                                    {:fi "Lisää..."
                                                 :sv "Lägg till..."
                                                 :en "Add..."}
   :add-more-button                             {:fi "Lisää"
                                                 :sv "Lägg till"
                                                 :en "Add"}
   :add-row                                     {:fi "Lisää rivi"
                                                 :sv "Lägg till rad"
                                                 :en "Add row"}
   :allow-publishing-of-results-online          {:fi "Jos tulen hyväksytyksi, oppilaitos voi julkaista nimeni omilla verkkosivuillaan."
                                                 :sv "Om jag blir antagen, får läroanstalen publicera mitt namn på sin webbplats."
                                                 :en "Education institution may publish my admission results on their web page."}
   :allow-use-of-contact-information            {:fi "Annan suostumuksen yhteystietojeni luovuttamiseen koulutusta koskevaa suoramarkkinointia varten"
                                                 :sv "Mina kontaktuppgifter får överlåtas för direkt marknadsföring angående utbildning"
                                                 :en "My contact information can be given to third parties for the purpose of direct education marketing"}
   :allow-use-of-contact-information-info       {:fi "Antamalla suostumuksesi suoramarkkinointiin voit saada eri koulutuksen järjestäjiltä mainoksia koulutuksista."
                                                 :sv "Genom att godkänna direktmarknadsföring kan olika utbildningsanordnare skicka dig reklam om utbildningar."
                                                 :en "By granting permission to direct educational marketing you can receive advertisements from education institutions."}
   :application-can-be-found-here               {:fi "Hakemuksesi löytyy täältä"
                                                 :sv "Din ansökan kan hittas här"
                                                 :en "You can find your application here"}
   :application-confirmation-kk-payment-info-1  {:fi "Jos sinun tulee maksaa hakemusmaksu, saat viimeistään vuorokauden sisällä erillisen sähköpostin, joka sisältää maksulinkin hakemusmaksuun. Tarkistathan myös roskapostikansiosi."
                                                 :sv "Om du måste betala ansökningsavgift får du senast inom ett dygn ett skilt meddelande med en länk genom vilken du kan betala ansökningsavgiften. Kontrollera även din skräppostmapp."
                                                 :en "If you need to pay an application fee, you will receive another email within the next 24 hours. This email has a payment link for the application fee. Please also check your spam folder."}
   :application-confirmation-kk-payment-info-2  {:fi "Jos olet jo maksanut hakemusmaksun toisen haun yhteydessä, sinun ei tarvitse maksaa hakemusmaksua uudelleen. Et saa tällöin uutta maksulinkkiä."
                                                 :sv "Om du redan ha betalat ansökningsavgiften i samband med en tidigare ansökan behöver du inte betala ansökningsavgiften på nytt. I sådant fall skickas inte en ny länk till betalningen."
                                                 :en "If you have already paid the application fee after sending in another application, you do not need to pay the application fee again. In this case, you will not be sent a new payment link."}
   :application-confirmation                    {:fi "Saat vahvistuksen sähköpostiisi"
                                                 :sv "Du får en bekräftelse till din e-post"
                                                 :en "Confirmation email will be sent to the email address you've provided"}
   :application-confirmation-demo               {:fi "Tietojasi ei tallennettu"
                                                 :sv "Dina uppgifter har inte sparats"
                                                 :en "Your information has not been saved"}
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
   :application-period-less-than-day-left       {:fi "Hakuaikaa jäljellä alle vuorokausi"
                                                 :sv "Av ansökningstiden återstår mindre än ett dygn"
                                                 :en "Less than a day left in the application period"}
   :application-period-less-than-hour-left      {:fi "Hakuaikaa jäljellä alle tunti"
                                                 :sv "Av ansökningstiden återstår mindre än en timme"
                                                 :en "Less than one hour left in the application period"}
   :application-period-less-than-45-min-left    {:fi "Hakuaikaa jäljellä alle 45 min"
                                                 :sv "Av ansökningstiden återstår mindre än 45 min."
                                                 :en "Less than 45 min left in the application period"}
   :application-period-less-than-30-min-left    {:fi "Hakuaikaa jäljellä alle 30 min"
                                                 :sv "Av ansökningstiden återstår mindre än 30 min."
                                                 :en "Less than 30 min left in the application period"}
   :application-period-less-than-15-min-left    {:fi "Hakuaikaa jäljellä alle 15 min"
                                                 :sv "Av ansökningstiden återstår mindre än 15 min."
                                                 :en "Less than 15 min left in the application period"}
   :application-period-expired                  {:fi "Hakuaika on päättynyt"
                                                 :sv "Ansökningstiden har utgått"
                                                 :en "Application period has ended"}
   :application-priorization-invalid            {:fi "Hakukohteet ovat väärässä ensisijaisuusjärjestyksessä"
                                                 :sv "Fel prioritetsordning för ansökningsmålen"
                                                 :en "Study programmes are in an invalid order of preference"}
   :application-selected-study-programms        {:fi "Valitut hakukohteet "
                                                 :sv "Valda ansökningsmål"
                                                 :en "Selected study programmes"}
   :application-study-program-added             {:fi "Hakukohde lisätty: "
                                                 :sv "Ansökningsmål har lagts till: "
                                                 :en "Study programme added: "}
   :application-study-program-removed           {:fi "Hakukohde poistettu"
                                                 :sv "Ansökningsmål har tagits bort"
                                                 :en "Study programme removed"}
   :application-limit-reached-in-hakukohderyhma {:fi "Et voi hakea tähän hakukohteeseen, koska olet jo hakemassa seuraaviin hakukohteisiin:"
                                                 :sv "Du kan inte söka till detta ansökningsmål, eftersom du redan söker till följande ansökningsmål:"
                                                 :en "You can't apply to this study programme because you are applying to the following study programme:"}
   :application-processed-cant-modify           {:fi "Tämä hakemus on käsitelty eikä ole enää muokattavissa"
                                                 :sv "Denna ansökan har behandlats och kan inte längre bearbetas"
                                                 :en "This application has been processed and can no longer be modified"}
   :form-closed                                 {:fi "Hakulomake ei ole enää käytössä"
                                                 :sv "Ansökningsblanketten är inte längre i bruk"
                                                 :en "Application form is not in use"}
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
   :application-sent-demo                       {:fi "Harjoittelulomake täytetty"
                                                 :sv "Övningsblanketten är ifylld"
                                                 :en "The practice form is completed"}
   :application-submitted                       {:fi "Hakemuksesi on vastaanotettu!"
                                                 :sv "Din ansökan har tagits emot!"
                                                 :en "Your application has been received!"}
   :application-submitted-demo                  {:fi "Tämä on harjoittelulomake. Tietojasi ei tallennettu!"
                                                 :sv "Det här är en övningsblankett. Dina uppgifter sparades inte!"
                                                 :en "This is a practice form. Your information has not been saved."}
   :application-submitted-ok                    {:fi "OK"
                                                 :sv "OK"
                                                 :en "OK"}
   :application-virkailija-edit-text            {:fi "TALLENNA MUUTOKSET"
                                                 :sv "SPARA FÖRÄNDRINGARNA"
                                                 :en "SAVE MODIFICATIONS"}
   :applications_at_most                        {:fi "Tässä haussa voit hakea %s hakukohteeseen"
                                                 :sv "I denna ansökan kan du söka till %s ansökningsmål"
                                                 :en "In this application you can apply to %s study programmes "}
   :arvosana                                    {:fi "Arvosana"
                                                 :sv "Vitsord"
                                                 :en "Arvosana"}
   :best-regards                                {:fi "Ystävällisin terveisin"
                                                 :sv "Med vänliga hälsningar"
                                                 :en "Best Regards"}
   :should-be-higher-priorization-than          {:fi ["Hakukohde " " tulee olla korkeammalla prioriteetillä kuin "]
                                                 :sv ["Ansökningsmålet " " bör ha en högre prioritet än "]
                                                 :en ["Study programme " " has to be in higher order of preference than "]}
   :check-answers                               {:fi ["Tarkista " " tietoa"]
                                                 :sv ["Kontrollera " " uppgifter"]
                                                 :en ["Check " " answers"]}
   :clear                                       {:fi "Tyhjennä"
                                                 :sv "Töm"
                                                 :en "Clear"}
   :close                                       {:fi "Sulje"
                                                 :sv "Stäng"
                                                 :en "Close"}
   :contact-language                            {:fi "Asiointikieli"
                                                 :sv "Ärendespråk"
                                                 :en "Contact language"}
   :contact-language-info                       {:fi "Valitse kieli, jolla haluat vastaanottaa opiskelijavalintaan liittyviä tietoja. Toiveesi otetaan huomioon mahdollisuuksien mukaan."
                                                 :sv "Välj det språk på vilket du vill få information om studerandeantagningen. Ditt önskemål tas i beaktande om möjligt."
                                                 :en "Choose the language in which you wish to receive information regarding the student selection. Your choice will be taken into consideration if possible."}
   :continuous-period                           {:fi "Jatkuva haku"
                                                 :sv "Kontinuerlig ansökan"
                                                 :en "Continuous admissions"}
   :rolling-period                              {:fi "Joustava haku"
                                                 :sv "Flexibel ansökan"
                                                 :en "Rolling admissions"}
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
   :expired-secret-paragraph                    {:fi "Turvallisuussyistä hakemuslinkki on voimassa yhden muokkauskerran tai enintään %d päivää."
                                                 :en "For security reasons the link is valid for one application update or a maximum of %d days."
                                                 :sv "Av säkerhetsskäl är ansökningslänken i kraft under en session eller i högst %d dagar."}
   :expired-secret-sent                         {:fi "Uusi linkki lähetetty!"
                                                 :en "The new link has been sent!"
                                                 :sv "Den nya länken har skickats!"}
   :email-info-text                             {:fi "Varmista, että antamasi sähköpostiosoite on kirjoitettu oikein ja se on henkilökohtainen. Lähetämme sinulle tärkeitä viestejä tähän sähköpostiosoitteeseen."
                                                 :en "Please ensure that the given email address is typed correctly and is your personal address. Important messages will be sent to this email address."
                                                 :sv "Försäkra dig om att du skrivit din e-postadress rätt och att det är din personliga e-post. Vi skickar viktiga meddelanden till denna e-postadress."}
   :feedback-disclaimer                         {:fi "Yhteystietojasi ei käytetä tai yhdistetä palautteen tietoihin."
                                                 :en "Your personal information is not sent or associated with the feedback given."
                                                 :sv "Dina kontaktuppgifter används inte och kopplas inte heller ihop med responsuppgifterna."}
   :feedback-header                             {:fi "Kerro vielä mitä pidit hakulomakkeesta"
                                                 :en "Care to take a moment to rate our application form?"
                                                 :sv "Berätta ännu vad du tyckte om ansökningsblanketten"}
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
   :file-size-info                              {:fi "Tiedoston maksimikoko on %s"
                                                 :en "Maximum file size is %s"
                                                 :sv "Den maximala filstorleken är %s"}
   :file-size-info-min                          {:fi "Tiedoston koko on 0"
                                                 :en "The file size is 0"
                                                 :sv "Filstorleken är 0"}
   :filter-by-koulutustyyppi                    {:fi "Rajaa koulutustyypillä"
                                                 :en "Filter by education type"
                                                 :sv "Avgränsa enligt utbildningstyp"}
   :uploading                                   {:fi "Ladataan"
                                                 :sv "Laddar upp"
                                                 :en "Uploading"}
   :processing-file                             {:fi "Käsitellään"
                                                 :sv "Bearbetar"
                                                 :en "Processing"}
   :deadline-in                                 {:fi "Palautettava viimeistään"
                                                 :sv "Sista leveransdatum"
                                                 :en "Deadline in"}
   :file-type-forbidden                         {:fi "Tiedostomuoto ei ole sallittu"
                                                 :en "File type is invalid"
                                                 :sv "Filformatet är inte tillåtet"}
   :file-upload-failed                          {:fi "Tiedostoa ei ladattu, yritä uudelleen"
                                                 :en "File failed to upload, try again"
                                                 :sv "Filen kunde inte laddas, försök igen"}
   :file-upload-retransmit                      {:fi "Tiedostoa ei ladattu, yritä uudelleen"
                                                 :en "File failed to upload, try again"
                                                 :sv "Filen kunde inte laddas, försök igen"}
   :file-upload-error                           {:fi "Tiedostoa ei ladattu, yritä uudelleen"
                                                 :en "File failed to upload, try again"
                                                 :sv "Filen kunde inte laddas, försök igen"}
   :cancel-remove                               {:fi "Älä poista"
                                                 :sv "Radera inte"
                                                 :en "Don't remove"}
   :confirm-remove                              {:fi "Vahvista poisto"
                                                 :sv "Bekräfta raderingen"
                                                 :en "Confirm removal"}
   :cancel-upload                               {:fi "Keskeytä"
                                                 :sv "Avbryt"
                                                 :en "Cancel"}
   :cancel-cancel-upload                        {:fi "Älä keskeytä"
                                                 :sv "Avbryt inte"
                                                 :en "Don't cancel"}
   :confirm-cancel-upload                       {:fi "Vahvista keskeytys"
                                                 :sv "Bekräfta avbrytning"
                                                 :en "Confirm cancel"}
   :decrease-priority                           {:fi "Siirrä hakukohteen mieluisuusjärjestystä taaksepäin"
                                                 :en "Move the ranking order of the study option backward"
                                                 :sv "Flytta prioritetsordningen av ansökningsmålet bakåt"}
   :finnish                                     {:fi "Suomi"
                                                 :sv "Finska"
                                                 :en "Finnish"}
   :guardian-contact-information                {:fi "Huoltajan yhteystiedot"
                                                 :sv "Vårdnadshavarens kontaktuppgifter"
                                                 :en "Guardian's contact information"}
   :guardian-contact-minor-secondary            {:fi "Toisen huoltajan tiedot"
                                                 :sv "Den andara vårdnadshavarens kontaktuppgifter"
                                                 :en "The other guardian's contact information"}
   :guardian-email                              {:fi "Huoltajan sähköpostiosoite"
                                                 :sv "Vårdnadshavarens e-postadress"
                                                 :en "Guardian's e-mail address"}
   :guardian-firstname                          {:fi "Huoltajan etunimi"
                                                 :sv "Vårdnadshavarens förnamn"
                                                 :en "Guardian's first/given name"}
   :guardian-lastname                           {:fi "Huoltajan sukunimi"
                                                 :sv "Vårdnadshavarens efternamn"
                                                 :en "Guardian's surname/family name"}
   :guardian-phone                              {:fi "Huoltajan matkapuhelinnumero"
                                                 :sv "Vårdnadshavarens mobiltelefonnummer"
                                                 :en "Guardian's mobile phone number"}
   :hakija-new-text                             {:fi "Lähetä hakemus"
                                                 :sv "Skicka ansökan"
                                                 :en "Submit application"}
   :submit-demo                                 {:fi "Lähetä harjoitteluhakemus"
                                                 :sv "Skicka övningsansökan"
                                                 :en "Send practice application"}
   :hello-text                                  {:fi "Hei"
                                                 :sv "Hej"
                                                 :en "Hi"}
   :hour                                        {:fi "tunti"
                                                 :en "hour"
                                                 :sv "timme"}
   :hours                                       {:fi "tuntia"
                                                 :en "hours"
                                                 :sv "timmar"}
   :increase-priority                           {:fi "Siirrä hakukohteen mieluisuusjärjestystä eteenpäin"
                                                 :en "Move the ranking order of the study option forward"
                                                 :sv "Flytta prioritetsordningen av ansökningsmålet framåt"}
   :insufficient-base-education                 {:fi "Ilmoitus riittämättömästä pohjakoulutuksesta"
                                                 :sv "Meddelande om otillräcklig grundutbildning"
                                                 :en "Insufficient educational background"}
   :liitepyynto-for-hakukohde                   {:fi "Liitepyyntö kuuluu hakukohteisiin:"
                                                 :en "Request for attachment is for study programmes:"
                                                 :sv "Begäran om bilagor berör ansökningsmål:"}
   :lupatiedot-kk-info                          {:fi "Tarkista hakulomakkeelle täyttämäsi tiedot huolellisesti ennen hakulomakkeen lähettämistä. \n\nHakemuksella antamiasi tietoja käytetään opiskelijavalintaan. Näiden tietojen lisäksi opiskelijavalinnassa käytetään perusopetuksen, lukiokoulutuksen ja ammatillisen koulutuksen valtakunnalliseen KOSKI-tietovarantoon tallennettuja tietoja, Ylioppilastutkintolautakunnasta ja korkeakouluista saatavia tutkinto- ja arvosanatietoja sekä väestötietojärjestelmästä saatavia henkilötietoja.  Henkilötietojesi käsittely perustuu lakiin valtakunnallisista opinto- ja tutkintorekistereistä (884/2017).\n\nOpiskelijavalinnan jälkeen tietosi siirtyvät korkeakoululle, josta sait opiskelupaikan. Tietojasi voidaan lakiin perustuen luovuttaa myös muille viranomaisille sekä tutkimustarkoitukseen.\n\nTiedot säilytetään lain mukaan viisi vuotta, jonka jälkeen tiedot siirretään Kansallisarkiston päätöksen mukaan pysyvään säilytykseen. Opiskelupaikan vastaanottamistiedot säilytetään lain mukaan pysyvästi.\n\nSinulla on oikeus tarkastaa tietosi sekä vaatia tietojen oikaisemista tai käsittelyn rajoittamista. Sinulla on myös oikeus tehdä valitus tietosuojavaltuutetulle.\n\nLisätietoja: [Opintopolun tietosuojaseloste](https://opintopolku.fi/konfo/fi/sivu/opiskelijavalintarekisterin-tietosuojaseloste).\n\nSaat vahvistusviestin vastaanotetusta hakulomakkeesta sähköpostiisi."
                                                 :sv "Kontrollera noggrant de uppgifter som du har angett i ansökningsblanketten innan du skickar ansökningsblanketten. \n\nDe uppgifter som du har gett i din ansökan används för att genomföra antagningen av studerande. Utöver dessa uppgifter används uppgifter som sparats i informationsresursen Koski, examens- och vitsordsuppgifter från Studentexamensnämnden och högskolorna samt personuppgifter från Befolkningsdatasystemet. Behandlingen av dina uppgifter bygger på lagen om nationella studie- och examensregister (884/2017).\n\nEfter att antagningen av studerande har gjorts, överförs dina uppgifter till den högskolan där du fick en studieplats. Dina uppgifter kan enligt lag också ges till andra myndigheter eller för forskning.\n\nUppgifterna sparas enligt lag i fem år, varefter de enligt Riksarkivets beslut bevaras permanent. Uppgifterna om mottagande av studieplats bevaras också permanent.\n\nDu har rätt att granska dina egna uppgifter och be att uppgifterna ändras eller att behandlingen av dem begränsas. Du har dessutom rätt att begära ändring hos dataombudsmannen. \n\nMer information: [Studieinfors dataskyddsbeskrivning] (https://opintopolku.fi/konfo/sv/sivu/dataskyddsbeskrivning-foer-antagningsregistret).\n\nDu får i din e-post ett bekräftelsemeddelande över att ansökningsblanketten har kommit fram."
                                                 :en "Please check all the information you have given in the application before you submit the application. \n\nThe information given on the application will be used for student admission and selection purposes. In addition to this information, data stored on the National Data Register for Basic Education, General Upper Secondary Education and Vocational Education, data received from the Finnish Matriculation Examination Board and data regarding degrees and grades received from higher education institutes as well as personal data received from the Finnish Population Information System will be used in student admission and selection. The processing of personal data is based on the Act on the National Registers of Education Records, Qualifications and Degrees (884/2017).\n\nAfter the student admission and selection process your data is transferred to the higher education institute to which you have received the right to study. Your data can be transferred to other officials as well as for research purposes.\nData in the data registry are kept for five years, starting from the decision of student admission. After this, the data will be transferred to permanent storage, according to the decision made by the National Archives of Finland. Data concerning receiving the right to study are kept forever.\n\nYou have the right to demand rectification of false or inaccurate data as well as the right to demand the limitation of processing the data. Additionally, you have the right to file a complaint to a data protection supervisor.\n\nAdditional information: [Studyinfo's register description](https://opintopolku.fi/konfo/en/sivu/register-description-and-cookies#register-description-for-student-admission-register).\n\nYou will receive a confirmation of your application to your email."}
   :harkinnanvaraisuus-topic                    {:fi "Harkintaan perustuva valinta"
                                                 :sv "Antagning enligt prövning"
                                                 :en ""}
   :harkinnanvaraisuus-info                     {:fi "Harkintaan perustuvassa valinnassa hakija voidaan valita koulutukseen valintapisteistä riippumatta erityisen syyn perusteella. \n\nErityisen syyn tulee olla sellainen, joka on saattanut vaikuttaa koulumenestykseen. \n\nErityisiä syitä ovat oppimisvaikeudet, sosiaaliset syyt, koulutodistusten vertailuvaikeus tai riittämätön tutkintokielen kielitaito."
                                                 :sv "I antagning enligt prövning kan sökanden oberoende av antagningspoäng bli vald till utbildningen om det finns något särskilt skäl. \n\nDet särskilda skälet måste vara ett sådant som har påverkat skolframgången. \n\nSärskilda skäl är inlärningssvårigheter, sociala skäl, svårigheter att jämföra betygen eller otillräckliga kunskaper i examensspråket."
                                                 :en ""}
   :harkinnanvaraisuus-question                 {:fi "Haetko harkintaan perustuvassa valinnassa?"
                                                 :sv "Söker du via antagning enligt prövning?"
                                                 :en ""}
   :harkinnanvaraisuus-reason                   {:fi "Peruste harkinnanvaraisuudelle"
                                                 :sv "Grund för antagning enligt prövning"
                                                 :en ""}
   :harkinnanvaraisuus-reason-0                 {:fi "Oppimisvaikeudet"
                                                 :sv "Inlärningssvårigheter"
                                                 :en ""}
   :harkinnanvaraisuus-reason-1                 {:fi "Sosiaaliset syyt"
                                                 :sv "Sociala skäl "
                                                 :en ""}
   :harkinnanvaraisuus-reason-2                 {:fi "Koulutodistusten vertailuvaikeudet"
                                                 :sv "Svårigheter att jämföra skolbetyg "
                                                 :en ""}
   :harkinnanvaraisuus-reason-3                 {:fi "Riittämätön tutkintokielen taito"
                                                 :sv "Otillräcklig språkkunskap i examensspråket"
                                                 :en ""}
   :minute                                      {:fi "minuutti"
                                                 :en "minute"
                                                 :sv "minut"}
   :minutes                                     {:fi "minuuttia"
                                                 :en "minutes"
                                                 :sv "minuter"}
   :missing-input                               {:fi "Puuttuva tieto"
                                                 :en "Missing information"
                                                 :sv "Uppgift som saknas"}
   :modifications-saved                         {:fi "Muutokset tallennettu"
                                                 :sv "Ändringarna har sparats"
                                                 :en "The modifications have been saved"}
   :modify-link-text                            {:fi "Ylläolevan linkin kautta voit katsella ja muokata hakemustasi."
                                                 :en "You can view and modify your application using the link above."
                                                 :sv "Du kan se och redigera din ansökan via länken ovan."}
   :muokkaa-hakukohteita                        {:fi "Muokkaa hakukohteita"
                                                 :sv "Bearbeta ansökningsmål"
                                                 :en "Modify your study programmes"}
   :no-hakukohde-search-hits                    {:fi "Ei hakutuloksia"
                                                 :en "No search results found"
                                                 :sv "Inga sökresultat"}
   :not-applicable-for-hakukohteet              {:fi "Huomaathan, että pohjakoulutuksesi perusteella et ole hakukelpoinen seuraaviin hakukohteisiin. Tarkista hakukelpoisuusvaatimukset hakukohteen valintaperusteista."
                                                 :sv "Märk, att du på basis av din grundutbildning inte är behörig att söka till detta ansökningsmål. Kontrollera behörighetskraven för ansökan i ansökningsmålets antagningsgrunder."
                                                 :en "Please note that the education you have given does not provide eligibility for these study programmes. Please check the required eligibility from the study programme’s admission criteria."}
   :not-editable-application-period-ended       {:fi "Tämä hakutoive ei ole muokattavissa koska sen hakuaika on päättynyt."
                                                 :sv "Ansökningsmålet kan inte bearbetas eftersom ansökningstiden har utgått."
                                                 :en "You can't modify this study programme because the application period has ended."}
   :not-selectable-application-period-ended     {:fi "Hakuaika ei ole käynnissä"
                                                 :sv "Ingen pågående ansökningstid"
                                                 :en "Application period not ongoing"}
   :not-within-application-period               {:fi "hakuaika ei ole käynnissä"
                                                 :sv "ingen pågående ansökningstid"
                                                 :en "application period currently not ongoing"}
   :oppiaine                                    {:fi "Oppiaine"
                                                 :sv "Läroämne"
                                                 :en "Oppiaine"}
   :valinnaisaine                               {:fi "Valinnaisaine"
                                                 :sv "Valfritt ämne"
                                                 :en "Valinnaisaine"}
   :oppimaara                                   {:fi "Oppimäärä"
                                                 :sv "Lärokurs"
                                                 :en "Oppimäärä"}
   :suomi-aidinkielena                          {:fi "Suomi äidinkielenä"
                                                 :sv "Finska som modersmål"
                                                 :en "Suomi äidinkielenä"}
   :suomi-toisena-kielena                       {:fi "Suomi toisena kielena"
                                                 :sv "Finska som andra språk"
                                                 :en "Suomi toisena kielena"}
   :suomi-viittomakielisille                    {:fi "Suomi viittomakielisille"
                                                 :sv "Finska för teckenspråkiga"
                                                 :en "Suomi viittomakielisille"}
   :suomi-saamenkielisille                      {:fi "Suomi saamenkielisille"
                                                 :sv "Finska för samiskspråkiga"
                                                 :en "Suomi saamenkielisille"}
   :ruotsi-aidinkielena                         {:fi "Ruotsi äidinkielenä"
                                                 :sv "Svenska som modersmål"
                                                 :en "Ruotsi äidinkielenä"}
   :ruotsi-toisena-kielena                      {:fi "Ruotsi toisena kielenä"
                                                 :sv "Svenska som andra språk"
                                                 :en "Ruotsi toisena kielenä"}
   :ruotsi-viittomakielisille                   {:fi "Ruotsi viittomakielisille"
                                                 :sv "Svenska för teckenspråkiga"
                                                 :en "Ruotsi viittomakielisille"}
   :saame-aidinkielena                          {:fi "Saame äidinkielenä"
                                                 :sv "Samiska som modersmål"
                                                 :en "Saame äidinkielenä"}
   :romani-aidinkielena                         {:fi "Romani äidinkielenä"
                                                 :sv "Romani som modersmål"
                                                 :en "Romani äidinkielenä"}
   :viittomakieli-aidinkielena                  {:fi "Viittomakieli äidinkielenä"
                                                 :sv "Teckenspråk som modersmål"
                                                 :en "Viittomakieli äidinkielenä"}
   :muu-oppilaan-aidinkieli                     {:fi "Muu oppilaan äidinkieli"
                                                 :sv "Annat modersmål för eleven"
                                                 :en "Muu oppilaan äidinkieli"}
   :page-title                                  {:fi "Opintopolku – hakulomake"
                                                 :en "Studyinfo – application form"
                                                 :sv "Studieinfo – ansökningsblankett"}
   :permission-for-electronic-transactions      {:fi "Lupa sähköiseen asiointiin"
                                                 :sv "Medgivande till elektronisk kommunikation"
                                                 :en "Consent for electronic communication"}
   :permission-for-electronic-transactions-info {:fi "Täyttämällä sähköisen hakulomakkeen annat samalla luvan siihen, että opiskelijavalintaan liittyvä viestintä voidaan hoitaa pelkästään sähköisesti. Jos et suostu näihin ehtoihin, ota yhteyttä ensisijaisen hakutoiveesi korkeakoulun hakijapalveluihin."
                                                 :sv "Genom att fylla i denna elektroniska ansökningsblankett ger du samtidigt ditt medgivande till att kommunikationen gällande studerandeantagningen kan skötas enbart elektroniskt. Om du inte går med på dessa villkor, kontakta ansökningsservicen vid högskolan."
                                                 :en "By filling in this electronic application form you also give your consent that communication regarding student admissions can be carried out only by email. If you do not agree to these terms, please contact the admissions services of the higher education institution that you are applying to."}
   :permission-for-electronic-transactions-kylla  {:fi "Hyväksyn sähköisen asioinnin ehdot"
                                                   :sv "Jag godkänner villkoren för elektronisk ärendehantering"
                                                   :en "I agree on the terms of online/electronic service"}
   :paatos-opiskelijavalinnasta-sahkopostiin    {:fi "Oppilaitos saa toimittaa päätöksen opiskelijavalinnasta sähköpostiini"
                                                 :sv "Läroanstalten får skicka beslutet för studerandeantagningen till min e-post"
                                                 :en "EN: Oppilaitos saa toimittaa päätöksen opiskelijavalinnasta sähköpostiini"}
   :lupatiedot-toinen-aste                      {:fi "Lupatiedot"
                                                 :sv "Tillståndsuppgifter"
                                                 :en "Permissions"}
   :lupatiedot-toinen-aste-info                 {:fi "**Tarkista hakulomakkeelle täyttämäsi tiedot huolellisesti ennen hakulomakkeen lähettämistä. **\n\nHakemuksella antamiasi tietoja käytetään opiskelijavalintaan. Näiden tietojen lisäksi opiskelijavalinnassa käytetään perusopetuksen ja ammatillisen koulutuksen valtakunnalliseen KOSKI-tietovarantoon tallennettuja tietoja sekä väestötietojärjestelmästä saatavia henkilötietoja.  \n\nHenkilötietojesi käsittely perustuu lakiin valtakunnallisista opinto- ja tutkintorekistereistä (884/2017).\n\nOpiskelijavalinnan jälkeen tietosi siirtyvät oppilaitokseen, josta sait opiskelupaikan. Tietojasi voidaan lakiin perustuen luovuttaa myös muille viranomaisille sekä tutkimustarkoitukseen.\n\nTiedot säilytetään lain mukaan viisi vuotta, jonka jälkeen tiedot siirretään Kansallisarkiston päätöksen mukaan pysyvään säilytykseen. Opiskelupaikan vastaanottamistiedot säilytetään lain mukaan pysyvästi.\n\nSinulla on oikeus tarkastaa tietosi sekä vaatia tietojen oikaisemista tai käsittelyn rajoittamista. Sinulla on myös oikeus tehdä valitus tietosuojavaltuutetulle.\n\nLisätietoja: [Opintopolun tietosuojaseloste](https://opintopolku.fi/konfo/fi/sivu/opiskelijavalintarekisterin-tietosuojaseloste).\n\n\n\n"
                                                 :sv "**Kontrollera noggrant de uppgifter som du har angett i ansökningsblanketten innan du skickar ansökningsblanketten.**\n\nDe uppgifter som du har gett i din ansökan används för att genomföra antagningen av studerande. Utöver dessa uppgifter används uppgifter som sparats i informationsresursen Koski samt personuppgifter från Befolkningsdatasystemet. \n\nBehandlingen av dina uppgifter bygger på lagen om nationella studie- och examensregister (884/2017).\n\nEfter att antagningen av studerande har gjorts, överförs dina uppgifter till den högskolan där du fick en studieplats. Dina uppgifter kan enligt lag också ges till andra myndigheter eller för forskning.\n\nUppgifterna sparas enligt lag i fem år, varefter de enligt Riksarkivets beslut bevaras permanent. Uppgifterna om mottagande av studieplats bevaras också permanent.\n\nDu har rätt att granska dina egna uppgifter och be att uppgifterna ändras eller att behandlingen av dem begränsas. Du har dessutom rätt att begära ändring hos dataombudsmannen. \n\nMer information: [Studieinfos dataskyddsbeskrivning](https://opintopolku.fi/konfo/sv/sivu/dataskyddsbeskrivning-foer-antagningsregistret).\n"
                                                 :en "EN: **Tarkista hakulomakkeelle täyttämäsi tiedot huolellisesti ennen hakulomakkeen lähettämistä. **\n\nHakemuksella antamiasi tietoja käytetään opiskelijavalintaan. Näiden tietojen lisäksi opiskelijavalinnassa käytetään perusopetuksen ja ammatillisen koulutuksen valtakunnalliseen KOSKI-tietovarantoon tallennettuja tietoja sekä väestötietojärjestelmästä saatavia henkilötietoja.  \n\nHenkilötietojesi käsittely perustuu lakiin valtakunnallisista opinto- ja tutkintorekistereistä (884/2017).\n\nOpiskelijavalinnan jälkeen tietosi siirtyvät oppilaitokseen, josta sait opiskelupaikan. Tietojasi voidaan lakiin perustuen luovuttaa myös muille viranomaisille sekä tutkimustarkoitukseen.\n\nTiedot säilytetään lain mukaan viisi vuotta, jonka jälkeen tiedot siirretään Kansallisarkiston päätöksen mukaan pysyvään säilytykseen. Opiskelupaikan vastaanottamistiedot säilytetään lain mukaan pysyvästi.\n\nSinulla on oikeus tarkastaa tietosi sekä vaatia tietojen oikaisemista tai käsittelyn rajoittamista. Sinulla on myös oikeus tehdä valitus tietosuojavaltuutetulle.\n\nLisätietoja: [Opintopolun tietosuojaseloste](https://opintopolku.fi/konfo/fi/sivu/opiskelijavalintarekisterin-tietosuojaseloste).\n\n\n\n"}
   :lupatiedot-kk                               {:fi "Lupatiedot"
                                                 :sv "Tilläggsuppgifter"
                                                 :en "Permissions"}
   :pohjakoulutusvaatimus                       {:fi "Pohjakoulutusvaatimus"
                                                 :sv "Grundutbildningskrav"
                                                 :en "Educational background restrictions"}
   :preview                                     {:fi "Esikatselu"
                                                 :en "Preview"
                                                 :sv "Förhandsvisa"}
   :question-for-hakukohde                      {:fi "Kysymys kuuluu hakukohteisiin:"
                                                 :en "This question is for study programmes:"
                                                 :sv "Frågan berör ansökningsmål:"}
   :info-for-hakukohde                          {:fi "Osio kuuluu hakukohteisiin:"
                                                 :en "This information is shown for study programmes:"
                                                 :sv "Frågan berör ansökningsmål:"}
   :show-application-options                    {:fi "Näytä hakukohteet"
                                                 :en "Show study programmes"
                                                 :sv "Visa ansökningsmål"}
   :hide-application-options                    {:fi "Piilota hakukohteet"
                                                 :en "Hide study programmes"
                                                 :sv "Dölj ansökningsmål"}
   :limit-reached                               {:fi "ei valittavissa"
                                                 :en "unselectable"
                                                 :sv "icke-valbar"}
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
   :remove-question-group-answer                {:fi "Poista vastausvaihtoehto"
                                                 :sv "Radera svarsalternativ"
                                                 :en "Remove answer option"}
   :search-application-options                  {:fi "Etsi tämän haun koulutuksia"
                                                 :sv "Hämta ansökningsmål i denna ansökan"
                                                 :en "Search for study programmes"}
   :search-application-options-or-education     {:fi "Etsi oppilaitosta tai koulutusta"
                                                 :sv "Sök läroanstalt eller utbildningar"
                                                 :en "EN: Etsi oppilaitosta tai koulutusta"}
   :close-application-options                   {:fi "Sulje koulutusten haku"
                                                 :sv "Dölj hämta ansökningsmål"
                                                 :en "Close study program search"}
   :second                                      {:fi "sekunti"
                                                 :en "second"
                                                 :sv "sekund"}
   :seconds                                     {:fi "sekuntia"
                                                 :en "seconds"
                                                 :sv "sekunder"}
   :select-max-n-application-options            {:fi ["Voit valita enintään " " hakukohdetta."]
                                                 :sv ["Du kan välja högst " " ansökningsmål."]
                                                 :en ["EN: Valitse enintään " " ansökningsmål"]}
   :select-still-1-application-option           {:fi "Voit valita vielä yhden hakukohteen"
                                                 :sv "Du kan välja ännu ett ansökningsmål"
                                                 :en "EN: Voit valita vielä yhden hakukohteen"}
   :select-still-n-application-options          {:fi ["Voit valita vielä " " hakukohdetta"]
                                                 :sv ["Du kan välja ännu " " ansökningsmål"]
                                                 :en ["EN: Voit valita vielä " " hakukohdetta"]}
   :show-more                                   {:fi "Näytä lisää..."
                                                 :en "Show more..."
                                                 :sv "Visa mer..."}
   :swedish                                     {:fi "Ruotsi"
                                                 :sv "Svenska"
                                                 :en "Swedish"}
   :verify-email                                {:fi "Kirjoita sähköpostiosoitteesi uudelleen"
                                                 :sv "Ange din e-postadress igen"
                                                 :en "Please write your e-mail address again"}
   :window-close-warning                        {:fi "Varmistathan että hakemus on lähetetty ennen sivun sulkemista."
                                                 :en "Please ensure you have submitted the form before closing the page."
                                                 :sv "Försäkra dig om att du har skickat din ansökan innan du stänger sidan"}
   :edit-answers                                {:fi "Muokkaus"
                                                 :sv "Bearbeta"
                                                 :en "Edit"}
   :hyvaksytty                                  {:fi "Hyväksytty"
                                                 :sv "Godkänd"
                                                 :en "Selected"}
   :hyvaksytty-s                                {:fi "S (Hyväksytty)"
                                                 :sv "S (Godkänd)"
                                                 :en "S (Selected)"}
   :osallistunut-o                              {:fi "O (Osallistunut)"
                                                 :sv "O (Deltagit)"
                                                 :en "O (Osallistunut)"}
   :ei-arvosanaa                                {:fi "Ei arvosanaa"
                                                 :sv "Inget vitsord"
                                                 :en "Ei arvosanaa"}
   :preview-answers                             {:fi "Esikatselu"
                                                 :sv "Förhandsvisa"
                                                 :en "Preview"}
   :network-offline                             {:fi "Ei yhteyttä verkkoon."
                                                 :sv "Network is offline."
                                                 :en "Network is offline."}
   :application-period-closed                   {:fi "Hakemusta ei voi lähettää: Hakuaika on päättynyt tai liitteen määräaika on umpeutunut"
                                                 :sv "Ansökan kan inte skickas: Ansökningstiden eller deadline för bilagor har utgått"
                                                 :en "Application cannot be submitted: Application period has ended or deadline for submitting attachments has passed"}
   :inactivated                                 {:fi "Hakemuksesi on peruttu."
                                                 :sv "Din ansökning har annullerats."
                                                 :en "Your application is cancelled."}
   :internal-server-error                       {:fi "Tapahtui palvelinvirhe."
                                                 :sv "Internal server error."
                                                 :en "Internal server error."}
   :oppija-session-not-found                    {:fi "Hakemuksesi tallentaminen ei onnistunut, koska istuntosi ei ole enää voimassa. Voit lähettää uuden hakemuksen kirjautumalla uudelleen tai ilman kirjautumista."
                                                 :sv "Din ansökan kunde inte sparas eftersom din session har utgått. Du kan skicka en ny ansökan genom att logga in på nytt eller utan inloggning."
                                                 :en "Your session has expired and your application has not been saved. You can send a new application by logging in again. You can also send a new application without identification."}
   :application-validation-failed-error         {:fi "Tapahtui palvelinvirhe."
                                                 :sv "Internal server error."
                                                 :en "Internal server error."}
   :secret-expired                              {:fi "Tämä hakemuslinkki on vanhentunut"
                                                 :en "This application link has expired"
                                                 :sv "Denna ansökningslänk är föråldrad"}
   :oppiaine-valinnainen                        {:fi "%s, valinnainen"
                                                 :sv "%s, valinnainen"
                                                 :en "%s, valinnainen"}
   :lisaa-valinnaisaine                         {:fi "Lisää valinnaisaine"
                                                 :sv "Lägg till valfritt ämne"
                                                 :en "Lisää valinnaisaine"}
   :application-submitted-payment               {:fi "Hakemuksesi on tallentunut"
                                                 :sv "Din ansökan har sparats."
                                                 :en "Your application has been saved."}
   :application-submitted-payment-text          {:fi "Olet saanut vahvistusviestin sähköpostiisi. Siirry nyt maksamaan käsittelymaksu. Pääset maksamaan käsittelymaksun myös sähköpostiisi saamasi linkin kautta. Huomaathan, ettet voi muokata hakemusta sen jälkeen, kun olet maksanut käsittelymaksun."
                                                 :sv "Du har fått en bekräftelse till din e-post. Gå nu till betalning av din behandlingsavgift. Du kan också betala behandlingsavgiften via länken du har fått till din e-post. Vänligen notera att du kan inte ändra din ansökan efter att du har betalt behandlingsavgiften."
                                                 :en "You have received a confirmation to your email. Proceed now to the payment of your processing fee. You can also pay the processing fee through the link you have received by email. Please note that you cannot edit your application after you have paid the decision fee."}
   :application-submitted-payment-text-2        {:fi "Emme palauta käsittelymaksua, vaikka peruuttaisit hakemuksesi."
                                                 :sv "Vi returnerar inte den betalda behandlingsavgiften även om du skulle dra tillbaka din ansökan."
                                                 :en "We will not return the processing fee to you even if you cancel your application."}
   :payment-button                              {:fi "Siirry maksamaan"
                                                 :sv "Gå till betalning"
                                                 :en "Go to payment"}
   :poista                                      {:fi "Poista"
                                                 :sv "Radera"
                                                 :en "Poista"}
   :poista-osio                                 {:fi "Poista osio"
                                                 :sv "SV: Poista osio"
                                                 :en ""}
   :demo                                        {:fi "Harjoittelulomake — Tietojasi ei tallenneta!"
                                                 :sv "Övningsblankett – Dina uppgifter sparas inte!"
                                                 :en "Practice form  — Your information will not be saved!"}
   :demo-notification                           {:fi "Tämä on harjoittelulomake. Täyttämiäsi tietoja ei tallenneta."
                                                 :sv "Det här är en övningsblankett. De uppgifter som du fyller i sparas inte."
                                                 :en "This is a practice form. The information that you fill in will not be saved."}
   :demo-closed-notification                    {:fi ["Demolomake ei ole tällä hetkellä käytössä. Tutustu koulutustarjontaan ja tuleviin hakuihin " ":ssä."]
                                                 :sv ["Demoblanketten är inte i användning just nu. Bekanta dig med utbildningsutbudet och kommande ansökningar på " "."]
                                                 :en ["The application demo form is currently not in use. Explore available study programmes and upcoming application periods at " "."]}
   :demo-closed-link                            {:fi "Opintopolku.fi"
                                                 :sv "Studieinfo.fi"
                                                 :en "Studyinfo.fi"}
   :dismiss-demo-notification                   {:fi "Jatka"
                                                 :sv "Fortsätt"
                                                 :en "Continue"}
   :demo-notification-title                     {:fi "Harjoittelulomake"
                                                 :sv "Övningsblankett"
                                                 :en "Practice form"}
   :demo-closed-title                           {:fi "Demolomake suljettu"
                                                 :sv "Demoblanketten stängd"
                                                 :en "Application demo form closed"}
   :toimitusosoite                              {:fi "Lähetä liite osoitteeseen"
                                                 :sv "Skicka bilagan till adressen"
                                                 :en "EN: Lähetä liite osoitteeseen"}
   :verkkosivu                                  {:fi "Tai käytä"
                                                 :sv "Eller använd"
                                                 :en "EN: Tai käytä"}
   :siirretty-ylos                              {:fi "siirretty ylös"
                                                 :sv "flyttad upp"
                                                 :en "moved up"}
   :siirretty-alas                              {:fi "siirretty alas"
                                                 :sv "flyttad ner"
                                                 :en "moved down"}
   :archived                                    {:fi "Arkistoitu"
                                                 :sv "Arkiverad"
                                                 :en "Archived"}
   :required                                    {:fi "(pakollinen tieto)"
                                                 :sv "(obligatorisk uppgift)"
                                                 :en "(mandatory information)"}
   :selected                                    {:fi "Valittu: "
                                                 :sv "Godkänd: "
                                                 :en "Selected: "}
   :email-vain-harkinnanvaraisessa-subject      {:fi "Muutos yhteishaun valintatapaan"
                                                 :sv "Ändring i antagningssättet i den gemensamma ansökan"
                                                 :en "EN: Muutos yhteishaun valintatapaan"}
   :email-vain-harkinnanvaraisessa              {:fi "Olet hakenut yhteishaussa perusopetuksen jälkeiseen koulutukseen. Pohjakoulutustietosi mukaan olet mukana vain harkintaan perustuvassa valinnassa."
                                                 :sv "Du har sökt i den gemensamma ansökan till utbildning efter den grundläggande utbildningen. Enligt uppgifter om din grundutbildning är du endast med i antagning enligt prövning."
                                                 :en "EN: Olet hakenut yhteishaussa perusopetuksen jälkeiseen koulutukseen. Pohjakoulutustietosi mukaan olet mukana vain harkintaan perustuvassa valinnassa."}
   :email-vain-harkinnanvaraisessa-link-text    {:fi "Lue lisää harkintaan perustuvasta valinnasta Opintopolusta."
                                                 :sv "Läs mera om antagning enligt prövning i Studieinfo."
                                                 :en "EN: Lue lisää harkintaan perustuvasta valinnasta Opintopolusta."}
   :email-vain-harkinnanvaraisessa-link         {:fi "https://opintopolku.fi/konfo/fi/sivu/hakeminen-harkinnanvaraisen-valinnan-kautta"
                                                 :sv "https://opintopolku.fi/konfo/sv/sivu/ansoekan-via-antagning-enligt-proevning"
                                                 :en "https://opintopolku.fi/konfo/en/sivu/student-admission-in-the-joint-application-to-upper-secondary-education-and"}
   :email-myos-pistevalinnassa                  {:fi "Olet hakenut yhteishaussa perusopetuksen jälkeiseen koulutukseen. Hakemuksellasi ilmoittamiesi tietojen perusteella sait ilmoituksen, että hakemuksesi käsitellään harkinnanvaraisessa valinnassa. Oppilaitoksesi mukaan tämä tieto on ollut virheellinen, joten hakemuksesi käsitellään normaalissa pistevalinnassa harkinnanvaraisen valinnan sijaan."
                                                 :sv "Du har sökt i den gemensamma ansökan till utbildning efter den grundläggande utbildningen. På grund av uppgifterna du uppgav på ansökningen fick du ett meddelande att din ansökning behandlas i antagning enligt prövning. Eftersom den här uppgiften enligt din läroanstalt har varit felaktig, behandlas din ansökan i den vanliga poängantagningen och inte i antagning enligt prövning."
                                                 :en "EN: Olet hakenut yhteishaussa perusopetuksen jälkeiseen koulutukseen. Hakemuksellasi ilmoittamiesi tietojen perusteella sait ilmoituksen, että hakemuksesi käsitellään harkinnanvaraisessa valinnassa. Oppilaitoksesi mukaan tämä tieto on ollut virheellinen, joten hakemuksesi käsitellään normaalissa pistevalinnassa harkinnanvaraisen valinnan sijaan."}
   :email-kk-payment-link-subject               {:fi "Opintopolku: Hakemusmaksu, maksathan maksun viimeistään"
                                                 :sv "Studieinfo: Ansökningsavgift, betala avgiften senast"
                                                 :en "Studyinfo: application fee, reminder to pay the fee by"}
   :email-kk-payment-reminder-subject           {:fi "Opintopolku: Hakemusmaksu, maksathan maksun viimeistään"
                                                 :sv "Ansökningsavgift, påminnelse att betala avgiften senast"
                                                 :en "Studyinfo: Application fee, please remember to pay the fee by"}
   :kk-payment-info-url                         {:fi "https://opintopolku.fi/konfo/fi/sivu/hakemusmaksu"
                                                 :sv "https://opintopolku.fi/konfo/sv/sivu/ansoekningsavgift"
                                                 :en "https://opintopolku.fi/konfo/en/sivu/application-fee"}
   :ht-lander-header                            {:fi "Miten haluat siirtyä hakulomakkeelle?"
                                                 :sv "Hur vill du öppna ansökningsblanketten?"
                                                 :en "How do you want to access the application form?"}
   :ht-lander-top-text                          {:fi "Tässä haussa sinulla on mahdollisuus kirjautua hakulomakkeelle. Valitse sinulle sopiva vaihtoehto."
                                                 :sv "I den här ansökan kan du också logga in i ansökningsblanketten. Välj här det alternativ som passar dig."
                                                 :en "In this application you can log in to the application form. Select your preferred method."}
   :ht-kirjaudu-sisaan                          {:fi "Kirjaudu sisään"
                                                 :sv "Logga in"
                                                 :en "Log in"}
   :ht-ilman-kirjautumista                      {:fi "Jatka ilman kirjautumista"
                                                 :sv "Fortsätt utan att logga in"
                                                 :en "Continue without logging in"}
   :ht-tunnistaudu-ensin-header                 {:fi "Tunnistaudu ja täytä hakulomake"
                                                 :sv "Identifiera dig och fyll i ansökningsblanketten"
                                                 :en "Identify and fill in the application form"}
   :ht-tunnistaudu-ensin-text                   {:fi "Kun tunnistaudut suomi.fi-palvelussa pankkitunnuksilla, mobiilivarmenteella tai sirullisella henkilökortilla, tuodaan henkilötietosi hakulomakkeelle automaattisesti."
                                                 :sv "Då du identifierar dig i suomi.fi-tjänsten med bankkoder, mobilcertifikat eller elektroniskt ID-kort, hämtas dina personuppgifter automatiskt till ansökningsblanketten."
                                                 :en "When you identify yourself on Suomi.fi with your banking credentials, mobile authenticator or with your ID card, your personal information will be prefilled on the application form automatically."}
   :ht-tunnistaudu-ensin-tutkinto-fetch-notice  {:fi "Myös tutkintotietosi tuodaan lomakkeelle, jos tunnistaudut."
                                                 :sv "SV: Myös tutkintotietosi tuodaan lomakkeelle, jos tunnistaudut."
                                                 :en ""}
   :ht-tunnistaudu-ensin-post-notice            {:fi "Yhteystietojasi voit kuitenkin muokata hakulomakkeella."
                                                 :sv "Du kan ändra dina kontaktuppgifter i ansökningsblanketten."
                                                 :en "You can modify your contact information on the application form."}
   :ht-tunnistaudu-ensin-text-2                 {:fi "Sinulla on tunnistautumisen jälkeen neljä tuntia aikaa täyttää ja lähettää hakulomake, minkä jälkeen istuntosi aikakatkaistaan."
                                                 :sv "Efter att du har identifierat dig har du fyra timmar på dig att fylla i och skicka din ansökningsblankett. Efter detta avbryts din session."
                                                 :en "Once you have completed identification, you have four hours to fill in and send the application form. The session will expire after four hours."}
   :ht-jatka-tunnistautumatta-header            {:fi "Täytä hakulomake ilman tunnistautumista"
                                                 :sv "Fyll i ansökningsblanketten utan identifiering"
                                                 :en "Fill in the application without identification"}
   :ht-jatka-tunnistautumatta-text              {:fi "Valitsemalla tämän vaihtoehdon täytät hakulomakkeelle itse kaikki vaaditut tiedot."
                                                 :sv "Om du väljer detta alternativ bör du själv fylla i samtliga uppgifter i ansökningsblanketten."
                                                 :en "By choosing this alternative you fill in all the necessary information on the application form yourself."}
   :ht-tai                                      {:fi "TAI"
                                                 :sv "ELLER"
                                                 :en "OR"}
   :ht-logout-confirmation-header               {:fi "Haluatko kirjautua ulos?"
                                                 :sv "Vill du logga ut?"
                                                 :en "Do you want to log out?"}
   :ht-logout-confirmation-text                 {:fi "Et ole vielä lähettänyt hakemustasi, etkä voi tallentaa sitä keskeneräisenä. Jos kirjaudut ulos, täyttämiäsi tietoja ei tallenneta. Et voi tallentaa hakemustasi keskeneräisenä."
                                                 :sv "Du har inte ännu skickat din ansökan och du kan inte heller spara den som halvfärdig. Om du loggar ut, sparas inte de uppgifter som du fyllt i."
                                                 :en "You have not sent your application yet. You cannot save the application as a draft. If you log out, the information you filled in will not be saved."}
   :ht-kirjaudu-ulos                            {:fi "Kirjaudu ulos"
                                                 :sv "Logga ut"
                                                 :en "Log out"}
   :ht-has-applied-lander-header                {:fi "Olet jo lähettänyt hakemuksen hakuun:"
                                                 :sv "Du har redan skickat en ansökan:"
                                                 :en "You have already sent an application in:"}
   :ht-has-applied-lander-paragraph1            {:fi "Tässä haussa voit lähettää vain yhden (1) hakemuksen. Olet jo lähettänyt hakemuksen tähän hakuun ja siksi et voi lähettää toista hakemusta."
                                                 :sv "I den här ansökan kan du skicka bara en (1) ansökan. Du har redan skickat en ansökan och därför kan du inte skicka flera ansökningar."
                                                 :en "You can only send one (1) application form in this application round. You have already sent an application and therefore cannot send another."}
   :ht-has-applied-lander-paragraph2            {:fi "Jos haluat muuttaa hakemustasi, niin kirjaudu Oma Opintopolku-palveluun."
                                                 :sv "Om du vill ändra din ansökan, ska du logga in i tjänsten Min Studieinfo."
                                                 :en "If you want to modify your application, log in to My Studyinfo."}
   :ht-has-applied-lander-paragraph2-eidas      {:fi "Jos haluat muuttaa hakemustasi, niin löydät muokkauslinkin sähköpostiviestistä, jonka sait jättäessäsi edellisen hakemuksen."
                                                 :sv "Om du vill ändra din ansökan, hittar du bearbetningslänken i e-postmeddelandet som du fick när du skickade din tidigare ansökning."
                                                 :en "If you want to make changes to your previous application, you can do so by clicking the link in the confirmation email you have received with your earlier application."}
   :ht-has-applied-lander-paragraph3            {:fi "Ongelmatilanteissa ole yhteydessä hakemaasi oppilaitokseen."
                                                 :sv "Vid problem kan du kontakta den läroanstalt som du har sökt till."
                                                 :en "If you have any issues, please contact the educational institution you are applying to."}
   :ht-siirry-oma-opintopolkuun                 {:fi "Katso hakemustasi Oma Opintopolku-palvelussa"
                                                 :sv "Se din ansökan i tjänsten Min Studieinfo"
                                                 :en "View your application on My Studyinfo"}
   :ht-jatka-palvelun-kayttoa                   {:fi "Jatka palvelun käyttöä"
                                                 :sv "Fortsätt använda tjänsten"
                                                 :en "Continue using the service"}
   :ht-session-expiring-header                  {:fi "Istuntosi on vanhentumassa"
                                                 :sv "Din session håller på att föråldras"
                                                 :en "Your session is about to expire"}
   :ht-session-expiring-text                    {:fi "Sinut kirjataan 15 min päästä ulos palvelusta ja hakemustasi ei tallenneta, jos et lähetä hakemustasi sitä ennen."
                                                 :sv "Du loggas ut ur tjänsten om 15 minuter. Din ansökan sparas inte om du inte har skickat in din ansökan före utloggningen."
                                                 :en "You will be logged out of the service in 15 minutes. If you do not send your application before that, the application will not be saved."}
   :ht-session-expiring-text-variable           {:fi "Sinut kirjataan %d min päästä ulos palvelusta ja hakemustasi ei tallenneta, jos et lähetä hakemustasi sitä ennen."
                                                 :sv "Du loggas ut ur tjänsten om %d minuter. Din ansökan sparas inte om du inte har skickat in din ansökan före utloggningen."
                                                 :en "You will be logged out of the service in %d minutes. If you do not send your application before that, the application will not be saved."}
   :ht-session-expired-header                   {:fi "Istunto on vanhentunut"
                                                 :sv "Sessionen har löpt ut"
                                                 :en "The session has expired"}
   :ht-session-expired-text                     {:fi "Istunto vanhentui ja sinut on kirjattu ulos palvelusta. Hakemustasi ei ole tallennettu."
                                                 :sv "Sessionen löpte ut och du har loggats ut ur tjänsten. Din ansökan sparades inte."
                                                 :en "The session has expired and you have been logged out of the service. Your application has not been saved."}
   :ht-session-expired                          {:fi "Siirry Opintopolun etusivulle"
                                                 :sv "Gå till framsidan av Studieinfo"
                                                 :en "Go to the Studyinfo front page"}
   :ht-application-submitted                    {:fi "Hakemuksesi on vastaanotettu!"
                                                 :sv "Din ansökan har mottagits!"
                                                 :en "Your application has been saved!"}
   :ht-application-confirmation                 {:fi "Saat vahvistuksen sähköpostiisi. Voit katsoa hakemustasi tai kirjautua ulos. Pääset katsomaan ja muokkaamaan hakemustasi myöhemmin Oma Opintopolku-palvelussa."
                                                 :sv "Du får en bekräftelse i din e-post. Du kan granska din ansökan eller logga ut. Du kan senare granska och ändra din ansökan via tjänsten Min Studieinfo."
                                                 :en "You will receive a confirmation to your email. You can view your application or log out. You can view or modify your application later on My Studyinfo."}
   :ht-application-confirmation-eidas           {:fi "Pääset katsomaan ja muokkaamaan hakemustasi myöhemmin vahvistussähköpostista löytyvän muokkauslinkin kautta."
                                                 :sv "Du kan granska och redigera din ansökan senare via bearbetningslänken som finns i bekräftelsemeddelandet."
                                                 :en "You can view and edit your application later via the edit link found in the confirmation email."}
   :ht-katso-hakemustasi                        {:fi "Katso hakemustasi"
                                                 :sv "Granska din ansökan"
                                                 :en "View your application"}
   :ht-person-info-module-top-text              {:fi "Henkilötietosi on tuotu väestötietojärjestelmästä. Yhteystietosi ovat muokattavissa. Väestötietojärjestelmän tietoja voit muuttaa"
                                                 :sv "Dina personuppgifter har hämtats från befolkningsdatasystemet. Du kan ändå ändra dina kontaktuppgifter. De uppgifter som finns i befolkningsdatasystemet kan du ändra via tjänsten"
                                                 :en "Your personal information has been prefilled with data from the population data service. You can modify your contact information. You can modify the data from the population data service on"}
   :ht-person-info-module-top-text-link-url     {:fi "https://www.suomi.fi/omat-tiedot/henkilotiedot"
                                                 :sv "https://www.suomi.fi/mina-uppgifter/personuppgifter"
                                                 :en "https://www.suomi.fi/your-data/personal-data"}
   :ht-person-info-module-top-text-link-text    {:fi "Suomi.fi:ssä."
                                                 :sv "Suomi.fi."
                                                 :en "Suomi.fi."}
   :ht-person-info-module-top-text-eidas        {:fi "Nimitietosi ja syntymäaikasi on tuotu hakulomakkeelle tunnistautumisen kautta."
                                                 :sv "Ditt namn och födelsetid har hämtats till ansökningsblanketten via identifiering."
                                                 :en "Your name and date of birth have been prefilled on the application form via identification."}
   :add-tutkinto                                {:fi "Lisää tutkinto"
                                                 :sv "SV: Lisää tutkinto"
                                                 :en ""}
   :information-request-reminder-subject-prefix {:fi "Muistutus"
                                                 :en "Reminder"
                                                 :sv "Påminnelse"}})

(def oppiaine-translations
  {:oppiaine-a                 {:fi "Äidinkieli ja kirjallisuus"
                                :sv "Modersmål och litteratur"
                                :en "Äidinkieli ja kirjallisuus"}
   :oppiaine-a1                {:fi "A1-kieli"
                                :sv "A1-språk"
                                :en "A1-kieli"}
   :oppiaine-a2                {:fi "A2-kieli"
                                :sv "A2-språk"
                                :en "A2-kieli"}
   :oppiaine-b1                {:fi "B1-kieli"
                                :sv "B1-språk"
                                :en "B1-kieli"}
   :oppiaine-b2                {:fi "B2-kieli"
                                :sv "B2-språk"
                                :en "B2-kieli"}
   :oppiaine-b3                {:fi "B3-kieli"
                                :sv "B3-språk"
                                :en "B3-kieli"}
   :oppiaine-ma                {:fi "Matematiikka"
                                :sv "Matematik"
                                :en "Matematiikka"}
   :oppiaine-bi                {:fi "Biologia"
                                :sv "Biologi"
                                :en "Biologia"}
   :oppiaine-ge                {:fi "Maantieto"
                                :sv "Geografi"
                                :en "Maantieto"}
   :oppiaine-fy                {:fi "Fysiikka"
                                :sv "Fysik"
                                :en "Fysiikka"}
   :oppiaine-ke                {:fi "Kemia"
                                :sv "Kemi"
                                :en "Kemia"}
   :oppiaine-tt                {:fi "Terveystieto"
                                :sv "Hälsokunskap"
                                :en "Terveystieto"}
   :oppiaine-ty                {:fi "Uskonto tai elämänkatsomustieto"
                                :sv "Religion eller livsåskådningskunskap"
                                :en "Uskonto tai elämänkatsomustieto"}
   :oppiaine-hi                {:fi "Historia"
                                :sv "Historia"
                                :en "Historia"}
   :oppiaine-yh                {:fi "Yhteiskuntaoppi"
                                :sv "Samhällslära"
                                :en "Yhteiskuntaoppi"}
   :oppiaine-mu                {:fi "Musiikki"
                                :sv "Musik"
                                :en "Musiikki"}
   :oppiaine-ku                {:fi "Kuvaamataito"
                                :sv "Bildkonst"
                                :en "Kuvaamataito"}
   :oppiaine-ka                {:fi "Käsityö"
                                :sv "Slöjd"
                                :en "Käsityö"}
   :oppiaine-li                {:fi "Liikunta"
                                :sv "Gymnastik"
                                :en "Liikunta"}
   :oppiaine-ko                {:fi "Kotitalous"
                                :sv "Huslig ekonomi"
                                :en "Kotitalous"}
   :oppiaine-valinnainen-kieli {:fi "Valinnainen kieli"
                                :sv "Valfritt språk"
                                :en "Valinnainen kieli"}})

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
                        :sv "Avlagd år"}
   :close              {:en "Close"
                        :fi "Sulje"
                        :sv "Stäng"}})

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

(def base-education-2nd-module-texts
  {:section-title                           {:fi "Pohjakoulutuksesi"
                                             :sv "Grundutbildning"
                                             :en "EN: Pohjakoulutuksesi"}
   :choose-base-education                   {:fi "Valitse yksi pohjakoulutus, jolla haet koulutukseen"
                                             :sv "Välj den grundutbildning med vilken du söker till utbildningen"
                                             :en "EN: Valitse yksi pohjakoulutus, jolla haet koulutukseen"}
   :choose-base-education-info              {:fi "Jos saat perusopetuksen päättötodistuksen tänä keväänä (olet ysiluokkalainen), valitse se oppimäärä, jonka perusteella suoritat perusopetusta. \n \n\nJos sinulla on ainoastaan ulkomailla suoritettu koulutus, niin valitse Ulkomailla suoritettu koulutus. Perusopetuksen oppimäärällä tarkoitetaan Suomessa suoritettua tai suoritettavaa oppimäärää."
                                             :sv "Om du får avgångsbetyg från den grundläggande utbildningen den här våren (du går på nian), välj den lärokurs med vilken du avlägger din grundutbildning.\n\n\nOm du endast har en utbildning som du avlagt utomlands, välj då ”Utbildning utomlands”. Den grundläggande utbildningens lärokurs betyder en lärokurs som du avlagt eller avlägger i Finland."
                                             :en "EN: Jos saat perusopetuksen päättötodistuksen tänä keväänä (olet ysiluokkalainen), valitse se oppimäärä, jonka perusteella suoritat perusopetusta. \n \n\nJos sinulla on ainoastaan ulkomailla suoritettu koulutus, niin valitse Ulkomailla suoritettu koulutus. Perusopetuksen oppimäärällä tarkoitetaan Suomessa suoritettua tai suoritettavaa oppimäärää. \n\n\n\n\n"}
   :base-education                          {:fi "Perusopetuksen oppimäärä"
                                             :sv "Den grundläggande utbildningens lärokurs"
                                             :en "EN: Perusopetuksen oppimäärä"}
   :base-education-partially-individualized {:fi "Perusopetuksen osittain yksilöllistetty oppimäärä"
                                             :sv "Delvis individualiserad lärokurs inom den grundläggande utbildningen"
                                             :en "EN: Perusopetuksen osittain yksilöllistetty oppimäärä"}
   :base-education-individualized           {:fi "Perusopetuksen pääosin tai kokonaan yksilöllistetty oppimäärä"
                                             :sv "Helt eller i huvudsak individualiserad lärokurs inom den grundläggande utbildningen"
                                             :en "EN: Perusopetuksen pääosin tai kokonaan yksilöllistetty oppimäärä"}
   :base-education-organized-regionly       {:fi "Perusopetuksen yksilöllistetty oppimäärä, opetus järjestetty toiminta-alueittain"
                                             :sv "Individualiserad lärokurs inom den grundläggande utbildningen, som utgår från verksamhetsområden"
                                             :en "EN: Perusopetuksen yksilöllistetty oppimäärä, opetus järjestetty toiminta-alueittain"}
   :base-education-foreign                  {:fi "Ulkomailla suoritettu koulutus"
                                             :sv "Utbildning utomlands"
                                             :en "EN: Ulkomailla suoritettu koulutus"}
   :base-education-no-graduation            {:fi "Ei päättötodistusta"
                                             :sv "Inget avgångsbetyg"
                                             :en "EN: Ei päättötodistusta"}
   :study-language                          {:fi "Millä opetuskielellä olet suorittanut perusopetuksen?"
                                             :sv "På vilket språk har du avlagt grundutbildningen?"
                                             :en "EN: Millä opetuskielellä olet suorittanut perusopetuksen?"}
   :language-finnish                        {:fi "suomi"
                                             :sv "finska"
                                             :en "Finnish"}
   :language-swedish                        {:fi "ruotsi"
                                             :sv "svenska"
                                             :en "Swedish"}
   :language-saame                          {:fi "saame"
                                             :sv "samiska"
                                             :en "EN: saame"}
   :language-english                        {:fi "englanti"
                                             :sv "engelska"
                                             :en "English"}
   :language-german                         {:fi "saksa"
                                             :sv "tyska"
                                             :en "German"}
   :graduated-question                      {:fi "Oletko suorittanut Suomessa tai ulkomailla ammatillisen tutkinnon, lukion oppimäärän tai korkeakoulututkinnon?"
                                             :sv "Har du avlagt en yrkesutbildning, gymnasiets lärokurs eller en högskoleexamen i Finland eller utomlands?"
                                             :en "EN: Oletko suorittanut Suomessa tai ulkomailla ammatillisen tutkinnon, lukion oppimäärän tai korkeakoulututkinnon?"}
   :graduated-notification                  {:fi "Koska olet suorittanut ammatillisen tutkinnon, lukion oppimäärän tai korkeakoulututkinnon, et voi hakea perusopetuksen jälkeisen koulutuksen yhteishaussa. \n"
                                             :sv "Eftersom du redan har avlagt en yrkesinriktad examen, gymnasiets lärokurs eller en högskoleexamen kan du inte söka i gemensamma ansökan.\n\n"
                                             :en "EN: Koska olet suorittanut ammatillisen tutkinnon, lukion oppimäärän tai korkeakoulututkinnon, et voi hakea perusopetuksen jälkeisen koulutuksen yhteishaussa. \n"}
   :graduated-question-conditional          {:fi "Jos olet suorittanut jonkun seuraavista, valitse koulutus"
                                             :sv "Om du har avlagt någon av följande, välj utbildning"
                                             :en "EN: Jos olet suorittanut jonkun seuraavista, valitse koulutus"}
   :tenth-grade                             {:fi "Kymppiluokka (perusopetuksen lisäopetus, vähintään 1100 tuntia) "
                                             :sv "Tionde klassen (den grundläggande utbildningens påbyggnadsundervisning, minst 1 100 timmar)"
                                             :en "EN: Kymppiluokka (perusopetuksen lisäopetus, vähintään 1100 tuntia) "}
   :valma                                   {:fi "Ammatilliseen koulutukseen valmentava koulutus VALMA (vähintään 30 osaamispistettä)"
                                             :sv "Utbildning som handleder för yrkesutbildning (VALMA) (minst 30 kompetenspoäng)"
                                             :en "EN: Ammatilliseen koulutukseen valmentava koulutus VALMA (vähintään 30 osaamispistettä)"}
   :luva                                    {:fi "Maahanmuuttajien lukiokoulutukseen valmistava koulutus LUVA (vähintään 25 kurssia)"
                                             :sv "Utbildning som förbereder för gymnasieutbildning som ordnas för invandrare (minst 25 kurser)"
                                             :en "EN: Maahanmuuttajien lukiokoulutukseen valmistava koulutus LUVA (vähintään 25 kurssia)"}
   :kansanopisto                            {:fi "Kansanopiston lukuvuoden mittainen linja (vähintään 28 opiskelijaviikkoa)"
                                             :sv "Ett år lång studielinje vid en folkhögskola (minst 28 studerandeveckor)"
                                             :en "EN: Kansanopiston lukuvuoden mittainen linja (vähintään 28 opiskelijaviikkoa)"}
   :free-civilized                          {:fi "Oppivelvollisille suunnattu vapaan sivistystyön koulutus (vähintään 17 opiskelijaviikkoa)"
                                             :sv "Utbildning inom det fria bildningsarbetet som riktar sig till läropliktiga (minst 17 studerandeveckor)"
                                             :en "EN: Oppivelvollisille suunnattu vapaan sivistystyön koulutus (vähintään 17 opiskelijaviikkoa)"}
   :year-of-graduation                      {:fi "Suoritusvuosi"
                                             :sv "År då utbildningen har avlagts"
                                             :en "EN: Suoritusvuosi"}
   :year-of-graduation-question             {:fi "Suoritusvuosi"
                                             :sv "År för avläggande av den grundläggande utbildningen"
                                             :en "EN: Suoritusvuosi"}
   :individualized-question                 {:fi "Oletko opiskellut sekä matematiikan että äidinkielen yksilöllistetyn oppimäärän mukaisesti?"
                                             :sv "Har du studerat både matematik och modersmål enligt individualiserad lärokurs?"
                                             :en "EN Oletko opiskellut sekä matematiikan että äidinkielen yksilöllistetyn oppimäärän mukaisesti?"}
   :individualized-info                     {:fi "Jos sinulla on jo perusopetuksen päättötodistus, yksilöllistettyjen oppiaineiden arvosanat on merkitty tähdellä (*). Jos et ole vielä saanut päättötodistusta, voit kysyä neuvoa opinto-ohjaajalta."
                                             :sv "Om du redan har den grundläggande utbildningens avgångsbetyg, är vitsorden för individualiserade lärokurser märkt med en stjärna (*). Om du inte ännu har fått avgångsbetyget, kan du be elevhandledaren om råd."
                                             :en "EN: Jos sinulla on jo perusopetuksen päättötodistus, yksilöllistettyjen oppiaineiden arvosanat on merkitty tähdellä (*). Jos et ole vielä saanut päättötodistusta, voit kysyä neuvoa opinto-ohjaajalta."}
   :individualized-harkinnanvaraisuus       {:fi "Koska olet opiskellut sekä matematiikan että äidinkielen yksilöllistetyn oppimäärän mukaisesti, hakemuksesi käsitellään harkinnanvaraisesti, jos haet ammatilliseen koulutukseen tai lukioon. \n\nJos haluat lähettää liitteitä harkinnanvaraisen haun tueksi, tarkista palautusosoite oppilaitoksista, joihin haet. "
                                             :sv "Eftersom du har studerat både matematik och modersmål enligt individualiserad lärokurs, behandlas din ansökan via antagning enligt prövning om du söker till yrkesutbildning eller till gymnasium.\n\nOm du vill skicka in bilagor som stöd för din ansökan via prövning, kontrollera leveransadressen från de läroanstalter som du söker till."
                                             :en "EN: Koska olet opiskellut sekä matematiikan että äidinkielen yksilöllistetyn oppimäärän mukaisesti, hakemuksesi käsitellään harkinnanvaraisesti, jos haet ammatilliseen koulutukseen tai lukioon. \n\nJos haluat lähettää liitteitä harkinnanvaraisen haun tueksi, tarkista palautusosoite oppilaitoksista, joihin haet. "}
   :foreign-harkinnanvaraisuus              {:fi "Koska olet suorittanut tutkintosi ulkomailla, haet automaattisesti harkintaan perustuvassa valinnassa. Toimitathan kopion tutkintotodistuksestasi oppilaitoksiin."
                                             :sv "Eftersom du har avlagt din examen utomlands, söker du automatiskt via antagning enligt prövning. Skicka en kopia av ditt examensbetyg till läroanstalterna."
                                             :en "EN: Koska olet suorittanut tutkintosi ulkomailla, haet automaattisesti harkintaan perustuvassa valinnassa. Toimitathan kopion tutkintotodistuksestasi oppilaitoksiin."}
   :copy-of-proof-of-certificate            {:fi "Kopio tutkintotodistuksesta "
                                             :sv "Kopia av examensbetyget"
                                             :en "EN: Kopio tutkintotodistuksesta "}
   :copy-of-proof-of-certificate-info       {:fi "Tallenna tutkintotodistuksesi joko pdf-muodossa tai kuvatiedostona (esim. png tai jpeg). \n\n"
                                             :sv "Spara ditt examensbetyg antingen i pdf-format eller som bildfil (t.ex. png eller jpeg)."
                                             :en "EN: Tallenna tutkintotodistuksesi joko pdf-muodossa tai kuvatiedostona (esim. png tai jpeg). \n\n"}
   :copy-of-certificate                     {:fi "Todistus, jolla haet "
                                             :sv "Betyg som du söker med"
                                             :en "EN: Todistus, jolla haet "}
   :copy-of-certificate-info                {:fi "Tallenna todistuksesi joko pdf-muodossa tai kuvatiedostona (esim. png tai jpeg). "
                                             :sv "Spara ditt betyg antingen i pdf-format eller som bildfil (t.ex. png eller jpeg)."
                                             :en "EN: Tallenna todistuksesi joko pdf-muodossa tai kuvatiedostona (esim. png tai jpeg). "}
   :no-graduation-info                      {:fi "Valitse tämä vain silloin, kun olet keskeyttänyt perusopetuksen. \n\nHaet automaattisesti harkintaan perustuvassa valinnassa. "
                                             :sv "Välj den här endast om du har avbrutit den grundläggande utbildningen.\n\nDu söker automatiskt via antagning enligt prövning."
                                             :en "EN: Valitse tämä vain silloin, kun olet keskeyttänyt perusopetuksen. \n\nHaet automaattisesti harkintaan perustuvassa valinnassa. "}})

(def base-education-cotinuous-admissions-module-texts
  {:section-title              {:fi "Pohjakoulutuksesi"
                                :sv "Grundutbildning"
                                :en "Your educational background"}
   :choose-base-education      {:fi "Valitse yksi pohjakoulutus, jolla haet koulutukseen"
                                :sv "Välj den grundutbildning med vilken du söker till utbildningen"
                                :en "Fill in the education that you have completed"}
   :choose-base-education-info {:fi "Jos olet suorittanut useamman pohjakoulutuksen, \nvalitse ylin koulutuksesi."
                                :sv "Om du har avlagt mer än en grundutbildning, välj \ndin högsta utbildning."
                                :en "If you have completed several qualifications, fill in \nthe highest level that you have completed."}
   :base-education             {:fi "Perusopetuksen oppimäärä"
                                :sv "Den grundläggande utbildningens lärokurs"
                                :en "EN: Perusopetuksen oppimäärä"}})

(def kk-application-payment-module-texts
  {:section-title                      {:fi "Hakemusmaksu ja lukuvuosimaksu"
                                        :sv "Ansökningsavgift och läsårsavgift"
                                        :en "Application fee and tuition fee"}
   :document-option-title              {:fi "Minulla on seuraava dokumentti"
                                        :sv "Jag har följande dokument"
                                        :en "I have the following document"}
   :document-option-info               {:fi "Hakemusmaksu\n\nEU- ja ETA-alueen sekä Sveitsin ulkopuoliselta kansalaiselta peritään 100 euron suuruinen hakemusmaksu, kun hakija hakee tutkintoon johtavaan koulutukseen. \nHakemusmaksu on lukukausikohtainen, eli voit hakea useissa hauissa samana lukuvuotena alkaviin koulutuksiin ja riittää, että maksat hakemusmaksun vain kerran. \nSinun ei tarvitse maksaa hakemusmaksua, jos sinulla on jokin alla mainituista dokumenteista. \nHuomioithan, että sinun tulee liittää hakemukselle kopio maksusta vapauttavasta dokumentista.\n\nVoit lukea lisää hakemusmaksuista [Opintopolku.fi -sivulta](https://opintopolku.fi/konfo/fi/sivu/hakemusmaksu). \n\nLukuvuosimaksu\n\nEU- ja ETA-alueen sekä Sveitsin ulkopuoliselta kansalaiselta, joka hyväksytään muuhun kuin suomen- tai ruotsinkieliseen koulutukseen, peritään lukuvuosimaksu. Sinun ei tarvitse maksaa lukuvuosimaksua, jos sinulla on jokin alla mainituista dokumenteista. Huomioithan, että sinun tulee liittää hakemukselle kopio maksusta vapauttavasta dokumentista.\n\nVoit lukea lisää lukuvuosimaksuista [Opintopolku.fi-sivulta](https://opintopolku.fi/konfo/fi/sivu/valmistaudu-korkeakoulujen-yhteishakuun#lukuvuosimaksu-korkeakoulujen-vieraskielisissa-koulutuksissa). Tarkemmat tiedot lukuvuosimaksuista ja apurahan hakemisesta löydät korkeakoulujen omilta verkkosivuilta ja [Opintopolku.fi](http://opintopolku.fi/) -sivustolla olevista hakukohteiden valintaperustetiedoista. "
                                        :sv "Ansökningsavgift\n\nEn ansökningsavgift på 100 euro tas ut av medborgare utanför EU- och EES-området samt Schweiz i samband med ansökan till utbildning som leder till högskoleexamen.\n\nAnsökningsavgiften gäller för en termin, det vill säga att du kan söka till utbildningar som börjar samma läsår i flera olika ansökningar och det räcker att du betalar ansökningsavgiften endast en gång.\n\nDu är befriad från att betala ansökningsavgiften om du har något av dokumenten nedan.\n\nVänligen notera att du måste ladda upp en kopia av det dokument som befriar dig från att betala avgiften.\n\nDu kan läsa mer om ansökningsavgiften [på Studieinfo.fi](https://opintopolku.fi/konfo/sv/sivu/ansoekningsavgift).\n\nLäsårsavgift\n\nMedborgare utanför EU- och EES-området samt Schweiz som antas till annan än finsk- eller svenskspråkig utbildning ska betala en läsårsavgift. Du behöver inte betala läsårsavgift om du har något av nedanstående dokument. Observera att du till ansökan ska bifoga en kopia av det dokument som befriar från avgiften.\n\nLäs mer om läsårsavgifter på [Studieinfo.fi](https://opintopolku.fi/konfo/sv/sivu/forbered-dig-for-gemensam-ansokan-till-hogskolor#lasarsavgift-fr-hgskolornas-utbildningar-pa-frammande-sprak). Närmare information om läsårsavgifter och att ansöka om stipendium finns på högskolornas webbsidor och i utbildningarnas uppgifter om ansökningsmål och antagningskriterier i [Studieinfo.fi](https://opintopolku.fi/konfo/sv/)."
                                        :en "Application fee\n\nIf you are not an EU/EEA or Swiss citizen, you will need to pay an application fee to the sum of 100 euros when you apply to a programme leading to a Bachelor's or Master's degree. \n\nIf you apply in multiple application rounds to study programmes starting in the same academic term (e.g. fall 2025), you only need to pay the application fee once. \n\nIf you have one of the following documents, you do not need to pay the application fee. \n\nPlease not that you need to attach a copy of said document that frees you from paying the application fee to the application form. \n\nYou can read more about application fees on [Studyinfo](https://opintopolku.fi/konfo/en/sivu/application-fee).\n\nTuition fee\n\nIf you are not an EU/EEA or Swiss citizen and you are offered admission to a degree programme that is not in Finnish or Swedish, you need to pay an annual tuition fee. \n\nIf you have one of the following documents, you do not need to pay the tuition fee. \n\nPlease not that you need to attach a copy of said document that frees you from paying the tuition fee to the application form. \n\nYou can read more about tuition fees on [Studyinfo](https://opintopolku.fi/konfo/en/sivu/tuition-fees). Detailed information about tuition fees and scholarships can be found on the websites of the higher education institutions and from the programme descriptions on [Studyinfo.fi](http://studyinfo.fi/)."}
   :passport-option                    {:fi "Passi (tai henkilökortti), josta ilmenee EU- tai ETA-maan tai Sveitsin kansalaisuus"
                                        :sv "Pass (eller identitetskort) som visar medborgarskap i EU/EES/Schweiz"
                                        :en "Passport (or identity card) to indicate the citizenship of EU/EEA/Switzerland "}
   :eu-blue-card-option                {:fi "EU:n sininen kortti Suomessa"
                                        :sv "EU-blåkort i Finland"
                                        :en "EU Blue Card in Finland"}
   :continuous-residence-option        {:fi "Jatkuva oleskelulupakortti Suomessa, oleskelulupatyyppi A, myönnetty muulla kuin opiskelun perusteella"
                                        :sv "Kontinuerligt uppehållstillstånd i Finland, tillståndstyp A, som beviljats av annan orsak än för studier"
                                        :en "Continuous residence permit in Finland,  Type A permit issued for purposes other than studies"}
   :longterm-residence-option          {:fi "Pitkään oleskelleen kolmannen maan kansalaisen EU-oleskelulupakortti Suomessa, oleskelulupatyyppi P-EU"
                                        :sv "EU-uppehållstillstånd för varaktigt bosatta tredjelandsmedborgare med permanent uppehållstillstånd i Finland, tillståndstyp P-EU"
                                        :en "EU residence permit for third-country citizens with long-term residence permit card in Finland (Type P-EU)"}
   :brexit-option                      {:fi "Brexit-oleskelulupakortti, lupatyyppi SEU-sopimuksen 50 artikla = Erosopimuksen piiriin kuuluva oleskeluoikeus, tai  P SEU-sopimuksen 50 artikla = Erosopimuksen mukainen pysyvä oleskeluoikeus"
                                        :sv "Brexit-uppehållstillståndskort, tillståndstyp SEU-sopimuksen 50 artikla = Uppehållsrätt i enlighet med utträdesavtalet, eller P SEU-sopimuksen 50 artikla = Permanent uppehållsrätt i enlighet med utträdesavtalet"
                                        :en "Brexit residence permit card, Type SEU = Right of residence under the withdrawal agreement, or P SEU = Right of permanent residence under the withdrawal agreement"}
   :permanent-residence-option         {:fi "Pysyvä oleskelulupakortti Suomessa, oleskelulupa P"
                                        :sv "Permanent uppehållstillstånd i Finland, tillståndstyp P"
                                        :en "Permanent residence permit card in Finland, Type P permit"}
   :eu-family-member-option            {:fi "EU-kansalaisen perheenjäsenen oleskelukortti Suomessa"
                                        :sv "Uppehållstillståndkort som EU medborgares familjemedlem i Finland"
                                        :en "EU Family Member's Residence Card in Finland "}
   :temporary-protection-option        {:fi "Tilapäisen suojelun oleskelulupakortti Suomessa"
                                        :sv "Uppehållstillstånd på grund av tillfälligt skydd i Finland"
                                        :en "Residence permit card on the basis of temporary protection in Finland"}
   :no-document-option                 {:fi "Minulla ei ole mitään edellä mainituista dokumenteista"
                                        :sv "Jag har inget av de ovannämnda"
                                        :en "I do not have any of the above"}
   :asiakasnumero-migri                {:fi "Asiakasnumero"
                                        :sv "Kundnummer"
                                        :en "Customer number"}
   :asiakasnumero-migri-info           {:fi "Kirjoita tähän asiakasnumerosi Maahanmuuttovirastossa. Löydät asiakasnumerosi oleskelulupakortistasi. "
                                        :sv "Fyll i ditt kundnummer hos Migrationsverket. Du hittar ditt kundnummer på ditt uppehållstillståndskort."
                                        :en "Write your customer number at the Finnish Immigration Service. You can find your customer number on your residence permit card."}
   :continuous-residence-info          {:fi "Jatkuva oleskelulupa (oleskelulupatyyppi A) voi vapauttaa sinut lukuvuosimaksuvelvollisuudesta, jos se on myönnetty muulla perusteella kuin opiskelua varten. Jatkuva oleskelulupa ei kuitenkaan vapauta lukuvuosimaksuvelvollisuudesta seuraavissa tapauksissa: \n- Jos ensimmäinen oleskelulupasi Suomessa on myönnetty opiskelua varten, olet hakemus- ja/tai lukuvuosimaksuvelvollinen, vaikka olisit myöhemmin saanut jatkuvan oleskeluluvan (oleskelulupatyyppi A) muulla perusteella. \n- Jos sinulla on jatkuva oleskelulupa (oleskelulupatyyppi A) perhesiteen perusteella ja perheenjäsenesi on alun perin saapunut Suomeen opiskelua varten myönnetyllä oleskeluluvalla, olet myös hakemus- ja/tai lukuvuosimaksuvelvollinen."
                                        :sv "Ett kontinuerligt uppehållstillstånd (typ A) kan befria dig från att betala ansöknings- och/eller läsårsavgift om det beviljas på annan grund än studier. Du är dock skyldig att betala ansöknings- och/eller läsårsavgift i följande fall: \n- Om ditt första uppehållstillstånd i Finland beviljades för studier är du skyldig att betala ansöknings- och/eller läsårsavgift även om du senare fick ett kontinuerligt uppehållstillstånd (typ A) av annan orsak. \n- Om du har ett kontinuerligt uppehållstillstånd (typ A) på grund av familjeband och din familjemedlem ursprungligen kom till Finland med uppehållstillstånd för studier, måste du betala ansöknings- och/eller läsårsavgift."
                                        :en "A continuous residence permit (type A) may exempt you from paying application and/or tuition fees if it is granted on grounds other than studying. However, you are required to pay application and/or tuition fees in the following cases: \n- If your first residence permit in Finland was granted for the purpose of studying, you are required to pay application and/or tuition fees, even if you later obtained a continuous residence permit (type A) on other grounds. \n- If you have a continuous residence permit (type A) based on family ties, and your family member originally came to Finland on a residence permit granted for studying, you are also required to pay application and/or tuition fees."}
   :attachment-info                    {:fi "Tallenna liite viimeistään 7 vuorokauden sisällä hakuajan päättymisestä. Määräaika ilmoitetaan liitepyynnön vieressä. \n\nNimeä tiedostot muotoon \"Sukunimi\\_Etunimi\\_dokumentti\", esimerkiksi Meikäläinen\\_Maija\\_oleskelulupakortti.\n\n Skannaa vaadittavan dokumentin kaikki sivut, joissa on tekstiä, tai ota niistä hyvälaatuiset kuvat. Varmista, että kuvista saa selvää. Kokoa samaan kokonaisuuteen liittyvät sivut yhteen tiedostoon. Esimerkiksi oleskelulupakortin tulisi olla yksi tiedosto, joka voi sisältää useita sivuja. Tarkista, että dokumentit ovat tiedostossa oikein päin. \n\nSuositeltuja tiedostomuotoja ovat PDF, JPG, PNG ja DOCX."
                                        :sv "Spara bilagan senast inom 7 dygn efter att ansökningstiden har utgått. Den angivna tidpunkten syns invid begäran om bilagor. \n\nNamnge bilagorna i formen \"Efternamn\\_Förnamn\\_dokument\", t.ex. Svensson\\_Sven\\_uppehållskort.\n\n Skanna samtliga textsidor i dokumentet, eller fotografera sidorna med tillräckligt hög kvalitet. Kontrollera att bilderna är tydliga. Samla samtliga sidor som hör till samma helhet i en gemensam fil. T.ex. ska uppehållskortet ingå i en fil, som dock kan innehålla flera sidor. Kontrollera att dokumenten i filen är rättvända. \n\nRekommenderade filformat är PDF, JPG, PNG och DOCX."
                                        :en "Submit the attachment within 7 (seven) days after the application period has closed. The exact deadline is available next to the attachment request. \n\nName the attachment file(s) in the following way: \"Lastname\\_First name\\_description/name of document\". For example, Smith\\_Mary\\_residence_permit. \n\nScan all the pages of the required document or take good quality pictures. Make sure that the pictures/scans are legible. Combine the pages of the same document into one file. For example, a residence permit should be in one file that can include several pages. Check that the documents are all positioned in the same way upright. \n\nRecommended file formats are: PDF, JPG, PNG and DOCX."}
   :attachment-deadline                {:fi "Voimassaolon viimeinen päivä (pp/kk/vvvv)"
                                        :sv "Det sista giltighetsdatum (dd/mm/åååå)"
                                        :en "Valid until (dd/mm/yyyy)"}
   :passport-attachment                {:fi "Kopio passista tai henkilökortista"
                                        :sv "Pass eller identitetskort"
                                        :en "Copy of passport or identity card"}
   :eu-blue-card-attachment            {:fi "Kopio oleskeluluvasta (EU:n sininen kortti) Suomeen"
                                        :sv "Uppehållstillstånd (EU-blåkort) i Finland"
                                        :en "Copy of residence permit (EU Blue Card) to Finland"}
   :continuous-permit-front-attachment {:fi "Kopio oleskeluluvasta (A) Suomeen - etupuoli"
                                        :sv "Uppehållstillstånd (A) i Finland - framsida"
                                        :en "Copy of residence permit (A) to Finland - frontside"}
   :continuous-permit-back-attachment  {:fi "Kopio oleskeluluvasta (A) Suomeen - takapuoli"
                                        :sv "Uppehållstillstånd (A) i Finland - baksida"
                                        :en "Copy of residence permit (A) to Finland - backside"}
   :longterm-permit-attachment         {:fi "Kopio oleskeluluvasta (P-EU) Suomeen"
                                        :sv "Uppehållstillstånd (P-EU) i Finland"
                                        :en "Copy of residence permit (P-EU) to Finland"}
   :brexit-permit-attachment           {:fi "Kopio Brexit-oleskeluluvasta (SEU tai P SEU) Suomeen"
                                        :sv "Kopian av Brexit-uppehållstillstånd (SEU eller P SEU) i Finland"
                                        :en "Copy of Brexit residence permit (SEU or P SEU) to Finland"}
   :permanent-permit-attachment        {:fi "Kopio oleskeluluvasta (P) Suomeen"
                                        :sv "Uppehållstillstånd (P) i Finland"
                                        :en "Copy of residence permit (P) to Finland"}
   :eu-family-member-attachment        {:fi "Kopio oleskeluluvasta (EU-kansalaisen perheenjäsenen oleskelukortti) Suomeen"
                                        :sv "Uppehållstillstånd (Uppehållstillståndkort som EU medborgares familjemedlem) i Finland"
                                        :en "Copy of residence permit (EU Family Member's Residence Card) to Finland"}
   :temporary-protection-attachment    {:fi "Tilapäisen suojelun oleskelulupakortti"
                                        :sv "Uppehållstillståndskort på grund av tillfälligt skydd"
                                        :en "Residence permit card on the basis of temporary protection"}
   :none-passport-info                 {:fi "Ilmoittamiesi tietojen perusteella sinulla ei ole maksusta vapauttavia dokumentteja, joten sinun tulee maksaa 100 euron suuruinen hakemusmaksu viimeistään 7 vuorokauden kuluttua hakemuksen lähettämisestä. Saat hakulomakkeen lähettämisen jälkeen hakemusmaksun maksuohjeet sähköpostiisi.\n\nJos olet jo maksanut hakemusmaksun toisen haun yhteydessä, sinun ei tarvitse maksaa hakemusmaksua uudelleen. Et saa tällöin uutta maksulinkkiä.\n\nHuomioithan, että hakemusmaksun maksaminen ei vielä tarkoita, että sinut hyväksytään koulutukseen.\n\nJos haet muuhun kuin suomen- tai ruotsinkieliseen koulutukseen, sinun tulee maksaa myös lukuvuosimaksu, jos sinut hyväksytään koulutukseen."
                                        :sv "Enligt de uppgifter som du angett har du inte dokument som befriar dig från ansökningsavgiften. Du måste alltså betala en ansökningsavgift på 100 euro inom 7 dygn från att du skickat ansökningsblanketten. Efter att du skickat ansökningsblanketten får du anvisningar för hur du betalar ansökningsavgiften till din e-post.\n\nObservera att betalning av ansökningsavgift ännu inte innebär att du blir antagen till utbildningen.\n\nOm du söker till annat än svensk- eller finskspråkig utbildning, ska du också betala en läsårsavgift ifall du blir antagen till utbildningen."
                                        :en "According to the information you have provided you do not have any documents that free you from paying the application fee. You need to pay the application in the sum of 100 euros in seven (7) days after sending in the application. You will be sent instructions on how to pay the application fee as well as a link to the payment after you have sent in the application.\n\nIf you have already paid the application fee after sending in another application, you do not need to pay the application fee again. In this case, you will not be sent a new payment link.\n\nPlease note that paying the application fee does not mean that you will be automatically offered admission.\n\nIf you are applying to degree programmes that are not in Finnish or Swedish, please note that you also need to pay an annual tuition fee if you are offered admission."}})

(def person-info-module-texts
  {:forenames            {:fi "Etunimet"
                          :sv "Förnamn"
                          :en "First/given names"}
   :main-forename        {:fi "Kutsumanimi"
                          :sv "Tilltalsnamn"
                          :en "Preferred first/given name"}
   :surname              {:fi "Sukunimi"
                          :sv "Efternamn"
                          :en "Surname/Family name"}
   :nationality          {:fi "Kansalaisuus"
                          :sv "Medborgarskap"
                          :en "Nationality"}
   :country-of-residence {:fi "Asuinmaa"
                          :sv "Bosättningsland"
                          :en "Country of residence"}
   :have-finnish-ssn     {:fi "Onko sinulla suomalainen henkilötunnus?"
                          :sv "Har du en finländsk personbeteckning?"
                          :en "Do you have a Finnish personal identity code?"}
   :ssn                  {:fi "Henkilötunnus"
                          :sv "Personbeteckning"
                          :en "Finnish personal identity code"}
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
                          :en "Municipality of residence"}
   :city                 {:fi "Kaupunki ja maa"
                          :sv "Stad och land"
                          :en "City and country"}
   :postal-code          {:fi "Postinumero"
                          :sv "Postnummer"
                          :en "Postal code"}
   :postal-office        {:fi "Postitoimipaikka"
                          :sv "Postkontor"
                          :en "Town/city"}
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

(def email-link-section-texts {:default              {:fi "Voit katsella ja muokata hakemustasi yllä olevan linkin kautta. Älä jaa linkkiä ulkopuolisille. Jos käytät yhteiskäyttöistä tietokonetta, muista kirjautua ulos sähköpostiohjelmasta.\n\nJos sinulla on verkkopankkitunnukset, mobiilivarmenne tai sähköinen henkilökortti, voit vaihtoehtoisesti kirjautua sisään [Opintopolku.fi](https://www.opintopolku.fi):ssä, ja tehdä muutoksia hakemukseesi Oma Opintopolku -palvelussa hakuaikana. Oma Opintopolku -palvelussa voit lisäksi nähdä valintojen tulokset ja ottaa opiskelupaikan vastaan."
                                                      :sv "Om du vill ändra din ansökan, kan du göra ändringar via länken ovan. Dela inte länken vidare till utomstående. Kom ihåg att logga ut från e-postprogrammet om du använder en offentlig dator.\n\nOm du har nätbankskoder, mobilcertifikat eller ett elektroniskt ID-kort, kan du alternativt logga in i [Studieinfo.fi](https://www.studieinfo.fi) och under ansökningstiden göra ändringarna i tjänsten Min Studieinfo. I tjänsten kan du också, se antagningsresultaten och ta emot studieplatsen."
                                                      :en "If you wish to edit your application, you can use the link above and make the changes within the application period. Do not share the link with others. If you are using a public or shared computer, remember to log out of the email application.\n\nIf you have Finnish online banking credentials, an electronic\nID-card or mobile certificate, you can also log in\nat [Studyinfo.fi](https://www.studyinfo.fi) and make the\nchanges in the My Studyinfo -service within the application period. In addition to making changes to your application, if you have access to the My Studyinfo -service you can also view the admission results and confirm the study place."}
                               :no-hakuaika-mentions {:fi "Voit katsella ja muokata hakemustasi yllä olevan linkin kautta. Älä jaa linkkiä ulkopuolisille. Jos käytät yhteiskäyttöistä tietokonetta, muista kirjautua ulos sähköpostiohjelmasta.\n\nJos sinulla on verkkopankkitunnukset, mobiilivarmenne tai sähköinen henkilökortti, voit vaihtoehtoisesti kirjautua sisään [Opintopolku.fi](https://www.opintopolku.fi):ssä, ja tehdä muutoksia hakemukseesi Oma Opintopolku -palvelussa."
                                                      :sv "Om du vill ändra din ansökan, kan du göra ändringar via länken ovan. Dela inte länken vidare till utomstående. Kom ihåg att logga ut från e-postprogrammet om du använder en offentlig dator. Om du har nätbankskoder, mobilcertifikat eller ett elektroniskt ID-kort, kan du alternativt logga in i [Studieinfo.fi](https://www.studieinfo.fi) och göra ändringarna i tjänsten Min Studieinfo."
                                                      :en "If you wish to edit your application, you can use the link above to make changes to your application. Do not share the link with others. If you are using a public or shared computer, remember to log out of the email application. If you have Finnish online banking credentials, an electronic ID-card or mobile certificate, you can also log in at [Studyinfo.fi](https://www.studyinfo.fi) and make the changes in the My Studyinfo -service."}})

(def email-default-texts
  {:email-submit-confirmation-template
   {:submit-email-subjects      {:fi "Opintopolku: hakemuksesi on vastaanotettu"
                                 :sv "Studieinfo: Din ansökan har mottagits"
                                 :en "Studyinfo: Your application has been received"}
    :with-application-period    {:fi "Voit katsella ja muokata hakemustasi hakuaikana yllä olevan linkin kautta. Älä jaa linkkiä ulkopuolisille. Jos käytät yhteiskäyttöistä tietokonetta, muista kirjautua ulos sähköpostiohjelmasta.\n\nJos sinulla on verkkopankkitunnukset, mobiilivarmenne tai sähköinen henkilökortti, voit vaihtoehtoisesti kirjautua sisään [Opintopolku.fi](https://www.opintopolku.fi):ssä, ja tehdä muutoksia hakemukseesi Oma Opintopolku -palvelussa hakuaikana. Oma Opintopolku -palvelussa voit lisäksi nähdä valintojen tulokset ja ottaa opiskelupaikan vastaan.\n"
                                 :sv "Om du vill ändra din ansökan, kan du göra ändringar via länken ovan under ansökningstiden. Dela inte länken vidare till utomstående. Kom ihåg att logga ut från e-postprogrammet om du använder en offentlig dator.\n\nOm du har nätbankskoder, mobilcertifikat eller ett elektroniskt ID-kort, kan du alternativt logga in i [Studieinfo.fi](https://www.studieinfo.fi) och under ansökningstiden göra ändringarna i tjänsten Min Studieinfo. I tjänsten kan du också se ditt antagningsresultat och ta emot studieplatsen.\n"
                                 :en "If you wish to edit your application, you can use the link above and make the changes within the application period. Do not share the link with others. If you are using a public or shared computer, remember to log out of the email application.\n\nIf you have Finnish online banking credentials, an electronic ID-card or mobile certificate, you can also log in at [Studyinfo.fi](https://www.studyinfo.fi) and make the changes in the My Studyinfo -service within the application period. In addition to making changes to your application, if you have access to the My Studyinfo -service you can also view the admission results and confirm the study place.\n"}
    :without-application-period {:fi "Voit katsella ja muokata hakemustasi yllä olevan linkin kautta. Älä jaa linkkiä ulkopuolisille. Jos käytät yhteiskäyttöistä tietokonetta, muista kirjautua ulos sähköpostiohjelmasta.\n\nJos sinulla on verkkopankkitunnukset, mobiilivarmenne tai sähköinen henkilökortti, voit vaihtoehtoisesti kirjautua sisään [Opintopolku.fi](https://www.opintopolku.fi):ssä, ja tehdä muutoksia hakemukseesi Oma Opintopolku -palvelussa hakuaikana. Oma Opintopolku -palvelussa voit lisäksi nähdä valintojen tulokset ja ottaa opiskelupaikan vastaan.\n"
                                 :sv "Om du vill ändra din ansökan, kan du göra ändringar via länken ovan. Dela inte länken vidare till utomstående. Kom ihåg att logga ut från e-postprogrammet om du använder en offentlig dator.\n\nOm du har nätbankskoder, mobilcertifikat eller ett elektroniskt ID-kort, kan du alternativt logga in i [Studieinfo.fi](https://www.studieinfo.fi) och under ansökningstiden göra ändringarna i tjänsten Min Studieinfo. I tjänsten kan du också, se antagningsresultaten och ta emot studieplatsen.\n"
                                 :en "If you wish to edit your application, you can use the link above and make the changes within the application period. Do not share the link with others. If you are using a public or shared computer, remember to log out of the email application.\n\nIf you have Finnish online banking credentials, an electronic\nID-card or mobile certificate, you can also log in\nat [Studyinfo.fi](https://www.studyinfo.fi) and make the\nchanges in the My Studyinfo -service within the application period. In addition to making changes to your application, if you have access to the My Studyinfo -service you can also view the admission results and confirm the study place.\n"}
    :signature                  {:fi "Älä vastaa tähän viestiin - viesti on lähetetty automaattisesti.\n\nYstävällisin terveisin <br/>\nOpintopolku\n"
                                 :sv "Svara inte på detta meddelande, det har skickats automatiskt.\n\nMed vänliga hälsningar, <br/>\nStudieinfo\n"
                                 :en "This is an automatically generated email, please do not reply.\n\nBest regards, <br/>\nStudyinfo\n"}}
   :hakemusnumero {:fi "Hakemusnumero"
                   :sv "Ansökningsnummer"
                   :en "Application number"}})


(def virkailija-texts
  {:person-found-matching                                    {:fi "ONR:stä löytyi hakija samalla sähköpostilla, syntymäajalla ja sukupuolella."
                                                              :sv "Sökande med samma e-postadress, födelsetid och kön hittades i studentnummerregistret."
                                                              :en "An applicant with the same e-mail address, date of birth and gender was found in the Student number register."}
   :person-dob-or-gender-conflict                            {:fi "Tämän hakemuksen sähköpostilla löytyi jo ONR:stä henkilö, mutta syntymäaika ja/tai sukupuoli eroaa ONR:stä löytyvän hakijan syntymäajasta ja/tai sukupuolesta. Tämän hakemuksen hakijasta luotiin siksi uusi henkilö, eikä hakemuksella ilmoitettua sähköpostia tallennettu uuden henkilön tietoihin."
                                                              :sv "I studentnummerregistret hittades redan en person med samma e-postadress som används i den här ansökningen, men födelsetid och/eller kön skiljer sig från den födelsetid och/eller kön som finns för sökande i studentnummerregistret. En ny person har därför skapats istället för den här ansökningens sökande. E-postadressen som meddelades på ansökningen har inte sparats i den nya personens uppgifter."
                                                              :en "A person with the same e-mail address as on the application was already found in the Student number register, but the date of birth and/or gender differ from the date of birth and/or gender for the person in the register. A new person was therefore created of the applicant on this application. The e-mail address on this application was not saved in the new person’s data."}
   :valintakasittelymerkinta                                 {:fi "Valintakäsittelymerkintä"
                                                              :sv "Notering om antagningsbehandling"
                                                              :en "Valintakäsittelymerkintä"}
   :valinnan-tila                                            {:fi "Valinnan tila"
                                                              :sv "Antagningens status"
                                                              :en "Valinnan tila"}
   :search-placeholder                                       {:fi "Hae.."
                                                              :sv "Sök.."
                                                              :en "Search.."}
   :use                                                      {:fi "Käytä: "
                                                              :sv "Använda: "
                                                              :en "Use: "}
   :or-use                                                   {:fi ". Tai käytä: "
                                                              :sv ". Eller använda: "
                                                              :en ". Or use: "}
   :return-latest                                            {:fi "Palautettava viimeistään"
                                                              :sv "Sista leveransdatum"
                                                              :en "Deadline in"}
   :valinnan-tila-ladattu-valinnoista                        {:fi "Valintatieto tuotu valintarekisteristä"
                                                              :sv "Antagningsuppgiften hämtad från antagningsregistret"
                                                              :en "Valintatieto tuotu valintarekisteristä"}
   :valinnan-tila-ladataan-valinnoista                       {:fi "Valintatieto lataamatta valintarekisteristä"
                                                              :sv "Antagningsuppgiften inhämtad från antagningsregistret"
                                                              :en "Valintatieto lataamatta valintarekisteristä"}
   :arvosanat-peruskoulu                                     {:fi "Arvosanat (peruskoulu)"
                                                              :sv "Vitsord (grundskola)"
                                                              :en "Arvosanat (peruskoulu)"}
   :arvosanat-lukio                                          {:fi "Arvosanat (lukio)"
                                                              :sv "Vitsord (gymnasium)"
                                                              :en "Arvosanat (lukio)"}
   :arvosana-aidinkieli-ja-kirjallisuus                      {:fi "Äidinkieli ja kirjallisuus"
                                                              :sv "Modersmål och litteratur"
                                                              :en "Äidinkieli ja kirjallisuus"}
   :arvosana-a1-kieli                                        {:fi "A1-kieli"
                                                              :sv "A1-språk"
                                                              :en "A1-kieli"}
   :arvosana-a2-kieli                                        {:fi "A2-kieli"
                                                              :sv "A2-språk"
                                                              :en "A2-kieli"}
   :arvosana-b1-kieli                                        {:fi "B1-kieli"
                                                              :sv "B1-språk"
                                                              :en "B1-kieli"}
   :arvosana-matematiikka                                    {:fi "Matematiikka"
                                                              :sv "Matematik"
                                                              :en "Matematiikka"}
   :arvosana-biologia                                        {:fi "Biologia"
                                                              :sv "Biologi"
                                                              :en "Biologia"}
   :arvosana-maantieto                                       {:fi "Maantieto"
                                                              :sv "Geografi"
                                                              :en "Maantieto"}
   :arvosana-fysiikka                                        {:fi "Fysiikka"
                                                              :sv "Fysik"
                                                              :en "Fysiikka"}
   :arvosana-kemia                                           {:fi "Kemia"
                                                              :sv "Kemi"
                                                              :en "Kemia"}
   :arvosana-terveystieto                                    {:fi "Terveystieto"
                                                              :sv "Hälsokunskap"
                                                              :en "Terveystieto"}
   :arvosana-uskonto-tai-elamankatsomustieto                 {:fi "Uskonto tai elämänkatsomustieto"
                                                              :sv "Religion eller livsåskådningskunskap"
                                                              :en "Uskonto tai elämänkatsomustieto"}
   :arvosana-historia                                        {:fi "Historia"
                                                              :sv "Historia"
                                                              :en "Historia"}
   :arvosana-yhteiskuntaoppi                                 {:fi "Yhteiskuntaoppi"
                                                              :sv "Samhällslära"
                                                              :en "Yhteiskuntaoppi"}
   :arvosana-musiikki                                        {:fi "Musiikki"
                                                              :sv "Musik"
                                                              :en "Musiikki"}
   :arvosana-kuvataide                                       {:fi "Kuvataide"
                                                              :sv "Bildkonst"
                                                              :en "Kuvataide"}
   :arvosana-kasityo                                         {:fi "Käsityö"
                                                              :sv "Slöjd"
                                                              :en "Käsityö"}
   :arvosana-liikunta                                        {:fi "Liikunta"
                                                              :sv "Gymnastik"
                                                              :en "Liikunta"}
   :arvosana-kotitalous                                      {:fi "Kotitalous"
                                                              :sv "Huslig ekonomi"
                                                              :en "Kotitalous"}
   :arvosanat-info                                           {:fi "Merkitse arvosanat perusopetuksen päättötodistuksestasi. Korotetut arvosanat voit merkitä, mikäli olet saanut korotuksista virallisen todistuksen. Jos olet suorittanut lukion oppimäärän, et voi hakea perusopetuksen päättötodistuksella. Ammatillisella perustutkinnolla et voi hakea. Oppilaitokset tarkistavat todistukset hyväksytyksi tulleilta hakijoilta.\n\nHuom! Jos haet perusopetuksen päättötodistuksella, muista täyttää myös valinnaisaineiden arvosanat. Valinnaisaineiden arvosanat merkitään vain mikäli olet opiskellut niitä vähintään kaksi vuosiviikkotuntia perusopetuksen vuosiluokkien 7-9 aikana."
                                                              :sv "Ange vitsorden enligt ditt avgångsbetyg från den grundläggande utbildningen. Du kan ange höjda vitsord, om du har fått ett officiellt intyg över höjningen. Om du har avlagt gymnasiets lärokurs, kan du inte söka med avgångsbetyget över grundläggande utbildning. Du kan inte söka enligt en yrkesinriktad grundexamen. Läroanstalterna kontrollerar betygen för de sökande som godkänns till utbildning.\n\nOBS! Om du söker med den grundläggande utbildningens avgångsbetyg ska du komma ihåg att fylla i vitsorden över valfria ämnen. Ange ändå vitsorden bara om du studerat ämnet i minst två årsveckotimmar under årsklasser 7-9."
                                                              :en "Merkitse arvosanat sitä todistuksesta, jolla haet koulutukseen. Korotetut arvosanat voit merkitä, mikäli olet saanut korotuksista virallisen todistuksen. Jos olet suorittanut lukion oppimäärän, et voi hakea perusopetuksen päättötodistuksella. Ammatillisella perustutkinnolla et voi hakea. Oppilaitokset tarkistavat todistukset hyväksytyksi tulleilta hakijoilta.\n\nHuom! Jos haet perusopetuksen päättötodistuksella, muista täyttää myös valinnaisaineiden arvosanat. Valinnaisaineiden arvosanat merkitään vain mikäli olet opiskellut niitä vähintään kaksi vuosiviikkotuntia perusopetuksen vuosiluokkien 7-9 aikana."}
   :show-hakukierros-paattynyt                               {:fi "Näytä haut joissa hakukierros päättynyt"
                                                              :sv "Visa ansökningar där ansökningsperioden har avslutats"
                                                              :en "Näytä haut joissa hakukierros päättynyt"}
   :hide-hakukierros-paattynyt                               {:fi "Piilota haut joissa hakukierros päättynyt"
                                                              :sv "Dölj ansökningar där ansökningsperioden har avslutats"
                                                              :en "Piilota haut joissa hakukierros päättynyt"}
   :active                                                   {:fi "Aktiivinen"
                                                              :sv "Aktiv"
                                                              :en "Active"}
   :archived                                                 {:fi "Arkistoitu"
                                                              :sv "Arkiverad"
                                                              :en "Archived"}
   :add                                                      {:fi "Lisää"
                                                              :sv "Lägg till"
                                                              :en "Add more"}
   :adjacent-fieldset                                        {:fi "Vierekkäiset tekstikentät"
                                                              :sv "Parallella textfält"
                                                              :en "Parallel text areas"}
   :all                                                      {:fi "Kaikki"
                                                              :sv "Alla"
                                                              :en "All"}
   :all-hakukohteet                                          {:fi "Kaikki hakukohteet"
                                                              :sv "Alla ansökningsmål"
                                                              :en "All study programmes"}
   :allow-invalid-koodis                                     {:fi "Sisällytä päättyneet koodit"
                                                              :sv "Inbegrip utgångna koder"
                                                              :en "EN: Sisällytä päättyneet koodit"}
   :alphabetically                                           {:fi "Aakkosjärjestyksessä"
                                                              :sv "I alfabetisk ordning"
                                                              :en "In alphabetical order"}
   :answers                                                  {:fi "vastausta:"
                                                              :sv "svar:"
                                                              :en "answers:"}
   :applicant                                                {:fi "Hakija"
                                                              :sv "Sökande"
                                                              :en "Applicant"}
   :applicant-will-receive-following-email                   {:fi "Hakija saa allaolevan viestin sähköpostilla hakemuksen lähettämisen jälkeen lähettäjältä"
                                                              :sv "Sökande får nedanstående meddelande i sin e-post efter att hen har skickat sin ansökan"
                                                              :en "EN: Hakija saa allaolevan viestin sähköpostilla hakemuksen lähettämisen jälkeen lähettäjältä"}
   :application                                              {:fi "hakemus"
                                                              :sv "ansökan"
                                                              :en "Application"}
   :application-oid-here                                     {:fi "Tähän tulee hakemusnumero, hakutoiveet, puuttuvat liitepyynnöt ja muokkauslinkki"
                                                              :sv "Här visas ansökningsnummer, ansökningsönskemål, begäran om bilagor som saknas och bearbetningslänken"
                                                              :en "EN: Tähän tulee hakemusnumero, hakutoiveet, puuttuvat liitepyynnöt ja muokkauslinkki"}
   :application-options                                      {:fi "hakukohdetta"
                                                              :sv "ansökningsmål"
                                                              :en "study programme"}
   :application-received                                     {:fi "Hakemus vastaanotettu"
                                                              :sv "Ansökan har mottagits"
                                                              :en "Application submitted"}
   :application-state                                        {:fi "Hakemuksen tila"
                                                              :sv "Ansökans status"
                                                              :en "Status of application"}
   :applications                                             {:fi "hakemusta"
                                                              :sv "ansökningar"
                                                              :en "application"}
   :view-applications                                        {:fi "Näytä oppijan hakemukset"
                                                              :sv "Visa sökandes ansökningar"
                                                              :en "Show applications"}
   :valintojen-toteuttaminen                                 {:fi "Valintojen toteuttaminen"
                                                              :sv "Förverkligandet av antagningar"
                                                              :en "Valintojen toteuttaminen"}
   :applications-panel                                       {:fi "Hakemukset"
                                                              :sv "Ansökningar"
                                                              :en "Applications"}
   :asiointikieli                                            {:fi "Asiointikieli"
                                                              :sv "Kontaktspråk"
                                                              :en "Language of communication"}
   :attachment                                               {:fi "Liitepyyntö"
                                                              :sv "Begäran om bilagor"
                                                              :en "Attachment"}
   :attachment-info-text                                     {:fi "Liitepyyntö sisältää ohjetekstin"
                                                              :sv "Begäran om bilagor innehåller anvisningar"
                                                              :en "EN: Liitepyyntö sisältää ohjetekstin"}
   :fetch-info-from-kouta                                    {:fi "Haetaan osoitetiedot ja palautuspäivämäärä koulutustarjonnasta"
                                                              :sv "SV: Haetaan osoitetiedot ja palautuspäivämäärä koulutustarjonnasta"
                                                              :en "EN: Haetaan osoitetiedot ja palautuspäivämäärä koulutustarjonnasta"}
   :attachment-type                                          {:fi "Liitetiedoston tyyppi"
                                                              :sv "SV: Liitetiedoston tyyppi"
                                                              :en "EN: Liitetiedoston tyyppi"}
   :mail-attachment-text                                     {:fi "Postitettava liitepyyntö"
                                                              :sv "Begäran om bilagor som kan postas"
                                                              :en "EN: Postitettava liitepyyntö"}
   :attachment-name                                          {:fi "Liitteen nimi"
                                                              :sv "Bilagans namn"
                                                              :en "Title of an attachment"}
   :attachment-deadline                                      {:fi "Valinnainen toimituspäivämäärä (pp.kk.vvvv hh:mm)"
                                                              :sv "Valfri leveransdag (pp.kk.vvvv hh:mm)"
                                                              :en "EN: Valinnainen toimituspäivämäärä (pp.kk.vvvv hh:mm)"}
   :attachments                                              {:fi "Liitepyynnöt"
                                                              :sv "Begäran om bilagor"
                                                              :en "Attachments"}
   :auto-expand-hakukohteet                                  {:fi "Näytä hakukohteet hakukohdekohtaisissa kysymyksissä"
                                                              :sv "SV: Näytä hakukohteet hakukohdekohtaisissa kysymyksissä"
                                                              :en "EN: Näytä hakukohteet hakukohdekohtaisissa kysymyksissä"}
   :order-hakukohteet-by-opetuskieli                         {:fi "Järjestä hakukohteet opetuskielen mukaan"
                                                              :sv "SV: Järjestä hakukohteet opetuskielen mukaan"
                                                              :en "EN: Järjestä hakukohteet opetuskielen mukaan"}
   :properties                                               {:fi "Yleiset asetukset"
                                                              :sv "SV: Yleiset asetukset"
                                                              :en "EN: Yleiset asetukset"}
   :demo-link                                                {:fi "Avaa demolomake"
                                                              :sv "Öppna demoblanketten"
                                                              :en "EN: Avaa demolomake"}
   :demo-validity-start                                      {:fi "Demon voimassaolo alkaa"
                                                              :sv "Demoversionen är giltig från och med"
                                                              :en "EN: Demon voimassaolo alkaa"}
   :demo-validity-end                                        {:fi "Demon voimassaolo päättyy"
                                                              :sv "Demoversionen avslutas"
                                                              :en "EN: Demon voimassaolo päättyy"}
   :only-yhteishaku                                          {:fi "Lomake on sallittu vain yhteishauille"
                                                              :sv "SV: Lomake on sallittu vain yhteishauille"
                                                              :en "EN: Lomake on sallittu vain yhteishauille"}
   :hakeminen-tunnistautuneena-allowed-on-form               {:fi "Lomakkeella voi hakea tunnistautuneena"
                                                              :sv "SV: Lomakkeella voi hakea tunnistautuneena"
                                                              :en "EN: Lomakkeella voi hakea tunnistautuneena"}
   :lomakkeeseen-liittyy-maksutoiminto                       {:fi "Lomakkeeseen liittyy maksutoiminto"}
   :maksutyyppi-tutu-radio                                   {:fi "TUTU (käsittelymaksu ja päätösmaksu)"}
   :kasittelymaksu-input                                     {:fi "Käsittelymaksu (€) *"}
   :maksutyyppi-astu-radio                                   {:fi "ASTU (päätösmaksu)"}
   :vat-input                                                {:fi "ALV % *"}
   :order-id-prefix-input                                    {:fi "Viitteet *"}
   :close-form                                               {:fi "Sulje lomake"
                                                              :sv "Stänga blanketten"
                                                              :en "Close form"}
   :submitted-content-search-placeholder                     {:fi "Hae kysymyksellä tai liitepyynnöllä..."
                                                              :sv "Sök enligt fråga eller begäran om bilaga..."
                                                              :en "EN: Hae kysymyksellä tai liitepyynnöllä..."}
   :submitted-content-search-label                           {:fi "Kysymys / liitepyyntö"
                                                              :sv "Fråga / begäran om bilaga"
                                                              :en "EN: Kysymys / liitepyyntö"}
   :base-education                                           {:fi "Pohjakoulutus"
                                                              :sv "Grundutbildning"
                                                              :en "Education background"}
   :base-education-module                                    {:fi "Pohjakoulutusmoduuli"
                                                              :sv "Grundutbildningsmodul"
                                                              :en "EN: Pohjakoulutusmoduuli"}
   :base-education-module-2nd                                {:fi "Pohjakoulutusmoduuli (peruskoulu)"
                                                              :sv "Grundutbildningsmodul (grundskolan)"
                                                              :en "EN: Pohjakoulutusmoduuli (peruskoulu)"}
   :base-education-continuous-admission                      {:fi "Pohjakoulutusmoduuli (jatkuva haku)"
                                                              :sv "Grundutbildningsmodul (kontinuerlig ansökan)"
                                                              :en "EN: Pohjakoulutusmoduuli (jatkuva haku)"}
   :cannot-display-file-type-in-attachment-skimming          {:fi "Tätä liitettä ei valitettavasti voida näyttää esikatselussa, mutta voit ladata sen tästä tiedostona."
                                                              :sv "Denna bilaga kan tyvärr inte visas i förhandsgranskningen, men du kan ladda ner bilagan som en fil."
                                                              :en "This attachment can't unfortunately be shown but you can download it here."}
   :partial-preview-in-attachment-skimming                   {:fi "Voit ladata koko liitteen tästä. Esikatselussa näytetään sivut "
                                                              :sv "Du kan ladda ner hela bilaga här. I förhandsvisningen visas sidorna "
                                                              :en "Voit ladata koko liitteen tästä. Esikatselussa näytetään sivut (en) "}
   :change                                                   {:fi "Muuta"
                                                              :sv "Byt"
                                                              :en "Change"}
   :change-organization                                      {:fi "Vaihda organisaatio"
                                                              :sv "Byt organisation"
                                                              :en "Change the organization"}
   :changed                                                  {:fi "muutti"
                                                              :sv "ändrades av"
                                                              :en "changed"}
   :changes                                                  {:fi "muutosta"
                                                              :sv "ändringar"
                                                              :en "changes"}
   :checking                                                 {:fi "Tarkastetaan"
                                                              :sv "Kontrolleras"
                                                              :en "Inspecting"}
   :choose-user-rights                                       {:fi "Valitse käyttäjän oikeudet"
                                                              :sv "Välj användarrättigheter"
                                                              :en "Choose the users access"}
   :close                                                    {:fi "sulje"
                                                              :sv "stäng"
                                                              :en "close"}
   :collapse-info-text                                       {:fi "Pienennä pitkä ohjeteksti"
                                                              :sv "Visa mindre av långa anvisningar"
                                                              :en "EN: Pienennä pitkä ohjeteksti"}
   :show-for-identified-info-text                            {:fi "Näytä vain tunnistautuneille hakijoille"
                                                              :sv "SV: Näytä vain tunnistautuneille hakijoille"
                                                              :en "EN: Näytä vain tunnistautuneille hakijoille"}
   :compare                                                  {:fi "Vertaile"
                                                              :sv "Jämför"
                                                              :en "Compare"}
   :confirm-change                                           {:fi "Vahvista muutos"
                                                              :sv "Bekräfta ändringen"
                                                              :en "Confirm the change"}
   :confirm-delete                                           {:fi "Vahvista poisto"
                                                              :sv "Bekräfta raderingen"
                                                              :en "Confirm the deletion"}
   :cancel-remove                                            {:fi "Älä poista"
                                                              :sv "Radera inte"
                                                              :en "Cancel remove"}
   :confirm-cut                                              {:fi "Vahvista leikkaus"
                                                              :sv "Bekräfta utklippning"
                                                              :en "Confirm the cut"}
   :cancel-cut                                               {:fi "Älä leikkaa"
                                                              :sv "Klipp inte ut"
                                                              :en "Cancel cut"}
   :cancel-copy                                              {:fi "Älä kopio"
                                                              :sv "Kopiera inte"
                                                              :en "Cancel copy"}
   :applicant-email                                          {:fi "Lähetä sähköposti %d hakijalle"
                                                              :sv "Send %d applicant e-mail"
                                                              :en "Send %d applicant e-mail"}
   :send-update-link                                         {:fi "Viestin mukana lähetetään hakemuksen muokkauslinkki hakijalle"
                                                              :sv "SV: Viestin mukana lähetetään hakemuksen muokkauslinkki hakijalle"
                                                              :en "EN: Viestin mukana lähetetään hakemuksen muokkauslinkki hakijalle"}
   :guardian-email                                           {:fi "Lähetä sähköposti huoltajille"
                                                              :sv "Send guardian e-mail"
                                                              :en "Send guardian e-mail"}
   :only-selected-hakukohteet                                {:fi "vain valituille hakukohteille"
                                                              :sv "till valda ansökningsmål"
                                                              :en "only selected study programmes"}
   :confirmation-sent                                        {:fi "Vahvistussähköposti lähetetty hakijalle"
                                                              :sv "E-post med bekräftelse har skickats till sökande"
                                                              :en "Confirmation email has been sent"}
   :contains-fields                                          {:fi "Sisältää kentät:"
                                                              :sv "Innehåller fälten:"
                                                              :en "Includes the areas:"}
   :copy-form                                                {:fi "Kopioi lomake"
                                                              :sv "Kopiera blanketten"
                                                              :en "Copy the form"}
   :form-contains-applications?                              {:fi "Lomakkeella on hakemuksia"
                                                              :sv "Innehåller ansökningar"
                                                              :en "Form contains applications"}
   :cut-element                                              {:fi "Leikkaa"
                                                              :sv "Klipp ut"
                                                              :en "Cut"}
   :paste-element                                            {:fi "Liitä"
                                                              :sv "Klistra in"
                                                              :en "Paste"}
   :copy-element                                             {:fi "Kopioi"
                                                              :sv "Kopiera"
                                                              :en "Copy"}
   :copy-answer-id                                           {:fi "Kopioi vastauksen tunniste leikepöydälle"
                                                              :sv "Kopiera svarstaggen till klippbordet"
                                                              :en "EN: Kopioi vastauksen tunniste leikepöydälle"}
   :copy-question-id                                         {:fi "Kopioi kysymyksen tunniste leikepöydälle"
                                                              :sv "Kopiera svarstaggen till klippbordet"
                                                              :en "EN: Kopioi kysymyksen tunniste leikepöydälle"}
   :created-by                                               {:fi "Luonut"
                                                              :sv "Grundad av"
                                                              :en "Created by"}
   :custom-choice-label                                      {:fi "Omat vastausvaihtoehdot"
                                                              :sv "Egna svarsalternativ"
                                                              :en "Own answer options"}
   :decimals                                                 {:fi "desimaalia"
                                                              :sv "decimaler"
                                                              :en "decimals"}
   :delete-form                                              {:fi "Poista lomake"
                                                              :sv "Ta bort blanketten"
                                                              :en "Delete the form"}
   :cancel-form-delete                                       {:fi "Älä poista"
                                                              :sv "Radera inte"
                                                              :en "Don't remove"}
   :did                                                      {:fi "teki"
                                                              :sv "har gjort"
                                                              :en "has made"}
   :diff-from-changes                                        {:fi "Vertailu muutoksesta"
                                                              :sv "Jämför ändringen"
                                                              :en "Compare the change"}
   :diff-added                                               {:fi "Lisätty"
                                                              :sv "Lagts till"
                                                              :en "Added"}
   :diff-removed                                             {:fi "Poistettu"
                                                              :sv "Raderats"
                                                              :en "Removed"}
   :dropdown                                                 {:fi "Pudotusvalikko"
                                                              :sv "Rullgardinsmeny"
                                                              :en "Dropdown"}
   :dropdown-koodisto                                        {:fi "Pudotusvalikko, koodisto"
                                                              :sv "Rullgardinsmeny, kodregister"
                                                              :en "Dropdown, codes"}
   :edit-application                                         {:fi "Muokkaa hakemusta"
                                                              :sv "Bearbeta ansökan"
                                                              :en "Edit the application"}
   :edit-application-with-rewrite                            {:fi "Muokkaa hakemusta rekisterinpitäjänä"
                                                              :sv "Bearbeta ansökan som registerförare"
                                                              :en "Muokkaa hakemusta rekisterinpitäjänä"}
   :edit-applications-rights-panel                           {:fi "Hakemusten arviointi"
                                                              :sv "Utvärdering av ansökningar"
                                                              :en "Evaluation of applications"}
   :edit-valinta-rights-panel                                {:fi "Valinnan tuloksen muokkaus"
                                                              :sv "Bearbeta antagningsresultat"
                                                              :en "EN: Valinnan tuloksen muokkaus"}
   :edit-email-templates                                     {:fi "Muokkaa sähköpostipohjia"
                                                              :sv "Bearbeta e-postmallar"
                                                              :en "Edit the email templates"}
   :edit-link-sent-automatically                             {:fi "Muokkauslinkki lähtee viestin mukana automaattisesti hakijoille"
                                                              :sv "Bearbetningslänken skickas automatiskt med meddelandet"
                                                              :en "The edit link will be sent automatically to applicants"}
   :editable-content-beginning                               {:fi "Muokattava osuus (viestin alku)"
                                                              :sv "Del som ska bearbetas (början av meddelandet)"
                                                              :en "EN: Muokattava osuus (viestin alku)"}
   :editable-content-ending                                  {:fi "Muokattava osuus (viestin loppu)"
                                                              :sv "Del som ska bearbetas (slutet av meddelandet)"
                                                              :en "EN: Muokattava osuus (viestin loppu)"}
   :editable-content-title                                   {:fi "Muokattava osuus (otsikko)"
                                                              :sv "Del som ska bearbetas (rubrik)"
                                                              :en "EN: Muokattava osuus (otsikko)"}
   :editable-signature                                       {:fi "Muokattava osuus (allekirjoitus)"
                                                              :sv "Del som ska bearbetas (underteckning)"
                                                              :en "EN: Muokattava osuus (allekirjoitus)"}
   :ehdollisuus                                              {:fi "Ehdollisuus"
                                                              :sv "Villkorlighet"
                                                              :en "EN: Ehdollisuus"}
   :ehdollisesti-hyvaksyttavissa                             {:fi "Ehdollinen"
                                                              :sv "Villkorlig"
                                                              :en "EN: Ehdollinen"}
   :ei-ehdollisesti-hyvaksyttavissa                          {:fi "Ei ehdollinen"
                                                              :sv "Icke-villkorlig"
                                                              :en "EN: Ei ehdollinen"}
   :eligibility                                              {:fi "Hakukelpoisuus:"
                                                              :sv "Ansökningsbehörighet:"
                                                              :en "Criteria for eligibility"}
   :eligibility-explanation                                  {:fi "Kelpoisuusmerkinnän selite"
                                                              :sv "Förklaring till behörighetsanteckningen"
                                                              :en "Explanation of eligibility"}
   :eligibility-set-automatically                            {:fi "Hakukelpoisuus asetettu automaattisesti"
                                                              :sv "Ansökningsbehörigheten har satts automatiskt"
                                                              :en "Eligibility set automatically"}
   :other-application-info                                   {:fi "Muut hakemustiedot*"
                                                              :sv "Annan information om ansökningen*"
                                                              :en "Other application information*"}
   :applicants-school-of-departure                           {:fi "Lähtökoulu"
                                                              :sv "Avgångsskola"
                                                              :en "School of departure"}
   :applicants-classes                                       {:fi "Luokka"
                                                              :sv "Klass"
                                                              :en "Class"}
   :valpas-hakutilanne-link-text-1                           {:fi "*Ei hakeneita voi katsoa "
                                                              :sv "De som inte sökt hittas i "
                                                              :en "Non-applicants can be viewed in "}
   :valpas-hakutilanne-link-text-2                           {:fi "Valpas-palvelusta"
                                                              :sv "Valpas-tjänsten"
                                                              :en "Valpas service"}
   :only-harkinnanvaraiset                                   {:fi "Vain harkinnanvaraiset"
                                                              :sv "Endast antagning enlig prövning"
                                                              :en "EN: Vain harkinnanvaraiset"}
   :guardian-contact-information                             {:fi "Huoltajan yhteystiedot"
                                                              :sv "Vårdnadshavarens kontaktuppgifter"
                                                              :en "Guardians contact information"}
   :payment-obligation                                       {:fi "Lukuvuosimaksuvelvollisuus"
                                                              :sv "Betalningsskyldighet"
                                                              :en "Obligated to pay"}
   :payment-obligation-set-automatically                     {:fi "Maksuvelvollisuus asetettu automaattisesti"
                                                              :sv "Betalningsskyldighet har ställts automatiskt"
                                                              :en "Payment obligation set automatically"}
   :email-content                                            {:fi "Sähköpostiviestin sisältö"
                                                              :sv "E-postmeddelandets innehåll"
                                                              :en "Content of the email"}
   :empty-option                                             {:fi "Ei vastausta"
                                                              :sv "Inget svar"
                                                              :en "No answer"}
   :english                                                  {:fi "Englanti"
                                                              :sv "Engelska"
                                                              :en "English"}
   :ensisijaisesti                                           {:fi "Hakenut ensisijaisesti"
                                                              :sv "Sökt i förstahand"
                                                              :en "First priority applicants"}
   :ensisijaisuus                                            {:fi "Ensisijaisuus"
                                                              :sv "I förstahand"
                                                              :en "First priority"}
   :error                                                    {:fi "Virhe"
                                                              :sv "Fel"
                                                              :en "Error"}
   :events                                                   {:fi "Tapahtumat"
                                                              :sv "Händelser"
                                                              :en "Events"}
   :filter-applications                                      {:fi "Rajaa hakemuksia"
                                                              :sv "Avgränsa ansökningar"
                                                              :en "Filter the applications"}
   :filter-by-state                                          {:fi "Rajaa tilan mukaan"
                                                              :sv "Avgränsa enligt status"
                                                              :en "EN: Rajaa tilan mukaan"}
   :filters-apply-button                                     {:fi "Ota käyttöön"
                                                              :sv "Använd"
                                                              :en "Apply"}
   :filters-cancel-button                                    {:fi "Peruuta"
                                                              :sv "Annullera"
                                                              :en "Cancel"}
   :finnish                                                  {:fi "Suomi"
                                                              :sv "Finska"
                                                              :en "Finnish"}
   :followups                                                {:fi "Lisäkysymykset"
                                                              :sv "Tilläggsfrågor"
                                                              :en "Extra questions"}
   :for-hakukohde                                            {:fi "hakukohteelle"
                                                              :sv "för ansökningsmålet"
                                                              :en "for study programme"}
   :form                                                     {:fi "Lomake"
                                                              :sv "Blankett"
                                                              :en "Form"}
   :form-edit-rights-panel                                   {:fi "Lomakkeiden muokkaus"
                                                              :sv "Bearbetning av blanketter"
                                                              :en "Form editing"}
   :form-locked                                              {:fi "Lomakkeen muokkaus on estetty"
                                                              :sv "Du kan inte bearbeta blanketten"
                                                              :en "You can't edit the form anymore"}
   :form-name                                                {:fi "Lomakkeen nimi"
                                                              :sv "Blankettens namn"
                                                              :en "Name of the application form"}
   :form-section                                             {:fi "Lomakeosio"
                                                              :sv "Blankettdel"
                                                              :en "Section"}
   :form-outdated                                            {:fi "Lomakkeesta on uudempi versio!"
                                                              :sv "Det finns en ny version av sökandens blankett!"
                                                              :en "There is a new version of the application"}
   :show-newest-version                                      {:fi "Näytä lomake uusimmalla versiolla"
                                                              :sv "Visa blanketten i nyaste version"
                                                              :en "Show the latest version of the form"}
   :forms                                                    {:fi "Lomakkeet"
                                                              :sv "Blanketter"
                                                              :en "Applications"}
   :forms-panel                                              {:fi "Lomakkeet"
                                                              :sv "Blanketter"
                                                              :en "Applications"}
   :from-applicant                                           {:fi "Hakijalta"
                                                              :sv "Av sökande"
                                                              :en "From applicant"}
   :from-state                                               {:fi "Tilasta"
                                                              :sv "Från behandlingsskede"
                                                              :en "From processing status"}
   :group                                                    {:fi "ryhmä"
                                                              :sv "grupp"
                                                              :en "Group"}
   :group-header                                             {:fi "Kysymysryhmän otsikko"
                                                              :sv "Rubrik för frågegrupp"
                                                              :en "Header of the question group"}
   :hakukohde-info                                           {:fi "Tässä hakija voi valita hakukohteet. Hakukohteiden määrä ja priorisointi määritetään haun asetuksissa."
                                                              :sv "Sökande kan här välja ansökningsmål. Antalet ansökningsmål och prioriteringen definieras i inställningarna för ansökan."
                                                              :en "EN: Tässä hakija voi valita hakukohteet. Hakukohteiden määrä ja priorisointi määritetään haun asetuksissa."}
   :hakukohteet                                              {:fi "Hakukohteet"
                                                              :sv "Ansökningsmål"
                                                              :en "Study programmes"}
   :hakukohderyhmat                                          {:fi "Hakukohderyhmät"
                                                              :sv "Ansökningsmålsgrupp"
                                                              :en "Study programme groups"}
   :harkinnanvaraisuus                                       {:fi "Harkinnanvaraisuus"
                                                              :sv "Antagning enligt prövning"
                                                              :en "EN: Harkinnanvaraisuus"}
   :search-hakukohde-placeholder                             {:fi "Etsi hakukohteita"
                                                              :sv "Sök ansökningsmål"
                                                              :en "Search for study programmes"}
   :search-hakukohde-and-hakukohderyhma-placeholder          {:fi "Etsi hakukohteita ja hakukohderyhmiä"
                                                              :sv "Sök ansökningsmål och ansökningsmålsgrupper"
                                                              :en "Search for study programmes and study programme groups"}
   :handling-notes                                           {:fi "Käsittelymerkinnät"
                                                              :sv "Anteckningar om behandling"
                                                              :en "Notes"}
   :hide-options                                             {:fi "Sulje vastausvaihtoehdot"
                                                              :sv "Stäng svarsalternativen"
                                                              :en "Hide the options"}
   :identified                                               {:fi "Yksilöidyt"
                                                              :sv "Identifierade"
                                                              :en "Identified"}
   :identifying                                              {:fi "Yksilöinti"
                                                              :sv "Identifiering"
                                                              :en "Identifying"}
   :incomplete                                               {:fi "Kesken"
                                                              :sv "Inte färdig"
                                                              :en "Incomplete"}
   :ineligibility-reason                                     {:fi "Hylkäyksen syy"
                                                              :sv "Orsak till avslag"
                                                              :en "Reason for ineligibility"}
   :info-addon                                               {:fi "Kysymys sisältää ohjetekstin"
                                                              :sv "Frågan innehåller anvisningar"
                                                              :en "EN: Kysymys sisältää ohjetekstin"}
   :info-element                                             {:fi "Infoteksti"
                                                              :sv "Infotext"
                                                              :en "Info element"}
   :modal-info-element                                       {:fi "Infoteksti, koko ruutu"
                                                              :sv "SV: Infoteksti, koko ruutu"
                                                              :en "EN: Infoteksti, koko ruutu"}
   :information-request-sent                                 {:fi "Täydennyspyyntö lähetetty"
                                                              :sv "Begäran om komplettering har skickats"
                                                              :en "Information request email has been sent"}
   :single-information-request-sent                          {:fi "Viesti lähetetty"
                                                              :sv "Meddelandet har skickats"
                                                              :en "Message has been sent"}
   :mass-information-request-sent                            {:fi "Viesti lähetetty"
                                                              :sv "Meddelandet har skickats"
                                                              :en "Message has been sent"}
   :integer                                                  {:fi "kokonaisluku"
                                                              :sv "heltal"
                                                              :en "integer"}
   :kk-base-education-module                                 {:fi "Pohjakoulutusmoduuli (korkeakoulut)"
                                                              :sv "Grundutbildningsmodul (Gea till högskolor)"
                                                              :en "EN: Pohjakoulutusmoduuli (kk-yhteishaku)"}
   :koodisto                                                 {:fi "Koodisto"
                                                              :sv "Kodregister"
                                                              :en "Codes"}
   :koulutusmarkkinointilupa                                 {:fi "Koulutusmarkkinointilupa"
                                                              :sv "Tillstånd för utbildningsmarknadsföring"
                                                              :en "EN: Koulutusmarkkinointilupa"}
   :last-modified                                            {:fi "Viimeksi muokattu"
                                                              :sv "Senast bearbetad"
                                                              :en "Modified last"}
   :last-modified-by                                         {:fi "viimeksi muokannut"
                                                              :sv "Senast bearbetad av"
                                                              :en "Last modified by"}
   :link-to-form                                             {:fi "Linkki lomakkeeseen"
                                                              :sv "Länk till blanketten"
                                                              :en "Link to the form"}
   :link-to-feedback                                         {:fi "Linkki palautteeseen"
                                                              :sv "Länk till responsen"
                                                              :en "Link to the feedback"}
   :link-to-applications                                     {:fi "Linkki hakemuksiin"
                                                              :sv "Länk till ansökningar"
                                                              :en "Link to applications"}
   :show-results                                             {:fi "Näytä hakemukset"
                                                              :sv "Visa ansökningar"
                                                              :en "Show applications"}
   :load-excel                                               {:fi "Lataa Excel"
                                                              :sv "Ladda ner Excel"
                                                              :en "Load excel"}
   :load-attachments                                         {:fi "Lataa liitteet"
                                                              :sv "Ladda ner bilagor"
                                                              :en "Load attachments"}
   :load-attachment-in-skimming                              {:fi "lataa"
                                                              :sv "ladda"
                                                              :en "download"}
   :select-all                                               {:fi "Valitse kaikki"
                                                              :sv "Välj alla"
                                                              :en "Select all"}
   :lock-form                                                {:fi "Lukitse lomake"
                                                              :sv "Lås blanketten"
                                                              :en "Lock the form"}
   :logout                                                   {:fi "Kirjaudu ulos"
                                                              :sv "Logga ut"
                                                              :en "Log out"}
   :lupa-sahkoiseen-asiointiin                               {:fi "Sähköisen asioinnin lupa"
                                                              :sv "Tillstånd för elektronisk kontakt"
                                                              :en "EN: Sähköisen asioinnin lupa"}
   :lupatiedot-kk                                            {:fi "Lupatiedot (korkeakoulutus)"
                                                              :sv "Tillståndsuppgifter (högre utbildning)"
                                                              :en "Permissions (higher education)"}
   :lupatiedot-toinen-aste                                   {:fi "Lupatiedot (2. aste)"
                                                              :sv "Tillståndsuppgifter (2. stadiet)"
                                                              :en "Permissions"}
   :mass-edit                                                {:fi "Massamuutos"
                                                              :sv "Massändring"
                                                              :en "Mass editing"}
   :mass-review-notes                                        {:fi "Massamuistiinpano"
                                                              :sv "SV: Massanteckningar"
                                                              :en "EN: Mass notes"}
   :mass-review-notes-n-applications                         {:fi "Olet lisäämässä muistiinpanoa %d hakijalle"
                                                              :sv "SV: Olet lisäämässä muistiinpanoa %d hakijalle"
                                                              :en "EN: Olet lisäämässä muistiinpanoa %d hakijalle"}
   :mass-review-notes-content                                {:fi "Sisältö"
                                                              :sv "SV: Sisältö"
                                                              :en "EN: Sisältö"}
   :mass-review-notes-confirm-n-applications                 {:fi "Vahvista %s muistiinpanon tallennus"
                                                              :sv "SV: Vahvista %s muistiinpanon tallennus"
                                                              :en "EN: Vahvista %s muistiinpanon tallennus"}
   :mass-review-notes-saving                                 {:fi "Tallennetaan muistiinpanoja..."
                                                              :sv "SV: Tallennetaan muistiinpanoja..."
                                                              :en "EN: Tallennetaan muistiinpanoja..."}
   :mass-review-notes-saved                                  {:fi "Muistiinpanot on tallennettu!"
                                                              :sv "SV: Muistiinpanot on tallennettu!"
                                                              :en "EN: Muistiinpanot on tallennettu!"}
   :mass-review-notes-save-error                             {:fi "Muistiinpanon tallennus epäonnistui!"
                                                              :sv "SV: Muistiinpanon tallennus epäonnistui!"
                                                              :en "EN: Muistiinpanon tallennus epäonnistui!"}
   :excel-request                                            {:fi "Excel"
                                                              :sv "Excel"
                                                              :en "Excel"}
   :excel-included-ids                                       {:fi "Exceliin sisältyvät tunnisteet:"
                                                              :sv "Identifikationer som ingår i excelfilen:"
                                                              :en "Identifiers included in the Excel file:"}
   :excel-include-all-placeholder                            {:fi "Kaikki tunnisteet"
                                                              :sv "Alla identifikationer"
                                                              :en "All identifiers"}
   :excel-kasittelymerkinnat                                 {:fi "Käsittelymerkinnät"
                                                              :sv "Behandlingsmarkeringar"
                                                              :en "Processing notes"}
   :excel-hakemuksen-yleiset-tiedot                          {:fi "Hakemuksen yleiset tiedot"
                                                              :sv "Allmänna uppgifter om ansökan"
                                                              :en "General application information"}
   :excel-mode-ids-only                                      {:fi "Valitse excelin tiedot"
                                                              :sv "Valitse excelin tiedot"
                                                              :en "Valitse excelin tiedot"}
   :excel-mode-with-defaults                                 {:fi "Kirjoita tunnisteet"
                                                              :sv "Kirjoita tunnisteet"
                                                              :en "Kirjoita tunnisteet"}
   :excel-valitse-kaikki                                     {:fi "Valitse kaikki"
                                                              :sv "Valitse kaikki"
                                                              :en "Valitse kaikki"}
   :excel-poista-valinnat                                    {:fi "Poista valinnat"
                                                              :sv "Poista valinnat"
                                                              :en "Poista valinnat"}
   :mass-information-request                                 {:fi "Massaviesti"
                                                              :sv "Massmeddelande"
                                                              :en "Mass message"}
   :mass-information-request-confirm-n-messages              {:fi "Vahvista %s viestin lähetys"
                                                              :sv "Bekräfta att %s meddelanden kommer att skickas"
                                                              :en "Confirm sending %s messages"}
   :mass-information-request-email-n-recipients              {:fi "Lähetä sähköposti %d hakijalle:"
                                                              :sv "Skicka e-post till %d sökande:"
                                                              :en "Send email to %d applicants:"}
   :mass-information-request-messages-sent                   {:fi "Viestit lisätty lähetysjonoon!"
                                                              :sv "Meddelandena har lagts till i utskickskön!"
                                                              :en "Messages have been sent!"}
   :mass-information-request-send                            {:fi "Lähetä"
                                                              :sv "Skicka:"
                                                              :en "Send:"}
   :mass-information-request-sending-messages                {:fi "Käsitellään viestejä..."
                                                              :sv "Meddelanden behandlas..."
                                                              :en "Sending the messages"}
   :mass-information-request-subject                         {:fi "Aihe (max. 120 merkkiä):"
                                                              :sv "Ämne (max. 120 tecken):"
                                                              :en "Subject (max. 120 characters):"}
   :single-information-request                               {:fi "Viesti"
                                                              :sv "Meddelande"
                                                              :en "Message"}
   :single-information-request-subject                       {:fi "Aihe (max. 120 merkkiä):"
                                                              :sv "Ämne (max. 120 tecken):"
                                                              :en "Subject (max. 120 characters):"}
   :single-information-request-vaidation-error-message       {:fi "Aihetekstin maksimipituus on 120 merkkiä, lyhennä aihetta."
                                                              :sv "Den maximala längden på ämnestexten är 120 tecken, förkorta texten."
                                                              :en "Maximum length of subject field is 120 characters, please shorten the subject."}
   :single-information-request-email-applicant               {:fi "Olet lähettämässä sähköpostia 1 hakijalle: %s"
                                                              :sv "Skicka e-post till 1 sökande: "
                                                              :en "Send email to applicants:"}
   :single-information-request-send                          {:fi "Lähetä"
                                                              :sv "Skicka:"
                                                              :en "Send:"}
   :single-information-request-message-sent                  {:fi "Viesti lisätty lähetysjonoon!"
                                                               :sv "Meddelanden har lagts till i utskickskön!"
                                                               :en "Message has been sent!"}
   :max-characters                                           {:fi "Max. merkkimäärä"
                                                              :sv "Max. teckenantal"
                                                              :en "Max. characters"}
   :md-help-bold                                             {:fi "**lihavoitava sisältö**"
                                                              :sv "**innehåll med fetstil**"
                                                              :en "**bold content**"}
   :md-help-cursive                                          {:fi "*kursivoitava sisältö*"
                                                              :sv "*med kursiv stil*"
                                                              :en "*cursive content*"}
   :md-help-link                                             {:fi "[linkin teksti](http://linkin osoite)"
                                                              :sv "[länkens text](http://länkens adress)"
                                                              :en "[link text](http://link address)"}
   :md-help-more                                             {:fi "Lisää muotoiluohjeita"
                                                              :sv "Lägg till anvisningar för utformning"
                                                              :en "More instructions"}
   :md-help-title                                            {:fi "# otsikko (# ylin - ###### alin)"
                                                              :sv "# rubrik (# högsta - ###### lägst)"
                                                              :en "# title (# highest - ###### lowest)"}
   :message-preview                                          {:fi "Viestin esikatselu"
                                                              :sv "Förhandsgranska meddelandet"
                                                              :en "Preview the message"}
   :more-results-refine-search                               {:fi "Lataa lisää tuloksia"
                                                              :sv "Ladda mera"
                                                              :en "Load more results"}
   :multiple-answers                                         {:fi "Vastaaja voi lisätä useita vastauksia"
                                                              :sv "Du kan ge flera svar"
                                                              :en "EN: Vastaaja voi lisätä useita vastauksia"}
   :multiple-choice                                          {:fi "Lista, monta valittavissa"
                                                              :sv "Flervalslista"
                                                              :en "Multiple choice"}
   :multiple-choice-koodisto                                 {:fi "Lista, monta valittavissa, koodisto"
                                                              :sv "Flervalslista, kodregister"
                                                              :en "Multiple choice, codes"}
   :multiple-organizations                                   {:fi "Useita organisaatioita"
                                                              :sv "Flera organisationer"
                                                              :en "Multiple organizations"}
   :new-form                                                 {:fi "Uusi lomake"
                                                              :sv "Ny blankett"
                                                              :en "New form"}
   :no-search-hits                                           {:fi "Ei hakutuloksia"
                                                              :sv "Inga sökresultat"
                                                              :en "EN: Ei hakutuloksia"}
   :no-organization                                          {:fi "Ei organisaatiota"
                                                              :sv "Ingen organisation"
                                                              :en "No organization"}
   :notes                                                    {:fi "Muistiinpanot"
                                                              :sv "Anteckningar"
                                                              :en "Notes"}
   :of-form                                                  {:fi "Lomakkeen"
                                                              :sv "Blankettens"
                                                              :en "Form's"}
   :of-hakukohde                                             {:fi "Hakukohteen"
                                                              :sv "Ansökningsmålets"
                                                              :en "Study programme's"}
   :only-numeric                                             {:fi "Kenttään voi täyttää vain numeroita"
                                                              :sv "Endast siffror i fältet"
                                                              :en "Only numbers"}
   :numeric-range                                            {:fi "Arvoalueen rajaus"
                                                              :sv "Avgränsning av värdeområde"
                                                              :en "Arvoalueen rajaus"}
   :open                                                     {:fi "avaa"
                                                              :sv "öppna"
                                                              :en "Open"}
   :options                                                  {:fi "Vastausvaihtoehdot"
                                                              :sv "Svarsalternativ"
                                                              :en "Options"}
   :passive                                                  {:fi "Passiivinen"
                                                              :sv "Passiv"
                                                              :en "Inactive"}
   :person-completed-education                               {:fi "Henkilön suoritukset"
                                                              :sv "Personens prestationer"
                                                              :en "Applicant's exams in Finland"}
   :grades                                                   {:fi "Arvosanat"
                                                              :sv "Vitsord"
                                                              :en "Grades"}
   :grades-header                                            {:fi "Valinnoissa käytettävät arvosanat"
                                                              :sv "Vitsord i antagningarna"
                                                              :en "EN: Valinnoissa käytettävät arvosanat"}
   :valinnat                                                 {:fi "Valinnat"
                                                              :sv "Antagningar"
                                                              :en "EN: Valinnat"}
   :scores                                                   {:fi "Pisteet"
                                                              :sv "Poäng"
                                                              :en "Scores"}
   :valinnan-kokonaispisteet                                 {:fi "Valinnan kokonaispisteet"
                                                              :sv "Helhetspoäng för antagningen"
                                                              :en "EN: Valinnan kokonaispisteet"}
   :sijoittelun-tulos                                        {:fi "Sijoittelun tulos"
                                                              :sv "Placeringsresultat"
                                                              :en "EN: Sijoittelun tulos"}
   :vastaanottotieto                                         {:fi "Vastaanottotieto"
                                                              :sv "Uppgift om mottagande"
                                                              :en "EN: Vastaanottotieto"}
   :ilmoittautumistila                                       {:fi "Ilmoittautumistila"
                                                              :sv "Uppgift om anmälan"
                                                              :en "EN: Ilmoittautumistila"}
   :valinnan-tulokset-kesken                                 {:fi "Valinnan tulokset kesken"
                                                              :sv "Ansökans resultat på hälft"
                                                              :en "EN: Valinnan tulokset kesken"}
   :person-info-module-onr                                   {:fi "Opiskelijavalinta"
                                                              :en "Opiskelijavalinta"
                                                              :sv "Studerandeantagning"}
   :person-info-module-muu                                   {:fi "Muu käyttö"
                                                              :en "Muu käyttö"
                                                              :sv "Annat bruk"}
   :person-info-module-onr-2nd                               {:fi "Opiskelijavalinta, perusopetuksen jälkeinen yhteishaku"
                                                              :en "Opiskelivalinta, perusopetuksen jälkeinen yhteishaku"
                                                              :sv "Opiskelivalinta, perusopetuksen jälkeinen yhteishaku"}
   :metadata-not-found                                       {:fi "Hakijan liitteitä ei löytynyt"
                                                              :sv "Sökandes bilagor hittades inte"
                                                              :en "Applicant's attachements can't be found"}
   :person-not-individualized                                {:fi "Hakijaa ei ole yksilöity."
                                                              :sv "Sökande har inte identifierats."
                                                              :en "Applicant isn't identified."}
   :individualize-in-henkilopalvelu                          {:fi "Tee yksilöinti henkilöpalvelussa."
                                                              :sv "Identifiera i persontjänsten."
                                                              :en "Identify the applicant."}
   :operation-failed                                         {:fi "Toiminto epäonnistui"
                                                              :sv "Funktionen misslyckades"
                                                              :en "EN: Toiminto epäonnistui"}
   :creating-henkilo-failed                                  {:fi "Henkilön luonti ei ole valmistunut! Tarkista hakemuksen nimitiedot (esim. kutsumanimi on yksi etunimistä)."
                                                              :sv "Att bilda personen är inte färdig. Kontrollera att namnuppgifterna är korrekta (t.ex. att tilltalsnamnet ingår)"
                                                              :en "EN: Henkilön luonti ei ole valmistunut! Tarkista hakemuksen nimitiedot (esim. kutsumanimi on yksi etunimistä)"}
   :henkilo-info-incomplete                                  {:fi "Hakemuksen lataus epäonnistui puuttuvien henkilötietojen vuoksi."
                                                              :sv "Att ladda ner ansökan misslyckades p g a bristfälliga personuppgifter."
                                                              :en "EN: Hakemuksen lataus epäonnistui puuttuvien henkilötietojen vuoksi."}
   :review-in-henkilopalvelu                                 {:fi "Tarkasta henkilön tiedot henkilöpalvelussa."
                                                              :sv "Kontrollera personens uppgifter i persontjänsten."
                                                              :en "EN: Tarkasta henkilön tiedot henkilöpalvelussa."}
   :pohjakoulutus_am                                         {:fi "Suomessa suoritettu ammatillinen perustutkinto, kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto"
                                                              :sv "I Finland avlagd yrkesinriktad grundexamen, examen på skolnivå, institutnivå eller inom yrkesutbildning på högre nivå"
                                                              :en "EN: Suomessa suoritettu ammatillinen perustutkinto, kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto"}
   :pohjakoulutus_amp                                        {:fi "Suomessa suoritettu ammatillinen perustutkinto"
                                                              :sv "Yrkesinriktad grundexamen som avlagts i Finland"
                                                              :en "Vocational upper secondary qualification completed in Finland (ammatillinen perustutkinto)"}
   :pohjakoulutus_amt                                        {:fi "Suomessa suoritettu ammatti- tai erikoisammattitutkinto"
                                                              :sv "Yrkesexamen eller specialyrkesexamen som avlagts i Finland"
                                                              :en "Further or specialist vocational qualification completed in Finland (ammatti- tai erikoisammattitutkinto)"}
   :pohjakoulutus_amv                                        {:en "Vocational upper secondary qualification completed in Finland (kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto)"
                                                              :fi "Suomessa suoritettu kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto"
                                                              :sv "Yrkesinriktad examen på skolnivå, examen på institutsnivå eller yrkesinriktad examen på högre nivå som avlagts i Finland"}
   :pohjakoulutus_avoin                                      {:en "Open university/UAS studies required by the higher education institution"
                                                              :fi "Korkeakoulun edellyttämät avoimen korkeakoulun opinnot"
                                                              :sv "Studier som högskolan kräver vid en öppen högskola"}
   :pohjakoulutus_kk                                         {:fi "Suomessa suoritettu korkeakoulututkinto"
                                                              :sv "Högskoleexamen som avlagts i Finland"
                                                              :en "Bachelor’s/Master’s/Doctoral degree completed in Finland"}
   :pohjakoulutus_kk_ulk                                     {:en "Bachelor’s/Master’s/Doctoral degree completed outside Finland"
                                                              :fi "Muualla kuin Suomessa suoritettu korkeakoulututkinto"
                                                              :sv "Högskoleexamen som avlagts annanstans än i Finland"}
   :pohjakoulutus_lk                                         {:en "General upper secondary school syllabus completed in Finland (lukion oppimäärä ilman ylioppilastutkintoa)"
                                                              :fi "Suomessa suoritettu lukion oppimäärä ilman ylioppilastutkintoa"
                                                              :sv "Gymnasiets lärokurs som avlagts i Finland utan studentexamen"}
   :pohjakoulutus_muu                                        {:en "Other eligibility for higher education"
                                                              :fi "Muu korkeakoulukelpoisuus"
                                                              :sv "Övrig högskolebehörighet"}
   :pohjakoulutus_ulk                                        {:en "Upper secondary education completed outside Finland (general or vocational)"
                                                              :fi "Muualla kuin Suomessa suoritettu muu tutkinto, joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun"
                                                              :sv "Övrig examen som avlagts annanstans än i Finland, och ger behörighet för högskolestudier i ifrågavarande land"}
   :pohjakoulutus_yo                                         {:fi "Suomessa suoritettu ylioppilastutkinto"
                                                              :sv "Studentexamen som avlagts i Finland"
                                                              :en "Matriculation examination completed in Finland"}
   :pohjakoulutus_yo_ammatillinen                            {:en "Upper secondary double degree completed in Finland (kaksoistutkinto)",
                                                              :fi "Suomessa suoritettu kaksoistutkinto (ammatillinen perustutkinto ja ylioppilastutkinto)",
                                                              :sv "Dubbelexamen som avlagts i Finland"}
   :pohjakoulutus_yo_kansainvalinen_suomessa                 {:en "International matriculation examination completed in Finland (IB, EB and RP/DIA)"
                                                              :fi "Suomessa suoritettu kansainvälinen ylioppilastutkinto (IB, EB ja RP/DIA)"
                                                              :sv "Internationell studentexamen som avlagts i Finland (IB, EB och RP/DIA)"}
   :pohjakoulutus_yo_ulkomainen                              {:en "International matriculation examination completed outside Finland (IB, EB and RP/DIA)"
                                                              :fi "Muualla kuin Suomessa suoritettu kansainvälinen ylioppilastutkinto (IB, EB ja RP/DIA)"
                                                              :sv "Internationell studentexamen som avlagts annanstans än i Finland (IB, EB och RP/DIA)"}
   :pohjakoulutusristiriita                                  {:fi "Pohjakoulutusristiriita"
                                                              :sv "Motstridighet i grundutbildningen"
                                                              :en "EN: Pohjakoulutusristiriita"}
   :points                                                   {:fi "Pisteet"
                                                              :sv "Poäng"
                                                              :en "Points"}
   :processed-haut                                           {:fi "Käsitellyt haut"
                                                              :sv "Behandlade ansökningar"
                                                              :en "Processed admissions"}
   :processing-state                                         {:fi "Käsittelyvaihe"
                                                              :sv "Behandlingsskede"
                                                              :en "State of processing"}
   :question                                                 {:fi "Kysymys"
                                                              :sv "Fråga"
                                                              :en "Question"}
   :question-group                                           {:fi "Kysymysryhmä"
                                                              :sv "Frågegrupp"
                                                              :en "Question group"}
   :question-group-tutkinto                                  {:fi "Kysymysryhmä (tutkintokokonaisuus)"
                                                              :sv "SV: Kysymysryhmä (tutkintokokonaisuus)"
                                                              :en "EN: Kysymysryhmä (tutkintokokonaisuus)"}
   :receiver                                                 {:fi "Vastaanottaja:"
                                                              :sv "Mottagare:"
                                                              :en "Receiver:"}
   :rejection-reason                                         {:fi "Hylkäyksen syy"
                                                              :sv "Orsak till avslag"
                                                              :en "Reason for rejection"}
   :remove                                                   {:fi "Poista"
                                                              :sv "Radera"
                                                              :en "Delete"}
   :remove-filters                                           {:fi "Poista rajaimet"
                                                              :sv "Ta bort avgränsningar"
                                                              :en "Remove the filters"}
   :remove-lock                                              {:fi "Poista lukitus"
                                                              :sv "Öppna låset"
                                                              :en "Unlock the form"}
   :selection-limit                                          {:fi "Rajoitettu valinta"
                                                              :sv "Begränsat urval"
                                                              :en "Limited selection"}
   :selection-limit-input                                    {:fi "Raja-arvo"
                                                              :sv "Gränsvärde"
                                                              :en "Limit"}
   :required                                                 {:fi "Pakollinen tieto"
                                                              :sv "Obligatorisk uppgift"
                                                              :en "Mandatory information"}
   :sensitive-answer                                         {:fi "Tämän kysymyksen vastaus piilotetetaan hakijalta muokkauslinkistä"
                                                              :sv "SV: Tämän kysymyksen vastaus piilotetetaan hakijalta muokkauslinkistä"
                                                              :en "EN: Tämän kysymyksen vastaus piilotetetaan hakijalta muokkauslinkistä"}
   :per-hakukohde                                            {:fi "Vastaukset ovat hakukohdekohtaisia"
                                                              :sv "Svaren ges enligt ansökningsmål"
                                                              :en "Answers are specific to application options"}
   :invalid-date-format                                      {:fi "Päivämäärän tulee olla muotoa p.k.vvvv"
                                                              :sv "Ange datumet i formen p.k.vvvv"
                                                              :en "EN: Päivämäärän tulee olla muotoa d.m.yyyy"}
   :invalid-time-format                                      {:fi "Ajan tulee olla muotoa t.mm"
                                                              :sv "Ange tiden i formen h.mm"
                                                              :en "EN: Ajan tulee olla muotoa h.mm"}
   :reset-organization                                       {:fi "Palauta oletusorganisaatio"
                                                              :sv "Återställ utgångsorganisation"
                                                              :en "Reset the organization"}
   :save                                                     {:fi "Tallenna"
                                                              :sv "Spara"
                                                              :en "Save"}
   :save-changes                                             {:fi "Tallenna muutokset"
                                                              :sv "Spara ändringar"
                                                              :en "Save the changes"}
   :search-by-applicant-info                                 {:fi "Etsi hakijan henkilötiedoilla"
                                                              :sv "Sök med sökandes personuppgifter"
                                                              :en "Search by applicant's personal information"}
   :search-sub-organizations                                 {:fi "Etsi aliorganisaatioita"
                                                              :sv "Sök underorganisationer"
                                                              :en "EN: Etsi aliorganisaatioita"}
   :search-terms-list                                        {:fi "Nimi, henkilötunnus, syntymäaika, sähköpostiosoite tai oidit"
                                                              :sv "Namn, personbeteckning, födelsetid eller e-postadress"
                                                              :en "Name, Finnish personal identity number, date of birth, email or OIDs"}
   :id-in-shared-use                                         {:fi "(tunniste on jaetussa käytössä)"
                                                              :sv "(identifikationen är delad)"
                                                              :en "(tunniste on jaetussa käytössä)"}
   :questions                                                {:fi "kysymykset"
                                                              :sv "frågor"
                                                              :en "questions"}
   :selection                                                {:fi "Valinta"
                                                              :sv "Antagning"
                                                              :en "Selection"}
   :send-email-to-applicant                                  {:fi "Lähetä viesti hakijalle"
                                                              :sv "Skicka e-post till sökanden"
                                                              :en "Send email"}
   :send-confirmation-email-to-applicant                     {:fi "Lähetä vahvistussähköposti hakijalle"
                                                              :sv "Skicka e-post med bekräftelse till sökanden"
                                                              :en "Send confirmation email again"}
   :send-confirmation-email-to-applicant-and-guardian        {:fi "Lähetä vahvistussähköposti hakijalle ja huoltajalle"
                                                              :sv "Skicka e-post med bekräftelse till sökanden och vårdnadshavaren"
                                                              :en "Send confirmation email again"}
   :send-edit-link-to-applicant                              {:fi "Vahvistussähköposti lähetetty uudelleen hakijalle"
                                                              :sv "Bearbetningslänken har skickats till sökande per e-post"
                                                              :en "Confirmation email has been sent"}
   :send-information-request                                 {:fi "Lähetä täydennyspyyntö"
                                                              :sv "Skicka begäran om komplettering"
                                                              :en "Send information request"}
   :send-information-request-to-applicant                    {:fi "Lähetä täydennyspyyntö hakijalle"
                                                              :sv "Skicka begäran om komplettering till sökanden"
                                                              :en "Send information request message"}
   :sending-information-request                              {:fi "Täydennyspyyntöä lähetetään"
                                                              :sv "Begäran om komplettering skickas"
                                                              :en "Message has been sent"}
   :information-request-send-reminder                        {:fi "Hakijalle lähtee muistutusviesti vastaamattomasta täydennyspyynnöstä"
                                                              :sv "SV Hakijalle lähtee muistutusviesti vastaamattomasta täydennyspyynnöstä"
                                                              :en "EN Hakijalle lähtee muistutusviesti vastaamattomasta täydennyspyynnöstä"}
   :set-haku-to-form                                         {:fi "Aseta ensin lomake haun käyttöön niin voit tehdä hakukohteen mukaan näkyviä sisältöjä."
                                                              :sv "Ställ först blanketten för användning i ansökan för att kunna bilda innehåll för ansökningsmålet."
                                                              :en "EN: Aseta ensin lomake haun käyttöön niin voit tehdä hakukohteen mukaan näkyviä sisältöjä."}
   :state                                                    {:fi "Tila"
                                                              :sv "Status"
                                                              :en "EN: Tila"}
   :states-selected                                          {:fi "tilaa valittu"
                                                              :sv "status har valts"
                                                              :en "EN: tilaa valittu"}
   :liitepyynto-deadline                                     {:fi "Hakijakohtainen aikaraja"
                                                              :sv "Tidsgräns enligt sökande"
                                                              :en "EN: Hakijakohtainen aikaraja"}
   :liitepyynto-deadline-date                                {:fi "Viimeinen palautusajankohta"
                                                              :sv "Sista returdatum"
                                                              :en "EN: Viimeinen palautusajankohta"}
   :liitepyynto-deadline-time                                {:fi "klo"
                                                              :sv "kl."
                                                              :en "EN: klo"}
   :liitepyynto-deadline-error                               {:fi "Aikarajan tallennus epäonnistui"
                                                              :sv "Att spara tidsgränsen misslyckades"
                                                              :en "EN: Aikarajan tallennus epäonnistui"}
   :liitepyynto-deadline-set                                 {:fi "Hakijakohtainen aikaraja asetettu"
                                                              :sv "En tidsgräns enligt sökande har angetts"
                                                              :en "EN: Hakijakohtainen aikaraja asetettu"}
   :liitepyynto-deadline-unset                               {:fi "Hakijakohtainen aikaraja poistettu"
                                                              :sv "En tidsgräns enligt sökande har raderats"
                                                              :en "EN: Hakijakohtainen aikaraja poistettu"}
   :selected-hakukohde-no-rights                             {:fi "Sinulla on valittuna hakukohde, johon ei ole käsittelyoikeuksia"
                                                              :sv "SV: Sinulla on valittuna hakukohde, johon ei ole käsittelyoikeuksia"
                                                              :en "EN: Sinulla on valittuna hakukohde, johon ei ole käsittelyoikeuksia"}
   :settings                                                 {:fi "Asetukset"
                                                              :sv "Inställningar"
                                                              :en "EN: Asetukset"}
   :shape                                                    {:fi "Muoto:"
                                                              :sv "Form:"
                                                              :en "EN: Muoto:"}
   :show-more                                                {:fi "Näytä lisää.."
                                                              :sv "Visa mer.."
                                                              :en "EN: Näytä lisää.."}
   :show-options                                             {:fi "Näytä vastausvaihtoehdot"
                                                              :sv "Visa svarsalternativ"
                                                              :en "EN: Näytä vastausvaihtoehdot"}
   :single-choice-button                                     {:fi "Painikkeet, yksi valittavissa"
                                                              :sv "En tangent kan väljas"
                                                              :en "EN: Painikkeet, yksi valittavissa"}
   :single-choice-button-koodisto                            {:fi "Painikkeet, yksi valittavissa, koodisto"
                                                              :sv "En tangent kan väljas, kodregister"
                                                              :en "EN: Painikkeet, yksi valittavissa, codes"}
   :ssn                                                      {:fi "Henkilötunnus"
                                                              :sv "Personbeteckning"
                                                              :en "Personal identity code"}
   :with-ssn                                                 {:fi "Henkilötunnuksellinen"
                                                              :sv "Med personbeteckning"
                                                              :en "With personal identity code"}
   :without-ssn                                              {:fi "Henkilötunnukseton"
                                                              :sv "Utan personbeteckning"
                                                              :en "Without personal identity code"}
   :student                                                  {:fi "Oppija"
                                                              :sv "Studerande"
                                                              :en "Applicant"}
   :person-oid                                               {:fi "Henkilö-OID"
                                                              :sv "Person OID"
                                                              :en "Person OID"}
   :student-number                                           {:fi "Oppijanumero"
                                                               :sv "SV: Oppijanumero"
                                                               :en "Student number"}
   :submitted-application                                    {:fi "syötti hakemuksen"
                                                              :sv "matade in ansökan"
                                                              :en "submitted"}
   :submitted-at                                             {:fi "Hakemus jätetty"
                                                              :sv "Ansökan inlämnad"
                                                              :en "Application submitted"}
   :swedish                                                  {:fi "Ruotsi"
                                                              :sv "Svenska"
                                                              :en "Swedish"}
   :test-application                                         {:fi "Testihakemus / Virkailijatäyttö"
                                                              :sv "Testansökan / Administratören fyller i"
                                                              :en "Test application"}
   :text                                                     {:fi "Teksti"
                                                              :sv "Text"
                                                              :en "Text"}
   :text-area                                                {:fi "Tekstialue"
                                                              :sv "Textområde"
                                                              :en "Text area"}
   :text-area-size                                           {:fi "Tekstialueen koko"
                                                              :sv "Textområdets storlek"
                                                              :en "EN: Tekstialueen koko"}
   :text-field                                               {:fi "Tekstikenttä"
                                                              :sv "Textfält"
                                                              :en "Text field"}
   :text-field-size                                          {:fi "Tekstikentän koko"
                                                              :sv "Textfältets storlek"
                                                              :en "EN: Tekstikentän koko"}
   :title                                                    {:fi "Otsikko"
                                                              :sv "Rubrik"
                                                              :en "Title"}
   :to-state                                                 {:fi "Muutetaan tilaan"
                                                              :sv "Status ändras till"
                                                              :en "Change status to"}
   :unidentified                                             {:fi "Yksilöimättömät"
                                                              :sv "Inte identifierade"
                                                              :en "Unidentified"}
   :unknown                                                  {:fi "Tuntematon"
                                                              :sv "Okänd"
                                                              :en "Unknown"}
   :unknown-virkailija                                       {:fi "Tuntematon virkailija"
                                                              :sv "Okänd administratör"
                                                              :en "Unknown official"}
   :unknown-option                                           {:fi "Tuntematon vastausvaihtoehto"
                                                              :sv "Okänt svarsalternativ"
                                                              :en "Unknown option"}
   :unprocessed                                              {:fi "Käsittelemättä"
                                                              :sv "Obehandlad"
                                                              :en "Unprocessed"}
   :unprocessed-haut                                         {:fi "Käsittelemättä olevat haut"
                                                              :sv "Obehandlade ansökningar"
                                                              :en "Unprocessed admissions"}
   :used-by-haku                                             {:fi "Tämä lomake on haun käytössä"
                                                              :sv "Denna blankett används i ansökan"
                                                              :en "EN: Tämä lomake on haun käytössä"}
   :used-by-haut                                             {:fi "Tämä lomake on seuraavien hakujen käytössä"
                                                              :sv "Denna blankett används i följande ansökningar"
                                                              :en "EN: Tämä lomake on seuraavien hakujen käytössä"}
   :requires-kk-application-payment                          {:fi "Haussa on käytössä hakemusmaksu"
                                                              :sv "SV: Haussa on käytössä hakemusmaksu"
                                                              :en "EN: Haussa on käytössä hakemusmaksu"}
   :kevyt-valinta-valinnan-tila-change                       {:fi "Valinta: %s"
                                                              :sv "Antagning: %s"
                                                              :en "Student selection: %s"}
   :valintatuloksen-julkaisulupa                             {:fi "Valintatuloksen julkaisulupa"
                                                              :sv "Tillstånd att publicera antagningsresultat"
                                                              :en "EN : Valintatuloksen julkaisulupa"}
   :view-applications-rights-panel                           {:fi "Hakemusten katselu"
                                                              :sv "Granskning av ansökningar"
                                                              :en "EN: Hakemusten katselu"}
   :view-valinta-rights-panel                                {:fi "Valinnan tuloksen katselu"
                                                              :sv "Granskning av antagningsresultat"
                                                              :en "EN: Valinnan tuloksen katselu"}
   :opinto-ohjaaja                                           {:fi "Opinto-ohjaaja"
                                                              :sv "Studiehandledare"
                                                              :en "Study advisor"}
   :valinnat-valilehti                                       {:fi "Valinnat-välilehti"
                                                              :sv "Antagningar-fliken"
                                                              :en "EN: Valinnat-välilehti"}
   :virus-found                                              {:fi "Virus löytyi"
                                                              :sv "Ett virus hittades"
                                                              :en "Virus found"}
   :virus-scan-failed                                        {:fi "Virustarkistus epäonnistui teknisen virheen vuoksi"
                                                              :sv "Viruskontrollen misslyckades på grund av ett tekniskt fel"
                                                              :en "Virus scan failed due to a technical error"}
   :visibility-on-form                                       {:fi "Näkyvyys lomakkeella:"
                                                              :sv "Visas på blanketten:"
                                                              :en "EN: Näkyvyys lomakkeella:"}
   :visible-to-all                                           {:fi "näkyy kaikille"
                                                              :sv "visas för alla"
                                                              :en "EN: näkyy kaikille"}
   :hidden                                                   {:fi "piilotettu"
                                                              :sv "dold"
                                                              :en "hidden"}
   :is-hidden?                                               {:fi "ei näytetä lomakkeella (piilotettu)"
                                                              :sv "visas inte på blanketten (dold)"
                                                              :en "EN: ei näytetä lomakkeella (piilotettu)"}
   :visible-to-hakukohteet                                   {:fi "vain valituille hakukohteille",
                                                              :sv "endast för valda ansökningsmål"
                                                              :en "EN: vain valituille hakukohteille"}
   :wrapper-element                                          {:fi "Lomakeosio"
                                                              :sv "Blankettdel"
                                                              :en "Form element"}
   :wrapper-header                                           {:fi "Osion nimi"
                                                              :sv "Delens namn"
                                                              :en "Element's header"}
   :active-status                                            {:fi "Aktiivisuus"
                                                              :sv "Aktivitet"
                                                              :en "Active"}
   :only-edited-hakutoiveet-edited                           {:fi "Muokatut"
                                                              :sv "Redigerade"
                                                              :en "Edited"}
   :only-edited-hakutoiveet-unedited                         {:fi "Muokkaamattomat"
                                                              :sv "Oredigerad"
                                                              :en "Unedited"}
   :active-status-active                                     {:fi "Aktiiviset"
                                                              :sv "Aktiva"
                                                              :en "Actives"}
   :active-status-passive                                    {:fi "Passivoidut"
                                                              :sv "Passiverade"
                                                              :en "Passives"}
   :application-count-unprocessed                            {:fi "Käsittelemättä"
                                                              :sv "Obehandlad"
                                                              :en "Unprocessed"}
   :application-count-processing                             {:fi "Käsittely on kesken"
                                                              :sv "Behandlingen är inte färdig"
                                                              :en "In process"}
   :application-count-processed                              {:fi "Käsitelty"
                                                              :sv "Behandlad"
                                                              :en "Processed"}
   :navigate-applications-forward                            {:fi "Seuraava hakemus"
                                                              :sv "Följande ansökan"
                                                              :en "Next application"}
   :navigate-applications-back                               {:fi "Edellinen hakemus"
                                                              :sv "Föregående ansökan"
                                                              :en "Previous application"}
   :autosave-enabled                                         {:fi "Automaattitalletus: päällä"
                                                              :sv "Automatspar: på"
                                                              :en "Auto-save: enabled"}
   :multiple-values                                          {:fi "Monta arvoa"
                                                              :sv "Multipla värden"
                                                              :en "Multiple values"}
   :autosave-disabled                                        {:fi "Automaattitalletus: pois päältä"
                                                              :sv "Automatspar: av"
                                                              :en "Auto-save: disabled"}
   :hylatty                                                  {:fi "Hylätty"
                                                              :sv "Underkänd"
                                                              :en "Rejected"}
   :varalla                                                  {:fi "Varalla"
                                                              :sv "På reserv"
                                                              :en "On reserve place"}
   :peruuntunut                                              {:fi "Peruuntunut"
                                                              :sv "Inställt"
                                                              :en "Cancelled"}
   :varasijalta-hyvaksytty                                   {:fi "Varasijalta hyväksytty"}
   :hyvaksytty                                               {:fi "Hyväksytty"
                                                              :sv "Godkänd"
                                                              :en "Selected"}
   :julkaistu                                                {:fi "Julkaistu"}
   :ei-julkaistu                                             {:fi "Ei julkaistu"}
   :ei-vastaanotettu-maaraaikana                             {:fi "Ei vastaanotettu määräaikana"}
   :perunut                                                  {:fi "Perunut"}
   :peruutettu                                               {:fi "Peruutettu"}
   :ottanut-vastaan-toisen-paikan                            {:fi "Ottanut vastaan toisen paikan"}
   :ehdollisesti-vastaanottanut                              {:fi "Ehdollisesti vastaanottanut"}
   :vastaanottanut-sitovasti                                 {:fi "Vastaanottanut sitovasti"}
   :kesken                                                   {:fi "Kesken"
                                                              :sv "Inte färdig"
                                                              :en "Incomplete"}
   :vastaanottanut                                           {:fi "Vastaanottanut"}
   :ei-tehty                                                 {:fi "Ei tehty"}
   :lasna-koko-lukuvuosi                                     {:fi "Läsnä (koko lukuvuosi)"}
   :poissa-koko-lukuvuosi                                    {:fi "Poissa (koko lukuvuosi)"}
   :ei-ilmoittautunut-maaraaikana                            {:fi "Ei ilmoittautunut määräaikana"}
   :lasna-syksy                                              {:fi "Läsnä syksy, poissa kevät"}
   :poissa-syksy                                             {:fi "Poissa syksy, läsnä kevät"}
   :lasna                                                    {:fi "Läsnä, keväällä alkava koulutus"}
   :poissa                                                   {:fi "Poissa, keväällä alkava koulutus"}
   :valinta                                                  {:fi "Valinta"
                                                              :sv "Antagning"
                                                              :en "Selection"}
   :julkaisu                                                 {:fi "Julkaisu"}
   :vastaanotto                                              {:fi "Vastaanotto"}
   :ilmoittautuminen                                         {:fi "Ilmoittautuminen"}
   :odottamaton-virhe-otsikko                                {:fi "Tapahtui odottamaton virhe"
                                                              :sv "Ett oväntat fel uppstod"}
   :odottamaton-virhe-aputeksti                              {:fi "Yritä uudelleen tai ota yhteyttä ylläpitoon."
                                                              :sv "Försök igen, om problemet kvarstår, kontakta registratorn."}
   :cannot-deactivate-info                                   {:fi "Hakemuksen tilaa ei voi muuttaa, koska hakemukselle on muodostunut valinnan tuloksia. Ota yhteyttä Opetushallitukseen hakemuksen passivoimiseksi."
                                                              :sv "Ansökningens status kan inte ändras eftersom antagningsresultat har bildats för ansökningen. Kontakta Utbildningsstyrelsen för att få ansökningen passiverad."
                                                              :en "EN: Hakemuksen tilaa ei voi muuttaa, koska hakemukselle on muodostunut valinnan tuloksia. Ota yhteyttä Opetushallitukseen hakemuksen passivoimiseksi."}
   :lisakysymys                                              {:fi "Lisäkysymys"
                                                              :sv "Tilläggsfråga"
                                                              :en "Extra question"}
   :lisakysymys-arvon-perusteella                            {:fi "Lisäkysymys arvon perusteella"
                                                              :sv "SV: Lisäkysymys arvon perusteella"
                                                              :en "EN: Lisäkysymys arvon perusteella"}
   :lisakysymys-arvon-perusteella-ehto                       {:fi "Jos vastauksen arvo on"
                                                              :sv "Om svarets värde är"
                                                              :en "EN: Jos vastauksen arvo on"}
   :lisakysymys-arvon-perusteella-ehto-pienempi              {:fi "pienempi kuin"
                                                              :sv "mindre än"
                                                              :en "EN: less than"}
   :lisakysymys-arvon-perusteella-ehto-suurempi              {:fi "suurempi kuin"
                                                              :sv "större än"
                                                              :en "EN: greater than"}
   :lisakysymys-arvon-perusteella-ehto-yhtasuuri             {:fi "yhtä suuri kuin"
                                                              :sv "lika med"
                                                              :en "EN: equal to"}
   :lisakysymys-arvon-perusteella-lisaa-ehto                 {:fi "Lisää ehto"
                                                              :sv "Lisää ehto"
                                                              :en "EN: Lisää ehto"}
   :lomakeosion-piilottaminen-arvon-perusteella              {:fi "Toisen lomakeosion piilottaminen arvon perusteella"
                                                              :sv "SV: Toisen lomakeosion piilottaminen arvon perusteella"
                                                              :en "EN: Toisen lomakeosion piilottaminen arvon perusteella"}
   :lomakeosion-piilottaminen-arvon-perusteella-valitse-osio {:fi "Valitse piilotettava osio tästä"
                                                              :sv "SV: Valitse piilotettava osio tästä"
                                                              :en "EN: Valitse piilotettava osio tästä"}
   :filter-by-question-answer                                {:fi "Rajaa vastauksen mukaan"
                                                              :sv "Avgränsa enligt svar"
                                                              :en "EN: Rajaa vastauksen mukaan"}
   :question-answer                                          {:fi "Vastaus"
                                                              :sv "Svar"
                                                              :en "EN: Vastaus"}
   :question-answers-selected                                {:fi "vastausvaihtoehtoa valittu"
                                                              :sv "Svarsalternativ valt"
                                                              :en "EN: vastausvaihtoehtoa valittu"}
   :attachments-tab-header                                   {:fi "Toimitettavat liitteet"
                                                              :sv ""
                                                              :en ""}
   :maksupyynto-amount-label                                 {:fi "Maksun määrä"
                                                              :sv ""
                                                              :en ""}
   :maksupyynto-total-paid-label                             {:fi "Yhteissumma"
                                                              :sv ""
                                                              :en ""}
   :maksupyynto-due-label                                    {:fi "Eräpäivä"
                                                              :sv ""
                                                              :en ""}
   :maksupyynto-header                                       {:fi "Hakemusmaksupyyntö"
                                                              :sv ""
                                                              :en ""}
   :maksupyynto-processing-header                            {:fi "Käsittelymaksu:"
                                                              :sv ""
                                                              :en ""}
   :maksupyynto-decision-header                              {:fi "Päätösmaksu:"
                                                              :sv ""
                                                              :en ""}
   :maksupyynto-recipient                                    {:fi "Vastaanottaja:"
                                                              :sv ""
                                                              :en ""}
   :maksupyynto-amount                                       {:fi "Summa"
                                                              :sv ""
                                                              :en ""}
   :maksupyynto-message                                      {:fi "Viesti:"
                                                              :sv ""
                                                              :en ""}
   :maksupyynto-send-button                                  {:fi "Lähetä maksupyyntö"
                                                              :sv ""
                                                              :en ""}
   :maksupyynto-again-button                                 {:fi "Lähetä uudelleen"
                                                              :sv ""
                                                              :en ""}
   :maksupyynto-kasittelymaksu-button                        {:fi "Uudelleenlähetä käsittelymaksu"
                                                              :sv ""
                                                              :en ""}
   :maksupyynto-invoice-notfound                             {:fi "Maksun tietoja ei löydy"
                                                              :sv ""
                                                              :en ""}
   :maksupyynto-not-sent                                     {:fi "Maksupyyntöä ei ole lähetetty"
                                                              :sv "SV: Maksupyyntöä ei ole lähetetty"
                                                              :en "EN: Maksupyyntöä ei ole lähetetty"}
   :maksupyynto-payment-active                               {:fi "Avoin"
                                                              :sv ""
                                                              :en ""}
   :maksupyynto-payment-paid                                 {:fi "Maksettu"
                                                              :sv ""
                                                              :en ""}
   :maksupyynto-payment-overdue                              {:fi "Eräpäivä ylitetty"
                                                              :sv ""
                                                              :en ""}
   :maksupyynto-payment-unknown                              {:fi "Maksun tilaa ei tiedetä"
                                                              :sv ""
                                                              :en ""}
   :maksupyynto-amount-input-placeholder                     {:fi "Anna summa muodossa 123 tai 123.00"
                                                              :sv ""
                                                              :en ""}
   :maksupyynto-payment-download-receipt                     {:fi "Lataa kuitti"
                                                              :sv "Lataa kuitti"
                                                              :en "Lataa kuitti"}
   :prevent-submission                                       {:fi "Valinta estää hakemuksen lähettämisen"
                                                              :sv "SV: Valinta estää hakemuksen lähettämisen"
                                                              :en "EN: Valinta estää hakemuksen lähettämisen"}
   :button-text                                              {:fi "Painikkeen teksti"
                                                              :sv "SV: Painikkeen teksti"
                                                              :en "EN: Painikkeen teksti"}
   :pohjakoulutus-for-valinnat                               {:fi "TIedot valintoja varten"
                                                              :sv "Uppgifter för antagningarna"
                                                              :en "EN: TIedot valintoja varten"}
   :pohjakoulutus-for-valinnat-alaotsikko                    {:fi "(koostettu KOSKI-järjestelmästä ja hakijan antamista tiedoista)"
                                                              :sv "(Sammanställt av KOSKI-uppgifter samt uppgifter angivna av sökande)"
                                                              :en "EN: (koostettu KOSKI-järjestelmästä ja hakijan antamista tiedoista)"}
   :pohjakoulutus-opetuskieli                                {:fi "Opetuskieli"
                                                              :sv "Undervisningsspråk"
                                                              :en "EN: Opetuskieli"}
   :pohjakoulutus-suoritusvuosi                              {:fi "Suoritusvuosi"
                                                              :sv "Avlagd år"
                                                              :en "EN: Suoritusvuosi"}
   :pohjakoulutus-yksilollistetty                            {:fi "Matematiikan ja äidinkielen yksilöllistetty oppimäärä"
                                                              :sv "SV: Matematiikan ja äidinkielen yksilöllistetty oppimäärä"
                                                              :en "EN: Matematiikan ja äidinkielen yksilöllistetty oppimäärä"}
   :only-harkinnanvarainen-valinta                           {:fi "Hakija on mukana vain harkintaan perustuvassa valinnassa (ammatilliseen koulutukseen tai lukioon haettaessa)."
                                                              :sv "Sökande är med endast i antagning enligt prövning (i ansökan till yrkesinriktad utbildning eller gymnasieutbildning)."
                                                              :en "EN: Hakija on mukana vain harkintaan perustuvassa valinnassa (ammatilliseen koulutukseen tai lukioon haettaessa)."}
   :desync-harkinnanvarainen                                 {:fi "Hakija ei ole mukana harkinnanvaraisessa valinnassa."
                                                              :sv "Sökande är inte med i antagning enligt prövning."
                                                              :en "EN: Hakija ei ole mukana harkinnanvaraisessa valinnassa."}
   :lisapistekoulutukset                                     {:fi "Lisäpistekoulutukset"
                                                              :sv "SV: Lisäpistekoulutukset"
                                                              :en "EN: Lisäpistekoulutukset"}
   :lisapistekoulutus-perusopetuksenlisaopetus               {:fi "Kymppiluokka (perusopetuksen lisäopetus)"
                                                              :sv "SV: Kymppiluokka (perusopetuksen lisäopetus)"
                                                              :en "EN: Kymppiluokka (perusopetuksen lisäopetus)"}
   :lisapistekoulutus-valma                                  {:fi "Ammatilliseen koulutukseen valmentava koulutus VALMA"
                                                              :sv "SV: Ammatilliseen koulutukseen valmentava koulutus VALMA"
                                                              :en "EN: Ammatilliseen koulutukseen valmentava koulutus VALMA"}
   :lisapistekoulutus-luva                                   {:fi "Maahanmuuttajien lukiokoulutukseen valmistava koulutus LUVA"
                                                              :sv "SV: Maahanmuuttajien lukiokoulutukseen valmistava koulutus LUVA"
                                                              :en "EN: Maahanmuuttajien lukiokoulutukseen valmistava koulutus LUVA"}
   :lisapistekoulutus-kansanopisto                           {:fi "Kansanopiston lukuvuoden mittainen linja"
                                                              :sv "SV: Kansanopiston lukuvuoden mittainen linja"
                                                              :en "EN: Kansanopiston lukuvuoden mittainen linja"}
   :lisapistekoulutus-opistovuosi                            {:fi "Oppivelvollisille suunnattu vapaan sivistystyön koulutus"
                                                              :sv "SV: Oppivelvollisille suunnattu vapaan sivistystyön koulutus"
                                                              :en "EN: Oppivelvollisille suunnattu vapaan sivistystyön koulutus"}
   :lisapistekoulutus-tuva                                   {:fi "Tutkintokoulutukseen valmentava koulutus"
                                                              :sv "Utbildning som handleder för examensutbildning"
                                                              :en "EN: Tutkintokoulutukseen valmentava koulutus"}
   :error-loading-harkinnanvaraisuus                         {:fi "Virhe harkinnanvaraisuustietojen hakemisessa"
                                                              :sv "Fel i sökning av uppgifter om individualiseringar"
                                                              :en "EN: Virhe harkinnanvaraisuustietojen hakemisessa"}
   :error-loading-pohjakoulutus                              {:fi "Virhe pohjakoulutuksen hakemisessa"
                                                              :sv "Fel i sökning av grundutbildning"
                                                              :en "EN: Virhe pohjakoulutuksen hakemisessa"}
   :pohjakoulutus-not-found                                  {:fi "Pohjakoulutusta ei löytynyt"
                                                              :sv "Grundutbildning hittades inte"
                                                              :en "EN: Pohjakoulutusta ei löytynyt"}
   :error-loading-valinnat                                   {:fi "Virhe hakemuksen valintojen hakemisessa"
                                                              :sv "SV: Virhe hakemuksen valintojen hakemisessa"
                                                              :en "EN: Virhe hakemuksen valintojen hakemisessa"}
   :ht-hakenut-vahvasti-tunnistautuneena                     {:fi "Hakija on vahvasti tunnistautunut."
                                                              :sv "SV: Hakija on vahvasti tunnistautunut."
                                                              :en "EN: Hakija on vahvasti tunnistautunut."}
   :ht-eidas-tunnistautunut                                  {:fi "Hakija on eidas-tunnistautunut."
                                                              :sv "SV: Hakija on eidas-tunnistautunut."
                                                              :en "EN: Hakija on eidas-tunnistautunut."}
   :valittu                                                  {:fi "valittu"
                                                              :sv "valda"
                                                              :en "selected"}
   :payment-not-checked                                      {:fi "Hakijan maksuvelvollisuuden tila ei ole vielä tiedossa"
                                                              :sv "Sökandens betalningsskyldighet är ännu okänd"
                                                              :en "Applicant payment liability unknown"}
   :payment-not-obligated                                    {:fi "Hakija ei ole maksuvelvollinen"
                                                              :sv "SV: Hakija ei ole maksuvelvollinen"
                                                              :en "EN: Hakija ei ole maksuvelvollinen"}
   :tutkinnot                                                {:fi "Tutkintotiedot Koski-Palvelusta"
                                                              :sv "SV: Tutkintotiedot Koski-Palvelusta"
                                                              :en "EN: Tutkintotiedot Koski-Palvelusta"}
   :koski-originated-tutkinto-tag-first-row                  {:fi "Tutkinnon tiedot haettu"
                                                              :sv "SV: Tutkinnon tiedot haettu"
                                                              :en "EN: Tutkinnon tiedot haettu"}
   :koski-originated-tutkinto-tag-second-row                 {:fi "KOSKI-järjestelmästä"
                                                              :sv "SV: KOSKI-järjestelmästä"
                                                              :en "EN: KOSKI-järjestelmästä"}
   :koski-tutkinto-fetch-failed                              {:fi "Tutkintotietojen haku KOSKI-järjestelmästä epäonnistui"
                                                              :sv "SV: Tutkintotietojen haku KOSKI-järjestelmästä epäonnistui"
                                                              :en "EN: Tutkintotietojen haku KOSKI-järjestelmästä epäonnistui"}})

(def state-translations
  {:active                       {:fi "Aktiivinen"
                                  :sv "Aktiv"
                                  :en "Active"}
   :passive                      {:fi "Passiivinen"
                                  :sv "Passiv"
                                  :en "Inactive"}
   :unprocessed                  {:fi "Käsittelemättä"
                                  :sv "Obehandlad"
                                  :en "Unprocessed"}
   :processing                   {:fi "Käsittelyssä"
                                  :sv "Under behandling"
                                  :en "Under process"}
   :invited-to-interview         {:fi "Kutsuttu haast."
                                  :sv "Kallad till intervju"
                                  :en "Invited to interview"}
   :invited-to-exam              {:fi "Kutsuttu valintak."
                                  :sv "Kallad till urvalsprov"
                                  :en "Invited to entrance examination"}
   :evaluating                   {:fi "Arvioinnissa"
                                  :sv "Under bedömning"
                                  :en "Under evaluation"}
   :valintaesitys                {:fi "Valintaesitys"
                                  :sv "Antagningsförslag"
                                  :en "Admission proposal"}
   :processed                    {:fi "Käsitelty"
                                  :sv "Behandlad"
                                  :en "Processed"}
   :information-request          {:fi "Täydennyspyyntö"
                                  :sv "Begäran om komplettering"
                                  :en "Information request"}
   :incomplete                   {:fi "Kesken"
                                  :sv "Inte färdig"
                                  :en "Incomplete"}
   :not-done                     {:fi "Ei tehty"
                                  :sv "Inte gjort"
                                  :en "Not done"}
   :selection-proposal           {:fi "Valintaesitys"
                                  :sv "Antagningsförslag"
                                  :en "Selected (pending)"}
   :reserve                      {:fi "Varalla"
                                  :sv "På reserv"
                                  :en "On reserve place"}
   :cancelled                    {:fi "Peruuntunut"
                                  :sv "Inställd"
                                  :en "Cancelled"}
   :selected                     {:fi "Hyväksytty"
                                  :sv "Godkänd"
                                  :en "Selected"}
   :accepted                     {:fi "Hyväksytty"
                                  :sv "Accepterad"
                                  :en "Accepted"}
   :rejected                     {:fi "Hylätty"
                                  :sv "Underkänd"
                                  :en "Rejected"}
   :accepted-from-reserve        {:fi "Varasijalta hyväksytty"
                                  :sv "Godkänd från reservplats"
                                  :en "Accepted from reserve"}
   :bindingly-received           {:fi "Vastaanottanut sitovasti"
                                  :sv "Mottagit bindande"}
   :present-whole-academic-year  {:fi "Läsnä koko lukuvuoden"
                                  :sv "Närvarande hela läsåret"
                                  :en "Present whole academic year"}
   :away-whole-acedemic-year     {:fi "Poissa koko lukuvuoden"
                                  :sv "Frånvarande hela läsåret"
                                  :en "Away whole academic year"}
   :cancelled-by-someone         {:fi "Peruutettu"
                                  :sv "Annullerats"}
   :cancelled-by-applicant       {:fi "Perunut"
                                  :sv "Annullerad"
                                  :en "Cancelled by applicant"}
   :present-autumn               {:fi "Läsnä syksyn"
                                  :sv "Närvarande hösten"
                                  :en "Present during autumn"}
   :away-autumn                  {:fi "Poissa syksyn"
                                  :sv "Frånvarande hösten"
                                  :en "Away during autumn"}
   :present-spring               {:fi "Läsnä kevään"
                                  :sv "Närvarande våren"
                                  :en "Present during spring"}
   :away-spring                  {:fi "Poissa kevään"
                                  :sv "Frånvarande våren"
                                  :en "Away during spring"}
   :accepted-harkinnanvaraisesti {:fi "Harkinnanvaraisesti hyväksytty"
                                  :sv "Godkänd enligt prövning"}
   :not-enrolled                 {:fi "Ei ilmoittautunut"
                                  :sv "Ej anmält sig"
                                  :en "Not enrolled"}
   :not-received-during-period   {:fi "Ei vastaanotettu määrä-aikana"
                                  :sv "Ej mottagit inom utsatt tid"}
   :received-another             {:fi "Ottanut vastaan toisen paikan"
                                  :sv "Tagit emot annan plats"}
   :conditionally-received       {:fi "Ehdollisesti vastaanottanut"
                                  :sv "Mottagit villkorligt"}
   :unreviewed                   {:fi "Tarkastamatta"
                                  :sv "Inte granskad"
                                  :en "Unreviewed"}
   :fulfilled                    {:fi "Täyttyy"
                                  :sv "Fylls"
                                  :en "Meets requirement"}
   :unfulfilled                  {:fi "Ei täyty"
                                  :sv "Fylls inte"
                                  :en "Does nor meet requirement"}
   :eligible                     {:fi "Hakukelpoinen"
                                  :sv "Ansökningsbehörig"
                                  :en "Eligible"}
   :uneligible                   {:fi "Ei hakukelpoinen"
                                  :sv "Inte ansökningsbehörig"
                                  :en "Not eligible"}
   :conditionally-eligible       {:fi "Ehdollisesti hakukelpoinen"
                                  :sv "Villkorligt ansökningsbehörig"
                                  :en "Conditionally eligible"}
   :obligated                    {:fi "Velvollinen"
                                  :sv "Förpliktad"
                                  :en "Obligated"}
   :not-obligated                {:fi "Ei velvollinen"
                                  :sv "Inte förpliktad"
                                  :en "Not obligated"}
   :processing-state             {:fi "Käsittelyvaihe"
                                  :sv "Behandlingsskede"
                                  :en "State of processing"}
   :language-requirement         {:fi "Kielitaitovaatimus"
                                  :sv "Språkkunskapskrav"
                                  :en "Language requirement"}
   :only-edited-hakutoiveet      {:fi "Muokatut hakutoiveet"
                                  :sv "Bearbetad ansökningsönskemål"
                                  :en "Edited study program"}
   :degree-requirement           {:fi "Tutkinnon kelpoisuus"
                                  :sv "Examens behörighet"
                                  :en "Degree requirement"}
   :eligibility-state            {:fi "Hakukelpoisuus"
                                  :sv "Ansökningsbehörighet"
                                  :en "Eligibility"}
   :payment-obligation           {:fi "Lukuvuosimaksuvelvollisuus"
                                  :sv "Betalningsskyldighet"
                                  :en "Obligated to pay"}
   :selection-state              {:fi "Valinta"
                                  :sv "Antagning"
                                  :en "Selection"}
   :not-checked                  {:fi "Tarkastamatta"
                                  :sv "Inte granskad"
                                  :en "Not checked"}
   :checked                      {:fi "Tarkistettu"
                                  :sv "Granskad"
                                  :en "Checked"}
   :incomplete-answer            {:fi "Puutteellinen"
                                  :sv "Bristfällig"
                                  :en "Incomplete"}
   :overdue                      {:fi "Myöhässä"
                                  :sv "Försenad"
                                  :en "Overdue"}
   :no-attachment-required       {:fi "Ei liitepyyntöä"
                                  :sv "Ingen begäran om bilagor"
                                  :en "No attachment requirement"}
   :incomplete-attachment        {:fi "Puutteellinen liite"
                                  :sv "Bristfällig bilaga"
                                  :en "Insufficient attachment"}
   :attachment-missing           {:fi "Liite puuttuu"
                                  :sv "Bilaga fattas"
                                  :en "Attachment missing"}
   :processing-fee-overdue       {:fi "Käsittely maksamatta"
                                  :sv "Käsittely maksamatta (sv) TODO"
                                  :en "Käsittely maksamatta (en) TODO"}
   :processing-fee-paid          {:fi "Käsittely maksettu"
                                  :sv "Käsittely maksettu (sv) TODO"
                                  :en "Käsittely maksettu (en) TODO"}
   :decision-fee-outstanding     {:fi "Päätösmaksu avoin"
                                  :sv "Päätösmaksu avoin (sv) TODO"
                                  :en "Päätösmaksu avoin (en) TODO"}
   :decision-fee-overdue         {:fi "Päätös maksamatta"
                                  :sv "Päätös maksamatta (sv) TODO"
                                  :en "Päätös maksamatta (en) TODO"}
   :decision-fee-paid            {:fi "Päätös maksettu"
                                  :sv "Päätös maksettu (sv) TODO"
                                  :en "Päätös maksettu (en) TODO"}
   :invoiced                     {:fi "Laskutuksessa"
                                  :sv "Laskutuksessa (sv) TODO"
                                  :en "Laskutuksessa (en) TODO"}
   :multiple-values              {:fi "Monta arvoa"
                                  :sv "Multipla värden"
                                  :en "Multiple values"}
   :attachments-tab-info         {:fi "Kaikkien hakukohteiden liitetiedot eivät välttämättä näy tässä, mikäli oppilaitos ei ole tallentanut tietoja."
                                  :sv "Alla uppgifter om bilagor syns nödvändigtvis inte om läroanstalten inte sparat uppgifterna."
                                  :en "EN: Kaikkien hakukohteiden liitetiedot eivät välttämättä näy tässä, mikäli oppilaitos ei ole tallentanut tietoja."}
   :kk-application-payment       {:fi "Hakemusmaksu"
                                  :sv "Ansökningsavgift"
                                  :en "Application fee"}
   :kk-payment-not-checked       {:fi "Hakijan maksuvelvollisuuden tila ei ole vielä tiedossa"
                                  :sv "Sökandens betalningsskyldighet är ännu okänd"
                                  :en "Applicant payment liability unknown"}
   :not-required                 {:fi "Ei vaadittu"
                                  :sv "Krävs ej"
                                  :en "Not required"}
   :awaiting                     {:fi "Odottaa maksua"
                                  :sv "Väntar på betalning"
                                  :en "Awaiting payment"}
   :ok-by-proxy                  {:fi "Maksettu toisessa haussa"
                                  :sv "Betald i en annan ansökan"
                                  :en "Paid in another application round"}
   :paid                         {:fi "Maksettu"
                                  :sv "Betalt"
                                  :en "Paid"}
   :kk-payment-overdue           {:fi "Erääntynyt"
                                  :sv "Förfallen"
                                  :en "Expired"}})

(def tutu-decision-email
  {:header                {:fi "Päätös tutkintosi tunnustamisesta on tehty"
                           :sv "Beslut om erkännande av din examen har fattats"
                           :en "Decision on the recognition of your qualification has been made"}
   :subject-prefix        {:fi "Opintopolku"
                           :sv "Studieinfo"
                           :en "Studyinfo"}

   :decision-header       {:fi "Päätösmaksu"
                           :sv "Beslutsavgiften"
                           :en "Decision fee"}

   :decision-text-1       {:fi "Lähetämme päätöksen sinulle, kun olet maksanut päätösmaksun."
                           :sv "Vi skickar dig beslutet först då du har betalat beslutsavgiften."
                           :en "We will send you the decision once you have paid the decision fee."}
   :decision-text-2       {:fi "Voit maksaa päätösmaksun ja tarkastella maksusi tietoja seuraavasta linkistä."
                           :sv "Du kan betala beslutsavgiften och kontrollera uppgifterna som gäller betalningen via nedanstående länk."
                           :en "You can pay the decision fee and view the details of your payment through the following link."}
   :decision-text-3       {:fi "Jos et suorita päätösmaksua 14 vuorokauden sisällä, lähetämme sinulle päätösmaksusta erillisen laskun. Maksu on ulosottokelpoinen ilman tuomiota tai päätöstä (valtion maksuperustelaki (150/1992) 11§ 1.mom.)."
                           :sv "Om du inte betalabeslutsavgiften inom 14 dygn, skickar vi dig en separat faktura för beslutsavgiften. Avgiften från indrivas utan dom eller beslut (lag om grunderna för avgifter till staten (150/1992 11 § 1 mom.)."
                           :en "If you do not pay the decision fee within 14 days, we will send you a separate invoice for the decision fee. The payment is enforceable without a judgement or a decision (Act on Criteria for Charges Payable to the State 150/1992, section 11, subsection 1)."}

   :decision-info         {:fi "Lisätietoja päätöksistä ja maksuista on nettisivuillamme:"
                           :sv "Mer information om besluten och avgifterna finns på vår webbplats:"
                           :en "More information on the decisions and fees is available on our website:"}
   :decision-info-url     {:fi "https://www.oph.fi/fi/palvelut/tutkintojen-tunnustaminen"
                           :sv "https://www.oph.fi/sv/tjanster/erkannande-av-examina"
                           :en "https://www.oph.fi/en/services/recognition-and-international-comparability-qualifications"}

   :decision-info-noreply {:fi "Älä vastaa tähän viestiin – viesti on lähetetty automaattisesti. Jos sinulla on kysyttävää, otathan meihin yhteyttä sähköpostitse osoitteessa "
                           :sv "Svara inte på detta meddelande, det har skickats automatiskt. Om du har frågor, vänligen kontakta oss per epost via "
                           :en "This is an automatically generated email, please do not reply. If you have any questions, please send us an email at "}

   :signature-header      {:fi "Ystävällisin terveisin"
                           :sv "Med vänliga hälsningar,"
                           :en "Best regards"}
   :signature-name        {:fi "Opetushallitus"
                           :sv "Utbildningsstyrelsen"
                           :en "Finnish National Agency for Education"}})

(def astu-decision-email
  {:header1               {:fi "Hakemuksesi "
                           :sv "Din ansökan "
                           :en "Your application "}
   :header2               {:fi " on käsitelty."
                           :sv " har behandlats."
                           :en " has been processed."}
   :subject               {:fi "Opetushallitus: Hakemuksesi on käsitelty"
                           :sv "Utbildningsstyrelsen: Din ansökan har behandlats"
                           :en "The Finnish National Agency for Education: Your application has been processed"}
   :payment               {:fi "Maksu: "
                           :sv "Avgiften: "
                           :en "Fee: "}
   :due-date-desc         {:fi "Eräpäivä: "
                           :sv "Förfallodag: "
                           :en "Due date: "}
   :includes-vat          {:fi "sis. alv "
                           :sv "inkl. moms "
                           :en "incl. VAT "}
   :text-1                {:fi "Voit maksaa maksun ja tarkastella maksun tietoja seuraavasta linkistä: "
                           :sv "Du kan betala avgiften och kontrollera uppgifterna som gäller betalningen via nedanstående länk:"
                           :en "You can pay the fee and view the details of your payment through the following link:"}
   :text-2                {:fi "Linkki sulkeutuu eräpäivän jälkeen tietosuojasyistä."
                           :sv "Länken stängs efter förfallodatumet av dataskyddsskäl."
                           :en "The link will close after the due date for data protection reasons."}
   :text-3                {:fi "Jos et maksa maksua eräpäivään mennessä, lähetämme sinulle maksusta erillisen laskun."
                           :sv "Om du inte betalar före förfallodatumet, skickar vi dig en separat faktura för beslutsavgiften."
                           :en "If you do not pay by the due date, we will send you a separate invoice for the decision fee."}
   :info-noreply          {:fi "Älä vastaa tähän viestiin – viesti on lähetetty automaattisesti. Jos sinulla on kysyttävää, voit lähettää meille sähköpostia osoitteeseen "
                           :sv "Svara inte på detta meddelande, det har skickats automatiskt. Om du har frågor kan du kontakta oss på adressen "
                           :en "This is an automatically generated email, please do not reply. If you have any questions, please send us an email at "}
   :signature-header      {:fi "Ystävällisin terveisin"
                           :sv "Med vänliga hälsningar,"
                           :en "Best regards"}
   :signature-name        {:fi "Opetushallitus"
                           :sv "Utbildningsstyrelsen"
                           :en "Finnish National Agency for Education"}})

(def excel-texts
  {:name                     {:fi "Nimi"
                              :sv "Namn"
                              :en "EN: Nimi"}
   :id                       {:fi "Id"
                              :sv "Id"
                              :en "EN: Id"}
   :application-number       {:fi "Hakemusnumero"
                              :sv "Ansökningsnummer"
                              :en "Application number"}
   :key                      {:fi "Tunniste"
                              :sv "Identifikation"
                              :en "EN: Tunniste"}
   :created-time             {:fi "Viimeksi muokattu"
                              :sv "Senast bearbetad"
                              :en "EN: Viimeksi muokattu"}
   :created-by               {:fi "Viimeinen muokkaaja"
                              :sv "Senast bearbetad av"
                              :en "EN: Viimeinen muokkaaja"}
   :application-modified     {:fi "Hakemuksen viimeisimmän muokkauksen aika"
                              :sv "Tidpunkten för senaste bearbetning av ansökan"
                              :en "Time of last modification"}
   :application-submitted    {:fi "Hakemuksen tallennusaika"
                              :sv "Tidpunkten för när ansökan har sparats"
                              :en "Time of application submission"}
   :application-state        {:fi "Hakemuksen tila"
                              :sv "Ansökans status"
                              :en "Application status"}
   :hakukohde-handling-state {:fi "Hakukohteen käsittelyn tila"
                              :sv "Status för behandling av ansökningsmålet"
                              :en "EN: Hakukohteen käsittelyn tila"}
   :kielitaitovaatimus       {:fi "Kielitaitovaatimus"
                              :sv "Språkkunskapskrav"
                              :en "EN: Kielitaitovaatimus"}
   :tutkinnon-kelpoisuus     {:fi "Tutkinnon kelpoisuus"
                              :sv "Examens behörighet"
                              :en "EN: Tutkinnon kelpoisuus"}
   :hakukelpoisuus           {:fi "Hakukelpoisuus"
                              :sv "Ansökningsbehörighet"
                              :en "EN: Hakukelpoisuus"}
   :maksuvelvollisuus        {:fi "Maksuvelvollisuus"
                              :sv "Betalningsskyldighet"
                              :en "Obligated to pay"}
   :valinnan-tila            {:fi "Valinnan tila"
                              :sv "Antagningens status"
                              :en "EN: Valinnan tila"}
   :ehdollinen               {:fi "Ehdollinen"
                              :sv "Villkorlig"
                              :en "Conditional"}
   :pisteet                  {:fi "Pisteet"
                              :sv "Poäng"
                              :en "EN: Pisteet"}
   :student-number           {:fi "Oppijanumero"
                              :sv "Studentnummer"
                              :en "Student number"}
   :applicant-oid            {:fi "Hakijan henkilö-OID"
                              :sv "Sökandes person-OID"
                              :en "EN: Hakijan henkilö-OID"}
   :turvakielto              {:fi "Turvakielto"
                              :sv "Spärrmarkering"
                              :en "EN: Turvakielto"}
   :notes                    {:fi "Muistiinpanot"
                              :sv "Anteckningar"
                              :en "EN: Notes"}})

(def koski-tutkinnot-texts
  {:section-label                                 {:fi "Tutkinnot"
                                                   :sv "SV: Tutkinnot"
                                                   :en ""}
   :section-description                           {:fi "Hakijalle tuodaan Koski-palvelusta valittujen koulutusasteiden tutkintotiedot. Lisäksi hakija voi syöttää puuttuvia tutkintotietoja. Kirjautumaton hakija syöttää aina omat tutkintotietonsa."
                                                   :sv "SV: Hakijalle tuodaan Koski-palvelusta valittujen koulutusasteiden tutkintotiedot. Lisäksi hakija voi syöttää puuttuvia tutkintotietoja. Kirjautumaton hakija syöttää aina omat tutkintotietonsa."
                                                   :en ""}
   :field-list                                    {:fi "Tutkinto, Koulutusohjelma, Oppilaitos, Valmistumispäivä, Tutkintotodistus (liitepyyntö kirjautumattomille)."
                                                   :sv "SV: Tutkinto, Koulutusohjelma, Oppilaitos, Valmistumispäivä, Tutkintotodistus (liitepyyntö kirjautumattomille)."
                                                   :en ""}
   :completed-study-question-label                {:fi "Hakijalle näytetään opintosuoritukset"
                                                   :sv "SV: Hakijalle näytetään opintosuoritukset"
                                                   :en ""}
   :koski-update-policy-label                     {:fi "Koskesta tuodun tutkintotiedon päivittyminen"
                                                   :sv "SV: Koskesta tuodun tutkintotiedon päivittyminen"
                                                   :en ""}
   :koski-update-option-only-once-label           {:fi "Tiedot säilyvät samoina kuin ne ovat hakemushetkellä"
                                                   :sv "SV: Tiedot säilyvät samoina kuin ne ovat hakemushetkellä"
                                                   :en ""}
   :koski-update-option-allways-label             {:fi "Tiedot voivat päivittyä hakemuksen teon jälkeen, päätöksentekoon asti"
                                                   :sv "SV: Tiedot voivat päivittyä hakemuksen teon jälkeen, päätöksentekoon asti"
                                                   :en ""}
   :info-label                                    {:fi "Valitse ne tutkinnot, jotka haluat liittää hakemukseen"
                                                   :sv "SV: Valitse ne tutkinnot, jotka haluat liittää hakemukseen"
                                                   :en ""}
   :tutkintotaso-label                            {:fi "Tutkintotasot"
                                                   :sv "SV: Tutkintotasot"
                                                   :en ""}
   :tutkintotaso-description                      {:fi "Valitse tutkintotasot, joita haku koskee. Koskesta tuodaan vain tätä valintaa vastaavia tutkintotietoja. Vain valitun tutkintotason tiedot ovat valittavissa tutkinto-, koulutusohjelma- ja oppilaitosvalikosta."
                                                   :sv "SV: Valitse tutkintotasot, joita haku koskee. Koskesta tuodaan vain tätä valintaa vastaavia tutkintotietoja. Vain valitun tutkintotason tiedot ovat valittavissa tutkinto-, koulutusohjelma- ja oppilaitosvalikosta."
                                                   :en ""}
   :perusopetus-label                             {:fi "Perusopetus"
                                                   :sv "SV: Perusopetus"
                                                   :en ""}
   :lukiokoulutus-label                           {:fi "Lukiokoulutus"
                                                   :sv "SV: Lukiokoulutus"
                                                   :en ""}
   :yo-tutkinnot-label                            {:fi "Ylioppilastutkinnot"
                                                   :sv "SV:Ylioppilastutkinnot"
                                                   :en ""}
   :amm-perustutkinnot-label                      {:fi "Ammatilliset perustutkinnot"
                                                   :sv "SV: Ammatilliset perustutkinnot"
                                                   :en ""}
   :amm-tutkinnot-label                           {:fi "Ammattitutkinnot"
                                                   :sv "SV: Ammattitutkinnot"
                                                   :en ""}
   :amm-erikoistutkinnot-label                    {:fi "Erikoisammattitutkinnot"
                                                   :sv "SV: Erikoisammattitutkinnot"
                                                   :en ""}
   :alemmat-kk-tutkinnot-label                    {:fi "Alemmat korkeakoulututkinnot"
                                                   :sv "SV: Alemmat korkeakoulututkinnot"
                                                   :en ""}
   :ylemmat-kk-tutkinnot-label                    {:fi "Ylemmät korkeakoulututkinnot"
                                                   :sv "SV: Ylemmät korkeakoulututkinnot"
                                                   :en ""}
   :lisensiaatti-tutkinnot-label                  {:fi "Lisensiaattitutkinnot"
                                                   :sv "SV: Lisensiaattitutkinnot"
                                                   :en ""}
   :tohtori-tutkinnot-label                       {:fi "Tohtoritutkinnot"
                                                   :sv "SV: Tohtoritutkinnot"
                                                   :en ""}
   :itse-syotetty-tutkinnot-label                 {:fi "Suoritus, joka ei ole Koskessa"
                                                   :sv "SV: Suoritus, joka ei ole Koskessa"
                                                   :en ""}
   :koski-followup-label                          {:fi "Lisäkysymykset Koskesta tuoduille tutkinnoille"
                                                   :sv "SV: Lisäkysymykset Koskesta tuoduille tutkinnoille"
                                                   :en ""}
   :itse-syotetty-followup-label                  {:fi "Kysymykset"
                                                   :sv "SV: Kysymykset"
                                                   :en ""}
   :itse-syotetty-tutkinto-group-label            {:fi "Tutkinto"
                                                   :sv "SV: Tutkinto"
                                                   :en ""}
   :tutkinto-followup-label                       {:fi "Tutkinto"
                                                   :sv "SV: Tutkinto"
                                                   :en ""}
   :koulutusohjelma-followup-label                {:fi "Koulutusohjelma"
                                                   :sv "SV: Koulutusohjelma"
                                                   :en ""}
   :oppilaitos-followup-label                     {:fi "Oppilaitos"
                                                   :sv "SV: Oppilaitos"
                                                   :en ""}
   :valmistumispvm-followup-label                 {:fi "Valmistumispäivä"
                                                   :sv "SV: Valmistumispäivä"
                                                   :en ""}
   :itse-syotetty-valimistumispvm-infotext-label  {:fi "Päivämäärä muodossa pp.kk.vvvv, esim. 31.12.2024"
                                                   :sv "SV: Päivämäärä muodossa pp.kk.vvvv, esim. 31.12.2024"
                                                   :en ""}
   :itse-syotetty-liitteet-followup-label         {:fi "Tutkintotodistus"
                                                   :sv "SV: Tutkintotodistus"
                                                   :en ""}
   :itse-syotetty-liitteet-infotext-value         {:fi "Tallenna todistuksesi joko pdf -muodossa tai kuvatiedostona (esim png tai jpeg)"
                                                   :sv "SV: Tallenna todistuksesi joko pdf -muodossa tai kuvatiedostona (esim png tai jpeg)"
                                                   :en ""}})

(defn email-applied-error
  [email preferred-name]
  {:fi [:div.application__validation-error-dialog
        [:p (if (not (string/blank? preferred-name))
              (str "Hei " preferred-name "!")
              "Hei!")]
        [:p "Tässä haussa voit lähettää vain yhden (1) hakemuksen. "
         [:strong "Olet jo lähettänyt hakemuksen"]
         " tähän hakuun ja siksi et voi lähettää toista hakemusta.
         Jos lähetät useampia hakemuksia, viimeisin jätetty hakemus
         jää voimaan ja aiemmin lähettämäsi hakemukset perutaan."]
        [:p "Jos haluat "
         [:strong "muuttaa hakemustasi"]
         ", voit tehdä muokkaukset sähköpostiisi saapuneen hakemuksen muokkauslinkin kautta
         tai vaihtoehtoisesti kirjautumalla Oma Opintopolku -palveluun."]
        [:p "Tarkista myös, että syöttämäsi sähköpostiosoite "
         [:strong email]
         " on varmasti oikein."]
        [:p "Ongelmatilanteissa ole yhteydessä oppilaitokseen johon haet."]]
   :sv [:div.application__validation-error-dialog
        [:p (if (not (string/blank? preferred-name))
              (str "Hej " preferred-name "!")
              "Hej!")]
        [:p "I denna ansökan kan du skicka in endast en (1) ansökan."
         [:strong "Du redan har skickat en ansökning"]
         " i denna ansökan och därför kan du inte skicka en annan
          ansökning. Om du skickar in flera beaktas endast den som
          du skickat in senast och alla tidigare ansökningar raderas."]
        [:p "Om du vill "
         [:strong "ändra din ansökning"]
         " kan du under ansökningstiden göra det via en länk i e-postmeddelandet som du får
         som bekräftelse över din ansökan eller genom att logga in i tjänsten Min Studieinfo."]
        [:p "Kontrollera även att e-postadressen du har angett "
         [:strong email]
         " säkert är korrekt."]
        [:p "Vid eventuella problemsituationer kontakta den läroanstalt du
             söker till."]]
   :en [:div.application__validation-error-dialog
        [:p (if (not (string/blank? preferred-name))
              (str "Dear " preferred-name ",")
              "Dear applicant,")]
        [:p "You can only submit one (1) application form in this application."
         [:strong "You have already submitted an application"]
         " to this admission and therefore cannot submit another
          application. If you submit several applications, only the latest one
          will be taken into consideration and all others will be deleted."]
        [:p "If you want to, you can "
         [:strong "make changes"]
         " to your application during the application period by using
         the link in the confirmation email or by logging in to My Studyinfo."]
        [:p "Please also check that the email address "
         [:strong email]
         " you have given is correct."]
        [:p "If you have any problems, please contact the educational
             institution you are applying to."]]})

(defn email-applied-error-when-modifying
  [email preferred-name]
  {:fi
   [:div.application__validation-error-dialog
    [:div
     [:p (if (not (string/blank? preferred-name))
           (str "Hei " preferred-name "!")
           "Hei!")]
     [:p "Tässä haussa voit lähettää vain yhden (1) hakemuksen. "
      [:strong "Olet jo lähettänyt hakemuksen"]
      " tähän hakuun ja siksi et voi lähettää toista hakemusta.
         Jos lähetät useampia hakemuksia, viimeisin jätetty hakemus
         jää voimaan ja aiemmin lähettämäsi hakemukset perutaan."]
     [:p "Jos haluat "
      [:strong "muuttaa hakemustasi"]
      ", voit tehdä muokkaukset sähköpostiisi saapuneen hakemuksen muokkauslinkin kautta
         tai vaihtoehtoisesti kirjautumalla Oma Opintopolku -palveluun."]
     [:p "Tarkista myös, että syöttämäsi sähköpostiosoite "
      [:strong email]
      " on varmasti oikein."]
     [:p "Ongelmatilanteissa ole yhteydessä oppilaitokseen johon haet."]]]
   :sv [:div.application__validation-error-dialog
        [:p (if (not (string/blank? preferred-name))
              (str "Hej " preferred-name "!")
              "Hej!")]
        [:p "I denna ansökan kan du skicka in endast en (1) ansökan."
         [:strong "Du redan har skickat en ansökning"]
         " i denna ansökan och därför kan du inte skicka en annan
          ansökning. Om du skickar in flera beaktas endast den som
          du skickat in senast och alla tidigare ansökningar raderas."]
        [:p "Om du vill "
         [:strong "ändra din ansökning"]
         " kan du under ansökningstiden göra det via en länk i e-postmeddelandet
         som du får som bekräftelse över din ansökan eller genom att logga in i tjänsten Min Studieinfo."]
        [:p "Kontrollera även att e-postadressen du har angett "
         [:strong email]
         " säkert är korrekt."]
        [:p "Vid eventuella problemsituationer kontakta den läroanstalt du
             söker till."]]
   :en [:div.application__validation-error-dialog
        [:p (if (not (string/blank? preferred-name))
              (str "Dear " preferred-name ",")
              "Dear applicant,")]
        [:p "You can only submit one (1) application form in this application."
         [:strong "You have already submitted an application"]
         " to this admission and therefore cannot submit another
          application. If you submit several applications, only the latest one
          will be taken into consideration and all others will be deleted."]
        [:p "If you want to, you can "
         [:strong "make changes"]
         " to your application during the application period by using
         the link in the confirmation email or by logging in to My Studyinfo."]
        [:p "Please also check that the email address "
         [:strong email]
         " you have given is correct."]
        [:p "If you have any problems, please contact the educational
             institution you are applying to."]]})

(def person-info-module-validation-error-texts
  {:ssn                   {:fi "Henkilötunnuksen on oltava muodossa PPKKVVvälimerkkiNNNT, jossa välimerkki on \"-\" tai \"A\". Myös välimerkit \"Y\" ja \"B\" ovat sallittuja."
                           :sv "Personbeteckningen ska vara i formen DDMMÅÅskiljeteckenNNNT, där skiljetecknet är \"-\" eller \"A\". Också Y och B godkänns som skiljetecken."
                           :en "Your personal identity code has to be in the format DDMMYYintermediatecharacterNNNT, where the intermediate character is \"-\" or \"A\". Additionally, intermediate characters \"Y\" and \"B\" are allowed."}
   :phone                 {:fi "Matkapuhelinnumero on virheellinen. Numero on oltava muodossa 050123456 tai +35850123456."
                           :sv "Din mobiltelefonnummer är fel. Numret ska anges i formen 050123456 eller +35850123456."
                           :en "The mobile phone number is in incorrect format. The number has to be in format 050123456 or +35850123456."}
   :email                 {:fi "Sähköpostiosoitteesi on väärässä muodossa. Sähköpostiosoite on oltava muodossa nimi@osoite.fi."
                           :sv "Din e-postadress är i fel form. E-postadressen ska anges i formen namn@adress.fi."
                           :en "Your email address is in incorrect format. The email address has to be in the format name@address.com."}
   :different-email       {:fi "Sähköpostiosoitteet eivät ole samanlaiset."
                           :sv "E-postadresserna motsvarar inte varandra."
                           :en "The email addresses are not identical."}
   :postal-code           {:fi "Postinumerossa on oltava viisi numeroa."
                           :sv "Postnumret ska innehålla fem siffror."
                           :en "The postal code must include five digits."}
   :postal-office-missing {:fi "Postinumero on virheellinen."
                           :sv "Postnummer är felaktigt."
                           :en "The postal code is incorrect."}
   :main-first-name       {:fi "Kutsumanimen tulee olla yksi etunimistäsi."
                           :sv "Ditt tilltalsnamn ska vara ett av dina förnamn."
                           :en "The preferred name has to be one of your first/given names."}
   :past-date             {:fi "Syntymäajan on oltava muodossa pp.kk.vvvv."
                           :sv "Födelsetiden ska anges i formen dd.mm.åååå."
                           :en "The date of birth has to be in the format dd.mm.yyyy."}
   :email-simple          {:fi "Sähköpostiosoitteesi on väärässä muodossa. Sähköpostiosoite on oltava muodossa nimi@osoite.fi."
                           :sv "Din e-postadress är i fel form. E-postadressen ska anges i formen namn@adress.fi."
                           :en "Your email address is in incorrect format. The email address has to be in the format name@address.com."}})

(defn person-info-validation-error [msg-key]
  (when (some? msg-key)
    (when-let [texts (get person-info-module-validation-error-texts msg-key)]
      {:fi [:div.application__person-info-validation-error-dialog {:class msg-key}
            [:p (:fi texts)]]
       :sv [:div.application__person-info-validation-error-dialog {:class msg-key}
            [:p (:sv texts)]]
       :en [:div.application__person-info-validation-error-dialog {:class msg-key}
            [:p (:en texts)]]})))

(defn ssn-applied-error
  [preferred-name]
  {:fi [:div.application__validation-error-dialog
        [:p (if (not (string/blank? preferred-name))
              (str "Hei " preferred-name "!")
              "Hei!")]
        [:p "Tässä haussa voit lähettää vain yhden (1) hakemuksen. "
         [:strong "Olet jo lähettänyt hakemuksen"]
         " tähän hakuun ja siksi et voi lähettää toista hakemusta.
         Jos lähetät useampia hakemuksia, viimeisin jätetty hakemus
         jää voimaan ja aiemmin lähettämäsi hakemukset perutaan."]
        [:p "Jos haluat "
         [:strong "muuttaa hakemustasi"]
         ", voit tehdä muokkaukset sähköpostiisi saapuneen hakemuksen muokkauslinkin kautta
         tai vaihtoehtoisesti kirjautumalla Oma Opintopolku -palveluun."]
        [:p "Ongelmatilanteissa ole yhteydessä hakemaasi oppilaitokseen."]]
   :sv [:div.application__validation-error-dialog
        [:p (if (not (string/blank? preferred-name))
              (str "Hej " preferred-name "!")
              "Hej!")]
        [:p "I denna ansökan kan du skicka in endast en (1) ansökan."
         [:strong "Du redan har skickat en ansökning"]
         " i denna ansökan och därför kan du inte skicka en annan
          ansökning. Om du skickar in flera beaktas endast den som
          du skickat in senast och alla tidigare ansökningar raderas."]
        [:p "Om du vill "
         [:strong "ändra din ansökning"]
         " kan du under ansökningstiden göra det via en länk i e-postmeddelandet
         som du får som bekräftelse över din ansökan eller genom att logga in i tjänsten Min Studieinfo."]
        [:p "Vid eventuella problemsituationer kontakta den läroanstalt du
         söker till."]]
   :en [:div.application__validation-error-dialog
        [:p (if (not (string/blank? preferred-name))
              (str "Dear " preferred-name ",")
              "Dear applicant,")]
        [:p "You can only submit one (1) application form in this application."
         [:strong "You have already submitted an application"]
         " to this admission and therefore cannot submit another
          application. If you submit several applications, only the latest one
          will be taken into consideration and all others will be deleted."]
        [:p "If you want to, you can "
         [:strong "make changes"]
         " to your application during the application period by using
         the link in the confirmation email or by logging in to My Studyinfo."]
        [:p "If you have any problems, please contact the educational
         institution."]]})
