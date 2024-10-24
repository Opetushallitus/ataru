(ns ataru.schema.params-schema
  (:require [ataru.schema.info-text-schema :as info-text-schema]
            [ataru.schema.localized-schema :as localized-schema]
            [schema.core :as s]))

(s/defschema Params {(s/optional-key :adjacent)                         s/Bool
                     (s/optional-key :can-submit-multiple-applications) s/Bool
                     (s/optional-key :deadline)                         (s/maybe s/Str)
                     (s/optional-key :deadline-label)                   localized-schema/LocalizedDateTime
                     (s/optional-key :yhteishaku)                       (s/maybe s/Bool)
                     (s/optional-key :repeatable)                       s/Bool
                     (s/optional-key :numeric)                          s/Bool
                     (s/optional-key :min-value)                        s/Str
                     (s/optional-key :max-value)                        s/Str
                     (s/optional-key :decimals)                         (s/maybe s/Int)
                     (s/optional-key :selection-group-id)               s/Str
                     (s/optional-key :max-hakukohteet)                  (s/maybe s/Int)
                     (s/optional-key :question-group-id)                s/Int
                     (s/optional-key :max-length)                       s/Str
                     (s/optional-key :hidden)                           s/Bool
                     (s/optional-key :size)                             s/Str
                     (s/optional-key :haku-oid)                         s/Str
                     (s/optional-key :placeholder)                      localized-schema/LocalizedString
                     (s/optional-key :mail-attachment?)                 (s/maybe s/Bool)
                     (s/optional-key :fetch-info-from-kouta?)           (s/maybe s/Bool)
                     (s/optional-key :attachment-type)                  (s/maybe s/Str)
                     (s/optional-key :info-text)                        (s/maybe info-text-schema/InfoText)
                     (s/optional-key :info-text-collapse)               (s/maybe s/Bool)
                     (s/optional-key :show-only-for-identified)         (s/maybe s/Bool)
                     (s/optional-key :allow-tutkinto-question-group)    (s/maybe s/Bool)
                     (s/optional-key :invalid-values)                   [s/Str]})
