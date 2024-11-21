(ns ataru.schema.form-element-schema
  (:require [ataru.schema.localized-schema :as localized-schema]
            [schema.core :as s])
  #?(:clj (:import [org.joda.time DateTime])))

(s/defschema Form {(s/optional-key :id)                s/Int
                   :name                               localized-schema/LocalizedStringOptional
                   :content                            (s/pred empty?)
                   (s/optional-key :locked)            #?(:clj  (s/maybe DateTime)
                                                          :cljs (s/maybe s/Str))
                   (s/optional-key :locked-by)         (s/maybe s/Str)
                   (s/optional-key :locked-by-oid)     (s/maybe s/Str)
                   (s/optional-key :languages)         [s/Str]
                   (s/optional-key :key)               s/Str
                   (s/optional-key :created-by)        s/Str
                   (s/optional-key :created-time)      #?(:clj  DateTime
                                                          :cljs s/Str)
                   (s/optional-key :application-count) s/Int
                   (s/optional-key :deleted)           (s/maybe s/Bool)
                   (s/optional-key :properties)        {(s/optional-key :auto-expand-hakukohteet)          s/Bool
                                                        (s/optional-key :order-hakukohteet-by-opetuskieli) s/Bool
                                                        (s/optional-key :allow-only-yhteishaut)            s/Bool
                                                        (s/optional-key :allow-hakeminen-tunnistautuneena) s/Bool
                                                        (s/optional-key :demo-validity-start)              (s/maybe s/Str)
                                                        (s/optional-key :demo-validity-end)                (s/maybe s/Str)
                                                        (s/optional-key :closed)                           s/Bool
                                                        (s/optional-key :payment) (s/maybe {(s/optional-key :type)   s/Str
                                                                                            (s/optional-key :processing-fee) (s/maybe s/Str)
                                                                                            (s/optional-key :decision-fee)   (s/maybe s/Str)})}
                   (s/optional-key :demo-allowed)      s/Bool})
