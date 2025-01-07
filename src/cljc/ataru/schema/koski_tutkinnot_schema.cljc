(ns ataru.schema.koski-tutkinnot-schema
  (:require [ataru.schema.localized-schema :as localized-schema]
            [schema.core :as s]))

(s/defschema KoskiItemWithLocalizedNimi
  {s/Any  s/Any
   (s/optional-key :koodiarvo) s/Str
   (s/optional-key :nimi)  localized-schema/LocalizedString})

(s/defschema KoskiSuoritusKoulutustyyppiItem
  {s/Any        s/Any
   :koodistoUri s/Str
   :koodiarvo   s/Str})
(s/defschema KoskiSuoritusItem
  {s/Any                s/Any
   :koulutusmoduuli     {s/Any            s/Any
                         :tunniste        KoskiItemWithLocalizedNimi
                         (s/optional-key :koulutustyyppi)  KoskiSuoritusKoulutustyyppiItem}
   :vahvistus           {:päivä       s/Str}
   :toimipiste          KoskiItemWithLocalizedNimi
   (s/optional-key :tyyppi)              KoskiItemWithLocalizedNimi})

(s/defschema KoskiSuoritusResponse
  {
   s/Any              s/Any
   :opiskeluoikeudet [{s/Any        s/Any
                       :suoritukset [KoskiSuoritusItem]}]})

(s/defschema AtaruKoskiTutkintoResponse
  {:tutkintonimi                      localized-schema/LocalizedString
   :koulutusohjelmanimi               s/Str;localized-schema/LocalizedString
   :toimipistenimi                    localized-schema/LocalizedString
   :valmistumispvm                    s/Str
   (s/optional-key :koulutustyyppi)   KoskiSuoritusKoulutustyyppiItem})

(s/defschema Tutkinnot
  {:tutkinnot {:description localized-schema/LocalizedString
               :field-list localized-schema/LocalizedString}})
