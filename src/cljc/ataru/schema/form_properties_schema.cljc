(ns ataru.schema.form-properties-schema
  (:require [schema.core :as s]))

(s/defschema PaymentProperties
  {(s/optional-key :type)                             (s/maybe s/Str)
   (s/optional-key :processing-fee)                   (s/maybe s/Str)
   (s/optional-key :decision-fee)                     (s/maybe s/Str)
   (s/optional-key :vat)                              (s/maybe s/Str)
   (s/optional-key :order-id-prefix)                  (s/maybe s/Str)})

(s/defschema FormCategoryProperties
             {(s/optional-key :selected-option-ids)  [s/Str]})

(s/defschema TutkintoProperties
             (merge FormCategoryProperties
                    {(s/optional-key :show-completed-studies)   s/Bool
                     (s/optional-key :save-koski-tutkinnot)     s/Bool
                     (s/optional-key :koski-update-allways)     s/Bool}))

(s/defschema FormProperties
             {(s/optional-key :auto-expand-hakukohteet)          s/Bool
              (s/optional-key :order-hakukohteet-by-opetuskieli) s/Bool
              (s/optional-key :allow-only-yhteishaut)            s/Bool
              (s/optional-key :allow-hakeminen-tunnistautuneena) s/Bool
              (s/optional-key :demo-validity-start)              (s/maybe s/Str)
              (s/optional-key :demo-validity-end)                (s/maybe s/Str)
              (s/optional-key :closed)                           s/Bool
              (s/optional-key :payment)                          (s/maybe PaymentProperties)
              (s/optional-key :tutkinto-properties)              TutkintoProperties})
