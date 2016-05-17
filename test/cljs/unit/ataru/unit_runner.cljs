(ns ataru.unit-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [lomake-editori.editor.handlers-test]))

(doo-tests 'lomake-editori.editor.handlers-test)
