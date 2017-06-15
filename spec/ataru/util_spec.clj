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
        '(:first-name :preferred-name :last-name :nationality :ssn :birth-date :gender :email :phone :address :postal-office :postal-code :home-town :language :047da62c-9afe-4e28-bfe8-5b50b21b4277 :c8558a1f-86e9-4d76-83eb-a0d7e1fd44b0 :b05a6057-2c65-40a8-9312-c837429f44bb)
        (->>
          (util/group-answers-by-wrapperelement (extract-wrapper-sections form) (util/answers-by-key (:answers answer)))
          first
          second
          (map keys)
          flatten))))

(def field-descriptor-id "64d4a625-370b-4814-ae4f-d5956e8881be")
(def field-descriptor {:id         field-descriptor-id
                       :label      {:fi "Pohjakoulutuksesi?" :sv ""}
                       :fieldType  "textField"
                       :fieldClass "formField"})

(describe "get-field-descriptor"
  (tags :get-fd)

  (it "gets field-descriptor from simple list"
    (should= field-descriptor (util/get-field-descriptor [field-descriptor] field-descriptor-id)))

  (it "gets field-descriptor from a child list"
    (should= field-descriptor (util/get-field-descriptor [{:id "invalid"
                                                           :children [field-descriptor]}]
                                                         field-descriptor-id))))
