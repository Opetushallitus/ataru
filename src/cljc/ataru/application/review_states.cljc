(ns ataru.application.review-states
  (:require [clojure.set :refer [difference]]))

(def application-review-states
  [["unprocessed" "Käsittelemättä"]
   ["processing" "Käsittelyssä"]
   ["invited-to-interview" "Kutsuttu haastatteluun"]
   ["invited-to-exam" "Kutsuttu valintakokeeseen"]
   ["evaluating" "Arvioinnissa"]
   ["processed" "Käsitelty"]
   ["inactivated" "Passivoitu"]])

(def application-hakukohde-selection-states
  [["incomplete" "Kesken"]
   ["selection-proposal" "Valintaesitys"]
   ["selected" "Hyväksytty"]
   ["selected-from-reserve" "Hyväksytty varasijalta"]
   ["rejected" "Hylätty"]])

(def application-hakukohde-review-states
  [["unreviewed" "Tarkastamatta"]
   ["fulfilled" "Täyttyy"]
   ["unfulfilled" "Ei täyty"]])

(def application-hakukohde-eligibility-states
  [["unreviewed" "Tarkastamatta"]
   ["eligible" "Hakukelpoinen"]
   ["uneligible" "Ei hakukelpoinen"]])

(def application-hakukohde-states
  {:language-requirement application-hakukohde-review-states
   :degree-requirement application-hakukohde-review-states
   :apply-eligibility application-hakukohde-eligibility-states})

;; States that are - at least for the time being - considered terminal. They have been handled
;; and might be left at this state forever
;; TODO remove this!
(def complete-states ["canceled" "selected" "rejected" "applicant-has-accepted" "not-selected" "selection-proposal"])

;; States which are not considered terminal, see above for terminal states
(def incomplete-states
  (-> application-review-states first set (difference (set complete-states)) vec))
