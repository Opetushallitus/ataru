(ns ataru.schema.child-validator-schema
  (:require [schema.core :as s]))

(s/defschema ChildValidator (s/enum :one-of
                                    :birthdate-and-gender-component
                                    :ssn-or-birthdate-component
                                    :oppiaine-a1-or-a2-component))
