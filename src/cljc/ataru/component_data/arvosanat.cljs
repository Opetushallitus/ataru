(ns ataru.component-data.arvosanat
  (:require [ataru.component-data.component :as component]
            [ataru.translations.texts :as texts]
            [schema.core :as s]
            [schema-tools.core :as st]))

(s/defschema ArvosanatSpec
  {:type (s/enum :peruskoulu)})

(s/defn arvosanat
  [{:keys [type]} :- ArvosanatSpec
   metadata]
  (let [id (case type
             :peruskoulu "arvosanat-peruskoulu")]
    (merge (component/form-section metadata)
           {:id     id
            :label  (:arvosanat texts/virkailija-texts)
            :module id})))

(defn arvosanat-peruskoulu [metadata]
  (arvosanat {:type :peruskoulu} metadata))
