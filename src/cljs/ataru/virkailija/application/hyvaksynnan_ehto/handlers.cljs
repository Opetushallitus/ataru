(ns ataru.virkailija.application.hyvaksynnan-ehto.handlers
  (:require [cljs-time.core :as c]
            [cljs-time.format :as f]
            [re-frame.core :as re-frame]
            [ataru.util :as util]
            [ataru.application-common.fx :refer [http]]
            [ataru.virkailija.application.hyvaksynnan-ehto.hyvaksynnan-ehto-xforms :as hx]))

(defn- update-hyvaksynnan-ehdot-for-selected-hakukohde-oids
  [db update-fn application-key hakukohde-oids]
  (update-in db
             [:hyvaksynnan-ehto application-key]
             (partial reduce-kv
                      (fn [acc k v]
                        (assoc acc
                          k
                          (cond-> v
                                  (some #{k} hakukohde-oids)
                                  update-fn)))
                      {})))

(defn- update-ehdollisesti-hyvaksyttavissa [state]
  (fn set-state-to-hyvaksynnan-ehto [hyvaksynnan-ehto]
    (assoc hyvaksynnan-ehto :ehdollisesti-hyvaksyttavissa? state)))

(defn- set-request-in-flight [hyvaksynnan-ehto]
  (assoc hyvaksynnan-ehto :request-in-flight? true))

(re-frame/reg-event-db
  :hyvaksynnan-ehto/set-ehdollisesti-hyvaksyttavissa
  (fn [db [_ application-key hakukohde-oids state]]
    (update-hyvaksynnan-ehdot-for-selected-hakukohde-oids
      db
      (update-ehdollisesti-hyvaksyttavissa state)
      application-key
      hakukohde-oids)))

(defn- update-hyvaksynnan-ehto-koodi [koodi]
  (fn [hyvaksynnan-ehto]
    (assoc-in hyvaksynnan-ehto
              [:hakukohteessa :koodi]
              koodi)))

(re-frame/reg-event-db
  :hyvaksynnan-ehto/set-ehto-koodi
  (fn [db [_ application-key hakukohde-oids koodi]]
    (update-hyvaksynnan-ehdot-for-selected-hakukohde-oids
      db
      (update-hyvaksynnan-ehto-koodi koodi)
      application-key
      hakukohde-oids)))

(defn- update-hyvaksynnan-ehto-text [text lang]
  (fn [hyvaksynnan-ehto]
    (assoc-in hyvaksynnan-ehto [:hakukohteessa :ehto-text lang] text)))

(re-frame/reg-event-db
  :hyvaksynnan-ehto/set-ehto-text
  (fn [db [_ application-key hakukohde-oids lang value]]
    (update-hyvaksynnan-ehdot-for-selected-hakukohde-oids
      db
      (update-hyvaksynnan-ehto-text value lang)
      application-key
      hakukohde-oids)))

(re-frame/reg-event-db
  :hyvaksynnan-ehto/remove-error
  (fn [db [_ application-key hakukohde-oid]]
    (update-in db [:hyvaksynnan-ehto application-key hakukohde-oid]
               dissoc :error)))

(re-frame/reg-event-fx
  :hyvaksynnan-ehto/flash-error
  (fn [{db :db} [_ application-key hakukohde-oid]]
    {:db             (assoc-in db [:hyvaksynnan-ehto application-key hakukohde-oid :error] :error)
     :dispatch-later [{:ms       2100
                       :dispatch [:hyvaksynnan-ehto/remove-error
                                  application-key
                                  hakukohde-oid]}]}))

(re-frame/reg-event-fx
  :hyvaksynnan-ehto/get-koodit
  (fn [_ _]
    {:hyvaksynnan-ehto/get-koodit nil}))

(re-frame/reg-event-fx
  :hyvaksynnan-ehto/get-ehto-hakukohteessa
  (fn [{db :db} [_ application-key hakukohde-oid]]
    (let [rights (->> (get-in db [:application :selected-application-and-form :application :rights-by-hakukohde])
                      (map second)
                      (apply clojure.set/union))]
      (when (or (contains? rights :view-applications)
                (contains? rights :edit-applications))
        {:db
         (assoc-in db [:hyvaksynnan-ehto application-key hakukohde-oid :request-in-flight?] true)
         :hyvaksynnan-ehto/get-ehto-hakukohteessa
         {:application-key application-key
          :hakukohde-oid   hakukohde-oid}
         :hyvaksynnan-ehto/get-ehto-hakukohteessa-muutoshistoria
         {:application-key application-key
          :hakukohde-oid   hakukohde-oid}}))))

(re-frame/reg-event-fx
  :hyvaksynnan-ehto/debounced-save-ehto-hakukohteissa
  (fn [_ [_ application-key hakukohde-oids]]
    {:dispatch-debounced {:timeout  2000
                          :id       [:hyvaksynnan-ehto/save-ehto-hakukohteissa application-key hakukohde-oids]
                          :dispatch [:hyvaksynnan-ehto/save-ehto-hakukohteissa application-key hakukohde-oids]}}))

(re-frame/reg-event-fx
  :hyvaksynnan-ehto/save-ehto-hakukohteissa
  (fn [{db :db} [_ application-key hakukohde-oids]]
    (let [hyvaksynnan-ehdot-to-save (->> (get-in db [:hyvaksynnan-ehto application-key])
                                         (into []
                                               (comp (hx/filter-hyvaksynnan-ehdot-for-correct-hakukohde hakukohde-oids)
                                                     (hx/filter-hyvaksynnan-ehdot-for-hakukohteet)
                                                     (hx/map->hyvaksynnan-ehto-with-hakukohde-oid
                                                       #(select-keys % [:hakukohteessa :last-modified])))))]
      {:db         (update-hyvaksynnan-ehdot-for-selected-hakukohde-oids
                     db
                     set-request-in-flight
                     application-key
                     hakukohde-oids)
       :dispatch-n (map (fn [{:keys [hakukohde-oid
                                     hakukohteessa
                                     last-modified]}]
                          [:hyvaksynnan-ehto/save-ehto-hakukohteessa
                           application-key
                           hakukohde-oid
                           hakukohteessa
                           last-modified])
                        hyvaksynnan-ehdot-to-save)})))

(re-frame/reg-event-fx
  :hyvaksynnan-ehto/save-ehto-hakukohteessa
  (fn [{db :db} [_ application-key hakukohde-oid hakukohteessa last-modified]]
    (let [ehto (if (= "muu" (:koodi hakukohteessa))
                 {:koodi "muu"
                  :fi    (get-in hakukohteessa [:ehto-text :fi] "")
                  :sv    (get-in hakukohteessa [:ehto-text :sv] "")
                  :en    (get-in hakukohteessa [:ehto-text :en] "")}
                 (assoc (get-in db [:hyvaksynnan-ehto-koodit (:koodi hakukohteessa)])
                        :koodi (:koodi hakukohteessa)))]
      {:hyvaksynnan-ehto/save-ehto-hakukohteessa
       {:application-key application-key
        :hakukohde-oid   hakukohde-oid
        :ehto            ehto
        :last-modified   last-modified}})))

(re-frame/reg-event-fx
  :hyvaksynnan-ehto/delete-ehto-hakukohteessa
  (fn [_ [_ application-key hakukohde-oid last-modified]]
    {:hyvaksynnan-ehto/delete-ehto-hakukohteessa {:application-key application-key
                                                  :hakukohde-oid   hakukohde-oid
                                                  :last-modified   last-modified}}))

(re-frame/reg-event-fx
  :hyvaksynnan-ehto/delete-ehto-hakukohteissa
  (fn [{db :db} [_ application-key hakukohde-oids]]
    (let [hyvaksynnan-ehdot-to-delete (->> (get-in db [:hyvaksynnan-ehto application-key])
                                           (into []
                                                 (comp (hx/filter-hyvaksynnan-ehdot-for-correct-hakukohde hakukohde-oids)
                                                       (hx/filter-hyvaksynnan-ehdot-for-hakukohteet)
                                                       (hx/map->hyvaksynnan-ehto-with-hakukohde-oid
                                                         #(select-keys % [:last-modified])))))]
      {:db (update-hyvaksynnan-ehdot-for-selected-hakukohde-oids
             db
             set-request-in-flight
             application-key
             hakukohde-oids)
       :dispatch-n (map (fn [{hakukohde-oid :hakukohde-oid
                              last-modified :last-modified}]
                          [:hyvaksynnan-ehto/delete-ehto-hakukohteessa
                           application-key
                           hakukohde-oid
                           last-modified])
                        hyvaksynnan-ehdot-to-delete)})))

(re-frame/reg-event-fx
  :hyvaksynnan-ehto/get-valintatapajono
  (fn [_ [_ valintatapajono-oid]]
    {:hyvaksynnan-ehto/get-valintatapajono
     {:valintatapajono-oid valintatapajono-oid}}))

(re-frame/reg-event-db
  :hyvaksynnan-ehto/set-koodit
  (fn [db [_ response]]
    (->> (:body response)
         (mapv #(vector (:value %) (:label %)))
         (into {})
         (assoc db :hyvaksynnan-ehto-koodit))))

(re-frame/reg-event-db
  :hyvaksynnan-ehto/ignore-error
  (fn [db _] db))

(defn- retry-dispatch
  [db application-key hakukohde-oid is-401? dispatch]
  (if-let [retry-delay (get-in db [:hyvaksynnan-ehto application-key hakukohde-oid :retry-delay])]
    {:db             (assoc-in db [:hyvaksynnan-ehto application-key hakukohde-oid :retry-delay] (* 10 retry-delay))
     :dispatch-later [{:ms       retry-delay
                       :dispatch dispatch}]}
    (merge
     {:db         (assoc-in db [:hyvaksynnan-ehto application-key hakukohde-oid :retry-delay] 10)
      :dispatch-n [[:hyvaksynnan-ehto/flash-error application-key hakukohde-oid]
                   (when (not is-401?)
                     dispatch)]}
     (when is-401?
       {:authenticate-to-valinta-tulos-service
        {:dispatch-after dispatch}}))))

(defn- update-ehto-hakukohteessa
  [old response]
  (let [koodi (get-in response [:body :koodi])
        text  (case koodi
                "muu"
                (select-keys (:body response) [:fi :sv :en])
                nil
                {:fi "" :sv "" :en ""}
                (get-in old [:hakukohteessa :ehto-text]))]
    (-> old
        (assoc :ehdollisesti-hyvaksyttavissa? (some? koodi))
        (dissoc :valintatapajonoissa)
        (assoc :hakukohteessa {:koodi     koodi
                               :ehto-text {:fi (:fi text "")
                                           :sv (:sv text "")
                                           :en (:en text "")}})
        (assoc :last-modified (get-in response [:headers "last-modified"]))
        (dissoc :request-in-flight?)
        (dissoc :retry-delay))))

(def iso-formatter (f/formatter "yyyy-MM-dd'T'HH:mm:ssZZ"))

(re-frame/reg-event-db
  :hyvaksynnan-ehto/set-ehto-hakukohteessa-muutoshistoria
  (fn [db [_ application-key hakukohde-oid response]]
    (if (= 200 (:status response))
      (assoc-in db [:hyvaksynnan-ehto application-key hakukohde-oid :events]
                (mapv (fn [versio]
                        (merge
                         {:event-type      (if (contains? versio :arvo)
                                             "ehto-hakukohteessa-set"
                                             "ehto-hakukohteessa-unset")
                          :time            (->> (:alku versio)
                                                (f/parse iso-formatter)
                                                c/to-default-time-zone)
                          :id              -1
                          :application-key application-key
                          :hakukohde       hakukohde-oid
                          :first-name      nil
                          :last-name       nil}
                         (when (contains? versio :arvo)
                           {:ehto (:arvo versio)})))
                      (:body response)))
      db)))

(re-frame/reg-event-fx
  :hyvaksynnan-ehto/set-ehto-hakukohteessa
  (fn [{db :db} [_ application-key hakukohde-oid response]]
    (case (:status response)
      (200 201 204 404)
      {:db
       (update-in db [:hyvaksynnan-ehto application-key hakukohde-oid]
                  update-ehto-hakukohteessa response)
       :hyvaksynnan-ehto/get-ehto-hakukohteessa-muutoshistoria
       {:application-key application-key
        :hakukohde-oid   hakukohde-oid}}
      410
      {:hyvaksynnan-ehto/get-ehto-valintatapajonoissa
       {:application-key application-key
        :hakukohde-oid   hakukohde-oid}}
      (retry-dispatch db application-key hakukohde-oid
                      (= 401 (:status response))
                      [:hyvaksynnan-ehto/get-ehto-hakukohteessa
                       application-key
                       hakukohde-oid]))))

(defn- update-ehto-valintatapajonoissa
  [old response]
  (-> old
      (assoc :ehdollisesti-hyvaksyttavissa? (not (empty? (:body response))))
      (dissoc :hakukohteessa)
      (assoc :valintatapajonoissa
             (->> (:body response)
                  (map (fn [[oid ehto]]
                         [(name oid)
                          {:koodi     (:koodi ehto)
                           :ehto-text {:fi (:fi ehto "")
                                       :sv (:sv ehto "")
                                       :en (:en ehto "")}}]))
                  (into {})))
      (dissoc :last-modified)
      (dissoc :request-in-flight?)
      (dissoc :retry-delay)))

(re-frame/reg-event-fx
  :hyvaksynnan-ehto/set-ehto-valintatapajonoissa
  (fn [{db :db} [_ application-key hakukohde-oid response]]
    (case (:status response)
      200
      {:db         (update-in db [:hyvaksynnan-ehto application-key hakukohde-oid]
                              update-ehto-valintatapajonoissa response)
       :dispatch-n (mapv (fn [[oid _]]
                           [:hyvaksynnan-ehto/get-valintatapajono (name oid)])
                         (:body response))}
      (retry-dispatch db application-key hakukohde-oid
                      (= 401 (:status response))
                      [:hyvaksynnan-ehto/get-ehto-valintatapajonoissa
                       application-key
                       hakukohde-oid]))))

(re-frame/reg-event-db
  :hyvaksynnan-ehto/set-valintatapajono
  (fn [db [_ valintatapajono-oid response]]
    (assoc-in db [:valintatapajono valintatapajono-oid] (:body response))))

(re-frame/reg-fx
  :hyvaksynnan-ehto/get-koodit
  (fn [_]
    (http (aget js/config "virkailija-caller-id")
          {:method        :get
           :url           "/lomake-editori/api/koodisto/hyvaksynnanehdot/1?allow-invalid=false"
           :handler       [:hyvaksynnan-ehto/set-koodit]
           :error-handler [:hyvaksynnan-ehto/ignore-error]})))

(re-frame/reg-fx
  :hyvaksynnan-ehto/get-ehto-hakukohteessa-muutoshistoria
  (fn [{:keys [application-key hakukohde-oid]}]
    (http (aget js/config "virkailija-caller-id")
          {:method        :get
           :url           (.url js/window
                                "valinta-tulos-service.hyvaksynnan-ehto.muutoshistoria"
                                hakukohde-oid
                                application-key)
           :handler       [:hyvaksynnan-ehto/set-ehto-hakukohteessa-muutoshistoria
                           application-key
                           hakukohde-oid]
           :error-handler [:hyvaksynnan-ehto/ignore-error]})))

(re-frame/reg-fx
  :hyvaksynnan-ehto/get-ehto-hakukohteessa
  (fn [{:keys [application-key hakukohde-oid]}]
    (http (aget js/config "virkailija-caller-id")
          {:method        :get
           :url           (.url js/window
                                "valinta-tulos-service.hyvaksynnan-ehto.hakukohteessa.hakemus"
                                hakukohde-oid
                                application-key)
           :handler       [:hyvaksynnan-ehto/set-ehto-hakukohteessa
                           application-key
                           hakukohde-oid]
           :error-handler [:hyvaksynnan-ehto/set-ehto-hakukohteessa
                           application-key
                           hakukohde-oid]})))

(re-frame/reg-fx
  :hyvaksynnan-ehto/get-ehto-valintatapajonoissa
  (fn [{:keys [application-key hakukohde-oid]}]
    (http (aget js/config "virkailija-caller-id")
          {:method        :get
           :url           (.url js/window
                                "valinta-tulos-service.hyvaksynnan-ehto.valintatapajonoissa.hakemus"
                                hakukohde-oid
                                application-key)
           :handler       [:hyvaksynnan-ehto/set-ehto-valintatapajonoissa
                           application-key
                           hakukohde-oid]
           :error-handler [:hyvaksynnan-ehto/set-ehto-valintatapajonoissa]})))

(re-frame/reg-fx
  :hyvaksynnan-ehto/get-valintatapajono
  (fn [{:keys [valintatapajono-oid]}]
    (http (aget js/config "virkailija-caller-id")
          {:method        :get
           :url           (str "/lomake-editori/api/valintaperusteet/valintatapajono/"
                               valintatapajono-oid)
           :handler       [:hyvaksynnan-ehto/set-valintatapajono
                           valintatapajono-oid]
           :error-handler [:hyvaksynnan-ehto/ignore-error]})))

(re-frame/reg-fx
  :hyvaksynnan-ehto/save-ehto-hakukohteessa
  (fn [{:keys [application-key hakukohde-oid ehto last-modified]}]
    (http (aget js/config "virkailija-caller-id")
          {:method        :put
           :url           (.url js/window
                                "valinta-tulos-service.hyvaksynnan-ehto.hakukohteessa.hakemus"
                                hakukohde-oid
                                application-key)
           :post-data     ehto
           :headers       (if (some? last-modified)
                            {"If-Unmodified-Since" last-modified}
                            {"If-None-Match" "*"})
           :handler       [:hyvaksynnan-ehto/set-ehto-hakukohteessa
                           application-key
                           hakukohde-oid]
           :error-handler [:hyvaksynnan-ehto/set-ehto-hakukohteessa
                           application-key
                           hakukohde-oid]})))

(re-frame/reg-fx
  :hyvaksynnan-ehto/delete-ehto-hakukohteessa
  (fn [{:keys [application-key hakukohde-oid last-modified]}]
    (http (aget js/config "virkailija-caller-id")
          {:method        :delete
           :url           (.url js/window
                                "valinta-tulos-service.hyvaksynnan-ehto.hakukohteessa.hakemus"
                                hakukohde-oid
                                application-key)
           :headers       {"If-Unmodified-Since" last-modified}
           :handler       [:hyvaksynnan-ehto/set-ehto-hakukohteessa
                           application-key
                           hakukohde-oid]
           :error-handler [:hyvaksynnan-ehto/set-ehto-hakukohteessa
                           application-key
                           hakukohde-oid]})))
