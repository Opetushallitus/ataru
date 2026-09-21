(ns ataru.hakija.hakija-application-service-spec
  (:require [ataru.hakija.hakija-application-service :as hakija-application-service]
            [ataru.applications.automatic-eligibility :as automatic-eligibility]
            [ataru.applications.automatic-payment-obligation :as automatic-payment-obligation]
            [ataru.email.application-email-jobs :as application-email]
            [ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-store :as tutkintojen-tunnustaminen-store]
            [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit]
            [speclj.core :refer [describe it should-be should-not-be should-contain should=
                                  with-stubs stub should-have-invoked should-not-have-invoked]]))

(def edited-cannot-edit-questions #'hakija-application-service/edited-cannot-edit-questions)
(def start-hakija-edit-jobs #'hakija-application-service/start-hakija-edit-jobs)
(def start-virkailija-edit-jobs #'hakija-application-service/start-virkailija-edit-jobs)
(def form-with-followup {:content [{:id      "id"
                                    :options [{:value 0 :followups [{:id          "followup-id"
                                                                     :cannot-view true
                                                                     :followup-of "id"
                                                                     :option-value 0}]}
                                              {:value 1}]}]})

(def followup-answer {:key                               "followup-id_1"
                      :value                             "old-value"
                      :original-followup                 "followup-id"
                      :duplikoitu-followup-hakukohde-oid "1"})
(defn- create-per-hakukohde-answer [id hakukohde answer-value]
  {:key (str id "_" hakukohde)
   :original-question id
   :duplikoitu-kysymys-hakukohde-oid hakukohde
   :value answer-value})

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
        (let [old-application {:answers   [followup-answer]
                               :hakukohde ["1"]}
              new-application {:answers   []
                               :hakukohde []}
              answers         (:answers (hakija-application-service/merge-unviewable-answers-from-previous new-application old-application form-with-followup))]
          (should= 0 (count answers))))

      (it "copies answer value from old application when new application doesn't have answer"
        (let [old-application {:answers   [followup-answer]
                               :hakukohde ["1"]}
              new-application {:answers   [{:key                               "followup-id_1"
                                            :value                             nil
                                            :original-followup                 "followup-id"
                                            :duplikoitu-followup-hakukohde-oid "1"}]
                               :hakukohde ["1"]}
              answers         (:answers (hakija-application-service/merge-unviewable-answers-from-previous new-application old-application form-with-followup))]
          (should= 1 (count answers))
          (should-contain followup-answer answers)))

      (it "replaces old answer value with value from new application"
        (let [old-application {:answers   [followup-answer]
                               :hakukohde ["1"]}
              new-application {:answers   [{:key                               "followup-id_1"
                                            :value                             "new-value"
                                            :original-followup                 "followup-id"
                                            :duplikoitu-followup-hakukohde-oid "1"}]
                               :hakukohde ["1"]}
              answers         (:answers (hakija-application-service/merge-unviewable-answers-from-previous new-application old-application form-with-followup))]
          (should= 1 (count answers))
          (should-contain {:key                               "followup-id_1"
                           :value                             "new-value"
                           :original-followup                 "followup-id"
                           :duplikoitu-followup-hakukohde-oid "1"} answers)))

      (it "does not copy followup answer value from old application when new application has answered different option parent of followup"
          (let [old-application {:answers   [followup-answer
                                             (create-per-hakukohde-answer "id" "1" 0)]
                                 :hakukohde ["1"]}
                new-application {:answers   [(create-per-hakukohde-answer "id" "1" 1)]
                                 :hakukohde ["1"]}
                answers         (:answers (hakija-application-service/merge-unviewable-answers-from-previous new-application old-application form-with-followup))]
            (should= 1 (count answers))
            (should-contain (create-per-hakukohde-answer "id" "1" 1) answers)))

      (it "copies followup answer value from old application when new application has multiple parents with different values but with same application target"
          (let [old-application {:answers   [followup-answer
                                             (create-per-hakukohde-answer "id" "1" 0)]
                                 :hakukohde ["1"]}
                new-application {:answers   [(create-per-hakukohde-answer "id" "2" 1)
                                             (create-per-hakukohde-answer "id" "1" 0)]
                                 :hakukohde ["1" "2"]}
                answers         (:answers (hakija-application-service/merge-unviewable-answers-from-previous new-application old-application form-with-followup))]
            (should= 3 (count answers))
            (should-contain followup-answer answers)))

      (it "does not copy followup answer value from old application when new application has multiple parents with different values but with same application target"
          (let [old-application {:answers   [followup-answer
                                             (create-per-hakukohde-answer "id" "1" 0)]
                                 :hakukohde ["1"]}
                new-application {:answers   [(create-per-hakukohde-answer "id" "2" 0)
                                             (create-per-hakukohde-answer "id" "1" 1)]
                                 :hakukohde ["1" "2"]}
                answers         (:answers (hakija-application-service/merge-unviewable-answers-from-previous new-application old-application form-with-followup))]
            (should= 2 (count answers))
            (should-contain (create-per-hakukohde-answer "id" "1" 1) answers)
            (should-contain (create-per-hakukohde-answer "id" "2" 0) answers)))

      (it "does not copy followup answer value from old application when new application has multiple parents with different values but not same application target"
          (let [old-application {:answers   [followup-answer
                                             (create-per-hakukohde-answer "id" "1" 0)]
                                 :hakukohde ["1"]}
                new-application {:answers   [(create-per-hakukohde-answer "id" "2" 0)
                                             (create-per-hakukohde-answer "id" "3" 1)]
                                 :hakukohde ["1" "2" "3"]}
                answers         (:answers (hakija-application-service/merge-unviewable-answers-from-previous new-application old-application form-with-followup))]
            (should= 2 (count answers))
            (should-contain (create-per-hakukohde-answer "id" "3" 1) answers)
            (should-contain (create-per-hakukohde-answer "id" "2" 0) answers))))))

