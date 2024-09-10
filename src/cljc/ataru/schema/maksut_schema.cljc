(ns ataru.schema.maksut-schema
  (:require [schema.core :as s]))

(s/defschema PaymentStatus
  (s/enum
   :active
    :paid
    :overdue))

(s/defschema Locale
  (s/enum
    "fi"
    "sv"
    "en"))

(s/defschema LaskuStatus
  {:order_id s/Str
   :reference s/Str
   :status PaymentStatus})

(s/defschema Lasku
  {:order_id s/Str
   :first_name s/Str
   :last_name s/Str
   :amount s/Str
   :status PaymentStatus
   :due_date s/Str
   :origin s/Str
   :reference s/Str
   (s/optional-key :secret) s/Str
   (s/optional-key :paid_at) s/Str})

(s/defschema Laskut
  [Lasku])

(s/defschema TutuLaskuCreate
  {:application-key s/Str
   :first-name s/Str
   :last-name s/Str
   :email s/Str
   :amount s/Str
   (s/optional-key :locale) (s/maybe Locale)
   (s/optional-key :due-date) (s/maybe s/Str)
   (s/optional-key :message) (s/maybe s/Str)
   :index (s/constrained s/Int #(<= 1 % 2) 'valid-tutu-maksu-index)})

(s/defschema Origin
  (s/enum
    "tutu"
    "astu"))

(s/defschema LaskuCreate
  {:first-name s/Str
   :last-name s/Str
   :email s/Str
   :amount s/Str
   (s/optional-key :due-date) (s/maybe s/Str) ;If not defined, then due-days used
   :due-days (s/constrained s/Int #(> % 0) 'positive-due-days)
   :origin Origin
   :reference s/Str
   :index (s/constrained s/Int #(<= 1 % 2) 'valid-tutu-maksu-index)
   (s/optional-key :locale) (s/maybe Locale)
   (s/optional-key :message) (s/maybe s/Str)})

(s/defschema TutuProcessingEmailRequest
  {:application-key s/Str
   :locale Locale})
