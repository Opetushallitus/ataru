(ns ataru.hakija.application-validators-spec
  (:require [ataru.fixtures.email :as email]
            [ataru.fixtures.phone :as phone]
            [ataru.fixtures.date :as date]
            [ataru.fixtures.postal-code :as postal-code]
            [ataru.fixtures.ssn :as ssn]
            [ataru.fixtures.first-name :as first-name]
            [ataru.fixtures.hakukohde :as hakukohde]
            [ataru.fixtures.numeric-input :refer [numbers integers value-between]]
            [ataru.hakija.application-validators :as validator]
            [speclj.core :refer :all]
            [clojure.core.async :as async]))

(defn- validate!
  ([validator value answers-by-key field-descriptor]
   (validate! (fn [haku-oid identifier] (async/go false))
              validator
              value
              answers-by-key
              field-descriptor))
  ([has-applied validator value answers-by-key field-descriptor]
   (first (async/<!! (validator/validate {:has-applied      has-applied
                                          :validator        validator
                                          :value            value
                                          :answers-by-key   (assoc answers-by-key :email {:value value :verify value})
                                          :field-descriptor (assoc field-descriptor :id :email)})))))

(describe "required validator"
  (tags :unit :validator)

  (it "should not allow nil"
    (should-not (validate! :required nil {} nil)))

  (it "should not allow empty string"
    (should-not (validate! :required "" {} nil)))

  (it "should not allow string with only whitespace"
    (should-not (validate! :required " " {} nil)))

  (it "should allow string with at least one character"
    (should (validate! :required "a" {} nil))))

(describe "ssn validator"
  (tags :unit :validator)

  (map (fn [ssn]
         (map (fn [century-char]
                (let [ssn (str (:start ssn) century-char (:end ssn))]
                  (it (str "should validate " ssn)
                    (should (validate! :ssn ssn {} nil)))))
              ["A" "B" "C" "D" "E" "F" "U" "V" "W" "X" "Y" "-"]))
       ssn/ssn-list)

  (it "should fail to validate nil"
    (should-not (validate! :ssn nil {} nil)))

  (it "should fail to validate empty string"
      (should-not (validate! :ssn "" {} nil)))

  (it "should fail to validate SSN with this century and year in the future"
      (let [fun (partial validate! :ssn)]
        (doseq [experiment ["020266B0202"
                            "020277C202"
                            "020288D020J"]]
          (should-not (fun experiment {} nil)))
        (doseq [experiment ["020202A0202"
                            "020202A0202"
                            "020200A020J"
                            "020200F020J"]]
          (should (fun experiment {} nil)))))

  (it "should fail to validate SSN if cannot submit multiple applications and has applied"
      (should-not (validate! (fn [_ _] (async/go true))
                             :ssn
                             "020202A0202"
                             {}
                             {:params {:can-submit-multiple-applications false
                                       :haku-oid "dummy-haku-oid"}}))))

(describe "email validator"
  (tags :unit :validator)

  (mapv (fn [email]
          (let [expected (get email/email-list email)
                pred     (if expected true? false?)
                actual   (validate! :email email {} nil)]
            (it (str "should validate " email)
              (should (pred actual)))))
        (keys email/email-list))

  (it "should fail to validate email if cannot submit multiple applications and has applied"
      (should-not (validate! (fn [_ _] (async/go true))
                             :email
                             "test@example.com"
                             {}
                             {:params {:can-submit-multiple-applications false
                                       :haku-oid "dummy-haku-oid"}}))))

(describe "postal code validation"
  (tags :unit :validator)

  (mapv (fn [postal-code]
          (let [expected (get postal-code/postal-code-list postal-code)
                pred     (if expected true? false?)
                actual   (validate! "postal-code" postal-code {:country-of-residence {:value "246"}} nil)]
            (it (str "should validate " postal-code expected)
              (should (pred actual)))))
    (keys postal-code/postal-code-list)))

(describe "phone number validation"
  (tags :unit :validator)

  (mapv (fn [number]
          (let [expected (get phone/phone-list number)
                pred     (if expected true? false?)
                actual   (validate! :phone number {} nil)]
            (it (str "should validate " number)
              (should (pred actual)))))
        (keys phone/phone-list)))

(describe "birthdate validation"
  (tags :unit :validator :birthdate-validation)
  (doall
    (for [[input expected] date/date-list]
      (it (str "should validate past-date " input " to " expected)
          (should= expected (validate! :past-date input {} nil))))))

(describe "main first name validation"
  (tags :unit :validator)

  (doall
    (for [[first main expected] first-name/first-name-list]
      (it (str "should validate first-name " first " with main name " main " as " expected)
          (should= expected (validate! :main-first-name main {:first-name {:value first}} nil))))))

(describe "hakukohde validation"
          (tags :unit :validator)
          (doall
            (for [[answer field expected] hakukohde/hakukohteet]
              (it (str "should validate hakukohteet " answer " with field " field " as " expected)
                  (should= expected (validate! :hakukohteet answer nil field))))))

(describe "numeric validator"
  (tags :unit :validator :numeric)
  (describe "integers and floats"
    (doall
      (for [number (keys numbers)
            :let [expected (get numbers number)]]
        (it (str "should " (when-not expected "not ") "validate " number)
          (should= expected (validate! :numeric number nil {:params {:decimals 8
                                                                     :numeric true}}))))))

  (describe "only integers"
    (doall
      (for [number (keys integers)
            :let [expected (get integers number)]]
        (it (str "should " (when-not expected "not ") "validate " number)
          (should= expected (validate! :numeric number nil {:params {:decimals nil
                                                                     :numeric true}}))))))

  (describe "number between range"
            (doall
              (for [[number inputs] value-between]
                (doall
                  (for [[valid? ranges] inputs]
                    (doall
                      (for [[min-value max-value] ranges]
                        (it (str "should " (when-not valid? "not ") "validate " number " between " (or min-value "_") " - " (or max-value "_"))
                            (should= valid? (validate! :numeric number nil {:params {:decimals  8
                                                                                     :numeric   true
                                                                                     :min-value min-value
                                                                                     :max-value max-value
                                                                                     }})))))))))))

