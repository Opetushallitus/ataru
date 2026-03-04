(ns ataru.applications.question-util-spec
  (:require [speclj.core :refer [it describe tags should=]]
            [ataru.applications.question-util :as qu :refer [urheilijan-lisakysymykset-ammatillisiinkohteisiin-wrapper-key
                                                             kaksoistutkinto-keys]]
            [ataru.component-data.base-education-module-2nd :refer [base-education-choice-key
                                                                    base-education-wrapper-key
                                                                    suoritusvuosi-keys
                                                                    tutkintokieli-keys
                                                                    matematiikka-ja-aidinkieli-yksilollistetty-keys]]))

(def form-2nd-aste {"content" [{"id" "oppikeywrapper"
                                "children" [{"id" "kiinnostunut-oppisopimuskoulutuksesta"}]}
                               {"id" urheilijan-lisakysymykset-ammatillisiinkohteisiin-wrapper-key
                                "children" [{"id" "urheilija-amm-key"
                                             "belongs-to-hakukohderyhma" ["ryhma1" "ryhma2"]
                                             "options" [{"followups" [{"id" "urheilija-2nd-amm-peruskoulu"}
                                                                      {"id" "urheilija-2nd-amm-tamakausi"}
                                                                      {"id" "urheilija-2nd-amm-viimekausi"}
                                                                      {"id" "urheilija-2nd-amm-toissakausi"}
                                                                      {"id" "urheilija-2nd-amm-sivulaji"}
                                                                      {"id" "urheilija-2nd-amm-valmentaja-nimi"}
                                                                      {"id" "urheilija-2nd-amm-valmentaja-email"}
                                                                      {"id" "urheilija-2nd-amm-valmentaja-puh"}
                                                                      {"id" "urheilija-2nd-amm-valmennus-seurajoukkue"}
                                                                      {"id" "urheilija-2nd-amm-valmennus-piirijoukkue"}
                                                                      {"id" "urheilija-2nd-amm-valmennus-maajoukkue"}
                                                                      {"id" "urheilija-2nd-amm-lajivalinta-dropdown"
                                                                       "options" [{"label" {"fi" "jalkapallo"}}]}
                                                                      {"id" "urheilija-2nd-amm-seura"}
                                                                      {"id" "urheilija-2nd-amm-liitto"}]}
                                                        {}]}]}
                               {"id" "ammkaksoistutkintowrapper"
                                "children" [{"id" "kaksoistutkinto-amm"}]}
                               {"id" "lukkaksoistutkintowrapper"
                                "children" [{"id" "kaksoistutkinto-lukio"}]}
                               {"id" "sorawrapper"
                                "children" [{"id" "sora-terveys"}
                                            {"id" "sora-aiempi"}]}
                               {"id" base-education-wrapper-key
                                "children" [{"id" base-education-choice-key
                                             "options" [{"followups" [{"id" "suoritusvuosi-perusopetus"}
                                                                      {"id" "tutkintokieli-perusopetus"}]}
                                                        {"followups" [{"id" "suoritusvuosi-yks"}
                                                                      {"id" "tutkintokieli-yks"}]}]}]}]})

