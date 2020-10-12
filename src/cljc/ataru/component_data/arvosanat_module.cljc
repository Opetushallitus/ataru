(ns ataru.component-data.arvosanat-module
  (:require [ataru.component-data.component :as component]
            [ataru.schema.element-metadata-schema :as element-metadata-schema]
            [ataru.schema.localized-schema :as localized-schema]
            [ataru.translations.texts :as texts]
            [schema.core :as s]
            [clojure.string :as string]))

(def ArvosanatVersio
  (s/eq "oppiaineen-arvosanat"))

(s/defschema OppiaineenKoodi
  (s/enum "A"
          "A1"
          "A2"
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
          "KO"
          "valinnainen-kieli"))

(s/defschema OppiaineenArvosana
  {:fieldClass                (s/eq "questionGroup")
   :fieldType                 (s/eq "fieldset")
   :id                        OppiaineenKoodi
   :label                     localized-schema/LocalizedString
   :metadata                  element-metadata-schema/ElementMetadata
   :version                   ArvosanatVersio
   :params                    {}
   (s/optional-key :children) [s/Any]})

(def ArvosanatTaulukkoChildren
  (s/conditional
    #(-> % :id (= "oppiaineen-arvosanat-valinnaiset-kielet"))
    s/Any
    :else
    OppiaineenArvosana))

(s/defschema ArvosanatTaulukko
  {:id              s/Str
   :fieldClass      (s/eq "wrapperElement")
   :fieldType       (s/eq "fieldset")
   :children        [ArvosanatTaulukkoChildren]
   :child-validator (s/eq :oppiaine-a1-or-a2-component)
   :metadata        element-metadata-schema/ElementMetadata
   :label           localized-schema/LocalizedString
   :version         ArvosanatVersio
   :params          {}})

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

(s/defn oppimaara-dropdown
  [{:keys [oppiaineen-koodi
           metadata
           options]} :- {:oppiaineen-koodi OppiaineenKoodi
                         :metadata         element-metadata-schema/ElementMetadata
                         :options          [{:label localized-schema/LocalizedString
                                             :value s/Str}]}]
  (let [label-kwd (if (= oppiaineen-koodi "A")
                    :oppimaara
                    :oppiaine)]
    {:fieldClass       "formField"
     :fieldType        "dropdown"
     :version          "generic"
     :label            (concat-labels
                         ": "
                         (label-kwd texts/translation-mapping)
                         (oppiaine-label oppiaineen-koodi))
     :unselected-label (label-kwd texts/translation-mapping)
     :options          options
     :metadata         metadata}))

(s/defn dropdown-option
  [{:keys [label
           value]} :- {:label localized-schema/LocalizedString
                       :value s/Str}]
  (merge (component/dropdown-option value)
         {:label label
          :value value}))

(s/defn arvosana-dropdown
  [{:keys [oppiaineen-koodi
           metadata
           label]} :- {:metadata         element-metadata-schema/ElementMetadata
                       :oppiaineen-koodi OppiaineenKoodi
                       :label            localized-schema/LocalizedString}]
  (merge (component/dropdown metadata)
         {:version          "generic"
          :id               (str "arvosana-" oppiaineen-koodi)
          :unselected-label (:arvosana texts/translation-mapping)
          :label            label
          :options          (conj (->> (range 4 11)
                                       reverse
                                       (map str)
                                       (mapv (fn [arvosana]
                                               (dropdown-option
                                                 {:label {:fi arvosana
                                                          :sv arvosana
                                                          :en arvosana}
                                                  :value (str "arvosana-" oppiaineen-koodi "-" arvosana)}))))
                                  (dropdown-option
                                    {:label (:hyvaksytty-s texts/translation-mapping)
                                     :value (str "arvosana-" oppiaineen-koodi "-hyvaksytty")})
                                  (dropdown-option
                                    {:label (:osallistunut-o texts/translation-mapping)
                                     :value (str "arvosana-" oppiaineen-koodi "-osallistunut")})
                                  (dropdown-option
                                    {:label (:ei-arvosanaa texts/translation-mapping)
                                     :value (str "arvosana-" oppiaineen-koodi "-ei-arvosanaa")}))
          :metadata         metadata
          :validators       ["required"]}))

