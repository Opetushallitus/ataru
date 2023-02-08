(ns ataru.virkailija.application.attachments.attachments-tab-view
  (:require [re-frame.core :refer [subscribe]]))

(defn attachments-tab-view []
  (let [liitteet @(subscribe [:virkailija-attachments/liitepyynnot-hakemuksen-hakutoiveille])]
    [:div.grades
     [:div.grades__left-panel
      [:h2 @(subscribe [:editor/virkailija-translation :attachments-tab-header])]]
     [:div.grades__right-panel
      (doall
        (for [liitegroup (keys liitteet)]
          ^{:key liitegroup}
          [:div.grade
           (prn (get liitteet liitegroup))
           [:span.grade__subject liitegroup]
           (for [liite (get liitteet liitegroup)]
             ^{:key (get-in liite [:hakukohde :oid])}
             [:span.grade__value (get-in liite [:hakukohde :name :fi]) ", " (get-in liite [:hakukohde :tarjoaja :fi])])]))]]))