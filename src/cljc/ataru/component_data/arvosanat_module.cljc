(ns ataru.component-data.arvosanat-module
  (:require [ataru.component-data.component :as component]
            [ataru.schema.element-metadata-schema :as element-metadata-schema]
            [ataru.schema.localized-schema :as localized-schema]
            [ataru.translations.texts :as texts]
            [schema.core :as s]
            [schema-tools.core :as st]))

(s/defschema OppiaineenKoodi
  (s/enum "A"
          "A1"
          "B1"
          "MA"
          "BI"
          "GE"
          "FY"
          "KE"
          "TT"
          "TY"
          "HI"
          "YH"
          "MU"
          "KU"
          "KA"
          "LI"
          "KO"))

(s/defschema OppiaineenArvosana
  {:fieldClass                (s/eq "wrapperElement")
   :fieldType                 (s/eq "oppiaineenArvosana")
   :id                        OppiaineenKoodi
   :label                     localized-schema/LocalizedString
   (s/optional-key :children) [s/Any]})

(s/defschema ArvosanatTaulukko
  {:id         s/Str
   :fieldClass (s/eq "wrapperElement")
   :fieldType  (s/eq "arvosanat-taulukko")
   :children   [OppiaineenArvosana]
   :metadata   element-metadata-schema/ElementMetadata})

(defn- oppimaara-aidinkieli-ja-kirjallisuus [{:keys [metadata]}]
  {:fieldClass       "formField"
   :fieldType        "dropdown"
   :version          :accessible
   :id               "oppimaara-aidinkieli-ja-kirjallisuus"
   :label            (:oppimaara-aidinkieli-ja-kirjallisuus texts/translation-mapping)
   :unselected-label (:oppimaara texts/translation-mapping)
   :options          [{:label (:suomi-aidinkielena texts/translation-mapping)
                       :value "suomi-aidinkielena"}]
   :metadata         metadata
   :validators       ["required"]})

(s/defn oppiaineen-arvosana :- OppiaineenArvosana
  [{:keys [oppiaineen-koodi
           label
           second-column-component]} :- (st/open-schema
                                          {:oppiaine OppiaineenKoodi
                                           :label    localized-schema/LocalizedString})]
  (cond-> {:fieldClass "wrapperElement"
           :fieldType  "oppiaineenArvosana"
           :id         oppiaineen-koodi
           :label      label
           :children   (cond-> []
                               (some? second-column-component)
                               (conj second-column-component))}))

(def ^:private arvosana-aidinkieli-ja-kirjallisuus
  (oppiaineen-arvosana
    {:oppiaineen-koodi        "A"
     :label                   (:arvosana-aidinkieli-ja-kirjallisuus texts/virkailija-texts)
     :second-column-component oppiaineen-oppimaara}))

(def ^:private arvosana-a1-kieli
  (oppiaineen-arvosana
    {:oppiaineen-koodi "A1"
     :label            (:arvosana-a1-kieli texts/virkailija-texts)}))

(def ^:private arvosana-b1-kieli
  (oppiaineen-arvosana
    {:oppiaineen-koodi "B1"
     :label            (:arvosana-b1-kieli texts/virkailija-texts)}))

(def ^:private arvosana-matematiikka
  (oppiaineen-arvosana
    {:oppiaineen-koodi "MA"
     :label            (:arvosana-matematiikka texts/virkailija-texts)}))

(def ^:private arvosana-biologia
  (oppiaineen-arvosana
    {:oppiaineen-koodi "BI"
     :label            (:arvosana-biologia texts/virkailija-texts)}))

(def ^:private arvosana-maantieto
  (oppiaineen-arvosana
    {:oppiaineen-koodi "GE"
     :label            (:arvosana-maantieto texts/virkailija-texts)}))

(def ^:private arvosana-fysiikka
  (oppiaineen-arvosana
    {:oppiaineen-koodi "FY"
     :label            (:arvosana-fysiikka texts/virkailija-texts)}))

(def ^:private arvosana-kemia
  (oppiaineen-arvosana
    {:oppiaineen-koodi "KE"
     :label            (:arvosana-kemia texts/virkailija-texts)}))

(def ^:private arvosana-terveystieto
  (oppiaineen-arvosana
    {:oppiaineen-koodi "TT"
     :label            (:arvosana-terveystieto texts/virkailija-texts)}))

(def ^:private arvosana-uskonto-tai-elamankatsomustieto
  (oppiaineen-arvosana
    {:oppiaineen-koodi "TY"
     :label            (:arvosana-uskonto-tai-elamankatsomustieto texts/virkailija-texts)}))

(def ^:private arvosana-historia
  (oppiaineen-arvosana
    {:oppiaineen-koodi "HI"
     :label            (:arvosana-historia texts/virkailija-texts)}))

(def ^:private arvosana-yhteiskuntaoppi
  (oppiaineen-arvosana
    {:oppiaineen-koodi "YH"
     :label            (:arvosana-yhteiskuntaoppi texts/virkailija-texts)}))

(def ^:private arvosana-musiikki
  (oppiaineen-arvosana
    {:oppiaineen-koodi "MU"
     :label            (:arvosana-musiikki texts/virkailija-texts)}))

(def ^:private arvosana-kuvataide
  (oppiaineen-arvosana
    {:oppiaineen-koodi "KU"
     :label            (:arvosana-kuvataide texts/virkailija-texts)}))

(def ^:private arvosana-kasityo
  (oppiaineen-arvosana
    {:oppiaineen-koodi "KA"
     :label            (:arvosana-kasityo texts/virkailija-texts)}))

(def ^:private arvosana-liikunta
  (oppiaineen-arvosana
    {:oppiaineen-koodi "LI"
     :label            (:arvosana-liikunta texts/virkailija-texts)}))

(def ^:private arvosana-kotitalous
  (oppiaineen-arvosana
    {:oppiaineen-koodi "KO"
     :label            (:arvosana-kotitalous texts/virkailija-texts)}))

(s/defn arvosanat-taulukko :- ArvosanatTaulukko
  [{:keys [metadata
           children]} :- {:metadata element-metadata-schema/ElementMetadata
                          :children [OppiaineenArvosana]}]
  {:id         "arvosanat-taulukko"
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
                          :children [arvosana-aidinkieli-ja-kirjallisuus
                                     arvosana-a1-kieli
                                     arvosana-b1-kieli
                                     arvosana-matematiikka
                                     arvosana-biologia
                                     arvosana-maantieto
                                     arvosana-fysiikka
                                     arvosana-kemia
                                     arvosana-terveystieto
                                     arvosana-uskonto-tai-elamankatsomustieto
                                     arvosana-historia
                                     arvosana-yhteiskuntaoppi
                                     arvosana-musiikki
                                     arvosana-kuvataide
                                     arvosana-kasityo
                                     arvosana-liikunta
                                     arvosana-kotitalous]})]})))

(defn arvosanat-peruskoulu [metadata]
  (arvosanat {:type :peruskoulu} metadata))
