(ns ataru.application-field-common)

(defn answer-key [field-data]
  (keyword (:id field-data)))

(defn required-hint [field-descriptor] (if (-> field-descriptor :required) " *" ""))

(defn textual-field-value [field-descriptor application]
  (:value ((answer-key field-descriptor) (:answers application))))
