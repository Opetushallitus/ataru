(ns lomake-editori.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [lomake-editori.core-test]))

(doo-tests 'lomake-editori.core-test)
