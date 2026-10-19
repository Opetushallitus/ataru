(ns ataru.application-common.components.link-component
  (:require [schema.core :as s]
            [ataru.translations.translation-util :as translations]))

(defn new-tab-hint [lang]
  [:span.visually-hidden (str " " (translations/get-hakija-translation :opens-in-new-tab lang))])

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
