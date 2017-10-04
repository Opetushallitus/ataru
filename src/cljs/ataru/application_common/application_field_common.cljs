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

(defn textual-field-value [field-descriptor application & {:keys [lang question-group-index]}]
  (let [key                (answer-key field-descriptor)
        value-or-koodi-uri (if question-group-index
                             (-> application :answers key :value (nth question-group-index))
                             (-> application :answers key :value))
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

(defn group-spacer
  [index]
  ^{:key (str "spacer-" index)}
  [:div.application__question-group-spacer])

(defn scroll-to-anchor
  [field-descriptor]
  [:span.application__scroll-to-anchor {:id (str "scroll-to-" (:id field-descriptor))} "."])

(defn question-group-answer? [answers]
  (letfn [(l? [x]
            (or (list? x)
                (vector? x)
                (seq? x)))]
    (and (every? l? answers)
         (every? (partial every? l?) answers))))

(defn answers->read-only-format
  "Converts format of repeatable answers in a question group from the one
   stored by a form component into a format required by read-only views.

   Let adjacent fieldset with repeatable answers in a question group:

   Group 1:
   a1 - b1 - c1
   a2 - b2 - c2

   Group 2:
   d1 - e1 - f1

   This reduce converts:
   ([[\"a1\" \"a2\"] [\"d1\"]] [[\"b1\" \"b2\"] [\"e1\"]] [[\"c1\" \"c2\"] [\"f1\"]])

   to:
   [[[\"a1\" \"b1\" \"c1\"] [\"a2\" \"b2\" \"c2\"]] [[\"d1\" \"e1\" \"f1\"]]]"
  [answers]
  (let [val-or-empty-vec (fnil identity [])]
    (reduce (fn [acc [col-idx answers]]
              (reduce (fn [acc [question-group-idx answers]]
                        (reduce (fn [acc [row-idx answer]]
                                  (-> acc
                                      (update-in [question-group-idx row-idx] val-or-empty-vec)
                                      (assoc-in [question-group-idx row-idx col-idx] answer)))
                                (update acc question-group-idx val-or-empty-vec)
                                (map vector (range) answers)))
                      acc
                      (map vector (range) answers)))
            []
            (map vector (range) answers))))
