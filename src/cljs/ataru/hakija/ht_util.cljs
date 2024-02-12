(ns ataru.hakija.ht-util)

(defn warning-to-set [seconds-left polling-interval-seconds extra-margin-seconds previous-warnings]
  (let [warning-target-minutes '(30 20 10)
        should-set-warning-fn (fn [warning-target seconds-left]
                                (let [target-seconds (* 60 warning-target)
                                      warning-target-in-future? (<= target-seconds seconds-left)
                                      reaching-warning-target-soon? (>= (+ target-seconds polling-interval-seconds extra-margin-seconds) seconds-left)
                                      not-previously-activated? (not (contains? previous-warnings warning-target))]
                                  (when (and
                                          warning-target-in-future?
                                          reaching-warning-target-soon?
                                          not-previously-activated?)
                                    [warning-target (* 1000 (- seconds-left target-seconds))])))]

    (first (filter some? (map #(should-set-warning-fn % seconds-left) warning-target-minutes)))))
