(ns ataru.hakija.arvosanat.components.valinnainen-kieli-dropdown
  (:require [ataru.hakija.components.dropdown-component :as dropdown-component]
            [ataru.hakija.schema.render-field-schema :as render-field-schema]
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
          :on-change        (fn []
                              (re-frame/dispatch [:application/add-question-group-row valinnaiset-kielet-field-descriptor]))}])
