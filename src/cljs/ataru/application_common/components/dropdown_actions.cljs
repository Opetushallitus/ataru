(ns ataru.application-common.components.dropdown-actions
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [schema.core :as s]))

(s/defn collapse-dropdown
  [{:keys [dropdown-id]} :- {:dropdown-id s/Str}]
  (re-frame/dispatch [:application-components/collapse-dropdown {:dropdown-id dropdown-id}]))

(s/defn expand-dropdown
  [{:keys [dropdown-id]} :- {:dropdown-id s/Str}]
  (re-frame/dispatch [:application-components/expand-dropdown {:dropdown-id dropdown-id}]))

(s/defn maybe-collapse-when-disabled!
  "Kenttä voi lukittua (esim. vahva tunnistautuminen / prefill) kesken popupin
  ollessa vielä auki — dropdown-render peittää expanded?:n disabled?:lla,
  jolloin popup lakkaa näkymästä, mutta mikään normaali sulkemispolku (on-
  dropdown-blur/on-option-click/on-trigger-click) ei ehdi ajaa. Ilman tätä
  re-frame-tilaan jäisi expanded? true, ja kenttä avautuisi heti uudelleen
  auki, jos se myöhemmin muuttuu takaisin käytettäväksi."
  [{:keys [dropdown-id disabled?]} :- {:dropdown-id s/Str
                                        :disabled?   s/Bool}]
  (when (and disabled?
             @(re-frame/subscribe [:state-query [:components :dropdown dropdown-id :expanded?] false]))
    (reagent/after-render #(collapse-dropdown {:dropdown-id dropdown-id}))))
