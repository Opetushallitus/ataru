(ns ataru.hakukohde.hakukohde-store
  (:require [ataru.db.db :as db]
            [ataru.hakukohde.hakukohde-store-queries :as queries]))

(defn- exec-db
  [ds-key query params]
  (db/exec ds-key query params))

(defn selection-state-used-in-hakukohde?
  [hakukohde-oid]
  (-> (exec-db :db queries/yesql-selection-state-used-in-hakukohde {:hakukohde_oid hakukohde-oid})
      first
      :exists))
