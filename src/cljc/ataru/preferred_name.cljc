(ns ataru.preferred-name)

(defn main-first-name?
  [value answers-by-key _]
  (let [first-names     (clojure.string/split (-> answers-by-key :first-name :value) #"[\s-]+")
        num-first-names (count first-names)
        possible-names  (set
                          (for [sub-length (range 1 (inc num-first-names))
                                start-idx  (range 0 num-first-names)
                                :when (<= (+ sub-length start-idx) num-first-names)]
                            (clojure.string/join " " (subvec first-names start-idx (+ start-idx sub-length)))))]
    (contains? possible-names (clojure.string/replace value "-" " "))))