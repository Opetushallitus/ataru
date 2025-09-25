(ns ataru.cljs-util-test
  (:require [ataru.cljs-util :as util]
            [ataru.util :as u]
            [cljs.test :refer-macros [deftest are is]]))

(deftest stringifies-cljs-objects
  (are [expected data] (= expected (util/cljs->str data))
    "[1]" [1]
    "[1 :children 0]" [1 :children 0]))

(deftest parses-str-to-cljs
  (are [expected str] (= expected (util/str->cljs str))
    [1] "[1]"
    [1 :children 0] "[1 :children 0]"))

(deftest generates-rfc-4122-version-4-uuids
  (let [uuid (util/new-uuid)]
    (is (re-matches #"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$" uuid))))

(deftest keeps-non-empty-event-changes
  (let [changes {:1 {:old "" :new nil}
                 :2 {:old ["1"] :new []}
                 :3 {:old nil :new nil}
                 :4 {:old nil :new []}
                 :5 {:old [] :new "1"}
                 :6 {:old 1 :new nil}
                 :7 {:old [nil "1"] :new []}}
        non-empty-changes (u/keep-non-empty-changes changes)]
    (is (= non-empty-changes
           (select-keys changes [:2 :5 :6 :7])))))