(ns ataru.hakija.arvosanat.components.valinnainen-kieli-label
  (:require [ataru.hakija.arvosanat.valinnainen-oppiaine-koodi :as vok]
            [ataru.schema.lang-schema :as lang-schema]
            [ataru.translations.texts :as texts]
            [re-frame.core :as re-frame]
            [schema.core :as s]))

(s/defn valinnainen-kieli-label
        [{:keys [field-descriptor
                 idx
                 lang]} :- {:field-descriptor s/Any
                            :idx              s/Int
                            :lang             lang-schema/Lang}]
        (let [answer @(re-frame/subscribe [:application/answer
                                           (:id field-descriptor)
                                           idx])
              label  (as-> (:value answer) key
                           (subs key vok/valinnainen-kieli-id-oppiaine-koodi-idx)
                           (str "oppiaine-" key)
                           (keyword key)
                           (-> texts/oppiaine-translations key lang))]
          [:span label]))
