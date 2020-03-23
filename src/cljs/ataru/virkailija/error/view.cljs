(ns ataru.virkailija.error.view
  (:require [re-frame.core :refer [subscribe]]))

(defn error []
  [:div
   [:div.editor-form__container.panel-content
    [:div.editor-form__form-header-row
     [:h1.editor-form__form-heading @(subscribe [:editor/virkailija-translation :odottamaton-virhe-otsikko])]
     ]
    [:div @(subscribe [:editor/virkailija-translation :odottamaton-virhe-aputeksti])]]])
