(ns ataru.virkailija.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :active-panel
 (fn [db]
   (:active-panel db)))

(re-frame/reg-sub
  :state-query
  (fn [db [_ path]]
    (get-in db path)))

(re-frame/reg-sub
  :snackbar-message
  (fn [db]
    (get-in db [:snackbar-message])))

(re-frame/reg-sub
  :toast-messages
  (fn [db]
    (get-in db [:toast-messages])))
