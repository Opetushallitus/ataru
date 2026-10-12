(ns ataru.component-data.value-transformers-test
  (:require [cljs.test :refer-macros [deftest is]]
            [ataru.component-data.value-transformers :as t]))

(deftest transforms-dob-string
  (doall
    (for [[expected dob] [["01.10.2010" "1.10.2010"]
                          ["10.01.2010" "10.1.2010"]
                          ["01.01.2010" "1.1.2010"]
                          ["10.10.2010" "10.10.2010"]]]
      (is (= (t/birth-date dob) expected)))))

(deftest transforms-cas-oppija-dob-string
  (doall
    (for [[expected dob] [["01.10.2010" "2010-10-01"]
                          ["10.01.2010" "2010-1-10"]
                          ["01.01.2010" "2010-1-1"]
                          ["10.10.2010" "2010-10-10"]]]
      (is (= (t/cas-oppija-dob-to-ataru-dob dob) expected)))))
