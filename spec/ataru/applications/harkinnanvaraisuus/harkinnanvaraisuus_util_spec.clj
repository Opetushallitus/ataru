(ns ataru.applications.harkinnanvaraisuus.harkinnanvaraisuus-util-spec
  (:require [speclj.core :refer [it describe tags should= should-be-nil]]
            [ataru.application.harkinnanvaraisuus-types :refer [harkinnanvaraisuus-reasons]]
            [ataru.applications.harkinnanvaraisuus.harkinnanvaraisuus-util :as hu]))

(describe "get-common-harkinnanvaraisuus-reason"
          (tags :unit :harkinnanvaraisuus)

          (it "returns nil when no common harkinnanvaraisuus"
              (should-be-nil (hu/get-common-harkinnanvaraisuus-reason {})))

          (it "returns yksilollistetty-matikka-aikka"
              (should= (:ataru-yks-mat-ai harkinnanvaraisuus-reasons) (hu/get-common-harkinnanvaraisuus-reason {:matematiikka-ja-aidinkieli-yksilollistetty_1 "1"})))

          (it "returns nil when yksilollistetty-matikka-aikka value is something else than 'yes'"
              (should-be-nil (hu/get-common-harkinnanvaraisuus-reason {:matematiikka-ja-aidinkieli-yksilollistetty_1 "0"})))

          (it "returns ulkomailla-opiskelu"
              (should= (:ataru-ulkomailla-opiskelu harkinnanvaraisuus-reasons) (hu/get-common-harkinnanvaraisuus-reason {:base-education-2nd "0"})))

          (it "returns ei-paattotodistusta"
              (should= (:ataru-ei-paattotodistusta harkinnanvaraisuus-reasons) (hu/get-common-harkinnanvaraisuus-reason {:base-education-2nd "7"}))))