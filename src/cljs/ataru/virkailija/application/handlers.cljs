(ns ataru.virkailija.application.handlers
  (:require [ataru.virkailija.virkailija-ajax :as ajax]
            [re-frame.core :refer [subscribe dispatch dispatch-sync reg-event-db reg-event-fx]]
            [ataru.virkailija.autosave :as autosave]
            [ataru.virkailija.application-sorting :as application-sorting]
            [ataru.virkailija.virkailija-ajax :refer [http]]
            [ataru.util :as util]
            [reagent.core :as r]
            [taoensso.timbre :refer-macros [spy debug]]
            [ataru.feature-config :as fc]
            [ataru.url :as url]))

(reg-event-fx
  :application/select-application
  (fn [{:keys [db]} [_ application-key]]
    (if (not= application-key (get-in db [:application :selected-key]))
      (let [db (-> db
                   (assoc-in [:application :selected-key] application-key)
                   (assoc-in [:application :selected-application-and-form] nil)
                   (assoc-in [:application :form-list-expanded?] false))]
        {:db         db
         :dispatch-n [[:application/stop-autosave]
                      [:application/fetch-application application-key]]}))))

(defn close-application [db]
  (-> db
      (assoc-in [:application :selected-key] nil)
      (assoc-in [:application :selected-application-and-form] nil)
      (assoc-in [:application :form-list-expanded?] true)))

(reg-event-db
 :application/close-application
 (fn [db [_ _]]
   (close-application db)))

(defn- languages->kwd [form]
  (update form :languages
    (partial mapv keyword)))

(defn review-state-counts [applications]
  (into {} (map (fn [[state values]] [state (count values)]) (group-by :state applications))))

(defn- update-review-field-of-selected-application-in-list
  [application selected-application-key field value]
  (if (= selected-application-key (:key application))
    (assoc application field value)
    application))

