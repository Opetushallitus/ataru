(ns lomake-editori.test-utils
  (:require [speclj.core :refer :all]))

(defn should-have-header
  [header expected-val resp]
  (let [headers (:headers resp)]
    (should-not-be-nil headers)
    (should-contain header headers)
    (should= expected-val (get headers header))))
