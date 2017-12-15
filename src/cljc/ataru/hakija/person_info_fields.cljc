(ns ataru.hakija.person-info-fields)

(def person-info-field-ids
  #{:first-name
    :preferred-name
    :last-name
    :nationality
    :have-finnish-ssn
    :ssn
    :birth-date
    :gender
    :birthplace
    :passport-number
    :national-id-number
    :email
    :phone
    :country-of-residence
    :address
    :postal-code
    :home-town
    :city
    :language})

(def viewing-forbidden-person-info-field-ids
  #{:ssn
    :birth-date})

(def editing-forbidden-person-info-field-ids
  (clojure.set/union
   viewing-forbidden-person-info-field-ids
   #{:first-name
     :preferred-name
     :last-name
     :nationality
     :have-finnish-ssn
     :gender
     :language}))

(def editing-allowed-person-info-field-ids
  (clojure.set/difference
   person-info-field-ids
   editing-forbidden-person-info-field-ids))
