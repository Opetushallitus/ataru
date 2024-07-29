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
  (testing "Empty"
    (is (= (query-hakukohteet "auto" :fi false true [] {:options []})
           {:hakukohde-hits [] :rest-results [] :hakukohde-query "auto"})))

  (testing "For virkailija hakukohteet are sorted alphabetically by default"
    (let [virkailija? true]
      (testing "Show hakukohteet in alphabetical order when user language is english and 'order-hakukohteet-by-opetuskieli' is false"
        (is (= (query-hakukohteet "" :en virkailija? false tarjonta-hakukohteet hakukohteet-field)
               {:hakukohde-hits ["5" "1" "4" "2" "3"] :rest-results [] :hakukohde-query ""})))
      (testing "Show hakukohteet with english as opetuskieli first when user language is english and 'order-hakukohteet-by-opetuskieli' is true"
        (is (= (query-hakukohteet "" :en virkailija? true tarjonta-hakukohteet hakukohteet-field)
               {:hakukohde-hits ["4" "2" "3" "5" "1"] :rest-results [] :hakukohde-query ""})))
      (testing "Filter by search term on other language"
        (is (= (query-hakukohteet "english" :fi virkailija? true tarjonta-hakukohteet hakukohteet-field)
               {:hakukohde-hits ["4" "2" "3"] :rest-results [] :hakukohde-query "english"})))))
  (testing "For hakija hakukohteet with ongoing hakuaika are sorted first"
    (let [virkailija? false]
      (testing "Show hakukohteet ongoing hakuaika first and order when user language is english and 'order-hakukohteet-by-opetuskieli' is false"
        (is (= (query-hakukohteet "" :en virkailija? false tarjonta-hakukohteet hakukohteet-field)
               {:hakukohde-hits ["1" "2" "3" "5" "4"] :rest-results [] :hakukohde-query ""})))
      (testing "Show hakukohteet with english as opetuskieli first when user language is english and 'order-hakukohteet-by-opetuskieli' is true"
        (is (= (query-hakukohteet "" :en virkailija? true tarjonta-hakukohteet hakukohteet-field)
               {:hakukohde-hits ["2" "3" "1" "4" "5"] :rest-results [] :hakukohde-query ""})))
      (testing "Filter by search term in other language"
        (is (= (query-hakukohteet "english" :fi virkailija? true tarjonta-hakukohteet hakukohteet-field)
               {:hakukohde-hits ["2" "3" "4"] :rest-results [] :hakukohde-query "english"}))))))

