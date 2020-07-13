(ns ataru.application-common.components.link-component
  (:require [schema.core :as s]))

(s/defn link
        [{:keys [on-click
                 disabled?
                 data-test-id]} :- {:on-click                      s/Any
                                    :disabled?                     s/Bool
                                    (s/optional-key :data-test-id) s/Str}
         label :- s/Any]
        (if disabled?
          [:span.a-linkki--disabled
           {:data-test-id data-test-id}
           label]
          [:a.a-linkki
           {:href         "#"
            :data-test-id data-test-id
            :on-click     (fn [event]
                            (.preventDefault event)
                            (on-click))}
           label]))
