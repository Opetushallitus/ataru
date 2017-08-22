(ns ataru.application-common.application-field-common)

(defn answer-key [field-data]
  (keyword (:id field-data)))

(def required-validators
  #{"required"
    "postal-code"
    "postal-office"
    "home-town"
    "city"
    "hakukohteet"
    "birthplace"})
(def contains-required-validators? (partial contains? required-validators))

(defn is-required-field?
  [field-descriptor]
  (if (contains? field-descriptor :children)
    (some is-required-field? (:children field-descriptor))
    (some contains-required-validators? (:validators field-descriptor))))

(defn required-hint
  [field-descriptor]
  (if (is-required-field? field-descriptor)
    " *"
    ""))

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

(defn- predefined-value-answer?
  "Does the answer have predefined values? Form elements like dropdowns
   and single and multi-choice buttons have fixed, predefined values, as
   opposed to a text input field where an user can provide anything as
   the answer."
  [{:keys [fieldClass fieldType options]}]
  (and (= fieldClass "formField")
       (some #{fieldType} ["dropdown" "singleChoice" "multipleChoice"])
       (not-empty options)))

(defn textual-field-value [field-descriptor application & {:keys [lang]}]
  (let [key                (answer-key field-descriptor)
        value-or-koodi-uri (:value (get (:answers application) key))
        is-koodisto?       (contains? field-descriptor :koodisto-source)
        split-values       (cond-> value-or-koodi-uri
                                   (and is-koodisto? (string? value-or-koodi-uri))
                                   (clojure.string/split #"\s*,\s*"))]
    (cond
      is-koodisto?
      (let [values (map (partial value-or-koodi-uri->label field-descriptor lang) split-values)]
        (if (= (count values) 1)
          (first values)
          [:ul.application__form-field-list
           (map wrap-value values)]))

      (predefined-value-answer? field-descriptor)
      (let [option (->> (:options field-descriptor)
                        (filter (comp (partial = value-or-koodi-uri) :value))
                        (first))]
        (get-in option [:label lang] value-or-koodi-uri))

      (and (sequential? split-values) (< 1 (count split-values)))
      [:ul.application__form-field-list
       (map wrap-value split-values)]

      :else value-or-koodi-uri)))

(defn scroll-to-anchor
  [field-descriptor]
  [:span.application__scroll-to-anchor {:id (str "scroll-to-" (:id field-descriptor))} "."])
