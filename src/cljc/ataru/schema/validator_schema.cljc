(ns ataru.schema.validator-schema
  (:require [ataru.hakija.application-validators :as validator]
            [schema.core :as s]))

(s/defschema Validator (apply s/enum (concat (keys validator/pure-validators)
                                             (keys validator/async-validators))))
