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
                                       :en "Add more..."}})
