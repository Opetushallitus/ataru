(ns ataru.virkailija.application.pohjakoulutus-toinen-aste.pohjakoulutus-toinen-aste-view
  (:require [re-frame.core :refer [subscribe]]))

(defn- labeled-value
  [label value]
  [:div.application__form-field
   [:label.application__form-field-label @(subscribe [:editor/virkailija-translation label])]
   [:div.application__form-field-value value]])

(defn- harkinnanvarainen-component
  []
  [:div.harkinnanvaraisuus__wrapper
   [:i.zmdi.zmdi-alert-triangle]
   [:span @(subscribe [:editor/virkailija-translation :only-harkinnanvarainen-valinta])]])

(defn- lisapistekoulutukset-component
  [lisapistekoulutukset]
  (into [:ul]
    (for [lisapistekoulutus lisapistekoulutukset]
      [:li @(subscribe [:editor/virkailija-translation (keyword lisapistekoulutus)])])))

(defn- loading-indicator
  []
  [:i.zmdi.zmdi-spinner.spin])

(defn- not-found
  []
  [:p @(subscribe [:editor/virkailija-translation :pohjakoulutus-not-found])])

(defn- error-loading
  []
  [:p @(subscribe [:editor/virkailija-translation :error-loading-pohjakoulutus])])

(defn- pohjakoulutus-for-valinnat-loaded
  []
  (let [lang                 @(subscribe [:editor/virkailija-lang])
        pohjakoulutus        @(subscribe [:application/pohjakoulutus-for-valinnat])
        pohjakoulutus-choice (:pohjakoulutus pohjakoulutus)
        opetuskieli          (:opetuskieli pohjakoulutus)
        suoritusvuosi        (:suoritusvuosi pohjakoulutus)
        lisapistekoulutukset (:lisapistekoulutukset pohjakoulutus)]
    [:<>
     (when pohjakoulutus-choice
       [labeled-value :base-education (lang (:label pohjakoulutus-choice))])
     (when opetuskieli
       [labeled-value :pohjakoulutus-opetuskieli (lang (:label opetuskieli))])
     (when suoritusvuosi
       [labeled-value :pohjakoulutus-suoritusvuosi suoritusvuosi])
     (when lisapistekoulutukset
       [labeled-value :lisapistekoulutukset [lisapistekoulutukset-component lisapistekoulutukset]])]))

(defn pohjakoulutus-for-valinnat
  []
  (let [harkinnanvarainen?   @(subscribe [:application/harkinnanvarainen-pohjakoulutus?])
        yksilollistetty?     @(subscribe [:application/yksilollistetty-matikka-aikka?])]
  [:<>
   [:span.application__wrapper-side-content-title
    @(subscribe [:editor/virkailija-translation :pohjakoulutus-for-valinnat])]
   (when harkinnanvarainen?
     [harkinnanvarainen-component])
   (case @(subscribe [:application/pohjakoulutus-for-valinnat-loading-state])
     :loading [loading-indicator]
     :loaded [pohjakoulutus-for-valinnat-loaded]
     :not-found [not-found]
     :error [error-loading])
   (when yksilollistetty?
     [labeled-value :pohjakoulutus-yksilollistetty @(subscribe [:editor/virkailija-translation :yes])])]))
