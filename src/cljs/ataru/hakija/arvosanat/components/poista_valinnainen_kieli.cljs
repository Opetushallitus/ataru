(ns ataru.hakija.arvosanat.components.poista-valinnainen-kieli
  (:require [ataru.application-common.components.link-component :as link-component]
            [ataru.translations.translation-util :as translations]
            [re-frame.core :as re-frame]
            [schema.core :as s]))

(s/defn poista-valinnainen-kieli
        [{:keys [field-descriptor
                 idx]} :- {:field-descriptor s/Any
                           :idx              s/Int}]
        (let [lang @(re-frame/subscribe [:application/form-language])]
          [link-component/link
           {:on-click  (fn remove-valinnainen-kieli-row []
                         (re-frame/dispatch [:application/remove-question-group-row field-descriptor idx]))
            :disabled? false}
           (translations/get-hakija-translation :remove lang)]))
