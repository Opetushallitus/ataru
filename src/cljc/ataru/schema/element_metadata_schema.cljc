(ns ataru.schema.element-metadata-schema
  (:require [schema.core :as s]))

(s/defschema ElementMetadata
  {:created-by              {:name s/Str
                             :oid  s/Str
                             :date s/Str}
   :modified-by             {:name s/Str
                             :oid  s/Str
                             :date s/Str}
   (s/optional-key :locked) s/Bool})
