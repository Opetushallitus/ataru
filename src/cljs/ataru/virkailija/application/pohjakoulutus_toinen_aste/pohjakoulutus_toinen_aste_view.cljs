(ns ataru.virkailija.application.pohjakoulutus-toinen-aste.pohjakoulutus-toinen-aste-view
  (:require [re-frame.core :refer [subscribe]]))

(defn-
  labeled-value
  [label value]
  [:div.application__form-field
   [:label.application__form-field-label @(subscribe [:editor/virkailija-translation label])]
   [:div.application__form-field-value value]])

(defn- lisapistekoulutukset-component
  [lisapistekoulutukset]
  (into [:ul]
    (for [lisapistekoulutus lisapistekoulutukset]
      [:li @(subscribe [:editor/virkailija-translation (keyword lisapistekoulutus)])])))

(defn pohjakoulutus-for-valinnat
  []
  (let [lang                 @(subscribe [:editor/virkailija-lang])
        pohjakoulutus        @(subscribe [:application/pohjakoulutus-for-valinnat])
        pohjakoulutus-choice (:pohjakoulutus pohjakoulutus)
        opetuskieli          (:opetuskieli pohjakoulutus)
        suoritusvuosi        (:suoritusvuosi pohjakoulutus)
        lisapistekoulutukset (:lisapistekoulutukset pohjakoulutus)]
    [:<>
     [:span.application__wrapper-side-content-title @(subscribe [:editor/virkailija-translation :pohjakoulutus-for-valinnat])]
     (when pohjakoulutus-choice
       [labeled-value :base-education (lang (:label pohjakoulutus-choice))])
     (when opetuskieli
       [labeled-value :pohjakoulutus-opetuskieli (lang (:label opetuskieli))])
     (when suoritusvuosi
       [labeled-value :pohjakoulutus-suoritusvuosi suoritusvuosi])
     (when lisapistekoulutukset
       [labeled-value :lisapistekoulutukset [lisapistekoulutukset-component lisapistekoulutukset]])]))
