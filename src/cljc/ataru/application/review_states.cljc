(ns ataru.application.review-states
  (:require [ataru.translations.texts :refer [state-translations
                                              virkailija-texts]]
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

(def valinnan-tila-selection-state
  {"HYLATTY"                (:kevyt-valinta/hylatty virkailija-texts)
   "VARALLA"                (:kevyt-valinta/varalla virkailija-texts)
   "PERUUNTUNUT"            (:kevyt-valinta/peruuntunut virkailija-texts)
   "VARASIJALTA_HYVAKSYTTY" (:kevyt-valinta/varasijalta-hyvaksytty virkailija-texts)
   "HYVAKSYTTY"             (:kevyt-valinta/hyvaksytty virkailija-texts)
   "PERUNUT"                (:kevyt-valinta/perunut virkailija-texts)
   "PERUUTETTU"             (:kevyt-valinta/peruutettu virkailija-texts)
   "KESKEN"                 (:kevyt-valinta/kesken virkailija-texts)})

(def julkaisun-tila-selection-state
  {true  (:kevyt-valinta/julkaistu-hakijalle virkailija-texts)
   false (:kevyt-valinta/ei-julkaistu virkailija-texts)})

(def vastaanotto-tila-selection-state
  {"EI_VASTAANOTETTU_MAARA_AIKANA" (:kevyt-valinta/ei-vastaanotettu-maaraaikana virkailija-texts)
   "PERUNUT"                       (:kevyt-valinta/perunut virkailija-texts)
   "PERUUTETTU"                    (:kevyt-valinta/peruutettu virkailija-texts)
   "OTTANUT_VASTAAN_TOISEN_PAIKAN" (:kevyt-valinta/ottanut-vastaan-toisen-paikan virkailija-texts)
   "EHDOLLISESTI_VASTAANOTTANUT"   (:kevyt-valinta/ehdollisesti-vastaanottanut virkailija-texts)
   "VASTAANOTTANUT_SITOVASTI"      (:kevyt-valinta/vastaanottanut-sitovasti virkailija-texts)
   "KESKEN"                        (:kevyt-valinta/kesken virkailija-texts)
   "VASTAANOTTANUT"                (:kevyt-valinta/vastaanottanut virkailija-texts)})

(def ilmoittautumisen-tila-selection-state
  {"EI_TEHTY"              (:kevyt-valinta/ei-tehty virkailija-texts)
   "LASNA_KOKO_LUKUVUOSI"  (:kevyt-valinta/lasna-koko-lukuvuosi virkailija-texts)
   "POISSA_KOKO_LUKUVUOSI" (:kevyt-valinta/poissa-koko-lukuvuosi virkailija-texts)
   "EI_ILMOITTAUTUNUT"     (:kevyt-valinta/ei-ilmoittautunut virkailija-texts)
   "LASNA_SYKSY"           (:kevyt-valinta/lasna-syksy virkailija-texts)
   "POISSA_SYKSY"          (:kevyt-valinta/poissa-syksy virkailija-texts)
   "LASNA"                 (:kevyt-valinta/lasna virkailija-texts)
   "POISSA"                (:kevyt-valinta/poissa virkailija-texts)})

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
  {:kevyt-valinta/valinnan-tila         {:fi "Valinta"}
   :kevyt-valinta/julkaisun-tila        {:fi "Julkaisu"}
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
