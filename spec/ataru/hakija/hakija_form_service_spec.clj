(ns ataru.hakija.hakija-form-service-spec
  (:require [ataru.hakija.hakija-form-service :as hfs]
            [speclj.core :refer [describe it should-be]]
            [clj-time.core :as time]))

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

(def allowed-to-edit-person-field
  {:id "address"
   :fieldClass "formField"})

(describe "flag-uneditable-and-unviewable-field"
  (describe "when role is hakija"
    (it "should mark field with sensitive-answer as not viewable and not editable"
      (let [new-field (hfs/flag-uneditable-and-unviewable-field now nil [:hakija] false nil false sensitive-answer-field)]
        (should-be true? (:cannot-view new-field))
        (should-be true? (:cannot-edit new-field))))

    (it "should mark ssn field as not viewable and not editable"
      (let [new-field (hfs/flag-uneditable-and-unviewable-field now nil [:hakija] false nil false ssn-field)]
        (should-be true? (:cannot-view new-field))
        (should-be true? (:cannot-edit new-field))))

    (it "should mark normal field as viewable and editable"
      (let [new-field (hfs/flag-uneditable-and-unviewable-field now nil [:hakija] false nil false normal-field)]
        (should-be false? (:cannot-view new-field))
        (should-be false? (:cannot-edit new-field))))

    (it "should mark field with expired hakuaika as viewable but not editable"
      (let [field normal-field
            field-deadlines {(:id field) {:field-id (:id field)
                                          :deadline past-date}}
            new-field (hfs/flag-uneditable-and-unviewable-field now nil [:hakija] false field-deadlines false normal-field)]
        (should-be false? (:cannot-view new-field))
        (should-be true? (:cannot-edit new-field)))))

  (describe "when role is virkailija"
    (it "should mark field with sensitive-answer as viewable and editable"
      (let [new-field (hfs/flag-uneditable-and-unviewable-field now nil [:virkailija] false nil false sensitive-answer-field)]
        (should-be false? (:cannot-view new-field))
        (should-be false? (:cannot-edit new-field))))

    (it "should mark ssn field as viewable and editable"
      (let [new-field (hfs/flag-uneditable-and-unviewable-field now nil [:virkailija] false nil false ssn-field)]
        (should-be false? (:cannot-view new-field))
        (should-be false? (:cannot-edit new-field))))

    (it "should not mark normal field as viewable and editable"
      (let [new-field (hfs/flag-uneditable-and-unviewable-field now nil [:virkailija] false nil false normal-field)]
        (should-be false? (:cannot-view new-field))
        (should-be false? (:cannot-edit new-field))))

    (describe "when using toisen asteen yhteishaku restrictions"
      (it "should mark normal field as viewable but not editable"
        (let [new-field (hfs/flag-uneditable-and-unviewable-field now nil [:virkailija] false nil true normal-field)]
          (should-be false? (:cannot-view new-field))
          (should-be true? (:cannot-edit new-field))))

      (it "should mark lupatieto field as viewable and editable"
        (let [new-field (hfs/flag-uneditable-and-unviewable-field now nil [:virkailija] false nil true lupatieto-field)]
          (should-be false? (:cannot-view new-field))
          (should-be false? (:cannot-edit new-field))))

      (it "should mark allowed person info field as viewable and editable"
        (let [new-field (hfs/flag-uneditable-and-unviewable-field now nil [:virkailija] false nil true allowed-to-edit-person-field)]
          (should-be false? (:cannot-view new-field))
          (should-be false? (:cannot-edit new-field)))))))
