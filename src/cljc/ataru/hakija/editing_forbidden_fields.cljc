(ns ataru.hakija.editing-forbidden-fields)

(def viewing-forbidden-person-info-field-ids #{:ssn :birth-date})
(def editing-forbidden-person-info-field-ids #{:nationality
                                               :have-finnish-ssn
                                               :first-name
                                               :preferred-name
                                               :last-name
                                               :gender})
