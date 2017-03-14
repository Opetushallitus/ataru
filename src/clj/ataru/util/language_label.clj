(ns ataru.util.language-label)

(defn lang-label [lang label-map]
  (let [label-string (lang label-map)]
    (if (clojure.string/blank? label-string)
      nil
      label-string)))

(defn get-language-label-in-preferred-order
  "Returns a label of any language, preferring fi, then sv and then en"
  [label]
  (if (and (string? label)
           (not (clojure.string/blank? label)))
    ; some broken applications have bare string labels
    (clojure.string/trim label)
    (or
      (lang-label :fi label)
      (lang-label :sv label)
      (lang-label :en label))))
