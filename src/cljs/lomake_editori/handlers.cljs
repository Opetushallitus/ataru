(ns lomake-editori.handlers
    (:require [re-frame.core :as re-frame]
              [lomake-editori.db :as db]))

(re-frame/register-handler
 :initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/register-handler
 :set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))
