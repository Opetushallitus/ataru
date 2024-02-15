(ns ataru.hakija.oppija-session-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [ataru.hakija.ht-util :as ht-util]))

(deftest popup_timers
  (testing "10-minute warnings are set (and not set) correctly"
    (is (= (ht-util/warning-to-set 605 300 30 #{20 30}) [10 5000]))
    (is (= (ht-util/warning-to-set 630 300 30 #{20 30}) [10 30000]))
    (is (= (ht-util/warning-to-set 905 300 30 #{}) [10 305000]))
    (is (= (ht-util/warning-to-set 905 300 30 #{10}) nil))
    (is (= (ht-util/warning-to-set 931 300 30 #{}) nil))
    (is (= (ht-util/warning-to-set 599 300 30 #{}) nil)))
  (testing "20-minute warnings are set (and not set) correctly"
    (is (= (ht-util/warning-to-set 1205 300 30 #{}) [20 5000]))
    (is (= (ht-util/warning-to-set 1230 300 30 #{}) [20 30000]))
    (is (= (ht-util/warning-to-set 1505 300 30 #{}) [20 305000]))
    (is (= (ht-util/warning-to-set 1210 300 30 #{20 30}) nil))
    (is (= (ht-util/warning-to-set 1550 300 30 #{30}) nil))
    (is (= (ht-util/warning-to-set 1190 300 30 #{30}) nil)))
  (testing "30-minute warnings are set (and not set) correctly"
    (is (= (ht-util/warning-to-set 1855 300 30 #{}) [30 55000]))
    (is (= (ht-util/warning-to-set 2113 300 30 #{}) [30 313000]))
    (is (= (ht-util/warning-to-set 2105 300 30 #{}) [30 305000]))
    (is (= (ht-util/warning-to-set 2113 300 30 #{30}) nil))
    (is (= (ht-util/warning-to-set 1750 300 30 #{}) nil))
    (is (= (ht-util/warning-to-set 2160 300 30 #{}) nil))))

