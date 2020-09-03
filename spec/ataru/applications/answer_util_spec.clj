(ns ataru.applications.answer-util-spec
  (:require [ataru.applications.answer-util :as answer-util]
            [speclj.core :refer [describe it run-specs should= tags]]))

; required by APIs and some logging, irrelevant for functionality (for now)
; if further tests require this to be a real id, see into application-store and how it functions
(def application-key :odw_service_unit_test)

(def vocational-identifier {:higher-completed-base-education {:value ["pohjakoulutus_am"]}})

(defn select-year-for
  ([answers]
   (select-year-for nil answers))
  ([haku answers]
   (#'answer-util/get-kk-pohjakoulutus haku answers application-key)))

(describe "vocational degree / pohjakoulutus_am completion year selection when"
  (tags :unit :odw :tilastokeskus :OY-342 :OY-346)

  (it "vocational degree has been completed"
      (should= [{:pohjakoulutuskklomake "pohjakoulutus_am" :suoritusvuosi 2016}]
               (select-year-for (merge vocational-identifier {:pohjakoulutus_am--year-of-completion {:value "2016"}}))))

  (it "vocational degree has been completed between 2017 and 2020 (ODW hardcodings)"
      ; 1. yhteishaku
      (should= [{:pohjakoulutuskklomake "pohjakoulutus_am" :suoritusvuosi 2017}]
               (select-year-for (merge vocational-identifier {:5e5a0f04-f04d-478c-b093-3f47d33ba1a4 {:value "2017"}})))
      ; 2. yhteishaku
      (should= [{:pohjakoulutuskklomake "pohjakoulutus_am" :suoritusvuosi 2018}]
               (select-year-for (merge vocational-identifier {:75d3d13c-5865-4924-8a69-d22b8a8aea65 {:value "2018"}}))))

  (it "vocational degree is completed during 2020 (ODW special hardcoding)"
      ; 1. yhteishaku
      (should= [{:pohjakoulutuskklomake "pohjakoulutus_am" :suoritusvuosi 2020}]
               (select-year-for
                {:hakukausi-vuosi 2020}
                (merge vocational-identifier {:f9340e89-4a1e-4626-9246-2a77a32b22ed {:value "1"}})))
      ; 2. yhteishaku
      (should= [{:pohjakoulutuskklomake "pohjakoulutus_am" :suoritusvuosi 2020}]
               (select-year-for
                {:hakukausi-vuosi 2020}
                (merge vocational-identifier {:b6fa0257-c1fd-4107-b151-380e02c56fa9 {:value "1"}})))))

(def double-degree-identifier {:higher-completed-base-education {:value ["pohjakoulutus_yo_ammatillinen"]}})
(def double-degree-vocational-completed {:pohjakoulutus_yo_ammatillinen--vocational-completion-year {:value "2009"}})
(def double-degree-vocational-completed-odw {:60ce79f9-b37a-4b7e-a7e0-f25ba430f055 {:value "2010"}})

(describe "secondary level double degree (kaksoistutkinto) / pohjakoulutus_yo_ammatillinen completion year selection"
  (tags :unit :odw :tilastokeskus :OY-342 :OY-346)

  (it "uses completion year of vocational education with hard-coded static answer id"
    (let [answers (merge double-degree-identifier
                         double-degree-vocational-completed)]
      (should= [{:pohjakoulutuskklomake "pohjakoulutus_yo_ammatillinen" :suoritusvuosi 2009}]
               (select-year-for answers))))

  (it "uses completion year of vocational education with random-uuid looking answer id"
    (let [answers (merge double-degree-identifier
                         double-degree-vocational-completed-odw)]
      (should= [{:pohjakoulutuskklomake "pohjakoulutus_yo_ammatillinen" :suoritusvuosi 2010}]
               (select-year-for answers))))

  (it "applicant should complete education in future (ODW hardcodings)"
    ; 1. yhteishaku, 2017-nyt
    (should= [{:pohjakoulutuskklomake "pohjakoulutus_yo_ammatillinen" :suoritusvuosi 2099}]
             (select-year-for (merge double-degree-identifier {:0a6ba6b1-616c-492b-a501-8b6656900ebd {:value "2099"}})))
    ; 1. yhteishaku, nyt
    (should= [{:pohjakoulutuskklomake "pohjakoulutus_yo_ammatillinen" :suoritusvuosi 2098}]
             (select-year-for
              {:hakukausi-vuosi 2098}
              (merge double-degree-identifier {:22df6790-588f-4c45-8238-3ecfccdf6d93 {:value "1"}})))
    ; 2. yhteishaku, 2017-nyt
    (should= [{:pohjakoulutuskklomake "pohjakoulutus_yo_ammatillinen" :suoritusvuosi 2097}]
             (select-year-for (merge double-degree-identifier {:86c7cc27-e1b3-4b3a-863c-1719b424370f {:value "2097"}})))
    ; 2. yhteishaku, nyt
    (should= [{:pohjakoulutuskklomake "pohjakoulutus_yo_ammatillinen" :suoritusvuosi 2096}]
             (select-year-for
              {:hakukausi-vuosi 2096}
              (merge double-degree-identifier {:dfeb9d56-4d53-4087-9473-1b2d9437e47f {:value "1"}})))))

(def international-matriculation-fi-identifier {:higher-completed-base-education {:value ["pohjakoulutus_yo_kansainvalinen_suomessa"]}})

(describe "international matriculation examination in Finland / pohjakoulutus_yo_kansainvalinen_suomessa completion year selection when"
  (tags :unit :odw :tilastokeskus :OY-342 :OY-346)

  (it "applicant will complete degree this year"
    ; This is a regression: All these were "0" before 2020 but somewhere along the line the form answers were reordered
    ; causing the values to change
    (let [completion-year-output [{:pohjakoulutuskklomake "pohjakoulutus_yo_kansainvalinen_suomessa" :suoritusvuosi 2020}]
          application-form       {:hakukausi-vuosi 2020}
          degrees                {:pohjakoulutus_yo_kansainvalinen_suomessa--ib--year-of-completion-this-year "1"
                                  :pohjakoulutus_yo_kansainvalinen_suomessa--eb--year-of-completion-this-year "1"
                                  :pohjakoulutus_yo_kansainvalinen_suomessa--rb--year-of-completion-this-year "1"
                                  :32b5f6a9-1ccb-4227-8c68-3c0a82fb0a73                                       "0"
                                  :64d561e2-20f7-4143-9ad8-b6fa9a8f6fed                                       "0"
                                  :6b7119c9-42ec-467d-909c-6d1cc555b823                                       "0"}]
      (doall (map (fn [[degree value]]
                    (should= completion-year-output
                             (select-year-for
                              application-form
                              (merge international-matriculation-fi-identifier {degree {:value value}}))))
                  degrees)))))

(def international-matriculation-identifier {:higher-completed-base-education {:value ["pohjakoulutus_yo_ulkomainen"]}})

(describe "international matriculation examination / pohjakoulutus_yo_kansainvalinen completion year selection when"
  (tags :unit :odw :tilastokeskus :OY-342 :OY-346)

  (it "applicant will complete degree this year"
    ; Same as above, this is also a regression for the exact same reason.
    (let [completion-year-output [{:pohjakoulutuskklomake "pohjakoulutus_yo_ulkomainen" :suoritusvuosi 2020}]
          application-form       {:hakukausi-vuosi 2020}
          degrees                {:pohjakoulutus_yo_ulkomainen--ib--year-of-completion-this-year "1"
                                  :pohjakoulutus_yo_ulkomainen--eb--year-of-completion-this-year "1"
                                  :pohjakoulutus_yo_ulkomainen--rb--year-of-completion-this-year "1"
                                  :d037fa56-6354-44fc-87d6-8b774b95dcdf                          "0"
                                  :6e980e4d-257a-49ba-a5e6-5424220e6f08                          "0"
                                  :220c3b47-1ca6-47e7-8af2-2f6ff823e07b                          "0"}]
      (doall (map (fn [[degree value]]
                    (should= completion-year-output
                             (select-year-for
                              application-form
                              (merge international-matriculation-identifier {degree {:value value}}))))
                  degrees)))))

(def higher-education-qualification-identifier {:higher-completed-base-education {:value ["pohjakoulutus_kk"]}})

(describe "Higher education qualification completed in Finland / pohjakoulutus_kk completion year selection when"
  (tags :unit :odw :tilastokeskus :OY-342 :OY-346)

  (it "vocational degree is completed after application period ends (ODW special hardcoding)"
      ; 1. yhteishaku
      (should= [{:pohjakoulutuskklomake "pohjakoulutus_kk" :suoritusvuosi 2020}]
               (select-year-for
                {:hakukausi-vuosi 2020}
                (merge higher-education-qualification-identifier {:64d82dce-14e6-4261-84b1-d868a265cd54 {:value "1"}})))
      ; 2. yhteishaku
      (should= [{:pohjakoulutuskklomake "pohjakoulutus_kk" :suoritusvuosi 2020}]
               (select-year-for
                {:hakukausi-vuosi 2020}
                (merge higher-education-qualification-identifier {:cc4fcbbb-6943-43b4-af9a-0b961bae6bb3 {:value "1"}})))))

(def other-qualification-outside-finland-identifier {:higher-completed-base-education {:value ["pohjakoulutus_ulk"]}})

(describe "Other qualification completed outside Finland / pohjakoulutus_ulk completion year selection when"
  (tags :unit :odw :tilastokeskus :OY-342 :OY-346)

  (it "vocational degree is completed after application period ends (ODW special hardcoding)"
      ; 1. yhteishaku
      (should= [{:pohjakoulutuskklomake "pohjakoulutus_ulk" :suoritusvuosi 2020}]
               (select-year-for
                {:hakukausi-vuosi 2020}
                (merge other-qualification-outside-finland-identifier {:beab461b-b743-44ba-b9f0-1a56daa3eece {:value "1"}})))
      ; 2. yhteishaku
      (should= [{:pohjakoulutuskklomake "pohjakoulutus_ulk" :suoritusvuosi 2020}]
               (select-year-for
                {:hakukausi-vuosi 2020}
                (merge other-qualification-outside-finland-identifier {:3fe6e8e1-6622-4fee-950a-7e602b3cccce {:value "1"}})))))

(def higher-qualification-outside-finland-identifier {:higher-completed-base-education {:value ["pohjakoulutus_kk_ulk"]}})

(describe "Higher education qualification completed outside Finland / pohjakoulutus_kk_ulk completion year selection when"
  (tags :unit :odw :tilastokeskus :OY-342 :OY-346)

  (it "vocational degree is completed after application period ends (ODW special hardcoding)"
      ; 1. yhteishaku
      (should= [{:pohjakoulutuskklomake "pohjakoulutus_kk_ulk" :suoritusvuosi 2020}]
               (select-year-for
                {:hakukausi-vuosi 2020}
                (merge higher-qualification-outside-finland-identifier {:f2b4db5e-7090-4859-b404-4a6334686afe {:value "1"}})))
      ; 2. yhteishaku
      (should= [{:pohjakoulutuskklomake "pohjakoulutus_kk_ulk" :suoritusvuosi 2020}]
               (select-year-for
                {:hakukausi-vuosi 2020}
                (merge higher-qualification-outside-finland-identifier {:a722150f-d2b5-43eb-bdb6-b2d3ca3a428b {:value "1"}})))

      (should= [{:pohjakoulutuskklomake "pohjakoulutus_kk_ulk" :suoritusvuosi 2021}]
               (select-year-for
                 {:hakukausi-vuosi 2021}
                 (merge higher-qualification-outside-finland-identifier {:646f41a3-cac6-496b-a2cc-8c09dc4de1a8 {:value "2021"}})))))

(def further-vocational-qualification-identifier {:higher-completed-base-education {:value ["pohjakoulutus_amt"]}})

(describe "Further vocational qualification completed in Finland / pohjakoulutus_kk_ulk completion year selection when"
  (tags :unit :odw :tilastokeskus :OY-342 :OY-346)

  (it "vocational degree is completed after application period ends (ODW special hardcoding)"
      ; 1. yhteishaku
      (should= [{:pohjakoulutuskklomake "pohjakoulutus_amt" :suoritusvuosi 2020}]
               (select-year-for
                {:hakukausi-vuosi 2020}
                (merge further-vocational-qualification-identifier {:718d7f0b-4075-4960-8456-1ec49e147551 {:value "1"}})))
      ; 2. yhteishaku
      (should= [{:pohjakoulutuskklomake "pohjakoulutus_amt" :suoritusvuosi 2020}]
               (select-year-for
                {:hakukausi-vuosi 2020}
                (merge further-vocational-qualification-identifier {:1a9c3205-0500-439e-84b9-4bb7b90dabe8 {:value "2"}})))))

(def open-higher-education-identifier {:higher-completed-base-education {:value ["pohjakoulutus_avoin"]}})

(describe "Open higher education qualification completion / pohjakoulutus_avoin completion year selection when"
          (tags :unit :odw :tilastokeskus :OY-406)

          (it "open higher education completion year is picked correctly (ODW special hardcoding)"
              (should= [{:pohjakoulutuskklomake "pohjakoulutus_avoin" :suoritusvuosi 2033}]
                       (select-year-for
                         (merge open-higher-education-identifier {:pohjakoulutus_avoin--year-of-completion {:value "2033"}})))))

(run-specs)
