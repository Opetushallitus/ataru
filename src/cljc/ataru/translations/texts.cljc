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
   :payment-button                              {:fi "Siirry maksamaan"
                                                 :sv "Gå till betalning"
                                                 :en "Go to payment"}
   :poista                                      {:fi "Poista"
                                                 :sv "Radera"
                                                 :en "Poista"}
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
   :archived                                    {:fi "Arkistoitu"
                                                 :sv "Arkiverad"
                                                 :en "Archived"}
   :required                                    {:fi "(pakollinen tieto)"
                                                 :sv "(obligatorisk uppgift)"
                                                 :en "(mandatory information)"}
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
   :ht-lander-header                            {:fi "Valitse kuinka haluat kirjautua lomakkeelle"
                                                 :sv "Valitse kuinka haluat kirjautua lomakkeelle sv"
                                                 :en "Valitse kuinka haluat kirjautua lomakkeelle en"}
   :ht-kirjaudu-sisaan                          {:fi "Kirjaudu sisään"
                                                 :sv "Kirjaudu sisään sv"
                                                 :en "Kirjaudu sisään en"}
   :ht-ilman-kirjautumista                      {:fi "Jatka ilman kirjautumista"
                                                 :sv "Jatka ilman kirjautumista sv"
                                                 :en "Jatka ilman kirjautumista en"}
   :ht-tunnistaudu-ensin-header                 {:fi "Tunnistaudu ensin"
                                                 :sv "Tunnistaudu ensin sv"
                                                 :en "Tunnistaudu ensin en"}
   :ht-tunnistaudu-ensin-text                   {:fi "Kun tunnistaudut pankkitunnuksilla tai varmennekortilla, tuodaan henkilötietosi hakulomakkeelle automaattisesti."
                                                 :sv "Kun tunnistaudut pankkitunnuksilla tai varmennekortilla, tuodaan henkilötietosi hakulomakkeelle automaattisesti. sv"
                                                 :en "Kun tunnistaudut pankkitunnuksilla tai varmennekortilla, tuodaan henkilötietosi hakulomakkeelle automaattisesti. en"}
   :ht-jatka-tunnistautumatta-header            {:fi "Täytä hakulomake ilman tunnistautumista"
                                                 :sv "Täytä hakulomake ilman tunnistautumista sv"
                                                 :en "Täytä hakulomake ilman tunnistautumista en"}
   :ht-jatka-tunnistautumatta-text              {:fi "Valitsemalla tämän vaihtoehdon syötät hakulomakkeelle käsin kaikki vaaditut tiedot."
                                                 :sv "Valitsemalla tämän vaihtoehdon syötät hakulomakkeelle käsin kaikki vaaditut tiedot. sv"
                                                 :en "Valitsemalla tämän vaihtoehdon syötät hakulomakkeelle käsin kaikki vaaditut tiedot. en"}
   :ht-tai                                      {:fi "TAI"
                                                 :sv "ELLER"
                                                 :en "OR"}
   :ht-logout-confirmation-header               {:fi "Haluatko kirjautua ulos?"
                                                 :sv "Haluatko kirjautua ulos? sv"
                                                 :en "Haluatko kirjautua ulos? en"}
   :ht-logout-confirmation-text                 {:fi "Jos kirjaudut ulos, täyttämiäsi tietoja ei tallenneta. Et voi tallentaa hakemustasi keskeneräisenä."
                                                 :sv "Jos kirjaudut ulos, täyttämiäsi tietoja ei tallenneta. Et voi tallentaa hakemustasi keskeneräisenä. sv"
                                                 :en "Jos kirjaudut ulos, täyttämiäsi tietoja ei tallenneta. Et voi tallentaa hakemustasi keskeneräisenä. en"}
   :ht-logout-confirmation-text-submitted       {:fi "Olet jo jättänyt hakemuksen, eli et menetä mitään kirjautumalla ulos. :)"
                                                 :sv "Olet jo jättänyt hakemuksen, eli et menetä mitään kirjautumalla ulos. :) sv"
                                                 :en "Olet jo jättänyt hakemuksen, eli et menetä mitään kirjautumalla ulos. :) en"}
   :ht-kirjaudu-ulos                            {:fi "Kirjaudu ulos"
                                                 :sv "Kirjaudu ulos sv"
                                                 :en "Kirjaudu ulos en"}
   :ht-has-applied-lander-header                {:fi "Olet jo lähettänyt hakemuksen hakuun:"
                                                 :sv "Olet jo lähettänyt hakemuksen hakuun: sv"
                                                 :en "Olet jo lähettänyt hakemuksen hakuun: en"}
   :ht-has-applied-lander-paragraph1            {:fi "Tässä haussa voit lähettää vain yhden (1) hakemuksen. Olet jo lähettänyt hakemuksen tähän hakuun ja siksi et voi lähettää toista hakemusta."
                                                 :sv "Tässä haussa voit lähettää vain yhden (1) hakemuksen. Olet jo lähettänyt hakemuksen tähän hakuun ja siksi et voi lähettää toista hakemusta. sv"
                                                 :en "Tässä haussa voit lähettää vain yhden (1) hakemuksen. Olet jo lähettänyt hakemuksen tähän hakuun ja siksi et voi lähettää toista hakemusta. en"}
   :ht-has-applied-lander-paragraph2            {:fi "Jos haluat muuttaa hakemustasi niin löydät muokkauslinkin sähköpostiviestistä jonka sait lähettäessäsi edellisen hakemuksen."
                                                 :sv "Jos haluat muuttaa hakemustasi niin löydät muokkauslinkin sähköpostiviestistä jonka sait lähettäessäsi edellisen hakemuksen. sv"
                                                 :en "Jos haluat muuttaa hakemustasi niin löydät muokkauslinkin sähköpostiviestistä jonka sait lähettäessäsi edellisen hakemuksen. en"}
   :ht-has-applied-lander-paragraph3            {:fi "Ongelmatilanteissa ole yhteydessä hakemaasi oppilaitokseen."
                                                 :sv "Ongelmatilanteissa ole yhteydessä hakemaasi oppilaitokseen. sv"
                                                 :en "Ongelmatilanteissa ole yhteydessä hakemaasi oppilaitokseen. en"}
   :ht-siirry-oma-opintopolkuun                 {:fi "Katso hakemustasi Oma Opintopolussa"
                                                 :sv "Katso hakemustasi Oma Opintopolussa sv"
                                                 :en "Katso hakemustasi Oma Opintopolussa en"}
   :ht-jatka-palvelun-kayttoa                   {:fi "Jatka palvelun käyttöä"
                                                 :sv "Jatka palvelun käyttöä sv"
                                                 :en "Jatka palvelun käyttöä en"}
   :ht-session-expiring-header                  {:fi "Istuntosi on vanhentumassa"
                                                 :sv "Istuntosi on vanhentumassa sv"
                                                 :en "Istuntosi on vanhentumassa en"}
   :ht-session-expiring-text                    {:fi "Sinut kirjataan 5 min päästä ulos palvelusta ja hakemustasi ei tallenneta, jos et jatka palvelun käyttöä"
                                                 :sv "Sinut kirjataan 5 min päästä ulos palvelusta ja hakemustasi ei tallenneta, jos et jatka palvelun käyttöä sv"
                                                 :en "Sinut kirjataan 5 min päästä ulos palvelusta ja hakemustasi ei tallenneta, jos et jatka palvelun käyttöä en"}
   :ht-session-expired-header                   {:fi "Istunto on vanhentunut"
                                                 :sv "Istunto on vanhentunut sv"
                                                 :en "Istunto on vanhentunut en"}
   :ht-session-expired-text                     {:fi "Istunto vanhentui ja sinut on kirjattu ulos palvelusta. Hakemustasi ei ole tallennettu."
                                                 :sv "Istunto vanhentui ja sinut on kirjattu ulos palvelusta. Hakemustasi ei ole tallennettu. sv"
                                                 :en "Istunto vanhentui ja sinut on kirjattu ulos palvelusta. Hakemustasi ei ole tallennettu. en"}
   :ht-session-expired                          {:fi "Siirry Opintopolun etusivulle"
                                                 :sv "Siirry Opintopolun etusivulle sv"
                                                 :en "Siirry Opintopolun etusivulle en"}})

