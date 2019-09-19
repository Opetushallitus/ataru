(ns ataru.util.random-spec
  (:require [speclj.core :refer [describe tags it should= should-not=]]
            [ataru.util.random :as random]))
(describe "util.random"
  (tags :unit :random)
  (it "should create random url parts"
      (should=
        46
        (count (random/url-part 34)))
      (should-not=
        (random/url-part 34)
        (random/url-part 34))))