(describe "question-util"
          (tags :unit)
          (describe "get-hakurekisteri-toinenaste-specific-questions"
                    (it "returns oppisopimuskoulutus-key"
                        (should= :kiinnostunut-oppisopimuskoulutuksesta (:oppisopimuskoulutus-key (qu/get-hakurekisteri-toinenaste-specific-questions form-2nd-aste))))

                    (it "returns kaksoistutkinto keys"
                        (should= kaksoistutkinto-keys (:kaksoistutkinto-keys (qu/get-hakurekisteri-toinenaste-specific-questions form-2nd-aste))))

                    (it "returns tutkintovuosi keys"
                        (should= (map keyword suoritusvuosi-keys)
                                 (:tutkintovuosi-keys (qu/get-hakurekisteri-toinenaste-specific-questions form-2nd-aste))))

                    (it "returns tutkintokieli keys"
                        (should= (map keyword tutkintokieli-keys)
                                 (:tutkintokieli-keys (qu/get-hakurekisteri-toinenaste-specific-questions form-2nd-aste))))

                    (it "returns matematiikka-ja-aidinkieli-yksilollistetty keys"
                        (should= (map keyword matematiikka-ja-aidinkieli-yksilollistetty-keys)
                                 (:matematiikka-ja-aidinkieli-yksilollistetty-keys (qu/get-hakurekisteri-toinenaste-specific-questions form-2nd-aste))))

                    (it "returns urheilijan-amm-lisakysymys-key and groups"
                        (let [result (qu/get-hakurekisteri-toinenaste-specific-questions form-2nd-aste)]
                          (should= :urheilija-amm-key (:urheilijan-amm-lisakysymys-key result))
                          (should=  #{"ryhma1" "ryhma2"} (:urheilijan-amm-groups result))))

                    (it "returns urheilija vocational extra questions"
                        (let [sport-questions (:urheilijan-amm-lisakysymys-keys (qu/get-hakurekisteri-toinenaste-specific-questions form-2nd-aste))
                              expected {:peruskoulu                  "urheilija-2nd-amm-peruskoulu"
                                        :tamakausi                   "urheilija-2nd-amm-tamakausi"
                                        :viimekausi                  "urheilija-2nd-amm-viimekausi"
                                        :toissakausi                 "urheilija-2nd-amm-toissakausi"
                                        :sivulaji                    "urheilija-2nd-amm-sivulaji"
                                        :valmentaja_nimi             "urheilija-2nd-amm-valmentaja-nimi"
                                        :valmentaja_email            "urheilija-2nd-amm-valmentaja-email"
                                        :valmentaja_puh              "urheilija-2nd-amm-valmentaja-puh"
                                        :valmennusryhma_seurajoukkue "urheilija-2nd-amm-valmennus-seurajoukkue"
                                        :valmennusryhma_piirijoukkue "urheilija-2nd-amm-valmennus-piirijoukkue"
                                        :valmennusryhma_maajoukkue   "urheilija-2nd-amm-valmennus-maajoukkue"
                                        :paalaji-dropdown            "urheilija-2nd-amm-lajivalinta-dropdown"
                                        :seura                       "urheilija-2nd-amm-seura"
                                        :liitto                      "urheilija-2nd-amm-liitto"}]
                          (should= sport-questions expected)))

                    (it "returns urheilija vocational extra questions for specific haku"
                        (let [sport-questions (:urheilijan-amm-lisakysymys-keys (qu/get-hakurekisteri-toinenaste-specific-questions
                                                                                  form-2nd-aste 
                                                                                  "1.2.246.562.29.00000000000000021303"))
                              expected {:peruskoulu                  "22e8cc0a-ef4b-4f47-b0e3-a34bb1c3c07d"
                                        :tamakausi                   "a9a32f30-86b4-4e41-a6d4-4a6863a086ab"
                                        :viimekausi                  "6822dcb8-86b7-400b-a92f-4d02be6b7063"
                                        :toissakausi                 "0278ea3f-e6c1-41c3-a9cc-b3be8acd493d"
                                        :sivulaji                    "25c3adca-8a4f-41f2-91d2-7c787a47d166"
                                        :valmentaja_nimi             "548e2d07-ac5d-49c5-a744-9bc49550c742"
                                        :valmentaja_email            "83a487b1-3485-4763-a996-82a9640d1812"
                                        :valmentaja_puh              "346ac362-4f75-4225-a7ba-3ddcb9b0498d"
                                        :valmennusryhma_seurajoukkue "fc52158d-0d80-42d5-b16b-852e0e50e4d6"
                                        :valmennusryhma_piirijoukkue "6437bc3e-e554-4b9e-982d-4622fc77be50"
                                        :valmennusryhma_maajoukkue   "01a4a834-0128-4147-a890-6e2932c915d6"
                                        :paalaji-dropdown            "09257557-0bbf-4e94-a19b-44b561817eda"
                                        :seura                       "06900eee-7949-445d-ac4f-e8738a904185"
                                        :liitto                      "2b2ede36-e520-4727-8151-93115d26ef7f"}]
                          (should= sport-questions expected)))

                    (it "returns urheilija gymnasium extra questions"
                        (let [sport-questions (:urheilijan-lisakysymys-keys (qu/get-hakurekisteri-toinenaste-specific-questions form-2nd-aste))
                              expected {:keskiarvo                   "urheilija-2nd-keskiarvo"
                                        :peruskoulu                  "urheilija-2nd-peruskoulu"
                                        :tamakausi                   "urheilija-2nd-tamakausi"
                                        :viimekausi                  "urheilija-2nd-viimekausi"
                                        :toissakausi                 "urheilija-2nd-toissakausi"
                                        :sivulaji                    "urheilija-2nd-sivulaji"
                                        :valmentaja_nimi             "urheilija-2nd-valmentaja-nimi"
                                        :valmentaja_email            "urheilija-2nd-valmentaja-email"
                                        :valmentaja_puh              "urheilija-2nd-valmentaja-puh"
                                        :valmennusryhma_seurajoukkue "urheilija-2nd-valmennus-seurajoukkue"
                                        :valmennusryhma_piirijoukkue "urheilija-2nd-valmennus-piirijoukkue"
                                        :valmennusryhma_maajoukkue   "urheilija-2nd-valmennus-maajoukkue"
                                        :valmennusryhmatParent       "84cd8829-ee39-437f-b730-9d68f0f07555"
                                        :paalaji-dropdown            "urheilija-2nd-lajivalinta-dropdown"
                                        :seura                       "urheilija-2nd-seura"
                                        :liitto                      "urheilija-2nd-liitto"}]
                          (should= sport-questions expected)))

                    (it "returns urheilija gymnasium extra questions for specific haku"
                        (let [sport-questions (:urheilijan-lisakysymys-keys (qu/get-hakurekisteri-toinenaste-specific-questions
                                                                                  form-2nd-aste
                                                                                  "1.2.246.562.29.00000000000000005368"))
                              expected {:keskiarvo                   "7b88594a-c308-41f8-bac3-2d3779ea4443"
                                        :peruskoulu                  "9a4de985-9a70-4de6-bfa7-0a5c2f18cb8c"
                                        :tamakausi                   "f944c9c3-c1f8-43c7-a27e-49d89d4e8eec"
                                        :viimekausi                  "e3e8b5ef-f8d9-4256-8ef6-1a52d562a370"
                                        :toissakausi                 "95b565ee-f64e-4805-b319-55b99bbce1a8"
                                        :sivulaji                    "dbfc1215-896a-47d4-bc07-b9f1494658f4"
                                        :valmentaja_nimi             "a1f1147a-d466-4d98-9a62-079a42dd4089"
                                        :valmentaja_email            "625fe96d-a5ff-4b3a-8ace-e36524215d1c"
                                        :valmentaja_puh              "f1c5986c-bea8-44f7-8324-d1cac179e6f4"
                                        :valmennusryhma_seurajoukkue "92d579fb-dafa-4edc-9e05-8f493badc4f3"
                                        :valmennusryhma_piirijoukkue "58125631-762a-499b-a402-717778bf8233"
                                        :valmennusryhma_maajoukkue   "261d7ffc-54a7-4c5c-ab80-82f7de49f648"
                                        :paalajiSeuraLiittoParent    "98951abd-fdd5-46a0-8427-78fe9706d286"}]
                          (should= sport-questions expected)))

                    (it "returns sora-aiempi key"
                        (should= "sora-aiempi" (:sora-aiempi-key (qu/get-hakurekisteri-toinenaste-specific-questions form-2nd-aste))))

                    (it "returns sora-terveys key"
                        (should= "sora-terveys" (:sora-terveys-key (qu/get-hakurekisteri-toinenaste-specific-questions form-2nd-aste))))))
