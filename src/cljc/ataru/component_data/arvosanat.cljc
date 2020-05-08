(ns ataru.component-data.arvosanat
  (:require [ataru.component-data.component :as component]
            [ataru.translations.texts :as texts]
            [schema.core :as s]))

(s/defn arvosanat
  [{:keys [type]} :- {:type (s/enum :peruskoulu)}
   metadata]
  (let [id (case type
             :peruskoulu "arvosanat-peruskoulu")]
    (merge (component/form-section metadata)
           {:id       id
            :label    (:arvosanat texts/virkailija-texts)
            :module   id
            :children [(assoc (component/info-element metadata)
                              :text
                              (:arvosanat-info texts/virkailija-texts))]})))

(defn arvosanat-peruskoulu [metadata]
  (arvosanat {:type :peruskoulu} metadata))
