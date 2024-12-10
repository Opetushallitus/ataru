(ns ataru.schema.koski-tutkinnot-schema
  (:require [ataru.schema.localized-schema :as localized-schema]
            [schema.core :as s]))

(s/defschema KoodistoValueWithOptionalNimi
  {s/Any                   s/Any
   :koodiarvo              s/Str
   :koodistoUri            s/Str
   (s/optional-key :nimi)  localized-schema/LocalizedString})

(defn- koodisto-value [koodisto-uri]
  {s/Any        s/Any
   :koodiarvo   s/Str
   :koodistoUri (s/eq koodisto-uri)})

(s/defschema VirtaTiedot
  {s/Any        s/Any
   :virtaOpiskeluoikeudenTyyppi (koodisto-value "virtaopiskeluoikeudentyyppi")})

(s/defschema KoskiSuoritusItem
  {s/Any            s/Any
   :koulutusmoduuli {s/Any                            s/Any
                     :tunniste                        KoodistoValueWithOptionalNimi
                     (s/optional-key :virtaNimi)      localized-schema/LocalizedString
                     (s/optional-key :koulutustyyppi) KoodistoValueWithOptionalNimi}
   :vahvistus       {:päivä   s/Str}
   :toimipiste      {s/Any    s/Any
                     :oid     s/Str
                     :nimi    localized-schema/LocalizedString}})

(s/defschema KoskiResponse
  {s/Any              s/Any
   :opiskeluoikeudet [{s/Any                          s/Any
                       (s/optional-key :oid)          s/Str
                       (s/optional-key :lisätiedot)   VirtaTiedot
                       :tyyppi                        KoodistoValueWithOptionalNimi
                       :suoritukset                   [KoskiSuoritusItem]}]})

(def koski-levels
  ["perusopetus" "lukiokoulutus" "yo" "amm" "amm-perus" "amm-erikois" "kk-alemmat" "kk-ylemmat" "lisensiaatti" "tohtori"])
(s/defschema AtaruKoskiTutkinto
  {:id                                    s/Str
   :tutkintonimi                          localized-schema/LocalizedString
   (s/optional-key :koulutusohjelmanimi)  localized-schema/LocalizedString
   (s/optional-key :toimipistenimi)       localized-schema/LocalizedString
   :valmistumispvm                        s/Str
   :level                                 (apply s/enum koski-levels)})

(s/defschema Tutkinnot
  {:tutkinnot {:description localized-schema/LocalizedString
               :field-list localized-schema/LocalizedString}})
