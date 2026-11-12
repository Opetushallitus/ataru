(ns ataru.virkailija.application.virkailija-application-subs-test
  (:require [cljs.test :refer-macros [deftest is]]
            [clojure.string]
            [ataru.virkailija.application.application-authorization-subs :as app-auth-subs]))

(deftest show-mass-review-notes-link-for-superuser-2-asteen-yhteishaku
  (let [toisen-asteen-yhteishaku?              true
        superuser?                             true
        hakukohde-filtering-for-yhteishaku?    false
        applications-visible-with-some-filter? true
        expected      true
        actual        (app-auth-subs/show-mass-review-notes-link? [toisen-asteen-yhteishaku? superuser? hakukohde-filtering-for-yhteishaku? applications-visible-with-some-filter?])]
    (is (= actual expected))))

(deftest dont-show-mass-review-notes-link-for-2-asteen-yhteishaku
  (let [toisen-asteen-yhteishaku?              true
        superuser?                             false
        hakukohde-filtering-for-yhteishaku?    true
        applications-visible-with-some-filter? true
        expected      false
        actual        (app-auth-subs/show-mass-review-notes-link? [toisen-asteen-yhteishaku? superuser? hakukohde-filtering-for-yhteishaku? applications-visible-with-some-filter?])]
    (is (= actual expected))))

(deftest dont-show-mass-review-notes-link-without-hakukohde-filter-for-kkyhteishaku
  (let [toisen-asteen-yhteishaku?              false
        superuser?                             false
        hakukohde-filtering-for-yhteishaku?    false
        applications-visible-with-some-filter? true
        expected      false
        actual        (app-auth-subs/show-mass-review-notes-link? [toisen-asteen-yhteishaku? superuser? hakukohde-filtering-for-yhteishaku? applications-visible-with-some-filter?])]
    (is (= actual expected))))

(deftest show-mass-review-notes-link-with-hakukohde-filter-for-kkyhteishaku
  (let [toisen-asteen-yhteishaku?              false
        superuser?                             false
        hakukohde-filtering-for-yhteishaku?    true
        applications-visible-with-some-filter? true
        expected      true
        actual        (app-auth-subs/show-mass-review-notes-link? [toisen-asteen-yhteishaku? superuser? hakukohde-filtering-for-yhteishaku? applications-visible-with-some-filter?])]
    (is (= actual expected))))

(deftest show-review-info-for-superuser-2-asteen-yhteishaku
  (let [hakukohde-oid                          "1.2.246.562.20.00000000000000024490"
        superuser                              true
        toisen-asteen-yhteishaku?              true
        expected      true
        actual        (app-auth-subs/rights-to-view-review-states-for-hakukohde? hakukohde-oid {} superuser toisen-asteen-yhteishaku?)]
    (is (= actual expected))))

(deftest show-review-info-if-not-2-asteen-yhteishaku
  (let [hakukohde-oid                          "1.2.246.562.20.00000000000000024490"
        superuser                              false
        toisen-asteen-yhteishaku?              false
        expected      true
        actual        (app-auth-subs/rights-to-view-review-states-for-hakukohde? hakukohde-oid {} superuser toisen-asteen-yhteishaku?)]
    (is (= actual expected))))
(def ^:private note-hakukohde-oid "1.2.246.562.20.00000000000000024490")
(def ^:private view-rights {note-hakukohde-oid #{:view-applications}})
(def ^:private edit-rights {note-hakukohde-oid #{:view-applications :edit-applications}})

(deftest own-note-without-hakukohde-removable-with-view-rights
  (is (true? (app-auth-subs/review-note-removable? {:own? true} view-rights))))

(deftest others-note-without-hakukohde-not-removable-with-view-rights
  (is (false? (app-auth-subs/review-note-removable? {:own? false} view-rights)))
  (is (false? (app-auth-subs/review-note-removable? {} view-rights))))

(deftest others-note-without-hakukohde-removable-with-edit-rights
  (is (true? (app-auth-subs/review-note-removable? {:own? false} edit-rights))))

(deftest note-with-hakukohde-not-removable-with-view-rights-even-if-own
  (is (false? (app-auth-subs/review-note-removable? {:own? true :hakukohde note-hakukohde-oid} view-rights)))
  (is (false? (app-auth-subs/review-note-removable? {:own? false :hakukohde note-hakukohde-oid} view-rights))))

(deftest note-with-hakukohde-removable-with-edit-rights-to-that-hakukohde
  (is (true? (app-auth-subs/review-note-removable? {:own? false :hakukohde note-hakukohde-oid} edit-rights)))
  (is (false? (app-auth-subs/review-note-removable? {:own? false :hakukohde "1.2.246.562.20.00000000000000000001"} edit-rights))))

(def ^:private note-variants
  "Kaikki omistajuuden (oma, toisen, tieto puuttuu) ja hakukohteen (ei, tyhjä, on) yhdistelmät."
  (for [ownership [{:own? true} {:own? false} {}]
        hakukohde [{} {:hakukohde ""} {:hakukohde note-hakukohde-oid}]]
    (merge ownership hakukohde)))

(deftest every-note-removable-with-edit-rights-regardless-of-ownership
  (doseq [note note-variants]
    (is (true? (app-auth-subs/review-note-removable? note edit-rights))
        (str "should be removable with edit rights: " (pr-str note)))))

(deftest only-own-note-without-hakukohde-removable-with-view-rights
  (doseq [note note-variants
          :let [expected (and (true? (:own? note))
                              (clojure.string/blank? (:hakukohde note)))]]
    (is (= expected (app-auth-subs/review-note-removable? note view-rights))
        (str "removable with view rights should be " expected ": " (pr-str note)))))

;; Oma hakukohteeton muistiinpano jätetään pois: sen poistoon riittää hakemuksen katseluoikeus,
;; joka on aina olemassa kun hakemus on auki, joten oikeuksia ei tarkisteta erikseen.
(deftest no-note-removable-without-rights
  (doseq [note (remove #(and (:own? %) (clojure.string/blank? (:hakukohde %))) note-variants)]
    (is (false? (app-auth-subs/review-note-removable? note {}))
        (str "should not be removable without rights: " (pr-str note)))))
