(ns ataru.virkailija.kevyt-valinta.virkailija-kevyt-valinta-pseudo-random-valintatapajono-oids
  (:require [clojure.string :as string]))

(def ^:private dot-matcher #"\.")

(defn- remove-commas [str]
  (string/replace str dot-matcher ""))

(defn pseudo-random-valintatapajono-oid [haku-oid hakukohde-oid]
  (let [haku-oid-formatted      (-> haku-oid
                                    (string/reverse)
                                    (remove-commas))
        hakukohde-oid-formatted (remove-commas hakukohde-oid)]
    (subs (str hakukohde-oid-formatted haku-oid-formatted)
          0
          32)))
