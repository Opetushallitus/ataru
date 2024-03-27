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
