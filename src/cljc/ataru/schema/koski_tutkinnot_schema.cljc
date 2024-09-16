(ns ataru.schema.koski-tutkinnot-schema
  (:require [ataru.schema.localized-schema :as localized-schema]
            [schema.core :as s]))

(s/defschema KoskiItemWithLocalizedNimi
  {s/Any  s/Any
   :nimi  localized-schema/LocalizedString})

(s/defschema KoskiSuoritusKoulutustyyppiItem
  {s/Any        s/Any
   :koodistoUri s/Str
   :koodiarvo   s/Str})
(s/defschema KoskiSuoritusItem
  {s/Any                s/Any
   :koulutusmoduuli     {s/Any            s/Any
                         :tunniste        KoskiItemWithLocalizedNimi
                         :koulutustyyppi  KoskiSuoritusKoulutustyyppiItem}
   :vahvistus           {:päivä       s/Str}
   :toimipiste          KoskiItemWithLocalizedNimi
   :tyyppi              KoskiItemWithLocalizedNimi})

(s/defschema KoskiSuoritusResponse
  {
   s/Any              s/Any
   :opiskeluoikeudet [{s/Any        s/Any
                       :suoritukset [KoskiSuoritusItem]}]})

(s/defschema AtaruKoskiTutkintoResponse
  {:tutkintonimi          localized-schema/LocalizedString
   :koulutusohjelmanimi   localized-schema/LocalizedString
   :toimipistenimi        localized-schema/LocalizedString
   :valmistumispvm        s/Str
   :koulutustyyppi        KoskiSuoritusKoulutustyyppiItem})

(s/defschema Tutkinnot
  {:description localized-schema/LocalizedString})
