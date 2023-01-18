(ns ataru.preferred-name
  (:require [clojure.string :as string]))

(defn main-first-name?
  [{:keys [value answers-by-key]}]
  (let [first-names     (clojure.string/split (clojure.string/trim (or (-> answers-by-key :first-name :value)
                                                                       "")) #"[\s-]+")
        num-first-names (count first-names)
        possible-names  (set
                          (for [sub-length (range 1 (inc num-first-names))
                                start-idx  (range 0 num-first-names)
                                :when (<= (+ sub-length start-idx) num-first-names)]
                            (clojure.string/join " " (subvec first-names start-idx (+ start-idx sub-length)))))]
    (or (and (empty? (filter #(not (string/blank? %)) first-names)) (string/blank? value))
        (contains? possible-names (clojure.string/replace value "-" " ")))))
