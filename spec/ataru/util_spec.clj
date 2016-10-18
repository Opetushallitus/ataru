(ns ataru.util-spec
  (:require [speclj.core :refer :all]
            [ataru.fixtures.answer :refer [answer]]
            [ataru.fixtures.person-info-form :refer [form]]
            [ataru.util :as util]))

(defn extract-wrapper-sections [form]
  (map #(select-keys % [:id :label :children])
    (filter #(= (:fieldClass %) "wrapperElement") (:content form))))

(describe "grouping answers"
  (tags :unit)
  (it "puts the answer in a group or else it gets the hose again"
      (should=
        '(:first-name :preferred-name :last-name :nationality :ssn :birth-date :gender :email :phone :address :postal-office :postal-code :home-town :language :047da62c-9afe-4e28-bfe8-5b50b21b4277 :c8558a1f-86e9-4d76-83eb-a0d7e1fd44b0)
        (->>
          (util/group-answers-by-wrapperelement (extract-wrapper-sections form) (util/answers-by-key (:answers answer)))
          first
          second
          (map keys)
          flatten))))
