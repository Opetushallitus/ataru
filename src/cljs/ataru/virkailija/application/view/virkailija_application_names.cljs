(ns ataru.virkailija.application.view.virkailija-application-names
  (:require [re-frame.core :refer [subscribe]]
            [ataru.virkailija.application.view.virkailija-application-icons :as icons]))

(defn hakukohde-and-tarjoaja-name [hakukohde-oid]
  (if-let [hakukohde-and-tarjoaja-name @(subscribe [:application/hakukohde-and-tarjoaja-name
                                                    hakukohde-oid])]
    [:<>
     (when @(subscribe [:application/hakukohde-archived? hakukohde-oid])
       [icons/archived-icon])
     [:span hakukohde-and-tarjoaja-name]]
    [:i.zmdi.zmdi-spinner.spin]))


