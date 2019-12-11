(ns ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-subs
  (:require [ataru.feature-config :as fc]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-mappings :as mappings]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-rights :as kvr]
            [re-frame.core :as re-frame])
  (:require-macros [cljs.core.match :refer [match]]))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/valintalaskenta-in-hakukohteet
  (fn []
    [(re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]])
     (re-frame/subscribe [:state-query [:application :valintalaskentakoostepalvelu]])])
  (fn [[hakukohde-oids valintalaskentakoostepalvelu]]
    (transduce (comp (remove (partial = "form"))
                     (map (fn [hakukohde-oid]
                            (-> valintalaskentakoostepalvelu
                                (get hakukohde-oid)
                                :valintalaskenta))))
               conj
               hakukohde-oids)))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/show-selection-state-dropdown?
  (fn []
    [(re-frame/subscribe [:virkailija-kevyt-valinta/valintalaskenta-in-hakukohteet])])
  (fn [[valintalaskenta-in-hakukohteet]]
    (or (not (fc/feature-enabled? :kevyt-valinta))
        ;; true?, koska nil tarkoittaa ettei tietoa ole vielä ladattu
        ;; backendiltä ja nil? palauttaisi väärän positiivisen tiedon
        (every? true? valintalaskenta-in-hakukohteet))))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/sijoittelu?
  (fn []
    [(re-frame/subscribe [:state-query [:application :selected-application-and-form :application :haku]])
     (re-frame/subscribe [:state-query [:haut]])])
  (fn [[haku-oid haut]]
    (-> haut
        (get haku-oid)
        :sijoittelu)))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/show-kevyt-valinta?
  (fn []
    [(re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]])
     (re-frame/subscribe [:state-query [:application :selected-application-and-form :application :rights-by-hakukohde]])
     (re-frame/subscribe [:virkailija-kevyt-valinta/valintalaskenta-in-hakukohteet])
     (re-frame/subscribe [:virkailija-kevyt-valinta/sijoittelu?])])
  (fn [[hakukohde-oids rights-by-hakukohde valintalaskenta-in-hakukohteet sijoittelu?]]
    (and (fc/feature-enabled? :kevyt-valinta)
         ;; On päätetty, että kevyt valinta näkyy ainoastaan kun on yksi hakukohde valittavissa, muuten moni asia on todella epätriviaaleja toteuttaa
         (= (count hakukohde-oids) 1)
         (kvr/kevyt-valinta-rights-for-hakukohteet? hakukohde-oids rights-by-hakukohde)
         (not sijoittelu?)
         ;; false?, koska nil tarkoittaa ettei tietoa ole vielä ladattu
         ;; backendiltä ja nil? palauttaisi väärän positiivisen tiedon
         (every? false? valintalaskenta-in-hakukohteet))))

(defn- default-kevyt-valinta-property-state [kevyt-valinta-property]
  (when (= kevyt-valinta-property :kevyt-valinta/valinnan-tila)
    "KESKEN"))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/hakukohde-oid
  (fn []
    [(re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]])])
  (fn [[hakukohde-oids]]
    (first hakukohde-oids)))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/valinnan-tulos-for-application
  (fn [[_ application-key]]
    [(re-frame/subscribe [:virkailija-kevyt-valinta/hakukohde-oid])
     (re-frame/subscribe [:state-query [:application :valinta-tulos-service application-key]])])
  (fn [[hakukohde-oid valinnan-tulokset-for-application]]
    (-> valinnan-tulokset-for-application
        (get hakukohde-oid)
        :valinnantulos)))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/kevyt-valinta-property-state
  (fn [[_ _ application-key]]
    [(re-frame/subscribe [:virkailija-kevyt-valinta/valinnan-tulos-for-application application-key])])
  (fn [[valinnan-tulos-for-application] [_ kevyt-valinta-property]]
    (let [valinta-tulos-service-property (mappings/kevyt-valinta-property->valinta-tulos-service-property kevyt-valinta-property)
          kevyt-valinta-property-state   (valinta-tulos-service-property valinnan-tulos-for-application)]
      (if (nil? kevyt-valinta-property-state)
        (default-kevyt-valinta-property-state kevyt-valinta-property)
        kevyt-valinta-property-state))))

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

(def ^:private kevyt-valinta-property-order
  [:kevyt-valinta/valinnan-tila
   :kevyt-valinta/julkaisun-tila
   :kevyt-valinta/vastaanotto-tila
   :kevyt-valinta/ilmoittautumisen-tila])

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

