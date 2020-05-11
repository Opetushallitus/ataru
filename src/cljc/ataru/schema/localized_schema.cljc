(ns ataru.schema.localized-schema
  (:require [schema.core :as s]))

(s/defschema LocalizedString {:fi                  s/Str
                              (s/optional-key :sv) s/Str
                              (s/optional-key :en) s/Str})

(s/defschema LocalizedStringOptional {(s/optional-key :fi) s/Str
                                      (s/optional-key :sv) s/Str
                                      (s/optional-key :en) s/Str})

(s/defschema LocalizedDateTime {:fi s/Str
                                :sv s/Str
                                :en s/Str})
