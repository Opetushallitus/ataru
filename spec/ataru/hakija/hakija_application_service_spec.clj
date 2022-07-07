(ns ataru.hakija.hakija-application-service-spec
  (:require [ataru.hakija.hakija-application-service :as hakija-application-service]
            [speclj.core :refer [describe it should-be should-not-be should-contain should=]]))

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

(describe "merge-unviewable-answers-from-previous"
  (describe "question is cannot-view"
    (it "copies answer value from old application when new application doesn't have answer"
      (let [form            {:content [{:id          "id"
                                        :cannot-view true}]}
            old-application {:answers [{:key   "id"
                                        :value "old-value"}]}
            new-application {:answers [{:key   "id"
                                        :value nil}]}
            answers         (:answers (hakija-application-service/merge-unviewable-answers-from-previous new-application old-application form))]
        (should= 1 (count answers))
        (should-contain {:key "id" :value "old-value"} answers)))

    (describe "per-hakukohde followup"
      (it "removes answer when hakukohde is removed in new application"
        (let [form            {:content [{:id      "id"
                                          :options [{:followups [{:id          "followup-id"
                                                                  :cannot-view true}]}]}]}
              old-application {:answers   [{:key                               "followup-id_1"
                                            :value                             "old-value"
                                            :original-followup                 "followup-id"
                                            :duplikoitu-followup-hakukohde-oid "1"}]
                               :hakukohde ["1"]}
              new-application {:answers   []
                               :hakukohde []}
              answers         (:answers (hakija-application-service/merge-unviewable-answers-from-previous new-application old-application form))]
          (should= 0 (count answers))))

      (it "copies answer value from old application when new application doesn't have answer"
        (let [form            {:content [{:id      "id"
                                          :options [{:followups [{:id          "followup-id"
                                                                  :cannot-view true}]}]}]}
              old-application {:answers   [{:key                               "followup-id_1"
                                            :value                             "old-value"
                                            :original-followup                 "followup-id"
                                            :duplikoitu-followup-hakukohde-oid "1"}]
                               :hakukohde ["1"]}
              new-application {:answers   [{:key                               "followup-id_1"
                                            :value                             nil
                                            :original-followup                 "followup-id"
                                            :duplikoitu-followup-hakukohde-oid "1"}]
                               :hakukohde ["1"]}
              answers         (:answers (hakija-application-service/merge-unviewable-answers-from-previous new-application old-application form))]
          (should= 1 (count answers))
          (should-contain {:key                               "followup-id_1"
                           :value                             "old-value"
                           :original-followup                 "followup-id"
                           :duplikoitu-followup-hakukohde-oid "1"} answers)))

      (it "replaces old answer value with value from new application"
        (let [form            {:content [{:id      "id"
                                          :options [{:followups [{:id          "followup-id"
                                                                  :cannot-view true}]}]}]}
              old-application {:answers   [{:key                               "followup-id_1"
                                            :value                             "old-value"
                                            :original-followup                 "followup-id"
                                            :duplikoitu-followup-hakukohde-oid "1"}]
                               :hakukohde ["1"]}
              new-application {:answers   [{:key                               "followup-id_1"
                                            :value                             "new-value"
                                            :original-followup                 "followup-id"
                                            :duplikoitu-followup-hakukohde-oid "1"}]
                               :hakukohde ["1"]}
              answers         (:answers (hakija-application-service/merge-unviewable-answers-from-previous new-application old-application form))]
          (should= 1 (count answers))
          (should-contain {:key                               "followup-id_1"
                           :value                             "new-value"
                           :original-followup                 "followup-id"
                           :duplikoitu-followup-hakukohde-oid "1"} answers)))

      (it "does not copy followup answer value from old application when new application has answered different option parent of followup"
          (let [form            {:content [{:id      "id"
                                            :options [{:value 0 :followups [{:id          "followup-id"
                                                                             :cannot-view true}]}
                                                      {:value 1}]}]}
                old-application {:answers   [{:key                               "followup-id_1"
                                              :value                             "old-value"
                                              :original-followup                 "followup-id"
                                              :duplikoitu-followup-hakukohde-oid "1"}
                                             {:key "id_1"
                                              :original-question "id"
                                              :value 0}]
                                 :hakukohde ["1"]}
                new-application {:answers   [{:key "id_1"
                                              :original-question "id"
                                              :value 1}]
                                 :hakukohde ["1"]}
                answers         (:answers (hakija-application-service/merge-unviewable-answers-from-previous new-application old-application form))]
            (should= 1 (count answers))
            (should-contain {:key "id_1"
                             :original-question "id"
                             :value 1} answers)))        )))
