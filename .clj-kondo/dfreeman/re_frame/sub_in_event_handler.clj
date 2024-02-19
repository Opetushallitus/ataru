(ns dfreeman.re-frame.sub-in-event-handler
  (:require [clj-kondo.hooks-api :as api]))

(defn subscription-node?
  [blacklist node]
  (and
    (api/token-node? node)
    (let [s (api/sexpr node)]
      (and (symbol? s)
           (reduce (fn [_ sub-symbol]
                     (and (= sub-symbol s)
                          (reduced true)))
                   false
                   blacklist)))))

(defn find-subscription-node
  [blacklist node]
  (if (subscription-node? blacklist node)
    node
    (reduce (fn [acc n]
              (or acc (find-subscription-node blacklist n)))
            nil
            (:children node))))

(defn assert-config
  [blacklist]
  (when-not (every? symbol? blacklist)
    (throw
      (ex-info "clj-kondo config path [:linters :dfreeman.re-frame/sub-in-event-handler :subscribe-symbols] must be a set of symbols."
               {:provided blacklist}))))

(defn hook [{:keys [node] :as s}]
  (let [handler (last (:children node))
        blacklist (or (-> s :config :linters :dfreeman.re-frame/sub-in-event-handler :subscribe-symbols not-empty)
                      #{'re-frame.core/subscribe 'rf/subscribe})]
    (assert-config blacklist)
    (if-let [n (find-subscription-node blacklist handler)]
      (api/reg-finding!
       (assoc (meta n)
              :message "Do not use re-frame subscriptions in event handlers."
              :type :dfreeman.re-frame/sub-in-event-handler))
      node)))
