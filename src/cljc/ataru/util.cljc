(ns ataru.util
  (:require #?(:cljs [ataru.cljs-util :as util])
            #?(:clj  [clojure.core.match :refer [match]]
               :cljs [cljs.core.match :refer-macros [match]])
            #?(:clj  [taoensso.timbre :refer [spy debug]]
               :cljs [taoensso.timbre :refer-macros [spy debug]])
            #?(:cljs [goog.string :as gstring])
            #?(:clj  [clj-time.core :as time]
               :cljs [cljs-time.core :as time])
            #?(:clj  [clj-time.coerce :refer [from-long]]
               :cljs [cljs-time.coerce :refer [from-long]]))
  (:import #?(:clj [java.util UUID])))

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

(defn flatten-form-fields [fields]
  (flatten
    (for [field fields
          :when (not= "infoElement" (:fieldClass field))]
      (match
       field
       {:fieldClass (:or "wrapperElement" "questionGroup")
        :children   children}
        (flatten-form-fields (map #(assoc % :children-of (:id field)) children))

       {:fieldType (:or "dropdown" "multipleChoice" "singleChoice")
        :options   options}
       (cons field
             (mapcat (fn [option]
                       (map (fn [followup]
                              (cond-> followup
                                      (not (contains? followup :followup-of))
                                      (assoc :followup-of (:id field)
                                             :option-value (:value option))))
                            (flatten-form-fields (:followups option))))
                     options))
       :else field))))

(defn form-fields-by-id [form]
  (->> form
       :content
       flatten-form-fields
       (group-by-first (comp keyword :id))))

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
                                         (-> field :koodisto-source :version))]
      (cond
        (and (sequential? values)
             (every? sequential? values))
        (mapv (fn [value]
               (mapv #(get-readable-koodi-value koodisto %) value))
             values)

        (sequential? values)
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
    (for [{:keys [id children] :as field} wrapper-fields
          :let [top-level-children children
                section-id id]]
      {id (loop [acc []
                 [{:keys [id children] :as field} & rest-of-fields] top-level-children]
            (if (not-empty children)
              (recur acc (concat children rest-of-fields))
              ; this is the ANSWER id, NOT section/wrapperElement id
              (if id
                (recur (conj acc
                         {(keyword id)
                          (get answers-by-key (keyword id))})
                  rest-of-fields)
                acc)))})))

(defn followups? [dropdown-options]
  (some some? (mapcat :followups dropdown-options)))

(defn resolve-followups [dropdown-options value]
  (and
    value
    (->> dropdown-options
        (filter (comp (partial = value) :value))
      (mapcat :followups))))

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

(defn in?
  [vec item]
  (some #(= item %) vec))

(defn not-blank? [s]
  (not (clojure.string/blank? s)))

(defn not-blank [s]
  (when (not-blank? s) s))

(defn application-in-processing? [application-hakukohde-reviews]
  (boolean (some #(and (= "processing-state" (:requirement %))
                       (not (contains? #{"unprocessed" "information-request"}
                              (:state %))))
             application-hakukohde-reviews)))

(defn remove-nil-values [m]
  (->> m
       (filter second)
       (into {})))

(def ^:private email-pred (comp (partial = "email") :key))

(defn extract-email [application]
  (->> (:answers application)
       (filter email-pred)
       (first)
       :value))

(defn non-blank-val [m ks]
  (some #(not-blank (get m %)) ks))

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
