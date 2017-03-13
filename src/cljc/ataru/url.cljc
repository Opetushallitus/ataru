(ns ataru.url)

(defn items->query-part [param coll]
  (map-indexed (fn [idx key]
                 (let [separator (if (= idx 0) "?" "&")]
                   (str separator param "=" key)))
               coll))
