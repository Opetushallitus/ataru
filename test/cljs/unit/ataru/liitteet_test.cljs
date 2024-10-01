(ns ataru.liitteet-test
  (:require [ataru.hakukohde.liitteet :as liitteet]
            [cljs.test :refer-macros [deftest is testing]]
            [clojure.string :as string]))

(def deadline-1
  {:fi "31.3.2022 klo 00:00",
   :sv "31.3.2022 kl. 00:00 EEST",
   :en "Mar. 31, 2022 at 12:00 AM EEST"})

(def liite-1
  {:tyyppi               "liitetyypitamm_6#1",
   :toimitusaika         deadline-1,
   :toimitetaan-erikseen true,
   :toimitusosoite       {:osoite      {:fi "Hiushalkojantie 4"},
                          :postinumero {:fi {:koodiUri "posti_00100#2", :nimi "HELSINKI"}
                                        :sv {:koodiUri "posti_00100#2", :nimi "HELSINGFORS"}},
                          :verkkosivu  "https://liite-hius.fi"}})

(def deadline-2
  {:fi "28.2.2022 klo 00:00",
   :sv "28.2.2022 kl. 00:00 EET",
   :en "Feb. 28, 2022 at 12:00 AM EET"})

(def liite-2
  {:tyyppi               "liitetyypitamm_7#1",
   :toimitusaika         deadline-2,
   :toimitetaan-erikseen true,
   :toimitusosoite       {:osoite      {:fi "Hiuskatu 2"},
                          :postinumero {:fi {:koodiUri "posti_00500#2", :nimi "HELSINKI"}
                                        :sv {:koodiUri "posti_00500#2", :nimi "HELSINGFORS"}},
                          :verkkosivu  "https://tupee-liitteena.fi"}})

(def liite-with-swedish
  {:tyyppi               "liitetyypitamm_7#1",
   :toimitusaika         {:fi "28.2.2022 klo 00:00",
                          :sv "28.2.2022 kl. 00:00 EET",
                          :en "Feb. 28, 2022 at 12:00 AM EET"},
   :toimitetaan-erikseen true,
   :toimitusosoite       {:osoite      {:fi "Hiuskatu 2"
                                        :sv "Hårgatan 2"},
                          :postinumero {:fi {:koodiUri "posti_00500#2", :nimi "HELSINKI"}
                                        :sv {:koodiUri "posti_00500#2", :nimi "HELSINGFORS"}},
                          :verkkosivu  "https://tupee-liitteena.fi"}})

(def hakukohde-with-common-attachment-address
  {:liitteet-onko-sama-toimitusosoite? true
   :liitteiden-toimitusosoite          {:osoite      {:fi "Toimisto\nElintie 5"},
                                        :postinumero {:fi {:koodiUri "posti_00100#2", :nimi "HELSINKI"}
                                                      :sv {:koodiUri "posti_00100#2", :nimi "HELSINGFORS"}},
                                        :verkkosivu  "https://elintie-liite.fi"}
   :liitteet                           [liite-1 liite-2]})

(def common-deadline
  {:fi "31.1.2022 klo 12:00",
   :sv "31.1.2022 kl. 12:00 EET",
   :en "Jan. 31, 2022 at 12:00 PM EET"})

(def hakukohde-with-common-attachment-deadline
  {:liitteet-onko-sama-toimitusaika? true
   :liitteiden-toimitusaika          common-deadline
   :liitteet                         [liite-1 liite-2]})

(deftest attachment-for-hakukohde-test
  (let [hakukohde {:liitteet [liite-1 liite-2]}]
    (is (nil? (liitteet/attachment-for-hakukohde "liitetyypitamm_8" hakukohde)))
    (is (= liite-2 (liitteet/attachment-for-hakukohde "liitetyypitamm_7" hakukohde)))))

(deftest attachment-address-test
  (testing "address from a single attachment"
    (let [hakukohde {:litteet [liite-1 liite-2]}
          address   (liitteet/attachment-address :fi liite-2 hakukohde)]
      (is (not (nil? address)))
      (is (string/includes? address "Hiuskatu 2"))
      (is (string/includes? address "00500 HELSINKI"))
      (is (string/includes? address "https://tupee-liitteena.fi"))))

  (testing "translation"
    (testing "uses :sv translation when given :sv"
      (let [hakukohde {:litteet [liite-1 liite-with-swedish]}
            address   (liitteet/attachment-address :sv liite-with-swedish hakukohde)]
        (is (string/includes? address "Hårgatan 2"))
        (is (string/includes? address "00500 HELSINGFORS"))))

    (testing "uses :fi translation when given :en"
      (let [hakukohde {:litteet [liite-1 liite-2]}
            address   (liitteet/attachment-address :en liite-2 hakukohde)]
        (is (string/includes? address "00500 HELSINKI")))))

  (testing "address from common attachment address"
    (let [address (liitteet/attachment-address :fi liite-1 hakukohde-with-common-attachment-address)]
      (is (not (nil? address)))
      (is (string/includes? address "Toimisto"))
      (is (string/includes? address "Elintie 5"))
      (is (string/includes? address "00100 HELSINKI"))
      (is (string/includes? address "https://elintie-liite.fi")))))

(deftest attachment-deadline-test
  (testing "deadline from single attachment"
    (let [hakukohde {:litteet [liite-1 liite-2]}
          deadline  (liitteet/attachment-deadline :fi liite-2 hakukohde)]
      (is (string/includes? "28.2.2022 klo 00:00" deadline))))

  (testing "deadline from common attachment deadline"
    (let [deadline (liitteet/attachment-deadline :fi liite-2 hakukohde-with-common-attachment-deadline)]
      (is (string/includes? "31.1.2022 klo 12:00" deadline)))))
