(ns ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-subs
  (:require [ataru.feature-config :as fc]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-rights :as kvr]
            [re-frame.core :as re-frame])
  (:require-macros [cljs.core.match :refer [match]]))

(defn- valintalaskenta-in-hakukohteet
  [db]
  (->> (get-in db [:application :selected-review-hakukohde-oids])
       (remove #(= "form" %))
       (map #(get-in db [:application :valintalaskentakoostepalvelu % :valintalaskenta]))))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/show-selection-state-dropdown?
  (fn [db _]
    (or (not (fc/feature-enabled? :kevyt-valinta))
        ;; true?, koska nil tarkoittaa ettei tietoa ole vielä ladattu
        ;; backendiltä ja nil? palauttaisi väärän positiivisen tiedon
        (every? true? (valintalaskenta-in-hakukohteet db)))))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/show-kevyt-valinta?
  (fn [db _]
    (let [haku-oid            (get-in db [:application :selected-application-and-form :application :haku])
          hakukohde-oids      (get-in db [:application :selected-review-hakukohde-oids])
          rights-by-hakukohde (get-in db [:application :selected-application-and-form :application :rights-by-hakukohde])]
      (and (fc/feature-enabled? :kevyt-valinta)
           ;; On päätetty, että kevyt valinta näkyy ainoastaan kun on yksi hakukohde valittavissa, muuten moni asia on todella epätriviaaleja toteuttaa
           (= (count hakukohde-oids) 1)
           (kvr/kevyt-valinta-rights-for-hakukohteet? hakukohde-oids rights-by-hakukohde)
           (not (get-in db [:haut haku-oid :sijoittelu]))
           ;; false?, koska nil tarkoittaa ettei tietoa ole vielä ladattu
           ;; backendiltä ja nil? palauttaisi väärän positiivisen tiedon
           (every? false? (valintalaskenta-in-hakukohteet db))))))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/valinnan-tila
  (fn [[_ application-key]]
    [(re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]])
     (re-frame/subscribe [:state-query [:application :valinta-tulos-service application-key]])])
  (fn [[hakukohde-oids valinnan-tulokset-for-application]]
    ;; Koska kevyt valinta näkyy ainoastaan yhdelle hakukohteelle, voidaan olettaa, että listassa on vain yksi alkio
    (let [hakukohde-oid (first hakukohde-oids)]
      (-> valinnan-tulokset-for-application
          (get hakukohde-oid)
          :valinnantulos
          :valinnantila))))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/julkaisun-tila
  (fn [[_ application-key]]
    [(re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]])
     (re-frame/subscribe [:state-query [:application :valinta-tulos-service application-key]])])
  (fn [[hakukohde-oids valinnan-tulokset-for-application]]
    (let [hakukohde-oid   (first hakukohde-oids)
          julkaistavissa? (-> valinnan-tulokset-for-application
                              (get hakukohde-oid)
                              :valinnantulos
                              :julkaistavissa)]
      (if julkaistavissa?
        :kevyt-valinta/julkaistu-hakijalle
        :kevyt-valinta/ei-julkaistu))))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/valintatapajono-oid
  (fn [[_ application-key]]
    [(re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]])
     (re-frame/subscribe [:state-query [:application :valinta-tulos-service application-key]])])
  (fn [[hakukohde-oids valinnan-tulokset-for-application]]
    (let [hakukohde-oid (first hakukohde-oids)]
      (-> valinnan-tulokset-for-application
          (get hakukohde-oid)
          :valinnantulos
          :valintatapajonoOid))))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/ongoing-request?
  (fn [db]
    (when-let [kevyt-valinta-property (-> db :application :kevyt-valinta :kevyt-valinta-ui/ongoing-request-for-property)]
      (let [request-id (-> db :application :kevyt-valinta kevyt-valinta-property :request-id)]
        (-> db
            :request-handles
            request-id
            nil?
            not)))))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/kevyt-valinta-property-state
  (fn [[_ _ application-key]]
    [(re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]])
     (re-frame/subscribe [:state-query [:application :valinta-tulos-service application-key]])])
  (fn [[hakukohde-oids valinnan-tulokset-for-application] [_ kevyt-valinta-property]]
    (let [hakukohde-oid       (first hakukohde-oids)
          {valinnan-tila  :valinnantila
           julkaisun-tila :julkaistavissa} (-> valinnan-tulokset-for-application
                                               (get hakukohde-oid)
                                               :valinnantulos)
          kevyt-valinta-state (match [valinnan-tila julkaisun-tila]
                                     [_ _]
                                     {:kevyt-valinta/valinnan-tila  :unchecked
                                      :kevyt-valinta/julkaisun-tila :unchecked})]
      (kevyt-valinta-state kevyt-valinta-property))))

