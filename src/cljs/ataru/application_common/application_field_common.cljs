(ns ataru.application-common.application-field-common)

(defn answer-key [field-data]
  (keyword (:id field-data)))

(defn required-hint [field-descriptor] (if (some #(= % "required") (:validators field-descriptor)) " *" ""))

(defn textual-field-value [field-descriptor application]
  (:value ((answer-key field-descriptor) (:answers application))))

(defn scroll-to-anchor
  [field-descriptor]
  [:span.application__scroll-to-anchor {:id (str "scroll-to-" (:id field-descriptor))} "."])
