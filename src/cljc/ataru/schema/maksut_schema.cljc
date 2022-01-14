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
  {:order-id s/Str
   :reference s/Str
   :status PaymentStatus})

(s/defschema Lasku
  {:order_id s/Str
   :first_name s/Str
   :last_name s/Str
   :amount s/Str
   :status PaymentStatus
   :due_date s/Str
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

(s/defschema TutuProcessingEmailRequest
  {:application-key s/Str
   :locale Locale})
