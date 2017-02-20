(ns ataru.person-service.legacy-person-schema
  (:require [schema.core :as s]))


; This schema is just "internal" part of the person-client ns
; and therefore it isn't placed into form-schema.cljc
(s/defschema LegacyPerson
  {(s/optional-key :personId)    (s/maybe s/Str)
   (s/optional-key :birthDate)   (s/maybe s/Str)
   :nativeLanguage               (s/maybe s/Str)
   :email                        s/Str
   :idpEntitys                   [{:idpEntityId s/Str
                                   :identifier  s/Str}]
   :firstName                    s/Str
   :lastName                     s/Str
   (s/optional-key :nationality) (s/maybe s/Str)
   (s/optional-key :gender)      (s/maybe s/Str)
   (s/optional-key :personOid)   (s/maybe s/Str)})
