(ns ataru.virkailija.application.attachments.liitepyynto-information-request-handlers
  (:require [ataru.cljs-util :as cu]
            [ajax.core :as ajax]
            [cljs-time.core :as c]
            [cljs-time.format :as f]
            [re-frame.core :as re-frame]))

(def iso-formatter (f/formatter "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"))

(def date-formatter (f/formatter "yyyy-MM-dd"))

(def time-formatter (f/formatter "HH:mm"))

(def datetime-formatter (f/formatter "yyyy-MM-dd HH:mm"))

(re-frame/reg-event-db
  :liitepyynto-information-request/set-send?
  (fn [db [_ application-key liitepyynto-key state]]
    (assoc-in db [:liitepyynto-information-request application-key liitepyynto-key :send?] state)))

(re-frame/reg-event-db
  :liitepyynto-information-request/set-deadline-date
  (fn [db [_ application-key liitepyynto-key value]]
    (assoc-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline-date] value)))

(re-frame/reg-event-db
  :liitepyynto-information-request/set-deadline-time
  (fn [db [_ application-key liitepyynto-key value]]
    (assoc-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline-time] value)))

(re-frame/reg-event-db
  :liitepyynto-information-request/set-deadline-last-modified
  (fn [db [_ application-key liitepyynto-key last-modified]]
    (assoc-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline-last-modified] last-modified)))

(re-frame/reg-event-fx
  :liitepyynto-information-request/show-deadline-error
  (fn [{db :db} [_ application-key liitepyynto-key]]
    {:db             (assoc-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline-error?] true)
     :dispatch-later [{:ms       5000
                       :dispatch [:liitepyynto-information-request/hide-deadline-error
                                  application-key
                                  liitepyynto-key]}]}))

(re-frame/reg-event-db
  :liitepyynto-information-request/hide-deadline-error
  (fn [db [_ application-key liitepyynto-key]]
    (assoc-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline-error?] false)))

(defn- handle-get-deadline
  [application-key liitepyynto-key send? date-string time-string last-modified]
  (re-frame/dispatch
   [:liitepyynto-information-request/set-send?
    application-key
    liitepyynto-key
    send?])
  (re-frame/dispatch
   [:liitepyynto-information-request/set-deadline-date
    application-key
    liitepyynto-key
    date-string])
  (re-frame/dispatch
   [:liitepyynto-information-request/set-deadline-time
    application-key
    liitepyynto-key
    time-string])
  (re-frame/dispatch
   [:liitepyynto-information-request/set-deadline-last-modified
    application-key
    liitepyynto-key
    last-modified]))

(re-frame/reg-event-fx
  :liitepyynto-information-request/get-deadlines
  (fn [_ [_ application-key]]
    {:liitepyynto-information-request/get-deadlines
     {:application-key application-key
      :on-success      (fn [deadlines last-modified]
                         (doseq [[liitepyynto-key date-string time-string] deadlines]
                           (handle-get-deadline
                            application-key
                            liitepyynto-key
                            true
                            date-string
                            time-string
                            last-modified)))
      :on-error        (fn [])}}))

(re-frame/reg-event-fx
  :liitepyynto-information-request/get-deadline
  (fn [{db :db} [_ application-key liitepyynto-key]]
    {:liitepyynto-information-request/get-deadline
     {:application-key application-key
      :liitepyynto-key liitepyynto-key
      :on-success      (fn [date-string time-string last-modified]
                         (handle-get-deadline
                          application-key
                          liitepyynto-key
                          true
                          date-string
                          time-string
                          last-modified))
      :on-error        (fn []
                         (handle-get-deadline
                          application-key
                          liitepyynto-key
                          false
                          ""
                          ""
                          nil))}}))

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
    (let [date          (get-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline-date])
          time          (get-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline-time])
          last-modified (get-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline-last-modified])]
      (when-let [datetime (try
                            (->> (str date " " time)
                                 (f/parse-local datetime-formatter)
                                 c/to-utc-time-zone)
                            (catch js/Error e
                              nil))]
        {:liitepyynto-information-request/save-deadline
         {:application-key application-key
          :liitepyynto-key liitepyynto-key
          :deadline        datetime
          :last-modified   last-modified
          :on-success      (fn [date-string time-string last-modified]
                             (handle-get-deadline
                              application-key
                              liitepyynto-key
                              true
                              date-string
                              time-string
                              last-modified))
          :on-error        (fn []
                             (re-frame/dispatch
                              [:liitepyynto-information-request/get-deadline
                               application-key
                               liitepyynto-key])
                             (re-frame/dispatch
                              [:liitepyynto-information-request/show-deadline-error
                               application-key
                               liitepyynto-key]))}}))))

