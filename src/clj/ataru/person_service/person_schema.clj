(ns ataru.person-service.person-schema
  (:require [schema.core :as s]))

(s/defschema HenkiloPerustieto
  {(s/optional-key :hetu)            (s/maybe s/Str)
   :etunimet                         s/Str
   :kutsumanimi                      s/Str
   :sukunimi                         s/Str
   :aidinkieli                       {(s/required-key :kieliKoodi) s/Str}
   :asiointiKieli                    {(s/required-key :kieliKoodi) s/Str}
   :kansalaisuus                     [{(s/required-key :kansalaisuusKoodi) s/Str}]
   :eiSuomalaistaHetua               s/Bool
   :sukupuoli                        s/Str
   :yhteystieto                      [{(s/required-key :yhteystietoTyyppi) s/Str
                                       (s/required-key :yhteystietoArvo)   s/Str}]
   (s/optional-key :syntymaaika)     (s/maybe s/Str)
   (s/optional-key :identifications) [{(s/required-key :idpEntityId) s/Str
                                       (s/required-key :identifier)  s/Str}]
   :henkiloTyyppi                    s/Str})
