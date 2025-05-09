(ns ataru.hakija.application-hakukohde-handlers-test
  (:require
   [ataru.hakija.application-hakukohde-handlers :refer [filter-take-hakukohteet-and-ryhmat
                                                        ;collect-root-ids-related-to-removable-hakukohde
                                                        ;filter-by-children-id
                                                        ;prepare-hakukohteet-data
                                                        ]]
   [cljs.test :refer-macros [deftest testing is]]))

(deftest test-filter-take-hakukohteet-and-ryhmat
  (testing "inncludes only entries with belongs-to-hakukohteet or belongs-to-hakukohderyhma"
    (let [data [{:foo 1}
                {:belongs-to-hakukohteet ["123"]}
                {:options [{:belongs-to-hakukohderyhma ["ryhma-1"]}]}]]
      (is (= 2 [count (filter-take-hakukohteet-and-ryhmat data)] )))))

