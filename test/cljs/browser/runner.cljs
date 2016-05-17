(ns lomake-editori.runner
    (:require [cljs.test :refer-macros [run-all-tests]]
              [lomake-editori.core-test]))

(defn ^:export run
  []
  (run-all-tests #"lomake-editori.*-test"))
