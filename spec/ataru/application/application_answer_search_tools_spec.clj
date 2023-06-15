(ns ataru.application.application-answer-search-tools-spec
  (:require [ataru.application.application-answer-search-tools :as atools]
            [speclj.core :refer [describe tags it should=]]
            [ataru.util :as util]))

(def hakukohde-question {:id "hakukohteet"
                         :label {:en ""
                                 :fi "Hakukohteet"
                                 :sv ""}
                         :params {}
                         :options []
                         :fieldType "hakukohteet"
                         :fieldClass "formField"
                         :validators ["required"]
                         :exclude-from-answers-if-hidden true})

(def hakukohde-answer {:key "hakukohteet"
                       :label {:en ""
                               :fi "Hakukohteet"
                               :sv ""}
                       :value ["1.2.246.562.20.352373851710"]
                       :fieldType "hakukohteet"})

(def per-hakukohde-specific-dropdown {:id "ce1864c0-ce3f-4c1d-8405-5c4ydff7ca2c"
                                      :label {:fi "Mikä on vointisi?"}
                                      :options [{:label {:fi "Hyvä"}
                                                 :value "Hyvä"}
                                                {:label {:fi "Huono"}
                                                 :value "Huono"
                                                 :followups [{:id "ce1864c0-ce3f-4c1d-8405-5c4ydff7dff2"
                                                              :label {:fi "Mikä mättää?"}
                                                              :fieldType "textField"
                                                              :fieldClass "formField"
                                                              :params {}
                                                              :validators []}]}]
                                      :fieldType "dropdown"
                                      :validators ["required"]
                                      :fieldClass "formField"
                                      :per-hakukohde true
                                      :belongs-to-hakukohderyhma ["1.2.246.562.20.352373851710"]})

(def per-hakukohde-specific-dropdown-answer {:key "ce1864c0-ce3f-4c1d-8405-5c4ydff7ca2c_1.2.246.562.20.352373851710",
                                             :label {:fi "Mikä on vointisi?"}
                                             :value "Huono"
                                             :duplikoitu-kysymys-hakukohde-oid "1.2.246.562.20.352373851710"
                                             :original-question "ce1864c0-ce3f-4c1d-8405-5c4ydff7ca2c"
                                             :fieldType "dropdown"})
(def per-hakukohde-specific-followup-answer {:key "ce1864c0-ce3f-4c1d-8405-5c4ydff7dff2_1.2.246.562.20.352373851710",
                                             :label {:fi "Mikä mättää?"}
                                             :value "Pikkuvarvas osui lipaston jalkaan"
                                             :duplikoitu-followup-hakukohde-oid "1.2.246.562.20.352373851710"
                                             :original-followup "ce1864c0-ce3f-4c1d-8405-5c4ydff7dff2"
                                             :fieldType "textField"})

(def flat-form (util/flatten-form-fields [hakukohde-question per-hakukohde-specific-dropdown per-hakukohde-specific-dropdown]))

(def application {:answers [hakukohde-answer per-hakukohde-specific-dropdown-answer per-hakukohde-specific-followup-answer]})

(describe "application answer search tools"
          (tags :unit :answers)

          (it "gets matching per hakukohde question"
              (should= (:id per-hakukohde-specific-dropdown)
                       (:id (atools/get-matching-per-hakukohde-question flat-form per-hakukohde-specific-dropdown-answer))))

          (it "gets matching per hakukohde followup"
              (should= "ce1864c0-ce3f-4c1d-8405-5c4ydff7dff2"
                       (:id (atools/get-matching-per-hakukohde-question flat-form per-hakukohde-specific-followup-answer))))

          (it "filters required per-hakukohde answers"
              (should= [per-hakukohde-specific-dropdown-answer]
                       (atools/filter-required-per-hakukohde-answers
                         flat-form [per-hakukohde-specific-dropdown-answer per-hakukohde-specific-followup-answer])))

          (it "gets matching parent field"
              (should= (:id per-hakukohde-specific-dropdown)
                       (:id (atools/get-matching-parent-field
                              flat-form
                              (atools/get-matching-per-hakukohde-question flat-form per-hakukohde-specific-followup-answer)))))

          (it "gets matching parent answer"
              (should= per-hakukohde-specific-dropdown-answer
                       (atools/get-matching-per-hakukohde-parent-answer
                         application
                         (atools/get-matching-parent-field
                           flat-form
                           (atools/get-matching-per-hakukohde-question flat-form per-hakukohde-specific-followup-answer))
                         per-hakukohde-specific-followup-answer
                         (atools/get-matching-per-hakukohde-question flat-form per-hakukohde-specific-followup-answer)))))
