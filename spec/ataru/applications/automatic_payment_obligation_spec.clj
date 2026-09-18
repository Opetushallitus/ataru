(ns ataru.applications.automatic-payment-obligation-spec
  (:require [speclj.core :refer [describe tags it should should-not with-stubs stub
                                  should-have-invoked should-not-have-invoked]]
            [ataru.applications.automatic-payment-obligation :as obligation]
            [ataru.applications.application-store :as application-store]))

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

(describe "start-automatic-payment-obligation-job-for-application"
  (tags :unit)
  (with-stubs)

  (it "starts the automatic payment obligation job for the application's person-oid"
      (with-redefs [application-store/get-application
                    (stub :get-application {:return {:person-oid "1.2.246.562.24.00000000001"}})

                    obligation/start-automatic-payment-obligation-job
                    (stub :start-automatic-payment-obligation-job)]
        (obligation/start-automatic-payment-obligation-job-for-application
         :job-runner "application-id")
        (should-have-invoked :get-application {:with ["application-id"]})
        (should-have-invoked :start-automatic-payment-obligation-job
                             {:with [:job-runner "1.2.246.562.24.00000000001"]})))

  (it "does nothing when the application has no person-oid yet"
      (with-redefs [application-store/get-application
                    (stub :get-application {:return {:person-oid nil}})

                    obligation/start-automatic-payment-obligation-job
                    (stub :start-automatic-payment-obligation-job)]
        (obligation/start-automatic-payment-obligation-job-for-application
         :job-runner "application-id")
        (should-not-have-invoked :start-automatic-payment-obligation-job))))
