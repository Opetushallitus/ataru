(ns ataru.virkailija.application.virkailija-application-shared-components)

(defn icon-check []
  [:span.application-handling__review-state-selected-icon.zmdi-hc-stack.zmdi-hc-lg
   [:i.zmdi.zmdi-check.zmdi-hc-stack-1x]])

(defn icon-unselected []
  [:span.application-handling__review-state-selected-icon.zmdi-hc-stack.zmdi-hc-lg
   [:i.zmdi.zmdi-hc-stack-1x]])
