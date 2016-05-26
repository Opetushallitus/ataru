(ns ataru.virkailija.browser-runner
    (:require [cljs.test :refer-macros [run-all-tests]]
              [ataru.virkailija.core-test]))

(defn ^:export run
  []
  (run-all-tests #"lomake-editori.*-test"))
