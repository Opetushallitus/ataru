(ns ataru.cljs-util-test
  (:require [ataru.cljs-util :as util]
            [cljs.test :refer-macros [deftest are]]))

(deftest stringifies-cljs-objects
  (are [expected data] (= expected (util/cljs->str data))
    "[1]" [1]
    "[1 :children 0]" [1 :children 0]))

(deftest parses-str-to-cljs
  (are [expected str] (= expected (util/str->cljs str))
    [1] "[1]"
    [1 :children 0] "[1 :children 0]"))
