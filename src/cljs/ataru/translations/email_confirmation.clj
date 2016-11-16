(ns ataru.translations.email-confirmation)

(def email-confirmation-translations
  {:subject                       {:fi "Opintopolku - Hakemuksesi on vastaanotettu"
                                   :sv "Opintopolku - Din ansökan har tagits emot"
                                   :en "Opintopolku - Your application has been received"}
   :application-received-text     {:fi "Hakemuksesi on vastaanotettu."
                                   :en "Your application has been received."
                                   :sv "Din ansökan har tagits emot."}
   :modify-link-text              {:fi "Allaolevan linkin kautta löydät hakemuksesi. Voit milloin tahansa käydä muokkaamassa tietojasi aina siihen asti, kunnes hakemus on otettu käsittelyyn."
                                   :en "You can view and modify your application using the link below. Application can be modified until it is being reviewed."
                                   :sv "Via länken nedan hittar du din ansökan. Du kan alltid gå till mass redigera dina data på alla gånger tills Kunes ansökan övervägs."}
   :best-regards                  {:fi "terveisin"
                                   :sv "Med vänliga hälsningar"
                                   :en "Best Regards"}
   :application-can-be-found-here {:fi "Hakemuksesi löytyy täältä"
                                   :sv "Din ansökan kan hittas här"
                                   :en "You can find your application here"}})
