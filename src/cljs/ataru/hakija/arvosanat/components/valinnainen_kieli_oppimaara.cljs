(ns ataru.hakija.arvosanat.components.valinnainen-kieli-oppimaara
  (:require [ataru.hakija.arvosanat.valinnainen-oppiaine-koodi :as vok]
            [ataru.hakija.schema.render-field-schema :as render-field-schema]
            [ataru.translations.texts :as texts]
            [re-frame.core :as re-frame]
            [schema.core :as s]
            [schema-tools.core :as st]
            [ataru.schema.lang-schema :as lang-schema]))

(s/defn valinnainen-kieli-oppimaara
        [{:keys [field-descriptor
                 render-field
                 application
                 lang
                 idx
                 read-only?]} :- (-> render-field-schema/RenderFieldArgs
                                 (st/merge {:lang        lang-schema/Lang
                                            :application s/Any
                                            :read-only?  s/Bool}))]
        (let [oppiaine (some-> @(re-frame/subscribe [:application/answer
                                                     :oppiaine-valinnainen-kieli
                                                     idx])
                               :value
                               (subs vok/valinnainen-kieli-id-oppiaine-koodi-idx))
              label    (if (= oppiaine "a")
                         (:oppimaara texts/translation-mapping)
                         (:oppiaine texts/translation-mapping))
              field-descriptor-with-label (assoc field-descriptor :unselected-label label)]
          (if read-only?
            [render-field
             field-descriptor-with-label
             application
             lang
             idx]
            [render-field
             field-descriptor-with-label
             idx])))
