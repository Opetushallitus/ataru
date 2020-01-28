(ns ataru.tarjonta-service.mock-tarjonta-service
  (:require [com.stuartsierra.component :as component]
            [ataru.tarjonta-service.tarjonta-client :as tarjonta-client]
            [ataru.tarjonta-service.tarjonta-protocol :refer [TarjontaService VirkailijaTarjontaService]]))

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
   :sijoittelu                                           false
   :canSubmitMultipleApplications                        true})

(def base-hakukohde
  {:tila                                    "JULKAISTU",
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
   :hakuaikaId                              "10291885",
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
                                 {:oid "haku.oid"
                                  :hakukohdeOids ["hakukohde.oid"]
                                  :usePriority true})
   :1.2.246.562.29.65950024186 (merge
                                 base-haku
                                 {:oid              "1.2.246.562.29.65950024186"
                                  :usePriority      true
                                  :ataruLomakeAvain "41101b4f-1762-49af-9db0-e3603adae3ae"
                                  :hakukohdeOids    ["1.2.246.562.20.49028196523"
                                                     "1.2.246.562.20.49028196524"
                                                     "1.2.246.562.20.49028196525"
                                                     "1.2.246.562.20.49028196526"]})
  :1.2.246.562.29.65950024187 (merge
                                base-haku
                                {:oid              "1.2.246.562.29.65950024187"
                                 :nimi  {:kieli_fi "hakija-hakukohteen-hakuaika-haku"}
                                 :usePriority      true
                                 :ataruLomakeAvain "hakija-hakukohteen-hakuaika-test-form"
                                 :hakukohdeOids    ["1.2.246.562.20.49028100001"
                                                    "1.2.246.562.20.49028100002"
                                                    "1.2.246.562.20.49028100003"
                                                    "1.2.246.562.20.490281000035"]})
   :1.2.246.562.29.65950024188 (merge
                                 base-haku
                                 {:oid              "1.2.246.562.29.65950024188"
                                  :nimi  {:kieli_fi "hakukohteen-organisaatiosta"}
                                  :usePriority      true
                                  :ataruLomakeAvain "hakukohteen-organisaatiosta-form"
                                  :hakukohdeOids    ["1.2.246.562.20.49028100004"]})

   :1.2.246.562.29.65950024189 (merge
                                base-haku
                                {:oid              "1.2.246.562.29.65950024189"
                                 :usePriority      true
                                 :ataruLomakeAvain "41101b4f-1762-49af-9db0-e3603adae3ae"
                                 :hakukohdeOids    ["1.2.246.562.20.49028100005"]
                                 :hakuaikas        [{:hakuaikaId "10291885",
                                                     :alkuPvm    (- (System/currentTimeMillis)
                                                                    (* 2 86400000)),
                                                     :loppuPvm   (- (System/currentTimeMillis)
                                                                    86400000),
                                                     :nimet      {:kieli_sv ""
                                                                  :kieli_fi ""
                                                                  :kieli_en ""}}]})
   :1.2.246.562.29.65950024190 (merge
                                 base-haku
                                 {:oid              "1.2.246.562.29.65950024190"
                                  :usePriority      true
                                  :kohdejoukkoUri   "haunkohdejoukko_12#"})})

