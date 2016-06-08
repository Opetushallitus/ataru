(ns ataru.hakija.banner
  (:require [re-frame.core :refer [subscribe dispatch]]
            [cljs.core.match :refer-macros [match]]))

(def logo-image
  [:div.logo])

(def logo-text
  [:span.logo-text "Opintopolku.fi"])

(def logo [:div.logo-elements logo-image logo-text])

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

(defn wrapper-sections []
  (let [wrapper-sections (subscribe [:application/wrapper-sections])]
        (fn []
          (when @wrapper-sections
            (into [:div.application__wrapper-sections]
                  (mapv (fn [ws]
                          [:span (str (-> ws :label :fi) (:valid ws))])
                        @wrapper-sections))))))

(defn banner [] [:div
                 [:div.top-banner.application-top-banner logo [apply-controls]]
                 [wrapper-sections]])
