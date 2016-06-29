(ns ataru.virkailija.browser-runner
    (:require [cljs.test :refer-macros [run-all-tests run-tests]]
              [ataru.virkailija.core-test]
              [ataru.virkailija.virkailija-integration-test]))

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (if (cljs.test/successful? m)
    (println "*** TEST SUCCESS")
    (println "*** TEST FAIL")))

(defn ^:export run
  []
  (enable-console-print!)
  ;(run-tests 'ataru.virkailija.core-test)
  (run-tests 'ataru.virkailija.virkailija-integration-test))
