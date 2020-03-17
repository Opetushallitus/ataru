(ns ataru.virkailija.application.hyvaksynnan-ehto.hyvaksynnan-ehto-xforms)

(defn filter-hyvaksynnan-ehdot-for-correct-hakukohde [hakukohde-oids]
  (filter (fn [[hakukohde-oid]]
            (some #{hakukohde-oid} hakukohde-oids))))
