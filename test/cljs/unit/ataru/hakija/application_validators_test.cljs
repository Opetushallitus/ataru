(ns ataru.hakija.application-validators-test
  (:require [ataru.fixtures.email :as email]
            [ataru.fixtures.postal-code :as postal-code]
            [ataru.fixtures.ssn :as ssn]
            [ataru.hakija.application-validators :as validator]
            [cljs.test :refer-macros [deftest is]]))

(deftest ssn-validation
  (doseq [ssn ssn/ssn-list]
    (doseq [century-char ["+" "-" "A"]]
      (let [ssn (str (:start ssn) century-char (:end ssn))]
        (is (validator/validate "ssn" ssn) (str "SSN " ssn " is not valid"))))))

(deftest email-validation
  (doseq [email (keys email/email-list)]
    (let [expected (get email/email-list email)
          pred     (if expected true? false?)
          actual   (validator/validate "email" email)
          message  (if expected "valid" "invalid")]
      (is (pred actual)
          (str "email " email " was not " message)))))

(deftest postal-code-validation
  (doseq [postal-code (keys postal-code/postal-code-list)]
    (let [expected (get postal-code/postal-code-list postal-code)
          pred     (if expected true? false?)
          actual   (validator/validate "postal-code" postal-code)
          message  (if expected "valid" "invalid")]
      (is (pred actual)
          (str "postal code " postal-code " was not " message)))))
