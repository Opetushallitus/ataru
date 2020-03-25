(ns ataru.hakija.schema
  (:require [schema.core :as s]))

(s/defschema Label
  {(s/optional-key :fi) s/Str
   (s/optional-key :sv) s/Str
   (s/optional-key :en) s/Str})

(s/defschema Value
  (s/conditional #(and (vector? %) (vector? (first %)))
                 (s/constrained [(s/constrained [(s/maybe s/Str)] vector?)] vector?)
                 vector?
                 (s/constrained [(s/maybe s/Str)] vector?)
                 :else
                 s/Str))

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
  (s/conditional #(and (vector? %) (vector? (first %)))
                 (s/constrained [(s/constrained [ValuesValue] vector?)] vector?)
                 vector?
                 (s/constrained [ValuesValue] vector?)
                 :else
                 ValuesValue))

(s/defschema Answer
  {:value                           (s/maybe Value)
   :values                          (s/maybe Values)
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
