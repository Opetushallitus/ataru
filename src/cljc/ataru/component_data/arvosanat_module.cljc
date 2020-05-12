(ns ataru.component-data.arvosanat-module
  (:require [ataru.component-data.component :as component]
            [ataru.translations.texts :as texts]
            [ataru.util :as util]
            [schema.core :as s]))

(s/defschema ElementMetadata
  {:created-by              {:name s/Str
                             :oid  s/Str
                             :date s/Str}                   ; java.time.ZonedDateTime
   :modified-by             {:name s/Str
                             :oid  s/Str
                             :date s/Str}
   (s/optional-key :locked) s/Bool})

(s/defschema LocalizedString {:fi                  s/Str
                              (s/optional-key :sv) s/Str
                              (s/optional-key :en) s/Str})

(s/defschema OppiaineenKoodi
  (s/enum "A"))

(s/defschema OppiaineenArvosana
  {:fieldClass (s/eq "oppiaineenArvosana")
   :fieldType  OppiaineenKoodi
   :label      LocalizedString})

(s/defschema ArvosanatTaulukko
  {:id         s/Str
   :fieldClass (s/eq "wrapperElement")
   :fieldType  (s/eq "arvosanat-taulukko")
   :children   [OppiaineenArvosana]
   :metadata   ElementMetadata})


(s/defn oppiaineen-arvosana :- OppiaineenArvosana
  [{:keys [oppiaineen-koodi
           label]} :- {:oppiaine OppiaineenKoodi
                       :label    LocalizedString}]
  {:fieldClass "oppiaineenArvosana"
   :fieldType  oppiaineen-koodi
   :label      label})

(def ^:private arvosana-aidinkieli-ja-kirjallisuus
  (oppiaineen-arvosana
    {:oppiaineen-koodi "A"
     :label            (:arvosanat-aidinkieli-ja-kirjallisuus texts/virkailija-texts)}))

(s/defn arvosanat-taulukko :- ArvosanatTaulukko
  [{:keys [metadata children]}]
  {:id         (util/component-id)
   :fieldClass "wrapperElement"
   :fieldType  "arvosanat-taulukko"
   :children   children
   :metadata   metadata})

(s/defn arvosanat
  [{:keys [type]} :- {:type (s/enum :peruskoulu)}
   metadata]
  (let [id        (case type
                    :peruskoulu "arvosanat-peruskoulu")
        label-kwd (case type
                    :peruskoulu :arvosanat-peruskoulu)]
    (merge (component/form-section metadata)
           {:id       id
            :label    (label-kwd texts/virkailija-texts)
            :module   id
            :children [(assoc (component/info-element metadata)
                         :text
                         (:arvosanat-info texts/virkailija-texts))
                       (arvosanat-taulukko
                         {:metadata metadata
                          :children [arvosana-aidinkieli-ja-kirjallisuus]})]})))

(defn arvosanat-peruskoulu [metadata]
  (arvosanat {:type :peruskoulu} metadata))
