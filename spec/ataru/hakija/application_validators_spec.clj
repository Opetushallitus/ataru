(ns ataru.hakija.application-validators-spec
  (:require [ataru.fixtures.ssn :as ssn]
            [ataru.hakija.application-validators :as validator]
            [speclj.core :refer :all]))

(describe "required validator"
  (tags :unit)

  (it "should not allow nil"
    (should-not (validator/validate "required" nil)))

  (it "should not allow empty string"
    (should-not (validator/validate "required" "")))

  (it "should not allow string with only whitespace"
    (should-not (validator/validate "required" " ")))

  (it "should allow string with at least one character"
    (should (validator/validate "required" "a"))))

(describe "ssn validator"
  (tags :unit)

  (map (fn [ssn]
         (it (str "should validate " ssn)
          (should (validator/validate "ssn" ssn))))
       ssn/ssn-list))
