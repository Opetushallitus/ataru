(ns ataru.schema.lang-schema
  (:require [schema.core :as s]))

(def Lang (s/enum :fi :sv :en))
