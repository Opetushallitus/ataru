(ns ataru.collections
  (:require [ataru.number :as number]))

(defn before?
  "Testaa onko a ennen b:tä annetussa coll:ssa iteroimatta turhia"
  [a b coll]
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

(defn generate-missing-values
  "Iterate through coll and to generate :value field, if it does not exist"
  [coll]
  (:options
    (reduce (fn [{:keys [next-value options]} option]
            (if (:value option)
              {:next-value next-value
               :options    (conj options option)}
              {:next-value (inc next-value)
               :options    (conj options (assoc option :value (str next-value)))}))
          {:next-value (if (seq coll)
                         (->> coll
                              (map #(number/->int (:value %)))
                              (map #(if (nil? %) -1 %))
                              (apply max)
                              inc)
                         0)
           :options    []}
          coll)))
