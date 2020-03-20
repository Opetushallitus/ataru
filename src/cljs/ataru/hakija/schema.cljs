(ns ataru.hakija.schema
  (:require [schema.core :as s]))

(s/defschema Label
  {(s/optional-key :fi) s/Str
   (s/optional-key :sv) s/Str
   (s/optional-key :en) s/Str})

(s/defschema Value
  (s/conditional string?
                 s/Str
                 #(and (vector? %) (vector? (first %)))
                 (s/constrained [(s/constrained [s/Str] vector?)] vector?)
                 :else
                 (s/constrained [s/Str] vector?)))

(s/defschema ValuesValue
  {:value                          (s/maybe s/Str)
   :valid                          s/Bool
   (s/optional-key :errors)        [[(s/one (s/maybe s/Keyword) "error keyword")
                                     (s/optional s/Str "error details")]]
   (s/optional-key :filename)      s/Str
   (s/optional-key :size)          s/Int
   (s/optional-key :status)        s/Keyword
   (s/optional-key :uploaded-size) s/Int
   (s/optional-key :last-progress) s/Any
   (s/optional-key :speed)         s/Num
   (s/optional-key :request)       s/Any})

(s/defschema Values
  (s/if #(and (vector? %) (vector? (first %)))
    (s/constrained [(s/constrained [ValuesValue] vector?)] vector?)
    (s/constrained [ValuesValue] vector?)))

(s/defschema Answer
  {(s/optional-key :value)          (s/maybe Value)
   (s/optional-key :values)         Values
   :valid                           s/Bool
   :label                           Label
   (s/optional-key :errors)         [{:fi s/Any
                                      :sv s/Any
                                      :en s/Any}]
   (s/optional-key :verify)         s/Str
   (s/optional-key :limit-reached)  (s/maybe #{s/Str})
   (s/optional-key :original-value) s/Any})

(s/defschema Db
  {:application {:answers  {s/Keyword Answer}
                 :editing? s/Bool
                 s/Any     s/Any}
   s/Any        s/Any})