(def hakukohde
  {:1.2.246.562.20.49028196522 base-hakukohde
   :hakukohde.oid              (merge base-hakukohde
                                      {:oid "hakukohde.oid"
                                       :hakuOid "haku.oid"})
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
                                                     :kieli_sv "sv Testihakukohde 3"}})
   :1.2.246.562.20.49028100001 (merge
                                 base-hakukohde
                                 {:ataruLomakeAvain "hakija-hakukohteen-hakuaika-test-form"
                                  :oid              "1.2.246.562.20.49028100001"
                                  :hakuOid          "1.2.246.562.29.65950024187"
                                  :kaytetaanHakukohdekohtaistaHakuaikaa true
                                  :hakuaikaAlkuPvm  (- (System/currentTimeMillis)
                                                       86400000)
                                  :hakuaikaLoppuPvm (- (System/currentTimeMillis)
                                                       16400000)
                                  :koulutukset      [{:oid "1.2.246.562.17.74335799465"}]
                                  :ryhmaliitokset [{:ryhmaOid "1.2.246.562.28.00000000001"}]
                                  :hakukohteenNimet
                                                    {:kieli_fi "Aikaloppu 1"
                                                     :kieli_sv "sv Aikaloppu 1"}})
   :1.2.246.562.20.49028100002 (merge
                                 base-hakukohde
                                 {:ataruLomakeAvain "hakija-hakukohteen-hakuaika-test-form"
                                  :oid              "1.2.246.562.20.49028100002"
                                  :hakuOid          "1.2.246.562.29.65950024187"
                                  :kaytetaanHakukohdekohtaistaHakuaikaa true
                                  :hakuaikaAlkuPvm  (- (System/currentTimeMillis)
                                                       86400000)
                                  :hakuaikaLoppuPvm (+ (System/currentTimeMillis)
                                                       86400000)
                                  :koulutukset      [{:oid "1.2.246.562.17.74335799465"}]
                                  :hakukohteenNimet
                                                    {:kieli_fi "Aikaa jäljellä 2"
                                                     :kieli_sv "sv Aikaa jäljellä 2"}})
   :1.2.246.562.20.49028100003 (merge
                                 base-hakukohde
                                 {:ataruLomakeAvain "hakija-hakukohteen-hakuaika-test-form"
                                  :oid              "1.2.246.562.20.49028100003"
                                  :hakuOid          "1.2.246.562.29.65950024187"
                                  :koulutukset      [{:oid "1.2.246.562.17.74335799465"}]
                                  :ryhmaliitokset [{:ryhmaOid "1.2.246.562.28.00000000001"}
                                                   {:ryhmaOid "1.2.246.562.28.00000000002"}]
                                  :hakukohteenNimet
                                                    {:kieli_fi "Aikaa loputtomasti 3"
                                                     :kieli_sv "sv Aikaa loputtomasti 3"}})
   :1.2.246.562.20.490281000035 (merge
                                base-hakukohde
                                {:ataruLomakeAvain "hakija-hakukohteen-hakuaika-test-form"
                                 :oid              "1.2.246.562.20.490281000035"
                                 :hakuOid          "1.2.246.562.29.65950024187"
                                 :koulutukset      [{:oid "1.2.246.562.17.74335799465"}]
                                 :ryhmaliitokset [{:ryhmaOid "1.2.246.562.28.00000000001"}
                                                  {:ryhmaOid "1.2.246.562.28.00000000002"}]
                                 :hakukohteenNimet
                                                   {:kieli_fi "Aikaa loputtomasti 3.5"
                                                    :kieli_sv "sv Aikaa loputtomasti 3.5"}})
   :1.2.246.562.20.49028100004 (merge
                                 base-hakukohde
                                 {:ataruLomakeAvain "hakukohteen-organisaatiosta-form"
                                  :oid              "1.2.246.562.20.49028100004"
                                  :hakuOid          "1.2.246.562.29.65950024188"
                                  :koulutukset      [{:oid "1.2.246.562.17.74335799465"}]
                                  :hakukohteenNimet {:kieli_fi "Hakukohde johon käyttäjällä on organisaatio"
                                                     :kieli_sv "sv Hakukohde johon käyttäjällä on organisaatio"}})

   :1.2.246.562.20.49028100005 (merge
                                base-hakukohde
                                {:ataruLomakeAvain "41101b4f-1762-49af-9db0-e3603adae3ae"
                                 :oid              "1.2.246.562.20.49028100005"
                                 :hakuOid          "1.2.246.562.29.65950024189"
                                 :koulutukset      [{:oid "1.2.246.562.17.74335799464"}]
                                 :hakukohteenNimet
                                 {:kieli_fi "Testihakukohde"
                                  :kieli_sv "sv Testihakukohde"}})})

