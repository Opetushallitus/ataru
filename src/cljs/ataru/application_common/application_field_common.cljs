(ns ataru.application-common.application-field-common)

(defn answer-key [field-data]
  (keyword (:id field-data)))

(defn required-hint [field-descriptor] (if (some #(= % "required") (:validators field-descriptor)) " *" ""))

(defn- extract-value-from-koodisto [{:keys [options]} koodi-uri]
  (if-some [koodi-value (->> options
                             (filter (comp (partial = koodi-uri)
                                           :value))
                             first)]
    (get-in koodi-value [:label :fi])
    ; If there isn't a suitable value in field-descriptor
    ; we might be in the officer side - there the backend
    ; already has replaced the koodi URI with a human-readable
    ; label -> just return the koodi-uri if koodi-value is nil
    koodi-uri))

(defn textual-field-value [field-descriptor application]
  (let [key                (answer-key field-descriptor)
        value-or-koodi-uri (:value (get (:answers application) key))]
    (cond->> value-or-koodi-uri
      (contains? field-descriptor :koodisto-source)
      (extract-value-from-koodisto field-descriptor))))

(defn scroll-to-anchor
  [field-descriptor]
  [:span.application__scroll-to-anchor {:id (str "scroll-to-" (:id field-descriptor))} "."])
