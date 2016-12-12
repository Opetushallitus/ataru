(ns ataru.application.review-states)

(def application-review-states
  (array-map "unprocessed"   "Käsittelemättä"
             "processing"    "Käsittelyssä"
             "rejected"      "Hylätty"
             "approved"      "Hyväksytty"
             "canceled"      "Peruutettu"))
