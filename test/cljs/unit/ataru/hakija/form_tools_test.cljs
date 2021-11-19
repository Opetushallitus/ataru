(ns ataru.hakija.form-tools-test
  (:require [cljs.test :refer-macros [deftest is]]
            [ataru.hakija.form-tools :refer [get-field-from-content
                                             get-field-from-flat-form-content
                                             update-field-in-db]]))

(def db
  {:form {
          :content [
                    {:id "onr-2nd" :children [
                                     {:id "email" :value "ruhtinas@nukettaja.com"}
                                     {:id "is-finnish-ssn" :value "true"}]}]}
  :flat-form-content [
                      {:id "email" :value "ruhtinas@nukettaja.com"}
                      {:id "is-finnish-ssn" :value "true"}
                      {:id "onr-2nd"}]})

(deftest fetches-field-from-flat-form-content
  (let [field (get-field-from-flat-form-content db "email")
        field2 (get-field-from-flat-form-content db "is-finnish-ssn")]
    (is (= "email" (:id field)))
    (is (= "ruhtinas@nukettaja.com" (:value field)))
    (is (= "is-finnish-ssn" (:id field2)))
    (is (= "true" (:value field2)))))

(deftest fetches-field-from-content
  (let [field (get-field-from-content db "email")
        field2 (get-field-from-content db "is-finnish-ssn")
        field3 (get-field-from-content db "onr-2nd")]
    (is (= "email" (:id field)))
    (is (= "ruhtinas@nukettaja.com" (:value field)))
    (is (= "is-finnish-ssn" (:id field2)))
    (is (= "true" (:value field2)))
    (is (= "onr-2nd" (:id field3)))
    (is (= 2 (count (:children field3))))))

(deftest updates-field-in-db
  (let [updated-db (update-field-in-db db {:id "email" :value "tiivi@taavi.fi"})]
    (is (= updated-db
           {:form {
                   :content [
                             {:id "onr-2nd" :children [
                                                       {:id "email" :value "tiivi@taavi.fi"}
                                                       {:id "is-finnish-ssn" :value "true"}]}]}
            :flat-form-content [
                                {:id "email" :value "tiivi@taavi.fi"}
                                {:id "is-finnish-ssn" :value "true"}
                                {:id "onr-2nd"}]}))))

