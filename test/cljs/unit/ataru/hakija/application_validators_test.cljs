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
            [cljs.test :refer-macros [deftest is async]]
            [ataru.feature-config]))

(defn- has-never-applied [_ _] (asyncm/go false))

(deftest required-validation
  (async done
         (asyncm/go
           (doseq [input ["" " " [] [""] ["" ""]]]
             (is (false? (first (async/<! (validator/validate {:has-applied    has-never-applied
                                                               :validator      "required"
                                                               :value          input}))))))
           (doseq [input ["f" ["f"] ["" "f"]]]
             (is (true? (first (async/<! (validator/validate {:has-applied    has-never-applied
                                                              :validator      "required"
                                                              :value          input}))))))
           (done))))

(deftest ssn-validation
  (async done
         (asyncm/go
           (with-redefs
             [ataru.feature-config/feature-enabled? (fn [feature] (when (= feature :disallow-temporary-ssn) false))]
             (doseq [ssn ssn/ssn-list]
               (doseq [century-char ["A" "B" "C" "D" "E" "F" "U" "V" "W" "X" "Y" "-"]]
                 (let [ssn (str (:start ssn) century-char (:end ssn))]
                   (is (first (async/<! (validator/validate {:has-applied has-never-applied :validator "ssn" :value ssn})))
                       (str "SSN " ssn " is not valid")))))

             (is (not (first (async/<! (validator/validate {:has-applied has-never-applied :validator "ssn"})))))
             (is (not (first (async/<! (validator/validate {:has-applied has-never-applied :validator "ssn"})))))
             (is (not (first (async/<! (validator/validate {:has-applied      (fn [_ _] (asyncm/go true))
                                                            :validator        "ssn"
                                                            :value            "020202A0202"
                                                            :field-descriptor
                                                            {:params {:can-submit-multiple-applications false
                                                                      :haku-oid "dummy-haku-oid"}}})))))

             (is (first (async/<! (validator/validate {:has-applied      has-never-applied
                                                       :validator        "ssn"
                                                       :value            "010170-960F"
                                                       :field-descriptor
                                                       {:params {:can-submit-multiple-applications false
                                                                 :haku-oid "dummy-haku-oid"}}})))))
           (done))))

(deftest ssn-validation-temporary-ssn-does-not-work-in-production
  (async done
    (asyncm/go
      (with-redefs
        [ataru.feature-config/feature-enabled? (fn [feature] (when (= feature :disallow-temporary-ssn) true))]

        (is (not (first (async/<! (validator/validate {:has-applied      has-never-applied
                                                       :validator        "ssn"
                                                       :value            "010170-960F"
                                                       :field-descriptor
                                                       {:params {:can-submit-multiple-applications false
                                                                 :haku-oid "dummy-haku-oid"}}}))))))

      (done))))
(deftest email-validation
  (async done
         (asyncm/go
           (doseq [email (keys email/email-list)]
             (let [expected (get email/email-list email)
                   pred     (if expected true? false?)
                   actual   (first (async/<! (validator/validate {:has-applied has-never-applied :validator "email" :value email
                                                                  :answers-by-key {:email {:value email :verify email}}
                                                                  :field-descriptor {:id :email}})))
                   message  (if expected "valid" "invalid")]
               (is (pred actual)
                   (str "email " email " was not " message))))
           (is (not (first (async/<! (validator/validate {:has-applied      (fn [_ _] (asyncm/go true))
                                                          :validator        "email"
                                                          :value            "test@example.com"
                                                          :answers-by-key {:email {:value "test@example.com" :verify "test@example.com"}}
                                                          :field-descriptor {:id :email
                                                                             :params {:can-submit-multiple-applications false
                                                                                      :haku-oid                         "dummy-haku-oid"}}})))))
           (done))))

