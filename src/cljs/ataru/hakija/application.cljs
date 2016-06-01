(ns ataru.hakija.application
  "Pure functions handling application data"
  (:require [cljs.core.match :refer-macros [match]]))

(defn- flatten-form-fields [fields]
  (flatten
    (for [field fields]
      (match
        [field] [{:fieldClass "wrapperElement"
                  :children   children}] children
        :else field))))

(defn- initial-valid-status [flattened-form-fields]
  (into {}
        (map
          (fn [field]
            [(keyword (:id field)) {:valid (not (:required field))}]) flattened-form-fields)))

(defn create-initial-answers
  "Create initial answer structure based on form structure. Mainly validity for now."
  [form]
  (initial-valid-status (flatten-form-fields (:content form))))
