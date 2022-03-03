(ns ataru.applications.harkinnanvaraisuus.harkinnanvaraisuus-util-spec
  (:require [speclj.core :refer [it describe tags should= should-be-nil]]
            [ataru.application.harkinnanvaraisuus-types :refer [harkinnanvaraisuus-reasons]]
            [ataru.applications.harkinnanvaraisuus.harkinnanvaraisuus-util :as hu]))

(describe "harkinnanvaraisuus-util"
          (tags :unit :harkinnanvaraisuus)
          (describe "get-common-harkinnanvaraisuus-reason"
                    (it "returns nil when no common harkinnanvaraisuus"
                        (should-be-nil (hu/get-common-harkinnanvaraisuus-reason {})))

                    (it "returns yksilollistetty-matikka-aikka"
                        (should= (:ataru-yks-mat-ai harkinnanvaraisuus-reasons)
                                 (hu/get-common-harkinnanvaraisuus-reason {:matematiikka-ja-aidinkieli-yksilollistetty_1 "1"})))

                    (it "returns nil when yksilollistetty-matikka-aikka value is something else than 'yes'"
                        (should-be-nil (hu/get-common-harkinnanvaraisuus-reason {:matematiikka-ja-aidinkieli-yksilollistetty_1 "0"})))

                    (it "returns ulkomailla-opiskelu"
                        (should= (:ataru-ulkomailla-opiskelu harkinnanvaraisuus-reasons)
                                 (hu/get-common-harkinnanvaraisuus-reason {:base-education-2nd "0"})))

                    (it "returns ei-paattotodistusta"
                        (should= (:ataru-ei-paattotodistusta harkinnanvaraisuus-reasons)
                                 (hu/get-common-harkinnanvaraisuus-reason {:base-education-2nd "7"}))))

          (describe "get-harkinnanvaraisuus-reason-for-hakukohde"

                    (it "returns none when no harkinnanvaraisuus reason"
                        (should= (:none harkinnanvaraisuus-reasons)
                                 (hu/get-harkinnanvaraisuus-reason-for-hakukohde {} "1.2.3.4")))

                    (it "returns none when harkinnanvaraisuus reason is for another hakukohde"
                        (should= (:none harkinnanvaraisuus-reasons)
                                 (hu/get-harkinnanvaraisuus-reason-for-hakukohde {:harkinnanvaraisuus-reason_1.2.3.5 "0"} "1.2.3.4")))

                    (it "returns learning difficulties"
                        (should= (:ataru-study-challenges harkinnanvaraisuus-reasons)
                                 (hu/get-harkinnanvaraisuus-reason-for-hakukohde {:harkinnanvaraisuus-reason_1.2.3.4 "0"} "1.2.3.4")))

                    (it "returns social reasons"
                        (should= (:ataru-social-reasons harkinnanvaraisuus-reasons)
                                 (hu/get-harkinnanvaraisuus-reason-for-hakukohde {:harkinnanvaraisuus-reason_1.2.3.4 "1"} "1.2.3.4")))

                    (it "returns certificate comparison difficulties"
                        (should= (:ataru-certificate-comparison-difficulties harkinnanvaraisuus-reasons)
                                 (hu/get-harkinnanvaraisuus-reason-for-hakukohde {:harkinnanvaraisuus-reason_1.2.3.4 "2"} "1.2.3.4")))

                    (it "returns insufficient language skill"
                        (should= (:ataru-insufficient-language-skill harkinnanvaraisuus-reasons)
                                 (hu/get-harkinnanvaraisuus-reason-for-hakukohde {:harkinnanvaraisuus-reason_1.2.3.4 "3"} "1.2.3.4")))))