(ns ataru.collections)

(defn before? [a b coll]
  "Testaa onko a ennen b:tä annetussa coll:ssa iteroimatta turhia"
  (true? (and a
              b
              (not= a b)
              (->> coll
                   (partition-all 2 1)
                   (transduce (comp (map (fn [[a' b']]
                                           (cond (and (= a' b)
                                                      a')
                                                 false

                                                 (and (= a' b)
                                                      (= b' a))
                                                 false

                                                 (and (= a' a)
                                                      b')
                                                 true)))
                                    (filter (comp not nil?)))
                              conj)
                   (first)))))

(defn generate-missing-values [coll]
  "Iterate through coll and to generate :value field, if it does not exist"
  (let [min-start-value 0
        to-number       (fn [value]
                          (let [result (ataru.number/->int value)]
                            (if (ataru.number/isNaN result) (dec min-start-value) result)))
        values          (map #(to-number (:value %)) coll)
        max-value       (apply max values)
        current         (atom (inc max-value))
        get-current (fn []
                      (let [previous-value @current]
                        (swap! current inc)
                        (str previous-value)))]
    (map #(assoc % :value (or (:value %) (get-current))) coll)))