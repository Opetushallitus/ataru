(ns ataru.hakija.application-hakukohde-util-test
  (:require
   [ataru.hakija.application-hakukohde-util :refer [query-hakukohteet 
                                                    filter-take-hakukohteet-and-ryhmat
                                                    collect-root-ids-related-to-removable-hakukohde
                                                    filter-by-children-id
                                                    prepare-hakukohteet-data]]
   [cljs.test :refer-macros [deftest testing is]]))

(defonce tarjonta-hakukohteet [{:oid "1" :opetuskieli-koodi-urit ["oppilaitoksenopetuskieli_1"] :hakuaika {:on true}}
                               {:oid "2" :opetuskieli-koodi-urit ["oppilaitoksenopetuskieli_4"] :hakuaika {:on true}}
                               {:oid "3" :opetuskieli-koodi-urit ["oppilaitoksenopetuskieli_4"] :hakuaika {:on true}}
                               {:oid "4" :opetuskieli-koodi-urit ["oppilaitoksenopetuskieli_4"] :hakuaika {:on false}}
                               {:oid "5" :opetuskieli-koodi-urit ["oppilaitoksenopetuskieli_1"] :hakuaika {:on false}}])

(defonce hakukohteet-field {:options [{:value "1"
                                       :label {:fi "eka suomeksi"}}
                                      {:value "2"
                                       :label {:en "second in english"}}
                                      {:value "3"
                                       :label {:en "third in english "}}
                                      {:value "4"
                                       :label {:en "fourth in english "}}
                                      {:value "5"
                                       :label {:fi "5. suomeksi"}}]})

(deftest query-hakukohteet-test
  (testing "For virkailija hakukohteet"
    (let [virkailija? true]
      (testing "are sorted alphabetically when 'order-hakukohteet-by-opetuskieli' is false"
        (is (= (query-hakukohteet "" :en virkailija? false tarjonta-hakukohteet hakukohteet-field)
               {:hakukohde-hits ["5" "1" "4" "2" "3"] :rest-results [] :hakukohde-query ""})))
      (testing "are first sorted by opetuskieli and then alphabetically when 'order-hakukohteet-by-opetuskieli' is true"
        (is (= (query-hakukohteet "" :en virkailija? true tarjonta-hakukohteet hakukohteet-field)
               {:hakukohde-hits ["4" "2" "3" "5" "1"] :rest-results [] :hakukohde-query ""})))
      (testing "are sorted alphabetically before filtering by given search term on other language"
        (is (= (query-hakukohteet "english" :fi virkailija? false tarjonta-hakukohteet hakukohteet-field)
               {:hakukohde-hits ["4" "2" "3"] :rest-results [] :hakukohde-query "english"})))))
  (testing "For hakija hakukohteet with ongoing hakuaika first are put first "
    (let [virkailija? false]
      (testing "and sorted alphabetically when 'order-hakukohteet-by-opetuskieli' is false"
        (is (= (query-hakukohteet "" :en virkailija? false tarjonta-hakukohteet hakukohteet-field)
               {:hakukohde-hits ["1" "2" "3" "5" "4"] :rest-results [] :hakukohde-query ""})))
      (testing "and sorted by opetuskieli before sorting alphabetically when 'order-hakukohteet-by-opetuskieli' is true"
        (is (= (query-hakukohteet "" :en virkailija? true tarjonta-hakukohteet hakukohteet-field)
               {:hakukohde-hits ["2" "3" "1" "4" "5"] :rest-results [] :hakukohde-query ""})))
      (testing "and sorted alphabetically before filtering by given search term in other language"
        (is (= (query-hakukohteet "english" :fi virkailija? false tarjonta-hakukohteet hakukohteet-field)
               {:hakukohde-hits ["2" "3" "4"] :rest-results [] :hakukohde-query "english"}))))))

(deftest test-filter-take-hakukohteet-and-ryhmat
  (testing "includes only entries with belongs-to-hakukohteet or belongs-to-hakukohderyhma"
    (let [data [{:foo 1}
                {:belongs-to-hakukohteet ["123"]}
                {:options [{:belongs-to-hakukohderyhma ["ryhma-1"]}]}]]
      (is (= 2 (count (filter-take-hakukohteet-and-ryhmat data)))))))

(deftest test-collect-root-ids-related-to-removable-hakukohde
  (testing "returns ids with matching hakukohde-oid or intersecting ryhma"
    (let [flat-content [{:id 1 :belongs-to-hakukohteet ["hakukohde 1"]}
                        {:id 2 :belongs-to-hakukohderyhma ["ryhma-1"]}
                        {:id 3 :options [{:belongs-to-hakukohteet ["hakukohde 1"]}]}]
          selected-hakukohteet {:removable-ryhmat #{"ryhma-1"}}]
      (is (= #{1 2 3}
             (collect-root-ids-related-to-removable-hakukohde
              "hakukohde 1" flat-content selected-hakukohteet))))))

(deftest test-filter-by-children-id
  (testing "includes items where id or children's id matches"
    (let [questions [{:id 1}
                     {:id 2 :children [{:id 3}]}
                     {:id 4}]
          id-set #{1 3}]
      (is (= [{:id 1} {:id 2 :children [{:id 3}]}]
             (filter-by-children-id questions id-set))))))

(deftest test-prepare-hakukohteet-data
  (testing "returns structured hakukohteet and ryhmat"
    (let [hakukohde-oid "hk-1"
          hakukohteet [{:oid "hk-1" :name "Hakukohde 1" :hakukohderyhmat ["r1"]}
                       {:oid "hk-2" :name "Hakukohde 2" :hakukohderyhmat ["r2"]}]
          selected-oids ["hk-2"]
          {:keys [removable-hakukohteet selected-ryhmat removable-ryhmat]}
          (prepare-hakukohteet-data hakukohde-oid hakukohteet selected-oids)]
      (is (= "hk-1" (-> removable-hakukohteet first :oid)))
      (is (= #{"r2"} selected-ryhmat))
      (is (= #{"r1"} removable-ryhmat)))))