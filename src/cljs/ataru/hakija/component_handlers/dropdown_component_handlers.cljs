(ns ataru.hakija.component-handlers.dropdown-component-handlers
  (:require [re-frame.core :as re-frame]))

(defn- toggle-dropdown-expand [dropdown-specs
                               {:keys [dropdown-id-to-toggle
                                       expand?]}]
  (as-> dropdown-specs dropdown-specs'
        (assoc-in dropdown-specs' [dropdown-id-to-toggle :expanded?] expand?)
        (reduce-kv (fn collapse-dropdown-component [acc dropdown-id dropdown-spec]
                     (let [dropdown-spec (cond-> dropdown-spec
                                                 (not= dropdown-id dropdown-id-to-toggle)
                                                 (assoc :expanded? false))]
                       (assoc acc dropdown-id dropdown-spec)))
                   {}
                   dropdown-specs')))

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
