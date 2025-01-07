(ns ataru.schema.form-properties-schema
  (:require [schema.core :as s]))

(s/defschema FormCategoryProperties
             {(s/optional-key :selected-option-ids)  [s/Str]})

(s/defschema TutkintoProperties
             (merge FormCategoryProperties {}))

(s/defschema FormProperties
             {(s/optional-key :auto-expand-hakukohteet)          s/Bool
              (s/optional-key :order-hakukohteet-by-opetuskieli) s/Bool
              (s/optional-key :allow-only-yhteishaut)            s/Bool
              (s/optional-key :allow-hakeminen-tunnistautuneena) s/Bool
              (s/optional-key :demo-validity-start)              (s/maybe s/Str)
              (s/optional-key :demo-validity-end)                (s/maybe s/Str)
              (s/optional-key :closed)                           s/Bool
              (s/optional-key :tutkinto-properties)              TutkintoProperties})
