(ns ataru.hakija.hakija-form-service-spec
  (:require [ataru.hakija.hakija-form-service :as hfs]
            [speclj.core :refer [describe it should-be]]
            [clj-time.core :as time]
            [ataru.attachment-deadline.attachment-deadline-service :as attachment-deadline-service]
            [ataru.ohjausparametrit.mock-ohjausparametrit-service :refer [->MockOhjausparametritService]]))

(def test-attachment-deadline-service (attachment-deadline-service/->AttachmentDeadlineService (->MockOhjausparametritService)))

(def now (time/local-date-time 2020 10 14 4 3 27 456))
(def past-date (time/local-date-time 1986 10 14 4 3 27 456))

(def sensitive-answer-field
  {:id "test-field-id"
   :fieldClass "formField"
   :sensitive-answer true})

(def ssn-field
  {:id "ssn"
   :fieldClass "formField"})

(def normal-field
  {:id "something"
   :fieldClass "formField"})

(def lupatieto-field
  {:id "valintatuloksen-julkaisulupa"
   :fieldClass "formField"})

(def koulutusmarkkinointilupa-field
  {:id "koulutusmarkkinointilupa"
   :fieldClass "formField"})

(def asiointikieli-field
  {:id "asiointikieli"
   :fieldClass "formField"})

(def expired-field-deadlines
  (fn [field]
    {(:id field) {:field-id (:id field)
                  :deadline past-date}}))

(def hakuajat-in-processing
  {:uniques []
   :by-oid {}})

(def allowed-to-edit-person-field
  {:id "address"
   :fieldClass "formField"})

