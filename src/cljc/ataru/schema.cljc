(ns ataru.schema
  (:require [oph.soresu.form.schema :as soresu]
            [schema.core :as s]))

(soresu/create-form-schema [] [] [])

(s/defschema Form
  {(s/optional-key :id) (s/maybe s/Int)
   :name                s/Str
   :content             [(s/either soresu/FormField soresu/WrapperElement)]
   s/Any                s/Any})


