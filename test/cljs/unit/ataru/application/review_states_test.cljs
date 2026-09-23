(ns ataru.application.review-states-test
  (:require [ataru.application.review-states :as review-states])
  (:require-macros [cljs.test :refer [deftest are]]))

;; Vastaanotto tallennetaan valinta-tulos-servicessä henkilön ja hakukohteen
;; perusteella, joten henkilön kaikki saman hakukohteen hakemukset saavat saman
;; vastaanoton tilan. Valinnan tila on hakemuskohtainen ja kertoo, voiko
;; vastaanotto koskea tätä hakemusta.
(deftest test-vastaanotto-koskee-tata-hakemusta?
  (are [valinnantulos expected]
       (= (review-states/vastaanotto-koskee-tata-hakemusta? valinnantulos)
          expected)

       ;; Vastaanoton tilaa ei ole tai se on KESKEN, mitään ei voi vuotaa
       nil                                                                      true
       {}                                                                       true
       {:valinnantila "HYLATTY"}                                                true
       {:valinnantila "HYLATTY" :vastaanottotila "KESKEN"}                      true

       ;; Hyväksytyllä mikä tahansa vastaanoton tila on mahdollinen
       {:valinnantila "HYVAKSYTTY" :vastaanottotila "VASTAANOTTANUT_SITOVASTI"} true
       {:valinnantila "VARASIJALTA_HYVAKSYTTY"
        :vastaanottotila "EHDOLLISESTI_VASTAANOTTANUT"}                         true
       {:valinnantila "HYVAKSYTTY" :vastaanottotila "PERUNUT"}                  true

       ;; Peruneilla vain oma tilansa, myöhästyminen tai KESKEN
       {:valinnantila "PERUNUT" :vastaanottotila "PERUNUT"}                     true
       {:valinnantila "PERUUTETTU" :vastaanottotila "PERUUTETTU"}               true
       {:valinnantila "PERUNUT"
        :vastaanottotila "EI_VASTAANOTETTU_MAARA_AIKANA"}                       true
       {:valinnantila "PERUUNTUNUT"
        :vastaanottotila "EI_VASTAANOTETTU_MAARA_AIKANA"}                       true
       {:valinnantila "PERUUNTUNUT"
        :vastaanottotila "OTTANUT_VASTAAN_TOISEN_PAIKAN"}                       true

       ;; KESKEN sallii myöhästymisen, muttei vastaanottoa
       {:valinnantila "KESKEN"
        :vastaanottotila "EI_VASTAANOTETTU_MAARA_AIKANA"}                       true

       ;; Valinnan tila ei voi kantaa tätä vastaanottoa: tieto kuuluu henkilön
       ;; toiselle saman hakukohteen hakemukselle
       {:valinnantila "HYLATTY" :vastaanottotila "VASTAANOTTANUT_SITOVASTI"}    false
       {:valinnantila "VARALLA" :vastaanottotila "VASTAANOTTANUT_SITOVASTI"}    false
       {:valinnantila "KESKEN" :vastaanottotila "VASTAANOTTANUT_SITOVASTI"}     false
       {:vastaanottotila "VASTAANOTTANUT_SITOVASTI"}                            false
       {:valinnantila "HYLATTY"
        :vastaanottotila "EI_VASTAANOTETTU_MAARA_AIKANA"}                       false

       ;; Peruneen valinnan tilan kanssa käy vain oma vastaanoton tila
       {:valinnantila "PERUUNTUNUT"
        :vastaanottotila "VASTAANOTTANUT_SITOVASTI"}                            false
       {:valinnantila "PERUUNTUNUT" :vastaanottotila "PERUNUT"}                 false
       {:valinnantila "PERUNUT" :vastaanottotila "PERUUTETTU"}                  false
       {:valinnantila "PERUUTETTU" :vastaanottotila "PERUNUT"}                  false))

;; Ilmoittautuminen tallennetaan samoin henkilön ja hakukohteen perusteella.
;; Sallittu joukko on tiukempi kuin vastaanotolla: valinta-tulos-service
;; hyväksyy ilmoittautumisen vain hyväksytylle ja vastaanottaneelle hakijalle.
(deftest test-ilmoittautuminen-koskee-tata-hakemusta?
  (are [valinnantulos expected]
       (= (review-states/ilmoittautuminen-koskee-tata-hakemusta? valinnantulos)
          expected)

       ;; Ilmoittautumisen tilaa ei ole tai se on EI_TEHTY, mitään ei voi vuotaa
       nil                                                                      true
       {}                                                                       true
       {:valinnantila "HYLATTY" :ilmoittautumistila "EI_TEHTY"}                 true

       ;; Hyväksytty ja vastaanottanut
       {:valinnantila       "HYVAKSYTTY"
        :vastaanottotila    "VASTAANOTTANUT_SITOVASTI"
        :ilmoittautumistila "LASNA_KOKO_LUKUVUOSI"}                             true
       {:valinnantila       "VARASIJALTA_HYVAKSYTTY"
        :vastaanottotila    "VASTAANOTTANUT_SITOVASTI"
        :ilmoittautumistila "LASNA_SYKSY"}                                      true

       ;; Hyväksytty mutta vastaanottoa ei ole tehty: ilmoittautuminen ei voi
       ;; olla tämän hakemuksen tieto
       {:valinnantila       "HYVAKSYTTY"
        :vastaanottotila    "KESKEN"
        :ilmoittautumistila "LASNA_KOKO_LUKUVUOSI"}                             false
       {:valinnantila       "HYVAKSYTTY"
        :ilmoittautumistila "LASNA_KOKO_LUKUVUOSI"}                             false

       ;; Valinnan tila ei kanna ilmoittautumista lainkaan
       {:valinnantila       "HYLATTY"
        :vastaanottotila    "VASTAANOTTANUT_SITOVASTI"
        :ilmoittautumistila "LASNA_KOKO_LUKUVUOSI"}                             false
       {:valinnantila       "VARALLA"
        :vastaanottotila    "VASTAANOTTANUT_SITOVASTI"
        :ilmoittautumistila "LASNA_KOKO_LUKUVUOSI"}                             false
       {:valinnantila       "PERUNUT"
        :vastaanottotila    "PERUNUT"
        :ilmoittautumistila "LASNA_KOKO_LUKUVUOSI"}                             false))

(deftest test-henkilotason-tiedot-koskevat-tata-hakemusta?
  (are [valinnantulos expected]
       (= (review-states/henkilotason-tiedot-koskevat-tata-hakemusta? valinnantulos)
          expected)

       nil                                                                      true
       {:valinnantila "HYLATTY" :vastaanottotila "KESKEN"}                      true
       {:valinnantila       "HYVAKSYTTY"
        :vastaanottotila    "VASTAANOTTANUT_SITOVASTI"
        :ilmoittautumistila "LASNA_KOKO_LUKUVUOSI"}                             true

       ;; Vastaanotto vuotaa
       {:valinnantila "HYLATTY" :vastaanottotila "VASTAANOTTANUT_SITOVASTI"}    false

       ;; Vain ilmoittautuminen vuotaa
       {:valinnantila       "HYVAKSYTTY"
        :vastaanottotila    "KESKEN"
        :ilmoittautumistila "LASNA_KOKO_LUKUVUOSI"}                             false))
