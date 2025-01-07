(ns ataru.util-spec
  (:require [speclj.core :refer :all]
            [ataru.fixtures.answer :refer [answer]]
            [ataru.fixtures.person-info-form :refer [form]]
            [ataru.component-data.person-info-module :as person-module]
            [medley.core :refer [find-first]]
            [ataru.component-data.kk-application-payment-module :as payment-module :refer [kk-application-payment-wrapper-key asiakasnumero-migri-key kk-application-payment-choice-key]]
            [ataru.util :as util]))

(def form-with-visibility-conditions (assoc form :content [(person-module/person-info-module :onr-kk-application-payment)
                                                           (payment-module/kk-application-payment-module)]))

(def payment-wrapper (get-in form-with-visibility-conditions [:content 1]))
(def answer-hiding-wrapper {:nationality {:value [["246"]]}})
(def answer-showing-wrapper {:nationality {:value [["200"]["245"]]}})

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

(describe "is-field-hidden-by-section-visibility-conditions"
          (tags :unit :visibility)

          (it "field is shown by default"
              (should= nil (util/is-field-hidden-by-section-visibility-conditions
                               form-with-visibility-conditions
                               {}
                               payment-wrapper
                               false)))

          (it "field is hidden by section"
              (should= true (util/is-field-hidden-by-section-visibility-conditions
                              form-with-visibility-conditions
                              answer-hiding-wrapper
                              payment-wrapper
                              false)))

          (it "field is shown when answer does not meet condition"
              (should= nil (util/is-field-hidden-by-section-visibility-conditions
                              form-with-visibility-conditions
                              answer-showing-wrapper
                              payment-wrapper
                              false))))

(describe "find-wrapper-parent"
          (tags :unit :form)

          (it "returns nil for element with no parent"
              (let [flattened-form (util/flatten-form-fields (:content form-with-visibility-conditions))
                    wrapper-field (find-first #(= (:id %) kk-application-payment-wrapper-key) flattened-form)]
                (should= nil (util/find-wrapper-parent flattened-form wrapper-field))))

          (it "finds parent that is wrapper"
              (let [flattened-form (util/flatten-form-fields (:content form-with-visibility-conditions))
                    choice-field (find-first #(= (:id %) kk-application-payment-choice-key) flattened-form)]
                (should= kk-application-payment-wrapper-key
                         (:id (util/find-wrapper-parent flattened-form choice-field)))))

          (it "finds grandparent that is wrapper"
              (let [flattened-form (util/flatten-form-fields (:content form-with-visibility-conditions))
                    migri-field (find-first #(= (:id %) asiakasnumero-migri-key) flattened-form)]
                (should= kk-application-payment-wrapper-key
                         (:id (util/find-wrapper-parent flattened-form migri-field))))))

(describe "checking group answers"
          (tags :unit)
          (it "accept non-empty answer"
              (should (util/answered-in-group-idx {:value ["answer"]} 0)))
          (it "accept two dimensional non-empty answer"
              (should (util/answered-in-group-idx {:value [["answer"]]} 0)))
          (it "reject empty answer"
              (should-not (util/answered-in-group-idx {:value []} 0))
              (should-not (util/answered-in-group-idx {:value [[]]} 0)))
          (it "reject nil answer"
              (should-not (util/answered-in-group-idx {:value [nil]} 0))
              (should-not (util/answered-in-group-idx {:value [[nil]]} 0)))
          (it "reject empty string"
              (should-not (util/answered-in-group-idx {:value [""]} 0))
              (should-not (util/answered-in-group-idx {:value [[""]]} 0)))
          (it "reject non-array answer"
              (should-not (util/answered-in-group-idx {:value ""} 0)))
          (it "reject index-out-of-bounds answer"
              (should-not (util/answered-in-group-idx {:value [["answer"]]} 1))))

(def field-descriptor-id "64d4a625-370b-4814-ae4f-d5956e8881be")
(def field-descriptor {:id         field-descriptor-id
                       :label      {:fi "Pohjakoulutuksesi?" :sv ""}
                       :fieldType  "textField"
                       :fieldClass "formField"})
