(ns ataru.virkailija.application.hyvaksynnan-ehto.handlers
  (:require [cljs-time.core :as c]
            [cljs-time.format :as f]
            [re-frame.core :as re-frame]
            [ataru.util :as util]
            [ataru.application-common.fx :refer [http]]))

(re-frame/reg-event-db
  :hyvaksynnan-ehto/set-ehdollisesti-hyvaksyttavissa
  (fn [db [_ application-key hakukohde-oid state]]
    (assoc-in db [:hyvaksynnan-ehto application-key hakukohde-oid :ehdollisesti-hyvaksyttavissa?] state)))

(re-frame/reg-event-db
  :hyvaksynnan-ehto/set-ehto-koodi
  (fn [db [_ application-key hakukohde-oid koodi]]
    (assoc-in db [:hyvaksynnan-ehto application-key hakukohde-oid :hakukohteessa :koodi] koodi)))

(re-frame/reg-event-db
  :hyvaksynnan-ehto/set-ehto-text
  (fn [db [_ application-key hakukohde-oid lang value]]
    (assoc-in db [:hyvaksynnan-ehto application-key hakukohde-oid :hakukohteessa :ehto-text lang] value)))

(re-frame/reg-event-db
  :hyvaksynnan-ehto/remove-error
  (fn [db [_ application-key hakukohde-oid]]
    (-> db
        (update-in [:hyvaksynnan-ehto application-key hakukohde-oid]
                   dissoc :error)
        (update-in [:hyvaksynnan-ehto application-key hakukohde-oid]
                   dissoc :retry-delay))))

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
    (let [rights (get-in db [:application
                             :selected-application-and-form
                             :application
                             :rights-by-hakukohde
                             hakukohde-oid])]
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
  :hyvaksynnan-ehto/debounced-save-ehto-hakukohteessa
  (fn [_ [_ application-key hakukohde-oid]]
    {:dispatch-debounced
     {:timeout  500
      :id       [:hyvaksynnan-ehto/save-ehto-hakukohteessa
                 application-key
                 hakukohde-oid]
      :dispatch [:hyvaksynnan-ehto/save-ehto-hakukohteessa
                 application-key
                 hakukohde-oid]}}))

(re-frame/reg-event-fx
  :hyvaksynnan-ehto/save-ehto-hakukohteessa
  (fn [{db :db} [_ application-key hakukohde-oid]]
    (when-let [hakukohteessa (get-in db [:hyvaksynnan-ehto application-key hakukohde-oid :hakukohteessa])]
      (let [last-modified (get-in db [:hyvaksynnan-ehto application-key hakukohde-oid :last-modified])]
        {:db
         (assoc-in db [:hyvaksynnan-ehto application-key hakukohde-oid :request-in-flight?] true)
         :hyvaksynnan-ehto/save-ehto-hakukohteessa
         {:application-key application-key
          :hakukohde-oid   hakukohde-oid
          :ehto            (if (= "muu" (:koodi hakukohteessa))
                             {:koodi "muu"
                              :fi    (get-in hakukohteessa [:ehto-text :fi] "")
                              :sv    (get-in hakukohteessa [:ehto-text :sv] "")
                              :en    (get-in hakukohteessa [:ehto-text :en] "")}
                             (assoc (get-in db [:hyvaksynnan-ehto-koodit (:koodi hakukohteessa)])
                                    :koodi (:koodi hakukohteessa)))
          :last-modified   last-modified}}))))

(re-frame/reg-event-fx
  :hyvaksynnan-ehto/delete-ehto-hakukohteessa
  (fn [{db :db} [_ application-key hakukohde-oid]]
    (when-let [hakukohteessa (get-in db [:hyvaksynnan-ehto application-key hakukohde-oid :hakukohteessa])]
      (when-let [last-modified (get-in db [:hyvaksynnan-ehto application-key hakukohde-oid :last-modified])]
        {:db
         (assoc-in db [:hyvaksynnan-ehto application-key hakukohde-oid :request-in-flight?] true)
         :hyvaksynnan-ehto/delete-ehto-hakukohteessa
         {:application-key application-key
          :hakukohde-oid   hakukohde-oid
          :last-modified   last-modified}}))))

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
        (dissoc :request-in-flight?))))

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
      {:db (update-in db [:hyvaksynnan-ehto application-key hakukohde-oid]
                      update-ehto-hakukohteessa response)}
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
      (dissoc :request-in-flight?)))

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
                                "valinta-tulos-service.hyvaksynnan-ehto-muutoshistoria"
                                application-key
                                hakukohde-oid)
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
                                "valinta-tulos-service.hyvaksynnan-ehto"
                                application-key
                                hakukohde-oid)
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
                                "valinta-tulos-service.hyvaksynnan-ehto.valintatapajonot"
                                application-key
                                hakukohde-oid)
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
                                "valinta-tulos-service.hyvaksynnan-ehto"
                                application-key
                                hakukohde-oid)
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
                                "valinta-tulos-service.hyvaksynnan-ehto"
                                application-key
                                hakukohde-oid)
           :headers       {"If-Unmodified-Since" last-modified}
           :handler       [:hyvaksynnan-ehto/set-ehto-hakukohteessa
                           application-key
                           hakukohde-oid]
           :error-handler [:hyvaksynnan-ehto/set-ehto-hakukohteessa
                           application-key
                           hakukohde-oid]})))
