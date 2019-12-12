(ns ataru.unit-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [ataru.cljs-util-test]
            [ataru.dob-test]
            [ataru.virkailija.editor.handlers-test]
            [ataru.hakija.application-test]
            [ataru.hakija.application-validators-test]
            [ataru.hakija.rules-test]
            [ataru.hakija.banner-test]
            [ataru.component-data.value-transformers-test]))

(doo-tests 'ataru.cljs-util-test
           'ataru.dob-test
           'ataru.virkailija.editor.handlers-test
           'ataru.hakija.application-test
           'ataru.hakija.application-validators-test
           'ataru.hakija.rules-test
           'ataru.hakija.banner-test
           'ataru.component-data.value-transformers-test)
