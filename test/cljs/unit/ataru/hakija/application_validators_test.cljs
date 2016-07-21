(ns ataru.hakija.application-validators-test
  (:require [ataru.fixtures.email :as email]
            [ataru.fixtures.ssn :as ssn]
            [ataru.hakija.application-validators :as validator]
            [cljs.test :refer-macros [deftest is]]))

(deftest ssn-validation
  (doseq [ssn ssn/ssn-list]
    (is (validator/validate "ssn" ssn) (str "SSN " ssn " is not valid"))))

(deftest email-validation
  (doseq [email (keys email/email-list)]
    (let [expected (get email/email-list email)
          pred     (if expected true? false?)
          actual   (validator/validate "email" email)
          message  (if expected "valid" "invalid")]
      (is (pred actual)
          (str "email " email " was not " message)))))
