(ns ataru.virkailija.editor.demo.handlers
  (:require [re-frame.core :as re-frame]
            [ataru.virkailija.editor.db :as db]
            [clojure.string :as string]
            [cljs-time.format :as time-format]))

(re-frame/reg-event-db
  :editor/toggle-demo-allowed
  (fn [db _]
    (let [form-path (db/current-form-properties-path db [:demo-allowed])]
      (update-in db form-path not))))

(re-frame/reg-event-db
  :editor/change-demo-validity-start
  (fn [db [_ demo-validity-start]]
    (let [path (-> (db/current-form-properties-path db [:demo-validity-start]))
          value (if (string/blank? demo-validity-start)
                  nil
                  (time-format/parse demo-validity-start))]
      (assoc-in db path value))))

(re-frame/reg-event-db
  :editor/change-demo-validity-end
  (fn [db [_ demo-validity-end]]
    (let [path (-> (db/current-form-properties-path db [:demo-validity-end]))
          value (if (string/blank? demo-validity-end)
                  nil
                  (time-format/parse demo-validity-end))]
      (assoc-in db path value))))