(reg-event-db
 :application/update-review-field
 (fn [db [_ field value]]
   (let [selected-key         (get-in db [:application :selected-key])
         application-list     (get-in db [:application :applications])
         updated-applications (if (some #{field} [:state :score])
                                (mapv
                                 #(update-review-field-of-selected-application-in-list % selected-key field value)
                                 application-list)
                                application-list)]
     (-> db
         (update-in [:application :review] assoc field value)
         (assoc-in [:application :applications] updated-applications)
         (assoc-in [:application :review-state-counts] (review-state-counts updated-applications))))))

(reg-event-db
 :application/update-sort
 (fn [db [_ column-id]]
   (let [current-applications (get-in db [:application :applications])
         current-sort         (get-in db [:application :sort])
         new-order            (if (= :ascending (:order current-sort))
                                :descending
                                :ascending)]
     (if (= column-id (:column current-sort))
       (-> db
           (update-in
            [:application :sort]
            assoc
            :order
            new-order)
           (assoc-in
            [:application :applications]
            (application-sorting/sort-by-column current-applications column-id new-order)))
       (-> db
           (assoc-in
            [:application :sort]
            {:column column-id :order :descending})
           (assoc-in
            [:application :applications]
            (application-sorting/sort-by-column current-applications column-id :descending)))))))

(reg-event-db
  :application/handle-fetch-applications-response
  (fn [db [_ {:keys [applications]}]]
    (-> db
        (assoc-in [:application :applications] applications)
        (assoc-in [:application :review-state-counts] (review-state-counts applications))
        (assoc-in [:application :sort] application-sorting/initial-sort))))

(reg-event-fx
  :application/fetch-applications
  (fn [{:keys [db]} [_ form-key]]
    {:db   db
     :http {:method              :get
            :path                (str "/lomake-editori/api/applications/list?formKey=" form-key)
            :handler-or-dispatch :application/handle-fetch-applications-response}}))

(reg-event-fx
  :application/fetch-applications-by-hakukohde
  (fn [{:keys [db]} [_ hakukohde-oid]]
    {:db   db
     :http {:method              :get
            :path                (str "/lomake-editori/api/applications/list?hakukohdeOid=" hakukohde-oid)
            :handler-or-dispatch :application/handle-fetch-applications-response}}))

(reg-event-fx
  :application/fetch-applications-by-haku
  (fn [{:keys [db]} [_ haku-oid]]
    {:db   db
     :http {:method              :get
            :path                (str "/lomake-editori/api/applications/list?hakuOid=" haku-oid)
            :handler-or-dispatch :application/handle-fetch-applications-response}}))

(reg-event-db
 :application/review-updated
 (fn [db [_ response]]
   (assoc-in db [:application :events] (:events response))))

(defn answers-indexed
  "Convert the rest api version of application to a version which application
  readonly-rendering can use (answers are indexed with key in a map)"
  [application]
  (let [answers    (:answers application)
        answer-map (into {} (map (fn [answer] [(keyword (:key answer)) answer])) answers)]
    (assoc application :answers answer-map)))

(defn update-application-details [db {:keys [form application events review]}]
  (-> db
      (assoc-in [:application :selected-application-and-form]
        {:form        form
         :application (answers-indexed application)})
      (assoc-in [:application :events] events)
      (assoc-in [:application :review] review)))

(defn review-autosave-predicate [current prev]
  (if (not= (:id current) (:id prev))
    false
    ;timestamp instances for same timestamp fetched via ajax are not equal :(
    (not= (dissoc current :created-time) (dissoc prev :created-time))))

(defn start-application-review-autosave [db]
  (assoc-in
    db
    [:application :review-autosave]
    (autosave/interval-loop {:subscribe-path [:application :review]
                             :changed-predicate review-autosave-predicate
                             :handler (fn [current _]
                                        (ajax/http
                                          :put
                                          "/lomake-editori/api/applications/review"
                                          :application/review-updated
                                          :override-args {:params (select-keys current [:id
                                                                                        :application-id
                                                                                        :application-key
                                                                                        :notes
                                                                                        :score
                                                                                        :state])}))})))

(reg-event-fx
  :application/handle-fetch-application-attachment-metadata
  (fn [{:keys [db]} [_ response]]
    (let [response-map (group-by :key response)
          db (->> (get-in db [:application :selected-application-and-form :application :answers])
                  (map (fn [[_ {:keys [fieldType] :as answer}]]
                         (cond-> answer
                           (= fieldType "attachment")
                           (update :value (partial map (fn [file-key]
                                                         (first (get response-map file-key))))))))
                  (reduce (fn [db {:keys [key] :as answer}]
                            (assoc-in db [:application :selected-application-and-form :application :answers (keyword key)] answer))
                          db))]
      {:db       db
       :dispatch [:application/start-autosave]})))

(reg-event-fx
  :application/fetch-application-attachment-metadata
  (fn [{:keys [db]} _]
    (let [query-part (->> (get-in db [:application :selected-application-and-form :application :answers])
                          (filter (comp (partial = "attachment") :fieldType second))
                          (map (comp :value second))
                          (flatten)
                          (url/items->query-part "key")
                          (clojure.string/join))
          path       (str "/lomake-editori/api/files/metadata" query-part)]
      {:db       db
       :http     {:method              :get
                  :path                path
                  :handler-or-dispatch :application/handle-fetch-application-attachment-metadata}})))

(reg-event-fx
  :application/handle-fetch-application
  (fn [{:keys [db]} [_ response]]
    (let [db (update-application-details db response)]
      {:db db
       :dispatch (if (fc/feature-enabled? :attachment)
                   [:application/fetch-application-attachment-metadata]
                   [:application/start-autosave])})))

(reg-event-fx
  :application/fetch-application
  (fn [{:keys [db]} [_ application-id]]
    (when-let [autosave (get-in db [:application :review-autosave])]
      (autosave/stop-autosave! autosave))
    (let [db (assoc-in db [:application :review-autosave] nil)]
      {:db   db
       :http {:method              :get
              :path                (str "/lomake-editori/api/applications/" application-id)
              :handler-or-dispatch :application/handle-fetch-application}})))

(reg-event-db
  :application/start-autosave
  (fn [db _]
    (start-application-review-autosave db)))

(reg-event-fx
  :application/stop-autosave
  (fn [{:keys [db]} _]
    (let [autosave (get-in db [:application :review-autosave])]
      (cond-> {:db db}
        (some? autosave) (assoc :stop-autosave autosave)))))

(reg-event-db
  :application/search-form-list
  (fn [db [_ search-term]]
    (assoc-in db [:application :search-term] search-term)))

(reg-event-db
  :application/clear-search-term
  (fn [db]
    (assoc-in db [:application :search-term] nil)))

(reg-event-db
 :application/select-form
 (fn [db [_ form-key]]
   (-> db
       (assoc-in [:application :selected-form-key] form-key)
       (close-application))))

(reg-event-db
  :application/select-hakukohde
  (fn [db [_ hakukohde]]
    (-> db
        (update-in [:application] dissoc :selected-form-key :selected-haku)
        (assoc-in [:application :selected-hakukohde] hakukohde)
        (close-application))))

(reg-event-db
  :application/select-haku
  (fn [db [_ haku]]
    (-> db
        (update :application dissoc :selected-form-key :selected-hakukohde)
        (assoc-in [:application :selected-haku] haku)
        (close-application))))

(reg-event-db
  :application/refresh-hakukohteet-from-applications
  (fn [db _]
    (http
      :get
      "/lomake-editori/api/hakukohteet"
      (fn [db hakukohteet]
        (assoc-in db [:application :hakukohteet] hakukohteet)))
    db))

(reg-event-db
  :editor/handle-refresh-haut-from-applications
  (fn [db [_ haut]]
    (assoc-in db [:application :haut] haut)))

(reg-event-fx
  :application/refresh-haut-from-applications
  (fn [{:keys [db]}]
    {:db   db
     :http {:method              :get
            :path                "/lomake-editori/api/haut"
            :handler-or-dispatch :editor/handle-refresh-haut-from-applications}}))

(defn get-hakukohteet-from-haut [haut]
  (flatten (map :hakukohteet (:tarjonta-haut haut))))

(reg-event-db
  :editor/handle-refresh-haut2
  (fn [db [_ haut]]
    (-> db
        (assoc-in [:application :haut2] haut)
        (assoc-in [:application :hakukohteet2] (get-hakukohteet-from-haut haut)))))

(reg-event-fx
  :application/refresh-haut2
  (fn [{:keys [db]}]
    {:db   db
     :http {:method              :get
            :path                "/lomake-editori/api/haut2"
            :handler-or-dispatch :editor/handle-refresh-haut2}}))

