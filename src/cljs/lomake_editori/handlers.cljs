(ns lomake-editori.handlers
    (:require [re-frame.core :as re-frame :refer [register-handler]]
              [lomake-editori.db :as db]
              [taoensso.timbre :refer-macros [spy]]))

(register-handler
 :initialize-db
 (fn  [_ _]
   db/default-db))

(register-handler
 :set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

