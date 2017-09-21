(ns ataru.hakija.application-validators-test
  (:require-macros [cljs.core.async.macros :as asyncm])
  (:require [ataru.fixtures.email :as email]
            [ataru.fixtures.date :as date]
            [ataru.fixtures.phone :as phone]
            [ataru.fixtures.postal-code :as postal-code]
            [ataru.fixtures.ssn :as ssn]
            [ataru.fixtures.first-name :as first-name]
            [ataru.hakija.application-validators :as validator]
            [cljs.core.async :as async]
            [cljs.test :refer-macros [deftest is testing async]]))

(deftest ssn-validation
  (async done
         (asyncm/go
           (doseq [ssn ssn/ssn-list]
             (doseq [century-char ["A"]]
               (let [ssn (str (:start ssn) century-char (:end ssn))]
                 (is (first (async/<! (validator/validate "ssn" ssn {} nil)))
                     (str "SSN " ssn " is not valid")))))

           (is (not (first (async/<! (validator/validate "ssn" nil {} nil)))))
           (is (not (first (async/<! (validator/validate "ssn" "" {} nil)))))
           (done))))

(deftest email-validation
  (async done
         (asyncm/go
           (doseq [email (keys email/email-list)]
             (let [expected (get email/email-list email)
                   pred     (if expected true? false?)
                   actual   (first (async/<! (validator/validate "email" email {} nil)))
                   message  (if expected "valid" "invalid")]
               (is (pred actual)
                   (str "email " email " was not " message))))
           (done))))

(deftest postal-code-validation
  (async done
         (asyncm/go
           (doseq [postal-code (keys postal-code/postal-code-list)]
             (let [expected (get postal-code/postal-code-list postal-code)
                   pred     (if expected true? false?)
                   actual   (first (async/<! (validator/validate "postal-code"
                                                                 postal-code
                                                                 {:country-of-residence {:value "246"}}
                                                                 nil)))
                   message  (if expected "valid" "invalid")]
               (is (pred actual)
                   (str "postal code " postal-code " was not " message))))
           (done))))

(deftest phone-number-validation
  (async done
         (asyncm/go
           (doseq [number (keys phone/phone-list)]
             (let [expected (get phone/phone-list number)
                   pred     (if expected true? false?)
                   actual   (first (async/<! (validator/validate "phone" number {} nil)))
                   message  (if expected "valid" "invalid")]
               (is (pred actual)
                   (str "phone number " number " was not " message))))
           (done))))

(deftest date-validation
  (async done
         (asyncm/go
           (doseq [[input expected] date/date-list]
             (is (= expected (first (async/<! (validator/validate :past-date
                                                                  input
                                                                  {}
                                                                  nil))))))
           (done))))

(deftest main-first-name-validation
  (async done
         (asyncm/go
           (doseq [[first-name main-name expected] first-name/first-name-list]
             (is (= expected (first (async/<! (validator/validate :main-first-name
                                                                  main-name
                                                                  {:first-name {:value first-name}}
                                                                  nil))))))
           (done))))
