(ns ataru.applications.automatic-payment-obligation-spec
  (:require [speclj.core :refer [describe tags it should should-not]]
            [ataru.applications.automatic-payment-obligation :as obligation]))

(describe "nationality-finland-or-aland?"
  (tags :unit)

  (it "returns true for Finnish nationality (246)"
      (should (obligation/nationality-finland-or-aland?
                {:kansalaisuus [{:kansalaisuusKoodi "246"}]})))

  (it "returns true for Åland nationality (248)"
      (should (obligation/nationality-finland-or-aland?
                {:kansalaisuus [{:kansalaisuusKoodi "248"}]})))

  (it "returns false for non-Finnish nationality"
      (should-not (obligation/nationality-finland-or-aland?
                    {:kansalaisuus [{:kansalaisuusKoodi "784"}]})))

  (it "returns true when Finnish nationality is one of multiple nationalities"
      (should (obligation/nationality-finland-or-aland?
                {:kansalaisuus [{:kansalaisuusKoodi "784"}
                                {:kansalaisuusKoodi "246"}]})))

  (it "returns true when Åland nationality is one of multiple nationalities"
      (should (obligation/nationality-finland-or-aland?
                {:kansalaisuus [{:kansalaisuusKoodi "784"}
                                {:kansalaisuusKoodi "248"}]}))))
