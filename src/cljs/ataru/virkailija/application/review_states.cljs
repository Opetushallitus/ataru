(ns ataru.virkailija.application.review-states)

(def application-review-states
  (array-map "received"   "Saapunut"
             "processing" "Käsittelyssä"
             "rejected"   "Hylätty"
             "approved"   "Hyväksytty"
             "canceled"   "Peruutettu"))
