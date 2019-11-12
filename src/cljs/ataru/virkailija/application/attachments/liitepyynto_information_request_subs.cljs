(ns ataru.virkailija.application.attachments.liitepyynto-information-request-subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
  :liitepyynto-information-request/deadline-toggle-on?
  (fn [db [_ application-key liitepyynto-key]]
    (let [state (get-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline :visibility-state] :hidden)]
      (or (= :visible state)
          (= :appearing state)))))

(re-frame/reg-sub
  :liitepyynto-information-request/deadline-visible?
  (fn [db [_ application-key liitepyynto-key]]
    (let [state (get-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline :visibility-state] :hidden)]
      (or (= :visible state)
          (= :disappearing state)))))

(re-frame/reg-sub
  :liitepyynto-information-request/deadline-date
  (fn [db [_ application-key liitepyynto-key]]
    (get-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline :date] "")))

(re-frame/reg-sub
  :liitepyynto-information-request/deadline-time
  (fn [db [_ application-key liitepyynto-key]]
    (get-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline :time] "")))

(re-frame/reg-sub
  :liitepyynto-information-requets/deadline-error?
  (fn [db [_ application-key liitepyynto-key]]
    (get-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline :error?] false)))
