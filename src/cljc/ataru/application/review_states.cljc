(ns ataru.application.review-states
  (:require [clojure.set :refer [difference]]))

(def application-review-states
  [["unprocessed" "Käsittelemättä"]
   ["processing" "Käsittelyssä"]
   ["invited-to-interview" "Kutsuttu haastatteluun"]
   ["invited-to-exam" "Kutsuttu valintakokeeseen"]
   ["evaluating" "Arvioinnissa"]
   ["processed" "Käsitelty"]
   ["inactivated" "Passiivinen"]])

(def application-hakukohde-selection-states
  [["incomplete" "Kesken"]
   ["selection-proposal" "Valintaesitys"]
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

(def hakukohde-review-types
  [[:language-requirement "Kielitaitovaatimus" application-hakukohde-review-states]
   [:degree-requirement "Tutkinnon kelpoisuus" application-hakukohde-review-states]
   [:eligibility-state "Hakukelpoisuus" application-hakukohde-eligibility-states]
   [:selection-state "Valinta" application-hakukohde-selection-states]])

;; States that are - at least for the time being - considered terminal. They have been handled
;; and might be left at this state forever
(def complete-states ["processed" "inactivated"])

;; States which are not considered terminal, see above for terminal states
(def incomplete-states
  (-> application-review-states first set (difference (set complete-states)) vec))
