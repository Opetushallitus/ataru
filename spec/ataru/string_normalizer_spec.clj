(ns ataru.string-normalizer-spec
  (:require [ataru.string-normalizer :as normalizer]
            [ataru.fixtures.string-normalizer-test-fixtures :as fixtures]
            [speclj.core :refer :all]))

(defn- ->test [{expected :expected
                input    :input}]
  (it (format "formats string \"%s\" to \"%s\"" input expected)
    (should= expected
             (normalizer/normalize-string input))))

(describe "ataru.string-normalizer/normalize-string"
  (tags :unit)
  (map ->test fixtures/string-normalizer-fixtures))
