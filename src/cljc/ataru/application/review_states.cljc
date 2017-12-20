(ns ataru.application.review-states
  (:require [clojure.set :refer [difference]]))

(def application-review-states
  [["active" "Aktiivinen"]
   ["inactivated" "Passiivinen"]])

(def initial-application-review-state "active")

(def application-hakukohde-processing-states
  [["unprocessed" "Käsittelemättä"]
   ["processing" "Käsittelyssä"]
   ["invited-to-interview" "Kutsuttu haast."]
   ["invited-to-exam" "Kutsuttu valintak."]
   ["evaluating" "Arvioinnissa"]
   ["processed" "Käsitelty"]
   ["information-request" "Täydennyspyyntö"]])

(def initial-application-hakukohde-processing-state "unprocessed")

(def application-hakukohde-selection-states
  [["incomplete" "Kesken"]
   ["selection-proposal" "Valintaesitys"]
   ["reserve" "Varalla"]
   ["selected" "Hyväksytty"]
   ["rejected" "Hylätty"]])

(def application-hakukohde-review-states
  [["unreviewed" "Tarkastamatta"]
   ["fulfilled" "Täyttyy"]
   ["unfulfilled" "Ei täyty"]])

(def application-hakukohde-eligibility-states
  [["unreviewed" "Tarkastamatta"]
   ["eligible" "Hakukelpoinen"]
   ["uneligible" "Ei hakukelpoinen"]])

(def application-payment-obligation-states
  [["unreviewed" "Tarkastamatta"]
   ["obligated" "Velvollinen"]
   ["not-obligated" "Ei velvollinen"]])

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

(def complete-hakukohde-process-states ["processed"])

