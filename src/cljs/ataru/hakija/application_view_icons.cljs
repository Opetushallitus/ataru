(ns ataru.hakija.application-view-icons)

(defn tutu-payment-paid []
  [:span.application-handling__tutu-payment-icon--paid.zmdi-hc-stack.zmdi-hc-lg
   [:i.zmdi.zmdi-check-circle.zmdi-hc-stack-1x]])

(defn icon-check []
  [:span.application__submitted-icon.zmdi-hc-stack.zmdi-hc-lg
   [:i.zmdi.zmdi-check]])

(defn icon-card []
  [:span.application__submitted-icon.zmdi-hc-stack.zmdi-hc-lg
   [:i.zmdi.zmdi-card]])

(defn icon-arrow []
  [:span.application__submitted-icon.zmdi-hc-stack.zmdi-hc-lg
   [:i.zmdi.zmdi-trending-flat]])

(defn icon-lock []
  [:span.application__locked-icon.zmdi-hc-stack.zmdi-hc-lg
   [:i.zmdi.zmdi-lock-open]])

(defn icon-lock-closed []
  [:span.application__locked-icon.zmdi-hc-stack.zmdi-hc-lg
   [:i.zmdi.zmdi-lock-outline]])

(defn icon-account []
  [:span.application__logged-icon.zmdi-hc-2x
   [:i.zmdi.zmdi-account-o]])

(defn icon-logout []
  [:span.application__logged-icon.zmdi-hc-lg
   [:i.zmdi.zmdi-arrow-right]])

(defn icon-arrow-drop-down []
  [:span.application__arrow-drop-down-icon.zmdi-hc
   [:i.zmdi.zmdi-caret-down.zmdi-hc-2x]])


(defn icon-arrow-drop-up []
  [:span.application__arrow-drop-up-icon.zmdi-hc
   [:i.zmdi.zmdi-caret-up.zmdi-hc-2x]])