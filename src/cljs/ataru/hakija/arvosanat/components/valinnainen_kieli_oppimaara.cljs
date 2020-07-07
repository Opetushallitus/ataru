(ns ataru.hakija.arvosanat.components.valinnainen-kieli-oppimaara
  (:require [ataru.hakija.arvosanat.valinnainen-oppiaine-koodi :as vok]
            [ataru.hakija.schema.render-field-schema :as render-field-schema]
            [ataru.translations.texts :as texts]
            [re-frame.core :as re-frame]
            [schema.core :as s]))

(s/defn valinnainen-kieli-oppimaara
        [{:keys [field-descriptor
                 render-field
                 idx]} :- render-field-schema/RenderFieldArgs]
        (let [oppiaine (some-> @(re-frame/subscribe [:application/answer
                                                     :oppiaine-valinnainen-kieli
                                                     idx])
                               :value
                               (subs vok/valinnainen-kieli-id-oppiaine-koodi-idx))
              label    (if (= oppiaine "a")
                         (:oppimaara texts/translation-mapping)
                         (:oppiaine texts/translation-mapping))]
          [render-field
           (assoc field-descriptor :unselected-label label)
           idx]))
