(ns ataru.virkailija.application.handlers
  (:require [ataru.virkailija.virkailija-ajax :as ajax]
            [re-frame.core :refer [subscribe dispatch dispatch-sync reg-event-db reg-event-fx]]
            [ataru.virkailija.autosave :as autosave]
            [ataru.virkailija.application-sorting :as application-sorting]
            [reagent.core :as r]
            [taoensso.timbre :refer-macros [spy debug]]))

(reg-event-fx
  :application/select-application
  (fn [{:keys [db]} [_ application-key]]
    (if (not= application-key (get-in db [:application :selected-key]))
      (-> {:db db}
          (assoc-in [:db :application :selected-key] application-key)
          (assoc-in [:db :application :selected-application-and-form] nil)
          (assoc-in [:db :application :ui :form-list-expanded?] false)
          (assoc :dispatch [:application/fetch-application application-key])))))

(reg-event-fx
  :application/close-application
  (fn [{:keys [db]} [_ application-key]]
    (-> {:db db}
        (assoc-in [:db :application :selected-key] nil)
        (assoc-in [:db :application :selected-application-and-form] nil)
        (assoc-in [:db :application :ui :form-list-expanded?] true))))

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

(reg-event-db
  :application/fetch-application
  (fn [db [_ application-id]]
    (when-let [autosave (get-in db [:application :review-autosave])]
      (autosave/stop-autosave! autosave))
    (ajax/http
      :get
      (str "/lomake-editori/api/applications/" application-id)
      (fn [db application-response]
        (-> db
          (update-application-details application-response)
          (start-application-review-autosave))))
    (assoc db [:application :review-autosave] nil)))

(reg-event-db
  :application/search-form-list
  (fn [db [_ search-term]]
    (assoc-in db [:application :search-term] search-term)))

(reg-event-db
  :application/clear-search-term
  (fn [db]
    (assoc-in db [:application :search-term] nil)))
