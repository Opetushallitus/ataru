(ns ataru.schema.info-element-schema
  (:require [ataru.schema.element-metadata-schema :as element-metadata-schema]
            [ataru.schema.localized-schema :as localized-schema]
            [ataru.schema.params-schema :as params-schema]
            [schema.core :as s]))

(s/defschema InfoElement {:fieldClass                                 (s/eq "infoElement")
                          :id                                         s/Str
                          :fieldType                                  (apply s/enum ["h1"
                                                                                     "h3"
                                                                                     "link"
                                                                                     "p"
                                                                                     "bulletList"
                                                                                     "dateRange"
                                                                                     "endOfDateRange"])
                          :metadata                                   element-metadata-schema/ElementMetadata
                          (s/optional-key :params)                    params-schema/Params
                          (s/optional-key :label)                     localized-schema/LocalizedString
                          (s/optional-key :text)                      localized-schema/LocalizedString
                          (s/optional-key :belongs-to-hakukohteet)    [s/Str]
                          (s/optional-key :belongs-to-hakukohderyhma) [s/Str]})
