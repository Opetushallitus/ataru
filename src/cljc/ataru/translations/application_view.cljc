(ns ataru.translations.application-view)

(def application-view-translations
  {:application-period                {:fi "Hakuaika"
                                       :sv "Ansökningstid"
                                       :en "Application period"}
   :not-within-application-period     {:fi "hakuaika ei ole käynnissä"
                                       :sv "inte inom ansökningstiden"
                                       :en "not within application period"}
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
   :add-more                          {:fi "Lisää..."
                                       :sv "Lägg till..."
                                       :en "Add more..."}
   :feedback-header                   {:fi "Hei, kerro vielä mitä pidit hakulomakkeesta!"
                                       :en "Hi! Care to take a moment to rate our application form?"}
   :feedback-disclaimer               {:fi "Yhteystietojasi ei käytetä tai yhdistetä palautteen tietoihin."
                                       :en "Your personal information is not sent or associated with the feedback given."}
   :feedback-ratings                  {:fi {1 "Huono"
                                            2 "Välttävä"
                                            3 "Tyydyttävä"
                                            4 "Hyvä"
                                            5 "Kiitettävä"}
                                       :en {1 "Poor"
                                            2 "Passable"
                                            3 "OK"
                                            4 "Good"
                                            5 "Excellent"}}
   :feedback-text-placeholder         {:fi "Anna halutessasi kehitysideoita tai kommentteja hakijan palvelusta"
                                       :en "Feel free to also share your comments or ideas regarding the service"}
   :feedback-send                     {:fi "Lähetä palaute"
                                       :en "Send feedback"}
   :feedback-thanks                   {:fi "Kiitos palautteestasi!"
                                       :en "Thank you for your feedback!"}})
