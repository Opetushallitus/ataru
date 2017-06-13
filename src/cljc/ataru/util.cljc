(ns ataru.util
  (:require #?(:cljs [ataru.cljs-util :as util])
            #?(:clj  [clojure.core.match :refer [match]]
               :cljs [cljs.core.match :refer-macros [match]])
            #?(:clj  [taoensso.timbre :refer [spy debug]]
               :cljs [taoensso.timbre :refer-macros [spy debug]])
            #?(:cljs [goog.string :as gstring]))
  (:import #?(:clj [java.util UUID])))

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
        {:fieldClass "wrapperElement"
         :children   children}
        (flatten-form-fields children)

        {:fieldType (:or "dropdown" "multipleChoice" "singleChoice")
         :options options}
        (cons field
          (->> options
               (mapcat :followups)
               (mapcat (fn [{:keys [children] :as followup}]
                         (map #(assoc % :followup? true)
                           (cons followup
                             (flatten-form-fields children)))))))

        :else field))))

(defn answers-by-key [answers]
  (group-by-first (comp keyword :key) answers))

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

(defn size-bytes->str [bytes]
  #?(:cljs (condp > bytes
             b-limit (str bytes "B")
             kb-limit (gstring/format "%.01fkB" (/ bytes 1024))
             (gstring/format "%.01fMB" (/ bytes 1024000)))
     :clj (condp > bytes
            b-limit (str bytes " B")
            kb-limit (format "%.2f kB" (float (/ bytes 1024)))
            (format "%.2f MB" (float (/ bytes 1024000))))))

(defn remove-nth
  "remove nth elem in vector"
  [v n]
  (vec (concat (subvec v 0 n) (subvec v (inc n)))))