(ns ataru.collections-test
  (:require [ataru.collections :as c])
  (:require-macros [cljs.test :refer [deftest are is]]))

(deftest returns-true-when-a-before-b
  (are [coll]
    (true? (c/before? :a :b coll))
    [:a :b :c :d]
    [:a :c :b :d]
    [:a :c :d :b]
    [:c :a :b :d]
    [:c :a :d :b]
    [:c :d :a :b]))

(deftest returns-false-when-a-not-before-b
  (are [coll]
    (false? (c/before? :a :b coll))
    [:b :a :c :d]
    [:b :c :a :d]
    [:b :c :d :a]
    [:c :b :a :d]
    [:c :b :d :a]
    [:c :d :b :a]))

(deftest returns-false-when-a-or-b-nil
  (are [a b]
    (false? (c/before? a b [:a :b :c :d]))
    [:a nil]
    [nil :a]))

(deftest returns-false-when-a-and-b-nil
  (is (false? (c/before? nil nil [:a :b :c :d]))))

(deftest returns-false-when-a-equals-b
  (is (false? (c/before? :a :a [:a :b :c :d]))))
