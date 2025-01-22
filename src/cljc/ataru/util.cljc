(ns ataru.util
  (:require #?(:cljs [ataru.cljs-util :as util])
            #?(:cljs [goog.string :as gstring])
            [clojure.string :as string]
            [ataru.application.option-visibility :as option-visibility]
            [medley.core :refer [find-first]])
  (:import #?(:clj [java.util UUID])))

(defn is-question-group-answer? [value]
  (and (vector? value)
       (not-empty value)
       (or (vector? (first value))
           (nil? (first value)))))

(defn gender-int-to-string [gender]
  (case gender
    "1" "mies"
    "2" "nainen"
    nil))

(defn map-kv [m f]
  (reduce-kv #(assoc %1 %2 (f %3)) {} m))

(defn group-by-first [kw m]
  (-> (group-by kw m)
      (map-kv first)))

(defn component-id []
  #?(:cljs (util/new-uuid)
     :clj  (str (UUID/randomUUID))))

(declare flatten-form-fields)

(defn- flatten-form-field [field]
  (let [children  (->> (:children field)
                       (map #(assoc % :children-of (:id field)))
                       flatten-form-fields)
        followups (->> (:options field)
                       (mapcat (fn [option]
                                 (map #(assoc %
                                              :followup-of (:id field)
                                              :option-value (:value option))
                                      (:followups option))))
                       flatten-form-fields)]
    (cons (cond-> (dissoc field :children)
                  (contains? field :options)
                  (update :options (partial mapv #(dissoc % :followups))))
          (concat children followups))))

(defn flatten-form-fields [fields]
  (vec (mapcat flatten-form-field fields)))

(defn find-descendant-ids-by-parent-id
  [flat-form-content parent-id]
  (let [descendant-ids (mapv :id (filter #(or (= parent-id (:followup-of %)) (= parent-id (:children-of %)))
                                         flat-form-content))
        nested-ids (flatten (map #(find-descendant-ids-by-parent-id flat-form-content %) descendant-ids))]
    (concat descendant-ids nested-ids)))

(defn answerable? [field]
  (not (contains? #{"infoElement" "modalInfoElement" "wrapperElement" "questionGroup" "formPropertyField"}
                  (:fieldClass field))))

(defn find-field [fields id]
  (cond (empty? fields)
        nil
        (= id (:id (first fields)))
        (first fields)
        :else
        (recur (into (rest fields)
                     (concat (:children (first fields))
                             (mapcat :followups (:options (first fields)))))
               id)))

(declare map-form-fields)

(defn map-form-field [f field]
  (let [new-field (f field)]
    (cond-> new-field
            (contains? new-field :children)
            (update :children (partial map-form-fields f))
            (contains? new-field :options)
            (update :options (partial mapv (fn [option]
                                             (cond-> option
                                                     (contains? option :followups)
                                                     (update :followups (partial map-form-fields f)))))))))

(defn map-form-fields [f fields]
  (mapv (partial map-form-field f) fields))

(defn form-fields-by-id [form]
  (->> form
       :content
       flatten-form-fields
       (group-by-first (comp keyword :id))))

(defn- form-sections-by-id [form]
  (->> form
       :content
       (filter #(= "wrapperElement" (:fieldClass %)))
       (group-by-first (comp keyword :id))))

(def form-sections-by-id-memo (memoize form-sections-by-id))

(defn form-attachment-fields [form]
  (->> form
       :content
       flatten-form-fields
       (filter #(= "attachment" (:fieldType %)))))

(defn- get-readable-koodi-value
  [koodisto value]
  (-> (filter #(= value (:value %)) koodisto)
      first
      :label
      :fi
      (or "")))

(defn populate-answer-koodisto-values
  [values field get-koodisto-options]
  (if (:koodisto-source field)
    (let [koodisto (get-koodisto-options (-> field :koodisto-source :uri)
                                         (-> field :koodisto-source :version)
                                         (-> field :koodisto-source :allow-invalid?))]
      (cond (is-question-group-answer? values)
            (mapv (fn [value]
                    (when (some? value)
                      (mapv #(get-readable-koodi-value koodisto %) value)))
                  values)

            (vector? values)
            (mapv #(get-readable-koodi-value koodisto %) values)

            :else
            (get-readable-koodi-value koodisto values)))
    values))

(defn reduce-form-fields [f init [field & fs :as fields]]
  (if (empty? fields)
    init
    (recur f
           (f init field)
           (concat fs
                   (:children field)
                   (mapcat :followups (:options field))))))

(defn answers-by-key [answers]
  (group-by-first (comp keyword :key) answers))

(defn application-answers-by-key [application]
  (-> application :content :answers answers-by-key))

(defn group-answers-by-wrapperelement [wrapper-fields answers-by-key]
  (into {}
    (for [{:keys [id children]} wrapper-fields
          :let [top-level-children children]]
      {id (loop [acc []
                 [{:keys [id children]} & rest-of-fields] top-level-children]
            (if (not-empty children)
              (recur acc (concat children rest-of-fields))
              ; this is the ANSWER id, NOT section/wrapperElement id
              (if id
                (recur (conj acc
                         {(keyword id)
                          (get answers-by-key (keyword id))})
                  rest-of-fields)
                acc)))})))

(defn find-wrapper-parent [flat-form-fields field]
  (let [field-from-flattened-fields (find-first #(= (:id %) (:id field)) flat-form-fields)
        parent-id (or (:children-of field-from-flattened-fields) (:followup-of field-from-flattened-fields))
        parent-element (find-first #(= (:id %) parent-id) flat-form-fields)]
    (when parent-element
      (if (= "wrapperElement" (:fieldClass parent-element))
        parent-element
        (find-wrapper-parent flat-form-fields parent-element)))))

(def ^:private b-limit 1024)
(def ^:private kb-limit 102400)
(def ^:private mb-limit (* 1024 1024 1024))

(defn size-bytes->str
  ([bytes] (size-bytes->str bytes true))
  ([bytes unit?]
   #?(:cljs (condp > bytes
              b-limit  (str bytes (when unit? " B"))
              kb-limit (gstring/format (str "%.01f" (when unit? " kB")) (/ bytes 1024))
              mb-limit (gstring/format (str "%.01f" (when unit? " MB")) (/ bytes 1024 1024))
              (gstring/format (str "%.01f" (when unit? " GB")) (/ bytes 1024 1024 1024)))
      :clj (condp > bytes
             b-limit  (str bytes (when unit? " B"))
             kb-limit (format (str "%.2f" (when unit? " kB")) (float (/ bytes 1024)))
             mb-limit (format (str "%.2f" (when unit? " MB")) (float (/ bytes 1024 1024)))
             (format (str "%.2f" (when unit? " GB")) (float (/ bytes 1024 1024 1024)))))))

(defn remove-nth
  "remove nth elem in vector"
  [v n]
  (vec (concat (subvec v 0 n) (subvec v (inc n)))))

(defn add-to-nth
  [v n x]
  (vec (concat (subvec v 0 n) [x] (subvec v n))))

(defn in?
  [vec item]
  (some #(= item %) vec))

(defn not-blank? [s]
  (not (string/blank? s)))

(defn not-blank [s]
  (when (not-blank? s) s))

(defn non-blank-answer? [answer-entity]
  (let [value (:value answer-entity)]
    (if (vector? value)
      (if (or (vector? (first value)) (nil? (first value)))
        (some #(and (not-blank? (first %)) (every? some? %)) value)
        (and (not-blank? (first value)) (every? some? value)))
      (not-blank? value))))

(defn application-in-processing? [application-hakukohde-reviews]
  (boolean (some #(and (= "processing-state" (:requirement %))
                       (not (contains? #{"unprocessed" "information-request"}
                              (:state %))))
             application-hakukohde-reviews)))

(defn answered-in-group-idx [answer-entity idx]
  (let [answer-arr (get answer-entity :value)]
    (boolean (and (vector? answer-arr)
                  (< idx (count answer-arr))
                  (let [answer (get answer-arr idx)]
                    (if (vector? answer)
                      (not-blank? (first answer))
                      (not-blank? answer)))))))

(defn any-answered? [answers fields]
  (let [field-ids (map #(keyword %) fields)]
    (some #(non-blank-answer? (get answers %)) field-ids)))

(defn remove-nil-values [m]
  (->> m
       (remove #(nil? (second %)))
       (into {})))

(def ^:private email-pred (comp (partial = "email") :key))

(defn extract-email [application]
  (->> (:answers application)
       (filter email-pred)
       (first)
       :value))

(defn non-blank-val [m ks]
  (some #(not-blank (get m %)) ks))

(defn from-multi-lang [text lang]
  (non-blank-val text [lang :fi :sv :en]))

(defn from-multi-lang-object [object lang kw]
  (some #(not-blank (get-in object [% kw])) [lang :fi :sv :en]))

(defn indices-of [f coll]
  (keep-indexed #(if (f %2) %1 nil) coll))

(defn first-index-of [f coll]
  (first (indices-of f coll)))

(defn assoc?
  ; https://stackoverflow.com/questions/16356888/assoc-if-in-clojure
  "Same as assoc, but skip the assoc if v is nil"
  [m & kvs]
  (->> kvs
       (partition 2)
       (filter #(some? (second %)))
       (map vec)
       (into m)))

(defn should-search? [search-term]
  (> (count search-term) 1))

(defn- term-text-matches
  [text matches index term]
  (if-let [match-start (clojure.string/index-of text term index)]
    (let [match-end (+ match-start (count term))]
      (recur text (conj matches [match-start match-end]) match-end term))
    matches))

(defn- text-matches
  [text search-terms all-terms-must-match?]
  (let [text         (clojure.string/lower-case text)
        search-terms (->> search-terms
                          (filter should-search?)
                          (map clojure.string/lower-case))
        matches      (map (partial term-text-matches text [] 0) search-terms)]
    (if (and all-terms-must-match?
             (some empty? matches))
      []
      (mapcat identity matches))))

(defn- combine-overlapping-matches
  [text-matches]
  (reduce
    (fn [acc [match-start match-end]]
      (let [[previous-start previous-end] (last acc)]
        (if (and previous-start (<= match-start previous-end))
          (conj (vec (butlast acc)) [previous-start match-end])
          (conj acc [match-start match-end]))))
    []
    (sort text-matches)))

(defn match-text [text search-terms all-terms-must-match?]
  (if (or (empty? search-terms)
          (every? false? (map should-search? search-terms)))
    [{:text text :hilight false}]
    (let [highlights (-> text
                         (text-matches search-terms all-terms-must-match?)
                         (combine-overlapping-matches))]
      (loop [res           []
             current-index 0
             [[match-begin match-end] & rest-highlights] highlights]
        (cond
          (nil? match-begin)
          (if (= current-index (count text))
            res
            (conj res {:text    (subs text current-index)
                       :hilight false}))

          (< current-index match-begin)
          (recur (conj res
                       {:text    (subs text current-index match-begin)
                        :hilight false}
                       {:text    (subs text match-begin match-end)
                        :hilight true})
                 match-end
                 rest-highlights)

          :else
          (recur (conj res
                       {:text    (subs text current-index match-end)
                        :hilight true})
                 match-end
                 rest-highlights))))))

(defn collect-ids [acc {:keys [id children options]}]
  (let [acc (reduce collect-ids acc (mapcat :followups options))
        acc (reduce collect-ids acc children)]
    (conj acc id)))

(defn non-blank-option-label [option langs]
  (non-blank-val (:label option) langs))

(defn visibility-conditions [content]
  (->> content
       (keep :section-visibility-conditions)
       flatten))

(defn- fields-with-visibility-rules [form]
  (filter :section-visibility-conditions (flatten-form-fields (:content form))))

(def fields-with-visibility-rules-memo
  (memoize fields-with-visibility-rules))

(defn- visibility-condition-applies-to-field?
  [visibility-condition field]
  (let [field-name (-> field :id keyword)
        section-name (-> visibility-condition :section-name keyword)]
    (= section-name field-name)))

(defn- visibility-conditions-on-field
  [form answers field usememo?]
  (let [fields-with-visibility-rules (if usememo?
                                       (fields-with-visibility-rules-memo form)
                                       (fields-with-visibility-rules form))]
    (mapcat
      (fn [{conditions :section-visibility-conditions condition-owner-id :id}]
        (keep
          (fn [visibility-condition]
            (when (visibility-condition-applies-to-field? visibility-condition field)
              (let [value (get-in answers [(keyword condition-owner-id) :value])
                    answer-compared-to (-> visibility-condition :condition :answer-compared-to)
                    processed-value (if (is-question-group-answer? value)
                                      (->> value
                                           flatten
                                           (filter #(= % answer-compared-to))
                                           first)
                                      value)]
                (assoc visibility-condition :value processed-value))))
          conditions))
      fields-with-visibility-rules)))

(defn- condition-quantifier
  [condition]
  (case (:data-type (:condition condition))
    "str" :some
    :every))

(defn- every-condition-satisfied
  [conditions]
  (and
    (seq conditions)
    (every?
      (fn [{value :value :as condition}]
        (option-visibility/answer-satisfies-condition? value condition))
      conditions)))

(defn- some-condition-satisfied
  [conditions]
  (and
    (seq conditions)
    (some
      (fn [{value :value :as condition}]
        (option-visibility/answer-satisfies-condition? value condition))
      conditions)))

(defn is-field-hidden-by-section-visibility-conditions
  ([form answers field]
  (is-field-hidden-by-section-visibility-conditions form answers field true))
  ([form answers field usememo?]
  (let [visibility-conditions (visibility-conditions-on-field form answers field usememo?)
        by-quantifier         (group-by condition-quantifier visibility-conditions)]
    (or
      (every-condition-satisfied (seq (:every by-quantifier)))
      (some-condition-satisfied (seq (:some by-quantifier)))))))

(defn distinct-by [f coll]
  (map #(first (second %))
    (group-by f coll)))

(defn to-vec
  "Get value wrapped into vector, if it's not a vector"
  [val] 
  (if (vector? val) val [val]))

(defn koodi-uri-base [koodi-uri] (-> koodi-uri (string/split #"#") first))