(deftest email-optional-validation
  (async done
    (asyncm/go
      (is (true? (first (async/<! (validator/validate {:has-applied       has-never-applied
                                                       :validator         "email-optional"
                                                       :value             nil
                                                       :answers-by-key    {:email {:value nil :verify nil}
                                                                           :ssn {:value "081195-433H"}}
                                                       :field-descriptor  {:id :email}})))))
      (is (false? (first (async/<! (validator/validate {:has-applied       has-never-applied
                                                       :validator         "email-optional"
                                                       :value             nil
                                                       :answers-by-key    {:email {:value nil :verify nil}
                                                                           :ssn {:value nil}}
                                                       :field-descriptor  {:id :email}})))))
      (is (false? (first (async/<! (validator/validate {:has-applied       has-never-applied
                                                        :validator         "email-optional"
                                                        :value             "aaaa"
                                                        :answers-by-key    {:email {:value "aaaa" :verify "bbbb"}
                                                                            :ssn {:value nil}}
                                                        :field-descriptor  {:id :email}})))))
      (is (false? (first (async/<! (validator/validate {:has-applied       has-never-applied
                                                       :validator         "email-optional"
                                                       :value             "aaa@"
                                                       :answers-by-key    {:email {:value "aaa@" :verify "aaa@"}
                                                                           :ssn {:value nil}}
                                                       :field-descriptor  {:id :email}})))))
      (is (true? (first (async/<! (validator/validate {:has-applied       has-never-applied
                                                        :validator         "email-optional"
                                                        :value             "aaa@bbb.ccc"
                                                        :answers-by-key    {:email {:value "aaa@bbb.ccc" :verify "aaa@bbb.ccc"}
                                                                            :ssn {:value nil}}
                                                        :field-descriptor  {:id :email}})))))
      (done))))

(deftest email-simple-validation
  (async done
    (asyncm/go
      (is (true? (first (async/<! (validator/validate {:has-applied      has-never-applied
                                                       :validator        "email-simple"
                                                       :value            nil
                                                       :answers-by-key   {:email-simple {:value nil}}
                                                       :field-descriptor {:id :email-simple}})))))
      (is (true? (first (async/<! (validator/validate {:has-applied      has-never-applied
                                                       :validator        "email-simple"
                                                       :value            ""
                                                       :answers-by-key   {:email-simple {:value ""}}
                                                       :field-descriptor {:id :email-simple}})))))
      (is (true? (first (async/<! (validator/validate {:has-applied      has-never-applied
                                                       :validator        "email-simple"
                                                       :value            "test@example.com"
                                                       :answers-by-key   {:email-simple {:value "test@example.com"}}
                                                       :field-descriptor {:id :email-simple}})))))
      (is (false? (first (async/<! (validator/validate {:has-applied      has-never-applied
                                                        :validator        "email-simple"
                                                        :value            "not-valid"
                                                        :answers-by-key   {:email-simple {:value "not-valid"}}
                                                        :field-descriptor {:id :email-simple}}))))))
    (done)))

(deftest postal-code-validation
  (async done
         (asyncm/go
           (doseq [postal-code (keys postal-code/postal-code-list)]
             (let [expected (get postal-code/postal-code-list postal-code)
                   pred     (if expected true? false?)
                   actual   (first (async/<! (validator/validate {:has-applied    has-never-applied
                                                                  :validator      "postal-code"
                                                                  :value          postal-code
                                                                  :answers-by-key {:country-of-residence {:value "246"}}})))
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
                   actual   (first (async/<! (validator/validate {:has-applied has-never-applied
                                                                  :validator   "phone"
                                                                  :value       number})))
                   message  (if expected "valid" "invalid")]
               (is (pred actual)
                   (str "phone number " number " was not " message))))
           (done))))

(deftest date-validation
  (async done
         (asyncm/go
           (doseq [[input expected] date/date-list]
             (is (= expected (first (async/<! (validator/validate {:has-applied has-never-applied
                                                                   :validator   "past-date"
                                                                   :value       input}))))))
           (done))))

(deftest main-first-name-validation
  (async done
         (asyncm/go
           (doseq [[first-name main-name expected] first-name/first-name-list]
             (is (= expected (first (async/<! (validator/validate {:has-applied    has-never-applied
                                                                   :validator      "main-first-name"
                                                                   :value          main-name
                                                                   :answers-by-key {:first-name {:value first-name}}}))))))
           (done))))
