(ns ataru.hakija.banner
  (:require [re-frame.core :refer [subscribe dispatch]]
            [cljs.core.match :refer-macros [match]]))

(def logo
  [:div.logo])

(def logo-text
  [:span.logo-text "Opintopolku.fi"])

(defn apply-controls []
  (let [valid-status (subscribe [:application/valid-status])
        submit-status (subscribe [:state-query [:application :submit-status]])]
    (fn []
      [:div
       [:button.application__send-application-button
        {:disabled (or (not (:valid @valid-status)) (contains? #{:submitting :submitted} @submit-status))
         :on-click #(dispatch [:application/submit-form])}
        "LÄHETÄ HAKEMUS"]
       (match @submit-status
              :submitting [:div.application__sent-indicator "Hakemusta lähetetään"]
              :submitted [:div.application__sent-indicator "Hakemus lähetetty"]
              :else nil)])))

(defn banner [] [:div.top-banner.application-top-banner logo logo-text [apply-controls]])
