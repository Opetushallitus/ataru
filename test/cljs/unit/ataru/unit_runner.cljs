(ns ataru.unit-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [ataru.cljs-util-test]
            [ataru.virkailija.editor.handlers-test]
            [ataru.hakija.application-test]))

(doo-tests 'ataru.cljs-util-test
           'ataru.virkailija.editor.handlers-test
           'ataru.hakija.application-test)
