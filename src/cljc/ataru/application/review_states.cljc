(ns ataru.application.review-states
  (:require [ataru.translations.texts :refer [state-translations
                                              virkailija-texts]]
            [ataru.util :as util]))

(def application-review-states
  [["active" (:active state-translations)]
   ["inactivated" (:passive state-translations)]])

(def initial-application-review-state "active")

; This list contains also only Tutu -specific states that need to removed from the list for normal use
; (However they are left in this list because all schemas validate according to this)
(def application-hakukohde-processing-states
  [["unprocessed" (:unprocessed state-translations)]
   ["processing" (:processing state-translations)]
   ["invited-to-interview" (:invited-to-interview state-translations)]
   ["invited-to-exam" (:invited-to-exam state-translations)]
   ["evaluating" (:evaluating state-translations)]
   ["valintaesitys" (:valintaesitys state-translations)]
   ["processed" (:processed state-translations)]
   ["information-request" (:information-request state-translations)]
   ["processing-fee-overdue" (:processing-fee-overdue state-translations)]
   ["processing-fee-paid" (:processing-fee-paid state-translations)]
   ["decision-fee-outstanding" (:decision-fee-outstanding state-translations)]
   ["decision-fee-overdue" (:decision-fee-overdue state-translations)]
   ["decision-fee-paid" (:decision-fee-paid state-translations)]])

(def tutu-processing-state
  #{"processing-fee-overdue"
    "processing-fee-paid"
    "decision-fee-outstanding"
    "decision-fee-overdue"
    "decision-fee-paid"})

(def application-hakukohde-processing-states-normal
  (reduce (fn [acc [k v]]
            (if (tutu-processing-state k)
              acc
              (conj acc [k v])))
          []
          application-hakukohde-processing-states))

(def initial-application-hakukohde-processing-state "unprocessed")

(def application-hakukohde-selection-states
  [["incomplete" (:incomplete state-translations)]
   ["selection-proposal" (:selection-proposal state-translations)]
   ["reserve" (:reserve state-translations)]
   ["selected" (:selected state-translations)]
   ["rejected" (:rejected state-translations)]])

(def vastaanotto-tila-translation-key-mapping
  {"EI_VASTAANOTETTU_MAARA_AIKANA" :ei-vastaanotettu-maaraaikana
   "PERUNUT"                       :perunut
   "PERUUTETTU"                    :peruutettu
   "OTTANUT_VASTAAN_TOISEN_PAIKAN" :ottanut-vastaan-toisen-paikan
   "EHDOLLISESTI_VASTAANOTTANUT"   :ehdollisesti-vastaanottanut
   "VASTAANOTTANUT_SITOVASTI"      :vastaanottanut
   "KESKEN"                        :kesken})

(def kevyt-valinta-vastaanoton-tila-selection-states
  (map (fn [[value key]]
         [value
          (key virkailija-texts)])
       vastaanotto-tila-translation-key-mapping))

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

(def hakukohde-review-types-normal
  [[:processing-state (:processing-state state-translations) application-hakukohde-processing-states-normal]
   [:language-requirement (:language-requirement state-translations) application-hakukohde-review-states]
   [:degree-requirement (:degree-requirement state-translations) application-hakukohde-review-states]
   [:eligibility-state (:eligibility-state state-translations) application-hakukohde-eligibility-states]
   [:payment-obligation (:payment-obligation state-translations) application-payment-obligation-states]
   [:selection-state (:selection-state state-translations) application-hakukohde-selection-states]])

(def hakukohde-review-types-map
  (util/group-by-first first hakukohde-review-types))

(def hakukohde-review-type-names
  (map (comp name first) hakukohde-review-types))

(def uneditable-for-toisen-asteen-yhteishaku-states
  #{:language-requirement :degree-requirement :eligibility-state :payment-obligation :selection-state})

(def uneditable-for-opinto-ohjaaja-only
  #{:processing-state :hakukohde})

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
