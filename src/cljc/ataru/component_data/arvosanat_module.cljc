(ns ataru.component-data.arvosanat-module
  (:require [ataru.component-data.component :as component]
            [ataru.schema.element-metadata-schema :as element-metadata-schema]
            [ataru.schema.localized-schema :as localized-schema]
            [ataru.translations.texts :as texts]
            [schema.core :as s]
            [clojure.string :as string]))

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
   :metadata                  element-metadata-schema/ElementMetadata
   (s/optional-key :children) [s/Any]})

(s/defschema ArvosanatTaulukko
  {:id         s/Str
   :fieldClass (s/eq "wrapperElement")
   :fieldType  (s/eq "arvosanat-taulukko")
   :children   [OppiaineenArvosana]
   :metadata   element-metadata-schema/ElementMetadata})

(s/defn concat-labels
  [separator :- s/Str
   to :- localized-schema/LocalizedString
   from :- localized-schema/LocalizedString]
  (reduce-kv (fn [acc k v]
               (update acc k str separator v))
             to
             from))

(s/defn oppiaine-label :- localized-schema/LocalizedString
  [oppiaine-koodi :- OppiaineenKoodi]
  (->> oppiaine-koodi
       string/lower-case
       (str "oppiaine-")
       keyword
       texts/oppiaine-translations))

(s/defn oppimaara-aidinkieli-ja-kirjallisuus
  [{:keys [metadata]} :- {:metadata element-metadata-schema/ElementMetadata}]
  {:fieldClass       "formField"
   :fieldType        "dropdown"
   :version          :accessible
   :id               "oppimaara-aidinkieli-ja-kirjallisuus"
   :label            (concat-labels
                       ": "
                       (:oppimaara texts/translation-mapping)
                       (oppiaine-label "A"))
   :unselected-label (:oppimaara texts/translation-mapping)
   :options          [{:label (:suomi-aidinkielena texts/translation-mapping)
                       :value "suomi-aidinkielena"}]
   :metadata         metadata
   :validators       ["required"]})

(s/defn arvosana-dropdown
  [{:keys [oppiaineen-koodi
           metadata]} :- {:metadata         element-metadata-schema/ElementMetadata
                          :oppiaineen-koodi OppiaineenKoodi}]
  {:fieldClass       "formField"
   :fieldType        "dropdown"
   :version          :accessible
   :id               (str "arvosana-" oppiaineen-koodi)
   :label            (concat-labels
                       ": "
                       (:arvosana texts/translation-mapping)
                       (oppiaine-label oppiaineen-koodi))
   :unselected-label (:arvosana texts/translation-mapping)
   :options          (->> (range 4 11)
                          (map str)
                          (map (fn [arvosana]
                                 {:value arvosana
                                  :label {:fi arvosana
                                          :sv arvosana
                                          :en arvosana}})))
   :metadata         metadata
   :validators       ["required"]})

(s/defn oppiaineen-arvosana :- OppiaineenArvosana
  [{:keys [oppiaineen-koodi
           label
           oppimaara-dropdown
           metadata]} :- {:oppiaineen-koodi                    OppiaineenKoodi
                          :label                               localized-schema/LocalizedString
                          :metadata                            element-metadata-schema/ElementMetadata
                          (s/optional-key :oppimaara-dropdown) s/Any}]
  (cond-> {:fieldClass "wrapperElement"
           :fieldType  "oppiaineenArvosana"
           :id         oppiaineen-koodi
           :label      label
           :metadata   metadata
           :children   (as-> [] children
                             (cond-> children
                                     (some? oppimaara-dropdown)
                                     (conj oppimaara-dropdown))
                             (conj children (arvosana-dropdown
                                              {:metadata         metadata
                                               :oppiaineen-koodi oppiaineen-koodi})))}))

(defn- arvosana-aidinkieli-ja-kirjallisuus [{:keys [metadata]}]
  (oppiaineen-arvosana
    {:oppiaineen-koodi   "A"
     :label              (:arvosana-aidinkieli-ja-kirjallisuus texts/virkailija-texts)
     :oppimaara-dropdown (oppimaara-aidinkieli-ja-kirjallisuus {:metadata metadata})
     :metadata           metadata}))

(defn- arvosana-a1-kieli [{:keys [metadata]}]
  (oppiaineen-arvosana
    {:oppiaineen-koodi "A1"
     :label            (:arvosana-a1-kieli texts/virkailija-texts)
     :metadata         metadata}))

(defn- arvosana-b1-kieli [{:keys [metadata]}]
  (oppiaineen-arvosana
    {:oppiaineen-koodi "B1"
     :label            (:arvosana-b1-kieli texts/virkailija-texts)
     :metadata         metadata}))

