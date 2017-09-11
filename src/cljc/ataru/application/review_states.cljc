(ns ataru.application.review-states
  (:require [clojure.set :refer [difference]]))

(def application-review-states
  [["unprocessed" "Käsittelemättä"]
   ["processing" "Käsittelyssä"]                            ; TODO check jatkuva haku special case
   ["invited-to-interview" "Kutsuttu haastatteluun"]
   ["invited-to-exam" "Kutsuttu valintakokeeseen"]
   ["evaluating" "Arvioinnissa"]
   ["processed" "Käsitelty"]
   ["inactivated" "Passivoitu"]
   ; the following are deprecated: not shown in UI but old data must pass validation
   ["not-selected" "Ei valittu" :deprecated]
   ["selection-proposal" "Valintaesitys" :deprecated]
   ["selected" "Valittu" :deprecated]
   ["applicant-has-accepted" "Vastaanottanut" :deprecated]
   ["rejected" "Hylätty" :deprecated]
   ["canceled" "Perunut" :deprecated]])

(def active-application-review-states
  (filter (fn [state] (every? #(not= % :deprecated) state)) application-review-states))

(def application-hakukohde-selection-states
  [["incomplete" "Kesken"]
   ["not-selected" "Ei valittu"]
   ["selection-proposal" "Valintaesitys"]
   ["selected" "Valittu"]
   ["rejected" "Hylätty"]])

(def application-hakukohde-review-states
  [["unreviewed" "Tarkastamatta"
    "fulfilled" "Täyttyy"
    "unfulfilled" "Ei täyty"]])

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
;; ALL application options (hakukohde) must be in one of these states for the application to be considered complete
(def complete-states ["canceled" "selected" "rejected" "applicant-has-accepted" "not-selected" "selection-proposal"])

;; States which are not considered terminal, see above for terminal states
(def incomplete-states
  (-> application-review-states first set (difference (set complete-states)) vec))
