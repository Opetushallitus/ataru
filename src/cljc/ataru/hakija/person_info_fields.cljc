(ns ataru.hakija.person-info-fields
  (:require [clojure.set :as set]))

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
    :postal-office
    :home-town
    :city
    :language})

(def guardian-contact-info-field-ids
  #{:guardian-name
    :guardian-phone
    :guardian-email
    :guardian-name-secondary
    :guardian-phone-secondary
    :guardian-email-secondary})

(def viewing-forbidden-person-info-field-ids
  #{:ssn
    :birth-date})

(def editing-forbidden-person-info-field-ids
  (set/union
   viewing-forbidden-person-info-field-ids
   #{:first-name
     :preferred-name
     :last-name
     :nationality
     :have-finnish-ssn
     :gender
     :language}))

(def editing-allowed-person-info-field-ids
  (set/difference
    (set/union person-info-field-ids guardian-contact-info-field-ids)
    editing-forbidden-person-info-field-ids))
