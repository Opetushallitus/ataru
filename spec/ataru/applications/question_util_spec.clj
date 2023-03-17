(ns ataru.applications.question-util-spec
  (:require [speclj.core :refer [it describe tags should=]]
            [ataru.translations.texts :refer [base-education-2nd-module-texts]]
            [ataru.applications.question-util :as qu :refer [kiinnostunut-oppisopimuskoulutuksesta-wrapper-label
                                                             amm-kaksoistutkinto-wrapper-label
                                                             lukio-kaksoistutkinto-wrapper-label
                                                             sora-question-wrapper-label
                                                             urheilijan-lisakysymykset-ammatillisiinkohteisiin-wrapper-key]]
            [ataru.component-data.base-education-module-2nd :refer [base-education-choice-key base-education-wrapper-key]]))

(def form-2nd-aste {"content" [{"id" "oppikeywrapper"
                                "label" kiinnostunut-oppisopimuskoulutuksesta-wrapper-label
                                "children" [{"id" "oppikey"}]}
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
                                "label" amm-kaksoistutkinto-wrapper-label
                                "children" [{"id" "amm2tutkintokey"}]}
                               {"id" "lukkaksoistutkintowrapper"
                                "label" lukio-kaksoistutkinto-wrapper-label
                                "children" [{"id" "lukio2tutkintokey"}]}
                               {"id" "sorawrapper"
                                "label" sora-question-wrapper-label
                                "children" [{"id" "terveys-key"}
                                            {"id" "aiempi-key"}]}
                               {"id" base-education-wrapper-key
                                "children" [{"id" base-education-choice-key
                                             "options" [{"followups" [{"id" "year-key1"
                                                                       "label" (:year-of-graduation-question base-education-2nd-module-texts)}
                                                                      {"id" "language-key1"
                                                                       "label" (:study-language base-education-2nd-module-texts)}]}
                                                        {"followups" [{"id" "year-key2"
                                                                       "label" (:year-of-graduation-question base-education-2nd-module-texts)}
                                                                      {"id" "language-key2"
                                                                       "label" (:study-language base-education-2nd-module-texts)}]}]}]}]})

(describe "question-util"
          (tags :unit)
          (describe "get-hakurekisteri-toinenaste-specific-questions"
                    (it "returns oppisopimuskoulutus-key"
                        (should= :oppikey (:oppisopimuskoulutus-key (qu/get-hakurekisteri-toinenaste-specific-questions form-2nd-aste))))

                    (it "returns kaksoistutkinto keys"
                        (should= ["amm2tutkintokey" "lukio2tutkintokey"] (:kaksoistutkinto-keys (qu/get-hakurekisteri-toinenaste-specific-questions form-2nd-aste))))

                    (it "returns tutkintovuosi keys"
                        (should= [:year-key1 :year-key2] (:tutkintovuosi-keys (qu/get-hakurekisteri-toinenaste-specific-questions form-2nd-aste))))

                    (it "returns tutkintokieli keys"
                        (should= [:language-key1 :language-key2] (:tutkintokieli-keys (qu/get-hakurekisteri-toinenaste-specific-questions form-2nd-aste))))

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

                    (it "returns sora-aiempi key"
                        (should= "aiempi-key" (:sora-aiempi-key (qu/get-hakurekisteri-toinenaste-specific-questions form-2nd-aste))))

                    (it "returns sora-terveys key"
                        (should= "terveys-key" (:sora-terveys-key (qu/get-hakurekisteri-toinenaste-specific-questions form-2nd-aste))))))
