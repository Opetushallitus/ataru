(ns ataru.hakija.schema.render-field-schema
  (:require [schema.core :as s]))

(s/defschema RenderFieldArgs
  {:field-descriptor s/Any
   :render-field     s/Any
   :idx              (s/maybe s/Int)})
