(ns ataru.virkailija.application.attachments.liitepyynto-information-request-handlers
  (:require [ataru.application-common.fx :refer [http]]
            [cljs-time.core :as c]
            [cljs-time.format :as f]
            [re-frame.core :as re-frame]))

(def iso-formatter (f/formatter "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"))

(def date-formatter (f/formatter "yyyy-MM-dd"))

(def time-formatter (f/formatter "HH:mm"))

(def datetime-formatter (f/formatter "yyyy-MM-dd HH:mm"))

(defn- get-deadline-visibility-state [db application-key liitepyynto-key]
  (get-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline :visibility-state] :hidden))

(defn- set-deadline-visibility-state [db application-key liitepyynto-key state]
  (assoc-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline :visibility-state] state))

(defn- set-deadline-date [db application-key liitepyynto-key value]
  (assoc-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline :date] value))

(defn- set-deadline-time [db application-key liitepyynto-key value]
  (assoc-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline :time] value))

(defn- get-deadline-last-modified [db application-key liitepyynto-key]
  (get-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline :last-modified] nil))

(defn- set-deadline-last-modified [db application-key liitepyynto-key last-modified]
  (assoc-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline :last-modified] last-modified))

(defn- set-deadline
  [db application-key liitepyynto-key date-string time-string last-modified]
  (-> db
      (set-deadline-date application-key liitepyynto-key date-string)
      (set-deadline-time application-key liitepyynto-key time-string)
      (set-deadline-last-modified application-key liitepyynto-key last-modified)))

(re-frame/reg-event-db
  :liitepyynto-information-request/set-deadline-visibility-state
  (fn [db [_ application-key liitepyynto-key state]]
    (set-deadline-visibility-state db application-key liitepyynto-key state)))

(re-frame/reg-event-fx
  :liitepyynto-information-request/hide-deadline
  (fn [{db :db} [_ application-key liitepyynto-key]]
    (let [current-state (get-deadline-visibility-state db application-key liitepyynto-key)]
      (when (or (= :visible current-state)
                (= :appearing current-state))
        {:db             (set-deadline-visibility-state db application-key liitepyynto-key :disappearing)
         :dispatch-later [{:ms       200
                           :dispatch [:liitepyynto-information-request/set-deadline-visibility-state
                                      application-key
                                      liitepyynto-key
                                      :hidden]}]}))))

(re-frame/reg-event-fx
  :liitepyynto-information-request/show-deadline
  (fn [{db :db} [_ application-key liitepyynto-key]]
    (let [current-state (get-deadline-visibility-state db application-key liitepyynto-key)]
      (when (or (= :hidden current-state)
                (= :disappearing current-state))
        {:db             (set-deadline-visibility-state db application-key liitepyynto-key :appearing)
         :dispatch-later [{:ms       200
                           :dispatch [:liitepyynto-information-request/set-deadline-visibility-state
                                      application-key
                                      liitepyynto-key
                                      :visible]}]}))))

(re-frame/reg-event-fx
  :liitepyynto-information-request/set-deadline-toggle
  (fn [{db :db} [_ application-key liitepyynto-key on?]]
    {:dispatch [(cond on?
                      :liitepyynto-information-request/show-deadline
                      (some? (get-deadline-last-modified db application-key liitepyynto-key))
                      :liitepyynto-information-request/delete-deadline
                      :else
                      :liitepyynto-information-request/hide-deadline)
                application-key
                liitepyynto-key]}))

(re-frame/reg-event-db
  :liitepyynto-information-request/set-deadline-date
  (fn [db [_ application-key liitepyynto-key value]]
    (set-deadline-date db application-key liitepyynto-key value)))

(re-frame/reg-event-db
  :liitepyynto-information-request/set-deadline-time
  (fn [db [_ application-key liitepyynto-key value]]
    (set-deadline-time db application-key liitepyynto-key value)))

(re-frame/reg-event-db
  :liitepyynto-information-request/hide-deadline-error
  (fn [db [_ application-key liitepyynto-key]]
    (assoc-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline :error?] false)))

(re-frame/reg-event-fx
  :liitepyynto-information-request/show-deadline-error
  (fn [{db :db} [_ application-key liitepyynto-key]]
    {:db             (assoc-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline :error?] true)
     :dispatch-later [{:ms       5000
                       :dispatch [:liitepyynto-information-request/hide-deadline-error
                                  application-key
                                  liitepyynto-key]}]}))

(defn- parse-deadline [deadline]
  (let [datetime (->> deadline
                      (f/parse iso-formatter)
                      c/to-default-time-zone)]
    [(f/unparse date-formatter datetime)
     (f/unparse time-formatter datetime)]))

(re-frame/reg-event-fx
  :liitepyynto-information-request/set-deadlines
  (fn [{db :db} [_ application-key response]]
    (let [last-modified (get-in response [:headers "last-modified"])]
      {:db         (reduce (fn [db deadline]
                             (let [liitepyynto-key           (keyword (:field-id deadline))
                                   [date-string time-string] (parse-deadline (:deadline deadline))]
                               (set-deadline db application-key liitepyynto-key date-string time-string last-modified)))
                           db
                           (:body response))
       :dispatch-n (map (fn [deadline]
                          [:liitepyynto-information-request/show-deadline
                           application-key
                           (keyword (:field-id deadline))])
                        (:body response))})))

