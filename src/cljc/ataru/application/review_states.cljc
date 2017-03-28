(ns ataru.application.review-states
  (:require [clojure.set :refer [difference]]))

(def application-review-states
  (array-map "unprocessed"            "Käsittelemättä"
             "processing"             "Käsittelyssä"
             "invited-to-interview"   "Kutsuttu haastatteluun"
             "invited-to-exam"        "Kutsuttu valintakokeeseen"
             "not-selected"           "Ei valittu"
             "selection-proposal"     "Valintaesitys"
             "selected"               "Valittu"
             "applicant-has-accepted" "Vastaanottanut"
             "rejected"               "Hylätty"
             "canceled"               "Perunut"))

;; States that are - at least for the time being - considered terminal. They have been handled
;; and might be left at this state forever
(def complete-states ["canceled" "selected" "rejected"])

;; States which are not considered terminal, see above for terminal states
(def incomplete-states
  (-> application-review-states keys set (difference (set complete-states)) vec))
