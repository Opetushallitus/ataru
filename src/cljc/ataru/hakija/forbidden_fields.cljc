(ns ataru.hakija.forbidden-fields)

(def viewing-forbidden-person-info-field-ids #{:ssn :birth-date})
(def editing-forbidden-person-info-field-ids #{:nationality :have-finnish-ssn})
