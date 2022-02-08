(ns ataru.haku.haku-service-spec
  (:require [speclj.core :refer :all]
            [ataru.haku.haku-service :as service]))

(def toisen-asteen-yhteishaku "1.123.123.123")

(def hakukohteet [{:oid "1" :haku-oid "1.123.123.123"} {:oid "2" :haku-oid "1.123.123.123"}])

(def students #{"a" "b" "c"})

(describe "haku service - opinto-ohjaaja"
          (tags :unit :haku :opinto-ohjaaja)

          (it "returns all hakukohteet with application counts"
              (let [applications-persons-and-hakukohteet [{:hakukohde ["1"] :person_oid "a"} {:hakukohde ["2"] :person_oid "b"} {:hakukohde ["1"] :person_oid "c"}]
                    result (service/filter-and-count-hakukohteet-by-students #{toisen-asteen-yhteishaku} hakukohteet applications-persons-and-hakukohteet students)]
                (should= [{:oid "1" :haku-oid "1.123.123.123" :application-count 2} {:oid "2" :haku-oid "1.123.123.123" :application-count 1}] result)))

          (it "returns only toisen asteen yhteishaun hakukohteet with application counts"
              (let [non-yhteishaun-hakukohde {:oid "3" :haku-oid "1.321.321.321"}
                    applications-persons-and-hakukohteet [{:hakukohde ["1"] :person_oid "a"} {:hakukohde ["2"] :person_oid "b"} {:hakukohde ["1"] :person_oid "c"} {:hakukohde ["3"] :person_oid "c"}]
                    result (service/filter-and-count-hakukohteet-by-students #{toisen-asteen-yhteishaku} (concat hakukohteet [non-yhteishaun-hakukohde]) applications-persons-and-hakukohteet students)]
                (should= [{:oid "1" :haku-oid "1.123.123.123" :application-count 2} {:oid "2" :haku-oid "1.123.123.123" :application-count 1}] result)))

          (it "returns only hakukohteet application counts with given students"
              (let [applications-persons-and-hakukohteet [{:hakukohde ["1"] :person_oid "a"} {:hakukohde ["2"] :person_oid "b"} {:hakukohde ["1"] :person_oid "c"} {:hakukohde ["2"] :person_oid "d"}]
                    result (service/filter-and-count-hakukohteet-by-students #{toisen-asteen-yhteishaku} hakukohteet applications-persons-and-hakukohteet students)]
                (should= [{:oid "1" :haku-oid "1.123.123.123" :application-count 2} {:oid "2" :haku-oid "1.123.123.123" :application-count 1}] result))))