(re-frame/reg-event-fx
  :liitepyynto-information-request/set-deadline
  (fn [{db :db} [_ application-key liitepyynto-key response]]
    (let [last-modified             (get-in response [:headers "last-modified"])
          [date-string time-string] (parse-deadline (get-in response [:body :deadline]))]
      {:db       (set-deadline db application-key liitepyynto-key date-string time-string last-modified)
       :dispatch [:liitepyynto-information-request/show-deadline
                  application-key
                  liitepyynto-key]})))

(re-frame/reg-event-fx
  :liitepyynto-information-request/unset-deadline
  (fn [{db :db} [_ application-key liitepyynto-key _]]
    {:db       (set-deadline db application-key liitepyynto-key "" "" nil)
     :dispatch [:liitepyynto-information-request/hide-deadline
                application-key
                liitepyynto-key]}))

(re-frame/reg-event-db
  :liitepyynto-information-request/ignore-error
  (fn [db _] db))

(re-frame/reg-event-fx
  :liitepyynto-information-request/re-fetch-and-show-error
  (fn [_ [_ application-key liitepyynto-key _]]
    {:liitepyynto-information-request/get-deadline
     {:application-key application-key
      :liitepyynto-key liitepyynto-key}
     :dispatch [:liitepyynto-information-request/show-deadline-error
                application-key
                liitepyynto-key]}))

(re-frame/reg-event-fx
  :liitepyynto-information-request/get-deadlines
  (fn [_ [_ application-key]]
    {:liitepyynto-information-request/get-deadlines
     {:application-key application-key}}))

(re-frame/reg-event-fx
  :liitepyynto-information-request/debounced-save-deadline
  (fn [_ [_ application-key liitepyynto-key]]
    {:dispatch-debounced
     {:timeout  500
      :id       [:liitepyynto-information-request/save-deadline
                 application-key
                 liitepyynto-key]
      :dispatch [:liitepyynto-information-request/save-deadline
                 application-key
                 liitepyynto-key]}}))

(re-frame/reg-event-fx
  :liitepyynto-information-request/save-deadline
  (fn [{db :db} [_ application-key liitepyynto-key]]
    (let [date          (get-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline :date])
          time          (get-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline :time])
          last-modified (get-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline :last-modified])]
      (when-let [datetime (try
                            (->> (str date " " time)
                                 (f/parse-local datetime-formatter)
                                 c/to-utc-time-zone)
                            (catch js/Error _ nil))]
        {:liitepyynto-information-request/save-deadline
         {:application-key application-key
          :liitepyynto-key liitepyynto-key
          :deadline        datetime
          :last-modified   last-modified}}))))

(re-frame/reg-event-fx
  :liitepyynto-information-request/delete-deadline
  (fn [{db :db} [_ application-key liitepyynto-key]]
    (when-let [last-modified (get-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline :last-modified])]
      {:liitepyynto-information-request/delete-deadline
       {:application-key application-key
        :liitepyynto-key liitepyynto-key
        :last-modified   last-modified}})))

(re-frame/reg-fx
  :liitepyynto-information-request/get-deadlines
  (fn [{:keys [application-key]}]
    (http (aget js/config "virkailija-caller-id")
          {:method        :get
           :url           (str "/lomake-editori/api/applications/" application-key
                               "/field-deadline")
           :handler       [:liitepyynto-information-request/set-deadlines
                           application-key]
           :error-handler [:liitepyynto-information-request/ignore-error]})))

(re-frame/reg-fx
  :liitepyynto-information-request/get-deadline
  (fn [{:keys [application-key liitepyynto-key]}]
    (http (aget js/config "virkailija-caller-id")
          {:method        :get
           :url           (str "/lomake-editori/api/applications/" application-key
                               "/field-deadline/" (name liitepyynto-key))
           :handler       [:liitepyynto-information-request/set-deadline
                           application-key
                           liitepyynto-key]
           :error-handler [:liitepyynto-information-request/unset-deadline
                           application-key
                           liitepyynto-key]})))

(re-frame/reg-fx
  :liitepyynto-information-request/save-deadline
  (fn [{:keys [application-key liitepyynto-key deadline last-modified]}]
    (http (aget js/config "virkailija-caller-id")
          {:method        :put
           :url           (str "/lomake-editori/api/applications/" application-key
                               "/field-deadline/" (name liitepyynto-key))
           :post-data     {:deadline (f/unparse iso-formatter deadline)}
           :headers       (if (some? last-modified)
                            {"If-Unmodified-Since" last-modified}
                            {"If-None-Match" "*"})
           :handler       [:liitepyynto-information-request/set-deadline
                           application-key
                           liitepyynto-key]
           :error-handler [:liitepyynto-information-request/re-fetch-and-show-error
                           application-key
                           liitepyynto-key]})))

(re-frame/reg-fx
  :liitepyynto-information-request/delete-deadline
  (fn [{:keys [application-key liitepyynto-key last-modified]}]
    (http (aget js/config "virkailija-caller-id")
          {:method        :delete
           :url           (str "/lomake-editori/api/applications/" application-key
                               "/field-deadline/" (name liitepyynto-key))
           :headers       {"If-Unmodified-Since" last-modified}
           :handler       [:liitepyynto-information-request/unset-deadline
                           application-key
                           liitepyynto-key]
           :error-handler [:liitepyynto-information-request/re-fetch-and-show-error
                           application-key
                           liitepyynto-key]})))
