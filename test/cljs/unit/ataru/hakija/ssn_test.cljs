(ns ataru.hakija.ssn-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [ataru.hakija.ssn :as ssn]))

(def valid-ssn "241000A757Y")
(def valid-new-ssn "241001C757Y")
(def invalid-new-ssn "241001G757Y")
(def invalid-ssn "roskaa")

(deftest parse-birth-date-from-ssn
  (testing "returns birth date when given valid ssn"
    (let [demo?      false
          birth-date (ssn/parse-birth-date-from-ssn demo? valid-ssn)]
      (is (= "24.10.2000" birth-date))))

  (testing "returns birth date when given valid ssn with new century character"
    (let [demo?      false
          birth-date (ssn/parse-birth-date-from-ssn demo? valid-new-ssn)]
      (is (= "24.10.2001" birth-date))))

  (testing "throws exception when give invalid ssn"
    (let [demo? false]
      (is (thrown? :default (ssn/parse-birth-date-from-ssn demo? invalid-ssn)))))

  (testing "throws exception when give strange new ssn"
    (let [demo? false]
      (is (thrown? :default (ssn/parse-birth-date-from-ssn demo? invalid-new-ssn)))))

  (testing "returns birth date when given valid ssn and demo is enabled"
    (let [demo?      true
          birth-date (ssn/parse-birth-date-from-ssn demo? valid-ssn)]
      (is (= "24.10.2000" birth-date))))

  (testing "returns empty string when given invalid ssn and demo is enabled"
    (let [demo?      true
          birth-date (ssn/parse-birth-date-from-ssn demo? invalid-ssn)]
      (is (= "" birth-date)))))

(deftest parse-gender-from-ssn
  (testing "returns gender when given valid ssn"
    (let [demo?      false
          gender (ssn/parse-gender-from-ssn demo? valid-ssn)]
      (is (= "1" gender))))

  (testing "throws exception when give invalid ssn"
    (let [demo? false]
      (is (thrown? :default (ssn/parse-gender-from-ssn demo? invalid-ssn)))))

  (testing "returns gender when given valid ssn and demo is enabled"
    (let [demo?      true
          gender (ssn/parse-gender-from-ssn demo? valid-ssn)]
      (is (= "1" gender))))

  (testing "returns empty string when given invalid ssn and demo is enabled"
    (let [demo?      true
          gender (ssn/parse-gender-from-ssn demo? invalid-ssn)]
      (is (= "" gender)))))
