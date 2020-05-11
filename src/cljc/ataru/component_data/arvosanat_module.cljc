(ns ataru.component-data.arvosanat-module
  (:require [ataru.component-data.component :as component]
            [ataru.translations.texts :as texts]
            [schema.core :as s]))

(s/defschema OppiaineenKoodi
  (s/enum "A"))

(s/defschema OppiaineenArvosana
  {:fieldClass (s/eq "oppiaineenArvosana")
   :fieldType  OppiaineenKoodi})

(s/defn oppiaineen-arvosana
  [{:keys [oppiaineen-koodi]} :- {:oppiaine OppiaineenKoodi}]
  {:fieldClass "oppiaineenArvosana"
   :fieldType  oppiaineen-koodi})

(def ^:private aidinkielen-arvosana
  (oppiaineen-arvosana
    {:oppiaineen-koodi "A"}))

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
                         (:arvosanat-info texts/virkailija-texts))]})))

(defn arvosanat-peruskoulu [metadata]
  (arvosanat {:type :peruskoulu} metadata))
