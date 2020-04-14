(ns ataru.virkailija.application.hyvaksynnan-ehto.subs
  (:require [re-frame.core :as re-frame]
            [ataru.util :as util]
            [ataru.virkailija.application.hyvaksynnan-ehto.hyvaksynnan-ehto-xforms :as hx]))

(defn- hyvaksynnan-ehto-has-request-in-flight? [[_ hyvaksynnan-ehto]]
  (-> hyvaksynnan-ehto :request-in-flight? true?))

(defn- hyvaksynnan-ehto-error [[_ hyvaksynnan-ehto]]
  (-> hyvaksynnan-ehto :error))

(re-frame/reg-sub
  :hyvaksynnan-ehto/requests-in-flight?
  (fn [[_ application-key]]
    [(re-frame/subscribe [:state-query [:hyvaksynnan-ehto application-key]])
     (re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]])])
  (fn [[hyvaksynnan-ehdot hakukohde-oids]]
    (->> hyvaksynnan-ehdot
         (into []
               (comp (hx/filter-hyvaksynnan-ehdot-for-correct-hakukohde hakukohde-oids)
                     (filter hyvaksynnan-ehto-has-request-in-flight?)))
         seq
         nil?
         not)))

(re-frame/reg-sub
  :hyvaksynnan-ehto/errors
  (fn [[_ application-key]]
    [(re-frame/subscribe [:state-query [:hyvaksynnan-ehto application-key]])
     (re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]])])
  (fn [[hyvaksynnan-ehdot hakukohde-oids]]
    (into []
          (comp (hx/filter-hyvaksynnan-ehdot-for-correct-hakukohde hakukohde-oids)
                (map hyvaksynnan-ehto-error)
                (filter (comp not nil?)))
          hyvaksynnan-ehdot)))

(re-frame/reg-sub
  :hyvaksynnan-ehto/valintatapajonoissa
  (fn [db [_ application-key]]
    (some->> (get-in db [:hyvaksynnan-ehto application-key])
             vals
             (map :valintatapajonoissa)
             (mapcat identity)
             (sort-by (fn [[oid _]]
                        (get-in db [:valintatapajono oid :priority] 0)))
             (mapv (fn [[oid ehto]]
                     [(get-in db [:valintatapajono oid :name] oid)
                      (:ehto-text ehto)])))))

(re-frame/reg-sub
  :hyvaksynnan-ehto/rights
  (fn [db _]
    (->> (get-in db [:application :selected-application-and-form :application :rights-by-hakukohde])
         (map second)
         (apply clojure.set/union))))

(re-frame/reg-sub
  :hyvaksynnan-ehto/ehdollisesti-hyvaksyttavissa?
  (fn [[_ application-key]]
    [(re-frame/subscribe [:state-query [:hyvaksynnan-ehto application-key]])
     (re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]])])
  (fn [[hyvaksynnan-ehdot hakukohde-oids]]
    (let [hakukohteet-ehdollisesti-hyvaksyttavissa? (->> hyvaksynnan-ehdot
                                                         (into []
                                                               (comp (hx/filter-hyvaksynnan-ehdot-for-correct-hakukohde hakukohde-oids)
                                                                     (map second)
                                                                     (map (fn [{ehdollisesti-hyvaksyttavissa? :ehdollisesti-hyvaksyttavissa?
                                                                                {:keys [koodi ehto-text]}     :hakukohteessa}]
                                                                            {:ehdollisesti-hyvaksyttavissa? ehdollisesti-hyvaksyttavissa?
                                                                             :koodi                         koodi
                                                                             :ehto-text                     ehto-text}))
                                                                     (dedupe))))
          total-values                              (count hakukohteet-ehdollisesti-hyvaksyttavissa?)
          hakukohde-ehdollisesti-hyvaksyttavissa?   (-> hakukohteet-ehdollisesti-hyvaksyttavissa?
                                                        first
                                                        :ehdollisesti-hyvaksyttavissa?)]
      (cond (> total-values 1)
            :hyvaksynnan-ehto/monta-arvoa

            (and (= total-values 1)
                 hakukohde-ehdollisesti-hyvaksyttavissa?)
            :hyvaksynnan-ehto/ehdollisesti-hyvaksyttavissa

            :else
            :hyvaksynnan-ehto/ei-ehdollisesti-hyvaksyttavissa))))

(re-frame/reg-sub
  :hyvaksynnan-ehto/hyvaksynnan-ehto-koodit
  (fn [db _]
    (:hyvaksynnan-ehto-koodit db)))

(re-frame/reg-sub
  :hyvaksynnan-ehto/ehto-koodit
  (fn [_ _]
    [(re-frame/subscribe [:editor/virkailija-lang])
     (re-frame/subscribe [:hyvaksynnan-ehto/hyvaksynnan-ehto-koodit])])
  (fn [[lang koodit] _]
    (mapv (fn [[koodi label]]
            [koodi (util/non-blank-val label [lang :fi :sv :en])])
          (conj (vec (dissoc koodit "muu"))
                ["muu" (get koodit "muu")]))))

(re-frame/reg-sub
  :hyvaksynnan-ehto/selected-ehto-koodi
  (fn [[_ application-key]]
    [(re-frame/subscribe [:state-query [:hyvaksynnan-ehto application-key]])
     (re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]])])
  (fn [[hyvaksynnan-ehdot hakukohde-oids]]
    (let [selected-ehto-koodit (->> hyvaksynnan-ehdot
                                    (into []
                                          (comp (hx/filter-hyvaksynnan-ehdot-for-correct-hakukohde hakukohde-oids)
                                                (map second)
                                                (map :hakukohteessa)
                                                (map :koodi)
                                                (filter (comp not nil?))
                                                (dedupe))))]
      (when (= (count selected-ehto-koodit) 1)
        (first selected-ehto-koodit)))))

