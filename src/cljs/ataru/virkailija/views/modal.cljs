(ns ataru.virkailija.views.modal
  (:require [ataru.cljs-util :as cljs-util]))

(defn- modal
  [close-handler content]
  [:div.virkailija-modal__backdrop
   [:div.virkailija-modal__container
    [:div.virkailija-modal__close-link-container
     [:button.virkailija-close-button
      {:on-click close-handler}
      [:i.zmdi.zmdi-close]]]
    [:div.virkailija-modal__content-container
     content]]])