(describe "flag-uneditable-and-unviewable-field"
  (describe "when role is hakija"
    (it "should mark field with sensitive-answer as not viewable and not editable"
      (let [new-field (hfs/flag-uneditable-and-unviewable-field test-attachment-deadline-service now nil [:hakija] false nil false false nil nil sensitive-answer-field)]
        (should-be true? (:cannot-view new-field))
        (should-be true? (:cannot-edit new-field))))

    (it "should mark ssn field as not viewable and not editable"
      (let [new-field (hfs/flag-uneditable-and-unviewable-field test-attachment-deadline-service now nil [:hakija] false nil false false nil nil ssn-field)]
        (should-be true? (:cannot-view new-field))
        (should-be true? (:cannot-edit new-field))))

    (it "should mark normal field as viewable and editable"
      (let [new-field (hfs/flag-uneditable-and-unviewable-field test-attachment-deadline-service now nil [:hakija] false nil false false nil nil normal-field)]
        (should-be false? (:cannot-view new-field))
        (should-be false? (:cannot-edit new-field))))

    (it "should mark field with expired hakuaika as viewable but not editable"
      (let [field normal-field
            field-deadlines {(:id field) {:field-id (:id field)
                                          :deadline past-date}}
            new-field (hfs/flag-uneditable-and-unviewable-field test-attachment-deadline-service now nil [:hakija] false field-deadlines false false nil nil normal-field)]
        (should-be false? (:cannot-view new-field))
        (should-be true? (:cannot-edit new-field))))

    (it "should mark normal field with overdue kk payment as viewable but not editable"
        (let [new-field (hfs/flag-uneditable-and-unviewable-field test-attachment-deadline-service now nil [:hakija] false nil false true nil nil normal-field)]
          (should-be false? (:cannot-view new-field))
          (should-be true? (:cannot-edit new-field)))))

  (describe "when role is virkailija"
    (it "should mark field with sensitive-answer as viewable and editable"
      (let [new-field (hfs/flag-uneditable-and-unviewable-field test-attachment-deadline-service now nil [:virkailija] false nil false false nil nil sensitive-answer-field)]
        (should-be false? (:cannot-view new-field))
        (should-be false? (:cannot-edit new-field))))

    (it "should mark ssn field as viewable and editable"
      (let [new-field (hfs/flag-uneditable-and-unviewable-field test-attachment-deadline-service now nil [:virkailija] false nil false false nil nil ssn-field)]
        (should-be false? (:cannot-view new-field))
        (should-be false? (:cannot-edit new-field))))

    (it "should mark normal field as viewable and editable"
      (let [new-field (hfs/flag-uneditable-and-unviewable-field test-attachment-deadline-service now nil [:virkailija] false nil false false nil nil normal-field)]
        (should-be false? (:cannot-view new-field))
        (should-be false? (:cannot-edit new-field))))

    (it "should mark normal field with overdue kk payment as viewable and editable"
        (let [new-field (hfs/flag-uneditable-and-unviewable-field test-attachment-deadline-service now nil [:virkailija] false nil false true nil nil normal-field)]
          (should-be false? (:cannot-view new-field))
          (should-be false? (:cannot-edit new-field))))

    (describe "when using toisen asteen yhteishaku restrictions"
      (it "should mark normal field as viewable but not editable"
        (let [new-field (hfs/flag-uneditable-and-unviewable-field test-attachment-deadline-service now nil [:virkailija] false nil true false nil nil normal-field)]
          (should-be false? (:cannot-view new-field))
          (should-be true? (:cannot-edit new-field))))

      (it "should mark lupatieto field as viewable and editable"
        (let [new-field (hfs/flag-uneditable-and-unviewable-field test-attachment-deadline-service now nil [:virkailija] false nil true false nil nil lupatieto-field)]
          (should-be false? (:cannot-view new-field))
          (should-be false? (:cannot-edit new-field))))

      (it "should mark allowed person info field as viewable and editable"
        (let [new-field (hfs/flag-uneditable-and-unviewable-field test-attachment-deadline-service now nil [:virkailija] false nil true false nil nil allowed-to-edit-person-field)]
          (should-be false? (:cannot-view new-field))
          (should-be false? (:cannot-edit new-field))))))

  (describe "always-editable lupatiedot fields (koulutusmarkkinointilupa, asiointikieli)"
    (describe "when hakuaika has ended (expired field deadline)"
      (it "should mark koulutusmarkkinointilupa as editable even after deadline"
        (let [new-field (hfs/flag-uneditable-and-unviewable-field test-attachment-deadline-service now nil [:hakija] false (expired-field-deadlines koulutusmarkkinointilupa-field) false false nil nil koulutusmarkkinointilupa-field)]
          (should-be false? (:cannot-view new-field))
          (should-be false? (:cannot-edit new-field))))

      (it "should mark asiointikieli as editable even after deadline"
        (let [new-field (hfs/flag-uneditable-and-unviewable-field test-attachment-deadline-service now nil [:hakija] false (expired-field-deadlines asiointikieli-field) false false nil nil asiointikieli-field)]
          (should-be false? (:cannot-view new-field))
          (should-be false? (:cannot-edit new-field))))

      (it "should mark valintatuloksen-julkaisulupa as not editable after deadline"
        (let [new-field (hfs/flag-uneditable-and-unviewable-field test-attachment-deadline-service now nil [:hakija] false (expired-field-deadlines lupatieto-field) false false nil nil lupatieto-field)]
          (should-be false? (:cannot-view new-field))
          (should-be true? (:cannot-edit new-field))))

      (it "should mark normal field as not editable after deadline"
        (let [new-field (hfs/flag-uneditable-and-unviewable-field test-attachment-deadline-service now nil [:hakija] false (expired-field-deadlines normal-field) false false nil nil normal-field)]
          (should-be false? (:cannot-view new-field))
          (should-be true? (:cannot-edit new-field)))))

    (describe "when application is in processing state (jatkuva/joustava haku)"
      (it "should mark koulutusmarkkinointilupa as editable when application is in processing"
        (let [new-field (hfs/flag-uneditable-and-unviewable-field test-attachment-deadline-service now hakuajat-in-processing [:hakija] true nil false false nil nil koulutusmarkkinointilupa-field)]
          (should-be false? (:cannot-view new-field))
          (should-be false? (:cannot-edit new-field))))

      (it "should mark asiointikieli as editable when application is in processing"
        (let [new-field (hfs/flag-uneditable-and-unviewable-field test-attachment-deadline-service now hakuajat-in-processing [:hakija] true nil false false nil nil asiointikieli-field)]
          (should-be false? (:cannot-view new-field))
          (should-be false? (:cannot-edit new-field))))

      (it "should mark valintatuloksen-julkaisulupa as not editable when application is in processing"
        (let [new-field (hfs/flag-uneditable-and-unviewable-field test-attachment-deadline-service now hakuajat-in-processing [:hakija] true nil false false nil nil lupatieto-field)]
          (should-be false? (:cannot-view new-field))
          (should-be true? (:cannot-edit new-field))))

      (it "should mark normal field as not editable when application is in processing"
        (let [new-field (hfs/flag-uneditable-and-unviewable-field test-attachment-deadline-service now hakuajat-in-processing [:hakija] true nil false false nil nil normal-field)]
          (should-be false? (:cannot-view new-field))
          (should-be true? (:cannot-edit new-field)))))))
