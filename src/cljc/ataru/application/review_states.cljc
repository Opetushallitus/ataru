(ns ataru.application.review-states)

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
