(ns ataru.hakija.application-validators-spec
  (:require [ataru.fixtures.email :as email]
            [ataru.fixtures.phone :as phone]
            [ataru.fixtures.date :as date]
            [ataru.fixtures.postal-code :as postal-code]
            [ataru.fixtures.ssn :as ssn]
            [ataru.fixtures.first-name :as first-name]
            [ataru.hakija.application-validators :as validator]
            [speclj.core :refer :all]))

(describe "required validator"
  (tags :unit)

  (it "should not allow nil"
    (should-not (validator/validate :required nil {})))

  (it "should not allow empty string"
    (should-not (validator/validate :required "" {})))

  (it "should not allow string with only whitespace"
    (should-not (validator/validate :required " " {})))

  (it "should allow string with at least one character"
    (should (validator/validate :required "a" {}))))

(describe "ssn validator"
  (tags :unit)

  (map (fn [ssn]
         (map (fn [century-char]
                (let [ssn (str (:start ssn) century-char (:end ssn))]
                  (it (str "should validate " ssn)
                    (should (validator/validate :ssn ssn {})))))
              ["A"]))
       ssn/ssn-list)

  (it "should fail to validate nil"
    (should-not (validator/validate :ssn nil {})))

  (it "should fail to validate empty string"
      (should-not (validator/validate :ssn "" {})))

  (it "should fail to validate SSN with century - / + and year between 2000-current_year"
      (let [fun (partial validator/validate :ssn)]
        (doseq [experiment ["020202-0202"
                            "020202+0202"
                            "020200+020J"]]
          (should-not (fun experiment {})))
        (doseq [experiment ["020202A0202"
                            "020202A0202"
                            "020200A020J"]]
          (should (fun experiment {}))))))

(describe "email validator"
  (tags :unit)

  (mapv (fn [email]
          (let [expected (get email/email-list email)
                pred     (if expected true? false?)
                actual   (validator/validate :email email {})]
            (it (str "should validate " email)
              (should (pred actual)))))
        (keys email/email-list)))

(describe "postal code validation"
  (tags :unit)

  (mapv (fn [postal-code]
          (let [expected (get postal-code/postal-code-list postal-code)
                pred     (if expected true? false?)
                actual   (validator/validate "postal-code" postal-code {:country-of-residence {:value "246"}})]
            (it (str "should validate " postal-code expected)
              (should (pred actual)))))
    (keys postal-code/postal-code-list)))

(describe "phone number validation"
  (tags :unit)

  (mapv (fn [number]
          (let [expected (get phone/phone-list number)
                pred     (if expected true? false?)
                actual   (validator/validate :phone number {})]
            (it (str "should validate " number)
              (should (pred actual)))))
        (keys phone/phone-list)))

(describe "birthdate validation"
  (tags :unit :birthdate-validation)
  (doall
    (for [[input expected] date/date-list]
      (it (str "should validate past-date " input " to " expected)
          (should= expected (validator/validate :past-date input {}))))))

(describe "main first name validation"
  (tags :unit)

  (doall
    (for [[first main expected] first-name/first-name-list]
      (it (str "should validate first-name " first " with main name " main " as " expected)
          (should= expected (validator/validate :main-first-name main {:first-name {:value first}}))))))
