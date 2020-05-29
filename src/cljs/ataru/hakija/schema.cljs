(ns ataru.hakija.schema
  (:require [schema.core :as s]))

(defn- is-question-group-answer? [value]
  (and (vector? value)
       (not-empty value)
       (or (vector? (first value))
           (nil? (first value)))))

(s/defschema Label
  {(s/optional-key :fi) s/Str
   (s/optional-key :sv) s/Str
   (s/optional-key :en) s/Str})

(s/defschema Value
  (s/conditional #(and (is-question-group-answer? %)
                       (some (fn [values] (some nil? values)) %))
                 [(s/maybe [(s/one (s/maybe s/Str) "single choice value")])]
                 is-question-group-answer?
                 [(s/maybe [s/Str])]
                 vector?
                 [s/Str]
                 :else
                 (s/maybe s/Str)))

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
  (s/conditional is-question-group-answer?
                 [(s/conditional is-question-group-answer?
                                 [(s/maybe [ValuesValue])]
                                 :else
                                 (s/maybe [ValuesValue]))]
                 vector?
                 [ValuesValue]
                 :else
                 ValuesValue))

(s/defschema Answer
  (s/constrained
   {:value                          Value
    :values                         (s/maybe Values)
    :valid                          s/Bool
    :label                          Label
    (s/optional-key :errors)        [{:fi s/Any
                                      :sv s/Any
                                      :en s/Any}]
    (s/optional-key :verify)        s/Str
    (s/optional-key :limit-reached) (s/maybe #{s/Str})
    :original-value                 (s/maybe Value)}
   (fn [answer]
     (= (:value answer)
        (cond (is-question-group-answer? (:values answer))
              (mapv #(when (vector? %)
                       (mapv :value %))
                    (:values answer))
              (vector? (:values answer))
              (mapv :value (:values answer))
              :else
              (:value (:values answer)))))))

(s/defschema Db
  {:application {:answers  {s/Keyword Answer}
                 :editing? s/Bool
                 s/Any     s/Any}
   s/Any        s/Any})
