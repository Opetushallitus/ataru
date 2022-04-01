(ns ataru.applications.harkinnanvaraisuus.harkinnanvaraisuus-util-spec
  (:require [speclj.core :refer [it describe tags should= should-be-nil]]
            [ataru.application.harkinnanvaraisuus-types :refer [harkinnanvaraisuus-reasons]]
            [ataru.applications.harkinnanvaraisuus.harkinnanvaraisuus-util :as hu]))

(def pick-value-fn (fn [answers question]
                     (question answers)))

(describe "harkinnanvaraisuus-util"
          (tags :unit :harkinnanvaraisuus)
          (describe "get-common-harkinnanvaraisuus-reason"
                    (it "returns nil when no common harkinnanvaraisuus"
                        (should-be-nil (hu/get-common-harkinnanvaraisuus-reason {} pick-value-fn)))

                    (it "returns yksilollistetty-matikka-aikka"
                        (should= (:ataru-yks-mat-ai harkinnanvaraisuus-reasons)
                                 (hu/get-common-harkinnanvaraisuus-reason {:matematiikka-ja-aidinkieli-yksilollistetty_1 "1"} pick-value-fn)))

                    (it "returns nil when yksilollistetty-matikka-aikka value is something else than 'yes'"
                        (should-be-nil (hu/get-common-harkinnanvaraisuus-reason {:matematiikka-ja-aidinkieli-yksilollistetty_1 "0"} pick-value-fn)))

                    (it "returns ulkomailla-opiskelu"
                        (should= (:ataru-ulkomailla-opiskelu harkinnanvaraisuus-reasons)
                                 (hu/get-common-harkinnanvaraisuus-reason {:base-education-2nd "0"} pick-value-fn)))

                    (it "returns ei-paattotodistusta"
                        (should= (:ataru-ei-paattotodistusta harkinnanvaraisuus-reasons)
                                 (hu/get-common-harkinnanvaraisuus-reason {:base-education-2nd "7"} pick-value-fn))))

          (describe "get-targeted-harkinnanvaraisuus-reason-for-hakukohde"

                    (it "returns none when no harkinnanvaraisuus reason"
                        (should= (:none harkinnanvaraisuus-reasons)
                                 (hu/get-targeted-harkinnanvaraisuus-reason-for-hakukohde {} "1.2.3.4" pick-value-fn)))

                    (it "returns none when harkinnanvaraisuus reason is for another hakukohde"
                        (should= (:none harkinnanvaraisuus-reasons)
                                 (hu/get-targeted-harkinnanvaraisuus-reason-for-hakukohde {:harkinnanvaraisuus-reason_1.2.3.5 "0"} "1.2.3.4" pick-value-fn)))

                    (it "returns learning difficulties"
                        (should= (:ataru-oppimisvaikeudet harkinnanvaraisuus-reasons)
                                 (hu/get-targeted-harkinnanvaraisuus-reason-for-hakukohde {:harkinnanvaraisuus-reason_1.2.3.4 "0"} "1.2.3.4" pick-value-fn)))

                    (it "returns social reasons"
                        (should= (:ataru-sosiaaliset-syyt harkinnanvaraisuus-reasons)
                                 (hu/get-targeted-harkinnanvaraisuus-reason-for-hakukohde {:harkinnanvaraisuus-reason_1.2.3.4 "1"} "1.2.3.4" pick-value-fn)))

                    (it "returns certificate comparison difficulties"
                        (should= (:ataru-koulutodistusten-vertailuvaikeudet harkinnanvaraisuus-reasons)
                                 (hu/get-targeted-harkinnanvaraisuus-reason-for-hakukohde {:harkinnanvaraisuus-reason_1.2.3.4 "2"} "1.2.3.4" pick-value-fn)))

                    (it "returns insufficient language skill"
                        (should= (:ataru-riittamaton-tutkintokielen-taito harkinnanvaraisuus-reasons)
                                 (hu/get-targeted-harkinnanvaraisuus-reason-for-hakukohde {:harkinnanvaraisuus-reason_1.2.3.4 "3"} "1.2.3.4" pick-value-fn))))

          (describe "get-harkinnanvaraisuus-reason-for-hakukohde"

                    (it "returns none when no harkinnanvaraisuus reason"
                        (should= (:none harkinnanvaraisuus-reasons)
                                 (hu/get-harkinnanvaraisuus-reason-for-hakukohde {} "1.2.3.4")))

                    (it "returns none when harkinnanvaraisuus reason is for another hakukohde"
                        (should= (:none harkinnanvaraisuus-reasons)
                                 (hu/get-harkinnanvaraisuus-reason-for-hakukohde {:harkinnanvaraisuus-reason_1.2.3.5 {:value "0"}} "1.2.3.4")))

                    (it "returns ulkomailla opiskelu"
                        (should= (:ataru-ulkomailla-opiskelu harkinnanvaraisuus-reasons)
                                 (hu/get-harkinnanvaraisuus-reason-for-hakukohde {:base-education-2nd {:value "0"} :harkinnanvaraisuus-reason_1.2.3.4 {:value "0"}} "1.2.3.4")))

                    (it "returns matematiikka ja aidinkieli yksilollistetty reason"
                        (should= (:ataru-yks-mat-ai harkinnanvaraisuus-reasons)
                                 (hu/get-harkinnanvaraisuus-reason-for-hakukohde {:matematiikka-ja-aidinkieli-yksilollistetty_1 {:value "1"} :harkinnanvaraisuus-reason_1.2.3.4 {:value "1"}} "1.2.3.4")))

                    (it "returns certificate comparison difficulties"
                        (should= (:ataru-koulutodistusten-vertailuvaikeudet harkinnanvaraisuus-reasons)
                                 (hu/get-harkinnanvaraisuus-reason-for-hakukohde {:harkinnanvaraisuus-reason_1.2.3.4 {:value "2"}} "1.2.3.4")))

                    (it "returns insufficient language skill"
                        (should= (:ataru-riittamaton-tutkintokielen-taito harkinnanvaraisuus-reasons)
                                 (hu/get-harkinnanvaraisuus-reason-for-hakukohde {:harkinnanvaraisuus-reason_1.2.3.4 {:value "3"}} "1.2.3.4"))))

          (describe "assoc-harkinnanvaraisuus-tieto"

                    (it "returns harkinnanvaraisuus reason none for each hakukohde"
                        (let [tarjonta-application {:keyValues {"question" "answer"}
                                                    :hakutoiveet [{:hakukohdeOid "1.2.3"} {:hakukohdeOid "1.2.1"}]}
                              result (hu/assoc-harkinnanvaraisuustieto tarjonta-application)
                              hakutoiveet (:hakutoiveet result)]
                          (should= 2 (count hakutoiveet))
                          (should= true (every? #(= (:none harkinnanvaraisuus-reasons) (:harkinnanvaraisuus %)) hakutoiveet))))

                    (it "returns common harkinnanvaraisuus reason for each hakukohde"
                        (let [tarjonta-application {:keyValues {"base-education-2nd" "7"}
                                                    :hakutoiveet [{:hakukohdeOid "1.2.3"} {:hakukohdeOid "1.2.1"}]}
                              result (hu/assoc-harkinnanvaraisuustieto tarjonta-application)
                              hakutoiveet (:hakutoiveet result)]
                          (should= 2 (count hakutoiveet))
                          (should= true (every? #(= (:ataru-ei-paattotodistusta harkinnanvaraisuus-reasons)
                                                    (:harkinnanvaraisuus %)) hakutoiveet))))

                    (it "returns common harkinnanvaraisuus reason for each hakukohde even if there are hakukohde specific reasons in answers"
                        (let [tarjonta-application {:keyValues {"base-education-2nd" "0", "harkinnanvaraisuus-reason_1.2.3" "1"}
                                                    :hakutoiveet [{:hakukohdeOid "1.2.3"} {:hakukohdeOid "1.2.1"}]}
                              result (hu/assoc-harkinnanvaraisuustieto tarjonta-application)
                              hakutoiveet (:hakutoiveet result)]
                          (should= 2 (count hakutoiveet))
                          (should= true (every? #(= (:ataru-ulkomailla-opiskelu harkinnanvaraisuus-reasons)
                                                    (:harkinnanvaraisuus %)) hakutoiveet))))

                    (it "returns specific harkinnanvaraisuus reason for each hakukohde"
                        (let [tarjonta-application {:keyValues {"base-education-2nd" "1", "harkinnanvaraisuus-reason_1.2.3" "0"}
                                                    :hakutoiveet [{:hakukohdeOid "1.2.3"} {:hakukohdeOid "1.2.1"}]}
                              result (hu/assoc-harkinnanvaraisuustieto tarjonta-application)
                              hakutoiveet (:hakutoiveet result)]
                          (should= 2 (count hakutoiveet))
                          (should= (:ataru-oppimisvaikeudet harkinnanvaraisuus-reasons) (:harkinnanvaraisuus (first hakutoiveet)))
                          (should= (:none harkinnanvaraisuus-reasons) (:harkinnanvaraisuus (last hakutoiveet))))))

          (describe "assoc-harkinnanvaraisuustieto-to-hakukohde"

                    (it "assocs none when no harkinnanvaraisuus reason"
                        (should= (:none harkinnanvaraisuus-reasons)
                                 (:harkinnanvaraisuudenSyy (hu/assoc-harkinnanvaraisuustieto-to-hakukohde {} "1.2.3.4"))))

                    (it "assocs none when harkinnanvaraisuus reason is for another hakukohde"
                        (should= (:none harkinnanvaraisuus-reasons)
                                 (:harkinnanvaraisuudenSyy (hu/assoc-harkinnanvaraisuustieto-to-hakukohde
                                                             {:harkinnanvaraisuus-reason_1.2.3.5 {:value "0"}} "1.2.3.4"))))

                    (it "assocs ulkomailla opiskelu"
                        (should= (:ataru-ulkomailla-opiskelu harkinnanvaraisuus-reasons)
                                 (:harkinnanvaraisuudenSyy (hu/assoc-harkinnanvaraisuustieto-to-hakukohde
                                                             {:base-education-2nd {:value "0"} :harkinnanvaraisuus-reason_1.2.3.4 {:value "0"}} "1.2.3.4"))))

                    (it "assocs matematiikka ja aidinkieli yksilollistetty reason"
                        (should= (:ataru-yks-mat-ai harkinnanvaraisuus-reasons)
                                 (:harkinnanvaraisuudenSyy (hu/assoc-harkinnanvaraisuustieto-to-hakukohde
                                                             {:matematiikka-ja-aidinkieli-yksilollistetty_1 {:value "1"} :harkinnanvaraisuus-reason_1.2.3.4 {:value "1"}} "1.2.3.4"))))

                    (it "assocs certificate comparison difficulties"
                        (should= (:ataru-koulutodistusten-vertailuvaikeudet harkinnanvaraisuus-reasons)
                                 (:harkinnanvaraisuudenSyy (hu/assoc-harkinnanvaraisuustieto-to-hakukohde
                                                             {:harkinnanvaraisuus-reason_1.2.3.4 {:value "2"}} "1.2.3.4"))))

                    (it "assocs insufficient language skill"
                        (should= (:ataru-riittamaton-tutkintokielen-taito harkinnanvaraisuus-reasons)
                                 (:harkinnanvaraisuudenSyy (hu/assoc-harkinnanvaraisuustieto-to-hakukohde
                                                             {:harkinnanvaraisuus-reason_1.2.3.4 {:value "3"}} "1.2.3.4"))))))