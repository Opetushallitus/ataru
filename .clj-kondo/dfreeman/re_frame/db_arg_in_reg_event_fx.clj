(ns dfreeman.re-frame.db-arg-in-reg-event-fx
  (:require [clj-kondo.hooks-api :as api]))

(defn suggested-name
  [node]
  (case (name (api/sexpr (first (:children node))))
    "reg-event-fx" "cofx or {db :db}"
    "reg-event-ctx" "ctx or {{db :db} :coeffects}"))

(defn hook
  [{:keys [node]}]
  (let [handler (last (:children node))
        args (first (filter api/vector-node? (:children handler)))
        cofx-or-ctx-arg (first (:children args))]
    (if (and (api/token-node? cofx-or-ctx-arg)
             (= 'db (api/sexpr cofx-or-ctx-arg)))
      (api/reg-finding!
        (assoc (meta cofx-or-ctx-arg)
               :message (str "Did you mean " (suggested-name node) "?")
               :type :dfreeman.re-frame/db-arg-in-reg-event-fx))
      node)))