(s/defn oppimaara-aidinkieli-ja-kirjallisuus
  [{:keys [metadata
           oppiaineen-koodi
           valinnainen-oppiaine?]} :- {:metadata              element-metadata-schema/ElementMetadata
                                       :oppiaineen-koodi      OppiaineenKoodi
                                       :valinnainen-oppiaine? s/Bool}]
  (merge
    (oppimaara-dropdown
      {:oppiaineen-koodi oppiaineen-koodi
       :metadata         metadata
       :options          [{:label (:suomi-aidinkielena texts/translation-mapping)
                           :value "suomi-aidinkielena"}
                          {:label (:suomi-toisena-kielena texts/translation-mapping)
                           :value "suomi-toisena-kielena"}
                          {:label (:suomi-viittomakielisille texts/translation-mapping)
                           :value "suomi-viittomakielisille"}
                          {:label (:suomi-saamenkielisille texts/translation-mapping)
                           :value "suomi-saamenkielisille"}
                          {:label (:ruotsi-aidinkielena texts/translation-mapping)
                           :value "ruotsi-aidinkielena"}
                          {:label (:ruotsi-toisena-kielena texts/translation-mapping)
                           :value "ruotsi-toisena-kielena"}
                          {:label (:ruotsi-viittomakielisille texts/translation-mapping)
                           :value "ruotsi-viittomakielisille"}
                          {:label (:saame-aidinkielena texts/translation-mapping)
                           :value "saame-aidinkielena"}
                          {:label (:romani-aidinkielena texts/translation-mapping)
                           :value "romani-aidinkielena"}
                          {:label (:viittomakieli-aidinkielena texts/translation-mapping)
                           :value "viittomakieli-aidinkielena"}
                          {:label (:muu-oppilaan-aidinkieli texts/translation-mapping)
                           :value "muu-oppilaan-aidinkieli"}]})
    (cond-> {:validators [(if valinnainen-oppiaine?
                            "required-valinnainen-oppimaara"
                            "required")]
             :id         (cond-> (str "oppimaara-a")
                                 (= oppiaineen-koodi "valinnainen-kieli")
                                 (str "-valinnainen-kieli"))}
            valinnainen-oppiaine?
            (assoc :rules {:set-oppiaine-valinnainen-kieli-value nil}))))

(s/defn oppiaine-valinnainen-kieli-dropdown
  [{:keys [metadata
           b3-kieli?]} :- {:metadata  element-metadata-schema/ElementMetadata
                           :b3-kieli? s/Bool}]
  (merge (component/dropdown metadata)
         {:version               "generic"
          :id                    (str "oppiaine-valinnainen-kieli")
          :label                 (:oppiaine texts/translation-mapping)
          :unselected-label      (:lisaa-kieli texts/translation-mapping)
          :unselected-label-icon [:i.zmdi.zmdi-plus-circle-o.arvosana__lisaa-valinnaisaine--ikoni.arvosana__lisaa-valinnainen-kieli--ikoni]
          :options               (as-> [(dropdown-option {:label (:oppiaine-a1 texts/oppiaine-translations)
                                                          :value "oppiaine-valinnainen-kieli-a1"})
                                        (dropdown-option {:label (:oppiaine-a2 texts/oppiaine-translations)
                                                          :value "oppiaine-valinnainen-kieli-a2"})
                                        (dropdown-option {:label (:oppiaine-b2 texts/oppiaine-translations)
                                                          :value "oppiaine-valinnainen-kieli-b2"})]
                                       options

                                       (cond-> options
                                               b3-kieli?
                                               (conj (dropdown-option {:label (:oppiaine-b3 texts/oppiaine-translations)
                                                                       :value "oppiaine-valinnainen-kieli-b3"})))

                                       (conj options (dropdown-option {:label (:oppiaine-a texts/oppiaine-translations)
                                                                       :value "oppiaine-valinnainen-kieli-a"})))
          :rules                 {:set-oppiaine-valinnainen-kieli-value nil}}))

(s/defn oppiaineen-arvosana :- OppiaineenArvosana
  [{:keys [oppiaineen-koodi
           label
           oppimaara-column
           metadata]} :- {:oppiaineen-koodi                  OppiaineenKoodi
                          :label                             localized-schema/LocalizedString
                          :metadata                          element-metadata-schema/ElementMetadata
                          (s/optional-key :oppimaara-column) s/Any}]
  (merge (component/question-group metadata)
         {:id       oppiaineen-koodi
          :version  "oppiaineen-arvosanat"
          :label    label
          :metadata metadata
          :children (as-> [] children
                          (cond-> children
                                  (some? oppimaara-column)
                                  (conj oppimaara-column))
                          (conj children (arvosana-dropdown
                                           {:metadata         metadata
                                            :oppiaineen-koodi oppiaineen-koodi
                                            :label            (concat-labels
                                                                ": "
                                                                (:arvosana texts/translation-mapping)
                                                                (oppiaine-label oppiaineen-koodi))})))}))

