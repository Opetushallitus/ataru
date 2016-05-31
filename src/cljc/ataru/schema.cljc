(ns ataru.schema
  (:require [schema.core :as s]))

(s/defschema Form
  {(s/optional-key :id)            s/Int
   :name                           s/Str
   (s/optional-key :modified-by)   s/Str
   (s/optional-key :modified-time) #?(:clj org.joda.time.DateTime
                                      :cljs s/Str)
   s/Any                           s/Any})
