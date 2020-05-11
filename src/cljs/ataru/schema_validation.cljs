(ns ataru.schema-validation
  (:require [ataru.feature-config :as fc]
            [schema.core :as s]))

(defn enable-schema-fn-validation []
  (when (fc/feature-enabled? :schema-validation)
    (s/set-fn-validation! true)))
