(ns ataru.virkailija.component-data.value-transformers-spec
  (:require [speclj.core :refer :all]
            [ataru.virkailija.component-data.value-transformers :as t]))

(describe "transforming date of birth into dd.mm.yyyy format"
  (tags :unit)
  (it "should transform date of birth into dd.mm.yyyy format"
    (doall
      (for [[expected dob] [["01.10.2010" "1.10.2010"]
                            ["10.01.2010" "10.1.2010"]
                            ["01.01.2010" "1.1.2010"]
                            ["10.10.2010" "10.10.2010"]]]
        (should= (t/birth-date dob) expected)))))