(defn- arvosana-matematiikka [{:keys [metadata]}]
  (oppiaineen-arvosana
    {:oppiaineen-koodi "MA"
     :label            (:arvosana-matematiikka texts/virkailija-texts)
     :metadata         metadata}))

(defn- arvosana-biologia [{:keys [metadata]}]
  (oppiaineen-arvosana
    {:oppiaineen-koodi "BI"
     :label            (:arvosana-biologia texts/virkailija-texts)
     :metadata         metadata}))

(defn- arvosana-maantieto [{:keys [metadata]}]
  (oppiaineen-arvosana
    {:oppiaineen-koodi "GE"
     :label            (:arvosana-maantieto texts/virkailija-texts)
     :metadata         metadata}))

(defn- arvosana-fysiikka [{:keys [metadata]}]
  (oppiaineen-arvosana
    {:oppiaineen-koodi "FY"
     :label            (:arvosana-fysiikka texts/virkailija-texts)
     :metadata         metadata}))

(defn- arvosana-kemia [{:keys [metadata]}]
  (oppiaineen-arvosana
    {:oppiaineen-koodi "KE"
     :label            (:arvosana-kemia texts/virkailija-texts)
     :metadata         metadata}))

(defn- arvosana-terveystieto [{:keys [metadata]}]
  (oppiaineen-arvosana
    {:oppiaineen-koodi "TT"
     :label            (:arvosana-terveystieto texts/virkailija-texts)
     :metadata         metadata}))

(defn- arvosana-uskonto-tai-elamankatsomustieto [{:keys [metadata]}]
  (oppiaineen-arvosana
    {:oppiaineen-koodi "TY"
     :label            (:arvosana-uskonto-tai-elamankatsomustieto texts/virkailija-texts)
     :metadata         metadata}))

(defn- arvosana-historia [{:keys [metadata]}]
  (oppiaineen-arvosana
    {:oppiaineen-koodi "HI"
     :label            (:arvosana-historia texts/virkailija-texts)
     :metadata         metadata}))

(defn- arvosana-yhteiskuntaoppi [{:keys [metadata]}]
  (oppiaineen-arvosana
    {:oppiaineen-koodi "YH"
     :label            (:arvosana-yhteiskuntaoppi texts/virkailija-texts)
     :metadata         metadata}))

(defn- arvosana-musiikki [{:keys [metadata]}]
  (oppiaineen-arvosana
    {:oppiaineen-koodi "MU"
     :label            (:arvosana-musiikki texts/virkailija-texts)
     :metadata         metadata}))

(defn- arvosana-kuvataide [{:keys [metadata]}]
  (oppiaineen-arvosana
    {:oppiaineen-koodi "KU"
     :label            (:arvosana-kuvataide texts/virkailija-texts)
     :metadata         metadata}))

(defn- arvosana-kasityo [{:keys [metadata]}]
  (oppiaineen-arvosana
    {:oppiaineen-koodi "KA"
     :label            (:arvosana-kasityo texts/virkailija-texts)
     :metadata         metadata}))

(defn- arvosana-liikunta [{:keys [metadata]}]
  (oppiaineen-arvosana
    {:oppiaineen-koodi "LI"
     :label            (:arvosana-liikunta texts/virkailija-texts)
     :metadata         metadata}))

(defn- arvosana-kotitalous [{:keys [metadata]}]
  (oppiaineen-arvosana
    {:oppiaineen-koodi "KO"
     :label            (:arvosana-kotitalous texts/virkailija-texts)
     :metadata         metadata}))

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
                          :children [(arvosana-aidinkieli-ja-kirjallisuus {:metadata metadata})
                                     (arvosana-a1-kieli {:metadata metadata})
                                     (arvosana-b1-kieli {:metadata metadata})
                                     (arvosana-matematiikka {:metadata metadata})
                                     (arvosana-biologia {:metadata metadata})
                                     (arvosana-maantieto {:metadata metadata})
                                     (arvosana-fysiikka {:metadata metadata})
                                     (arvosana-kemia {:metadata metadata})
                                     (arvosana-terveystieto {:metadata metadata})
                                     (arvosana-uskonto-tai-elamankatsomustieto {:metadata metadata})
                                     (arvosana-historia {:metadata metadata})
                                     (arvosana-yhteiskuntaoppi {:metadata metadata})
                                     (arvosana-musiikki {:metadata metadata})
                                     (arvosana-kuvataide {:metadata metadata})
                                     (arvosana-kasityo {:metadata metadata})
                                     (arvosana-liikunta {:metadata metadata})
                                     (arvosana-kotitalous {:metadata metadata})]})]})))

(defn arvosanat-peruskoulu [metadata]
  (arvosanat {:type :peruskoulu} metadata))
