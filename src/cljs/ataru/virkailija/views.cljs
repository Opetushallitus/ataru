(ns ataru.virkailija.views
    (:require [re-frame.core :as re-frame]
              [reagent.core :as r]
              [ataru.virkailija.views.banner :refer [top-banner]]
              [ataru.virkailija.application.view :refer [application]]
              [ataru.virkailija.dev.lomake :as l]
              [ataru.virkailija.editor.view :refer [editor]]
              [taoensso.timbre :refer-macros [spy]]))

(def panel-components
  {:editor editor :application application})

(defn no-privileges []
  [:div.privilege-info-outer [:div.privilege-info-inner "Ei oikeuksia"]])

(defn some-right-exists-for-user? [rights orgs]
  (boolean (some rights (->> orgs (map :rights) flatten (map keyword)))))

(defn privileged-panel [panel rights]
  (let [organizations (re-frame/subscribe [:state-query [:editor :user-info :organizations]])]
    (fn [panel rights]
      (if (some-right-exists-for-user? rights @organizations)
        [(get panel-components panel)]
        [no-privileges]))))

(defmulti panels identity)
(defmethod panels :application []
  [privileged-panel :application #{:view-applications :edit-applications}])
(defmethod panels :editor []
  [privileged-panel :editor #{:form-edit}])
(defmethod panels :default [])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
      [:div.main-container
       [top-banner]
        [:div (panels @active-panel)]])))
