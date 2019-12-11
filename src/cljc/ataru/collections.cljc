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
