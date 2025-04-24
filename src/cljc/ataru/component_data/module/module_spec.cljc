(ns ataru.component-data.module.module-spec
  (:require [schema.core :as s]
            [clojure.string :as string]
            [ataru.component-data.kk-application-payment-module :refer [payment-module-name]]))

(s/defschema ModuleSpec
  {:foldable?                    s/Bool
   :can-cut?                     s/Bool
   :can-copy?                    s/Bool
   :can-remove?                  s/Bool
   :show-child-component-names?  s/Bool
   :has-multiple-configurations? s/Bool})

(def default-spec
  {:foldable?                    false
   :can-cut?                     true
   :can-copy?                    false
   :can-remove?                  false
   :show-child-component-names?  false
   :has-multiple-configurations? false})

(def person-info-module-spec
  {:foldable?                    false
   :can-cut?                     true
   :can-copy?                    false
   :can-remove?                  false
   :show-child-component-names?  true
   :has-multiple-configurations? true})

(def arvosanat-module-spec
  {:foldable?                    false
   :can-cut?                     true
   :can-copy?                    false
   :can-remove?                  true
   :show-child-component-names?  false
   :has-multiple-configurations? false})

(def kk-application-payment-module-spec
  {:foldable?                    true
   :can-cut?                     true
   :can-copy?                    true
   :can-remove?                  true
   :show-child-component-names?  false
   :has-multiple-configurations? false
   })

(s/defn get-module-spec :- ModuleSpec
  [module-name :- s/Str]
  (let [spec (cond
               (= module-name "person-info") person-info-module-spec
               (string/starts-with? module-name "arvosanat-") arvosanat-module-spec
               (= module-name payment-module-name) kk-application-payment-module-spec
               :else default-spec)]
    (s/validate ModuleSpec spec)))
