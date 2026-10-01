(ns ataru.virkailija.kevyt-valinta.virkailija-kevyt-valinta-subs-test
  (:require [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-subs :as k])
  (:require-macros [cljs.test :refer [deftest are]]))

(deftest test-match-kevyt-valinta-states
  (are [valinnan-tulos-for-application kevyt-valinta-write-rights? expected-result]
       (= (k/match-kevytvalinta-states valinnan-tulos-for-application kevyt-valinta-write-rights?)
          expected-result)

       {} false                                                {:kevyt-valinta/valinnan-tila         :checked
                                                                :kevyt-valinta/julkaisun-tila        :checked
                                                                :kevyt-valinta/vastaanotto-tila      :checked
                                                                :kevyt-valinta/ilmoittautumisen-tila :checked}

       {:vastaanottotila "OTTANUT_VASTAAN_TOISEN_PAIKAN"} true {:kevyt-valinta/valinnan-tila         :checked
                                                                :kevyt-valinta/julkaisun-tila        :checked
                                                                :kevyt-valinta/vastaanotto-tila      :checked
                                                                :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

       {:valinnantila "HYLATTY"
        :julkaistavissa true} true                             {:kevyt-valinta/valinnan-tila         :checked
                                                                :kevyt-valinta/julkaisun-tila        :unchecked
                                                                :kevyt-valinta/vastaanotto-tila      :grayed-out
                                                                :kevyt-valinta/ilmoittautumisen-tila :grayed-out}
       {:valinnantila "VARALLA"
        :julkaistavissa true} true                             {:kevyt-valinta/valinnan-tila         :checked
                                                                :kevyt-valinta/julkaisun-tila        :unchecked
                                                                :kevyt-valinta/vastaanotto-tila      :grayed-out
                                                                :kevyt-valinta/ilmoittautumisen-tila :grayed-out}
       {:valinnantila "PERUUNTUNUT"
        :julkaistavissa true} true                             {:kevyt-valinta/valinnan-tila         :checked
                                                                :kevyt-valinta/julkaisun-tila        :unchecked
                                                                :kevyt-valinta/vastaanotto-tila      :grayed-out
                                                                :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

       {} true                                                 {:kevyt-valinta/valinnan-tila         :unchecked
                                                                :kevyt-valinta/julkaisun-tila        :grayed-out
                                                                :kevyt-valinta/vastaanotto-tila      :grayed-out
                                                                :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

       {} nil                                                  {:kevyt-valinta/valinnan-tila         :unchecked
                                                                :kevyt-valinta/julkaisun-tila        :grayed-out
                                                                :kevyt-valinta/vastaanotto-tila      :grayed-out
                                                                :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

       nil nil                                                 {:kevyt-valinta/valinnan-tila         :unchecked
                                                                :kevyt-valinta/julkaisun-tila        :grayed-out
                                                                :kevyt-valinta/vastaanotto-tila      :grayed-out
                                                                :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

       {:julkaistavissa false} true                            {:kevyt-valinta/valinnan-tila         :unchecked
                                                                :kevyt-valinta/julkaisun-tila        :unchecked
                                                                :kevyt-valinta/vastaanotto-tila      :grayed-out
                                                                :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

       {:julkaistavissa true
        :vastaanottotila "KESKEN"} true                        {:kevyt-valinta/valinnan-tila         :checked
                                                                :kevyt-valinta/julkaisun-tila        :unchecked
                                                                :kevyt-valinta/vastaanotto-tila      :unchecked
                                                                :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

       {:valinnantila "HYVAKSYTTY"
        :julkaistavissa true
        :vastaanottotila "EI_VASTAANOTETTU_MAARA_AIKANA"
        :vastaanottoDeadlineMennyt true} true                  {:kevyt-valinta/valinnan-tila         :checked
                                                                :kevyt-valinta/julkaisun-tila        :checked
                                                                :kevyt-valinta/vastaanotto-tila      :checked
                                                                :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

       {:valinnantila "VARASIJALTA_HYVAKSYTTY"
        :julkaistavissa true
        :vastaanottotila "EI_VASTAANOTETTU_MAARA_AIKANA"
        :vastaanottoDeadlineMennyt true} true                  {:kevyt-valinta/valinnan-tila         :checked
                                                                :kevyt-valinta/julkaisun-tila        :checked
                                                                :kevyt-valinta/vastaanotto-tila      :checked
                                                                :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

       {:valinnantila "PERUNUT"
        :julkaistavissa true
        :vastaanottotila "EI_VASTAANOTETTU_MAARA_AIKANA"
        :vastaanottoDeadlineMennyt true} true                  {:kevyt-valinta/valinnan-tila         :checked
                                                                :kevyt-valinta/julkaisun-tila        :checked
                                                                :kevyt-valinta/vastaanotto-tila      :checked
                                                                :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

       {:julkaistavissa true
        :vastaanottotila "PERUNUT"} true                       {:kevyt-valinta/valinnan-tila         :checked
                                                                :kevyt-valinta/julkaisun-tila        :checked
                                                                :kevyt-valinta/vastaanotto-tila      :unchecked
                                                                :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

       {:julkaistavissa true
        :vastaanottotila "VASTAANOTTANUT_SITOVASTI"
        :ilmoittautumistila "EI_TEHTY"} true                   {:kevyt-valinta/valinnan-tila         :checked
                                                                :kevyt-valinta/julkaisun-tila        :checked
                                                                :kevyt-valinta/vastaanotto-tila      :unchecked
                                                                :kevyt-valinta/ilmoittautumisen-tila :unchecked}

       {:julkaistavissa true
        :vastaanottotila "VASTAANOTTANUT_SITOVASTI"
        :ilmoittautumistila "EI_ILMOITTAUTUNUT"} true          {:kevyt-valinta/valinnan-tila         :checked
                                                                :kevyt-valinta/julkaisun-tila        :checked
                                                                :kevyt-valinta/vastaanotto-tila      :checked
                                                                :kevyt-valinta/ilmoittautumisen-tila :unchecked}))

;; Vastaanotto ja ilmoittautuminen tallennetaan valinta-tulos-servicessä
;; henkilön ja hakukohteen perusteella, joten henkilön saman hakukohteen toisen
;; hakemuksen tiedot palautuvat myös tälle hakemukselle. Silloin hakemus
;; näytetään vain luettavana eikä vuotavaa tietoa näytetä lainkaan.
(deftest test-kevytvalinta-states-for-hakemus
  (are [valinnan-tulos kevyt-valinta-write-rights? expected-result]
       (= (k/kevytvalinta-states-for-hakemus valinnan-tulos kevyt-valinta-write-rights?)
          expected-result)

       ;; Ei vuotoa: sama tulos kuin match-kevytvalinta-statesilla
       {:valinnantila "HYLATTY"
        :julkaistavissa true} true                              (k/match-kevytvalinta-states
                                                                  {:valinnantila "HYLATTY"
                                                                   :julkaistavissa true}
                                                                  true)

       {:julkaistavissa true
        :vastaanottotila "KESKEN"} true                         (k/match-kevytvalinta-states
                                                                  {:julkaistavissa true
                                                                   :vastaanottotila "KESKEN"}
                                                                  true)

       ;; Vuotanut vastaanotto hylätyllä hakemuksella: piilotetaan ja koko
       ;; paneeli vain luettavaksi
       {:valinnantila    "HYLATTY"
        :julkaistavissa  true
        :vastaanottotila "VASTAANOTTANUT_SITOVASTI"} true       {:kevyt-valinta/valinnan-tila         :checked
                                                                 :kevyt-valinta/julkaisun-tila        :checked
                                                                 :kevyt-valinta/vastaanotto-tila      :grayed-out
                                                                 :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

       ;; Sama ilman muokkausoikeuksia: tämä on se polku joka vuotaa nykyisin,
       ;; koska match-kevytvalinta-states palauttaa kaikille :checked
       {:valinnantila    "HYLATTY"
        :julkaistavissa  true
        :vastaanottotila "VASTAANOTTANUT_SITOVASTI"} false      {:kevyt-valinta/valinnan-tila         :checked
                                                                 :kevyt-valinta/julkaisun-tila        :checked
                                                                 :kevyt-valinta/vastaanotto-tila      :grayed-out
                                                                 :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

       ;; Vuotanut ilmoittautuminen, vastaanotto kuuluu tälle hakemukselle
       {:valinnantila       "HYVAKSYTTY"
        :julkaistavissa     true
        :vastaanottotila    "KESKEN"
        :ilmoittautumistila "LASNA_KOKO_LUKUVUOSI"} true        {:kevyt-valinta/valinnan-tila         :checked
                                                                 :kevyt-valinta/julkaisun-tila        :checked
                                                                 :kevyt-valinta/vastaanotto-tila      :checked
                                                                 :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

       ;; julkaistavissa nil + vuotanut vastaanotto: ei mene matchiin lainkaan,
       ;; koska tiedot eivät koske tätä hakemusta
       {:valinnantila    "HYLATTY"
        :vastaanottotila "VASTAANOTTANUT_SITOVASTI"} true       {:kevyt-valinta/valinnan-tila         :checked
                                                                 :kevyt-valinta/julkaisun-tila        :grayed-out
                                                                 :kevyt-valinta/vastaanotto-tila      :grayed-out
                                                                 :kevyt-valinta/ilmoittautumisen-tila :grayed-out}))

;; julkaistavissa tulee valinta-tulos-servicestä LEFT JOINilla, joten se voi
;; olla nil vaikka vastaanoton tila ei ole. Tällainen yhdistelmä ei täsmää
;; yhteenkään ehtoon, ja ilman catch-allia core.match heittäisi. Tiedot
;; näytetään, mutta mitään ei tarjota muokattavaksi.
(deftest test-match-kevytvalinta-states-tuntematon-yhdistelma
  (are [valinnan-tulos expected-result]
       (= (k/match-kevytvalinta-states valinnan-tulos true)
          expected-result)

       ;; Valinnan tila on hakemuskohtaista tietoa eikä sitä piiloteta
       {:valinnantila    "HYVAKSYTTY"
        :vastaanottotila "VASTAANOTTANUT_SITOVASTI"}            {:kevyt-valinta/valinnan-tila         :checked
                                                                 :kevyt-valinta/julkaisun-tila        :grayed-out
                                                                 :kevyt-valinta/vastaanotto-tila      :checked
                                                                 :kevyt-valinta/ilmoittautumisen-tila :grayed-out}))
