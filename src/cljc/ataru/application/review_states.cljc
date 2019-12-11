(ns ataru.application.review-states
  (:require [ataru.translations.texts :refer [state-translations
                                              kevyt-valinta-state-translations
                                              kevyt-valinta-vastaanotto-tila-translations
                                              kevyt-valinta-ilmoittautumisen-tila-translations]]
            [ataru.util :as util]
            [clojure.set :refer [difference]]))

(def application-review-states
  [["active" (:active state-translations)]
   ["inactivated" (:passive state-translations)]])

(def initial-application-review-state "active")

(def application-hakukohde-processing-states
  [["unprocessed" (:unprocessed state-translations)]
   ["processing" (:processing state-translations)]
   ["invited-to-interview" (:invited-to-interview state-translations)]
   ["invited-to-exam" (:invited-to-exam state-translations)]
   ["evaluating" (:evaluating state-translations)]
   ["processed" (:processed state-translations)]
   ["information-request" (:information-request state-translations)]])

(def initial-application-hakukohde-processing-state "unprocessed")

(def application-hakukohde-selection-states
  [["incomplete" (:incomplete state-translations)]
   ["selection-proposal" (:selection state-translations)]
   ["reserve" (:reserve state-translations)]
   ["selected" (:selected state-translations)]
   ["rejected" (:rejected state-translations)]])

(def kevyt-valinta-selection-state
  {"HYLATTY"                (:kevyt-valinta/hylatty kevyt-valinta-state-translations)
   "VARALLA"                (:kevyt-valinta/varalla kevyt-valinta-state-translations)
   "PERUUNTUNUT"            (:kevyt-valinta/peruuntunut kevyt-valinta-state-translations)
   "VARASIJALTA_HYVAKSYTTY" (:kevyt-valinta/varasijalta-hyvaksytty kevyt-valinta-state-translations)
   "HYVAKSYTTY"             (:kevyt-valinta/hyvaksytty kevyt-valinta-state-translations)
   "PERUNUT"                (:kevyt-valinta/perunut kevyt-valinta-state-translations)
   "PERUUTETTU"             (:kevyt-valinta/peruutettu kevyt-valinta-state-translations)
   "KESKEN"                 (:kevyt-valinta/kesken kevyt-valinta-state-translations)})

(def vastaanotto-tila-selection-state
  {"EI_VASTAANOTETTU_MAARA_AIKANA" (:kevyt-valinta/ei-vastaanotettu-maaraaikana kevyt-valinta-vastaanotto-tila-translations)
   "PERUNUT"                       (:kevyt-valinta/perunut kevyt-valinta-vastaanotto-tila-translations)
   "PERUUTETTU"                    (:kevyt-valinta/peruutettu kevyt-valinta-vastaanotto-tila-translations)
   "OTTANUT_VASTAAN_TOISEN_PAIKAN" (:kevyt-valinta/ottanut-vastaan-toisen-paikan kevyt-valinta-vastaanotto-tila-translations)
   "EHDOLLISESTI_VASTAANOTTANUT"   (:kevyt-valinta/ehdollisesti-vastaanottanut kevyt-valinta-vastaanotto-tila-translations)
   "VASTAANOTTANUT_SITOVASTI"      (:kevyt-valinta/vastaanottanut-sitovasti kevyt-valinta-vastaanotto-tila-translations)
   "KESKEN"                        (:kevyt-valinta/kesken kevyt-valinta-vastaanotto-tila-translations)})

(def ilmoittautumisen-tila-selection-state
  {"EI_TEHTY"              (:kevyt-valinta/ei-tehty kevyt-valinta-ilmoittautumisen-tila-translations)
   "LASNA_KOKO_LUKUVUOSI"  (:kevyt-valinta/lasna-koko-lukuvuosi kevyt-valinta-ilmoittautumisen-tila-translations)
   "POISSA_KOKO_LUKUVUOSI" (:kevyt-valinta/poissa-koko-lukuvuosi kevyt-valinta-ilmoittautumisen-tila-translations)
   "EI_ILMOITTAUTUNUT"     (:kevyt-valinta/ei-ilmoittautunut kevyt-valinta-ilmoittautumisen-tila-translations)
   "LASNA_SYKSY"           (:kevyt-valinta/lasna-syksy kevyt-valinta-ilmoittautumisen-tila-translations)
   "POISSA_SYKSY"          (:kevyt-valinta/poissa-syksy kevyt-valinta-ilmoittautumisen-tila-translations)
   "LASNA"                 (:kevyt-valinta/lasna kevyt-valinta-ilmoittautumisen-tila-translations)
   "POISSA"                (:kevyt-valinta/poissa kevyt-valinta-ilmoittautumisen-tila-translations)})

(def application-hakukohde-review-states
  [["unreviewed" (:unreviewed state-translations)]
   ["fulfilled" (:fulfilled state-translations)]
   ["unfulfilled" (:unfulfilled state-translations)]])

(def application-hakukohde-eligibility-states
  [["unreviewed" (:unreviewed state-translations)]
   ["eligible" (:eligible state-translations)]
   ["uneligible" (:uneligible state-translations)]
   ["conditionally-eligible" (:conditionally-eligible state-translations)]])

(def application-payment-obligation-states
  [["unreviewed" (:unreviewed state-translations)]
   ["obligated" (:obligated state-translations)]
   ["not-obligated" (:not-obligated state-translations)]])

(def hakukohde-review-types
  [[:processing-state (:processing-state state-translations) application-hakukohde-processing-states]
   [:language-requirement (:language-requirement state-translations) application-hakukohde-review-states]
   [:degree-requirement (:degree-requirement state-translations) application-hakukohde-review-states]
   [:eligibility-state (:eligibility-state state-translations) application-hakukohde-eligibility-states]
   [:payment-obligation (:payment-obligation state-translations) application-payment-obligation-states]
   [:selection-state (:selection-state state-translations) application-hakukohde-selection-states]])

(def kevyt-valinta-hakukohde-review-types
  {:kevyt-valinta/julkaisun-tila        {:fi "Julkaisu"}
   :kevyt-valinta/vastaanotto-tila      {:fi "Vastaanotto"}
   :kevyt-valinta/ilmoittautumisen-tila {:fi "Ilmoittautuminen"}})

(def hakukohde-review-types-map
  (util/group-by-first first hakukohde-review-types))

(def hakukohde-review-type-names
  (map (comp name first) hakukohde-review-types))

; States where applications are considered "complete" in the application handling UI
(def complete-states ["inactivated"])

;; States which are not considered terminal, see above for terminal states
(def incomplete-states ["active"])

(def attachment-hakukohde-review-types
  [["not-checked" (:not-checked state-translations)]
   ["checked" (:checked state-translations)]
   ["incomplete-attachment" (:incomplete-attachment state-translations)]
   ["attachment-missing" (:attachment-missing state-translations)]
   ["overdue" (:overdue state-translations)]])

(def attachment-hakukohde-review-types-with-multiple-values
  (cons
   ["multiple-values" (:multiple-values state-translations)]
   attachment-hakukohde-review-types))

(def no-attachment-requirements "no-requirements")

(def attachment-hakukohde-review-types-with-no-requirements
  (concat attachment-hakukohde-review-types
          [[no-attachment-requirements (:no-attachment-required state-translations)]]))

(def attachment-review-type-names
  (map first attachment-hakukohde-review-types))
