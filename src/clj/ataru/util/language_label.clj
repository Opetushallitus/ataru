(ns ataru.util.language-label)

(defn lang-label [lang label-map]
  (let [label-string (lang label-map)]
    (if (or
         (not label-string)
         (empty? (clojure.string/trim label-string)))
      nil
      label-string)))

(defn get-language-label-in-preferred-order
  "Returns a label of any language, preferring fi, then sv and then en"
  [label]
  (or
   (lang-label :fi label)
   (lang-label :sv label)
   (lang-label :en label)))
