(ns ataru.application.review-states
  (:require [ataru.translations.texts :refer [state-translations]]
            [clojure.set :refer [difference]]))

(def application-review-states
  [["active" (:active state-translations)]
   ["inactivated" (:passice state-translations)]])

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

(def application-hakukohde-review-states
  [["unreviewed" (:unreviewed state-translations)]
   ["fulfilled" (:fulfilled state-translations)]
   ["unfulfilled" (:unfulfilled state-translations)]])

(def application-hakukohde-eligibility-states
  [["unreviewed" (:unreviewed state-translations)]
   ["eligible" (:eligible state-translations)]
   ["uneligible" (:uneligible state-translations)]])

(def application-payment-obligation-states
  [["unreviewed" (:unreviewed state-translations)]
   ["obligated" (:obligated state-translations)]
   ["not-obligated" (:not-obligated state-translations)]])

(def hakukohde-review-types
  [[:processing-state "Käsittelyvaihe" application-hakukohde-processing-states]
   [:language-requirement "Kielitaitovaatimus" application-hakukohde-review-states]
   [:degree-requirement "Tutkinnon kelpoisuus" application-hakukohde-review-states]
   [:eligibility-state "Hakukelpoisuus" application-hakukohde-eligibility-states]
   [:payment-obligation "Maksuvelvollisuus" application-payment-obligation-states]
   [:selection-state "Valinta" application-hakukohde-selection-states]])

(def hakukohde-review-type-names
  (map (comp name first) hakukohde-review-types))

; States where applications are considered "complete" in the application handling UI
(def complete-states ["inactivated"])

;; States which are not considered terminal, see above for terminal states
(def incomplete-states ["active"])

(def attachment-hakukohde-review-types
  [["not-checked" (:not-checked state-translations)]
   ["checked" (:checked state-translations)]
   ["incomplete" (:incomplete-answer state-translations)]])

(def no-attachment-requirements "no-requirements")

(def attachment-hakukohde-review-types-with-no-requirements
  (concat attachment-hakukohde-review-types
          [[no-attachment-requirements (:attachmentless state-translations)]]))

(def attachment-review-type-names
  (map first attachment-hakukohde-review-types))