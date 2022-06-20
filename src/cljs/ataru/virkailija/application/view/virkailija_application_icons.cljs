(ns ataru.virkailija.application.view.virkailija-application-icons
  (:require [re-frame.core :refer [subscribe]]))

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

(defn tutu-payment-paid []
  [:span.application-handling__tutu-payment-icon--paid.zmdi-hc-stack.zmdi-hc-lg
   [:i.zmdi.zmdi-check-circle.zmdi-hc-stack-1x]])

(defn tutu-payment-outstanding []
  [:span.application-handling__tutu-payment-icon--outstanding.zmdi-hc-stack.zmdi-hc-lg
   [:i.zmdi.zmdi-alert-triangle.zmdi-hc-stack-1x]])

(defn tutu-payment-overdue []
  [:span.application-handling__tutu-payment-icon--overdue.zmdi-hc-stack.zmdi-hc-lg
   [:i.zmdi.zmdi-alert-octagon.zmdi-hc-stack-1x]])

(defn archived-icon []
  [:i.material-icons-outlined.arkistoitu
   {:title @(subscribe [:editor/virkailija-translation :archived])}
   "archive"])
