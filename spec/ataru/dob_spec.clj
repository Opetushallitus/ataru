(ns ataru.dob-spec
  (:require [ataru.dob :as dob]
            [speclj.core :refer :all]))

(describe "ataru.dob/dob?"
  (tags :unit)

  (it "should identify date correctly"
    (doall
      (for [[dob expected] [["1.1.2010" true]
                            ["01.01.2010" true]
                            ["1.01.2010" true]
                            ["01.1.2010" true]
                            ["1.13.2010" false]
                            ["31.1.2010" true]
                            ["29.10.2010" true]
                            ["40.10.2010" false]
                            ["41.1.1984" false]
                            [nil false]]]
        (should= expected (dob/dob? dob))))))
