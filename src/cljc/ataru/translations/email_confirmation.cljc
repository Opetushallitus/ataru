(ns ataru.translations.email-confirmation
  (:require [ataru.translations.common-translations :as t]))

(def email-confirmation-translations
  (merge {:application-received-subject  {:fi "Opintopolku - Hakemuksesi on vastaanotettu"
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
          :best-regards                  {:fi "terveisin"
                                          :sv "Med vänliga hälsningar"
                                          :en "Best Regards"}
          :application-can-be-found-here {:fi "Hakemuksesi löytyy täältä"
                                          :sv "Din ansökan kan hittas här"
                                          :en "You can find your application here"}}
         (select-keys t/translations [:modify-link-text :do-not-share-warning-text])))
