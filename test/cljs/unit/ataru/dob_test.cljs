(ns ataru.dob-test
  (:require [ataru.dob :as dob]
            [cljs.test :refer-macros [deftest is testing]]))

(deftest dob-identification
  (doall
    (for [[dob expected] [["1.1.2010" true]
                          ["01.01.2010" true]
                          ["1.01.2010" true]
                          ["01.1.2010" true]
                          ["1.13.2010" true] ; purposefully true
                          ["31.1.2010" true]
                          ["29.10.2010" true]
                          ["40.10.2010" true] ; purposefully true
                          ["41.1.1984" true] ; purposefully true
                          [nil false]]]
      (is (= expected (dob/dob? dob)) (str dob " should " (when-not expected "not ") "be interpreted as a date")))))