(def oppiaine-translations
  {:oppiaine-a {:fi "Äidinkieli ja kirjallisuus"
                :sv "Modersmål och litteratur"
                :en "Äidinkieli ja kirjallisuus"}
   :oppiaine-a1 {:fi "A1-kieli"
                 :sv "A1-språk"
                 :en "A1-kieli"}
   :oppiaine-a2 {:fi "A2-kieli"
                 :sv "A2-språk"
                 :en "A2-kieli"}
   :oppiaine-b1 {:fi "B1-kieli"
                 :sv "B1-språk"
                 :en "B1-kieli"}
   :oppiaine-b2 {:fi "B2-kieli"
                 :sv "B2-språk"
                 :en "B2-kieli"}
   :oppiaine-b3 {:fi "B3-kieli"
                 :sv "B3-språk"
                 :en "B3-kieli"}
   :oppiaine-ma {:fi "Matematiikka"
                 :sv "Matematik"
                 :en "Matematiikka"}
   :oppiaine-bi {:fi "Biologia"
                 :sv "Biologi"
                 :en "Biologia"}
   :oppiaine-ge {:fi "Maantieto"
                 :sv "Geografi"
                 :en "Maantieto"}
   :oppiaine-fy {:fi "Fysiikka"
                 :sv "Fysik"
                 :en "Fysiikka"}
   :oppiaine-ke {:fi "Kemia"
                 :sv "Kemi"
                 :en "Kemia"}
   :oppiaine-tt {:fi "Terveystieto"
                 :sv "Hälsokunskap"
                 :en "Terveystieto"}
   :oppiaine-ty {:fi "Uskonto tai elämänkatsomustieto"
                 :sv "Religion eller livsåskådningskunskap"
                 :en "Uskonto tai elämänkatsomustieto"}
   :oppiaine-hi {:fi "Historia"
                 :sv "Historia"
                 :en "Historia"}
   :oppiaine-yh {:fi "Yhteiskuntaoppi"
                 :sv "Samhällslära"
                 :en "Yhteiskuntaoppi"}
   :oppiaine-mu {:fi "Musiikki"
                 :sv "Musik"
                 :en "Musiikki"}
   :oppiaine-ku {:fi "Kuvaamataito"
                 :sv "Bildkonst"
                 :en "Kuvaamataito"}
   :oppiaine-ka {:fi "Käsityö"
                 :sv "Slöjd"
                 :en "Käsityö"}
   :oppiaine-li {:fi "Liikunta"
                 :sv "Gymnastik"
                 :en "Liikunta"}
   :oppiaine-ko {:fi "Kotitalous"
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
                                             :en "EN: Valitse tämä vain silloin, kun olet keskeyttänyt perusopetuksen. \n\nHaet automaattisesti harkintaan perustuvassa valinnassa. "}
   })