(re-frame/reg-event-fx
  :liitepyynto-information-request/delete-deadline
  (fn [{db :db} [_ application-key liitepyynto-key]]
    (when-let [last-modified (get-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline-last-modified])]
      {:liitepyynto-information-request/delete-deadline
       {:application-key application-key
        :liitepyynto-key liitepyynto-key
        :last-modified   last-modified
        :on-success      (fn []
                           (handle-get-deadline
                            application-key
                            liitepyynto-key
                            false
                            ""
                            ""
                            nil))
        :on-error        (fn []
                           (re-frame/dispatch
                            [:liitepyynto-information-request/get-deadline
                             application-key
                             liitepyynto-key])
                           (re-frame/dispatch
                            [:liitepyynto-information-request/show-deadline-error
                             application-key
                             liitepyynto-key]))}})))

(defn- parse-deadline-response [response]
  (let [last-modified (get-in response [:headers "last-modified"])
        datetime      (->> (get-in response [:body :deadline])
                           (f/parse iso-formatter)
                           c/to-default-time-zone)]
    [(f/unparse date-formatter datetime)
     (f/unparse time-formatter datetime)
     last-modified]))

(re-frame/reg-fx
  :liitepyynto-information-request/get-deadlines
  (fn [{:keys [application-key on-success on-error]}]
    (ajax/GET (str "/lomake-editori/api/applications/" application-key
                   "/field-deadline")
              {:response-format (ajax/ring-response-format
                                 {:format (ajax/json-response-format {:keywords? true})})
               :handler         (fn [response]
                                  (on-success
                                   (map (fn [deadline]
                                          (let [datetime (->> (:deadline deadline)
                                                              (f/parse iso-formatter)
                                                              c/to-default-time-zone)]
                                            [(keyword (:field-id deadline))
                                             (f/unparse date-formatter datetime)
                                             (f/unparse time-formatter datetime)]))
                                        (:body response))
                                   (get-in response [:headers "last-modified"])))
               :error-handler   (fn [_] (on-error))
               :headers         {"Caller-Id" (aget js/config "virkailija-caller-id")}})))

(re-frame/reg-fx
  :liitepyynto-information-request/get-deadline
  (fn [{:keys [application-key liitepyynto-key on-success on-error]}]
    (ajax/GET (str "/lomake-editori/api/applications/" application-key
                   "/field-deadline/" (name liitepyynto-key))
              {:response-format (ajax/ring-response-format
                                 {:format (ajax/json-response-format {:keywords? true})})
               :handler         (fn [response]
                                  (let [[date time last-modified] (parse-deadline-response response)]
                                    (on-success date time last-modified)))
               :error-handler   (fn [_] (on-error))
               :headers         {"Caller-Id" (aget js/config "virkailija-caller-id")}})))

(re-frame/reg-fx
  :liitepyynto-information-request/save-deadline
  (fn [{:keys [application-key liitepyynto-key deadline last-modified on-success on-error]}]
    (ajax/PUT (str "/lomake-editori/api/applications/" application-key
                   "/field-deadline/" (name liitepyynto-key))
              {:response-format (ajax/ring-response-format
                                 {:format (ajax/json-response-format {:keywords? true})})
               :format          :json
               :keywords?       true
               :params          {:deadline (f/unparse iso-formatter deadline)}
               :handler         (fn [response]
                                  (let [[date time last-modified] (parse-deadline-response response)]
                                    (on-success date time last-modified)))
               :error-handler   (fn [_] (on-error))
               :headers         (merge {"Caller-Id" (aget js/config "virkailija-caller-id")
                                        "CSRF"      (cu/csrf-token)}
                                       (if (some? last-modified)
                                         {"If-Unmodified-Since" last-modified}
                                         {"If-None-Match" "*"}))})))

(re-frame/reg-fx
  :liitepyynto-information-request/delete-deadline
  (fn [{:keys [application-key liitepyynto-key last-modified on-success on-error]}]
    (if (some? last-modified)
      (ajax/DELETE (str "/lomake-editori/api/applications/" application-key
                        "/field-deadline/" (name liitepyynto-key))
                   {:response-format :json
                    :keywords?       true
                    :handler         (fn [_] (on-success))
                    :error-handler   (fn [_] (on-error))
                    :headers         {"Caller-Id"           (aget js/config "virkailija-caller-id")
                                      "CSRF"                (cu/csrf-token)
                                      "If-Unmodified-Since" last-modified}})
      (on-error))))
