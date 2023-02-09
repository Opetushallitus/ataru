(ns ataru.virkailija.application.attachments.attachments-tab-view
  (:require [re-frame.core :refer [subscribe]]
            [ataru.hakukohde.liitteet :refer [format-attachment-address]]))

(defn- form-hakukohde-name
  [lang hakukohde]
  (str (@lang (:name hakukohde))
       ", "
       (@lang (:tarjoaja hakukohde)))
  )

(defn- form-address
  [lang address]
  (str (format-attachment-address @lang address)
       (when (some? (:verkkosivu address))
         (str "Tai käytä: "
              (:verkkosivu address)))))

(defn- liite-info
  [lang liite]
  [:div
   [:p.attachments-tab__content__hakukohde (form-hakukohde-name lang (:hakukohde liite))]
   [:ul
    [:li (form-address lang (:toimitusosoite liite))]
    [:li (str "Palautettava viimeistään "
              (@lang (:toimitusaika liite)))]]])

(defn attachments-tab-view []
  (let [liitteet @(subscribe [:virkailija-attachments/liitepyynnot-hakemuksen-hakutoiveille])
        lang (subscribe [:editor/virkailija-lang])]
    [:div.attachments-tab
     [:div.attachments-tab__left-panel
      [:h2 @(subscribe [:editor/virkailija-translation :attachments-tab-header])]]
     [:div.attachments-tab__right-panel
      (doall
        (for [liitegroup (keys liitteet)]
          ^{:key liitegroup}
          [:div.attachments-tab__content
           (prn (get liitteet liitegroup))
           [:h3.attachments-tab__content__type (@lang (:tyyppi-label (first (get liitteet liitegroup))))]
           (for [liite (get liitteet liitegroup)]
             ^{:key (get-in liite [:hakukohde :oid])}
             [liite-info lang liite])]))]]))