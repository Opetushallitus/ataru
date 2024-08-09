(ns ataru.hakija.application-hakukohde-util-test
  (:require
   [ataru.hakija.application-hakukohde-util :refer [query-hakukohteet]]
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