(describe "start-hakija-edit-jobs"
  (with-stubs)

  (it "re-triggers the automatic payment obligation check for the edited application's person-oid"
    (with-redefs [application-email/start-email-edit-confirmation-job
                  (stub :start-email-edit-confirmation-job)

                  tutkintojen-tunnustaminen-store/start-tutkintojen-tunnustaminen-edit-job
                  (stub :start-tutkintojen-tunnustaminen-edit-job)

                  tutkintojen-tunnustaminen-store/start-tutu-application-edit-notification-job
                  (stub :start-tutu-application-edit-notification-job)

                  hakija-application-service/start-attachment-finalizer-job
                  (stub :start-attachment-finalizer-job)

                  automatic-eligibility/start-automatic-eligibility-if-ylioppilas-job
                  (stub :start-automatic-eligibility-if-ylioppilas-job)

                  automatic-payment-obligation/start-automatic-payment-obligation-job
                  (stub :start-automatic-payment-obligation-job)]
      (start-hakija-edit-jobs :attachment-deadline-service :koodisto-cache :tarjonta-service
                              :organization-service :ohjausparametrit-service :job-runner
                              "application-id" {:person-oid "1.2.246.562.24.00000000001"})
      (should-have-invoked :start-automatic-payment-obligation-job
                           {:with [:job-runner "1.2.246.562.24.00000000001"]})))

  (it "does not start the automatic payment obligation job when the application has no person-oid yet"
    (with-redefs [application-email/start-email-edit-confirmation-job
                  (stub :start-email-edit-confirmation-job)

                  tutkintojen-tunnustaminen-store/start-tutkintojen-tunnustaminen-edit-job
                  (stub :start-tutkintojen-tunnustaminen-edit-job)

                  tutkintojen-tunnustaminen-store/start-tutu-application-edit-notification-job
                  (stub :start-tutu-application-edit-notification-job)

                  hakija-application-service/start-attachment-finalizer-job
                  (stub :start-attachment-finalizer-job)

                  automatic-eligibility/start-automatic-eligibility-if-ylioppilas-job
                  (stub :start-automatic-eligibility-if-ylioppilas-job)

                  automatic-payment-obligation/start-automatic-payment-obligation-job
                  (stub :start-automatic-payment-obligation-job)]
      (start-hakija-edit-jobs :attachment-deadline-service :koodisto-cache :tarjonta-service
                              :organization-service :ohjausparametrit-service :job-runner
                              "application-id" {:person-oid nil})
      (should-not-have-invoked :start-automatic-payment-obligation-job))))

(describe "start-virkailija-edit-jobs"
  (with-stubs)

  (it "starts the automatic payment obligation job when the application already has a person-oid"
    (with-redefs [virkailija-edit/invalidate-virkailija-update-and-rewrite-secret
                  (stub :invalidate-virkailija-update-and-rewrite-secret)

                  hakija-application-service/start-person-creation-job
                  (stub :start-person-creation-job)

                  hakija-application-service/start-attachment-finalizer-job
                  (stub :start-attachment-finalizer-job)

                  automatic-eligibility/start-automatic-eligibility-if-ylioppilas-job
                  (stub :start-automatic-eligibility-if-ylioppilas-job)

                  automatic-payment-obligation/start-automatic-payment-obligation-job
                  (stub :start-automatic-payment-obligation-job)]
      (start-virkailija-edit-jobs :job-runner :virkailija-secret "application-id"
                                  {:person-oid "1.2.246.562.24.00000000001"})
      (should-have-invoked :start-automatic-payment-obligation-job
                           {:with [:job-runner "1.2.246.562.24.00000000001"]})
      (should-not-have-invoked :start-person-creation-job)))

  (it "starts person creation instead when the application has no person-oid yet"
    (with-redefs [virkailija-edit/invalidate-virkailija-update-and-rewrite-secret
                  (stub :invalidate-virkailija-update-and-rewrite-secret)

                  hakija-application-service/start-person-creation-job
                  (stub :start-person-creation-job)

                  hakija-application-service/start-attachment-finalizer-job
                  (stub :start-attachment-finalizer-job)

                  automatic-eligibility/start-automatic-eligibility-if-ylioppilas-job
                  (stub :start-automatic-eligibility-if-ylioppilas-job)

                  automatic-payment-obligation/start-automatic-payment-obligation-job
                  (stub :start-automatic-payment-obligation-job)]
      (start-virkailija-edit-jobs :job-runner :virkailija-secret "application-id"
                                  {:person-oid nil})
      (should-have-invoked :start-person-creation-job {:with [:job-runner "application-id"]})
      (should-not-have-invoked :start-automatic-payment-obligation-job))))