(def koulutus
  {:1.2.246.562.17.74335799461 {:oid                  "1.2.246.562.17.74335799461"
                                :koulutuskoodi-name   {:fi "Koulutuskoodi"}
                                :koulutusohjelma-name {:fi "Koulutusohjelma"}
                                :tutkintonimike-names [{:fi "Tutkintonimike"}]
                                :tarkenne             "Tarkenne"}
   :1.2.246.562.17.74335799462 {:oid                  "1.2.246.562.17.74335799462"
                                :koulutuskoodi-name   {:fi "Koulutuskoodi A"}
                                :koulutusohjelma-name {:fi "Koulutusohjelma A"}
                                :tutkintonimike-names [{:fi "Tutkintonimike A"}]
                                :tarkenne             "Tarkenne A"}
   :1.2.246.562.17.74335799463 {:oid                  "1.2.246.562.17.74335799463"
                                :koulutuskoodi-name   {:fi "Koulutuskoodi B"}
                                :koulutusohjelma-name {:fi "Koulutusohjelma B"}
                                :tutkintonimike-names [{:fi "Tutkintonimike B"}]
                                :tarkenne             "Tarkenne B"}
   :1.2.246.562.17.74335799464 {:oid                  "1.2.246.562.17.74335799464"
                                :koulutuskoodi-name   {:fi "Koulutuskoodi C"}
                                :koulutusohjelma-name {:fi "Koulutusohjelma C"}
                                :tutkintonimike-names [{:fi "Tutkintonimike C"}]
                                :tarkenne             "Tarkenne C"}
   :1.2.246.562.17.74335799465 {:oid                  "1.2.246.562.17.74335799465"
                                :koulutuskoodi-name   {:fi "Koulutuskoodi D"}
                                :koulutusohjelma-name {:fi "Koulutusohjelma D"}
                                :tutkintonimike-names [{:fi "Tutkintonimike D"}]
                                :tarkenne             "Tarkenne D"}})

(defrecord MockTarjontaService []
  component/Lifecycle
  TarjontaService

  (start [this] this)
  (stop [this] this)

  (get-hakukohde [this hakukohde-oid]
    (when-let [h ((keyword hakukohde-oid) hakukohde)]
      (tarjonta-client/parse-hakukohde h)))

  (get-hakukohteet [this hakukohde-oids]
    (keep #(.get-hakukohde this %) hakukohde-oids))

  (get-hakukohde-name [this hakukohde-oid]
    (if (= hakukohde-oid "hakukohde.oid")
      {:fi "Ajoneuvonosturinkuljettajan ammattitutkinto"}
      {:fi "Testihakukohde"}))

  (hakukohde-search [_ haku-oid _]
    (let [to-hakukohteet (fn [hakukohde-oids] (->> (map #(get hakukohde %) hakukohde-oids)
                                                   (map tarjonta-client/parse-hakukohde)
                                                   (map #(assoc % :user-organization? true))))]
         (case haku-oid
               "1.2.246.562.29.65950024187" (to-hakukohteet [:1.2.246.562.20.49028100001
                                                             :1.2.246.562.20.49028100002
                                                             :1.2.246.562.20.49028100003
                                                             :1.2.246.562.20.490281000035])
               "1.2.246.562.29.65950024188" (to-hakukohteet [:1.2.246.562.20.49028100004])
               (to-hakukohteet [:1.2.246.562.20.49028196523
                                :1.2.246.562.20.49028196524
                                :1.2.246.562.20.49028196525]))))

  (get-haku [this haku-oid]
    ((keyword haku-oid) haku))

  (get-haku-name [this haku-oid]
    (when (= haku-oid "1.2.246.562.29.65950024185")
      {:fi "testing2"}))

  (get-koulutus [this koulutus-id]
    ((keyword koulutus-id) koulutus))

  (get-koulutukset [this koulutus-oids]
    (into {} (keep #(when-let [v (get koulutus (keyword %))]
                      [% v])
                   koulutus-oids))))

(defrecord MockVirkailijaTarjontaService []
  VirkailijaTarjontaService
  (get-forms-in-use [_ session]
    (if (= (-> session :identity :oid) "1.2.246.562.11.11111111000")
      {"hakukohteen-organisaatiosta-form"
       {"1.2.246.562.29.65950024188"
        {:haku-oid  "1.2.246.562.29.65950024188"
         :haku-name {:fi "hakukohteen-organisaatiosta"}}}}
      {"belongs-to-hakukohteet-test-form"
       {(:oid base-haku)
        {:haku-oid  (:oid base-haku)
         :haku-name {:fi (:kieli_fi (:nimi base-haku))}}}
       "hakija-hakukohteen-hakuaika-test-form"
       {"1.2.246.562.29.65950024187"
        {:haku-oid  "1.2.246.562.29.65950024187"
         :haku-name {:fi "hakija-hakukohteen-hakuaika-haku"}}}})))
