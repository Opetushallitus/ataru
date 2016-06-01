(ns ataru.unit-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [ataru.virkailija.editor.handlers-test]
            [ataru.hakija.application-test]))

(doo-tests 'ataru.virkailija.editor.handlers-test
           'ataru.hakija.application-test)
