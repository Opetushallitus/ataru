(ns ataru.application.filtering-test
  (:require [ataru.application.filtering :as filtering])
  (:require-macros [cljs.test :refer [deftest is testing]]))

;; Henkilöllä on kaksi hakemusta samaan hakukohteeseen. Vastaanotto on
;; tallennettu henkilölle ja hakukohteelle, joten valinta-tulos-service
;; palauttaa saman vastaanoton tilan molemmille hakemuksille. Vain hyväksytty
;; hakemus on voinut tuottaa vastaanoton, hylätyn oma vastaanotto on KESKEN.
(def ^:private valinta-tulos-service
  {"hyvaksytty-hakemus"
   {"hakukohde-1" {:valinnantulos {:valinnantila    "HYVAKSYTTY"
                                   :vastaanottotila "VASTAANOTTANUT_SITOVASTI"}}}
   "hylatty-hakemus"
   {"hakukohde-1" {:valinnantulos {:valinnantila    "HYLATTY"
                                   :vastaanottotila "VASTAANOTTANUT_SITOVASTI"}}}})

(defn- db-with-filter [states]
  {:application           {:kevyt-valinta-vastaanotto-state-filter states}
   :valinta-tulos-service valinta-tulos-service})

(deftest test-filter-by-kevyt-valinta-vastaanotto-state
  (testing "vastaanoton tilalla suodatus osuu hakemukseen, jota vastaanotto koskee"
    (is (true? (filtering/filter-by-kevyt-valinta-vastaanotto-state
                 (db-with-filter ["VASTAANOTTANUT_SITOVASTI"])
                 "hyvaksytty-hakemus"
                 ["hakukohde-1"]))))

  (testing "hylätty hakemus ei osu vastaanoton tilan suodattimeen"
    (is (false? (filtering/filter-by-kevyt-valinta-vastaanotto-state
                  (db-with-filter ["VASTAANOTTANUT_SITOVASTI"])
                  "hylatty-hakemus"
                  ["hakukohde-1"]))))

  (testing "hylätty hakemus osuu KESKEN-suodattimeen, koska sillä ei ole omaa vastaanottoa"
    (is (true? (filtering/filter-by-kevyt-valinta-vastaanotto-state
                 (db-with-filter ["KESKEN"])
                 "hylatty-hakemus"
                 ["hakukohde-1"])))))

(deftest test-add-kevyt-valinta-vastaanotto-state-counts
  (testing "vastaanotto lasketaan vain hakemukselle, jota se koskee, muut ovat KESKEN"
    (is (= {"VASTAANOTTANUT_SITOVASTI" 1
            "KESKEN"                   1}
           (filtering/add-kevyt-valinta-vastaanotto-state-counts
             {}
             (db-with-filter [])
             [{:key "hyvaksytty-hakemus" :hakukohde ["hakukohde-1"]}
              {:key "hylatty-hakemus" :hakukohde ["hakukohde-1"]}]
             #{"hakukohde-1"})))))
