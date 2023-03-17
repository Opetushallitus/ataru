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
                                             "belongs-to-hakukohderyhma" ["ryhma1" "ryhma2"]}]}
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

                    (it "returns sora-aiempi key"
                        (should= "aiempi-key" (:sora-aiempi-key (qu/get-hakurekisteri-toinenaste-specific-questions form-2nd-aste))))

                    (it "returns sora-terveys key"
                        (should= "terveys-key" (:sora-terveys-key (qu/get-hakurekisteri-toinenaste-specific-questions form-2nd-aste))))))
