(ns ataru.virkailija.application.handlers
  (:require [ataru.virkailija.virkailija-ajax :as ajax]
            [re-frame.core :refer [subscribe dispatch dispatch-sync reg-event-db reg-event-fx]]
            [ataru.virkailija.autosave :as autosave]
            [reagent.core :as r]
            [taoensso.timbre :refer-macros [spy debug]]))

(reg-event-fx
  :application/select-application
  (fn [{:keys [db]} [_ application-key]]
    (if (not= application-key (get-in db [:application :selected-key]))
      (-> {:db db}
          (assoc-in [:db :application :selected-key] application-key)
          (assoc-in [:db :application :selected-application-and-form] nil)
          (assoc :dispatch [:application/fetch-application application-key])))))

(defn- before?
  "Check if application2 is created before application1."
  [application1 application2]
  true)

(defn- ->latest-version [applications a1]
  (let [key (:key a1)
        a2  (get applications key)]
    (if (or (nil? a2)
            (before? a1 a2))
      (assoc applications key a1)
      applications)))

(reg-event-db
  :application/fetch-applications
  (fn [db [_ form-key]]
    (ajax/http
      :get
      (str "/lomake-editori/api/applications/list?formKey=" form-key)
      (fn [db applications-response]
        (let [applications (->> (:applications applications-response)
                                (reduce ->latest-version {})
                                (vals))]
          (assoc-in db [:application :applications] applications))))
    db))

(reg-event-db
  :application/fetch-applications-by-hakukohde
  (fn [db [_ hakukohde-oid]]
    (ajax/http
      :get
      (str "/lomake-editori/api/applications/list?hakukohdeOid=" hakukohde-oid)
      (fn [db applications-response]
        (let [applications (->> (:applications applications-response)
                                (reduce ->latest-version {})
                                (vals))]
          (assoc-in db [:application :applications] applications))))
      db))

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
                                          :override-args {:params (select-keys current [:id :application-id :application-key :notes :state])}))})))

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
