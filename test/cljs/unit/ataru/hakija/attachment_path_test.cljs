(ns ataru.hakija.attachment-path-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [ataru.hakija.attachment-path :as attachment-path]))

(defn- db-with-values [values]
  {:application {:answers {:liite {:values values}}}})

(deftest finds-attachment-outside-question-group
  (let [db (db-with-values [{:upload-id "a"} {:upload-id "b"}])]
    (is (= [:application :answers :liite :values 1]
           (attachment-path/path-by-upload-id db :liite "b")))
    (is (nil? (attachment-path/path-by-upload-id db :liite "c")))))

(deftest finds-attachment-in-whichever-question-group-row-holds-it
  (testing "a row removed before the upload's row shifts it, and the lookup follows"
    (let [db (db-with-values [[{:upload-id "a"}] nil [{:value "x"} {:upload-id "b"}]])]
      (is (= [:application :answers :liite :values 2 1]
             (attachment-path/path-by-upload-id db :liite "b")))
      (is (= [:application :answers :liite :values 0 0]
             (attachment-path/path-by-upload-id db :liite "a")))))
  (testing "the row is gone altogether"
    (is (nil? (attachment-path/path-by-upload-id (db-with-values [[{:upload-id "a"}]]) :liite "b")))))

(deftest missing-answer-has-no-path
  (is (nil? (attachment-path/path-by-upload-id (db-with-values nil) :liite "a")))
  (is (nil? (attachment-path/path-by-upload-id {} :liite "a"))))

(deftest nil-upload-id-matches-no-attachment
  (is (nil? (attachment-path/path-by-upload-id (db-with-values [{:value "valmis"} {:upload-id "a"}]) :liite nil)))
  (is (nil? (attachment-path/path-by-upload-id (db-with-values [[{:value "valmis"}]]) :liite nil))))

(deftest question-group-idx-of-path
  (is (= 2 (attachment-path/question-group-idx-of-path [:application :answers :liite :values 2 1])))
  (is (nil? (attachment-path/question-group-idx-of-path [:application :answers :liite :values 1]))))
