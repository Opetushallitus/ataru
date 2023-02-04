(ns ataru.hakukohde.hakukohde-store
  (:require [ataru.db.db :as db]
            [ataru.hakukohde.hakukohde-store-queries :as queries]))

(defn selection-state-used-in-hakukohde?
  [hakukohde-oid]
  (-> (db/exec :db queries/yesql-selection-state-used-in-hakukohde {:hakukohde_oid hakukohde-oid})
      first
      :exists))

(defn selection-state-used-in-hakukohdes?
  [hakukohde-oids]
  (if (seq hakukohde-oids)
    (->> (db/exec :db queries/yesql-selection-state-used-in-hakukohdes {:hakukohde_oids hakukohde-oids})
        (map #(:hakukohde %)))
    []))