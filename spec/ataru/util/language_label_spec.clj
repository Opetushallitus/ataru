(ns ataru.util-spec
  (:require [speclj.core :refer [describe tags it should=]]
            [ataru.util.language-label :as label]))

(describe "languagel label"
  (tags :unit :langlabel)
  (it "finnish should be default"
      (should=
       "kengän numero"
       (label/get-language-label-in-preferred-order {:fi "kengän numero" :sv "skostorlek" :en "shoe size"})))
  (it "should use swedish if finnish doesn't exist"
      (should=
       "skostorlek"
       (label/get-language-label-in-preferred-order {:sv "skostorlek" :en "shoe size"})))
  (it "works with null and empty strings"
      (should=
       "shoe size"
       (label/get-language-label-in-preferred-order {:fi nil :sv "   " :en "shoe size"}))))
