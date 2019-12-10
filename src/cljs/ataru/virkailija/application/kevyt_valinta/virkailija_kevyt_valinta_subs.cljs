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
      (or (-> valinnan-tulokset-for-application
              (get hakukohde-oid)
              :valinnantulos
              :valinnantila)
          "KESKEN"))))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/julkaisun-tila
  (fn [[_ application-key]]
    [(re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]])
     (re-frame/subscribe [:state-query [:application :valinta-tulos-service application-key]])])
  (fn [[hakukohde-oids valinnan-tulokset-for-application]]
    (let [hakukohde-oid (first hakukohde-oids)]
      (-> valinnan-tulokset-for-application
          (get hakukohde-oid)
          :valinnantulos
          :julkaistavissa))))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/vastaanotto-tila
  (fn [[_ application-key]]
    [(re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]])
     (re-frame/subscribe [:state-query [:application :valinta-tulos-service application-key]])])
  (fn [[hakukohde-oids valinnan-tulokset-for-application]]
    (let [hakukohde-oid (first hakukohde-oids)]
      (-> valinnan-tulokset-for-application
          (get hakukohde-oid)
          :valinnantulos
          :vastaanottotila))))

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
  :virkailija-kevyt-valinta/ongoing-request-property
  (fn [db]
    (when-let [kevyt-valinta-property (-> db :application :kevyt-valinta :kevyt-valinta-ui/ongoing-request-for-property)]
      (let [request-id (-> db :application :kevyt-valinta kevyt-valinta-property :request-id)]
        (when (-> db
                  :request-handles
                  request-id
                  nil?
                  not)
          kevyt-valinta-property)))))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/kevyt-valinta-property-state
  (fn [[_ _ application-key]]
    [(re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]])
     (re-frame/subscribe [:state-query [:application :valinta-tulos-service application-key]])])
  (fn [[hakukohde-oids valinnan-tulokset-for-application] [_ kevyt-valinta-property]]
    (let [hakukohde-oid       (first hakukohde-oids)
          {valinnan-tila    :valinnantila
           julkaisun-tila   :julkaistavissa
           vastaanotto-tila :vastaanottotila} (-> valinnan-tulokset-for-application
                                                  (get hakukohde-oid)
                                                  :valinnantulos)
          kevyt-valinta-state (match [valinnan-tila julkaisun-tila vastaanotto-tila]
                                     [_ false _]
                                     {:kevyt-valinta/valinnan-tila    :unchecked
                                      :kevyt-valinta/julkaisun-tila   :unchecked
                                      :kevyt-valinta/vastaanotto-tila :grayed-out}

                                     [(:or "HYLATTY" "VARALLA" "PERUUNTUNUT") true _]
                                     {:kevyt-valinta/valinnan-tila    :unchecked
                                      :kevyt-valinta/julkaisun-tila   :unchecked
                                      :kevyt-valinta/vastaanotto-tila :grayed-out}

                                     [(:or "VARASIJALTA_HYVAKSYTTY" "HYVAKSYTTY" "PERUNUT" "PERUUTETTU") true "KESKEN"]
                                     {:kevyt-valinta/valinnan-tila    :unchecked
                                      :kevyt-valinta/julkaisun-tila   :unchecked
                                      :kevyt-valinta/vastaanotto-tila :unchecked}

                                     [(:or "VARASIJALTA_HYVAKSYTTY" "HYVAKSYTTY" "PERUNUT" "PERUUTETTU") true (_ :guard #(not= % "KESKEN"))]
                                     {:kevyt-valinta/valinnan-tila    :checked
                                      :kevyt-valinta/julkaisun-tila   :checked
                                      :kevyt-valinta/vastaanotto-tila :unchecked})]
      (kevyt-valinta-state kevyt-valinta-property))))

(def ^:private kevyt-valinta-property-order
  [:kevyt-valinta/valinnan-tila
   :kevyt-valinta/julkaisun-tila
   :kevyt-valinta/vastaanotto-tila])

(defn- before? [a b coll]
  "Testaa onko a ennen b:tä annetussa coll:ssa iteroimatta turhia"
  (and (not= a b)
       (->> coll
            (partition-all 2 1)
            (map (fn [[a' b']]
                   (cond (and (= a' a)
                              b')
                         true

                         (and (= a' b)
                              (= b' a))
                         false)))
            (filter (comp not nil?))
            (first))))

(def ^:private not-nil? (comp not nil?))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/kevyt-valinta-checkmark-state
  (fn [[_ kevyt-valinta-property application-key]]
    [(re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-property-state kevyt-valinta-property application-key])
     (re-frame/subscribe [:virkailija-kevyt-valinta/ongoing-request-property])])
  (fn [[kevyt-valinta-property-state ongoing-request-property] [_ kevyt-valinta-property]]
    (let [ongoing-request? (and ongoing-request-property
                                (not (before? kevyt-valinta-property
                                              ongoing-request-property
                                              kevyt-valinta-property-order)))]
      (match [kevyt-valinta-property-state ongoing-request?]
             [:checked true]
             :unchecked

             [:checked _]
             :checked

             [:unchecked _]
             :unchecked

             [:grayed-out _]
             :grayed-out))))
