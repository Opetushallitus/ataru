(ns ataru.schema.pohjakoulutus-ristiriita-schema
  (:require [ataru.schema.element-metadata-schema :as element-metadata-schema]
            [ataru.schema.localized-schema :as localized-schema]
            [schema.core :as s]))

(s/defschema Pohjakoulutusristiriita
  {:id                   s/Str
   :fieldClass           (s/eq "pohjakoulutusristiriita")
   :fieldType            (s/eq "pohjakoulutusristiriita")
   :exclude-from-answers (s/eq true)
   :metadata             element-metadata-schema/ElementMetadata
   :params               {:deny-submit s/Bool}
   :rules                {s/Keyword s/Any}
   :label                localized-schema/LocalizedStringOptional
   :text                 localized-schema/LocalizedStringOptional})
