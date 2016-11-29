(ns ataru.translations.email-confirmation)

(def email-confirmation-translations
  {:subject                       {:fi "Opintopolku - Hakemuksesi on vastaanotettu"
                                   :sv "Opintopolku - Din ansökan har tagits emot"
                                   :en "Opintopolku - Your application has been received"}
   :application-received-text     {:fi "Hakemuksesi on vastaanotettu."
                                   :en "Your application has been received."
                                   :sv "Din ansökan har tagits emot."}
   :application-edited-text       {:fi "Hakemuksesi on päivitetty."
                                   :en "Your application has been updated."
                                   :sv "Din ansökan har uppdaterats."}
   :modify-link-text              {:fi "Allaolevan linkin kautta löydät hakemuksesi. Voit muokata tietojasi hakuajan päättymiseen asti."
                                   :en "You can view and modify your application using the link below. Application can be modified till the end of the application period."
                                   :sv "Via länken nedan hittar du din ansökan. Du kan granska och bearbeta din ansökningsblankett under ansökningstiden "}
   :best-regards                  {:fi "terveisin"
                                   :sv "Med vänliga hälsningar"
                                   :en "Best Regards"}
   :application-can-be-found-here {:fi "Hakemuksesi löytyy täältä"
                                   :sv "Din ansökan kan hittas här"
                                   :en "You can find your application here"}})
