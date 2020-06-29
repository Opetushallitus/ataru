(ns ataru.virkailija.application.view.names
  (:require [re-frame.core :refer [subscribe]]))

(defn hakukohde-and-tarjoaja-name [hakukohde-oid]
  (if-let [hakukohde-and-tarjoaja-name @(subscribe [:application/hakukohde-and-tarjoaja-name
                                                    hakukohde-oid])]
    [:span hakukohde-and-tarjoaja-name]
    [:i.zmdi.zmdi-spinner.spin]))


