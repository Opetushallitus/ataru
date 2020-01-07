(ns ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-rights)

(defn kevyt-valinta-read-only-rights-for-hakukohteet? [hakukohde-oids rights-by-hakukohde]
  (->> hakukohde-oids
       (map #(get rights-by-hakukohde %))
       (every? (partial some #{:view-valinta :edit-valinta}))))

(defn kevyt-valinta-write-rights-for-hakukohteet? [hakukohde-oids rights-by-hakukohde]
  (->> hakukohde-oids
       (map #(get rights-by-hakukohde %))
       (every? (partial some #{:edit-valinta}))))

