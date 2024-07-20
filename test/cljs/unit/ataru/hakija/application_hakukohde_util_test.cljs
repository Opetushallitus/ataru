(ns ataru.hakija.application-hakukohde-util-test
  (:require
   [ataru.hakija.application-hakukohde-util :refer [query-hakukohteet]]
   [cljs.test :refer-macros [deftest testing is]]))

(deftest query-hakukohteet-test
  (testing "Empty"
    (is (= (query-hakukohteet "auto" :fi false [] {})
           {:hakukohde-hits [] :rest-results [] :hakukohde-query "auto"})))
  (testing "Show hakukohteet with english first when user language is english"
    (is (= (query-hakukohteet "" :en false [{:oid "1"}
                                            {:oid "2"}
                                            {:oid "3"}
                                            {:oid "4"}
                                            {:oid "5"}]
                              {:options [{:value "1"
                                          :label {:fi "eka suomeksi"}}
                                         {:value "2"
                                          :label {:en "second in english"}}
                                         {:value "3"
                                          :label {:en "third in english "}}
                                         {:value "4"
                                          :label {:en "fourth in english "}}
                                         {:value "5"
                                          :label {:fi "5. suomeksi"}}]})
           {:hakukohde-hits ["4" "2" "3" "5" "1"] :rest-results [] :hakukohde-query ""}))))

