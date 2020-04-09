(ns ataru.string-normalizer-test
  (:require [ataru.string-normalizer :as normalizer]
            [ataru.fixtures.string-normalizer-test-fixtures :as fixtures]
            [goog.string :as gstring])
  (:require-macros [cljs.test :refer [deftest is]]))

(deftest normalizes-string
  (doseq [{expected :expected
           input    :input} fixtures/string-normalizer-fixtures]
    (let [actual (normalizer/normalize-string input)]
      (is (= actual expected)
          (gstring/format "Did not normalize string \"%s\" properly, was: \"%s\", should have been: \"%s\"" input actual expected)))))

