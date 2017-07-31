(ns ataru.tarjonta-service.mock-tarjonta-service
  (:require [com.stuartsierra.component :as component]
            [ataru.tarjonta-service.tarjonta-protocol :refer [TarjontaService]]))

(def base-haku
  {:tila                                                 "LUONNOS",
   :ataruLomakeAvain                                     "41101b4f-1762-49af-9db0-e3603adae3ad",
   :maksumuuriKaytossa                                   false,
   :korkeakouluHaku                                      false,
   :tarjoajaOids                                         ["1.2.246.562.10.73539475928"],
   :koulutuksenAlkamisVuosi                              2016,
   :sisaltyvatHaut                                       [],
   :hakutapaUri                                          "hakutapa_02#1",
   :tunnistusKaytossa                                    false,
   :modified                                             1480330233777,
   :autosyncTarjonta                                     false,
   :nimi                                                 {:kieli_fi "testing2"},
   :hakutyyppiUri                                        "hakutyyppi_01#1",
   :usePriority                                          false,
   :oid                                                  "1.2.246.562.29.65950024185",
   :hakukohdeOids                                        ["1.2.246.562.20.49028196522"],
   :organisaatioryhmat                                   [],
   :organisaatioOids                                     ["1.2.246.562.10.73539475928"],
   :ylioppilastutkintoAntaaHakukelpoisuuden              false,
   :jarjestelmanHakulomake                               false,
   :yhdenPaikanSaanto                                    {:voimassa false, :syy "Ei korkeakouluhaku"},
   :hakukohdeOidsYlioppilastutkintoAntaaHakukelpoisuuden [],
   :hakukausiUri                                         "kausi_s#1",
   :kohdejoukkoUri                                       "haunkohdejoukko_10#1",
   :maxHakukohdes                                        0,
   :modifiedBy                                           "1.2.246.562.24.70906349358",
   :koulutuksenAlkamiskausiUri                           "kausi_s#1",
   :hakukausiVuosi                                       2016,
   :hakuaikas                                            [{:hakuaikaId "10291885",
                                                           :alkuPvm    (- (System/currentTimeMillis)
                                                                          86400000),
                                                           :loppuPvm   (+ (System/currentTimeMillis)
                                                                          86400000),
                                                           :nimet      {:kieli_sv "", :kieli_fi "", :kieli_en ""}}],
   :sijoittelu                                           false})

(def base-hakukohde
  {:tila                                    "LUONNOS",
   :ataruLomakeAvain                        "41101b4f-1762-49af-9db0-e3603adae3ad",
   :ryhmaliitokset                          [],
   :overridesHaunHakulomakeUrl              false,
   :valintojenAloituspaikatLkm              0,
   :tutkintoonJohtava                       false,
   :soraKuvausKielet                        [],
   :tarjoajaOids                            ["1.2.246.562.10.10826252479"],
   :koulutukset                             [{:oid "1.2.246.562.17.74335799461"}],
   :hakukelpoisuusVaatimusKuvaukset         {},
   :josYoEiMuitaLiitepyyntoja               false,
   :kaytetaanJarjestelmanValintaPalvelua    true,
   :koulutuslaji                            "A",
   :hakukelpoisuusvaatimusUris              [],
   :opintoOikeusUris                        [],
   :hakuaikaId                              10291885,
   :hakukohteenLiitteet                     [],
   :modified                                1480330275077,
   :alinValintaPistemaara                   0,
   :hakukohdeKoulutusOids                   ["1.2.246.562.17.74335799461"],
   :aloituspaikatLkm                        0,
   :lisatiedot                              {:kieli_sv "", :kieli_fi "", :kieli_en ""},
   :hakuMenettelyKuvaukset                  {},
   :hakukohteenNimiUri                      "aikuhakukohteet_4136#4",
   :yhteystiedot                            [],
   :oid                                     "1.2.246.562.20.49028196522",
   :opetusKielet                            ["kieli_fi"],
   :kaytetaanHaunPaattymisenAikaa           false,
   :pohjakoulutusliitteet                   [],
   :ylioppilastutkintoAntaaHakukelpoisuuden false,
   :liitteidenToimitusOsoite                {:version 0, :osoiterivi1 "Koulukatu 41", :postinumero "posti_60100", :postinumeroArvo 60100, :postitoimipaikka "SEINÄJOKI"},
   :hakuOid                                 "1.2.246.562.29.65950024185",
   :valintaPerusteKuvausKielet              [],
   :koulutusmoduuliTyyppi                   "TUTKINTO",
   :edellisenVuodenHakijatLkm               0,
   :toteutusTyyppi                          "AMMATTITUTKINTO",
   :koulutusAsteTyyppi                      "AMMATTITUTKINTO",
   :yhdenPaikanSaanto                       {:voimassa false, :syy "Ei korkeakouluhaku ja hakukohde ei kuulu jatkuvaan korkeakouluhakuun, jonka kohdejoukon tarkenne kuuluu joukkoon [haunkohdejoukontarkenne_3#] tai sitä ei ole"},
   :organisaatioRyhmaOids                   [],
   :ylinValintapistemaara                   0,
   :koulutusmoduuliToteutusTarjoajatiedot   {:1.2.246.562.17.74335799461 {:tarjoajaOids ["1.2.246.562.10.10826252479"]}},
   :alinHyvaksyttavaKeskiarvo               0.0,
   :kaytetaanHakukohdekohtaistaHakuaikaa    false,
   :peruutusEhdotKuvaukset                  {},
   :version                                 1,
   :kaksoisTutkinto                         false,
   :painotettavatOppiaineet                 [],
   :hakukohteenNimet                        {:kieli_fi "Ajoneuvonosturinkuljettajan ammattitutkinto", :kieli_sv "Yrkesexamen för fordonskranförare"},
   :modifiedBy                              "1.2.246.562.24.70906349358",
   :tarjoajaNimet                           {:fi "Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie"},
   :valintakokeet                           []})