(re-frame/reg-sub
  :virkailija-kevyt-valinta/kevyt-valinta-selection-state
  (fn [[_ _ application-key]]
    [(re-frame/subscribe [:virkailija-kevyt-valinta/valinnan-tulos-for-application application-key])])
  (fn [[valinnan-tulos-for-application] [_ kevyt-valinta-property]]
    (let [{valinnan-tila         :valinnantila
           julkaisun-tila        :julkaistavissa
           vastaanotto-tila      :vastaanottotila
           ilmoittautumisen-tila :ilmoittautumistila} valinnan-tulos-for-application
          kevyt-valinta-states (match [valinnan-tila julkaisun-tila vastaanotto-tila ilmoittautumisen-tila]
                                      [_ (_ :guard nil?) (_ :guard nil?) (_ :guard nil?)]
                                      {:kevyt-valinta/valinnan-tila         :unchecked
                                       :kevyt-valinta/julkaisun-tila        :grayed-out
                                       :kevyt-valinta/vastaanotto-tila      :grayed-out
                                       :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

                                      [_ false _ _]
                                      {:kevyt-valinta/valinnan-tila         :unchecked
                                       :kevyt-valinta/julkaisun-tila        :unchecked
                                       :kevyt-valinta/vastaanotto-tila      :grayed-out
                                       :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

                                      [_ true "KESKEN" _]
                                      {:kevyt-valinta/valinnan-tila         :checked
                                       :kevyt-valinta/julkaisun-tila        :unchecked
                                       :kevyt-valinta/vastaanotto-tila      :unchecked
                                       :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

                                      [_ true (_ :guard #(not= % "KESKEN")) "EI_TEHTY"]
                                      {:kevyt-valinta/valinnan-tila         :checked
                                       :kevyt-valinta/julkaisun-tila        :checked
                                       :kevyt-valinta/vastaanotto-tila      :unchecked
                                       :kevyt-valinta/ilmoittautumisen-tila :unchecked})]
      (kevyt-valinta-states kevyt-valinta-property))))

(def ^:private not-nil? (comp not nil?))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/kevyt-valinta-checkmark-state
  (fn [[_ _ application-key]]
    [(re-frame/subscribe [:virkailija-kevyt-valinta/ongoing-request-property])
     (re-frame/subscribe [:virkailija-kevyt-valinta/valinnan-tulos-for-application application-key])])
  (fn [[ongoing-request-property valinnan-tulos-for-application] [_ kevyt-valinta-property]]
    (let [{valinnan-tila         :valinnantila
           julkaisun-tila        :julkaistavissa
           vastaanotto-tila      :vastaanottotila
           ilmoittautumisen-tila :ilmoittautumistila} valinnan-tulos-for-application
          checkmark-states (match [valinnan-tila julkaisun-tila vastaanotto-tila ilmoittautumisen-tila]
                                  [_ (_ :guard nil?) (_ :guard nil?) (_ :guard nil?)]
                                  {:kevyt-valinta/valinnan-tila         :unchecked
                                   :kevyt-valinta/julkaisun-tila        :grayed-out
                                   :kevyt-valinta/vastaanotto-tila      :grayed-out
                                   :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

                                  [_ false _ _]
                                  {:kevyt-valinta/valinnan-tila         :checked
                                   :kevyt-valinta/julkaisun-tila        :unchecked
                                   :kevyt-valinta/vastaanotto-tila      :grayed-out
                                   :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

                                  [_ true "KESKEN" _]
                                  {:kevyt-valinta/valinnan-tila         :checked
                                   :kevyt-valinta/julkaisun-tila        :checked
                                   :kevyt-valinta/vastaanotto-tila      :unchecked
                                   :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

                                  [_ true (_ :guard #(not= % "KESKEN")) "EI_TEHTY"]
                                  {:kevyt-valinta/valinnan-tila         :checked
                                   :kevyt-valinta/julkaisun-tila        :checked
                                   :kevyt-valinta/vastaanotto-tila      :checked
                                   :kevyt-valinta/ilmoittautumisen-tila :unchecked})
          checkmark-state  (checkmark-states kevyt-valinta-property)]
      (cond (and ongoing-request-property
                 (not (before? kevyt-valinta-property
                               ongoing-request-property
                               kevyt-valinta-property-order))
                 (= checkmark-state :checked))
            :unchecked

            (and ongoing-request-property
                 (before? ongoing-request-property
                          kevyt-valinta-property
                          kevyt-valinta-property-order)
                 (= checkmark-state :unchecked))
            :grayed-out

            :else
            checkmark-state))))
