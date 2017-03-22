(ns ataru.virkailija.application.application-search-control
  (:require
    [re-frame.core :refer [subscribe dispatch]]))

(defn tab-row []
  [:div.application__search-control-tab-row
   [:div.application__search-control-tab-selector "Käsittelemättä olevat haut"]
   [:div.application__search-control-tab-selector "Käsitellyt haut"]])

(defn application-search-control []
  [:div.application-handling__content-wrapper
   [tab-row]])
