(ns ataru.virkailija.application.hyvaksynnan-ehto.hyvaksynnan-ehto-xforms)

(defn filter-hyvaksynnan-ehdot-for-correct-hakukohde [hakukohde-oids]
  (filter (fn [[hakukohde-oid]]
            (some #{hakukohde-oid} hakukohde-oids))))

(defn filter-hyvaksynnan-ehdot-for-hakukohteet []
  (filter (fn [[_ hyvaksynnan-ehto]]
            (-> hyvaksynnan-ehto :hakukohteessa not-empty))))

(defn map->hyvaksynnan-ehto-with-hakukohde-oid [hyvaksynnan-ehto-map-fn]
  (map (fn [[hakukohde-oid hyvaksynnan-ehto]]
         (assoc (hyvaksynnan-ehto-map-fn hyvaksynnan-ehto)
                :hakukohde-oid
                hakukohde-oid))))
