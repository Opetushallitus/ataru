(ns ataru.cljs-util-test
  (:require [ataru.cljs-util :as util]
            [cljs.test :refer-macros [deftest are]]))

(deftest stringifies-cljs-objects
  (are [expected data] (= expected (util/cljs->str data))
    "[1]" [1]))
