(ns ataru.util-spec
  (:require [speclj.core :refer :all]
            [ataru.fixtures.answer :refer [answer]]
            [ataru.fixtures.form :refer [form]]
            [ataru.util :as util]))

(defn extract-wrapper-sections [form]
  (map #(select-keys % [:id :label :children])
    (filter #(= (:fieldClass %) "wrapperElement") (:content form))))

(describe "grouping answers"
  (it "puts the answer in a group or else it gets the hose again"
      (util/group-answers-by-wrapperelement (extract-wrapper-sections form) answer)
  ))

(first (:content form))
