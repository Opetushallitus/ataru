(ns ataru.virkailija.application.view.virkailija-application-icons)

(defn icon-check []
  [:span.application-handling__review-state-selected-icon.zmdi-hc-stack.zmdi-hc-lg
  [:i.zmdi.zmdi-check.zmdi-hc-stack-1x]])

(defn icon-many-checked []
  [:span.application-handling__review-state-selected-icon.zmdi-hc-stack.zmdi-hc-lg
   [:i.zmdi.zmdi-check-all.zmdi-hc-stack-1x]])

(defn icon-unselected []
  [:span.application-handling__review-state-selected-icon.zmdi-hc-stack.zmdi-hc-lg
    [:i.zmdi.zmdi-hc-stack-1x]])

(defn icon-multi-check []
  [:span.application-handling__review-state-selected-icon.zmdi-hc-stack.zmdi-hc-lg
    [:i.zmdi.zmdi-square-o.zmdi-hc-stack-1x]
    [:i.zmdi.zmdi-check.zmdi-hc-stack-1x]])

(defn icon-select []
  [:span.application-handling__review-state-selected-icon.zmdi-hc-stack.zmdi-hc-lg
    [:i.zmdi.zmdi-square-o.zmdi-hc-stack-1x]])
