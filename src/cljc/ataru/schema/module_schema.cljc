(ns ataru.schema.module-schema
  (:require [schema.core :as s]))

(s/defschema Module (s/enum :person-info :arvosanat-peruskoulu :arvosanat-2-aste))
