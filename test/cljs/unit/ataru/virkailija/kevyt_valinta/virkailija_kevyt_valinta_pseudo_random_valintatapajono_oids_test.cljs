(ns ataru.virkailija.kevyt-valinta.virkailija-kevyt-valinta-pseudo-random-valintatapajono-oids-test
  (:require [ataru.virkailija.kevyt-valinta.virkailija-kevyt-valinta-pseudo-random-valintatapajono-oids :as h])
  (:require-macros [cljs.test :refer [deftest is]]))

(deftest generates-valintatapajono-oid-from-haku-oid-and-hakukohde-oid
  (let [haku-oid      "1.2.246.562.29.89284287409"
        hakukohde-oid "1.2.246.562.20.22952820417"
        expected      "12246562202295282041790478248298"
        actual        (h/pseudo-random-valintatapajono-oid haku-oid hakukohde-oid)]
    (is (= actual expected))))
