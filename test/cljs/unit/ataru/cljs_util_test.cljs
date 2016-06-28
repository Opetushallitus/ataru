(ns ataru.cljs-util-test
  (:require [ataru.cljs-util :as util]
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
