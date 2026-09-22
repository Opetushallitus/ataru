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
