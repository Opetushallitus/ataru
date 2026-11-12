(ns ataru.hakija.component-handlers.dropdown-component-handlers
  (:require [re-frame.core :as re-frame]))

(def ^:private collapsed-spec {:expanded? false :query nil :active-index nil})

;; Avattaessa muut pudotusvalikot suljetaan, jotta auki on kerrallaan vain
;; yksi. Suljettaessa koskee vain suljettavaa: muuten aiemmin jonoon jäänyt
;; toisen valikon sulkeminen (esim. sen blurista) sulkisi juuri avatun.
(defn- toggle-dropdown-expand [dropdown-specs
                               {:keys [dropdown-id-to-toggle
                                       expand?]}]
  (if expand?
    (-> (reduce-kv (fn collapse-other-dropdown [acc dropdown-id dropdown-spec]
                     (assoc acc dropdown-id (if (= dropdown-id dropdown-id-to-toggle)
                                              dropdown-spec
                                              (merge dropdown-spec collapsed-spec))))
                   {}
                   dropdown-specs)
        (assoc-in [dropdown-id-to-toggle :expanded?] true))
    (update dropdown-specs dropdown-id-to-toggle merge collapsed-spec)))

(re-frame/reg-event-db
  :application-components/collapse-dropdown
  (fn on-collapse-dropdown-component-event [db [_ {:keys [dropdown-id]}]]
    (update-in db
               [:components :dropdown]
               toggle-dropdown-expand
               {:dropdown-id-to-toggle dropdown-id
                :expand?               false})))

(re-frame/reg-event-db
  :application-components/expand-dropdown
  (fn on-expand-dropdown-component-event [db [_ {:keys [dropdown-id]}]]
    (update-in db
               [:components :dropdown]
               toggle-dropdown-expand
               {:dropdown-id-to-toggle dropdown-id
                :expand?               true})))

(re-frame/reg-event-db
  :application-components/set-dropdown-query
  (fn on-set-dropdown-query-event [db [_ {:keys [dropdown-id query]}]]
    (assoc-in db [:components :dropdown dropdown-id :query] query)))

(re-frame/reg-event-db
  :application-components/set-dropdown-active-index
  (fn on-set-dropdown-active-index-event [db [_ {:keys [dropdown-id active-index]}]]
    (assoc-in db [:components :dropdown dropdown-id :active-index] active-index)))
