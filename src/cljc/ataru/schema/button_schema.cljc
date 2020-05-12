(ns ataru.schema.button-schema
  (:require [ataru.schema.element-metadata-schema :as element-metadata-schema]
            [ataru.schema.localized-schema :as localized-schema]
            [ataru.schema.params-schema :as params-schema]
            [schema.core :as s]))

(s/defschema Button {:fieldClass                                 (s/eq "button")
                     :id                                         s/Str
                     :fieldType                                  s/Keyword
                     :metadata                                   element-metadata-schema/ElementMetadata
                     (s/optional-key :label)                     localized-schema/LocalizedString
                     (s/optional-key :params)                    params-schema/Params
                     (s/optional-key :belongs-to-hakukohteet)    [s/Str]
                     (s/optional-key :belongs-to-hakukohderyhma) [s/Str]})
