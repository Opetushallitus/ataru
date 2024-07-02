(ns ataru.kk-application-payment.fixtures)

(def koodisto-valtioryhmat-response
  [{:uri "valtioryhmat_1"
    :version 1
    :value "EU"
    :label {}
    :valid { :start "2015-09-03T00:00:00+03:00" }
    :within [{:uri "maatjavaltiot2_246"
              :version 1
              :value "246"}
             {:uri "maatjavaltiot2_250"
              :version 1
              :value "250"}
             {:uri "maatjavaltiot2_233"
              :version 1
              :value "233"}
             {:uri "maatjavaltiot2_056"
              :version 1
              :value "056"}]}
   {:uri "valtioryhmat_2"
    :version 1
    :value "ETA"
    :label {}
    :valid { :start "2015-09-03T00:00:00+03:00" }
    :within {}}])
