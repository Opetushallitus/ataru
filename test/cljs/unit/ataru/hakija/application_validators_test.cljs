(ns ataru.hakija.application-validators-test
  (:require [ataru.fixtures.ssn :as ssn]
            [ataru.hakija.application-validators :as validator]
            [cljs.test :refer-macros [deftest is]]))

(deftest ssn-validation
  (doseq [ssn ssn/ssn-list]
    (is (validator/validate "ssn" ssn) (str "SSN " ssn " is not valid"))))
