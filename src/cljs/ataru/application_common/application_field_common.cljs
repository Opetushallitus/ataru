(ns ataru.application-common.application-field-common)

(defn answer-key [field-data]
  (keyword (:id field-data)))

(defn required-hint [field-descriptor] (if (some #(= % "required") (:validators field-descriptor)) " *" ""))

(defn textual-field-value [field-descriptor application & {:keys [lang]}]
  (let [key                (answer-key field-descriptor)
        value-or-koodi-uri (:value (get (:answers application) key))]
    (if-some [koodi-value (->> (:options field-descriptor)
                               (filter (comp (partial = value-or-koodi-uri)
                                             :value))
                               (map #(get-in % [:label lang]))
                               first)]
      koodi-value
      ; If there isn't a suitable value in field-descriptor
      ; we might be in the officer side - there the backend
      ; already has replaced the koodi URI with a human-readable
      ; label -> just return the koodi-uri if koodi-value is nil
      value-or-koodi-uri)))

(defn scroll-to-anchor
  [field-descriptor]
  [:span.application__scroll-to-anchor {:id (str "scroll-to-" (:id field-descriptor))} "."])
