(ns ataru.schema.module-schema
  (:require [schema.core :as s]
            [ataru.component-data.kk-application-payment-module :refer [payment-module-keyword]]))

(s/defschema Module (s/enum :person-info :arvosanat-peruskoulu :arvosanat-lukio payment-module-keyword))
