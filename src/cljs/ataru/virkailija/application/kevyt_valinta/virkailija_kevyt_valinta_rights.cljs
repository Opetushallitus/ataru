(ns ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-rights)

(defn kevyt-valinta-rights-for-hakukohteet? [hakukohde-oids rights-by-hakukohde]
  (->> hakukohde-oids
       (map #(get rights-by-hakukohde %))
       (every? #(or (contains? % :view-valinta)
                    (contains? % :edit-valinta)))))