(def haku
  {:1.2.246.562.29.65950024185 base-haku
   :haku.oid                   (merge
                                 base-haku
                                 {:hakukohdeOids ["hakukohde_oid"]})
   :1.2.246.562.29.65950024186 (merge
                                 base-haku
                                 {:oid              "1.2.246.562.29.65950024186"
                                  :ataruLomakeAvain "41101b4f-1762-49af-9db0-e3603adae3ae"
                                  :hakukohdeOids    ["1.2.246.562.20.49028196523"
                                                     "1.2.246.562.20.49028196524"
                                                     "1.2.246.562.20.49028196525"]})})

(def hakukohde
  {:1.2.246.562.20.49028196522 base-hakukohde
   :hakukohde.oid              base-hakukohde
   :hakukohde_oid              base-hakukohde
   :1.2.246.562.20.49028196523 (merge
                                 base-hakukohde
                                 {:ataruLomakeAvain "41101b4f-1762-49af-9db0-e3603adae3ae"
                                  :oid              "1.2.246.562.20.49028196523"
                                  :hakuOid          "1.2.246.562.29.65950024186"
                                  :koulutukset      [{:oid "1.2.246.562.17.74335799462"}]
                                  :hakukohteenNimet
                                                    {:kieli_fi "Testihakukohde 1"
                                                     :kieli_sv "sv Testihakukohde 1"}})
   :1.2.246.562.20.49028196524 (merge
                                 base-hakukohde
                                 {:ataruLomakeAvain "41101b4f-1762-49af-9db0-e3603adae3ae"
                                  :oid              "1.2.246.562.20.49028196524"
                                  :hakuOid          "1.2.246.562.29.65950024186"
                                  :koulutukset      [{:oid "1.2.246.562.17.74335799463"}]
                                  :hakukohteenNimet
                                                    {:kieli_fi "Testihakukohde 2"
                                                     :kieli_sv "sv Testihakukohde 2"}})

   :1.2.246.562.20.49028196525 (merge
                                 base-hakukohde
                                 {:ataruLomakeAvain "41101b4f-1762-49af-9db0-e3603adae3ae"
                                  :oid              "1.2.246.562.20.49028196525"
                                  :hakuOid          "1.2.246.562.29.65950024186"
                                  :koulutukset      [{:oid "1.2.246.562.17.74335799464"}]
                                  :hakukohteenNimet
                                                    {:kieli_fi "Testihakukohde 3"
                                                     :kieli_sv "sv Testihakukohde 3"}})})

(def koulutus
  {:1.2.246.562.17.74335799461 {:oid             "1.2.246.562.17.74335799461"
                                :koulutuskoodi   {:nimi "Koulutuskoodi"}
                                :tutkintonimike  {:nimi "Tutkintonimike"}
                                :koulutusohjelma {:nimi "Koulutusohjelma"}
                                :tarkenne        "Tarkenne"}
   :1.2.246.562.17.74335799462 {:oid             "1.2.246.562.17.74335799462"
                                :koulutuskoodi   {:nimi "Koulutuskoodi A"}
                                :tutkintonimike  {:nimi "Tutkintonimike A"}
                                :koulutusohjelma {:nimi "Koulutusohjelma A"}
                                :tarkenne        "Tarkenne A"}
   :1.2.246.562.17.74335799463 {:oid             "1.2.246.562.17.74335799463"
                                :koulutuskoodi   {:nimi "Koulutuskoodi B"}
                                :tutkintonimike  {:nimi "Tutkintonimike B"}
                                :koulutusohjelma {:nimi "Koulutusohjelma B"}
                                :tarkenne        "Tarkenne B"}
   :1.2.246.562.17.74335799464 {:oid             "1.2.246.562.17.74335799464"
                                :koulutuskoodi   {:nimi "Koulutuskoodi C"}
                                :tutkintonimike  {:nimi "Tutkintonimike C"}
                                :koulutusohjelma {:nimi "Koulutusohjelma C"}
                                :tarkenne        "Tarkenne C"}})


(defrecord MockTarjontaService []
  component/Lifecycle
  TarjontaService

  (start [this] this)
  (stop [this] this)

  (get-hakukohde [this hakukohde-oid]
    ((keyword hakukohde-oid) hakukohde))

  (get-hakukohde-name [this hakukohde-oid]
    (when (= hakukohde-oid "hakukohde.oid")
      "Ajoneuvonosturinkuljettajan ammattitutkinto"))

  (get-haku [this haku-oid]
    ((keyword haku-oid) haku))

  (get-haku-name [this haku-oid]
    (when (= haku-oid "1.2.246.562.29.65950024185")
      "testing2"))

  (get-koulutus [this koulutus-id]
    ((keyword koulutus-id) koulutus)))
