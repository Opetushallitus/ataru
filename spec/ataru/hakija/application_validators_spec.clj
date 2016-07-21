(ns ataru.hakija.application-validators-spec
  (:require [ataru.fixtures.email :as email]
            [ataru.fixtures.postal-code :as postal-code]
            [ataru.fixtures.ssn :as ssn]
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
         (map (fn [century-char]
                (let [ssn (str (:start ssn) century-char (:end ssn))]
                  (it (str "should validate " ssn)
                    (should (validator/validate "ssn" ssn)))))
              ["+" "-" "A"]))
       ssn/ssn-list))

(describe "email validator"
  (tags :unit)

  (map (fn [email]
         (let [expected (get email/email-list email)
               pred     (if expected true? false?)
               actual   (validator/validate "email" email)]
           (it (str "should validate " email)
             (should (pred actual)))))
       (keys email/email-list)))

(describe "postal code validation"
  (tags :unit)

  (map (fn [postal-code]
         (let [expected (get postal-code/postal-code-list postal-code)
               pred     (if expected true? false?)
               actual   (validator/validate "postal-code" postal-code)]
           (it (str "should validate " postal-code)
             (should (pred actual)))))
    (keys postal-code/postal-code-list)))
