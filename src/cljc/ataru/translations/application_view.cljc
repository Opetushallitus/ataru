(ns ataru.translations.application-view)

(def application-view-translations
  {:application-period            {:fi "Hakuaika"
                                   :sv "Ansökningstid"
                                   :en "Application period"}
   :not-within-application-period {:fi "hakuaika ei ole käynnissä"
                                   :sv "inte inom ansökningstiden"
                                   :en "not within application period"}
   :continuous-period             {:fi "Jatkuva haku"
                                   :sv "kontinuerlig ansökningstid"
                                   :en "Continuous application period"}
   :add-row                       {:fi "Lisää rivi"
                                   :sv "Lägg till rad"
                                   :en "Add row"}
   :remove-row                    {:fi "Poista rivi"
                                   :sv "Ta bort rad"
                                   :en "Remove row"}
   :add-more                      {:fi "Lisää..."
                                   :sv "Lägg till..."
                                   :en "Add more..."}
   :cannot-edit-personal-info     {:fi "Jos haluat muuttaa henkilötietojasi, ota yhteyttä hakemaasi oppilaitokseen."
                                   :en "To update your personal information, please contact the institution you're applying to."
                                   :sv "Om du vill ändra dina kontaktuppgifter, ta då kontakt med den läroanstalt som du har sökt till."}
   :not-edited                    {:fi "(ei muokattu)"
                                   :en "(unchanged)"
                                   :sv "(inte redigerat)"}})
