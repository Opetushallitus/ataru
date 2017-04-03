(ns ataru.tarjonta-service.tarjonta-service
  (:require
    [ataru.tarjonta-service.tarjonta-client :as client]
    [ataru.virkailija.user.organization-client :refer [oph-organization]]
    [com.stuartsierra.component :as component]
    [ataru.config.core :refer [config]]))

(defn forms-in-use
  [organization-service username]
  (let [direct-organizations     (.get-direct-organizations-for-rights organization-service username [:form-edit])
        all-organization-oids    (map :oid (.get-all-organizations organization-service (:form-edit direct-organizations)))
        in-oph-organization?     (some #{oph-organization} all-organization-oids)]
    (reduce (fn [acc1 {:keys [avain haut]}]
              (assoc acc1 avain
                          (reduce (fn [acc2 haku]
                                    (assoc acc2 (:oid haku)
                                                {:haku-oid  (:oid haku)
                                                 :haku-name (get-in haku [:nimi :kieli_fi])}))
                                  {} haut)))
            {}
            (client/get-forms-in-use (if in-oph-organization? nil all-organization-oids)))))

(defprotocol TarjontaService
  (get-hakukohde [this hakukohde-oid])
  (get-hakukohde-name [this hakukohde-oid])
  (get-haku [this haku-oid])
  (get-haku-name [this haku-oid])
  (get-koulutus [this haku-oid]))

(defrecord CachedTarjontaService []
  component/Lifecycle
  TarjontaService

  (start [this] this)
  (stop [this] this)

  (get-hakukohde [this hakukohde-oid]
    (.cache-get-or-fetch (:cache-service this) :hakukohde hakukohde-oid #(client/get-hakukohde hakukohde-oid)))

  (get-hakukohde-name [this hakukohde-oid]
    (-> this
        (.get-hakukohde hakukohde-oid)
        :hakukohteenNimet
        :kieli_fi))

  (get-haku [this haku-oid]
    (.cache-get-or-fetch (:cache-service this) :haku haku-oid #(client/get-haku haku-oid)))

  (get-haku-name [this haku-oid]
    (-> this
        (.get-haku haku-oid)
        :nimi
        :kieli_fi))

  (get-koulutus [this koulutus-oid]
    (.cache-get-or-fetch (:cache-service this) :koulutus koulutus-oid #(client/get-koulutus koulutus-oid))))

(defprotocol VirkailijaTarjontaService
  (get-forms-in-use [this username]))

(defrecord VirkailijaTarjontaFormsService []
  component/Lifecycle
  VirkailijaTarjontaService

  (start [this] this)
  (stop [this] this)

  (get-forms-in-use [this username]
    (forms-in-use (:organization-service this) username)))

(defrecord FakeTarjontaService []
  component/Lifecycle
  TarjontaService

  (start [this] this)
  (stop [this] this)

  (get-hakukohde [this hakukohde-oid]
    (when (= hakukohde-oid "hakukohde.oid")
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
       :valintakokeet                           []}))

  (get-hakukohde-name [this hakukohde-oid]
    (when (= hakukohde-oid "hakukohde.oid")
      "Ajoneuvonosturinkuljettajan ammattitutkinto"))

  (get-haku [this haku-oid]
    (when (= haku-oid "1.2.246.562.29.65950024185")
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
       :hakuaikas                                            [{:hakuaikaId "10291885", :alkuPvm 1480330218240, :loppuPvm 1480503020479, :nimet {:kieli_sv "", :kieli_fi "", :kieli_en ""}}],
       :sijoittelu                                           false}))

  (get-haku-name [this haku-oid]
    (when (= haku-oid "1.2.246.562.29.65950024185")
      "testing2"))

  (get-koulutus [this koulutus-id]
    (when (= koulutus-id "1.2.246.562.17.74335799461")
      {:oid             "1.2.246.562.17.74335799461"
       :koulutuskoodi   {:nimi "Koulutuskoodi"}
       :tutkintonimike  {:nimi "Tutkintonimike"}
       :koulutusohjelma {:nimi "Koulutusohjelma"}
       :tarkenne        "Tarkenne"})))

(defn new-tarjonta-service
  []
  (if (-> config :dev :fake-dependencies)
    (->FakeTarjontaService)
    (->CachedTarjontaService)))

(defn new-virkailija-tarjonta-service
  []
  (->VirkailijaTarjontaFormsService))