(s/defn oppiaine-kieli
  [{:keys [metadata
           oppiaineen-koodi]} :- {:metadata         element-metadata-schema/ElementMetadata
                                  :oppiaineen-koodi OppiaineenKoodi}]
  (merge
    (oppimaara-dropdown
      {:oppiaineen-koodi oppiaineen-koodi
       :metadata         metadata
       :options          []})
    {:validators      ["required"]
     :rules           {:set-oppiaine-valinnainen-kieli-value nil}
     :koodisto-source {:title "Kielikoodisto, opetushallinto" :uri "kielivalikoima" :version 1}
     :sort-by-label   true
     :id              (str "oppimaara-kieli-" oppiaineen-koodi)}))

(defn- valinnaiset-kielet [{:keys [metadata
                                   b3-kieli?]}]
  (merge (component/question-group metadata)
         {:id       "oppiaineen-arvosanat-valinnaiset-kielet"
          :version  "oppiaineen-arvosanat"
          :children [(oppiaine-valinnainen-kieli-dropdown {:metadata  metadata
                                                           :b3-kieli? b3-kieli?})
                     (oppimaara-aidinkieli-ja-kirjallisuus {:metadata              metadata
                                                            :oppiaineen-koodi      "valinnainen-kieli"
                                                            :valinnainen-oppiaine? true})
                     (oppiaine-kieli {:metadata metadata
                                      :oppiaineen-koodi "valinnainen-kieli"})
                     (arvosana-dropdown
                       {:metadata         metadata
                        :oppiaineen-koodi "valinnainen-kieli"
                        :label            (concat-labels
                                            ": "
                                            (:arvosana texts/translation-mapping)
                                            (oppiaine-label "valinnainen-kieli"))})]}))

(defn- arvosana-aidinkieli-ja-kirjallisuus [{:keys [metadata]}]
  (oppiaineen-arvosana
    {:oppiaineen-koodi "A"
     :label            (:arvosana-aidinkieli-ja-kirjallisuus texts/virkailija-texts)
     :oppimaara-column (oppimaara-aidinkieli-ja-kirjallisuus {:metadata              metadata
                                                              :oppiaineen-koodi      "A"
                                                              :valinnainen-oppiaine? false})
     :metadata         metadata}))

(defn- arvosana-a1-kieli [{:keys [metadata]}]
  (let [oppiaineen-koodi "A1"]
    (oppiaineen-arvosana
      {:oppiaineen-koodi oppiaineen-koodi
       :label            (:arvosana-a1-kieli texts/virkailija-texts)
       :oppimaara-column (oppiaine-kieli {:metadata         metadata
                                          :oppiaineen-koodi oppiaineen-koodi})
       :metadata         metadata})))

(defn- arvosana-a2-kieli [{:keys [metadata]}]
  (let [oppiaineen-koodi "A2"]
    (oppiaineen-arvosana
      {:oppiaineen-koodi oppiaineen-koodi
       :label            (:arvosana-a2-kieli texts/virkailija-texts)
       :oppimaara-column (oppiaine-kieli {:metadata         metadata
                                          :oppiaineen-koodi oppiaineen-koodi})
       :metadata         metadata})))

(defn- arvosana-b1-kieli [{:keys [metadata]}]
  (let [oppiaineen-koodi "B1"]
    (oppiaineen-arvosana
      {:oppiaineen-koodi oppiaineen-koodi
       :label            (:arvosana-b1-kieli texts/virkailija-texts)
       :oppimaara-column (oppiaine-kieli {:metadata         metadata
                                          :oppiaineen-koodi oppiaineen-koodi})
       :metadata         metadata})))

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
                          :children [ArvosanatTaulukkoChildren]}]
  (merge (component/form-section metadata)
         {:id              "arvosanat-taulukko"
          :version         "oppiaineen-arvosanat"
          :children        children
          :child-validator :oppiaine-a1-or-a2-component}))

(s/defn arvosanat
  [{:keys [type]} :- {:type (s/enum :peruskoulu :lukio)}
   metadata]
  (let [id        (case type
                    :peruskoulu "arvosanat-peruskoulu"
                    :lukio "arvosanat-lukio")
        label-kwd (case type
                    :peruskoulu :arvosanat-peruskoulu
                    :lukio :arvosanat-lukio)
        b3-kieli? (= type :lukio)]
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
                                     (arvosana-a2-kieli {:metadata metadata})
                                     (arvosana-b1-kieli {:metadata metadata})
                                     (valinnaiset-kielet {:metadata  metadata
                                                          :b3-kieli? b3-kieli?})
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

(defn arvosanat-lukio [metadata]
  (arvosanat {:type :lukio} metadata))
