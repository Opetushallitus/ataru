(ns ataru.hakukohde.hakukohde-store-spec
  (:require [speclj.core :refer :all]
            [ataru.db.db :as db]
            [yesql.core :refer [defqueries]]
            [ataru.hakukohde.hakukohde-store :as store]))

(defonce spec-session {:user-agent "spec"})
(defqueries "sql/dev-application-hakukohde-reviews.sql")

(def application "1.2.246.562.11.00000000002000000142")
(def missing-hakukohde-oid  "1.2.246.562.20.32670078305")
(def hakukohde-oid  "1.2.246.562.20.32670078306")
(def hakukohde-oid2  "1.2.246.562.20.32670078309")

(describe "hakukohde store"
  (tags :unit :hakukohde :selection-state)

  (before-all
    (db/exec :db yesql-add-application_hakukohde_reviews! {:application application :hakukohde hakukohde-oid})
    (db/exec :db yesql-add-application_hakukohde_reviews! {:application application :hakukohde hakukohde-oid2}))

  (after-all
    (db/exec :db yesql-delete_application_hakukohde_reviews! {:application application}))

  (it "should have selection state used as false"
    (let [hakukohde-result (store/selection-state-used-in-hakukohde? missing-hakukohde-oid)]
      (should-not hakukohde-result)))

  (it "should have selection state used as true"
    (let [hakukohde-result (store/selection-state-used-in-hakukohde? hakukohde-oid)]
      (should hakukohde-result)))

  (it "should not throw error for using empty collection"
    (let [hakukohde-result (store/selection-state-used-in-hakukohdes? [])]
      (should= [] hakukohde-result)))

  (it "should return hakukohde oids which use selection state"
    (let [hakukohde-results (store/selection-state-used-in-hakukohdes? [hakukohde-oid hakukohde-oid2 missing-hakukohde-oid])]
      (should= [hakukohde-oid hakukohde-oid2] hakukohde-results)))

  (it "should not return hakukohde oids with no selection state use"
    (let [hakukohde-results (store/selection-state-used-in-hakukohdes? [missing-hakukohde-oid])]
      (should= [] hakukohde-results))))