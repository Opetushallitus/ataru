(ns ataru.application.filtering-test
  (:require [ataru.application.filtering :as filtering])
  (:require-macros [cljs.test :refer [deftest is testing]]))

;; Henkilöllä on kaksi hakemusta samaan hakukohteeseen. Vastaanotto on
;; tallennettu henkilölle ja hakukohteelle, joten valinta-tulos-service
;; palauttaa saman vastaanoton tilan molemmille hakemuksille. Vain hyväksytty
;; hakemus on voinut tuottaa vastaanoton.
(def ^:private db
  {:application           {:kevyt-valinta-vastaanotto-state-filter ["VASTAANOTTANUT_SITOVASTI"]}
   :valinta-tulos-service {"hyvaksytty-hakemus"
                           {"hakukohde-1" {:valinnantulos {:valinnantila    "HYVAKSYTTY"
                                                           :vastaanottotila "VASTAANOTTANUT_SITOVASTI"}}}
                           "hylatty-hakemus"
                           {"hakukohde-1" {:valinnantulos {:valinnantila    "HYLATTY"
                                                           :vastaanottotila "VASTAANOTTANUT_SITOVASTI"}}}}})

(deftest test-filter-by-kevyt-valinta-vastaanotto-state
  (testing "vastaanoton tilalla suodatus osuu hakemukseen, jota vastaanotto koskee"
    (is (true? (filtering/filter-by-kevyt-valinta-vastaanotto-state
                 db
                 "hyvaksytty-hakemus"
                 ["hakukohde-1"]))))

  (testing "hylätty hakemus ei osu vastaanoton tilan suodattimeen"
    (is (false? (filtering/filter-by-kevyt-valinta-vastaanotto-state
                  db
                  "hylatty-hakemus"
                  ["hakukohde-1"])))))

(deftest test-add-kevyt-valinta-vastaanotto-state-counts
  (testing "vastaanotto lasketaan vain hakemukselle, jota se koskee"
    (is (= {"VASTAANOTTANUT_SITOVASTI" 1}
           (filtering/add-kevyt-valinta-vastaanotto-state-counts
             {}
             db
             [{:key "hyvaksytty-hakemus" :hakukohde ["hakukohde-1"]}
              {:key "hylatty-hakemus" :hakukohde ["hakukohde-1"]}]
             #{"hakukohde-1"})))))
