(ns ataru.hakija.arvosanat.components.valinnainen-kieli-dropdown
  (:require [ataru.hakija.components.hakija-dropdown-component :as dropdown-component]
            [ataru.hakija.schema.render-field-schema :as render-field-schema]
            [clojure.string :as string]
            [re-frame.core :as re-frame]
            [schema.core :as s]
            [schema-tools.core :as st]))

(s/defn valinnainen-kieli-dropdown
        [{:keys [valinnainen-kieli-field-descriptor
                 valinnaiset-kielet-field-descriptor
                 render-field
                 idx]} :- (-> render-field-schema/RenderFieldArgs
                              (st/select-keys [:render-field :idx])
                              (st/merge {:valinnainen-kieli-field-descriptor  s/Any
                                         :valinnaiset-kielet-field-descriptor s/Any}))]
        [dropdown-component/hakija-dropdown
         {:field-descriptor valinnainen-kieli-field-descriptor
          :render-field     render-field
          :idx              idx
          ;; hakija-dropdown kutsuu tätä myös kentän tyhjennyksellä (esim.
          ;; tyhjennysnappi, Backspace tyhjässä kentässä) — vain oikea kielen
          ;; valinta saa lisätä uuden rivin, muuten tyhjennys kasaisi tyhjiä
          ;; valinnaiset-kielet-rivejä.
          :on-change        (fn [value]
                              (when-not (string/blank? value)
                                (re-frame/dispatch [:application/add-question-group-row valinnaiset-kielet-field-descriptor])))}])
