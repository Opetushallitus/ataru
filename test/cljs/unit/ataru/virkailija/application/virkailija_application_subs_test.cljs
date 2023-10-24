(ns ataru.virkailija.application.virkailija-application-subs-test
  (:require [cljs.test :refer-macros [deftest is]]
            [ataru.virkailija.application.application-subs :as app-subs]))

(deftest show-mass-review-notes-link-for-superuser-2-asteen-yhteishaku
  (let [toisen-asteen-yhteishaku?              true
        superuser?                             true
        hakukohde-filtering-for-yhteishaku?    false
        applications-visible-with-some-filter? true
        expected      true
        actual        (app-subs/show-mass-review-notes-link? [toisen-asteen-yhteishaku? superuser? hakukohde-filtering-for-yhteishaku? applications-visible-with-some-filter?])]
    (is (= actual expected))))

(deftest dont-show-mass-review-notes-link-for-2-asteen-yhteishaku
  (let [toisen-asteen-yhteishaku?              true
        superuser?                             false
        hakukohde-filtering-for-yhteishaku?    true
        applications-visible-with-some-filter? true
        expected      false
        actual        (app-subs/show-mass-review-notes-link? [toisen-asteen-yhteishaku? superuser? hakukohde-filtering-for-yhteishaku? applications-visible-with-some-filter?])]
    (is (= actual expected))))

(deftest dont-show-mass-review-notes-link-without-hakukohde-filter-for-kkyhteishaku
  (let [toisen-asteen-yhteishaku?              false
        superuser?                             false
        hakukohde-filtering-for-yhteishaku?    false
        applications-visible-with-some-filter? true
        expected      false
        actual        (app-subs/show-mass-review-notes-link? [toisen-asteen-yhteishaku? superuser? hakukohde-filtering-for-yhteishaku? applications-visible-with-some-filter?])]
    (is (= actual expected))))

(deftest show-mass-review-notes-link-with-hakukohde-filter-for-kkyhteishaku
  (let [toisen-asteen-yhteishaku?              false
        superuser?                             false
        hakukohde-filtering-for-yhteishaku?    true
        applications-visible-with-some-filter? true
        expected      true
        actual        (app-subs/show-mass-review-notes-link? [toisen-asteen-yhteishaku? superuser? hakukohde-filtering-for-yhteishaku? applications-visible-with-some-filter?])]
    (is (= actual expected))))