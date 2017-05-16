(ns ataru.translations.email-confirmation)

(def email-confirmation-translations
  {:application-received-subject  {:fi "Opintopolku - Hakemuksesi on vastaanotettu"
                                   :sv "Opintopolku - Din ansökan har tagits emot"
                                   :en "Opintopolku - Your application has been received"}
   :application-edited-subject    {:fi "Opintopolku - Hakemuksesi on päivitetty"
                                   :sv "Opintopolku - Din ansökan har updaterats"
                                   :en "Opintopolku - Your application has been received"}
   :application-received-text     {:fi "Hakemuksesi on vastaanotettu."
                                   :en "Your application has been received."
                                   :sv "Din ansökan har tagits emot."}
   :application-edited-text       {:fi "Hakemuksesi on päivitetty."
                                   :en "Your application has been updated."
                                   :sv "Din ansökan har uppdaterats."}
   :modify-link-text              {:fi "Ylläolevan linkin kautta voit katsella ja muokata hakemustasi."
                                   :en "You can view and modify your application using the link above."
                                   :sv "Du kan se och redigera din ansökan via länken ovan."}
   :do-not-share-warning-text     {:fi "Älä jaa linkkiä ulkopuolisille. Jos käytät yhteiskäyttöistä tietokonetta, muista kirjautua ulos sähköpostiohjelmasta."
                                   :en "Do not share the link with others. If you are using a public or shared computer, remember to log out of the email application."
                                   :sv "Dela inte länken vidare till utomstående. Om du använder en offentlig dator, kom ihåg att logga ut från e-postprogrammet."}
   :best-regards                  {:fi "terveisin"
                                   :sv "Med vänliga hälsningar"
                                   :en "Best Regards"}
   :application-can-be-found-here {:fi "Hakemuksesi löytyy täältä"
                                   :sv "Din ansökan kan hittas här"
                                   :en "You can find your application here"}})
