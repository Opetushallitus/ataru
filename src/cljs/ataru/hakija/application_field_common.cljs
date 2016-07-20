(ns ataru.hakija.application-field-common)

(defn answer-key [field-data]
  (keyword (:id field-data)))

(defn required-hint [field-descriptor] (if (some #(= % :required) (:validators field-descriptor)) " *" ""))

(defn textual-field-value [field-descriptor application]
  (:value ((answer-key field-descriptor) (:answers application))))

(defn wrapper-id [field-descriptor]
  (str "wrapper-" (:id field-descriptor)))
