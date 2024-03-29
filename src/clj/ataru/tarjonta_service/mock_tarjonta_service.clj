(ns ataru.tarjonta-service.mock-tarjonta-service
  (:require [com.stuartsierra.component :as component]
            [ataru.tarjonta-service.tarjonta-client :as tarjonta-client]
            [ataru.tarjonta-service.tarjonta-protocol :refer [TarjontaService]]
            [ataru.tarjonta-service.kouta.kouta-client :as kouta-client]))

(def toisen-asteen-yhteishaku-kohdejokko "haunkohdejoukko_11#1")

(def yhteishaku-hakutapa "hakutapa_01#1")

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

(def haut
  {:1.2.246.562.29.65950024185               base-haku
   :1.2.246.562.29.93102260101               (merge
                                               base-haku
                                               {:oid "1.2.246.562.29.93102260101"
                                                :ataruLomakeAvain "synthetic-application-test-form"})
   :haku.oid                                 (merge
                                               base-haku
                                               {:oid           "haku.oid"
                                                :hakukohdeOids ["hakukohde.oid"]
                                                :usePriority   true})
   :haku-2.oid                               (merge
                                               base-haku
                                               {:oid           "haku-2.oid"
                                                :hakukohdeOids ["hakukohde-in-ryhma.oid"]
                                                :usePriority   true})
   :1.2.246.562.29.65950024186               (merge
                                               base-haku
                                               {:oid              "1.2.246.562.29.65950024186"
                                                :usePriority      true
                                                :ataruLomakeAvain "41101b4f-1762-49af-9db0-e3603adae3ae"
                                                :hakukohdeOids    ["1.2.246.562.20.49028196523"
                                                                   "1.2.246.562.20.49028196524"
                                                                   "1.2.246.562.20.49028196525"
                                                                   "1.2.246.562.20.49028196526"]})
   :1.2.246.562.29.65950024187               (merge
                                               base-haku
                                               {:oid              "1.2.246.562.29.65950024187"
                                                :nimi             {:kieli_fi "hakija-hakukohteen-hakuaika-haku"}
                                                :usePriority      true
                                                :ataruLomakeAvain "hakija-hakukohteen-hakuaika-test-form"
                                                :hakukohdeOids    ["1.2.246.562.20.49028100001"
                                                                   "1.2.246.562.20.49028100002"
                                                                   "1.2.246.562.20.49028100003"
                                                                   "1.2.246.562.20.490281000035"]})
   :1.2.246.562.29.65950024188               (merge
                                               base-haku
                                               {:oid              "1.2.246.562.29.65950024188"
                                                :nimi             {:kieli_fi "hakukohteen-organisaatiosta"}
                                                :usePriority      true
                                                :ataruLomakeAvain "hakukohteen-organisaatiosta-form"
                                                :hakukohdeOids    ["1.2.246.562.20.49028100004"]})

   :1.2.246.562.29.65950024189               (merge
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
   :1.2.246.562.29.65950024190               (merge
                                               base-haku
                                               {:oid            "1.2.246.562.29.65950024190"
                                                :usePriority    true
                                                :kohdejoukkoUri "haunkohdejoukko_12#"})
   :1.2.246.562.29.65950024191               (merge
                                               base-haku
                                               {:oid              "1.2.246.562.29.65950024191"
                                                :usePriority      true
                                                :kohdejoukkoUri   "haunkohdejoukko_12#"
                                                :ataruLomakeAvain "41101b4f-1762-49af-9db0-e3603adae3ae"
                                                :hakukohdeOids    ["1.2.246.562.20.49028196523"
                                                                   "1.2.246.562.20.49028196524"
                                                                   "1.2.246.562.20.49028196525"
                                                                   "1.2.246.562.20.49028196526"]})
   :1.2.246.562.29.65950024192               (merge
                                               base-haku
                                               {:oid              "1.2.246.562.29.65950024192"
                                                :usePriority      true
                                                :kohdejoukkoUri   "haunkohdejoukko_12#"
                                                :ataruLomakeAvain "pohjakoulutus-test-form"
                                                :hakukohdeOids    ["1.2.246.562.20.49028196523"]})

   :form-access-control-test-basic-haku      (merge
                                               base-haku
                                               {:oid              "form-access-control-test-basic-haku"
                                                :ataruLomakeAvain "form-access-control-test-basic-form"})

   :form-access-control-test-hakukohde-haku  (merge
                                               base-haku
                                               {:oid              "form-access-control-test-hakukohde-haku"
                                                :ataruLomakeAvain "form-access-control-test-hakukohde-form"
                                                :hakukohdeOids    ["form-access-control-test-hakukohde"]})

   :form-access-control-test-yhteishaku-haku (merge
                                               base-haku
                                               {:oid              "form-access-control-test-yhteishaku-haku"
                                                :kohdejoukkoUri   toisen-asteen-yhteishaku-kohdejokko
                                                :hakutapaUri      yhteishaku-hakutapa
                                                :ataruLomakeAvain "form-access-control-test-yhteishaku-form"
                                                :hakukohdeOids    ["form-access-control-test-hakukohde"]})})

(def hakukohde
  {:1.2.246.562.20.49028196522           base-hakukohde

   :hakukohde.oid                        (merge base-hakukohde
                                           {:oid     "hakukohde.oid"
                                            :hakuOid "haku.oid"})

   :hakukohde_oid                        base-hakukohde

   :hakukohde-in-ryhma.oid               (merge base-hakukohde
                                           {:oid            "hakukohde-in-ryhma.oid"
                                            :ryhmaliitokset [{:ryhmaOid "1.2.246.562.28.00000000001"}]
                                            :hakuOid        "haku.oid"})
   :1.2.246.562.20.49028196523           (merge
                                           base-hakukohde
                                           {:ataruLomakeAvain          "41101b4f-1762-49af-9db0-e3603adae3ae"
                                            :oid                       "1.2.246.562.20.49028196523"
                                            :hakuOid                   "1.2.246.562.29.65950024186"
                                            :koulutukset               [{:oid "1.2.246.562.17.74335799462"}]
                                            :josYoEiMuitaLiitepyyntoja true
                                            :hakukohteenNimet
                                                                       {:kieli_fi "Testihakukohde 1"
                                                                        :kieli_sv "sv Testihakukohde 1"}})
   :1.2.246.562.20.49028196524           (merge
                                           base-hakukohde
                                           {:ataruLomakeAvain "41101b4f-1762-49af-9db0-e3603adae3ae"
                                            :oid              "1.2.246.562.20.49028196524"
                                            :hakuOid          "1.2.246.562.29.65950024186"
                                            :koulutukset      [{:oid "1.2.246.562.17.74335799463"}]
                                            :hakukohteenNimet
                                                              {:kieli_fi "Testihakukohde 2"
                                                               :kieli_sv "sv Testihakukohde 2"}})

   :1.2.246.562.20.49028196525           (merge
                                           base-hakukohde
                                           {:ataruLomakeAvain "41101b4f-1762-49af-9db0-e3603adae3ae"
                                            :oid              "1.2.246.562.20.49028196525"
                                            :hakuOid          "1.2.246.562.29.65950024186"
                                            :koulutukset      [{:oid "1.2.246.562.17.74335799464"}]
                                            :hakukohteenNimet
                                                              {:kieli_fi "Testihakukohde 3"
                                                               :kieli_sv "sv Testihakukohde 3"}})
   :1.2.246.562.20.49028100001           (merge
                                           base-hakukohde
                                           {:ataruLomakeAvain                     "hakija-hakukohteen-hakuaika-test-form"
                                            :oid                                  "1.2.246.562.20.49028100001"
                                            :hakuOid                              "1.2.246.562.29.65950024187"
                                            :kaytetaanHakukohdekohtaistaHakuaikaa true
                                            :hakuaikaAlkuPvm                      (- (System/currentTimeMillis)
                                                                                    86400000)
                                            :hakuaikaLoppuPvm                     (- (System/currentTimeMillis)
                                                                                    16400000)
                                            :koulutukset                          [{:oid "1.2.246.562.17.74335799465"}]
                                            :ryhmaliitokset                       [{:ryhmaOid "1.2.246.562.28.00000000001"}]
                                            :hakukohteenNimet
                                                                                  {:kieli_fi "Aikaloppu 1"
                                                                                   :kieli_sv "sv Aikaloppu 1"}})
   :1.2.246.562.20.49028100002           (merge
                                           base-hakukohde
                                           {:ataruLomakeAvain                     "hakija-hakukohteen-hakuaika-test-form"
                                            :oid                                  "1.2.246.562.20.49028100002"
                                            :hakuOid                              "1.2.246.562.29.65950024187"
                                            :kaytetaanHakukohdekohtaistaHakuaikaa true
                                            :hakuaikaAlkuPvm                      (- (System/currentTimeMillis)
                                                                                    86400000)
                                            :hakuaikaLoppuPvm                     (+ (System/currentTimeMillis)
                                                                                    86400000)
                                            :koulutukset                          [{:oid "1.2.246.562.17.74335799465"}]
                                            :hakukohteenNimet
                                                                                  {:kieli_fi "Aikaa jäljellä 2"
                                                                                   :kieli_sv "sv Aikaa jäljellä 2"}})
   :1.2.246.562.20.49028100003           (merge
                                           base-hakukohde
                                           {:ataruLomakeAvain "hakija-hakukohteen-hakuaika-test-form"
                                            :oid              "1.2.246.562.20.49028100003"
                                            :hakuOid          "1.2.246.562.29.65950024187"
                                            :koulutukset      [{:oid "1.2.246.562.17.74335799465"}]
                                            :ryhmaliitokset   [{:ryhmaOid "1.2.246.562.28.00000000001"}
                                                               {:ryhmaOid "1.2.246.562.28.00000000002"}]
                                            :hakukohteenNimet
                                                              {:kieli_fi "Aikaa loputtomasti 3"
                                                               :kieli_sv "sv Aikaa loputtomasti 3"}})
   :1.2.246.562.20.490281000035          (merge
                                           base-hakukohde
                                           {:ataruLomakeAvain "hakija-hakukohteen-hakuaika-test-form"
                                            :oid              "1.2.246.562.20.490281000035"
                                            :hakuOid          "1.2.246.562.29.65950024187"
                                            :koulutukset      [{:oid "1.2.246.562.17.74335799465"}]
                                            :ryhmaliitokset   [{:ryhmaOid "1.2.246.562.28.00000000001"}
                                                               {:ryhmaOid "1.2.246.562.28.00000000002"}]
                                            :hakukohteenNimet
                                                              {:kieli_fi "Aikaa loputtomasti 3.5"
                                                               :kieli_sv "sv Aikaa loputtomasti 3.5"}})
   :1.2.246.562.20.49028100004           (merge
                                           base-hakukohde
                                           {:ataruLomakeAvain "hakukohteen-organisaatiosta-form"
                                            :oid              "1.2.246.562.20.49028100004"
                                            :hakuOid          "1.2.246.562.29.65950024188"
                                            :tarjoajaOids     ["1.2.246.562.10.10826252480"]
                                            :koulutukset      [{:oid "1.2.246.562.17.74335799465"}]
                                            :hakukohteenNimet {:kieli_fi "Hakukohde johon käyttäjällä on organisaatio"
                                                               :kieli_sv "sv Hakukohde johon käyttäjällä on organisaatio"}})

   :1.2.246.562.20.49028100005           (merge
                                           base-hakukohde
                                           {:ataruLomakeAvain "41101b4f-1762-49af-9db0-e3603adae3ae"
                                            :oid              "1.2.246.562.20.49028100005"
                                            :hakuOid          "1.2.246.562.29.65950024189"
                                            :koulutukset      [{:oid "1.2.246.562.17.74335799464"}]
                                            :hakukohteenNimet
                                                              {:kieli_fi "Testihakukohde"
                                                               :kieli_sv "sv Testihakukohde"}})

   :form-access-control-test-hakukohde (merge
                                           base-hakukohde
                                           {:oid          "form-access-control-test-hakukohde"
                                            :tarjoajaOids ["form-access-control-test-oppilaitos"]})})

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

  (get-hakukohde [_ hakukohde-oid]
    (when-let [h ((keyword hakukohde-oid) hakukohde)]
      (tarjonta-client/parse-hakukohde h)))

  (get-hakukohteet [this hakukohde-oids]
    (keep #(.get-hakukohde this %) hakukohde-oids))

  (get-hakukohde-name [_ hakukohde-oid]
    (if (contains? #{"hakukohde.oid" "hakukohde-in-ryhma.oid"} hakukohde-oid)
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

  (get-haku [_ haku-oid]
    (when-let [h ((keyword haku-oid) haut)]
      (tarjonta-client/parse-haku h)))

  (hakus-by-form-key [this form-key]
    (if-let [haku-key
             (case form-key
               "hakukohteen-organisaatiosta-form"
               "1.2.246.562.29.65950024188"

               "belongs-to-hakukohteet-test-form"
               "1.2.246.562.29.65950024185"

               "hakija-hakukohteen-hakuaika-test-form"
               "1.2.246.562.29.65950024187"

               "form-access-control-test-basic-form"
               "form-access-control-test-basic-haku"

               "form-access-control-test-hakukohde-form"
               "form-access-control-test-hakukohde-haku"

               "form-access-control-test-yhteishaku-form"
               "form-access-control-test-yhteishaku-haku"

               nil)]
      [(.get-haku this haku-key)]
      []))

  (get-haku-name [_ haku-oid]
    (when (= haku-oid "1.2.246.562.29.65950024185")
      {:fi "testing2"}))

  (get-koulutus [_ koulutus-id]
    ((keyword koulutus-id) koulutus))

  (get-koulutukset [_ koulutus-oids]
    (into {} (keep #(when-let [v (get koulutus (keyword %))]
                      [% v])
                   koulutus-oids))))

(def base-kouta-hakukohde
  {:kaytetaanHaunAikataulua true
   :tila "julkaistu"
   :hakulomakeKuvaus {}
   :pohjakoulutusvaatimusKoodiUrit ["pohjakoulutusvaatimuskouta_pk#1" "pohjakoulutusvaatimuskouta_er#1"]
   :koulutustyyppikoodi "koulutustyyppi_26"
   :organisaatioNimi {:fi "Espoon seudun koulutuskuntayhtymä Omnia", :sv "Espoon seudun koulutuskuntayhtymä Omnia", :en "Espoon seudun koulutuskuntayhtymä Omnia"}
   :kaytetaanHaunHakulomaketta true
   :aloituspaikat 80
   :kaytetaanHaunAlkamiskautta true
   :modified "2022-12-15T15:35:32"
   :paateltyAlkamiskausi {:alkamiskausityyppi "alkamiskausi ja -vuosi", :source "1.2.246.562.29.00000000000000021303", :kausiUri "kausi_s#1", :vuosi "2023"}
   :toteutusOid "1.2.246.562.17.00000000000000006915"
   :salliikoHakukohdeHarkinnanvaraisuudenKysymisen true
   :muuPohjakoulutusvaatimus {}
   :toinenAsteOnkoKaksoistutkinto true
   :muokkaaja "1.2.246.562.24.77641069200"
   :liitteetOnkoSamaToimitusaika false
   :hakuajat []
   :valintaperusteValintakokeet []
   :hakuOid "1.2.246.562.29.00000000000000021303"
   :uudenOpiskelijanUrl {}
   :tarjoaja "1.2.246.562.10.74572512155"
   :liitteetOnkoSamaToimitusosoite true
   :liitteet []
   :hakukohde {:koodiUri "hakukohteetperusopetuksenjalkeinenyhteishaku_1027#1"}
   :yhdenPaikanSaanto {:voimassa false, :syy "Ei korkeakoulutus koulutusta"}
   :kielivalinta ["fi"]
   :liitteidenToimitustapa "osoite"
   :jarjestaaUrheilijanAmmKoulutusta true
   :voikoHakukohteessaOllaHarkinnanvaraisestiHakeneita true
   :valintaperusteId "2306d148-9fa9-451b-9a1e-0c6ca0ca5d3f"
   :valintakokeet []
   :organisaatioOid "1.2.246.562.10.53642770753"
   :painotetutArvosanat []})

(def kouta-hakukohdes {
                       :1.2.246.562.20.00000000000000024371 (merge base-kouta-hakukohde {
                                                                                   :oid "1.2.246.562.20.00000000000000024371"
                                                                                   :koulutustyyppikoodi "koulutustyyppi_26"
                                                                                   :nimi {:fi "Ajoneuvoalan perustutkinto"}})
                       :1.2.246.562.20.00000000000000024372 (merge base-kouta-hakukohde {
                                                                                   :oid "1.2.246.562.20.00000000000000024372"
                                                                                   :koulutustyyppikoodi "koulutustyyppi_3"
                                                                                   :nimi {:fi "Ei tarvi tarkistaa harkinnanvaraisuutta"}})
                       })

(defrecord MockTarjontaKoutaService []
  component/Lifecycle
  TarjontaService

  (start [this] this)
  (stop [this] this)

  (get-hakukohde [_ hakukohde-oid]
    (when-let [h ((keyword hakukohde-oid) kouta-hakukohdes)]
      (kouta-client/parse-hakukohde h [] [] {})))

  (get-hakukohteet [this hakukohde-oids]
    (keep #(.get-hakukohde this %) hakukohde-oids))

  (get-hakukohde-name [_ hakukohde-oid]
    (if (contains? #{"hakukohde.oid" "hakukohde-in-ryhma.oid"} hakukohde-oid)
      {:fi "Ajoneuvonosturinkuljettajan ammattitutkinto"}
      {:fi "Testihakukohde"}))

  (hakukohde-search [_ haku-oid _]
    (let [to-hakukohteet (fn [hakukohde-oids] (->> (map #(get hakukohde %) hakukohde-oids)
                                                   (map #(kouta-client/parse-hakukohde % [] [] {}))
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

  (get-haku [_ haku-oid]
    (when-let [h ((keyword haku-oid) haut)]
      (kouta-client/parse-haku h [] [])))

  (hakus-by-form-key [this form-key]
    (if-let [haku-key
             (case form-key
               "hakukohteen-organisaatiosta-form"
               "1.2.246.562.29.65950024188"

               "belongs-to-hakukohteet-test-form"
               "1.2.246.562.29.65950024185"

               "hakija-hakukohteen-hakuaika-test-form"
               "1.2.246.562.29.65950024187"

               "form-access-control-test-basic-form"
               "form-access-control-test-basic-haku"

               "form-access-control-test-hakukohde-form"
               "form-access-control-test-hakukohde-haku"

               "form-access-control-test-yhteishaku-form"
               "form-access-control-test-yhteishaku-haku"

               nil)]
      [(.get-haku this haku-key)]
      []))

  (get-haku-name [_ haku-oid]
    (when (= haku-oid "1.2.246.562.29.65950024185")
      {:fi "testing2"}))

  (get-koulutus [_ koulutus-id]
    ((keyword koulutus-id) koulutus))

  (get-koulutukset [_ koulutus-oids]
    (into {} (keep #(when-let [v (get koulutus (keyword %))]
                      [% v])
                   koulutus-oids))))

