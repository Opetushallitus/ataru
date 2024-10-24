(ns ataru.schema.maksut-schema
  (:require [schema.core :as s]
            [ataru.schema.localized-schema :as localized-schema]))

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

(s/defschema Origin
  (s/enum
    "tutu"
    "astu"))

(s/defschema LaskuStatus
  {:order_id s/Str
   :reference s/Str
   :status PaymentStatus
   :origin Origin})

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
   (s/optional-key :paid_at) s/Str
   (s/optional-key :metadata) s/Any})

(s/defschema Laskut
  [Lasku])

(s/defschema LaskuMetadataCreate
  {:form-name localized-schema/LocalizedStringOptional})

(s/defschema LaskuCreate
  (s/constrained
    {(s/optional-key :order-id) s/Str
     :first-name s/Str
     :last-name s/Str
     :email s/Str
     :amount s/Str
     (s/optional-key :due-date) (s/maybe s/Str)
     (s/optional-key :due-days) (s/constrained s/Int #(> % 0) 'positive-due-days)
     :origin Origin
     :reference s/Str
     (s/optional-key :locale) (s/maybe Locale)
     (s/optional-key :message) (s/maybe s/Str)
     (s/optional-key :index) (s/constrained s/Int #(<= 1 % 2) 'valid-maksu-index)
     (s/optional-key :metadata) LaskuMetadataCreate}
    (fn [{:keys [due-date due-days]}]
      (or due-date due-days))
    'must-have-either-due-date-or-due-days))

(s/defschema TutuProcessingEmailRequest
  {:application-key s/Str
   :locale Locale})
