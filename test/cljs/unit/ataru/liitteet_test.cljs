(ns ataru.liitteet-test
  (:require [ataru.hakukohde.liitteet :as liitteet]
            [cljs.test :refer-macros [deftest is]]))

(def liite-1
  {:tyyppi               "liitetyypitamm_6#1",
   :toimitusaika         {:fi "31.3.2022 klo 00:00",
                          :sv "31.3.2022 kl. 00:00 EEST",
                          :en "Mar. 31, 2022 at 12:00 AM EEST"},
   :toimitetaan-erikseen true,
   :toimitusosoite       {:osoite      {:fi "Hiushalkojantie 4"},
                          :postinumero {:koodiUri "posti_00100#2",
                                        :nimi     {:fi "HELSINKI", :sv "HELSINGFORS"}},
                          :verkkosivu  "https://liite-hius.fi"}})

(def liite-2
  {:tyyppi               "liitetyypitamm_7#1",
   :toimitusaika         {:fi "28.2.2022 klo 00:00",
                          :sv "28.2.2022 kl. 00:00 EET",
                          :en "Feb. 28, 2022 at 12:00 AM EET"},
   :toimitetaan-erikseen true,
   :toimitusosoite       {:osoite      {:fi "Hiuskatu 2"},
                          :postinumero {:koodiUri "posti_00500#2",
                                        :nimi     {:fi "HELSINKI", :sv "HELSINGFORS"}},
                          :verkkosivu  "https://tupee-liitteena.fi"}})

(deftest attachment-for-hakukohde-test
  (let [hakukohde {:liitteet [liite-1 liite-2]}]
    (is (nil? (liitteet/attachment-for-hakukohde "liitetyypitamm_8" hakukohde)))
    (is (= liite-2 (liitteet/attachment-for-hakukohde "liitetyypitamm_7" hakukohde)))))
