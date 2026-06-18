(ns ataru.schema.form-element-schema
  (:require [ataru.schema.localized-schema :as localized-schema]
            [schema.core :as s]
            [ataru.schema.form-properties-schema :refer [FormProperties]])
  #?(:clj (:import [java.time ZonedDateTime])))

(s/defschema Form {(s/optional-key :id)                s/Int
                   :name                               localized-schema/LocalizedStringOptional
                   :content                            (s/pred empty?)
                   (s/optional-key :locked)            #?(:clj  (s/maybe ZonedDateTime)
                                                          :cljs (s/maybe s/Str))
                   (s/optional-key :locked-by)         (s/maybe s/Str)
                   (s/optional-key :locked-by-oid)     (s/maybe s/Str)
                   (s/optional-key :languages)         [s/Str]
                   (s/optional-key :key)               s/Str
                   (s/optional-key :created-by)        s/Str
                   (s/optional-key :created-time)      #?(:clj  ZonedDateTime
                                                          :cljs s/Str)
                   (s/optional-key :application-count) s/Int
                   (s/optional-key :deleted)           (s/maybe s/Bool)
                   (s/optional-key :properties)        FormProperties
                   (s/optional-key :demo-allowed)      s/Bool})