(re-frame/reg-sub
  :hyvaksynnan-ehto/selected-ehto-koodi-label
  (fn [[_ application-key] _]
    [(re-frame/subscribe [:editor/virkailija-lang])
     (re-frame/subscribe [:hyvaksynnan-ehto/hyvaksynnan-ehto-koodit])
     (re-frame/subscribe [:hyvaksynnan-ehto/selected-ehto-koodi application-key])])
  (fn [[lang koodit selected-koodi] _]
    (when selected-koodi
      (util/non-blank-val (get koodit selected-koodi) [lang :fi :sv :en]))))

(re-frame/reg-sub
  :hyvaksynnan-ehto/ehto-text
  (fn [db [_ application-key hakukohde-oid lang]]
    (get-in db [:hyvaksynnan-ehto application-key hakukohde-oid :hakukohteessa :ehto-text lang] "")))

(re-frame/reg-sub
  :hyvaksynnan-ehto/show?
  (fn [db _]
    (let [haku-oid       (get-in db [:application :selected-application-and-form :application :haku])
          hakukohde-oids (get-in db [:application :selected-review-hakukohde-oids])
          rights         (->> (get-in db [:application :selected-application-and-form :application :rights-by-hakukohde])
                              (map second)
                              (apply clojure.set/union))]
      (and (seq hakukohde-oids)
           (clojure.string/starts-with?
            (get-in db [:haut haku-oid :kohdejoukko-uri] "")
            "haunkohdejoukko_12#")
           (or (contains? rights :view-applications)
               (contains? rights :edit-applications))
           (or (get-in db [:haut haku-oid :sijoittelu])
               (->> hakukohde-oids
                    (map #(get-in db [:application :valintalaskentakoostepalvelu % :valintalaskenta]))
                    (some true?)))))))

(re-frame/reg-sub
  :hyvaksynnan-ehto/ehdollisesti-hyvaksyttavissa-disabled?
  (fn [[_ application-key] _]
    [(re-frame/subscribe [:hyvaksynnan-ehto/rights])
     (re-frame/subscribe [:hyvaksynnan-ehto/requests-in-flight? application-key])
     (re-frame/subscribe [:hyvaksynnan-ehto/errors application-key])
     (re-frame/subscribe [:hyvaksynnan-ehto/valintatapajonoissa application-key])])
  (fn [[rights requests-in-flight? error valintatapajonoissa] _]
    (or (not (contains? rights :edit-applications))
        requests-in-flight?
        (seq error)
        (seq valintatapajonoissa))))

(re-frame/reg-sub
  :hyvaksynnan-ehto/show-ehto-koodi?
  (fn [[_ application-key] _]
    [(re-frame/subscribe [:hyvaksynnan-ehto/ehdollisesti-hyvaksyttavissa? application-key])
     (re-frame/subscribe [:hyvaksynnan-ehto/valintatapajonoissa application-key])])
  (fn [[ehdollisesti-hyvaksyttavissa? valintatapajonoissa] _]
    (and (= ehdollisesti-hyvaksyttavissa? :hyvaksynnan-ehto/ehdollisesti-hyvaksyttavissa)
         (-> valintatapajonoissa seq nil?))))

(re-frame/reg-sub
  :hyvaksynnan-ehto/show-ehto-texts?
  (fn [[_ application-key] _]
    [(re-frame/subscribe [:hyvaksynnan-ehto/ehdollisesti-hyvaksyttavissa? application-key])
     (re-frame/subscribe [:hyvaksynnan-ehto/valintatapajonoissa application-key])
     (re-frame/subscribe [:hyvaksynnan-ehto/selected-ehto-koodi application-key])])
  (fn [[ehdollisesti-hyvaksyttavissa? valintatapajonoissa selected-ehto-koodi] _]
    (and (= ehdollisesti-hyvaksyttavissa? :hyvaksynnan-ehto/ehdollisesti-hyvaksyttavissa)
         (-> valintatapajonoissa seq nil?)
         (= "muu" selected-ehto-koodi))))

(re-frame/reg-sub
  :hyvaksynnan-ehto/ehto-text-disabled?
  (fn [[_ application-key] _]
    [(re-frame/subscribe [:hyvaksynnan-ehto/rights])
     (re-frame/subscribe [:hyvaksynnan-ehto/requests-in-flight? application-key])
     (re-frame/subscribe [:hyvaksynnan-ehto/errors application-key])])
  (fn [[rights requests-in-flight? error] _]
    (or (not (contains? rights :edit-applications))
        requests-in-flight?
        (seq error))))

(re-frame/reg-sub
  :hyvaksynnan-ehto/show-ehto-valintatapajonoissa?
  (fn [[_ application-key] _]
    (re-frame/subscribe [:hyvaksynnan-ehto/valintatapajonoissa application-key]))
  (fn [valintatapajonoissa _]
    (seq valintatapajonoissa)))

(re-frame/reg-sub
  :hyvaksynnan-ehto/show-single-ehto-valintatapajonoissa?
  (fn [[_ application-key] _]
    (re-frame/subscribe [:hyvaksynnan-ehto/valintatapajonoissa application-key]))
  (fn [valintatapajonoissa _]
    (if-let [ehdot (seq (map second valintatapajonoissa))]
      (apply = ehdot)
      false)))
