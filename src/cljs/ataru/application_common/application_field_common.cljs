(ns ataru.application-common.application-field-common)

(defn answer-key [field-data]
  (keyword (:id field-data)))

(defn required-hint [field-descriptor] (if (some #(= % "required") (:validators field-descriptor)) " *" ""))

(defn- get-koodi [field-descriptor koodi-uri]
  (->> (:options field-descriptor)
       (filter (fn [koodi]
                 (= (:value koodi) koodi-uri)))
       first))

(defn- value-or-koodi-uri->label [field-descriptor lang value-or-koodi-uri]
  (if-let [koodi (get-koodi field-descriptor value-or-koodi-uri)]
    (get-in koodi [:label lang])
    value-or-koodi-uri))

(defn- wrap-value [value]
  ^{:key value}
  [:li value])

(defn textual-field-value [field-descriptor application & {:keys [lang]}]
  (let [key                (answer-key field-descriptor)
        value-or-koodi-uri (:value (get (:answers application) key))
        split-values       (cond-> value-or-koodi-uri
                                   (string? value-or-koodi-uri) (clojure.string/split #"\s*,\s*"))]
    (cond
      (contains? field-descriptor :koodisto-source)
      (let [values (map (partial value-or-koodi-uri->label field-descriptor lang) split-values)]
        (if (= (count values) 1)
          (first values)
          [:ul.application__form-field-list
           (map wrap-value values)]))

      (and (sequential? split-values) (< 1 (count split-values)))
      [:ul.application__form-field-list
       (map wrap-value split-values)]

      :else value-or-koodi-uri)))

(defn scroll-to-anchor
  [field-descriptor]
  [:span.application__scroll-to-anchor {:id (str "scroll-to-" (:id field-descriptor))} "."])
