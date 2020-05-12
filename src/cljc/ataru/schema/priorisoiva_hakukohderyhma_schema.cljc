(ns ataru.schema.priorisoiva-hakukohderyhma-schema
  (:require [schema.core :as s]))

(s/defschema PriorisoivaHakukohderyhma
  {:haku-oid           s/Str
   :hakukohderyhma-oid s/Str
   :prioriteetit       [[s/Str]]})
