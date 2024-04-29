(ns ataru.applications.harkinnanvaraisuus.harkinnanvaraisuus-util-spec
  (:require [speclj.core :refer [it describe tags should= should-be-nil]]
            [ataru.application.harkinnanvaraisuus.harkinnanvaraisuus-types :refer [harkinnanvaraisuus-reasons]]
            [ataru.application.harkinnanvaraisuus.harkinnanvaraisuus-util :as hu]))

(def pick-value-fn (fn [answers question]
                     (question answers)))

(defn make-hakukohde
  [oid & {:keys [no-harkinnanvaraisuus]}]
  {:oid oid
   :voiko-hakukohteessa-olla-harkinnanvaraisesti-hakeneita? (not no-harkinnanvaraisuus)})

(def basic-hakukohde (make-hakukohde "1.2.3.4"))

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

                    (it "returns nil when yksilollistetty-matikka-aikka value is 'yes' but perusopetuksen suoritusvuosi is after 2018"
                        (should-be-nil (hu/get-common-harkinnanvaraisuus-reason {:matematiikka-ja-aidinkieli-yksilollistetty_1 "1" :suoritusvuosi-perusopetus "2020"} pick-value-fn)))

                    (it "returns yksilollistetty-matikka-aikka when yksilollistetty-matikka-aikka value is 'yes' and perusopetuksen suoritusvuosi is before 2018"
                        (should= (:ataru-yks-mat-ai harkinnanvaraisuus-reasons)
                         (hu/get-common-harkinnanvaraisuus-reason {:matematiikka-ja-aidinkieli-yksilollistetty_1 "1" :suoritusvuosi-perusopetus "2016"} pick-value-fn)))

                    (it "returns ulkomailla-opiskelu"
                        (should= (:ataru-ulkomailla-opiskelu harkinnanvaraisuus-reasons)
                                 (hu/get-common-harkinnanvaraisuus-reason {:base-education-2nd "0"} pick-value-fn)))

                    (it "returns ei-paattotodistusta"
                        (should= (:ataru-ei-paattotodistusta harkinnanvaraisuus-reasons)
                                 (hu/get-common-harkinnanvaraisuus-reason {:base-education-2nd "7"} pick-value-fn))))

          (describe "get-targeted-harkinnanvaraisuus-reason-for-hakukohde"

                    (it "returns none when no harkinnanvaraisuus reason"
                        (should= (:none harkinnanvaraisuus-reasons)
                                 (hu/get-targeted-harkinnanvaraisuus-reason-for-hakukohde {} basic-hakukohde pick-value-fn)))

                    (it "returns none when harkinnanvaraisuus reason is for another hakukohde"
                        (should= (:none harkinnanvaraisuus-reasons)
                                 (hu/get-targeted-harkinnanvaraisuus-reason-for-hakukohde {:harkinnanvaraisuus-reason_1.2.3.5 "0"} basic-hakukohde pick-value-fn)))

                    (it "returns learning difficulties"
                        (should= (:ataru-oppimisvaikeudet harkinnanvaraisuus-reasons)
                                 (hu/get-targeted-harkinnanvaraisuus-reason-for-hakukohde {:harkinnanvaraisuus-reason_1.2.3.4 "0"} basic-hakukohde pick-value-fn)))

                    (it "returns social reasons"
                        (should= (:ataru-sosiaaliset-syyt harkinnanvaraisuus-reasons)
                                 (hu/get-targeted-harkinnanvaraisuus-reason-for-hakukohde {:harkinnanvaraisuus-reason_1.2.3.4 "1"} basic-hakukohde pick-value-fn)))

                    (it "returns certificate comparison difficulties"
                        (should= (:ataru-koulutodistusten-vertailuvaikeudet harkinnanvaraisuus-reasons)
                                 (hu/get-targeted-harkinnanvaraisuus-reason-for-hakukohde {:harkinnanvaraisuus-reason_1.2.3.4 "2"} basic-hakukohde pick-value-fn)))

                    (it "returns insufficient language skill"
                        (should= (:ataru-riittamaton-tutkintokielen-taito harkinnanvaraisuus-reasons)
                                 (hu/get-targeted-harkinnanvaraisuus-reason-for-hakukohde {:harkinnanvaraisuus-reason_1.2.3.4 "3"} basic-hakukohde pick-value-fn)))

                    (it "returns ei-harkinnanvarainen-hakukohde"
                        (let [hakukohde (make-hakukohde "1.2.3.4" :no-harkinnanvaraisuus true)]
                          (should= (:ei-harkinnanvarainen-hakukohde harkinnanvaraisuus-reasons)
                                   (hu/get-targeted-harkinnanvaraisuus-reason-for-hakukohde {:harkinnanvaraisuus-reason_1.2.3.4 "0"} hakukohde pick-value-fn)))))

          (describe "get-harkinnanvaraisuus-reason-for-hakukohde"

                    (it "returns none when no harkinnanvaraisuus reason"
                        (should= (:none harkinnanvaraisuus-reasons)
                                 (hu/get-harkinnanvaraisuus-reason-for-hakukohde {} basic-hakukohde)))

                    (it "returns none when harkinnanvaraisuus reason is for another hakukohde"
                        (should= (:none harkinnanvaraisuus-reasons)
                                 (hu/get-harkinnanvaraisuus-reason-for-hakukohde {:harkinnanvaraisuus-reason_1.2.3.5 {:value "0"}} basic-hakukohde)))

                    (it "returns ulkomailla opiskelu"
                        (should= (:ataru-ulkomailla-opiskelu harkinnanvaraisuus-reasons)
                                 (hu/get-harkinnanvaraisuus-reason-for-hakukohde {:base-education-2nd {:value "0"} :harkinnanvaraisuus-reason_1.2.3.4 {:value "0"}} basic-hakukohde)))

                    (it "returns matematiikka ja aidinkieli yksilollistetty reason"
                        (should= (:ataru-yks-mat-ai harkinnanvaraisuus-reasons)
                                 (hu/get-harkinnanvaraisuus-reason-for-hakukohde {:matematiikka-ja-aidinkieli-yksilollistetty_1 {:value "1"} :harkinnanvaraisuus-reason_1.2.3.4 {:value "1"}} basic-hakukohde)))

                    (it "returns certificate comparison difficulties"
                        (should= (:ataru-koulutodistusten-vertailuvaikeudet harkinnanvaraisuus-reasons)
                                 (hu/get-harkinnanvaraisuus-reason-for-hakukohde {:harkinnanvaraisuus-reason_1.2.3.4 {:value "2"}} basic-hakukohde)))

                    (it "returns insufficient language skill"
                        (should= (:ataru-riittamaton-tutkintokielen-taito harkinnanvaraisuus-reasons)
                                 (hu/get-harkinnanvaraisuus-reason-for-hakukohde {:harkinnanvaraisuus-reason_1.2.3.4 {:value "3"}} basic-hakukohde)))

                    (it "returns ei-harkinnanvarainen-hakukohde"
                        (let [hakukohde (make-hakukohde "1.2.3.4" :no-harkinnanvaraisuus true)]
                          (should= (:ei-harkinnanvarainen-hakukohde harkinnanvaraisuus-reasons)
                                   (hu/get-harkinnanvaraisuus-reason-for-hakukohde {:harkinnanvaraisuus-reason_1.2.3.4 {:value "0"}} hakukohde))))
                    (it "returns ataru-ei-paattotodistusta when perusopetuksen suoritusvuosi is 2020"
                        (let [hakukohde (make-hakukohde "1.2.3.4")]
                          (should= (:ataru-ei-paattotodistusta harkinnanvaraisuus-reasons)
                                   (hu/get-harkinnanvaraisuus-reason-for-hakukohde {:suoritusvuosi-perusopetus {:value "2020"}} hakukohde))))
                    (it "returns none when perusopetuksen suoritusvuosi is 2016"
                        (let [hakukohde (make-hakukohde "1.2.3.4")]
                          (should= (:none harkinnanvaraisuus-reasons)
                                   (hu/get-harkinnanvaraisuus-reason-for-hakukohde {:suoritusvuosi-perusopetus {:value "2016"}} hakukohde)))))

          (describe "assoc-harkinnanvaraisuus-tieto"

                    (it "returns harkinnanvaraisuus reason none for each hakukohde"
                        (let [hakukohteet [(make-hakukohde "1.2.3")
                                           (make-hakukohde "1.2.1")]
                              tarjonta-application {:keyValues {"question" "answer"}
                                                    :hakutoiveet [{:hakukohdeOid "1.2.3"} {:hakukohdeOid "1.2.1"}]}
                              result (hu/assoc-harkinnanvaraisuustieto hakukohteet tarjonta-application)
                              hakutoiveet (:hakutoiveet result)]
                          (should= 2 (count hakutoiveet))
                          (should= true (every? #(= (:none harkinnanvaraisuus-reasons) (:harkinnanvaraisuus %)) hakutoiveet))))

                    (it "returns common harkinnanvaraisuus reason for each hakukohde"
                        (let [hakukohteet [(make-hakukohde "1.2.3")
                                           (make-hakukohde "1.2.1")]
                              tarjonta-application {:keyValues {"base-education-2nd" "7"}
                                                    :hakutoiveet [{:hakukohdeOid "1.2.3"} {:hakukohdeOid "1.2.1"}]}
                              result (hu/assoc-harkinnanvaraisuustieto hakukohteet tarjonta-application)
                              hakutoiveet (:hakutoiveet result)]
                          (should= 2 (count hakutoiveet))
                          (should= true (every? #(= (:ataru-ei-paattotodistusta harkinnanvaraisuus-reasons)
                                                    (:harkinnanvaraisuus %)) hakutoiveet))))

                    (it "returns common harkinnanvaraisuus reason for each hakukohde even if there are hakukohde specific reasons in answers"
                        (let [hakukohteet [(make-hakukohde "1.2.3")
                                           (make-hakukohde "1.2.1")]
                              tarjonta-application {:keyValues {"base-education-2nd" "0", "harkinnanvaraisuus-reason_1.2.3" "1"}
                                                    :hakutoiveet [{:hakukohdeOid "1.2.3"} {:hakukohdeOid "1.2.1"}]}
                              result (hu/assoc-harkinnanvaraisuustieto hakukohteet tarjonta-application)
                              hakutoiveet (:hakutoiveet result)]
                          (should= 2 (count hakutoiveet))
                          (should= true (every? #(= (:ataru-ulkomailla-opiskelu harkinnanvaraisuus-reasons)
                                                    (:harkinnanvaraisuus %)) hakutoiveet))))

                    (it "returns specific harkinnanvaraisuus reason for each hakukohde"
                        (let [hakukohteet [(make-hakukohde "1.2.3")
                                           (make-hakukohde "1.2.1")]
                              tarjonta-application {:keyValues {"base-education-2nd" "1", "harkinnanvaraisuus-reason_1.2.3" "0"}
                                                    :hakutoiveet [{:hakukohdeOid "1.2.3"} {:hakukohdeOid "1.2.1"}]}
                              result (hu/assoc-harkinnanvaraisuustieto hakukohteet tarjonta-application)
                              hakutoiveet (:hakutoiveet result)]
                          (should= 2 (count hakutoiveet))
                          (should= (:ataru-oppimisvaikeudet harkinnanvaraisuus-reasons) (:harkinnanvaraisuus (first hakutoiveet)))
                          (should= (:none harkinnanvaraisuus-reasons) (:harkinnanvaraisuus (last hakutoiveet)))))

                    (it "returns ei-harkinnanvarainen-hakukohde even if there is a common reason"
                      (let [hakukohteet [(make-hakukohde "1.2.3")
                                         (make-hakukohde "1.2.1" :no-harkinnanvaraisuus true)]
                            tarjonta-application {:keyValues {"base-education-2nd" "7"}
                                                  :hakutoiveet [{:hakukohdeOid "1.2.3"} {:hakukohdeOid "1.2.1"}]}
                            result (hu/assoc-harkinnanvaraisuustieto hakukohteet tarjonta-application)
                            [ht1 ht2 :as hakutoiveet] (:hakutoiveet result)]
                        (should= 2 (count hakutoiveet))
                        (should= (:ataru-ei-paattotodistusta harkinnanvaraisuus-reasons) (:harkinnanvaraisuus ht1))
                        (should= (:ei-harkinnanvarainen-hakukohde harkinnanvaraisuus-reasons) (:harkinnanvaraisuus ht2))))

            (it "returns ei-harkinnanvarainen-hakukohde even if there is a hakukohde specific reason"
              (let [hakukohteet [(make-hakukohde "1.2.3")
                                 (make-hakukohde "1.2.1" :no-harkinnanvaraisuus true)]
                    tarjonta-application {:keyValues {"harkinnanvaraisuus-reason_1.2.3" "1"
                                                      "harkinnanvaraisuus-reason_1.2.1" "1"}
                                          :hakutoiveet [{:hakukohdeOid "1.2.3"} {:hakukohdeOid "1.2.1"}]}
                    result (hu/assoc-harkinnanvaraisuustieto hakukohteet tarjonta-application)
                    [ht1 ht2 :as hakutoiveet] (:hakutoiveet result)]
                (should= 2 (count hakutoiveet))
                (should= (:ataru-sosiaaliset-syyt harkinnanvaraisuus-reasons) (:harkinnanvaraisuus ht1))
                (should= (:ei-harkinnanvarainen-hakukohde harkinnanvaraisuus-reasons) (:harkinnanvaraisuus ht2)))))

          (describe "assoc-harkinnanvaraisuustieto-to-hakukohde"

                    (it "assocs none when no harkinnanvaraisuus reason"
                        (should= (:none harkinnanvaraisuus-reasons)
                                 (:harkinnanvaraisuudenSyy (hu/assoc-harkinnanvaraisuustieto-to-hakukohde {} basic-hakukohde))))

                    (it "assocs none when harkinnanvaraisuus reason is for another hakukohde"
                        (should= (:none harkinnanvaraisuus-reasons)
                                 (:harkinnanvaraisuudenSyy (hu/assoc-harkinnanvaraisuustieto-to-hakukohde
                                                             {:harkinnanvaraisuus-reason_1.2.3.5 {:value "0"}} basic-hakukohde))))

                    (it "assocs ulkomailla opiskelu"
                        (should= (:ataru-ulkomailla-opiskelu harkinnanvaraisuus-reasons)
                                 (:harkinnanvaraisuudenSyy (hu/assoc-harkinnanvaraisuustieto-to-hakukohde
                                                             {:base-education-2nd {:value "0"} :harkinnanvaraisuus-reason_1.2.3.4 {:value "0"}} basic-hakukohde))))

                    (it "assocs matematiikka ja aidinkieli yksilollistetty reason"
                        (should= (:ataru-yks-mat-ai harkinnanvaraisuus-reasons)
                                 (:harkinnanvaraisuudenSyy (hu/assoc-harkinnanvaraisuustieto-to-hakukohde
                                                             {:matematiikka-ja-aidinkieli-yksilollistetty_1 {:value "1"} :harkinnanvaraisuus-reason_1.2.3.4 {:value "1"}} basic-hakukohde))))

                    (it "assocs certificate comparison difficulties"
                        (should= (:ataru-koulutodistusten-vertailuvaikeudet harkinnanvaraisuus-reasons)
                                 (:harkinnanvaraisuudenSyy (hu/assoc-harkinnanvaraisuustieto-to-hakukohde
                                                             {:harkinnanvaraisuus-reason_1.2.3.4 {:value "2"}} basic-hakukohde))))

                    (it "assocs insufficient language skill"
                        (should= (:ataru-riittamaton-tutkintokielen-taito harkinnanvaraisuus-reasons)
                                 (:harkinnanvaraisuudenSyy (hu/assoc-harkinnanvaraisuustieto-to-hakukohde
                                                             {:harkinnanvaraisuus-reason_1.2.3.4 {:value "3"}} basic-hakukohde))))

                    (it "assocs ei-harkinnanvarainen-hakukohde"
                        (let [hakukohde (make-hakukohde "1.2.3.4" :no-harkinnanvaraisuus true)]
                          (should= (:ei-harkinnanvarainen-hakukohde harkinnanvaraisuus-reasons)
                                   (:harkinnanvaraisuudenSyy (hu/assoc-harkinnanvaraisuustieto-to-hakukohde {:harkinnanvaraisuus-reason_1.2.3.4 {:value "0"}} hakukohde))))))

          (describe "decide-reason"
                    (it "returns always ei-harkinnanvarainen-hakukohde as targeted reason for ei-harkinnanvarainen hakukohde"
                        (should= (:ei-harkinnanvarainen-hakukohde harkinnanvaraisuus-reasons)
                                 (hu/decide-reason
                                  (:ataru-ei-paattotodistusta harkinnanvaraisuus-reasons)
                                  (:ei-harkinnanvarainen-hakukohde harkinnanvaraisuus-reasons)
                                  true)))
                    (it "returns common reason if there is both common reason and targeted reason for hakukohde"
                        (should= (:ataru-ulkomailla-opiskelu harkinnanvaraisuus-reasons)
                                 (hu/decide-reason
                                  (:ataru-ulkomailla-opiskelu harkinnanvaraisuus-reasons)
                                  (:ataru-sosiaaliset-syyt harkinnanvaraisuus-reasons)
                                  false)))
                    (it "returns ataru-ei-paattotodistusta (to be overridden) if info should be in sure and there is no reason"
                        (should= (:ataru-ei-paattotodistusta harkinnanvaraisuus-reasons)
                                 (hu/decide-reason nil (:none harkinnanvaraisuus-reasons) true)))
                    (it "returns ei-harkinnanvarainen targeted reason if info is not in sure and there is no common or targeted reason"
                        (should= (:none harkinnanvaraisuus-reasons)
                                 (hu/decide-reason nil (:none harkinnanvaraisuus-reasons) false)))
                    (it "returns targeted reason if info is not in sure and there is a targeted reason"
                        (should= (:ataru-oppimisvaikeudet harkinnanvaraisuus-reasons)
                                 (hu/decide-reason nil (:ataru-oppimisvaikeudet harkinnanvaraisuus-reasons) false)))))
