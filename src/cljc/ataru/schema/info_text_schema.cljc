(ns ataru.schema.info-text-schema
  (:require [ataru.schema.localized-schema :as localized-schema]
            [schema.core :as s]))

(s/defschema InfoText {(s/optional-key :enabled?) s/Bool
                       (s/optional-key :value)    localized-schema/LocalizedStringOptional
                       (s/optional-key :label)    (s/maybe localized-schema/LocalizedStringOptional)})