(def base-education-cotinuous-admissions-module-texts
  {:section-title                           {:fi "Pohjakoulutuksesi"
                                             :sv "Grundutbildning"
                                             :en "Your educational background"}
   :choose-base-education                   {:fi "Valitse yksi pohjakoulutus, jolla haet koulutukseen"
                                             :sv "Välj den grundutbildning med vilken du söker till utbildningen"
                                             :en "Fill in the education that you have completed"}
   :choose-base-education-info              {:fi "Jos olet suorittanut useamman pohjakoulutuksen, \nvalitse ylin koulutuksesi."
                                             :sv "Om du har avlagt mer än en grundutbildning, välj \ndin högsta utbildning."
                                             :en "If you have completed several qualifications, fill in \nthe highest level that you have completed."}
   :base-education                          {:fi "Perusopetuksen oppimäärä"
                                             :sv "Den grundläggande utbildningens lärokurs"
                                             :en "EN: Perusopetuksen oppimäärä"}
   })

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
  {:valintakasittelymerkinta                                 {:fi "Valintakäsittelymerkintä"
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
   :return-latest                                           {:fi "Palautettava viimeistään"
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
   :payment-obligation                                       {:fi "Maksuvelvollisuus"
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
   :single-information-request-message-sent                   {:fi "Viesti lisätty lähetysjonoon!"
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
   :student-number                                            {:fi "Oppijanumero"
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
   :tutu-amount-label                                        {:fi "Maksun määrä"
                                                              :sv ""
                                                              :en ""}
   :tutu-total-paid-label                                    {:fi "Yhteissumma"
                                                              :sv ""
                                                              :en ""}
   :tutu-due-label                                           {:fi "Eräpäivä"
                                                              :sv ""
                                                              :en ""}
   :tutu-maksupyynto-header                                  {:fi "Maksupyyntö"
                                                              :sv ""
                                                              :en ""}
   :tutu-processing-header                                   {:fi "Käsittelymaksu:"
                                                              :sv ""
                                                              :en ""}
   :tutu-decision-header                                     {:fi "Päätösmaksu:"
                                                              :sv ""
                                                              :en ""}
   :tutu-maksupyynto-recipient                               {:fi "Vastaanottaja:"
                                                              :sv ""
                                                              :en ""}
   :tutu-maksupyynto-amount                                  {:fi "Summa"
                                                              :sv ""
                                                              :en ""}
   :tutu-maksupyynto-message                                 {:fi "Viesti:"
                                                              :sv ""
                                                              :en ""}
   :tutu-maksupyynto-send-button                             {:fi "Lähetä maksupyyntö"
                                                              :sv ""
                                                              :en ""}
   :tutu-maksupyynto-again-button                            {:fi "Lähetä uudelleen"
                                                              :sv ""
                                                              :en ""}
   :tutu-kasittelymaksu-button                               {:fi "Uudelleenlähetä käsittelymaksu"
                                                              :sv ""
                                                              :en ""}
   :tutu-invoice-notfound                                    {:fi "Maksun tietoja ei löydy"
                                                              :sv ""
                                                              :en ""}
   :tutu-payment-active                                      {:fi "Avoin"
                                                              :sv ""
                                                              :en ""}
   :tutu-payment-paid                                        {:fi "Maksettu"
                                                              :sv ""
                                                              :en ""}
   :tutu-payment-overdue                                     {:fi "Eräpäivä ylitetty"
                                                              :sv ""
                                                              :en ""}
   :tutu-payment-unknown                                     {:fi "Maksun tilaa ei tiedetä"
                                                              :sv ""
                                                              :en ""}
   :tutu-amount-input-placeholder                            {:fi "Anna summa muodossa 123 tai 123.00"
                                                              :sv ""
                                                              :en ""}
   :tutu-payment-download-receipt                            {:fi "Lataa kuitti"
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
                                                              :en "EN: Virhe hakemuksen valintojen hakemisessa"}})

(def state-translations
  {:active                 {:fi "Aktiivinen"
                            :sv "Aktiv"
                            :en "Active"}
   :passive                {:fi "Passiivinen"
                            :sv "Passiv"
                            :en "Inactive"}
   :unprocessed            {:fi "Käsittelemättä"
                            :sv "Obehandlad"
                            :en "Unprocessed"}
   :processing             {:fi "Käsittelyssä"
                            :sv "Under behandling"
                            :en "Under process"}
   :invited-to-interview   {:fi "Kutsuttu haast."
                            :sv "Kallad till intervju"
                            :en "Invited to interview"}
   :invited-to-exam        {:fi "Kutsuttu valintak."
                            :sv "Kallad till urvalsprov"
                            :en "Invited to entrance examination"}
   :evaluating             {:fi "Arvioinnissa"
                            :sv "Under bedömning"
                            :en "Under evaluation"}
   :valintaesitys          {:fi "Valintaesitys"
                            :sv "Antagningsförslag"
                            :en "Admission proposal"}
   :processed              {:fi "Käsitelty"
                            :sv "Behandlad"
                            :en "Processed"}
   :information-request    {:fi "Täydennyspyyntö"
                            :sv "Begäran om komplettering"
                            :en "Information request"}
   :incomplete             {:fi "Kesken"
                            :sv "Inte färdig"
                            :en "Incomplete"}
   :not-done               {:fi "Ei tehty"
                            :sv "Inte gjort"
                            :en "Not done"}
   :selection-proposal     {:fi "Valintaesitys"
                            :sv "Antagningsförslag"
                            :en "Selected (pending)"}
   :reserve                {:fi "Varalla"
                            :sv "På reserv"
                            :en "On reserve place"}
   :cancelled              {:fi "Peruuntunut"
                            :sv "Inställd"
                            :en "Cancelled"}
   :selected               {:fi "Hyväksytty"
                            :sv "Godkänd"
                            :en "Selected"}
   :accepted               {:fi "Hyväksytty"
                            :sv "Accepterad"
                            :en "Accepted"}
   :rejected               {:fi "Hylätty"
                            :sv "Underkänd"
                            :en "Rejected"}
   :accepted-from-reserve  {:fi "Varasijalta hyväksytty"
                            :sv "Godkänd från reservplats"
                            :en "Accepted from reserve"}
   :bindingly-received     {:fi "Vastaanottanut sitovasti"
                            :sv "Mottagit bindande"}
   :present-whole-academic-year {:fi "Läsnä koko lukuvuoden"
                                 :sv "Närvarande hela läsåret"
                                 :en "Present whole academic year"}
   :away-whole-acedemic-year {:fi "Poissa koko lukuvuoden"
                              :sv "Frånvarande hela läsåret"
                              :en "Away whole academic year"}
   :cancelled-by-someone   {:fi "Peruutettu"
                            :sv "Annullerats"}
   :cancelled-by-applicant {:fi "Perunut"
                            :sv "Annullerad"
                            :en "Cancelled by applicant"}
   :present-autumn         {:fi "Läsnä syksyn"
                            :sv "Närvarande hösten"
                            :en "Present during autumn"}
   :away-autumn            {:fi "Poissa syksyn"
                            :sv "Frånvarande hösten"
                            :en "Away during autumn"}
   :present-spring         {:fi "Läsnä kevään"
                            :sv "Närvarande våren"
                            :en "Present during spring"}
   :away-spring            {:fi "Poissa kevään"
                            :sv "Frånvarande våren"
                            :en "Away during spring"}
   :accepted-harkinnanvaraisesti {:fi "Harkinnanvaraisesti hyväksytty"
                                  :sv "Godkänd enligt prövning"}
   :not-enrolled           {:fi "Ei ilmoittautunut"
                            :sv "Ej anmält sig"
                            :en "Not enrolled"}
   :not-received-during-period {:fi "Ei vastaanotettu määrä-aikana"
                                :sv "Ej mottagit inom utsatt tid"}
   :received-another       {:fi "Ottanut vastaan toisen paikan"
                            :sv "Tagit emot annan plats"}
   :conditionally-received {:fi "Ehdollisesti vastaanottanut"
                            :sv "Mottagit villkorligt"}
   :unreviewed             {:fi "Tarkastamatta"
                            :sv "Inte granskad"
                            :en "Unreviewed"}
   :fulfilled              {:fi "Täyttyy"
                            :sv "Fylls"
                            :en "Meets requirement"}
   :unfulfilled            {:fi "Ei täyty"
                            :sv "Fylls inte"
                            :en "Does nor meet requirement"}
   :eligible               {:fi "Hakukelpoinen"
                            :sv "Ansökningsbehörig"
                            :en "Eligible"}
   :uneligible             {:fi "Ei hakukelpoinen"
                            :sv "Inte ansökningsbehörig"
                            :en "Not eligible"}
   :conditionally-eligible {:fi "Ehdollisesti hakukelpoinen"
                            :sv "Villkorligt ansökningsbehörig"
                            :en "Conditionally eligible"}
   :obligated              {:fi "Velvollinen"
                            :sv "Förpliktad"
                            :en "Obligated"}
   :not-obligated          {:fi "Ei velvollinen"
                            :sv "Inte förpliktad"
                            :en "Not obligated"}
   :processing-state       {:fi "Käsittelyvaihe"
                            :sv "Behandlingsskede"
                            :en "State of processing"}
   :language-requirement   {:fi "Kielitaitovaatimus"
                            :sv "Språkkunskapskrav"
                            :en "Language requirement"}
   :only-edited-hakutoiveet {:fi "Muokatut hakutoiveet"
                             :sv "Bearbetad ansökningsönskemål"
                             :en "Edited study program"}
   :degree-requirement     {:fi "Tutkinnon kelpoisuus"
                            :sv "Examens behörighet"
                            :en "Degree requirement"}
   :eligibility-state      {:fi "Hakukelpoisuus"
                            :sv "Ansökningsbehörighet"
                            :en "Eligibility"}
   :payment-obligation     {:fi "Maksuvelvollisuus"
                            :sv "Betalningsskyldighet"
                            :en "Obligated to pay"}
   :selection-state        {:fi "Valinta"
                            :sv "Antagning"
                            :en "Selection"}
   :not-checked            {:fi "Tarkastamatta"
                            :sv "Inte granskad"
                            :en "Not checked"}
   :checked                {:fi "Tarkistettu"
                            :sv "Granskad"
                            :en "Checked"}
   :incomplete-answer      {:fi "Puutteellinen"
                            :sv "Bristfällig"
                            :en "Incomplete"}
   :overdue                {:fi "Myöhässä"
                            :sv "Försenad"
                            :en "Overdue"}
   :no-attachment-required {:fi "Ei liitepyyntöä"
                            :sv "Ingen begäran om bilagor"
                            :en "No attachment requirement"}
   :incomplete-attachment  {:fi "Puutteellinen liite"
                            :sv "Bristfällig bilaga"
                            :en "Insufficient attachment"}
   :attachment-missing     {:fi "Liite puuttuu"
                            :sv "Bilaga fattas"
                            :en "Attachment missing"}
   :processing-fee-overdue {:fi "Käsittely maksamatta"
                            :sv "Käsittely maksamatta (sv) TODO"
                            :en "Käsittely maksamatta (en) TODO"}
   :processing-fee-paid    {:fi "Käsittely maksettu"
                            :sv "Käsittely maksettu (sv) TODO"
                            :en "Käsittely maksettu (en) TODO"}
   :decision-fee-outstanding {:fi "Päätösmaksu avoin"
                              :sv "Päätösmaksu avoin (sv) TODO"
                              :en "Päätösmaksu avoin (en) TODO"}
   :decision-fee-overdue   {:fi "Päätös maksamatta"
                            :sv "Päätös maksamatta (sv) TODO"
                            :en "Päätös maksamatta (en) TODO"}
   :decision-fee-paid      {:fi "Päätös maksettu"
                            :sv "Päätös maksettu (sv) TODO"
                            :en "Päätös maksettu (en) TODO"}
   :multiple-values        {:fi "Monta arvoa"
                            :sv "Multipla värden"
                            :en "Multiple values"}
   :attachments-tab-info   {:fi "Kaikkien hakukohteiden liitetiedot eivät välttämättä näy tässä, mikäli oppilaitos ei ole tallentanut tietoja."
                            :sv "Alla uppgifter om bilagor syns nödvändigtvis inte om läroanstalten inte sparat uppgifterna."
                            :en "EN: Kaikkien hakukohteiden liitetiedot eivät välttämättä näy tässä, mikäli oppilaitos ei ole tallentanut tietoja."}})

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
   :sent-at                  {:fi "Lähetysaika"
                              :sv "Sändningstid"
                              :en "EN: Lähetysaika"}
   :application-state        {:fi "Hakemuksen tila"
                              :sv "Ansökans status"
                              :en "EN: Hakemuksen tila"}
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
                              :en "EN: Maksuvelvollisuus"}
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
                              :sv "SV: Oppijanumero"
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
         " niin löydät muokkauslinkin sähköpostiviestistä jonka sait
         jättäessäsi edellisen hakemuksen."]
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
         " hittar du bearbetningslänken i e-postmeddelandet som du fick när
          du skickade din tidigare ansökning."]
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
        [:p "If you want to "
         [:strong "make changes"]
         " to your previous application, you can do so by clicking the link
          in the confirmation email you have received with your earlier
          application."]
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
         " niin löydät muokkauslinkin sähköpostiviestistä jonka sait
         jättäessäsi edellisen hakemuksen."]
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
         " hittar du bearbetningslänken i e-postmeddelandet som du fick när
          du skickade din tidigare ansökning."]
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
        [:p "If you want to "
         [:strong "make changes"]
         " to your previous application, you can do so by clicking the link
          in the confirmation email you have received with your earlier
          application."]
        [:p "Please also check that the email address "
         [:strong email]
         " you have given is correct."]
        [:p "If you have any problems, please contact the educational
             institution you are applying to."]]})

(def person-info-module-validation-error-texts
  {:ssn                   {:fi "Henkilötunnus on oltava muodossa PPKKVVzNNNT, jossa z on \"-\" tai \"A\"."
                           :sv "Personbeteckningen ska vara i formen DDMMÅÅzNNNT, där z är \"-\" eller \"A\"."
                           :en "Your identification number has to be in format DDMMYYzNNNT, where the character z is \"-\" or \"A\"."}
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
                           :en "The preferred name has to be on of your first/given names."}
   :past-date             {:fi "Syntymäajan on oltava muodossa pp.kk.vvvv."
                           :sv "Födelsetiden ska anges i formen dd.mm.åååå."
                           :en "The date of birth has to be in the format dd.mm.yyyy."}
   :email-simple          {:fi "Sähköpostiosoitteesi on väärässä muodossa. Sähköpostiosoite on oltava muodossa nimi@osoite.fi."
                           :sv "Din e-postadress är i fel form. E-postadressen ska anges i formen namn@adress.fi."
                           :en "Your email address is in incorrect format. The email address has to be in the format name@address.com."}
   })

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
         " niin löydät muokkauslinkin sähköpostiviestistä jonka sait
         jättäessäsi edellisen hakemuksen."]
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
         " hittar du bearbetningslänken i e-postmeddelandet som du fick när
          du skickade din tidigare ansökning."]
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
        [:p "If you want to "
         [:strong "make changes"]
         " to your previous application, you can do so by clicking the link
          in the confirmation email you have received with your earlier
          application."]
        [:p "If you have any problems, please contact the educational
         institution."]]})
