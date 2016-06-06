(ns ataru.virkailija.browser-runner
    (:require [cljs.test :refer-macros [run-all-tests]]
              [ataru.virkailija.core-test]))

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (if (cljs.test/successful? m)
    (println "*** TEST SUCCESS")
    (println "*** TEST FAIL")))

(defn ^:export run
  []
  (enable-console-print!)
  (run-all-tests #"ataru.virkailija.*-test"))
