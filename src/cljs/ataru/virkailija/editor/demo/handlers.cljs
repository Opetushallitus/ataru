(ns ataru.virkailija.editor.demo.handlers
  (:require [re-frame.core :as re-frame]
            [ataru.virkailija.editor.db :as db]
            [clojure.string :as string]))

(re-frame/reg-event-db
  :editor/change-demo-validity-start
  (fn [db [_ demo-validity-start]]
    (let [path (-> (db/current-form-properties-path db [:demo-validity-start]))
          value (if (string/blank? demo-validity-start)
                  nil
                  demo-validity-start)]
      (assoc-in db path value))))

(re-frame/reg-event-db
  :editor/change-demo-validity-end
  (fn [db [_ demo-validity-end]]
    (let [path (-> (db/current-form-properties-path db [:demo-validity-end]))
          value (if (string/blank? demo-validity-end)
                  nil
                  demo-validity-end)]
      (assoc-in db path value))))
