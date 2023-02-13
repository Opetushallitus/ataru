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
  (let [formatted-address (format-attachment-address @lang address)
        address-string (str formatted-address
                            (when (and formatted-address (:verkkosivu address))
                                   @(subscribe [:editor/virkailija-translation :or-use]))
                            (when (and (nil? formatted-address) (:verkkosivu address))
                                  @(subscribe [:editor/virkailija-translation :use])))]
    [:span address-string
     (when (:verkkosivu address)
       [:a {:href (:verkkosivu address)}
        (:verkkosivu address)])]))

(defn- liite-info
  [lang liite]
  [:div
   [:p.attachments-tab__content__hakukohde (form-hakukohde-name lang (:hakukohde liite))]
   [:ul
    [:li (form-address lang (:toimitusosoite liite))]
    [:li (str @(subscribe [:editor/virkailija-translation :return-latest])
                          " "
                           (@lang (:toimitusaika liite)))]]])

(defn attachments-tab-view []
  (let [liitteet @(subscribe [:virkailija-attachments/liitepyynnot-hakemuksen-hakutoiveille])
        lang (subscribe [:editor/virkailija-lang])]
    [:div.attachments-tab
     [:div.attachments-tab__left-panel
      [:h2 @(subscribe [:editor/virkailija-translation :attachments-tab-header])]]
     [:div.attachments-tab__right-panel
      [:div.attachments-tab__info
       [:div.attachments-tab__info__icon
        [:i.zmdi.zmdi-alert-circle-o]]
       [:p.attachments-tab__info__text @(subscribe [:editor/virkailija-translation :attachments-tab-info])]]
      (doall
        (for [liitegroup (keys liitteet)]
          ^{:key liitegroup}
          [:div.attachments-tab__content
           [:h3.attachments-tab__content__type (@lang (:tyyppi-label (first (get liitteet liitegroup))))]
           (for [liite (get liitteet liitegroup)]
             ^{:key (get-in liite [:hakukohde :oid])}
             [liite-info lang liite])]))]]))