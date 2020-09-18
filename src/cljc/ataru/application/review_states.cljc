(ns ataru.application.review-states
  (:require [ataru.translations.texts :refer [state-translations
                                              virkailija-texts]]
            [ataru.util :as util]))

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
   ["valintaesitys" (:valintaesitys state-translations)]
   ["processed" (:processed state-translations)]
   ["information-request" (:information-request state-translations)]])

(def initial-application-hakukohde-processing-state "unprocessed")

(def application-hakukohde-selection-states
  [["incomplete" (:incomplete state-translations)]
   ["selection-proposal" (:selection-proposal state-translations)]
   ["reserve" (:reserve state-translations)]
   ["selected" (:selected state-translations)]
   ["rejected" (:rejected state-translations)]])

(def valinnan-tila-translation-key-mapping
  {"HYLATTY"                :hylatty
   "VARALLA"                :varalla
   "PERUUNTUNUT"            :peruuntunut
   "VARASIJALTA_HYVAKSYTTY" :varasijalta-hyvaksytty
   "HYVAKSYTTY"             :hyvaksytty
   "PERUNUT"                :perunut
   "PERUUTETTU"             :peruutettu
   "KESKEN"                 :kesken})

(def kevyt-valinta-valinnan-tila-selection-states
  (map (fn [[value key]]
         [value
          (key virkailija-texts)])
       valinnan-tila-translation-key-mapping))

(def julkaisun-tila-translation-key-mapping
  {true  :julkaistu
   false :ei-julkaistu})

(def vastaanotto-tila-translation-key-mapping
  {"EI_VASTAANOTETTU_MAARA_AIKANA" :ei-vastaanotettu-maaraaikana
   "PERUNUT"                       :perunut
   "PERUUTETTU"                    :peruutettu
   "OTTANUT_VASTAAN_TOISEN_PAIKAN" :ottanut-vastaan-toisen-paikan
   "EHDOLLISESTI_VASTAANOTTANUT"   :ehdollisesti-vastaanottanut
   "VASTAANOTTANUT_SITOVASTI"      :vastaanottanut-sitovasti
   "KESKEN"                        :kesken
   "VASTAANOTTANUT"                :vastaanottanut})

(def ilmoittautumisen-tila-translation-key-mapping
  {"EI_TEHTY"              :ei-tehty
   "LASNA_KOKO_LUKUVUOSI"  :lasna-koko-lukuvuosi
   "POISSA_KOKO_LUKUVUOSI" :poissa-koko-lukuvuosi
   "EI_ILMOITTAUTUNUT"     :ei-ilmoittautunut-maaraaikana
   "LASNA_SYKSY"           :lasna-syksy
   "POISSA_SYKSY"          :poissa-syksy
   "LASNA"                 :lasna
   "POISSA"                :poissa})

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
