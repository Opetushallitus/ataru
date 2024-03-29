(ns ataru.component-data.base-education-module-higher-spec
  (:require [ataru.component-data.base-education-module-higher :as higher-module]
            [ataru.fixtures.form :as form-fixtures]
            [speclj.core :refer :all]))


(describe "base-education-module-higher"
          (tags :unit :attachments)

          (it "should contain all the specified ids"
              (let [keys-generated higher-module/higher-education-base-education-questions
                    keys-to-check ["secondary-completed-base-education"
                                   "pohjakoulutus_lk--attachment"
                                   "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_grades_dia"
                                   "pohjakoulutus_ulk--attachment_past_translation"
                                   "pohjakoulutus_avoin--attachment"
                                   "pohjakoulutus_ulk--attachment_translation"
                                   "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_past_eb"
                                   "pohjakoulutus_yo--yes-year-of-completion"
                                   "pohjakoulutus_yo_ulkomainen--attachment_ib"
                                   "pohjakoulutus_yo_ulkomainen--attachment_grades_eb"
                                   "pohjakoulutus_avoin--year-of-completion"
                                   "pohjakoulutus_amp--attachment"
                                   "pohjakoulutus_ulk--attachment_transcript"
                                   "pohjakoulutus_amt--attachment"
                                   "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_progress_equi"
                                   "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_past_ib"
                                   "pohjakoulutus_yo_ulkomainen--year-of-completion"
                                   "pohjakoulutus_ulk--attachment_transcript_translation"
                                   "pohjakoulutus_yo_ulkomainen--attachment_grades_ib"
                                   "pohjakoulutus_kk--completion-date"
                                   "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_progress_ib"
                                   "pohjakoulutus_ulk-country"
                                   "pohjakoulutus_kk_ulk--attachment_transcript"
                                   "pohjakoulutus_kk_ulk--attachment_translation_past"
                                   "pohjakoulutus_lk--attachment_progress"
                                   "pohjakoulutus_amp--attachment_past"
                                   "pohjakoulutus_yo_ulkomainen--attachment_eb"
                                   "pohjakoulutus_yo_ulkomainen--attachment_progress_dia"
                                   "pohjakoulutus_yo_ulkomainen--attachment_past_dia"
                                   "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_progress_eb"
                                   "pohjakoulutus_kk--attachment_past"
                                   "pohjakoulutus_ulk--year-of-completion"
                                   "pohjakoulutus_kk--attachment_transcript"
                                   "finnish-vocational-before-1995--year-of-completion"
                                   "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_progress_dia"
                                   "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_eb"
                                   "pohjakoulutus_yo_ulkomainen--attachment_past_eb"
                                   "pohjakoulutus_kk_ulk--attachment_translation"
                                   "pohjakoulutus_ulk--attachment_progress"
                                   "pohjakoulutus_yo_kansainvalinen_suomessa--year-of-completion"
                                   "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_grades_ib"
                                   "pohjakoulutus_kk_ulk--attachment_past"
                                   "pohjakoulutus_muu--year-of-completion"
                                   "pohjakoulutus_amv--year-of-completion"
                                   "secondary-completed-base-education–country"
                                   "pohjakoulutus_amt--year-of-completion"
                                   "pohjakoulutus_kk_ulk--attachment_transcript_past"
                                   "pohjakoulutus_yo_ulkomainen--attachment_progress_eb"
                                   "higher-completed-base-education"
                                   "pohjakoulutus_kk_ulk--attachment_progress_translation"
                                   "pohjakoulutus_ulk--attachment_past"
                                   "pohjakoulutus_kk_ulk--year-of-completion"
                                   "pohjakoulutus_yo--attachment"
                                   "pohjakoulutus_ulk--attachment_progress_translation"
                                   "pohjakoulutus_kk--attachment_transcript_past"
                                   "pohjakoulutus_ulk--attachment"
                                   "pohjakoulutus_kk_ulk-country"
                                   "pohjakoulutus_kk--attachment_transcript_progress"
                                   "pohjakoulutus_yo_ulkomainen--attachment_grades_dia"
                                   "pohjakoulutus_yo_ulkomainen--attachment_dia"
                                   "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_equi"
                                   "pohjakoulutus_amt--attachment_past"
                                   "pohjakoulutus_yo_ulkomainen--attachment_progress_ib"
                                   "pohjakoulutus_kk--attachment"
                                   "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_ib"
                                   "pohjakoulutus_yo_ammatillinen--attachment_competence"
                                   "pohjakoulutus_kk_ulk--attachment"
                                   "pohjakoulutus_yo-country"
                                   "finnish-vocational-before-1995"
                                   "pohjakoulutus_kk_ulk--attachment_transcript_progress"
                                   "pohjakoulutus_kk--attachment_progress"
                                   "pohjakoulutus_lk--attachment_past"
                                   "pohjakoulutus_kk_ulk--attachment_transcript_progress_translation"
                                   "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_dia"
                                   "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_past-equi"
                                   "pohjakoulutus_kk_ulk--attachment_progress"
                                   "pohjakoulutus_yo_ulkomainen--attachment_past_ib"
                                   "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_grades_eb"
                                   "pohjakoulutus_amv--attachment"
                                   "pohjakoulutus_yo_ammatillinen--attachment"
                                   "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_past_dia"
                                   "pohjakoulutus_muu--attachment" "pohjakoulutus_amp--year-of-completion"
                                   "pohjakoulutus_lk--year-of-completion"
                                   "pohjakoulutus_yo_ammatillinen--vocational-completion-year"]]
                  (doseq [id keys-to-check]
                    (should-contain id keys-generated))))

          (it "should be possible to extract attachment ids from base education module"
              (should== #{"pohjakoulutus_kk_ulk--attachment"
                          "pohjakoulutus_lk--attachment"
                          "pohjakoulutus_ulk--attachment_past_translation"
                          "pohjakoulutus_avoin--attachment"
                          "pohjakoulutus_kk_ulk--attachment_transcript"
                          "pohjakoulutus_ulk--attachment_translation"
                          "pohjakoulutus_kk_ulk--attachment_past"
                          "pohjakoulutus_amp--attachment"
                          "pohjakoulutus_ulk--attachment_transcript"
                          "pohjakoulutus_kk_ulk--attachment_transcript_progress_translation"
                          "pohjakoulutus_amt--attachment"
                          "pohjakoulutus_kk_ulk--attachment_progress"
                          "pohjakoulutus_ulk--attachment_transcript_translation"
                          "pohjakoulutus_kk_ulk--attachment_progress_translation"
                          "pohjakoulutus_kk_ulk--attachment_translation"
                          "pohjakoulutus_lk--attachment_progress"
                          "pohjakoulutus_amp--attachment_past"
                          "pohjakoulutus_kk--attachment_past"
                          "pohjakoulutus_kk--attachment_transcript"
                          "pohjakoulutus_ulk--attachment_progress"
                          "pohjakoulutus_ulk--attachment_past"
                          "pohjakoulutus_ulk--attachment_progress_translation"
                          "pohjakoulutus_kk--attachment_transcript_past"
                          "pohjakoulutus_ulk--attachment"
                          "pohjakoulutus_kk--attachment_transcript_progress"
                          "pohjakoulutus_amt--attachment_past"
                          "pohjakoulutus_kk--attachment"
                          "pohjakoulutus_kk--attachment_progress"
                          "pohjakoulutus_lk--attachment_past"
                          "pohjakoulutus_kk_ulk--attachment_transcript_past"
                          "pohjakoulutus_kk_ulk--attachment_transcript_progress"
                          "pohjakoulutus_amv--attachment"
                          "pohjakoulutus_muu--attachment"
                          "pohjakoulutus_kk_ulk--attachment_translation_past"}
                        (higher-module/non-yo-attachment-ids
                          form-fixtures/base-education-attachment-test-form))))