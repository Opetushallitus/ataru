(ns ataru.schema.koski-tutkinnot-schema
  (:require [ataru.schema.element-metadata-schema :as element-metadata-schema]
            [ataru.schema.localized-schema :as localized-schema]
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
  {:id                   s/Str
   :fieldClass           (s/eq "tutkinnot")
   :fieldType            (s/eq "tutkinnot")
   :exclude-from-answers (s/eq true)
   :metadata             element-metadata-schema/ElementMetadata
   :params               {:deny-submit s/Bool}
   :label                localized-schema/LocalizedStringOptional
   :text                 localized-schema/LocalizedStringOptional})
