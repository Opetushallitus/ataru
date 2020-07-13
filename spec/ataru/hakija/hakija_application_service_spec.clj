(ns ataru.hakija.hakija-application-service-spec
  (:require [ataru.hakija.hakija-application-service :as hakija-application-service]
            [speclj.core :refer [describe it should-be should-not-be]]))

(def edited-cannot-edit-questions #'hakija-application-service/edited-cannot-edit-questions)

(describe "edited-cannot-edit-questions"
  (describe "old application does not have an answer"
    (describe "text field"
      (it "should allow empty text in new answer"
        (let [new-answer      ""
              new-application {:answers [{:key   "id"
                                          :value new-answer}]}
              old-application {:answers []}
              form            {:content [{:id          "id"
                                          :cannot-edit true}]}]
          (should-be empty? (edited-cannot-edit-questions new-application old-application form))))
      (it "should not allow any text in new answer"
        (let [new-answer      "Vastaus"
              new-application {:answers [{:key   "id"
                                          :value new-answer}]}
              old-application {:answers []}
              form            {:content [{:id          "id"
                                          :cannot-edit true}]}]
          (should-not-be empty? (edited-cannot-edit-questions new-application old-application form)))))
    (describe "repeatable text field"
      (it "should allow empty text in new answer"
        (let [new-answer      ["" ""]
              new-application {:answers [{:key   "id"
                                          :value new-answer}]}
              old-application {:answers []}
              form            {:content [{:id          "id"
                                          :cannot-edit true}]}]
          (should-be empty? (edited-cannot-edit-questions new-application old-application form))))
      (it "should not allow any text in new answer"
        (let [new-answer      ["Vastaus" ""]
              new-application {:answers [{:key   "id"
                                          :value new-answer}]}
              old-application {:answers []}
              form            {:content [{:id          "id"
                                          :cannot-edit true}]}]
          (should-not-be empty? (edited-cannot-edit-questions new-application old-application form)))))
    (describe "(repeatable) text field in question group"
      (it "should allow empty text in new answer"
        (let [new-answer      [["" ""] [""]]
              new-application {:answers [{:key   "id"
                                          :value new-answer}]}
              old-application {:answers []}
              form            {:content [{:id          "id"
                                          :cannot-edit true}]}]
          (should-be empty? (edited-cannot-edit-questions new-application old-application form))))
      (it "should not allow any text in new answer"
        (let [new-answer      [["" "Vastaus"] [""]]
              new-application {:answers [{:key   "id"
                                          :value new-answer}]}
              old-application {:answers []}
              form            {:content [{:id          "id"
                                          :cannot-edit true}]}]
          (should-not-be empty? (edited-cannot-edit-questions new-application old-application form)))))))
